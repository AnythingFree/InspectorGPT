package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import GPT.JavaRunCommand;

final class ChatServer {
    static final int SERVER_TEST_PORT = 5000;

    public static void main(String[] args) {
        ChatServer server = new ChatServer(SERVER_TEST_PORT);
        server.execute();
    }

    private final int port;
    private final Set<ServerThread> users;
    private List<Channel> allChannels = new ArrayList<>();

    ChatServer(int port) {
        this.port = port;
        this.users = Collections.synchronizedSet(new HashSet<>());
        this.allChannels.add(new Channel("general"));
    }

    void execute() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.err.println("Chat server is listening on port: " + port);

            // noinspection InfiniteLoopStatement
            while (true) {
                Socket client = server.accept();
                System.err.println("Client connected.");

                // We dispatch a new thread for each user in the chat
                ServerThread user = new ServerThread(client, this);
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

    /*
     * void broadcast(UserThread sender, String message) {
     * synchronized (this.users) {
     * this.users.stream()
     * .filter(u -> u != sender)
     * .forEach(u -> u.sendMessage(message));
     * }
     * }
     * 
     * public boolean sendRequestTo(String usernameOpponent, String username) {
     * Optional<UserThread> foundUser;
     * synchronized (this.users) {
     * foundUser = this.users.stream()
     * .filter(u -> u.getNickname().equals(usernameOpponent))
     * .findFirst();
     * }
     * 
     * String message = "Do you want to play a game with " + username +
     * "? (yes/no)";
     * 
     * boolean response = false;
     * if (foundUser.isPresent()) {
     * 
     * // foundUser.get().interrupt();
     * response = foundUser.get().receveRequest(message, username);
     * }
     * 
     * return response;
     * 
     * }
     * 
     * 
     * 
     * public UserThread getUserByName(String usernameOpponent) {
     * synchronized (this.users) {
     * return this.users.stream()
     * .filter(u -> u.getNickname().equals(usernameOpponent))
     * .findFirst()
     * .orElse(null);
     * }
     * }
     * 
     * 
     * void broadcast(ServerThread sender, String message) {
     * synchronized (this.users) {
     * this.users.stream()
     * .filter(u -> u != sender)
     * .forEach(u -> u.receiveMessage(message));
     * }
     * }
     */
    List<String> getUserNames() {
        synchronized (this.users) {
            return this.users.stream()
                    .map(ServerThread::getUsername)
                    .filter(nullName -> nullName != null)
                    .collect(Collectors.toList());
        }
    }

    void remove(ServerThread user) {
        String username = user.getUsername();
        this.users.remove(user);
        System.err.println("Client disconnected: " + username);
    }

    // ====channel======
    public Channel getChannelByName(String channelName) {
        return this.allChannels.stream()
                .filter(c -> c.getName().equals(channelName))
                .findFirst()
                .orElse(null);
    }

    public void addChannel(Channel channel) {
        this.allChannels.add(channel);
    }

    public void removeChannel(Channel channel) {
        this.allChannels.remove(channel);
    }

    // ===gpt====
    public String askGPT(String message) {
        return JavaRunCommand.ask(message);
    }

    // ==PLAY GAME====
    public void sendRequestTo(String usernameOpponent, String username) {
        Optional<ServerThread> foundUser;

        synchronized (this.users) {
            foundUser = this.users.stream()
                    .filter(u -> u.getUsername().equals(usernameOpponent))
                    .findFirst();
        }

        if (foundUser.isPresent()) {
            // foundUser.get().interrupt();
            foundUser.get().receveRequest(username);
        }

    }

    public void rejectRequest(String opponentUsername) {
        Optional<ServerThread> foundUser;

        synchronized (this.users) {
            foundUser = this.users.stream()
                    .filter(u -> u.getUsername().equals(opponentUsername))
                    .findFirst();
        }

        System.out.println(foundUser.get().getUsername());
        if (foundUser.isPresent()) {
            foundUser.get().rejectRequest();
        }
    }

    public void acceptRequest(String opponentUsername, ServerThread user2) {
        Optional<ServerThread> user1;
       

        synchronized (this.users) {
            user1 = this.users.stream()
                    .filter(u -> u.getUsername().equals(opponentUsername))
                    .findFirst();
        }

        
        if (user1.isPresent()) {
            user1.get().acceptRequest();
        }

        String name = user2.getUsername();

        Channel channel = new Channel(name+opponentUsername, user1.get(), user2);
        this.allChannels.add(channel);
    }

    public List<String> getUsersFromChannel(String channelName) {
        Channel channel = this.getChannelByName(channelName);
        List<ServerThread> sub;
        if (channel != null) {
            sub = channel.getSubscribers();
            if (sub != null) {
                return sub.stream()
                        .map(ServerThread::getUsername)
                        .filter(nullName -> nullName != null)
                        .collect(Collectors.toList());
            }
        }   
        return null;
    }

}