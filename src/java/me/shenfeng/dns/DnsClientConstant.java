package me.shenfeng.dns;

public interface DnsClientConstant {
    static byte[] FLAGS_PARAMS = new byte[] { 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 };
    static byte[] A_IN = new byte[] { 0, 1, 0, 1 };
    static final String DNS_UNKOWN_HOST = "unknown";
    static final String DNS_TIMEOUT = "timeout";
    static final int TIMEOUT = 6000;
    static final int CHECK_INTERVAL = 2000;
    static final String TIMER_NAME = "DNS-timer";
}
