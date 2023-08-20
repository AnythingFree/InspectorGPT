package server;

import java.util.ArrayList;
import java.util.List;

import GPT.Gpt;


 /* 
 * @TODO: razdvojiti u dva kanala/sobe, jedan za general chat, drugi za igru,
 *        treba
 *        rijesiti posmatrace
 */
public class Channel {
    private final String name;
    private final List<ThreadServer> subscribers;
    private final ThreadServer player1, player2;

    private Gpt playerGPT;
    private ArrayList<String> messageHistory;
    private boolean isGameFinished = false;
    private ChessClock chessClock;

    public Channel(String name) {
        this.name = name;
        this.subscribers = new ArrayList<>();
        this.player1 = null;
        this.player2 = null;
        this.playerGPT = null;
        this.chessClock = null;
    } // nemam vremena da pravim poseban kanal za general chat

    public Channel(String name, ThreadServer player1, ThreadServer player2) {
        this.name = name;
        this.subscribers = new ArrayList<>();
        this.player1 = player1;
        this.player2 = player2;
        this.subscribers.add(player1);
        this.subscribers.add(player2);
        player1.setCurrentChannel(this);
        player2.setCurrentChannel(this);
        player1.receiveMessage("You are playing with " + player2.getUsername());
        player2.receiveMessage("You are playing with " + player1.getUsername());

        this.chessClock = new ChessClock(60 * 10, this, player1, player2); // 30 seconds
        this.playerGPT = new Gpt();
        this.messageHistory = new ArrayList<>();
    }

    // start
    public void start() {
        initialMessage();
        this.chessClock.start();
    }

    public synchronized void subscribe(ThreadServer userThread) {
        publish(userThread, userThread.getUsername() + " has joined the chat.");
        subscribers.add(userThread);
    }

    public synchronized void unsubscribe(ThreadServer userThread) {
        publish(userThread, userThread.getUsername() + " has left the chat.");
        checkIfsurrender(userThread);
        subscribers.remove(userThread);

    }

    public synchronized void publish(ThreadServer sender, String message) {
        if (!this.isGameFinished) {

            this.subscribers.stream()
                    .filter(u -> u != sender)
                    .forEach(u -> u.receiveMessage(message));

            // ako je igrica ako nije general chat
            if (this.playerGPT != null) {
                this.messageHistory.add(message);
                getResponseGPT(sender);
            }

            // test
            if (message.contains("hihi"))
                gameOver(sender);
            // System.out.println(this.isGameFinished);
            // ==========

        }

    }

    private synchronized void getResponseGPT(ThreadServer sender) {

        String response = "[GPT]: " + this.playerGPT.getResponse(this.messageHistory);

        this.subscribers.stream()
                .forEach(u -> u.receiveMessage(response));

        this.messageHistory.add(response);

        if (response.toLowerCase().contains(this.playerGPT.secretKey.toLowerCase())) {
            gameOver(sender);
        }

    }

    synchronized void gameOver(ThreadServer sender) {

        // send message to all players
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage("Game over! " + sender.getUsername() + " has won!"));

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

    private void checkIfsurrender(ThreadServer userThread) {
        if (this.isGameFinished || this.name.equals("general"))
            return;

        if (this.player1.equals(userThread))
            surrender(userThread, this.player2);
        else if (this.player2.equals(userThread))
            surrender(userThread, this.player1);

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
                "3. There are no rules! This is a school project full of bugs and anything can happen! \\n";

        String welcome = "Welcome to the \"" + this.name + "\" channel! \\n" +
                "Game has started! \\n" +
                "You have 10 min to win the game! \\n" +
                "GPTs hint is: " + this.playerGPT.getTheHint() + "\\n";

        // send welcome message to users
        this.subscribers.stream()
                .forEach(u -> u.receiveMessage(rules));

        this.subscribers.stream()
                .forEach(u -> u.receiveMessage(welcome));
    }

    public synchronized List<ThreadServer> getSubscribers() {
        return subscribers;
    }

    public synchronized boolean isGameFinished() {
        return this.isGameFinished;
    }

    private synchronized void setGameFinished() {
        this.isGameFinished = true;
    }

    public boolean readyForCleanup() {
        return this.name != "general" && this.subscribers.size() == 0;
    }

    public String getName() {
        return name;
    }

    public void switchTurn() {
        this.chessClock.switchTurn();
        // this.player1.updateTimeLeft();
        // this.player2.updateTimeLeft();
    }

    // ne treba a zao brisati
    public synchronized boolean isHerePlayer(String username) {
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
}
