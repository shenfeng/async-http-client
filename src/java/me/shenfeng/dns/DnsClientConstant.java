package me.shenfeng.dns;

public interface DnsClientConstant {
    static byte[] FLAGS_PARAMS = new byte[] { 1,// message is standard query,
                                                // not truncated, do recursively
            0, // accept non-authenticated data
            0, 1, // 1 question
            0, 0, // 0 answer RRs
            0, 0, // 0 authority RRs
            0, 0 // 0 additional RRs
    };
    static byte[] A_IN = new byte[] { 0, 1, // type A(host address)
            0, 1 // class IN
    };
    static final String DNS_UNKOWN_HOST = "unknown";
    static final String DNS_TIMEOUT = "timeout";
    static final String TIMER_NAME = "DNS-timer";
}
