package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class Server {
    static final int SERVER_TEST_PORT = 5000;

    public static void main(String[] args) {
        Server server = new Server(SERVER_TEST_PORT);
        server.execute();
    }

    private final int port;
    private final Set<ThreadServer> users;
    private List<Channel> allChannels = new ArrayList<>();

    Server(int port) {
        this.port = port;
        this.users = Collections.synchronizedSet(new HashSet<>());
        this.allChannels.add(new Channel("general"));
    }

    void execute() {

        // Create the ChannelCleanupThread
        long cleanupIntervalMillis = 60000; // 1 minute
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
                ThreadServer user = new ThreadServer(client, this);
                this.users.add(user);
                user.start();

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
                    .map(ThreadServer::getUsername)
                    .filter(nullName -> nullName != null)
                    .collect(Collectors.toList());
        }
    }

    void remove(ThreadServer user) {
        String username = user.getUsername();
        this.users.remove(user);
        System.err.println("Client disconnected: " + username);
    }

    public List<String> getFreeUsers_Usernames(ThreadServer caller) {
        synchronized (this.users) {
            return this.users.stream()
                    .filter(u -> u.isFree())
                    .filter(u -> u != caller)
                    .map(ThreadServer::getUsername)
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
        List<ThreadServer> sub;
        if (channel != null) {
            sub = channel.getSubscribers();
            if (sub != null) {
                return sub.stream()
                        .map(ThreadServer::getUsername)
                        .filter(nullName -> nullName != null)
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    private void prepareChannel(String opponentUsername, ThreadServer user2, ThreadServer user1) {
        String name = user2.getUsername();

        ChannelGame channel = new ChannelGame(name + " VS " + opponentUsername, user1, user2);
        this.allChannels.add(channel);
        
        channel.start();
    }

    public List<String> getChannels() {
        return this.allChannels.stream()
                .map(Channel::getName)
                .filter(name -> !name.equals("general"))
                .collect(Collectors.toList());
    }

    // ==PLAY GAME====
    public void sendRequestTo(String usernameOpponent, String username) {
        Optional<ThreadServer> foundUser;

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
        Optional<ThreadServer> foundUser;

        synchronized (this.users) {
            foundUser = this.users.stream()
                    .filter(u -> u.getUsername().equals(opponentUsername))
                    .findFirst();
        }

        // System.out.println(foundUser.get().getUsername());
        if (foundUser.isPresent()) {
            foundUser.get().rejectRequest();
        }
    }

    public void triggerAcceptRequest_prepareChannel(String opponentUsername, ThreadServer user2) {
        Optional<ThreadServer> user1;

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

    public HashMap<String, Integer> getUserScores() {
        // iterate through users and get username and score
        HashMap<String, Integer> userScores = new HashMap<>();
        synchronized (this.users) {
            this.users.stream()
                    .forEach(u -> userScores.put(u.getUsername(), u.getScore()));
        }

        return userScores;

    }

}