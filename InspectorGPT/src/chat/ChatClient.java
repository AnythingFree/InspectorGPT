package chat;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

final class ChatClient {


    public static void main(String[] args) {
        ChatClient client = new ChatClient("localhost", ChatServer.SERVER_TEST_PORT);
        System.err.println("Connecting to local port: " + ChatServer.SERVER_TEST_PORT);
        client.execute();
    }


    private final String hostname;
    private final int port;
    private String name;


    ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    // postavlja se ime, konektuje se na server, pokrecu se tredovi, cekaju se tredovi
    void execute() {
        try {
            this.setName();

            try (Socket socket = new Socket(this.hostname, this.port)) {
            	
                //System.out.println("Connected to the chat server @ " + this.hostname);

                // Dispatch threads
                Thread rt = new ClientReadThread(this.name, socket);
                Thread wt = new ClientWriteThread(this.name, socket);
                rt.start();
                wt.start();

                // Wait for threads so we can close the socket (try-with-resources)
                rt.join();
                wt.join();
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void setName() throws IOException {
        System.out.print("Enter your name: ");
        Scanner sc = new Scanner(System.in);
        this.name = sc.nextLine();
        // sc.close();
        // We cannot close sc, since we will use it later
    }
}