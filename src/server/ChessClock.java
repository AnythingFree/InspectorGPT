package server;

public class ChessClock {

    private volatile int player1Time; // Time in seconds
    private volatile int player2Time; // Time in seconds
    private boolean isPlayer1Turn; // Indicates whose turn it is
    private volatile boolean isRunning; // Indicates if the clock is running
    private ThreadServer player1;
    private ThreadServer player2;
    private ChannelGame channel;
    private Thread thread;

    public ChessClock(int initialTime, ChannelGame channel, ThreadServer player1, ThreadServer player2) {
        this.player1Time = initialTime;
        this.player2Time = initialTime;
        this.isPlayer1Turn = true;
        this.isRunning = false;
        this.channel = channel;
        this.player1 = player1;
        this.player2 = player2;
    }

    public synchronized void start() {
        if (!isRunning) {
            isRunning = true;
            this.thread = new Thread(() -> {
                while (isRunning) {
                    try {
                        Thread.sleep(1000); // Sleep for 1 second

                        if (isPlayer1Turn) {
                            if (this.player1Time > 0) {
                                this.player1Time -= 1;
                            } else {
                                // Player 1 has run out of time
                                isRunning = false;
                                System.out.println("Player 2 wins on time!");
                                this.channel.gameOver(player2);

                            }
                        } else {
                            if (this.player2Time > 0) {
                                this.player2Time -= 1;
                            } else {
                                // Player 2 has run out of time
                                isRunning = false;
                                System.out.println("Player 1 wins on time!");
                                this.channel.gameOver(player1);

                            }
                        }

                    } catch (InterruptedException e) {
                        System.out.println("systemClock interrupted");
                        ;
                    }
                }
            });

            this.thread.start();
        }
    }

    public synchronized void stop() {
        isRunning = false;
        this.thread.interrupt();
    }

    public synchronized void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
    }

    public synchronized int getPlayer1Time() {
        return this.player1Time;
    }

    public synchronized int getPlayer2Time() {
        return this.player2Time;
    }

    public synchronized boolean isPlayer1Turn() {
        return isPlayer1Turn;
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public Thread getThread() {
        return this.thread;
    }

    public int getTimeLeft() {
        if (isPlayer1Turn)
            return this.player1Time;
        else
            return this.player2Time;
    }

}
