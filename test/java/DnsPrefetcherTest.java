import java.io.IOException;

import me.shenfeng.dns.DnsPrefecher;

import org.junit.Test;

public class DnsPrefetcherTest {

    @Test
    public void testPrefetch() throws IOException {
        DnsPrefecher prefecher = DnsPrefecher.getInstance();
        prefecher.prefetch("google.com");
    }
}
