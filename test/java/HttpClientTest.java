import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import me.shenfeng.http.HttpClient;
import me.shenfeng.http.HttpClientConfig;
import me.shenfeng.http.HttpResponseFuture;

import org.junit.Before;
import org.junit.Test;

public class HttpClientTest {

    HttpClient client;
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
    }

    @Test
    public void testPotentialError() throws URISyntaxException,
            InterruptedException, ExecutionException {
        URI uri = new URI("http://tianlongbabu3renjiebaimingcheng.cnjsj.net/");
        HttpResponseFuture get = client.execGet(uri, header);
        // get.operationComplete(new Listener<HttpResponse>() {
        // public void run(HttpResponse result) {
        // System.out.println(result);
        // }
        // });
        get.get();
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
        HttpResponseFuture future = client.execGet(
                new URI("http://baidu.com"), header);
        // future.operationComplete(new Listener<HttpResponse>() {
        // public void run(HttpResponse result) {
        // System.out.println(result);
        // }
        // });
        for (int i = 0; i < 10; i++) {
            future.isTimeout();
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
