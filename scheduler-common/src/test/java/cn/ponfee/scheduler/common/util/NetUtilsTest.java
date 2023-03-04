/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ponfee.scheduler.common.util;


import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NetUtils Test
 */
public class NetUtilsTest {

    @Test
    void testGetAvailablePort() {
        assertThat(NetUtils.findAvailablePort(), greaterThan(0));
        assertThat(NetUtils.findAvailablePort(12345), greaterThanOrEqualTo(12345));
        assertThat(NetUtils.findAvailablePort(-1), greaterThanOrEqualTo(0));
    }

    @Test
    void testValidAddress() {
        assertTrue(NetUtils.isValidAddress("10.20.130.230:20880"));
        assertFalse(NetUtils.isValidAddress("10.20.130.230"));
        assertFalse(NetUtils.isValidAddress("10.20.130.230:666666"));
    }

    @Test
    void testIsAvailablePort() {
        assertFalse(NetUtils.isAvailablePort(0));
        assertFalse(NetUtils.isAvailablePort(65536));
        assertTrue(NetUtils.isAvailablePort(1024));
    }

    @Test
    void testIsLocalHost() {
        assertTrue(NetUtils.isLocalHost("localhost"));
        assertTrue(NetUtils.isLocalHost("127.1.2.3"));
        assertFalse(NetUtils.isLocalHost("128.1.2.3"));
    }

    @Test
    void testIsAnyHost() {
        assertTrue(NetUtils.isAnyHost("0.0.0.0"));
        assertFalse(NetUtils.isAnyHost("1.1.1.1"));
    }

    @Test
    void testIsInvalidLocalHost() {
        assertTrue(NetUtils.isInvalidLocalHost(null));
        assertTrue(NetUtils.isInvalidLocalHost(""));
        assertTrue(NetUtils.isInvalidLocalHost("localhost"));
        assertTrue(NetUtils.isInvalidLocalHost("0.0.0.0"));
        assertTrue(NetUtils.isInvalidLocalHost("127.1.2.3"));
        assertTrue(NetUtils.isInvalidLocalHost("127.0.0.1"));
        assertFalse(NetUtils.isInvalidLocalHost("128.0.0.1"));
    }

    @Test
    void testIsValidLocalHost() {
        assertTrue(NetUtils.isValidLocalHost("1.2.3.4"));
        assertTrue(NetUtils.isValidLocalHost("128.0.0.1"));
    }

    @Test
    void testIsConnectable() {
        assertTrue(NetUtils.isConnectableHostPort("www.baidu.com", 80, 100));
        assertFalse(NetUtils.isConnectableHostPort("www.unknownhostname.com", 80, 100));
    }

    @Test
    void testGetLocalSocketAddress() {
        InetSocketAddress address = NetUtils.getLocalSocketAddress("localhost", 12345);
        assertTrue(address.getAddress().isAnyLocalAddress());
        assertEquals(address.getPort(), 12345);
        address = NetUtils.getLocalSocketAddress("dubbo-addr", 12345);
        assertEquals(address.getHostName(), "dubbo-addr");
        assertEquals(address.getPort(), 12345);
    }

    @Test
    void testIsValidAddress() {
        assertFalse(NetUtils.isValidV4Address((InetAddress) null));
        InetAddress address = mock(InetAddress.class);
        when(address.isLoopbackAddress()).thenReturn(true);
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("localhost");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("0.0.0.0");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("127.0.0.1");
        assertFalse(NetUtils.isValidV4Address(address));
        address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("1.2.3.4");
        assertTrue(NetUtils.isValidV4Address(address));
    }

    @Test
    void testGetLocalHost() {
        assertNotNull(NetUtils.getLocalHost());
    }

    @Test
    void testGetLocalAddress() {
        InetAddress address = NetUtils.getLocalAddress();
        assertNotNull(address);
        assertTrue(NetUtils.isValidLocalHost(address.getHostAddress()));
    }

    @Test
    void testGetHostName() {
        assertNotNull(NetUtils.getHostName("127.0.0.1"));
    }

    @Test
    void testGetIpByHost() {
        assertThat(NetUtils.getIpByHost("localhost"), equalTo("127.0.0.1"));
        assertThat(NetUtils.getIpByHost("dubbo.local"), equalTo("dubbo.local"));
    }

    @Test
    void testToAddressString() {
        InetAddress address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn("dubbo");
        InetSocketAddress socketAddress = new InetSocketAddress(address, 1234);
        assertThat(NetUtils.toAddressString(socketAddress), equalTo("dubbo:1234"));
    }

    @Test
    void testToAddress() {
        InetSocketAddress address = NetUtils.toAddress("localhost:1234");
        assertThat(address.getHostName(), equalTo("localhost"));
        assertThat(address.getPort(), equalTo(1234));
        address = NetUtils.toAddress("localhost");
        assertThat(address.getHostName(), equalTo("localhost"));
        assertThat(address.getPort(), equalTo(0));
    }

    @Test
    void testToURL() {
        String url = NetUtils.toURL("dubbo", "host", 1234, "foo");
        assertThat(url, equalTo("dubbo://host:1234/foo"));
    }

    @Test
    void testIsValidV6Address() {
        String saved = System.getProperty("java.net.preferIPv6Addresses", "false");
        System.setProperty("java.net.preferIPv6Addresses", "true");

        InetAddress address = NetUtils.getLocalAddress();
        boolean isPreferIPV6Address = NetUtils.isPreferIPV6Address();

        // Restore system property to previous value before executing test
        System.setProperty("java.net.preferIPv6Addresses", saved);

        assumeTrue(address instanceof Inet6Address);
        assertThat(isPreferIPV6Address, equalTo(true));
    }

    @Test
    void testLocalHost() {
        assertEquals(NetUtils.getLocalHost(), NetUtils.getLocalAddress().getHostAddress());
        assertTrue(NetUtils.isValidLocalHost(NetUtils.getLocalHost()));
        assertFalse(NetUtils.isInvalidLocalHost(NetUtils.getLocalHost()));
    }

    @Test
    void testFindNetworkInterface() {
        assertNotNull(NetUtils.findNetworkInterface());
    }

    @Test
    void testIgnoreAllInterfaces() {
        // store the origin ignored interfaces
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        try {
            // ignore all interfaces
            this.setIgnoredInterfaces(".*");
            assertNull(NetUtils.findNetworkInterface());
        } finally {
            // recover the origin ignored interfaces
            this.setIgnoredInterfaces(originIgnoredInterfaces);
        }
    }

    @Test
    void testIgnoreGivenInterface() {
        // store the origin ignored interfaces
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        try {
            NetworkInterface networkInterface = NetUtils.findNetworkInterface();
            assertNotNull(networkInterface);
            // ignore the given network interface's display name
            this.setIgnoredInterfaces(Pattern.quote(networkInterface.getDisplayName()));
            NetworkInterface newNetworkInterface = NetUtils.findNetworkInterface();
            if (newNetworkInterface != null) {
                assertTrue(!networkInterface.getDisplayName().equals(newNetworkInterface.getDisplayName()));
            }
        } finally {
            // recover the origin ignored interfaces
            this.setIgnoredInterfaces(originIgnoredInterfaces);
        }
    }

    @Test
    void testIgnoreGivenPrefixInterfaceName() {
        // store the origin ignored interfaces
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        try {
            NetworkInterface networkInterface = NetUtils.findNetworkInterface();
            assertNotNull(networkInterface);
            // ignore the given prefix network interface's display name
            String displayName = networkInterface.getDisplayName();
            if (StringUtils.isNotEmpty(displayName) && displayName.length() > 2) {
                String ignoredInterfaces = Pattern.quote(displayName.substring(0, 1)) + ".*";
                this.setIgnoredInterfaces(ignoredInterfaces);
                NetworkInterface newNetworkInterface = NetUtils.findNetworkInterface();
                if (newNetworkInterface != null) {
                    assertTrue(!newNetworkInterface.getDisplayName().startsWith(displayName.substring(0, 1)));
                }
            }
        } finally {
            // recover the origin ignored interfaces
            this.setIgnoredInterfaces(originIgnoredInterfaces);
        }
    }

    private String getIgnoredInterfaces() {
        return System.getProperty("distributed.scheduler.network.interface.ignored");
    }

    private void setIgnoredInterfaces(String ignoredInterfaces) {
        if (ignoredInterfaces != null) {
            System.setProperty("distributed.scheduler.network.interface.ignored", ignoredInterfaces);
        } else {
            System.setProperty("distributed.scheduler.network.interface.ignored", "");
        }
    }

}
