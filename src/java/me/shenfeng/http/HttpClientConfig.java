package me.shenfeng.http;

public class HttpClientConfig {

    protected int requestTimeoutInMs = 10000;
    protected int workerThread = 1;
    protected int connectionTimeOutInMs = 4500;
    protected String bossNamePrefix = "Http Boss";
    protected String workerNamePrefix = "Http Worker";
    protected String userAgent = "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)";

    public HttpClientConfig(int requestTimeoutInMs, int workerThread,
            int connectionTimeOutInMs, String bossNamePrefix,
            String workerNamePrefix, String userAgent) {
        this.requestTimeoutInMs = requestTimeoutInMs;
        this.workerThread = workerThread;
        this.connectionTimeOutInMs = connectionTimeOutInMs;
        this.bossNamePrefix = bossNamePrefix;
        this.workerNamePrefix = workerNamePrefix;
        this.userAgent = userAgent;
    }

    public HttpClientConfig() {

    }

    public void setRequestTimeoutInMs(int requestTimeoutInMs) {
        this.requestTimeoutInMs = requestTimeoutInMs;
    }

    public void setWorkerThread(int workerThread) {
        this.workerThread = workerThread;
    }

    public void setConnectionTimeOutInMs(int connectionTimeOutInMs) {
        this.connectionTimeOutInMs = connectionTimeOutInMs;
    }

    public void setBossNamePrefix(String bossNamePrefix) {
        this.bossNamePrefix = bossNamePrefix;
    }

    public void setWorkerNamePrefix(String workerNamePrefix) {
        this.workerNamePrefix = workerNamePrefix;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
