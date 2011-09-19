import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    public void testCname() throws InterruptedException, ExecutionException {
        DnsResponseFuture future = client.resolve("www.ogidc.com");
        String ip = future.get();

        System.out.println(ip);// 112.126.149.103
    }

    @Test
    public void testHomeMadeDnsResolver() throws InterruptedException,
            ExecutionException {
        String[] hosts = new String[] { "shenfeng.me", "onycloud.com",
                "trakrapp.com", "rssminer.net" };
        List<DnsResponseFuture> list = new ArrayList<DnsResponseFuture>();
        for (String host : hosts) {
            final DnsResponseFuture f = client.resolve(host);
            f.addListener(new Runnable() {
                public void run() {
                    try {
                        System.out.println("aaa\t" + f.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            list.add(f);
            // System.out.println(ip);
        }
        for (DnsResponseFuture f : list) {
            f.get();
        }
    }
}
