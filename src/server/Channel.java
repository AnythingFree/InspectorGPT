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
        this.messageHistory = new ArrayList<>();
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
        checkIfsurrender(userThread);
        subscribers.remove(userThread);

    }

    public synchronized void publish(ServerThread sender, String message) {
        if (!this.isGameFinished) {

            this.subscribers.stream()
                    .filter(u -> u != sender)
                    .forEach(u -> u.receiveMessage(message));

            if (this.playerGPT != null) {
                this.messageHistory.add(message);
                getResponseGPT(sender);
            }

            // test
            if (message.contains("hihi"))
                gameOver(sender);
            //System.out.println(this.isGameFinished);
            // ==========

        }

    }

    private synchronized void getResponseGPT(ServerThread sender) {

        String response = "[GPT]: " + this.playerGPT.getResponse(this.messageHistory);
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage(response));

        this.messageHistory.add(response);

        if (response.toLowerCase().contains(this.playerGPT.secretKey.toLowerCase())) {
            gameOver(sender);
        }

    }

    private synchronized void gameOver(ServerThread sender) {

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

        // signal GUIs to finish game
        this.player1.signalGameFinished();
        this.player2.signalGameFinished();
    }

    public synchronized boolean isHerePlayer(String username) {
        return this.player1.getUsername().equals(username) || this.player2.getUsername().equals(username);
    }

    public synchronized List<ServerThread> getSubscribers() {
        return subscribers;
    }

    public synchronized boolean isGameFinished() {
        return this.isGameFinished;
    }

    private synchronized void setGameFinished() {
        this.isGameFinished = true;
    }

    public boolean readyForCleanup() {
        return this.name != "general" && this.subscribers.size() == 0;
    }

    private void checkIfsurrender(ServerThread userThread) {
        if (this.isGameFinished || this.name.equals("general"))
            return;

        if (this.player1.equals(userThread))
            surrender(userThread, this.player2);
        else if (this.player2.equals(userThread))
            surrender(userThread, this.player1);

    }

    public void surrender(ServerThread serverThread, ServerThread winner) {
        // send message to all players that the player has surrendered
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage(serverThread.getUsername() + " has surrendered!"));

        gameOver(winner);
    }
}
