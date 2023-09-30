package server;

import GPT.Gpt;

public class ChannelGame extends Channel {
    private final ThreadServer player1, player2;
    private Gpt playerGPT;
    private ChessClock chessClock;
    private boolean isGameFinished = false;
    private ChannelSub channelChat;

    public ChannelGame(String name, ThreadServer player1, ThreadServer player2) {
        super(name);
        this.player1 = player1;
        this.player2 = player2;
        subscribers.add(player1);
        subscribers.add(player2);
        player1.setCurrentChannel(this);
        player2.setCurrentChannel(this);
        player1.receiveMessage("You are playing with " + player2.getUsername());
        player2.receiveMessage("You are playing with " + player1.getUsername());

        this.chessClock = new ChessClock(60 * 10, this, player1, player2);
        this.playerGPT = new Gpt();

        this.channelChat = new ChannelSub(name + "-chat");
    }

    // start
    public void start() {
        initialMessage();
        this.chessClock.start();
    }

    @Override
    public synchronized void subscribe(ThreadServer userThread) {
        userThread.receiveMessage("Hello, " + userThread.getUsername() + "! You are in \"" + this.getName() + "\"!}");
        showHistoryOfMessages(userThread);
        subscribers.add(userThread);
        this.channelChat.subscribe(userThread);
    }

    @Override
    public synchronized void unsubscribe(ThreadServer userThread) {
        if (isHerePlayer(userThread.getUsername())) {
            if (!this.isGameFinished) {
                if (this.player1.equals(userThread))
                    surrender(userThread, this.player2);
                else
                    surrender(userThread, this.player1);
            }

        } else {
            this.channelChat.unsubscribe(userThread);
        }
        subscribers.remove(userThread);
    }

    @Override
    public synchronized void publish(ThreadServer sender, String message) {
        if (isHerePlayer(sender.getUsername())) {
            if (!this.isGameFinished) {

                super.publish(sender, message);

                getResponseGPT(sender);

                // test
                if (message.contains("hihi"))
                    gameOver(sender);
                // System.out.println(this.isGameFinished);
                // ==========

            }
        } else {
            this.channelChat.publish(sender, message);
        }

    }

    private synchronized void getResponseGPT(ThreadServer sender) {

        String response = "[GPT]: " + this.playerGPT.getResponse(super.getMessageHistory());

        super.publish(null, response);

        if (response.toLowerCase().contains(this.playerGPT.secretKey.toLowerCase())) {
            gameOver(sender);
        }

    }

    synchronized void gameOver(ThreadServer sender) {

        // send message to all players
        super.publish(null, "Game over!  " + sender.getUsername() + " has won! Secret key was: "
                + this.playerGPT.secretKey);

        // update scores
        sender.setScore(sender.getScore() + 1);

        // set game finished
        setGameFinished();

        // remove gpt
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage("GPT has been removed."));
        this.playerGPT = null;

        // stop the clock
        this.chessClock.stop();

        // signal GUIs to finish game
        this.player1.signalGameFinished(sender, this.chessClock.getTimeLeft());
        this.player2.signalGameFinished(sender, this.chessClock.getTimeLeft());
    }

    public void surrender(ThreadServer serverThread, ThreadServer winner) {
        // send message to all players that the player has surrendered
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage(serverThread.getUsername() + " has surrendered!"));

        gameOver(winner);
    }

    private void initialMessage() {
        String rules = "Rules: \\n" +
                "1. The secret key is an English word. No numbers or simbols. \\n" +
                "2. You have to make the GPT say it, not you. \\n" +
                "3. There are no rules! This is a school project full of bugs and anything can happen! \\n" +
                "=====================================\\n";

        String welcome = "Welcome to the \"" + super.getName() + "\" channel! \\n" +
                "Game has started! \\n" +
                "You have 10 min to win the game! \\n" +
                "GPTs hint is: " + this.playerGPT.getTheHint() + "\\n" +
                "=============GAME START============\\n";

        // send welcome message to users
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage(rules + welcome));

    }

    public synchronized boolean isGameFinished() {
        return this.isGameFinished;
    }

    private synchronized void setGameFinished() {
        this.isGameFinished = true;
    }

    public void switchTurn() {
        this.chessClock.switchTurn();
        // this.player1.updateTimeLeft();
        // this.player2.updateTimeLeft();
    }

    // ne treba a zao brisati
    public synchronized boolean isHerePlayer(String username) {
        if (isGameFinished())
            return false;
        else
            return this.player1.getUsername().equals(username) || this.player2.getUsername().equals(username);
    }

    public ThreadServer getOpponent(ThreadServer threadServer) {
        if (this.player1.equals(threadServer))
            return this.player2;
        else if (this.player2.equals(threadServer))
            return this.player1;
        else
            return null;
    }

    public void pauseClock() {
        this.chessClock.stop();
    }

    public void resumeClock() {
        this.chessClock.start();
    }

    public int getPlayer1Time() {
        return this.chessClock.getPlayer1Time();

    }

    public int getPlayer2Time() {
        return this.chessClock.getPlayer2Time();
    }

    public Thread getClockThread() {
        return this.chessClock.getThread();
    }

    public Channel getChanelChat() {
        return this.channelChat;
    }

}
