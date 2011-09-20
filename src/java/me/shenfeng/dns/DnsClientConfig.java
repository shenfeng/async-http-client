package me.shenfeng.dns;

public class DnsClientConfig {
    protected int dnsTimeout = 3000;
    protected int timerInterval = 1500;

    public DnsClientConfig() {

    }

    public DnsClientConfig(int dnsTimeout, int timerInterval) {
        this.dnsTimeout = dnsTimeout;
        this.timerInterval = timerInterval;
    }

    public void setDnsTimeout(int dnsTimeout) {
        this.dnsTimeout = dnsTimeout;
    }

    public void setTimerInterval(int timerInterval) {
        this.timerInterval = timerInterval;
    }
}
