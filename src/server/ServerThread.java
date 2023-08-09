package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

final class ServerThread extends Thread {
    private BufferedReader reader;
    private PrintWriter writer;
    private Socket client;
    private ChatServer server;

    private String name;
    private Channel currentChannel;
    private Boolean request;

    public ServerThread(Socket client, ChatServer chatServer) {
        this.client = client;
        this.server = chatServer;
    }

    @Override
    public void run() {

        try {
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            writer = new PrintWriter(client.getOutputStream(), true);

            // get connected users and send it
            List<String> usernames = this.server.getUserNames();
            String data = getJsonFormating(usernames);
            writer.println(data);

            // get username from client
            name = reader.readLine();

            // MENUE
            String userRequest;
            while (true) {

                userRequest = reader.readLine();
                System.out.println("User request: " + userRequest);

                switch (userRequest) {
                    case ("1"):

                        // subscribe to general channel
                        Channel channel = this.server.getChannelByName("general");
                        channel.subscribe(this);
                        this.currentChannel = channel;

                        // get connected users and send it
                        usernames = this.server.getUsersFromChannel("general");
                        writer.println("{type:chat; data:Connected users: " + usernames);
                        

                        // msm da je isti kao 0
                        writeHello();

                        // send messages to clients
                        String mes;
                        do {
                            mes = reader.readLine();
                            currentChannel.publish(this, mes);
                        } while (!mes.equals("exit"));

                        // unsubscribe from general channel
                        channel.unsubscribe(this);
                        this.currentChannel = null;

                        break;
                    case ("2"):
                        // get connected users that are not playing a game and send it!!!!!!!!!!!!1
                        // treba promjeniti u KOJI NE IGRAJU IGRU
                        usernames = this.server.getUserNames();
                        usernames.remove(this.name);
                        writer.println(getJsonFormating(usernames));

                        // get opponent
                        String opponent = reader.readLine();

                        this.server.sendRequestTo(opponent, this.name);
                        break;
                    case ("3"):
                        System.out.println("gleda druge meceve");
                        break;
                    case ("4"):
                        String respond = reader.readLine();
                        String opponentUsername = reader.readLine();
                        if (respond.equals("yes")) {

                            acceptRequest();
                            this.server.acceptRequest(opponentUsername, this);
                        } else {
                            this.server.rejectRequest(opponentUsername);
                        }
                        break;
                    case ("0"):

                        writeHello();

                        // send messages to clients
                        String mess;
                        do {
                            mess = reader.readLine();

                            if (mess != null)
                                currentChannel.publish(this, mess);

                        } while (!mess.equals("exit"));

                        // unsubscribe from channel
                        this.currentChannel.unsubscribe(this);
                        this.currentChannel = null;

                        break;
                }

            }
        } catch (IOException e) {
            System.err.println("IO Error in UserThread: " + e.getMessage());
        } finally {

            if (this.currentChannel != null) {
                this.currentChannel.unsubscribe(this);
            }

            // Remove user from set
            this.server.remove(this);

            // Close socket
            try {
                this.client.close();
            } catch (IOException e) {
                System.out.println("Socket could not be closed." + e.getMessage());
            }

        }
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

}