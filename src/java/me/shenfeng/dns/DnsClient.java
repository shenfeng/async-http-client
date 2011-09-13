package me.shenfeng.dns;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static me.shenfeng.http.Utils.isIP;
import static me.shenfeng.http.Utils.toBytes;
import static me.shenfeng.http.Utils.toInt;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;
import static org.jboss.netty.util.ThreadNameDeterminer.CURRENT;
import static org.jboss.netty.util.ThreadRenamingRunnable.setThreadNameDeterminer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Random;

import me.shenfeng.ListenableFuture;
import me.shenfeng.ListenableFuture.Listener;
import me.shenfeng.PrefixThreadFactory;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

class Entry {
    final String host;
    final int id;

    public Entry(String host, int id) {
        this.host = host;
        this.id = id;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Entry) {
            Entry rhs = (Entry) obj;
            return rhs.host.equals(host) && id == rhs.id;
        }
        return false;
    }

    @Override
    public String toString() {
        return host + "@" + id;
    }

    public int hashCode() {
        return host.hashCode() + id;
    }
}

public class DnsClient {
    final static Random r = new Random();
    final static byte[] FLAGS_PARAMS = new byte[] { 1, 0, 0, 1, 0, 0, 0, 0,
            0, 0 };
    final static byte[] A_IN = new byte[] { 0, 1, 0, 1 };

    private final ConnectionlessBootstrap mBootstrap;
    private final InetSocketAddress mDnsServernew = new InetSocketAddress(
            "61.147.37.1", 53);
    private final DatagramChannel c;
    private final HashMap<Entry, DnsResponseFuture> mMeta = new HashMap<Entry, DnsResponseFuture>();

    public DnsClient() {
        setThreadNameDeterminer(CURRENT);
        NioDatagramChannelFactory factory = new NioDatagramChannelFactory(
                newCachedThreadPool(new PrefixThreadFactory("DNS")), 1);
        mBootstrap = new ConnectionlessBootstrap(factory);
        mBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new DnsResponseHandler(mMeta));
            }
        });
        c = (DatagramChannel) mBootstrap.bind(new InetSocketAddress(0));
    }

    public DnsResponseFuture resolve(final String host) {
        final DnsResponseFuture future = new DnsResponseFuture(host);
        if (isIP(host)) {
            future.done(host);
            return future;
        } else {
            final int id = r.nextInt(65536);
            final ChannelBuffer buffer = getBytes(id, host);
            c.write(buffer, mDnsServernew);
            mMeta.put(new Entry(host, id), future);
            future.addistener(new Listener<String>() {
                public void run(ListenableFuture<String> f, String result) {
                    mMeta.remove(host);
                }
            });
        }
        return future;
    }

    private ChannelBuffer getBytes(final int id, String domain) {
        ChannelBuffer buffer = dynamicBuffer();
        buffer.writeBytes(toBytes(id));
        buffer.writeBytes(FLAGS_PARAMS);

        int start = 0, length = domain.length();
        for (int i = 0; i < length; ++i) {
            if (domain.charAt(i) == '.') {
                buffer.writeByte(i - start);
                buffer.writeBytes(domain.substring(start, i).getBytes());
                start = i + 1;
            }
        }
        buffer.writeByte(length - start);
        buffer.writeBytes(domain.substring(start).getBytes());
        buffer.writeByte(0);
        buffer.writeBytes(A_IN);
        return buffer;
    }
}

class DnsResponseHandler extends SimpleChannelHandler {

    private final HashMap<Entry, DnsResponseFuture> mMeta;

    public DnsResponseHandler(HashMap<Entry, DnsResponseFuture> meta) {
        mMeta = meta;
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        // TODO cname www.ogidc.com
        ChannelBuffer o = ((ChannelBuffer) e.getMessage()).slice();
        byte[] array = o.array();
        final int id = toInt(array);
        int start = 12;
        int length = array[start];
        int answers = array[7];

        while (length != 0) {
            start = length + start + 1;
            length = array[start];
            array[start] = '.';
        }

        String host = new String(array, 13, start - 13);
        start += 5;// skip query type and class

        final DnsResponseFuture future = mMeta.get(new Entry(host, id));
        if (future != null) {
            if (answers > 0) {
                start += 12;// skip answers name, type, class, ttl, data length
                StringBuilder sb = new StringBuilder(15);
                for (int i = 0; i < 4; ++i) {
                    int b = toInt(array[start + i]);
                    sb.append(b).append('.');
                }
                String ip = sb.subSequence(0, sb.length() - 1).toString();
                future.done(ip);
            } else {
                future.done(DnsResponseFuture.RESOLVE_FAIL);
            }
        }
    }
}
