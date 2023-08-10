package server;

import java.util.ArrayList;
import java.util.List;

import GPT.Gpt;

public class Channel {
    private final String name;
    private final List<ServerThread> subscribers;
    private final ServerThread player1, player2;
    private Gpt playerGPT;
    private ArrayList<String> messageHistory;
    private boolean isGameFinished = false;

    public Channel(String name) {
        this.name = name;
        this.subscribers = new ArrayList<>();
        this.player1 = null;
        this.player2 = null;
        this.playerGPT = null;
    }

    public Channel(String name, ServerThread player1, ServerThread player2) {
        this.name = name;
        this.subscribers = new ArrayList<>();
        this.player1 = player1;
        this.player2 = player2;
        this.subscribers.add(player1);
        this.subscribers.add(player2);
        player1.setCurrentChannel(this);
        player2.setCurrentChannel(this);
        player1.receiveMessage("You are playing with " + player2.getUsername());
        player2.receiveMessage("You are playing with " + player1.getUsername());

        this.playerGPT = new Gpt();
        this.messageHistory = new ArrayList<String>();
    }

    public String getName() {
        return name;
    }

    public synchronized void subscribe(ServerThread userThread) {
        publish(userThread, userThread.getUsername() + " has joined the chat.");
        subscribers.add(userThread);
    }

    public synchronized void unsubscribe(ServerThread userThread) {
        publish(userThread, userThread.getUsername() + " has left the chat.");
        subscribers.remove(userThread);
    }

    public synchronized void publish(ServerThread sender, String message) {

        this.subscribers.stream()
                .filter(u -> u != sender)
                .forEach(u -> u.receiveMessage(message));

        this.messageHistory.add(message);

        if (this.playerGPT != null)
            getResponseGPT(sender);

        // test
        if (message.contains("hihi"))
            gameOver(sender);
        //System.out.println(this.isGameFinished);
        //==========

    }

    private void getResponseGPT(ServerThread sender) {

        String response = "[GPT]: " + this.playerGPT.getResponse(this.messageHistory);
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage(response));

        this.messageHistory.add(response);

        if (response.toLowerCase().contains(this.playerGPT.secretKey.toLowerCase())) {
            gameOver(sender);
        }

    }

    private void gameOver(ServerThread sender) {

        // send message to all players
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage("Game over! " + sender.getUsername() + " has won!"));

        // update scores
        sender.setScore(sender.getScore() + 1);

        // set game finished
        setGameFinished();

        // remove gpt
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage("GPT has been removed."));
        this.playerGPT = null;

    }

    public boolean isHerePlayer(String username) {
        return this.player1.getUsername().equals(username) || this.player2.getUsername().equals(username);
    }

    public List<ServerThread> getSubscribers() {
        return subscribers;
    }

    public synchronized  boolean isGameFinished() {
        return this.isGameFinished;
    }

    private synchronized void setGameFinished() {
        this.isGameFinished = true;
    }

    public boolean readyForCleanup() {
        return this.name != "general" && this.subscribers.size() == 0;
    }
}
