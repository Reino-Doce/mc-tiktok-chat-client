package br.com.reinodoce.mctiktok.client.overlay;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;

final class InlineMediaAddressPolicy {
    private static final int OCTET_ANY_LOCAL = 0;
    private static final int OCTET_PRIVATE_10 = 10;
    private static final int OCTET_LOOPBACK = 127;
    private static final int OCTET_CARRIER_GRADE_NAT = 100;
    private static final int OCTET_CARRIER_GRADE_NAT_MIN = 64;
    private static final int OCTET_CARRIER_GRADE_NAT_MAX = 127;
    private static final int OCTET_LINK_LOCAL = 169;
    private static final int OCTET_LINK_LOCAL_SECOND = 254;
    private static final int OCTET_PRIVATE_172 = 172;
    private static final int OCTET_PRIVATE_172_MIN = 16;
    private static final int OCTET_PRIVATE_172_MAX = 31;
    private static final int OCTET_PRIVATE_192 = 192;
    private static final int OCTET_PRIVATE_192_SECOND = 168;
    private static final int OCTET_BENCHMARK = 198;
    private static final int OCTET_BENCHMARK_MIN = 18;
    private static final int OCTET_BENCHMARK_MAX = 19;
    private static final int OCTET_TEST_NET_2_SECOND = 51;
    private static final int OCTET_TEST_NET_2_THIRD = 100;
    private static final int OCTET_TEST_NET_3 = 203;
    private static final int OCTET_TEST_NET_SECOND = 0;
    private static final int OCTET_TEST_NET_THIRD = 2;
    private static final int OCTET_TEST_NET_3_THIRD = 113;
    private static final int OCTET_MULTICAST_MIN = 224;
    private static final int OCTET_MAX = 255;
    private static final int IPV6_UNIQUE_LOCAL_MASK = 0xfe;
    private static final int IPV6_UNIQUE_LOCAL_PREFIX = 0xfc;

    private static final List<Ipv4Range> BLOCKED_IPV4_RANGES = List.of(
            Ipv4Range.firstOctet(OCTET_ANY_LOCAL),
            Ipv4Range.firstOctet(OCTET_PRIVATE_10),
            Ipv4Range.firstOctet(OCTET_LOOPBACK),
            Ipv4Range.secondOctetRange(
                    OCTET_CARRIER_GRADE_NAT,
                    OCTET_CARRIER_GRADE_NAT_MIN,
                    OCTET_CARRIER_GRADE_NAT_MAX),
            Ipv4Range.secondOctetRange(OCTET_LINK_LOCAL, OCTET_LINK_LOCAL_SECOND, OCTET_LINK_LOCAL_SECOND),
            Ipv4Range.secondOctetRange(OCTET_PRIVATE_172, OCTET_PRIVATE_172_MIN, OCTET_PRIVATE_172_MAX),
            Ipv4Range.prefix24(OCTET_PRIVATE_192, OCTET_TEST_NET_SECOND, OCTET_TEST_NET_SECOND),
            Ipv4Range.secondOctetRange(OCTET_PRIVATE_192, OCTET_PRIVATE_192_SECOND, OCTET_PRIVATE_192_SECOND),
            Ipv4Range.secondOctetRange(OCTET_BENCHMARK, OCTET_BENCHMARK_MIN, OCTET_BENCHMARK_MAX),
            Ipv4Range.prefix24(OCTET_BENCHMARK, OCTET_TEST_NET_2_SECOND, OCTET_TEST_NET_2_THIRD),
            Ipv4Range.prefix24(OCTET_TEST_NET_3, OCTET_TEST_NET_SECOND, OCTET_TEST_NET_3_THIRD),
            Ipv4Range.prefix24(OCTET_PRIVATE_192, OCTET_TEST_NET_SECOND, OCTET_TEST_NET_THIRD));

    private InlineMediaAddressPolicy() {
    }

    static void validatePublicRemote(URL url) throws IOException {
        for (InetAddress address : InetAddress.getAllByName(url.getHost())) {
            if (!isPublicAddress(address)) {
                throw new IOException("Non-public inline media host rejected for " + redactedUrl(url));
            }
        }
    }

    private static boolean isPublicAddress(InetAddress address) {
        boolean blocked = isBlockedByAddressType(address);
        if (address instanceof Inet4Address inet4Address) {
            blocked = blocked || isBlockedIpv4(inet4Address.getAddress());
        } else if (address instanceof Inet6Address inet6Address) {
            blocked = blocked || isUniqueLocalIpv6(inet6Address.getAddress());
        } else {
            blocked = true;
        }
        return !blocked;
    }

    private static boolean isBlockedByAddressType(InetAddress address) {
        boolean blocked = address.isAnyLocalAddress();
        blocked = blocked || address.isLoopbackAddress();
        blocked = blocked || address.isLinkLocalAddress();
        blocked = blocked || address.isSiteLocalAddress();
        blocked = blocked || address.isMulticastAddress();
        return blocked;
    }

    private static boolean isBlockedIpv4(byte[] address) {
        int first = Byte.toUnsignedInt(address[0]);
        int second = Byte.toUnsignedInt(address[1]);
        int third = Byte.toUnsignedInt(address[2]);
        if (first >= OCTET_MULTICAST_MIN) {
            return true;
        }
        for (Ipv4Range range : BLOCKED_IPV4_RANGES) {
            if (range.matches(first, second, third)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUniqueLocalIpv6(byte[] address) {
        int first = Byte.toUnsignedInt(address[0]);
        return (first & IPV6_UNIQUE_LOCAL_MASK) == IPV6_UNIQUE_LOCAL_PREFIX;
    }

    private static String redactedUrl(URL url) {
        return url.getProtocol() + "://" + url.getHost();
    }

    private record Ipv4Range(
            int firstOctet,
            int secondOctetMin,
            int secondOctetMax,
            int thirdOctetMin,
            int thirdOctetMax
    ) {
        static Ipv4Range firstOctet(int firstOctet) {
            return new Ipv4Range(
                    firstOctet,
                    OCTET_TEST_NET_SECOND,
                    OCTET_MAX,
                    OCTET_TEST_NET_SECOND,
                    OCTET_MAX);
        }

        static Ipv4Range secondOctetRange(int firstOctet, int secondOctetMin, int secondOctetMax) {
            return new Ipv4Range(
                    firstOctet,
                    secondOctetMin,
                    secondOctetMax,
                    OCTET_TEST_NET_SECOND,
                    OCTET_MAX);
        }

        static Ipv4Range prefix24(int firstOctet, int secondOctet, int thirdOctet) {
            return new Ipv4Range(firstOctet, secondOctet, secondOctet, thirdOctet, thirdOctet);
        }

        boolean matches(int first, int second, int third) {
            return first == firstOctet && matchesSecondOctet(second) && matchesThirdOctet(third);
        }

        private boolean matchesSecondOctet(int second) {
            return second >= secondOctetMin && second <= secondOctetMax;
        }

        private boolean matchesThirdOctet(int third) {
            return third >= thirdOctetMin && third <= thirdOctetMax;
        }
    }
}
