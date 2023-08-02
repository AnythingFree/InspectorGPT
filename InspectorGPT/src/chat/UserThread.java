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
            
            
            // send connected users list
            List<String> usernames = this.server.getUserNames();
            this.sendMessage(usernames.toString());
            
            // get username
            this.username = fromUser.readLine();  // ovo dobijas od clientwriteThreada
        
            //=============================================

            // Broadcast that new user has entered the chat
            this.server.broadcast(this, "New user connected: " + this.username);
            
             // choose user to play a game with
            String usernameOpponent = getUsernameOpponent();

            // send request to opponent
           // boolean accepted = this.server.sendRequestTo(usernameOpponent, this.username);
            //if (accepted)
                // send messages to opponent
            //    getClientMessageToOpponent(usernames, usernameOpponent);
            
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


    private String getUsernameOpponent() throws IOException {

        List<String> usernames = getUserNamesOfOpponents();

        // wait for users to connect
        usernames = ifNoUsersConnectedWait(usernames);

        // ask user to choose opponent
        this.sendMessage("Chose from 0 to " + (usernames.size()-1) + ". \nConnected users: " + usernames);

        // get index of opponent
        int userIndex = getValidInteger();
        while (userIndex < 0 || userIndex >= usernames.size()) {
            this.sendMessage("Invalid index. Chose from 0 to " + (usernames.size()-1) + ". Connected users: " + usernames);
            userIndex = getValidInteger();
        }
        return usernames.get(userIndex);
    }


    private int getValidInteger() throws IOException {
        
        int userIndex;
        while (true) {
            this.sendMessage("Chose opponent. Enter an integer: ");
            try {
                userIndex = Integer.parseInt(fromUser.readLine());
                break; // Exit loop on successful parsing
            } catch (NumberFormatException e) {
                this.sendMessage("Invalid input. Please enter a valid integer.");
            }
        }
        return userIndex;
    }


    private List<String> ifNoUsersConnectedWait(List<String> usernames) {
        if (usernames.size() == 0) {
            this.sendMessage("No other users connected. Wait for someone to connect.");
        }
        while (usernames.size() == 0) { // ovo se moze rijesiti i sa wait() i notify()
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            usernames = getUserNamesOfOpponents();
            
        }
        return usernames;
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

    private List<String> getUserNamesOfOpponents() {
        List<String> usernames = this.server.getUserNames();
        usernames.remove(this.username);
        return usernames;
    }


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
        else
            System.out.println("Error sending message to user " + this.username);
    }

    String getNickname() {
        return this.username;
    }
}