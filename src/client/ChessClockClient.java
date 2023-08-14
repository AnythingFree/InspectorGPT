package client;

import javafx.application.Platform;

public class ChessClockClient {

    private ClientGUI clientGUI;
    private volatile int time1 = 30;
    private volatile int time2 = 30;
    private boolean isPlayer1Turn=true;


    public ChessClockClient(ClientGUI clientGUI2) {
        this.clientGUI = clientGUI2;
    }

    void startUpdateThread() {
        Thread updateThread = new Thread(() -> {
            while (time1 != 0 || time2 != 0) {
                try {
                    Thread.sleep(1000);

                    if (isPlayer1Turn)
                        Platform.runLater(() -> {
                            this.clientGUI.player1Time.setText("Player 1: " + time1--);

                        });
                    else
                        Platform.runLater(() -> {
                            this.clientGUI.player2Time.setText("Player 2: " + time2--);
                        });

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

    // update times
    public synchronized void updateTime(String time1, String time2) {
        this.time1 = Integer.parseInt(time1);
        this.time2 = Integer.parseInt(time2);
    }

    // update turn
    public synchronized void updateTurn() {
        this.isPlayer1Turn = !isPlayer1Turn;
    }

}
