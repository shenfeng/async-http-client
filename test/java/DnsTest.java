import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import me.shenfeng.ListenableFuture;
import me.shenfeng.ListenableFuture.Listener;
import me.shenfeng.dns.DnsClient;
import me.shenfeng.dns.DnsResponseFuture;

import org.junit.Before;
import org.junit.Test;

public class DnsTest {

    private DnsClient client;

    @Before
    public void setup() {
        client = new DnsClient();
    }

    @Test
    public void testDns() throws UnknownHostException {
        for (int i = 0; i < 10; ++i) {
            // InetAddress a = new Inet
            // InetSocketAddress a = new InetSocketAddress("shenfeng.me", 80);
            InetAddress[] a = InetAddress.getAllByName("shenfeng.me");
            for (InetAddress inetAddress : a) {
                System.out.println(inetAddress);
            }

            // System.out.println(a);
            // a.getAddress()
            // InetAddress d = a.getAddress();
            // System.out.println(d);
        }
    }

    @Test
    public void testHomeMadeDnsResolver() throws InterruptedException,
            ExecutionException {
        final DnsResponseFuture f = client.resolve("shenfeng.me");
        f.addistener(new Listener<String>() {
            @Override
            public void run(ListenableFuture<String> f, String result) {
                try {
                    System.out.println("aaa\t" + result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        String ip = f.get();
        System.out.println(ip);
    }
}
