package me.shenfeng.http;


public class HttpClientConfig {

    protected String bossNamePrefix = "Http Boss";
    protected int connectionTimeOutInMs = 4500;
    protected int receiveBuffer = 16384;
    protected int requestTimeoutInMs = 20000;
    protected int sendBuffer = 2048;
    protected String userAgent = "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)";
    protected String workerNamePrefix = "Http Worker";
    protected int workerThread = 1;

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
    public HttpClientConfig (){}

    public void setBossNamePrefix(String bossNamePrefix) {
        this.bossNamePrefix = bossNamePrefix;
    }

    public void setConnectionTimeOutInMs(int connectionTimeOutInMs) {
        this.connectionTimeOutInMs = connectionTimeOutInMs;
    }

    public void setReceiveBuffer(int receiveBuffer) {
        this.receiveBuffer = receiveBuffer;
    }

    public void setRequestTimeoutInMs(int requestTimeoutInMs) {
        this.requestTimeoutInMs = requestTimeoutInMs;
    }

    public void setSendBuffer(int sendBuffer) {
        this.sendBuffer = sendBuffer;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setWorkerNamePrefix(String workerNamePrefix) {
        this.workerNamePrefix = workerNamePrefix;
    }

    public void setWorkerThread(int workerThread) {
        this.workerThread = workerThread;
    }
}
