package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    // konektuje se na server, postavlja se ime, pokrecu se tredovi, cekaju se tredovi
    void execute() {
        try {
            
            try (Socket socket = new Socket(this.hostname, this.port)) {
            	
                //System.out.println("Connected to the chat server @ " + this.hostname);

                // get connected users
                BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String usernames = read.readLine();
                

                // print connected users
                System.out.println("\rConnected users: " +  usernames);

                // make connected users list
                List<String> userList = getUserList(usernames);

                // choose username
                setName();
                while (userList.contains(this.name)) {
                    System.out.println("Username is taken. Please choose a different username: ");
                    setName();
                }
                
                // ovo sve iznad radimo ovde u klijentu jer this.name treba da se prosledi u tredove
                // dispatch threads
                getWt(socket);
                


                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private List<String> getUserList(String usernames) {
        List<String> userList = new ArrayList<>();
        if (!usernames.equals("[null]")) {

            // Remove brackets
             usernames = usernames.substring(1, usernames.length() - 1);

            // Split by ", " to get a list of usernames
            userList = new ArrayList<>(Arrays.asList(usernames.split(", ")));

        } else {
            userList = new ArrayList<>();
        }
        return userList;
    }

    private void getWt(Socket socket) throws InterruptedException {
        // Dispatch threads
        Thread rt = new ClientReadThread(this.name, socket);
        Thread wt = new ClientWriteThread(this.name, socket);
        rt.start();
        wt.start();

        // Wait for threads so we can close the socket (try-with-resources)
        rt.join();
        wt.join();
    }

    private void setName() throws IOException {
        System.out.print("Enter your username: ");
        Scanner sc = new Scanner(System.in);
        this.name = sc.nextLine();
        // sc.close();
        // We cannot close sc, since we will use it later
    }
}