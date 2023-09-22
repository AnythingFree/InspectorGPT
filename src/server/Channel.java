package server;

import java.util.ArrayList;
import java.util.List;

public class Channel {
    private final String name;
    final List<ThreadServer> subscribers;

    ArrayList<String> messageHistory = new ArrayList<>();

    public Channel(String name) {
        this.name = name;
        this.subscribers = new ArrayList<>();
    }

    public synchronized void subscribe(ThreadServer userThread) {
        sendMessageToUser(userThread, "Hello, " + userThread.getUsername() + "! You are in \"" + this.getName() + "\"!}");
        sendMessageToUser(userThread, "Connected users currently: " + subscribers.toString() +
                "\\n============================\\n");

        showHistoryOfMessages(userThread);

        publish(userThread, userThread.getUsername() + " has joined the chat.");
        subscribers.add(userThread);
    }

    void showHistoryOfMessages(ThreadServer userThread) {
        if (this.messageHistory.size() > 0) {
            for (String message : this.messageHistory) {
                sendMessageToUser(userThread, message);
            }
        }
    }

    public synchronized void unsubscribe(ThreadServer userThread) {
        publish(userThread, userThread.getUsername() + " has left the chat.");
        subscribers.remove(userThread);

    }

    public synchronized void publish(ThreadServer sender, String message) {
        this.subscribers.stream()
                .filter(u -> u != sender)
                .forEach(u -> sendMessageToUser(u, message));
        this.messageHistory.add(message);
    }

    public synchronized List<ThreadServer> getSubscribers() {
        return subscribers;
    }

    public boolean readyForCleanup() {
        return this.name != "general" && this.subscribers.size() == 0;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getMessageHistory() {
        return this.messageHistory;
    }

    public void addToMessageHistory(String response) {
        this.messageHistory.add(response);
    }

    void sendMessageToUser(ThreadServer userThread, String message) {
        userThread.receiveMessage(message);
    }
}