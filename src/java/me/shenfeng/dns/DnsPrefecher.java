package me.shenfeng.dns;

import static me.shenfeng.Utils.toBytes;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import me.shenfeng.Utils;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prefetching DNS A record, does not wait for resolution to be completed. <br/>
 * 
 * Used when a local DNS cache server is present. <br/>
 * <br/>
 * 
 * -Djava.net.preferIPv4Stack=true
 * 
 * @see InetAddress
 * @author feng
 * 
 */
public class DnsPrefecher implements DnsClientConstant {
    final static Logger logger = LoggerFactory.getLogger(DnsPrefecher.class);
    private DatagramSocket mSocket;
    private final AtomicInteger mId = new AtomicInteger(0);
    private final ArrayList<InetSocketAddress> mServers;

    public static DnsPrefecher getInstance() {

        return Holder.instance;
    }

    private static class Holder {
        public static final DnsPrefecher instance = new DnsPrefecher();
    }

    public DnsPrefecher() {
        try {
            mSocket = new DatagramSocket();
        } catch (SocketException e) {
            logger.error("init DnsPrefecher error", e.getMessage());
        }

        List<String> servers = Utils.getNameServer();
        mServers = new ArrayList<InetSocketAddress>(servers.size());
        for (String ns : servers) {
            mServers.add(new InetSocketAddress(ns, 53));
        }
    }

    public void prefetch(List<String> domains) throws IOException {
        for (String domain : domains) {
            prefetch(domain);
        }
    }

    public void prefetch(String domain) throws IOException {
        if (Utils.isIP(domain))
            return;

        int id = mId.incrementAndGet();
        if (id > Short.MAX_VALUE) {
            id = Short.MAX_VALUE;
            mId.set(0);
        }

        final ChannelBuffer buffer = dynamicBuffer(64);
        buffer.writeBytes(toBytes(id));
        buffer.writeBytes(FLAGS_PARAMS);

        int start = 0;
        final int length = domain.length();
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

        for (InetSocketAddress server : mServers) {
            DatagramPacket packet = new DatagramPacket(buffer.array(),
                    buffer.readerIndex(), buffer.readableBytes(), server);
            mSocket.send(packet);
        }
    }
}
