package client;

import java.io.IOException;
import java.net.Socket;

public class _ClientSocket {
    private Socket socket;

    public _ClientSocket(String serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
    }

    public Socket getSocket() {
        return socket;
    }

    public void close() {

    	System.err.println("Zatvara se soket...");
        try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Greska pri zatvaranju client soketa.");
		}
    }
}
