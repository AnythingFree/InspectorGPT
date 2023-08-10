package server;

import java.util.List;

public class ThreadChannelCleanUp extends Thread {

    private final List<Channel> channels;
    private final long cleanupIntervalMillis;

    public ThreadChannelCleanUp(List<Channel> channels, long cleanupIntervalMillis) {
        this.channels = channels;
        this.cleanupIntervalMillis = cleanupIntervalMillis;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Thread.sleep(cleanupIntervalMillis);
                cleanupFinishedGames();
            } catch (InterruptedException e) {
                // Thread interrupted, exit loop
                break;
            }
        }
    }

    private void cleanupFinishedGames() {
        synchronized (channels) {
            channels.removeIf(Channel::readyForCleanup);
        }
    }
}