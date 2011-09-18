import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.Test;

public class TestSocket {

    @Test
    public void testBuffer() throws UnknownHostException, IOException {
        Socket socket = new Socket("127.0.0.1", 8100);
        int buffer = socket.getSendBufferSize();

        System.out.println(buffer / 1024);

        buffer = socket.getReceiveBufferSize();

        System.out.println(buffer / 1024);
    }
}
