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
package cn.ponfee.disjob.common.util;


import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * NetUtils Test
 */
public class NetUtilsTest {

    @Test
    void testGetAvailablePort() {
        assertThat(NetUtils.findAvailablePort()).isGreaterThan(0);
        assertThat(NetUtils.findAvailablePort(12345)).isGreaterThanOrEqualTo(12345);
        assertThat(NetUtils.findAvailablePort(-1)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testIsAvailablePort() {
        assertFalse(NetUtils.isAvailablePort(0));
        assertFalse(NetUtils.isAvailablePort(65536));
        assertTrue(NetUtils.isAvailablePort(1024));
    }

    @Test
    void testIsAnyHost() {
        assertTrue(NetUtils.isAnyHost("0.0.0.0"));
        assertFalse(NetUtils.isAnyHost("1.1.1.1"));
    }

    @Test
    void testIsInvalidLocalHost() {
        assertFalse(NetUtils.isValidLocalHost(null));
        assertFalse(NetUtils.isValidLocalHost(""));
        assertFalse(NetUtils.isValidLocalHost("localhost"));
        assertFalse(NetUtils.isValidLocalHost("0.0.0.0"));
        assertFalse(NetUtils.isValidLocalHost("127.1.2.3"));
        assertFalse(NetUtils.isValidLocalHost("127.0.0.1"));
        assertTrue(NetUtils.isValidLocalHost("128.0.0.1"));
    }

    @Test
    void testIsValidLocalHost() {
        assertTrue(NetUtils.isValidLocalHost("1.2.3.4"));
        assertTrue(NetUtils.isValidLocalHost("128.0.0.1"));
    }

    @Test
    @Disabled
    void testIsConnectable() {
        assertTrue(NetUtils.isConnectableHostPort("www.baidu.com", 80, 100));
        assertFalse(NetUtils.isConnectableHostPort("www.unknownhostname.com", 80, 100));
    }

    @Test
    void testIsValidAddress() {
        assertFalse(NetUtils.isValidIpv4Address(null));

        InetAddress address1 = mock(InetAddress.class);
        when(address1.isLoopbackAddress()).thenReturn(true);
        assertFalse(NetUtils.isValidIpv4Address(address1));

        InetAddress address2 = mock(InetAddress.class);
        when(address2.getHostAddress()).thenReturn("localhost");
        assertFalse(NetUtils.isValidIpv4Address(address2));

        InetAddress address3 = mock(InetAddress.class);
        when(address3.getHostAddress()).thenReturn("0.0.0.0");
        assertFalse(NetUtils.isValidIpv4Address(address3));

        InetAddress address4 = mock(InetAddress.class);
        when(address4.getHostAddress()).thenReturn("127.0.0.1");
        assertFalse(NetUtils.isValidIpv4Address(address4));

        InetAddress address5 = mock(InetAddress.class);
        when(address5.getHostAddress()).thenReturn("1.2.3.4");
        assertTrue(NetUtils.isValidIpv4Address(address5));
    }

    @Test
    void testGetLocalHost() {
        String localHost = NetUtils.getLocalHost();
        System.out.println("Local host: " + localHost);
        assertNotNull(localHost);
    }

    @Test
    void testGetHostName() {
        assertNotNull(NetUtils.getHostName("127.0.0.1"));
    }

    @Test
    void testGetIpByHost() {
        assertEquals(NetUtils.getHostAddress("localhost"), "127.0.0.1");
        assertEquals(NetUtils.getHostAddress("127.0.0.1"), "127.0.0.1");
        assertEquals(NetUtils.getHostAddress("job.local"), "job.local");
        assertEquals(NetUtils.getHostName("www.baidu.com"), "www.baidu.com");
        assertTrue(NetUtils.isValidIpAddress("14.215.177.38"));
    }

    @Disabled
    @Test
    void testLocalHost() {
        assertTrue(NetUtils.isValidLocalHost(NetUtils.getLocalHost()));
    }

    @Test
    void testFindNetworkInterface() {
        assertNotNull(NetUtils.findValidNetworkInterface());
    }

    @Test
    void testIgnoreAllInterfaces() {
        // store the origin ignored interfaces
        String originIgnoredInterfaces = this.getIgnoredInterfaces();
        try {
            // ignore all interfaces
            this.setIgnoredInterfaces(".*");
            assertNull(NetUtils.findValidNetworkInterface());
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
            NetworkInterface networkInterface = NetUtils.findValidNetworkInterface();
            assertNotNull(networkInterface);
            // ignore the given network interface's display name
            this.setIgnoredInterfaces(Pattern.quote(networkInterface.getDisplayName()));
            NetworkInterface newNetworkInterface = NetUtils.findValidNetworkInterface();
            if (newNetworkInterface != null) {
                assertNotEquals(networkInterface.getDisplayName(), newNetworkInterface.getDisplayName());
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
            NetworkInterface networkInterface = NetUtils.findValidNetworkInterface();
            assertNotNull(networkInterface);
            // ignore the given prefix network interface's display name
            String displayName = networkInterface.getDisplayName();
            if (StringUtils.isNotEmpty(displayName) && displayName.length() > 2) {
                String ignoredInterfaces = Pattern.quote(displayName.substring(0, 1)) + ".*";
                this.setIgnoredInterfaces(ignoredInterfaces);
                NetworkInterface newNetworkInterface = NetUtils.findValidNetworkInterface();
                if (newNetworkInterface != null) {
                    assertFalse(newNetworkInterface.getDisplayName().startsWith(displayName.substring(0, 1)));
                }
            }
        } finally {
            // recover the origin ignored interfaces
            this.setIgnoredInterfaces(originIgnoredInterfaces);
        }
    }

    private String getIgnoredInterfaces() {
        return System.getProperty(NetUtils.IGNORED_NETWORK_INTERFACE);
    }

    private void setIgnoredInterfaces(String ignoredInterfaces) {
        if (ignoredInterfaces != null) {
            System.setProperty(NetUtils.IGNORED_NETWORK_INTERFACE, ignoredInterfaces);
        } else {
            System.setProperty(NetUtils.IGNORED_NETWORK_INTERFACE, "");
        }
    }

}
