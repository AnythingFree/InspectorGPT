package chat;

import java.io.IOException;
import java.net.Socket;

public class ClientSocket {
    private Socket socket;

    public ClientSocket(String serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
    }

    public Socket getSocket() {
        return socket;
    }

    public void close() throws IOException {
        socket.close();
    }
}
