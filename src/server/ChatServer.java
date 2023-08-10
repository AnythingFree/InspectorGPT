package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

        // Create the ChannelCleanupThread
        long cleanupIntervalMillis = 60000; // Adjust the interval as needed
        ThreadChannelCleanUp cleanupThread = new ThreadChannelCleanUp(allChannels, cleanupIntervalMillis);
        cleanupThread.start();

        // accept clients
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
        } finally {
            // Stop the ChannelCleanupThread
            cleanupThread.interrupt();
            try {
                cleanupThread.join();
            } catch (InterruptedException e) {
                // Handle the InterruptedException
            }
        }
    }

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

    public List<String> getFreeUsers_Usernames(ServerThread caller) {

        synchronized (this.users) {
            return this.users.stream()
                    .filter(u -> u.isFree())
                    .filter(u -> u != caller)
                    .map(ServerThread::getUsername)
                    .filter(nullName -> nullName != null)
                    .collect(Collectors.toList());
        }

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

    private void prepareChannel(String opponentUsername, ServerThread user2, ServerThread user1) {
        String name = user2.getUsername();

        Channel channel = new Channel(name + " VS " + opponentUsername, user1, user2);
        this.allChannels.add(channel);
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

    public void triggerRejectRequest(String opponentUsername) {
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

    public void triggerAcceptRequest_prepareChannel(String opponentUsername, ServerThread user2) {
        Optional<ServerThread> user1;

        synchronized (this.users) {
            user1 = this.users.stream()
                    .filter(u -> u.getUsername().equals(opponentUsername))
                    .findFirst();
        }

        if (user1.isPresent()) {
            user1.get().acceptRequest();
        }

        prepareChannel(opponentUsername, user2, user1.get());
    }
    // ======================================

}