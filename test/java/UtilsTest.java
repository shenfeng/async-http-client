import static org.jboss.netty.util.CharsetUtil.UTF_8;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import me.shenfeng.Utils;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void testGetPath() throws URISyntaxException {
        URI uri = new URI("http://shenfeng.me?a=b");
        String path = Utils.getPath(uri);
        Assert.assertTrue("/?a=b".equals(path));
        uri = new URI(
                "http://www.baidu.com/s?wd=%D5%AC%BC%B1%CB%CD&rsv_bp=0&inputT=3664");
        Assert.assertNotSame("should equal",
                "s?wd=%D5%AC%BC%B1%CB%CD&rsv_bp=0&inputT=3664",
                Utils.getPath(uri));
    }

    @Test
    public void testParseCharset() {
        Assert.assertEquals("default utf8", UTF_8, Utils.parseCharset(null));
        Assert.assertEquals("parse gbk", Charset.forName("gbk"),
                Utils.parseCharset("text/html;charset=gbk"));
        Assert.assertEquals("parse gb2312", Charset.forName("gb2312"),
                Utils.parseCharset("text/html;charset=gb2312"));
    }

    @Test
    public void testGetPort() throws URISyntaxException {
        Assert.assertEquals(80, Utils.getPort(new URI("http://google.com")));
        Assert.assertEquals(443, Utils.getPort(new URI("https://google.com")));
    }

    @Test
    public void testBytes() {

        for (int i = 0; i < 256; ++i) {
            System.out.println(i + "\t" + (byte) i);
        }

        int[] numbs = new int[] { 0x00000f0f, 0x0000ffff };
        byte[][] bytes = new byte[][] { new byte[] { 15, 15 },
                new byte[] { -1, -1 } };

        for (int i = 0; i < numbs.length; ++i) {
            int num = numbs[i];
            byte[] byts = bytes[i];
            Assert.assertArrayEquals(Utils.toBytes(num), byts);
            Assert.assertEquals(num, Utils.toInt(byts));
        }
    }

    @Test
    public void testIsIP() {
        Assert.assertTrue(Utils.isIP("12.1.1.1"));
        Assert.assertTrue(!Utils.isIP("shenfeng.me"));

    }

}
