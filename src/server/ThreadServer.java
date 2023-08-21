package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util._JsonUtil;

final class ThreadServer extends Thread {
    private BufferedReader reader;
    private PrintWriter writer;
    private ChatServer server;
    private Socket client;

    private String name;
    private Channel currentChannel;
    private int score = 0;

    public ThreadServer(Socket client, ChatServer chatServer) {
        this.server = chatServer;
        this.client = client;
        try {
            this.reader = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            this.writer = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error getting input/output streams in serverThread : " + e.getMessage());
        }

    }

    @Override
    public void run() {

        String message;

        try {
            while (!Thread.currentThread().isInterrupted() && !this.client.isClosed()) {
                message = this.reader.readLine();
                if (message.equals("close")) // ovo stoji jer klijent kad zatvori ovo ostane otvroneo
                    break;
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

                case "usernames":

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
                    String respond = resultMap.get("answer").toString();
                    String opponentUsername = resultMap.get("opponent").toString();

                    if (respond.equals("yes")) {
                        acceptRequest();
                        disableInputField();
                        disableTimerButton();
                        this.server.triggerAcceptRequest_prepareChannel(opponentUsername, this);
                    } else if (respond.equals("no")) {
                        this.server.triggerRejectRequest(opponentUsername);
                    } else {
                        throw new Exception("Unknown response type: " + respond);
                    }
                    break;

                case "subscribe":
                    // subscribe to channel
                    String channelName = resultMap.get("channelName").toString();
                    Channel channel = this.server.getChannelByName(channelName);
                    channel.subscribe(this);
                    this.currentChannel = channel;
                    writeHello();

                    writer.println(
                            "{type:chat; data: Connected users: " + this.server.getUsersFromChannel(channelName) + "}");
                    break;

                case "unsubscribe":
                    // unsubscribe from channel
                    if (this.currentChannel != null)
                        this.currentChannel.unsubscribe(this);
                    this.currentChannel = null;
                    break;

                case "publish":
                    // publish message to channel
                    String mesage = resultMap.get("message").toString();
                    this.currentChannel.publish(this, mesage);
                    break;

                case "scene":
                    String scene = resultMap.get("scene").toString();
                    if (scene.equals("gameMode")) {
                        // writeHello();
                        // writeRulesToTheGame();
                        break;
                    }
                    writer.println("{type:scene; scene:" + scene + "}");
                    break;

                case "gameOptions":
                    String option = resultMap.get("option").toString();
                    switch (option) {
                        case "switchPlayer":
                            this.currentChannel.pauseClock();
                            try {
                                this.currentChannel.getClockThread().join();
                            } catch (InterruptedException e) {
                                System.out.println("Nesto se desilo dok se sat zaustavljau u threadserveru");
                                ;
                            }
                            disableInputField();
                            disableTimerButton();

                            ThreadServer opponThreadServer = this.currentChannel.getOpponent(this);

                            String player1Time = Integer.toString(this.currentChannel.getPlayer1Time());
                            String player2Time = Integer.toString(this.currentChannel.getPlayer2Time());

                            opponThreadServer.updateTimeLabel(player1Time, player2Time);
                            updateTimeLabel(player1Time, player2Time);

                            opponThreadServer.enebleInputField();
                            opponThreadServer.enableTimerButton();

                            this.currentChannel.switchTurn();
                            this.currentChannel.resumeClock();
                            break;

                        default:
                            System.out.println("Unknown option: " + option);
                    }

                case "refreshTable":
                    writer.println("{type:system; data:refreshTable; result:" + getDataForLeaderboard() + "}");
                    break;

                case "option3":
                    break;

                case "channels":
                    List<String> channels = this.server.getChannels();
                    writer.println("{type:channels; data:" + channels.toString() + "}");
                    break;

                default:
                    System.out.println("Unknown message type: " + messageType);
            }

        } catch (Exception e) {
            System.out.println("Error in handleIncomingMessage [server]");
            System.out.println(e.getMessage());
        }

    }

    private String getDataForLeaderboard() {
        String data = "[";
        HashMap<String, Integer> leaderboard = this.server.getUserScores();
        for (String key : leaderboard.keySet()) {
            data += key + ":" + leaderboard.get(key) + ";";
        }
        data += "]";
        return data;
    }

    private void updateTimeLabel(String player1Time, String player2Time) {
        this.writer.println("{type:system; data:TimeLabel; player1Time:" + player1Time
                + "; player2Time:" + player2Time + "}");
    }

    void signalGameFinished(ThreadServer winner, int timeLeft) {
        // disableInputField();
        // disableTimerButton();
        gameFinished(Integer.toString(timeLeft));
        sendNotification("Game finished!" + winner.name + "has won! Your current total score: " + this.score);
    }

    private void gameFinished(String timeLeft) {
        this.writer.println("{type:system; data:gameFinished; timeLeft:" + timeLeft + "}");
    }

    void sendNotification(String message) {
        this.writer.println("{type:system; data:notification; message:" + message + "}");
    }

    private void disableInputField() {
        this.writer.println("{type:system; data:inputFiledOFF}");
    }

    private void enebleInputField() {
        this.writer.println("{type:system; data:inputFiledON}");
    }

    private void disableTimerButton() {
        this.writer.println("{type:system; data:TimerButton; state:OFF}");
    }

    private void enableTimerButton() {
        this.writer.println("{type:system; data:TimerButton; state:ON}");
    }

    private String getJsonFormating(List<String> usernames) {
        if (usernames == null)
            return "{type:usernames; data:[]}";
        return "{type:usernames; data:" + usernames.toString() + "}";
    }

    private void writeHello() {
        this.writer.println("{type:chat; data:Hello, " + this.name + "! You are in \"" + this.currentChannel.getName()
                + "\"!}");
    }

    private void writeRulesToTheGame() {
        this.writer.println("{type:chat; data:Rules: }");
        this.writer.println("{type:chat; data:1. The secret key is an English word. No numbers or simbols.}");
        this.writer.println("{type:chat; data:2. You have to make the GPT say it, not you.}");
        this.writer.println(
                "{type:chat; data:3. There are no rules! This is a school project full of bugs and anything can happen!}");
    }

    public void receiveMessage(String message) {
        System.out.println(this.name);
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

}