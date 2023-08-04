package chat;

import java.util.ArrayList;
import java.util.List;

public class Channel {
    private final String name;
    private final List<UserThread> subscribers; 
    private final UserThread player1, player2;
    

    public Channel(String name) {
        this.name = name;
        this.subscribers = new ArrayList<>();
        this.player1 = null;
        this.player2 = null;
    }

    public Channel(String name, UserThread player1, UserThread player2) {
        this.name = name;
        this.subscribers = new ArrayList<>();
        this.player1 = player1;
        this.player2 = player2;
        this.subscribers.add(player1);
        this.subscribers.add(player2);
        publish(player1, " You are playing with " + player2.getUsername());
        publish(player2, " You are playing with " + player1.getUsername());
    }

    public String getName() {
        return name;
    }

    public synchronized void subscribe(UserThread userThread) {
        publish(userThread, userThread.getUsername() + " has joined the chat.");
        subscribers.add(userThread);
    }

    public synchronized void unsubscribe(UserThread userThread) {
        publish(userThread, userThread.getUsername() + " has left the chat.");
        subscribers.remove(userThread);
    }

    public synchronized void publish(UserThread sender, String message) {
        this.subscribers.stream()
                .filter(u -> u != sender)
                .forEach(u -> u.receiveMessage(message));
    }

    public boolean isHerePlayer(String username) {
        return this.player1.getUsername().equals(username) || this.player2.getUsername().equals(username);
    }
}
