package me.shenfeng;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.util.CharsetUtil.UTF_8;

import java.net.URI;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class Utils {

    public static byte[] toBytes(int i) {
        return new byte[] { (byte) (i >> 8), (byte) (i & 0x00ff) };
    }

    public static int toInt(byte[] bytes) {
        return toInt(bytes, 0);
    }

    public static int toInt(byte[] bytes, int start) {
        return (toInt(bytes[start]) << 8) + toInt(bytes[start + 1]);
    }

    public static int toInt(int b) {
        if (b < 0)
            b += 256;
        return b;
    }

    public static boolean isIP(String host) {
        for (int i = 0; i < host.length(); ++i) {
            if (!(Character.isDigit(host.charAt(i)) || host.charAt(i) == '.')) {
                return false;
            }
        }
        return true;
    }

    private static final String CS = "charset=";

    public static String getPath(URI uri) {
        String path = uri.getPath();
        String query = uri.getRawQuery();
        if ("".equals(path))
            path = "/";
        if (query == null)
            return path;
        else
            return path + "?" + query;
    }

    public static int getPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            port = 80;
        }
        return port;
    }

    public static Charset parseCharset(String type) {
        try {
            if (type != null) {
                type = type.toLowerCase();
                int i = type.indexOf(CS);
                if (i != -1) {
                    String charset = type.substring(i + CS.length()).trim();
                    return Charset.forName(charset);
                }
            }
        } catch (Exception ignore) {
        }
        return UTF_8;
    }

    public static String bodyString(HttpResponse response) {
        String type = response.getHeader(CONTENT_TYPE);
        ChannelBuffer buffer = response.getContent();
        return new String(buffer.array(), 0, buffer.readableBytes(),
                parseCharset(type));
    }

}
