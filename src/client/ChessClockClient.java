package client;

import javafx.application.Platform;

public class ChessClockClient {

    private ClientGUI clientGUI;
    private volatile int time1;
    private volatile int time2;
    private boolean isPlayer1Turn = true;
    private Thread updateThread;

    public ChessClockClient(ClientGUI clientGUI2, int time) {
        this.clientGUI = clientGUI2;
        this.time1 = time;
        this.time2 = time;
    }

    void startUpdateThread() {
        updateThread = new Thread(() -> {
            while (time1 > 0 && time2 > 0) {
                try {
                    Thread.sleep(1000);

                    if (isPlayer1Turn)
                        Platform.runLater(() -> {
                            this.clientGUI.player1Time.setText("Player 1: " + getTimeFormatting(time1--));

                        });
                    else
                        Platform.runLater(() -> {
                            this.clientGUI.player2Time.setText("Player 2: " + getTimeFormatting(time2--));
                        });

                } catch (InterruptedException e) {
                    // Handle interruption if needed
                    System.out.println("clientClock interrupted");
                    break;
                }
            }
        });

        updateThread.setDaemon(true); // Make the thread a daemon so it doesn't prevent the application from exiting
        updateThread.start();
    }

    private synchronized String getTimeFormatting(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    // update times
    public synchronized void updateTime(int time1, int time2) {
        this.time1 = time1;
        this.time2 = time2;
    }

    // update turn
    public synchronized void updateTurn() {
        this.isPlayer1Turn = !isPlayer1Turn;
    }

    // stop thread
    public synchronized void stop(int time) {
        this.updateThread.interrupt();
        if (isPlayer1Turn)
            this.clientGUI.player1Time.setText(" Player 1: " + getTimeFormatting(time));
        else
            this.clientGUI.player2Time.setText(" Player 2: " + getTimeFormatting(time));
    }

}
