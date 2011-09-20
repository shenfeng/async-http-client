package me.shenfeng;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import static org.jboss.netty.handler.codec.compression.ZlibWrapper.GZIP;
import static org.jboss.netty.handler.codec.compression.ZlibWrapper.ZLIB_OR_NONE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.util.CharsetUtil.UTF_8;

import java.net.URI;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
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

    public static String bodyString(HttpResponse m) {
        String type = m.getHeader(CONTENT_TYPE);
        String contentEncoding = m.getHeader(Names.CONTENT_ENCODING);
        DecoderEmbedder<ChannelBuffer> decoder = null;
        if ("gzip".equalsIgnoreCase(contentEncoding)
                || "x-gzip".equalsIgnoreCase(contentEncoding)) {
            decoder = new DecoderEmbedder<ChannelBuffer>(
                    new ZlibDecoder(GZIP));
        } else if ("deflate".equalsIgnoreCase(contentEncoding)
                || "x-deflate".equalsIgnoreCase(contentEncoding)) {
            decoder = new DecoderEmbedder<ChannelBuffer>(new ZlibDecoder(
                    ZLIB_OR_NONE));
        }

        ChannelBuffer buffer = m.getContent();
        if (decoder != null) {
            decoder.offer(buffer);
            ChannelBuffer b = wrappedBuffer(decoder
                    .pollAll(new ChannelBuffer[decoder.size()]));
            if (decoder.finish()) {
                ChannelBuffer r = wrappedBuffer(decoder
                        .pollAll(new ChannelBuffer[decoder.size()]));
                buffer = wrappedBuffer(b, r);
            } else {
                buffer = b;
            }
        }

        return new String(buffer.array(), 0, buffer.readableBytes(),
                parseCharset(type));
    }

}
