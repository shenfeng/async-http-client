An Async HTTP Client Based on [Netty](http://netty.io/)

I write this for my personal part time project
[RSSMiner](http://rssminer.net), for the web crawler and feed fetcher module.


Features
--------

* Asynchronous
* Minimum: just download webpages from Internet efficently.
* Support SOCKS v5, HTTP proxy
* HTTPS(trust all)
* [Configurable](https://github.com/shenfeng/async-http-client/blob/master/src/java/me/shenfeng/http/HttpClientConfig.java)
* [DNS prefetch](https://github.com/shenfeng/async-http-client/blob/master/src/java/me/shenfeng/dns/DnsPrefecher.java),


Limitations:
------------
* All Content are buffered in memory as byte array=> can not handle
large file. Anyway, it's meant to download webpages(zipped if server
support)
* Dns prefetch is IPV4 only.

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

