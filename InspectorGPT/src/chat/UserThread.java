package chat;

import java.io.*;
import java.net.*;
import java.util.List;

final class UserThread extends Thread {
    private final ChatServer server;
    private final Socket sock;
    private BufferedReader fromUser;
    private PrintWriter toUser;
    private String username;


    UserThread(Socket socket, ChatServer server) {
        this.sock = socket;
        this.server = server;
        try {
            this.fromUser = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
            this.toUser = new PrintWriter(this.sock.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        try {
            
            List<String> usernames = this.server.getUserNames();

            // send connected users list
            this.sendMessage("Connected users: " + usernames);
            
            // get username
            this.username = fromUser.readLine();
            
            // Broadcast that new user has entered the chat
            this.server.broadcast(this, "New user connected: " + this.username);
            
             // choose user to play a game with
            String usernameOpponent = getUsernameOpponent(usernames);

            // send messages to opponent
            getClientMessageToOpponent(usernames, usernameOpponent);
            
            // broadcast to global chat
            // getClientMessage();
            
        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // Remove user from set
            this.server.remove(this);

            // Close socket
            try {
                this.sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void getClientMessageToOpponent(List<String> usernames, String usernameOpponent) throws IOException {

       
        String clientMessage;
        do {
            // Read message from user
            clientMessage = fromUser.readLine();
            if (clientMessage == null)
                break;

            // send message to chosen user
            this.server.broadcastTo(usernameOpponent, this, "[" + this.username + "]: " + clientMessage);
            
        } while (!clientMessage.equals("bye"));
    }


    private String getUsernameOpponent(List<String> usernames) throws IOException {
        this.sendMessage("If there isn't any user to play with, wait for someone to connect.");
        while (usernames.size() == 0) {
            try {
                wait(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        this.sendMessage("Chose from 0 to " + usernames.size() + ". Connected users: " + usernames);
        int userIndex = Integer.parseInt(fromUser.readLine());
        while (userIndex < 0 || userIndex >= usernames.size()) {
            this.sendMessage("Invalid index. Chose from 0 to " + usernames.size() + ". Connected users: " + usernames);
            userIndex = Integer.parseInt(fromUser.readLine());
        }
        return usernames.get(userIndex);
    }

/*
    private void getUsername(List<String> usernames) throws IOException {
        // Ask for username until it is unique
        this.sendMessage("Enter your username: ");
        while (usernames.contains(this.username = fromUser.readLine())) {
            this.sendMessage("Username is taken. Please choose a different username: ");
        }

    }
 */

    private void getClientMessage() throws IOException {
        String clientMessage;
        do {
            // Read message from user
            clientMessage = fromUser.readLine();
            if (clientMessage == null)
                break;

            // Broadcast the message
            this.server.broadcast(this, "[" + this.username + "]: " + clientMessage);
            
        } while (!clientMessage.equals("bye"));

        // Broadcast that user has disconnected
        this.server.broadcast(this, this.username + " has left the chat.");
    }

    void sendMessage(String message) {
        if (this.toUser != null)
            this.toUser.println(message);
    }

    String getNickname() {
        return this.username;
    }
}