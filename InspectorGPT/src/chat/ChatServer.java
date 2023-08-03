package chat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

final class ChatServer {
    static final int SERVER_TEST_PORT = 5000;


    public static void main(String[] args) {
        ChatServer server = new ChatServer(SERVER_TEST_PORT);
        server.execute();
    }


    private final int port;
    private final Set<UserThread> users;

    private List<Channel> allChannels = new ArrayList<>();
    


    ChatServer(int port) {
        this.port = port;
        this.users = Collections.synchronizedSet(new HashSet<>());
        this.allChannels.add(new Channel("general"));
        this.allChannels.add(new Channel("0"));
    }


    void execute() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.err.println("Chat server is listening on port: " + port);

            //noinspection InfiniteLoopStatement
            while (true) {
                Socket client = server.accept();
                System.err.println("Client connected.");

                // We dispatch a new thread for each user in the chat 
                UserThread user = new UserThread(client, this);
                this.users.add(user);
                user.start();

                // Also, we cannot use try-with-resources block on client socket
                // because it would be closed immediately after dispatching a
                // thread. We leave the thread to close the socket.
            }
        } catch (IOException ex) {
            System.err.println("Server errored: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void broadcast(UserThread sender, String message) {
        synchronized (this.users) {
            this.users.stream()
                    .filter(u -> u != sender)
                    .forEach(u -> u.sendMessage(message));
        }
    }


    public boolean sendRequestTo(String usernameOpponent, String username) {
        Optional<UserThread> foundUser;
        synchronized (this.users) {
            foundUser = 
                    this.users.stream()
                    .filter(u -> u.getNickname().equals(usernameOpponent))
                    .findFirst();             
        }

        String message = "Do you want to play a game with " + username + "? (yes/no)";

        boolean response = false; 
        if (foundUser.isPresent()) {

            //foundUser.get().interrupt();
            response = foundUser.get().receveRequest(message, username);
        }

        return response;

    }

    void remove(UserThread user) {
        String username = user.getNickname();
        this.users.remove(user);
        System.err.println("Client disconnected: " + username);
    }

    public UserThread getUserByName(String usernameOpponent) {
        synchronized (this.users) {
            return this.users.stream()
                    .filter(u -> u.getNickname().equals(usernameOpponent))
                    .findFirst()
                    .orElse(null);
        }
    }

    List<String> getUserNames() {
        synchronized (this.users) {
            return this.users.stream()
                    .map(UserThread::getNickname)
                    .filter(nullName -> nullName != null)
                    .collect(Collectors.toList());
        }
    }




    public Channel getChannelByName(String channelName) {
        return this.allChannels.stream()
                .filter(c -> c.getName().equals(channelName))
                .findFirst()
                .orElse(null);
    }


    public void addChannel(Channel channel) {
        this.allChannels.add(channel);
    }


    
    


    
    
}