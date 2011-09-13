import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import me.shenfeng.ListenableFuture;
import me.shenfeng.ListenableFuture.Listener;
import me.shenfeng.http.HttpClient;
import me.shenfeng.http.HttpClientConfig;
import me.shenfeng.http.HttpResponseFuture;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

public class HttpClientTest {

    HttpClient client;

    Map<String, Object> header = new HashMap<String, Object>();

    @Before
    public void setup() {
        HttpClientConfig config = new HttpClientConfig();
        client = new HttpClient(config);
    }

    @Test
    public void testPotentialError() throws URISyntaxException,
            InterruptedException, ExecutionException {
        URI uri = new URI("http://tianlongbabu3renjiebaimingcheng.cnjsj.net/");
        HttpResponseFuture get = client.execGet(uri, header);
        get.addistener(new Listener<HttpResponse>() {
            public void run(ListenableFuture<HttpResponse> f,
                    HttpResponse result) {
                System.out.println(result);
            }
        });
        get.get();
    }

    String str[] = new String[] { "http://192.168.1.1:8088/",
            "http://192.168.1.1:8088/c.html",
            "http://192.168.1.1:8088/qunar.html",
            "http://192.168.1.1:8088/CHANGES", };

    @Test
    public void testConnectionRefused() throws URISyntaxException,
            InterruptedException, ExecutionException {
        Map<String, Object> header = new HashMap<String, Object>();
        HttpClient client = new HttpClient(new HttpClientConfig());
        HttpResponseFuture resp = client.execGet(new URI("http://127.0.0.1"),
                header);

        resp.get();

        // Thread.sleep(10000000);
    }

    @Test
    public void testGetBigFile() throws URISyntaxException,
            InterruptedException, ExecutionException {
        Map<String, Object> header = new HashMap<String, Object>();
        HttpClient client = new HttpClient(new HttpClientConfig());
        HttpResponseFuture resp = client.execGet(new URI(
                "http://192.168.1.1/videos/AdbeRdr940_zh_CN.exe"), header);

        resp.get();
        // Thread.sleep(10000000);
    }

    @Test
    public void testAsyncGet() throws URISyntaxException,
            InterruptedException, ExecutionException {
        HttpResponseFuture resp = client.execGet(new URI(
                "http://192.168.1.1:8088/CHANGES"), header);

        resp.get();
        Thread.sleep(1000000);
    }

    @Test
    public void testAsyncGet2() throws URISyntaxException,
            InterruptedException, ExecutionException {
        HttpResponseFuture future = client.execGet(new URI(
                "http://baidu.com"), header);
        future.addistener(new Listener<HttpResponse>() {
            public void run(ListenableFuture<HttpResponse> f,
                    HttpResponse result) {
                System.out.println(result);
            }
        });
        for (int i = 0; i < 10; i++) {
            future.checkTimeout(null);
        }

        future.get();
        System.out.println("aaa");
        // Thread.sleep(1000000);
    }

    public void testHttps() throws URISyntaxException, InterruptedException,
            ExecutionException {
        URI uri = new URI("https://trakrapp.com");

        HttpResponseFuture future = client.execGet(uri, header);
        future.get();

        // future.abort(t)
    }

    @Test
    public void testAsyncLoop() throws InterruptedException,
            ExecutionException, URISyntaxException {
        final Semaphore semaphore = new Semaphore(10);

        while (true) {
            for (String s : str) {
                URI uri = new URI(s);
                HttpResponseFuture resp = client.execGet(uri, header);
                resp.addistener(new Listener<HttpResponse>() {
                    public void run(ListenableFuture<HttpResponse> f,
                            HttpResponse result) {
                        // System.out.println("----------");
                        semaphore.release();
                    }
                });
                semaphore.acquire();
            }
        }
    }
}
