package server;

public class ChannelSub extends Channel {

    public ChannelSub(String name) {
        super(name);
    }

    @Override
    void sendMessageToUser(ThreadServer userThread, String message) {
        userThread.receiveMessage2(message);
    }
}
