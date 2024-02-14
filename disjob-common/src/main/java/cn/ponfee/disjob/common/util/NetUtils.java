/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.collect.Collects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 *
 *  host address：host name、ip address
 * </pre>
 *
 * @author Ponfee
 */
public final class NetUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NetUtils.class);

    public static final String IGNORED_NETWORK_INTERFACE   = "disjob.network.interface.ignored";
    public static final String PREFERRED_NETWORK_INTERFACE = "disjob.network.interface.preferred";

    // returned port range is [30000, 39999]
    private static final int RND_PORT_START = 30000;
    private static final int RND_PORT_RANGE = 10000;

    // valid port range is (0, 65535]
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private static final String LOCAL_HOST_NAME  = "localhost";
    private static final String LOCAL_IP_ADDRESS = "127.0.0.1";
    private static final String ANY_IP_ADDRESS   = "0.0.0.0";
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    private static final int PING_TIMEOUT = 300;

    /**
     * store the used port.
     * the set used only on the synchronized method.
     */
    private static final BitSet USED_PORT = new BitSet(65536);

    private static final ConcurrentMap<String, String> HOST_NAME_CACHE = new ConcurrentHashMap<>(100);

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
        } catch (Exception ignored) {
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
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (Exception ignored) {
            USED_PORT.set(port);
            return false;
        }
    }

    public static boolean isValidIpAddress(String ip) {
        return IP_ADDRESS_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidLocalHost(String host) {
        if (StringUtils.isBlank(host) ||
            host.equalsIgnoreCase(LOCAL_HOST_NAME) ||
            host.equals(ANY_IP_ADDRESS) ||
            host.startsWith("127.")) {
            return false;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            return !inetAddress.isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isReachableHost(String host) {
        if (!isValidLocalHost(host)) {
            return false;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isReachable(PING_TIMEOUT)) {
                return true;
            }
        } catch (Exception ignored) {
            // ignored
        }

        try {
            Process process = Runtime.getRuntime().exec("ping -c 1 " + host);
            boolean exited = process.waitFor(PING_TIMEOUT, TimeUnit.MILLISECONDS);
            return exited && process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isAnyHost(String host) {
        return ANY_IP_ADDRESS.equals(host);
    }

    /**
     * Find first valid IP from local network card
     *
     * @return first valid local IP
     */
    public static String getLocalHost() {
        if (localHost != null) {
            return localHost;
        }

        synchronized (NetUtils.class) {
            if (localHost != null) {
                return localHost;
            }
            String hostAddress = Optional.ofNullable(getLocalAddress()).map(InetAddress::getHostAddress).orElse(LOCAL_IP_ADDRESS);
            NetUtils.localHost = hostAddress;
            return hostAddress;
        }
    }

    /**
     * Gets host name by host address
     *
     * @param hostAddress the host address
     * @return host name
     */
    public static String getHostName(String hostAddress) {
        if (StringUtils.isEmpty(hostAddress)) {
            return hostAddress;
        }

        int i = hostAddress.indexOf(':');
        if (i != -1) {
            hostAddress = hostAddress.substring(0, i);
        }

        return HOST_NAME_CACHE.computeIfAbsent(hostAddress, addr -> {
            String hostName = null;
            try {
                InetAddress inetAddress = InetAddress.getByName(addr);
                if (inetAddress != null) {
                    hostName = inetAddress.getHostName();
                }
            } catch (Throwable ignored) {
                // ignored
            }
            return StringUtils.isEmpty(hostName) ? addr : hostName;
        });
    }

    /**
     * Gets host address by host name
     *
     * @param hostName the host name
     * @return ip address or hostName if UnknownHostException
     */
    public static String getHostAddress(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException ignored) {
            return hostName;
        }
    }

    public static int findAvailablePort() {
        return findAvailablePort(getRandomPort());
        /*
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            serverSocket.setReuseAddress(true);
            USED_PORT.set(port);
            return port;
        } catch (IOException e) {
            throw new IllegalStateException("Not found available socket port.", e);
        }
        */
    }

    /**
     * Returns this server available port
     *
     * @param startPort the start port
     * @return available port
     * @throws IllegalStateException if not found
     */
    public static int findAvailablePort(int startPort) throws IllegalStateException {
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

        throw new IllegalStateException("Not found available socket port.");
    }

    public static boolean isValidIpv4Address(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }

        String name = address.getHostAddress();
        return name != null
            && IP_ADDRESS_PATTERN.matcher(name).matches()
            && !ANY_IP_ADDRESS.equals(name)
            && !LOCAL_IP_ADDRESS.equals(name);
    }

    /**
     * Get the suitable {@link NetworkInterface}
     *
     * @return If no {@link NetworkInterface} is available , return <code>null</code>
     */
    public static NetworkInterface findValidNetworkInterface() {
        List<NetworkInterface> validNetworkInterfaces = new LinkedList<>();
        try {
            // remove ignored network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!isIgnoredNetworkInterface(networkInterface)) {
                    validNetworkInterfaces.add(networkInterface);
                }
            }
        } catch (Throwable e) {
            LOG.warn("Failed to get network interface: {}", e.getMessage());
            return null;
        }

        // Try to find the preferred one
        for (NetworkInterface networkInterface : validNetworkInterfaces) {
            if (isPreferredNetworkInterface(networkInterface)) {
                if (getReachableAddress(networkInterface) != null) {
                    return networkInterface;
                } else {
                    LOG.warn("Preferred network interface not has reachable address: {}", networkInterface.getDisplayName());
                }
            }
        }

        // If not found, try to get the first valid one
        for (NetworkInterface networkInterface : validNetworkInterfaces) {
            if (getReachableAddress(networkInterface) != null) {
                return networkInterface;
            }
        }

        return Collects.getFirst(validNetworkInterfaces);
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
            return address.isReachable(PING_TIMEOUT);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static InetAddress getReachableAddress(NetworkInterface networkInterface) {
        if (networkInterface == null) {
            return null;
        }

        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress address = toValidAddress(addresses.nextElement());
            if (isReachableAddress(address)) {
                return address;
            }
        }
        return null;
    }

    /**
     * Check if an ipv6 address
     *
     * @return true if it is reachable
     */
    private static boolean isPreferIpv6Address() {
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
    private static InetAddress normalizeIpv6Address(Inet6Address address) {
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

    private static InetAddress toValidAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            Inet6Address v6Address = (Inet6Address) address;
            if (isPreferIpv6Address()) {
                return normalizeIpv6Address(v6Address);
            }
        }
        if (isValidIpv4Address(address)) {
            return address;
        }
        return null;
    }

    private static InetAddress getLocalAddress() {
        // choose the {@link NetworkInterface} first
        try {
            NetworkInterface validNetworkInterface = findValidNetworkInterface();
            InetAddress address = getReachableAddress(validNetworkInterface);
            if (address != null) {
                return address;
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
     *
     * @param networkInterface the {@link NetworkInterface} to check
     * @return {@code true} if the specified {@link NetworkInterface} should be ignored, otherwise {@code false}
     * @throws SocketException if an I/O error occurs.
     */
    private static boolean isIgnoredNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        if (    networkInterface == null
            ||  networkInterface.isLoopback()
            ||  networkInterface.isVirtual()
            || !networkInterface.isUp()
        ) {
            return true;
        }

        String ignoredInterfaceNameRegex = SystemUtils.getConfig(IGNORED_NETWORK_INTERFACE);
        if (StringUtils.isEmpty(ignoredInterfaceNameRegex)) {
            // not configured ignore network interface name
            return false;
        }

        List<String> networkNames = Stream.of(networkInterface.getDisplayName(), networkInterface.getName())
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
        if (networkNames.isEmpty()) {
            // network interface has not name
            return false;
        }

        for (String ignoredNameRegex : ignoredInterfaceNameRegex.split(",")) {
            String regex = ignoredNameRegex.trim();
            try {
                if (networkNames.stream().anyMatch(e -> e.equals(regex) || e.matches(regex))) {
                    return true;
                }
            } catch (Throwable t) {
                // if regex is invalid regular expression, will be thrown PatternSyntaxException
                LOG.warn("exception occurred: " + networkNames + " matches " + regex, t);
            }
        }
        return false;
    }

    /**
     * Is preferred {@link NetworkInterface} or not
     *
     * @param networkInterface {@link NetworkInterface}
     * @return if the name of the specified {@link NetworkInterface} matches
     * the property value from {@link #PREFERRED_NETWORK_INTERFACE}, return <code>true</code>,
     * or <code>false</code>
     */
    private static boolean isPreferredNetworkInterface(NetworkInterface networkInterface) {
        if (networkInterface == null) {
            return false;
        }
        String preferredNetworkInterfaceName = SystemUtils.getConfig(PREFERRED_NETWORK_INTERFACE);
        return Objects.equals(networkInterface.getDisplayName(), preferredNetworkInterfaceName)
            || Objects.equals(networkInterface.getName(), preferredNetworkInterfaceName);
    }

}
