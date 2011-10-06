import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.Proxy.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import me.shenfeng.Utils;
import me.shenfeng.http.HttpClient;
import me.shenfeng.http.HttpClientConfig;
import me.shenfeng.http.HttpResponseFuture;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

public class HttpClientTest {

    HttpClient client;
    Proxy proxy;
    Map<String, Object> header;
    String urls[] = new String[] { "http://192.168.1.1:8088/",
            "http://192.168.1.1:8088/c.html",
            "http://192.168.1.1:8088/qunar.html",
            "http://192.168.1.1:8088/CHANGES", };

    @Before
    public void setup() {
        HttpClientConfig config = new HttpClientConfig();
        header = new HashMap<String, Object>();
        client = new HttpClient(config);
        proxy = new Proxy(Type.HTTP, new InetSocketAddress("127.0.0.1", 3128));
    }

    @Test
    public void testProxy() throws Exception {
        final URI uri = new URI("http://www.facebook.com");
        final HttpResponseFuture resp = client.execGet(uri, header, proxy);
        HttpResponse r = resp.get();
        System.out.println(r);
    }

    @Test
    public void testNoProxy() throws Exception {
        final URI uri = new URI("http://shenfeng.me");
        final HttpResponseFuture resp = client.execGet(uri, header);
        HttpResponse r = resp.get();
        String s = Utils.bodyStr(r);
        System.out.println(s);
    }

    @Test
    public void testPotentialError() throws URISyntaxException,
            InterruptedException, ExecutionException {
        for (int i = 0; i < 1; ++i) {
            final URI uri = new URI("http://shenfeng.me");
            final HttpResponseFuture get = client.execGet(uri, header);
            get.addListener(new Runnable() {
                public void run() {
                    try {
                        HttpResponse resp = get.get();
                        System.out.println(resp);
                        // System.out.println(Utils.bodyString(resp));
                    } catch (Exception e) {
                    }
                }
            });
            get.get();
        }
        // Thread.sleep(210000);
    }

    @Test
    public void testConnectionRefused() throws URISyntaxException,
            InterruptedException, ExecutionException {
        HttpResponseFuture resp = client.execGet(new URI("http://127.0.0.1"),
                header);

        resp.get();

        // Thread.sleep(10000000);
    }

    @Test
    public void testGetBigFile() throws URISyntaxException,
            InterruptedException, ExecutionException {

        HttpResponseFuture resp = client.execGet(new URI(
                "http://192.168.1.1/videos/AdbeRdr940_zh_CN.exe"), header);

        resp.get();
        // Thread.sleep(10000000);
    }

    @Test
    public void testAsyncGet() throws URISyntaxException,
            InterruptedException, ExecutionException {
        final Semaphore semaphore = new Semaphore(200);
        String[] urls = new String[] { "/", "/browse", "/css/browse.css",
                "/js/lib/jquery.js" };

        while (true) {
            for (String url : urls) {
                final HttpResponseFuture resp = client.execGet(new URI(
                        "http://127.0.0.1:8100" + url), header);
                resp.addListener(new Runnable() {
                    public void run() {
                        try {
                            // System.out.println(resp.get().getStatus());
                            semaphore.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                semaphore.acquire();
            }
        }
    }

    @Test
    public void testAsyncGet2() throws URISyntaxException,
            InterruptedException, ExecutionException {
        String urls[] = new String[] {
                "http://news.sohu.com/upload/cs/cswb001/beida0909/index.html",
                "http://news.sohu.com/photo/", "http://baike.baidu.com/",
                "http://tieba.baidu.com/tb/index/v2/niuba.html",
                "http://web.qq.com/", "http://im.qq.com/webqq/",
                "http://news.sina.com.cn/society/", "http://www.baidu.com/" };
        for (int i = 0; i < 10; ++i) {
            for (final String url : urls) {
                final HttpResponseFuture future = client.execGet(
                        new URI(url), header);
                future.addListener(new Runnable() {
                    public void run() {
                        try {
                            HttpResponse r = future.get();
                            // if (r.getStatus().getCode() == 200)
                            System.out.println(r.getStatus() + "\t"
                                    + r.getContentLength() + "\t" + url);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        Thread.sleep(10000);
    }

    public void testHttps() throws URISyntaxException, InterruptedException,
            ExecutionException {
        URI uri = new URI("https://trakrapp.com");

        HttpResponseFuture future = client.execGet(uri, header);
        future.get();
        Thread.sleep(1000000);
        // future.abort(t)
    }

    @Test
    public void testCRCerror() throws URISyntaxException,
            InterruptedException, ExecutionException {
        HttpResponseFuture f = client.execGet(new URI(
                "http://www.allshizuo.com/index.php"), header);
        HttpResponse resp = f.get();
        String string = Utils.bodyStr(resp);
        System.out.println(string);
    }

    @Test
    public void testAsyncLoop() throws InterruptedException,
            ExecutionException, URISyntaxException {
        final Semaphore semaphore = new Semaphore(10);

        while (true) {
            for (String s : urls) {
                URI uri = new URI(s);
                HttpResponseFuture resp = client.execGet(uri, header);
                resp.addListener(new Runnable() {
                    public void run() {
                        // System.out.println("----------");
                        semaphore.release();
                    }
                });
                semaphore.acquire();
            }
        }
    }
}
