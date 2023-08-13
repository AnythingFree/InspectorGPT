package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;

public class ClockLabels {
    public Label currentPlayerLabel = new Label();
    public Label player1TimeLabel = new Label();
    public Label player2TimeLabel = new Label();

    private int time1 = 30, time2 = 30;

    void updateTimeLabels(String time1, String time2, String playerName) {
        this.currentPlayerLabel.setText("Current player: " + playerName);
        this.player1TimeLabel.setText("Player 1 Time: " + time1 + " sec");
        this.player2TimeLabel.setText("Player 2 Time: " + time2 + " sec");
    }

    void startUpdateThread() {
        Thread updateThread = new Thread(() -> {
            while (true) {

                updateTimeLabels("1", "2", "3");

                try {
                    // Sleep for some time before updating again
                    Thread.sleep(1000); // Update every second
                } catch (InterruptedException e) {
                    // Handle interruption if needed
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        updateThread.setDaemon(true); // Make the thread a daemon so it doesn't prevent the application from exiting
        updateThread.start();
    }

}
