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
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * 网络工具类
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
public final class Networks {

    private static final Logger LOG = LoggerFactory.getLogger(Networks.class);

    public static final String LOCALHOST_IP = "127.0.0.1";
    public static final String LOCALHOST_NAME = "localhost";
    public static final String EMPTY_IP = "0.0.0.0";

    /**
     * the max ip value
     * <p>toLong("255.255.255.255")
     */
    public static final long MAX_IP_VALUE = (1L << 32) - 1;

    /**
     * 掩码
     */
    private static final long[] MASK = {0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF};

    /**
     * IP V4 pattern
     */
    private static final Pattern PATTERN_IPV4 = Pattern.compile("(?:(?:2[0-4][0-9]\\.)|(?:25[0-5]\\.)|(?:1[0-9][0-9]\\.)|(?:[1-9][0-9]\\.)|(?:[0-9]\\.)){3}(?:(?:2[0-4][0-9])|(?:25[0-5])|(?:1[0-9][0-9])|(?:[1-9][0-9])|(?:[0-9]))");

    /**
     * IP V6 pattern
     */
    private static final Pattern PATTERN_IPV6 = Pattern.compile("^([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4}|:)|([0-9a-fA-F]{1,4}:){1,6}(:[0-9a-fA-F]{1,4}|:)|([0-9a-fA-F]{1,4}:){1,5}((:[0-9a-fA-F]{1,4}){1,2}|:)|([0-9a-fA-F]{1,4}:){1,4}((:[0-9a-fA-F]{1,4}){1,3}|:)|([0-9a-fA-F]{1,4}:){1,3}((:[0-9a-fA-F]{1,4}){1,4}|:)|([0-9a-fA-F]{1,4}:){1,2}((:[0-9a-fA-F]{1,4}){1,5}|:)|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6}|:)|:((:[0-9a-fA-F]{1,4}){1,7}|:)");


    /**
     * local ip
     */
    public static final String HOST_IP = getHostIp();

    /**
     * getMachineNetworkFlag 获取机器的MAC或者IP，优先获取MAC
     *
     * @param ia InetAddress
     * @return mac or ip
     */
    public static String getMacOrIp(InetAddress ia) {
        if (ia == null) {
            try {
                ia = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        String mac = getMacAddress(ia);
        return StringUtils.isBlank(mac) ? getIpAddress(ia) : mac;
    }

    /**
     * 获取指定地址的mac地址，不指定默认取本机的mac地址
     *
     * @param ia InetAddress
     * @return mac or ip
     */
    public static String getMacAddress(InetAddress ia) {
        byte[] mac;
        try {
            if (ia == null) {
                ia = InetAddress.getLocalHost();
            }
            mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }

        if (mac == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(17);
        for (int i = 0; i < mac.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            sb.append(Bytes.hexEncode(mac[i], false));
        }
        return sb.toString();
    }

    public static String getHostIp() {
        InetAddress address = getHostAddress();
        return address == null ? LOCALHOST_IP : address.getHostAddress();
    }

    public static String getHostName() {
        InetAddress address = getHostAddress();
        return address == null ? LOCALHOST_NAME : address.getHostName();
    }

    /**
     * Returns the hostname port is connectable.
     *
     * @param hostname the hostname
     * @param port     the port
     * @param timeout  the timeout value to be used in milliseconds.
     * @return {@code true} if connectable
     */
    public static boolean isConnectable(String hostname, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostname, port), timeout);
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
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.bind(null);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * Returns this server available port
     *
     * @param startPort
     * @return if -1 then not find available port
     * else returns available port
     */
    public static int findAvailablePort(int startPort) {
        int maxPortNumber = 65535;
        if (startPort < 0 || startPort > maxPortNumber) {
            return -1;
        }

        for (int port = startPort; port <= maxPortNumber; port++) {
            if (isAvailablePort(port)) {
                return port;
            }
        }

        for (int port = startPort - 1; port >= 0; port--) {
            if (isAvailablePort(port)) {
                return port;
            }
        }

        return -1;
    }

    /**
     * Convert ipv4 to long，max value is 4294967295
     *
     * @param ip the ip address
     * @return
     */
    public static long toLong(String ip) {
        if (!isIpv4(ip)) {
            throw new IllegalArgumentException("invalid ip address[" + ip + "]");
        }
        String[] ipNums = ip.split("\\.", 4);
        return (Long.parseLong(ipNums[0]) << 24)
             + (Long.parseLong(ipNums[1]) << 16)
             + (Long.parseLong(ipNums[2]) <<  8)
             + (Long.parseLong(ipNums[3])      );
    }

    /**
     * Convert long value to ipv4 address string
     *
     * @param ip
     * @return
     */
    public static String fromLong(long ip) {
        return new StringBuilder(15)
            .append((ip & MASK[0]) >> 24).append('.')
            .append((ip & MASK[1]) >> 16).append('.')
            .append((ip & MASK[2]) >>  8).append('.')
            .append((ip & MASK[3])      ).toString();
    }

    /**
     * 校验是否ipv4地址
     *
     * @param text
     * @return {@code true} is ipv4 address
     */
    public static boolean isIpv4(String text) {
        return text != null && PATTERN_IPV4.matcher(text).matches();
    }


    /**
     * 校验是否ipv6地址
     *
     * @param text
     * @return {@code true} is ipv6 address
     */
    public static boolean isIpv6(String text) {
        return text != null && PATTERN_IPV6.matcher(text).matches();
    }


    /**
     * 获取指定地址的ip地址，不指定默认取本机的ip地址
     *
     * @param ia InetAddress
     * @return mac or ip
     */
    private static String getIpAddress(InetAddress ia) {
        return ia.getHostAddress();
    }

    private static InetAddress getHostAddress() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidHostAddress(localAddress)) {
                return localAddress;
            }
        } catch (Exception e) {
            LOG.warn("Failed to get local host address: {} ", e.getMessage());
        }

        try {
            Enumeration<NetworkInterface> inters = NetworkInterface.getNetworkInterfaces();
            if (inters == null) {
                return localAddress;
            }
            while (inters.hasMoreElements()) {
                try {
                    Enumeration<InetAddress> addresses = inters.nextElement().getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        try {
                            InetAddress address = addresses.nextElement();
                            if (isValidHostAddress(address)) {
                                return address;
                            }
                        } catch (Exception e) {
                            LOG.warn("Failed to get host address: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to get network address: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to get network interface: {}", e.getMessage());
        }

        LOG.warn("Could not get host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }

    /**
     * Returns the host address is valid
     *
     * @param address
     * @return if {@code true} is valid, else invalid
     */
    private static boolean isValidHostAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String ip = address.getHostAddress();
        return ip != null
            && !EMPTY_IP.equals(ip)
            && !LOCALHOST_IP.equals(ip)
            && isIpv4(ip);
    }
}
