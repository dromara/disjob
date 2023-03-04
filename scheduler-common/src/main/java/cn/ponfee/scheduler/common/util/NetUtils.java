/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * IP and Port Helper for RPC
 *
 * <pre>
 *  isAnyLocalAddress  通配符地址        IPv4的通配符地址是0.0.0.0
 *  isLoopbackAddress  回环地址          IPv4的的范围是127.0.0.0 ~ 127.255.255.255    IPv6的是0:0:0:0:0:0:0:1,也可以简写成::1
 *  isLinkLocalAddress 本地连接地址       IPv4的的范围是169.254.0.0 ~ 169.254.255.255  IPv6的前12位是FE8，其他的位可以是任意取值
 *  isSiteLocalAddress 地区本地地址       IPv4的分为三段:10.0.0.0 ~ 10.255.255.255等   IPv6的地区本地地址的前12位是FEC，其他的位可以是任意取值
 *  isMulticastAddress 广播地址          IPv4的范围是224.0.0.0 ~ 239.255.255.255     IPv6的第一个字节是FF，其他的字节可以是任意值
 *  isMCGlobal         全球范围的广播地址
 *  isMCLinkLocal      子网广播地址
 *  isMCNodeLocal      本地接口广播地址
 *  isMCOrgLocal       组织范围的广播地址
 *  isMCSiteLocal      站点范围的广播地址
 * </pre>
 *
 * @author Ponfee
 */
public final class NetUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NetUtils.class);

    // returned port range is [30000, 39999]
    private static final int RND_PORT_START = 30000;
    private static final int RND_PORT_RANGE = 10000;

    // valid port range is (0, 65535]
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private static final String LOCALHOST_NAME  = "localhost";
    private static final String LOCALHOST_VALUE = "127.0.0.1";
    private static final String ANYHOST_VALUE   = "0.0.0.0";

    private static final String IGNORED_NETWORK_INTERFACE   = "distributed.scheduler.network.interface.ignored";
    private static final String PREFERRED_NETWORK_INTERFACE = "distributed.scheduler.network.interface.preferred";

    private static final Pattern ADDRESS_PATTERN  = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}\\:\\d{1,5}$");
    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");
    private static final Pattern IP_PATTERN       = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    /**
     * store the used port.
     * the set used only on the synchronized method.
     */
    private static BitSet USED_PORT = new BitSet(65536);

    private static final Map<String, String> HOST_NAME_CACHE = new ConcurrentHashMap<>(1000);

    private static volatile InetAddress localAddress = null;
    private static volatile String localHost;

    /**
     * Returns the hostname port is connectable.
     *
     * @param host    the host
     * @param port    the port
     * @param timeout the timeout value to be used in milliseconds.
     * @return {@code true} if connectable
     */
    public static boolean isConnectableHostPort(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * Check the port is available
     *
     * @param port 待测试端口
     * @return if @{code true} is available, else unavailable
     */
    public static boolean isAvailablePort(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            return false;
        }
        if (USED_PORT.get(port)) {
            return false;
        }
        try (ServerSocket ss = new ServerSocket(port)) {
            return true;
        } catch (IOException ignored) {
            USED_PORT.set(port);
            return false;
        }
    }

    public static int findAvailablePort() {
        return findAvailablePort(getRandomPort());
    }

    /**
     * Returns this server available port
     *
     * @param startPort
     * @return if -1 then not find available port
     * else returns available port
     */
    public static int findAvailablePort(int startPort) {
        startPort = Math.max(startPort, MIN_PORT);
        startPort = Math.min(startPort, MAX_PORT);

        for (int port = startPort; port <= MAX_PORT; port++) {
            if (isAvailablePort(port)) {
                return port;
            }
        }

        for (int port = startPort - 1; port >= MIN_PORT; port--) {
            if (isAvailablePort(port)) {
                return port;
            }
        }

        return -1;
    }

    public static boolean isValidAddress(String address) {
        return ADDRESS_PATTERN.matcher(address).matches();
    }

    public static boolean isLocalHost(String host) {
        if (StringUtils.isEmpty(host)) {
            return false;
        }
        return LOCAL_IP_PATTERN.matcher(host).matches()
            || host.equalsIgnoreCase(LOCALHOST_NAME);
    }

    public static boolean isAnyHost(String host) {
        return ANYHOST_VALUE.equals(host);
    }

    public static boolean isInvalidLocalHost(String host) {
        return StringUtils.isEmpty(host)
            || host.equalsIgnoreCase(LOCALHOST_NAME)
            || host.equals(ANYHOST_VALUE)
            || host.startsWith("127.");
    }

    public static boolean isValidLocalHost(String host) {
        return !isInvalidLocalHost(host);
    }

    public static String getLocalHost() {
        if (localHost != null) {
            return localHost;
        }

        synchronized (NetUtils.class) {
            if (localHost != null) {
                return localHost;
            }
            String hostAddress = Optional.ofNullable(getLocalAddress()).map(InetAddress::getHostAddress).orElse(LOCALHOST_VALUE);
            NetUtils.localHost = hostAddress;
            return hostAddress;
        }
    }

    /**
     * Find first valid IP from local network card
     *
     * @return first valid local IP
     */
    public static InetAddress getLocalAddress() {
        if (localAddress != null) {
            return localAddress;
        }
        synchronized (NetUtils.class) {
            if (localAddress != null) {
                return localAddress;
            }
            InetAddress localAddress = getLocalAddress0();
            NetUtils.localAddress = localAddress;
            return localAddress;
        }
    }

    public static InetSocketAddress getLocalSocketAddress(String host, int port) {
        return isInvalidLocalHost(host)
            ? new InetSocketAddress(port)
            : new InetSocketAddress(host, port);
    }

    public static boolean isValidV4Address(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }

        String name = address.getHostAddress();
        return name != null
            && IP_PATTERN.matcher(name).matches()
            && !ANYHOST_VALUE.equals(name)
            && !LOCALHOST_VALUE.equals(name);
    }

    /**
     * Check if an ipv6 address
     *
     * @return true if it is reachable
     */
    public static boolean isPreferIPV6Address() {
        return Boolean.getBoolean("java.net.preferIPv6Addresses");
    }

    /**
     * normalize the ipv6 Address, convert scope name to scope id.
     * e.g.
     * convert
     * fe80:0:0:0:894:aeec:f37d:23e1%en0
     * to
     * fe80:0:0:0:894:aeec:f37d:23e1%5
     * <p>
     * The %5 after ipv6 address is called scope id.
     * see java doc of {@link Inet6Address} for more details.
     *
     * @param address the input address
     * @return the normalized address, with scope id converted to int
     */
    public static InetAddress normalizeV6Address(Inet6Address address) {
        String addr = address.getHostAddress();
        int i = addr.lastIndexOf('%');
        if (i > 0) {
            try {
                return InetAddress.getByName(addr.substring(0, i) + '%' + address.getScopeId());
            } catch (UnknownHostException ignored) {
                // ignored
            }
        }
        return address;
    }

    /**
     * Is preferred {@link NetworkInterface} or not
     *
     * @param networkInterface {@link NetworkInterface}
     * @return if the name of the specified {@link NetworkInterface} matches
     * the property value from {@link #PREFERRED_NETWORK_INTERFACE}, return <code>true</code>,
     * or <code>false</code>
     */
    public static boolean isPreferredNetworkInterface(NetworkInterface networkInterface) {
        String preferredNetworkInterface = getSystemConfig(PREFERRED_NETWORK_INTERFACE);
        return Objects.equals(networkInterface.getDisplayName(), preferredNetworkInterface);
    }

    /**
     * Get the suitable {@link NetworkInterface}
     *
     * @return If no {@link NetworkInterface} is available , return <code>null</code>
     */
    public static NetworkInterface findNetworkInterface() {
        List<NetworkInterface> validNetworkInterfaces;
        try {
            validNetworkInterfaces = getValidNetworkInterfaces();
        } catch (Throwable e) {
            LOG.warn("Failed to get network interface: {}", e.getMessage());
            validNetworkInterfaces = Collections.emptyList();
        }

        // Try to find the preferred one
        for (NetworkInterface networkInterface : validNetworkInterfaces) {
            if (networkInterface != null && isPreferredNetworkInterface(networkInterface)) {
                return networkInterface;
            }
        }

        // If not found, try to get the first one
        for (NetworkInterface networkInterface : validNetworkInterfaces) {
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = toValidAddress(addresses.nextElement());
                if (isReachableAddress(address)) {
                    return networkInterface;
                }
            }
        }

        return Collects.getFirst(validNetworkInterfaces);
    }

    public static String getHostName(String address) {
        try {
            int i = address.indexOf(':');
            if (i > -1) {
                address = address.substring(0, i);
            }
            String hostname = HOST_NAME_CACHE.get(address);
            if (StringUtils.isNotEmpty(hostname)) {
                return hostname;
            }
            InetAddress inetAddress = InetAddress.getByName(address);
            if (inetAddress != null) {
                hostname = inetAddress.getHostName();
                HOST_NAME_CACHE.put(address, hostname);
                return hostname;
            }
        } catch (Throwable ignored) {
            // ignored
        }
        return address;
    }

    /**
     * getIpByHost
     *
     * @param hostName the host name
     * @return ip address or hostName if UnknownHostException
     */
    public static String getIpByHost(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            return hostName;
        }
    }

    public static String toAddressString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    public static InetSocketAddress toAddress(String address) {
        int i = address.indexOf(':');
        return i != -1
            ? new InetSocketAddress(address.substring(0, i), Integer.parseInt(address.substring(i + 1)))
            : new InetSocketAddress(address, 0);
    }

    public static String toURL(String protocol, String host, int port, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://").append(host).append(':').append(port);
        if (path.charAt(0) != '/') {
            sb.append('/');
        }
        sb.append(path);
        return sb.toString();
    }

    public static void joinMulticastGroup(MulticastSocket multicastSocket, InetAddress multicastAddress) throws IOException {
        setInterface(multicastSocket, multicastAddress instanceof Inet6Address);
        multicastSocket.setLoopbackMode(false);
        multicastSocket.joinGroup(multicastAddress);
    }

    public static void setInterface(MulticastSocket multicastSocket, boolean preferIpv6) throws IOException {
        for (NetworkInterface networkInterface : getValidNetworkInterfaces()) {
            Enumeration<?> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = (InetAddress) addresses.nextElement();
                if (preferIpv6) {
                    if (!(address instanceof Inet6Address)) {
                        continue;
                    }
                } else {
                    if (!(address instanceof Inet4Address)) {
                        continue;
                    }
                }
                if (isReachableAddress(address)) {
                    multicastSocket.setInterface(address);
                    return;
                }
            }
        }
    }

    // ----------------------------------------------------------------private methods

    private static int getRandomPort() {
        return RND_PORT_START + ThreadLocalRandom.current().nextInt(RND_PORT_RANGE);
    }

    private static boolean isReachableAddress(InetAddress address) {
        if (address == null) {
            return false;
        }
        try {
            if (address.isReachable(100)) {
                return true;
            }
        } catch (IOException ignored) {
            // ignored
        }
        return false;
    }

    private static InetAddress toValidAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            Inet6Address v6Address = (Inet6Address) address;
            if (isPreferIPV6Address()) {
                return normalizeV6Address(v6Address);
            }
        }
        if (isValidV4Address(address)) {
            return address;
        }
        return null;
    }

    private static InetAddress getLocalAddress0() {
        // choose the {@link NetworkInterface} first
        try {
            NetworkInterface networkInterface = findNetworkInterface();
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = toValidAddress(addresses.nextElement());
                if (isReachableAddress(address)) {
                    return address;
                }
            }
        } catch (Throwable e) {
            LOG.warn("Failed to get network address: {}", e.getMessage());
        }

        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            InetAddress address = toValidAddress(localAddress);
            if (address != null) {
                return address;
            }
        } catch (Throwable e) {
            LOG.warn("Failed to get local host address: {} ", e.getMessage());
        }

        return localAddress;
    }

    /**
     * Returns {@code true} if the specified {@link NetworkInterface} should be ignored with the given conditions.
     * @param networkInterface the {@link NetworkInterface} to check
     * @return {@code true} if the specified {@link NetworkInterface} should be ignored, otherwise {@code false}
     * @throws SocketException if an I/O error occurs.
     */
    private static boolean ignoreNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        if (   networkInterface == null
            || networkInterface.isLoopback()
            || networkInterface.isVirtual()
            || !networkInterface.isUp()
        ) {
            return true;
        }

        String networkInterfaceDisplayName = networkInterface.getDisplayName();
        if (StringUtils.isEmpty(networkInterfaceDisplayName)) {
            return false;
        }

        String ignoredInterfaces = getSystemConfig(IGNORED_NETWORK_INTERFACE);
        if (StringUtils.isEmpty(ignoredInterfaces)) {
            return false;
        }

        for (String ignoredInterface : ignoredInterfaces.split(",")) {
            String trimIgnoredInterface = ignoredInterface.trim();
            boolean matched = false;
            try {
                matched = networkInterfaceDisplayName.matches(trimIgnoredInterface);
            } catch (PatternSyntaxException e) {
                // if trimIgnoredInterface is invalid regular expression, a PatternSyntaxException will be thrown out
                LOG.warn("exception occurred: " + networkInterfaceDisplayName + " matches " + trimIgnoredInterface, e);
            }
            if (matched || networkInterfaceDisplayName.equals(trimIgnoredInterface)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the valid {@link NetworkInterface network interfaces}
     *
     * @return the valid {@link NetworkInterface}s
     * @throws SocketException if an I/O error occurs.
     */
    private static List<NetworkInterface> getValidNetworkInterfaces() throws SocketException {
        List<NetworkInterface> validNetworkInterfaces = new LinkedList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!ignoreNetworkInterface(networkInterface)) {
                validNetworkInterfaces.add(networkInterface);
            }
        }
        return validNetworkInterfaces;
    }

    public static String getSystemConfig(String name) {
        String value = System.getProperty(name);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }

        return System.getenv(name);
    }

}
