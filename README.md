An Async HTTP Client Based on Netty

I write this for my personal part time project
[RSSMiner](http://rssminer.net), for the web crawler and feed fetcher module.


Features
--------

1. Asynchronous
2. Minimum: just download webpages from Internet efficently.
3. Support SOCKS5, HTTP proxy
4. HTTPS(trust all)
5. [Configurable](https://github.com/shenfeng/netty-http/blob/master/src/java/me/shenfeng/http/HttpClientConfig.java)
6. [DNS prefetch](https://github.com/shenfeng/netty-http/blob/master/src/java/me/shenfeng/dns/DnsPrefecher.java),
IPV4 only.


Non-features
------------
1. All Content are buffered in memory as byte array=> can not handle
large file, anyway, It's meant to download webpages(zipped if server
support)


Example
-------

```java
  // Http client sample usage
   HttpClientConfig config = new HttpClientConfig();
   header = new HashMap<String, Object>();
   HttpClient client = new HttpClient(config);
   URI uri = new URI("http://onycloud.com");
   final HttpResponseFuture future = client.execGet(uri, header);
   resp.addListener(new Runnable() {
       public void run() {
           HttpResponse resp = future.get(); // async
       }
   });
   HttpResponse resp = future.get(); // blocking
```

