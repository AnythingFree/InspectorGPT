package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import client._JsonUtil;

final class ServerThread extends Thread {
    private BufferedReader reader;
    private PrintWriter writer;
    private ChatServer server;
    private Socket client;

    private String name;
    private Channel currentChannel;
    private int score = 0;

    public ServerThread(Socket client, ChatServer chatServer) {
        this.server = chatServer;
        this.client = client;
        try {
            this.reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.writer = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error getting input/output streams in serverThread : " + e.getMessage());
        }

    }

    @Override
    public void run() {

        String message;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                message = reader.readLine();
                handleIncomingMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Error reading message from client" + e.getMessage());
        } finally {

            if (this.currentChannel != null) {
                this.currentChannel.unsubscribe(this);
                this.currentChannel = null;
            }

            // Remove user from set
            this.server.remove(this);

            // Close socket
            try {
                this.client.close();
            } catch (IOException e) {
                System.out.println("Socket could not be closed." + e.getMessage());
            }

            System.err.println("Server thread stopped");

        }

    }

    private void handleIncomingMessage(String message) {
        try {

            // Parse the incoming JSON message
            Map<String, String> resultMap = _JsonUtil.jsonToMap(message);

            // Extract the "type" field from the JSON message
            String messageType = resultMap.get("type").toString();

            // Handle the message based on its type using a switch-case statement
            List<String> usernames;
            String data;
            switch (messageType) {

                case "setName":
                    // get username from client
                    this.name = resultMap.get("name").toString();
                    break;

                case "users":

                    data = resultMap.get("data").toString();
                    switch (data) {
                        case "all":
                            // get connected users on General channel
                            usernames = this.server.getUserNames();
                            writer.println(getJsonFormating(usernames));
                            break;

                        case "free":
                            // ZA SAD NEK BUDE KOJI NEMAJU nista za CURRENTchannel
                            usernames = this.server.getFreeUsers_Usernames(this);
                            writer.println(getJsonFormating(usernames));
                            break;

                        case "inChannel":
                            String channelName = resultMap.get("channelName").toString();
                            usernames = this.server.getUsersFromChannel(channelName);
                            writer.println(getJsonFormating(usernames));
                            break;
                    }
                    break;

                case "request":
                    // get opponent
                    String opponent = resultMap.get("opponent").toString();
                    this.server.sendRequestTo(opponent, this.name);
                    break;

                case "response":
                    String respond = resultMap.get("answ").toString();
                    String opponentUsername = resultMap.get("opponent").toString();

                    if (respond.equals("yes")) {
                        acceptRequest();
                        this.server.triggerAcceptRequest_prepareChannel(opponentUsername, this);
                    } else {
                        this.server.rejectRequest(opponentUsername);
                    }
                    break;

                case "subsribe":
                    // subscribe to channel
                    Channel channel = this.server.getChannelByName(resultMap.get("channelName").toString());
                    channel.subscribe(this);
                    this.currentChannel = channel;
                    break;

                case "unsubscribe":
                    // unsubscribe from channel
                    this.currentChannel.unsubscribe(this);
                    this.currentChannel = null;
                    break;

                case "publish":
                    // publish message to channel
                    String mesage = resultMap.get("message").toString();
                    this.currentChannel.publish(this, mesage);
                    break;

                case "chatMode":
                    writeHello();
                    break;

                case "gameFinished":
                    signalGameFinished();
                    break;

                case "option3":
                    break;

                default:
                    System.out.println("Unknown message type: " + messageType);
            }

        } catch (Exception e) {
            System.out.println("Error in handleIncomingMessage [server]");
            System.out.println(e.getMessage());

        }

    }

    private void signalGameFinished() {
        this.writer.println("{type:system; data:inputFiled}");
        this.writer.println(
                "{type:system; data:notification; message:Game finished! Your current score: " + this.score + "}");
    }

    private String getJsonFormating(List<String> usernames) {
        return "{type:usernames; data:" + usernames.toString() + "}";
    }

    private void writeHello() {
        writer.println("{type:chat; data:Hello, " + this.name + "! You are in " + this.currentChannel.getName()
                + "! Type 'exit' to leave.}");
    }

    public void receiveMessage(String message) {
        writer.println("{type:chat; data:" + message + "}");
    }

    public void receveRequest(String username) {
        writer.println("{type:request; opponent:" + username + "}");
    }

    public void rejectRequest() {
        writer.println("{type:response; data:no}");
    }

    public void acceptRequest() {
        writer.println("{type:response; data:yes}");
    }

    public void setCurrentChannel(Channel channel) {
        this.currentChannel = channel;
    }

    public String getUsername() {
        return this.name;
    }

    public boolean isFree() {
        if (this.currentChannel == null)
            return true;
        return false;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized void setScore(int score) {
        this.score = score;
    }

    public void blokiraj() {
        this.writer.println("{type:system; data:inputFiled}");
    }

}