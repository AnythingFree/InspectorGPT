package server;

import java.util.ArrayList;
import java.util.List;

public class Channel {
    private final String name;
    private final List<ServerThread> subscribers;
    private final ServerThread player1, player2;


    public Channel(String name) {
        this.name = name;
        this.subscribers = new ArrayList<>();
        this.player1 = null;
        this.player2 = null;
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
    }

    public boolean isHerePlayer(String username) {
        return this.player1.getUsername().equals(username) || this.player2.getUsername().equals(username);
    }

    public List<ServerThread> getSubscribers() {
        return subscribers;
    }
}
