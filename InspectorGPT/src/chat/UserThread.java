package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

final class UserThread extends Thread {
	private final ChatServer server;
	private final Socket sock;
	private BufferedReader fromUser;
	private PrintWriter toUser;
	private String username;

	private List<Channel> subscribedChannels = new ArrayList<>();

	private volatile boolean requestPending = false;
	private Channel currentChannel;
	private boolean exit = false;

	UserThread(Socket socket, ChatServer server) {
		this.sock = socket;
		this.server = server;
		try {
			this.fromUser = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
			this.toUser = new PrintWriter(this.sock.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
    public void run() {
        try {


            // send connected users list
            List<String> usernames = this.server.getUserNames();
            this.sendMessage(usernames.toString());

            // get username
            this.username = fromUser.readLine();  // ovo dobijas od clientwriteThreada
            System.err.println("Client username: " + username);

            // send initial Hello-message to user
			this.sendMessage("Hello, " + username + "!");
			this.sendMessage("Users connected: " + usernames.toString());

            //=============================================
            // menue
            /*
            while(this.exit == false){
                menue();
            }
            */
            chatWithEveryone();
            sendClientMessages();

        } catch (IOException ex) {
            System.out.println("Error in UserThread: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // Remove user from set
        	this.server.remove(this);

            // Close socket
            try {
                this.sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	private void menue() throws IOException { // ovo ce morati biti tred jer se rad mora prekinuti kad dodje zahtjev za
												// igru
		while (!requestPending && !this.exit) { // dok nema zahtjeva za igru

			this.sendMessage("1. Choose user to play a game\n2. Chat with everyone\n3. Exit\n4. pomocni");
			int choiceInt = getValidInteger();

			switch (choiceInt) {
			case 1:
				playGame();
				break;
			case 2:
				chatWithEveryone();
				break;
			case 3:
				this.exit = true;
				break;
			case 4:
				break;
			default:
				this.sendMessage("Invalid choice.");
				break;
			}

		}
		if (!this.exit) {
			sendClientMessages();
			this.requestPending = false;
		}
		System.out.println("izasao iz menija");



	}

	private void chatWithEveryone() {
		// subscribe to general channel
		Channel channel = this.server.getChannelByName("general");
		channel.subscribe(this);
		this.currentChannel = channel;
		this.requestPending = true;
	}

	private void playGame() {
		// choose user to play a game with
		String usernameOpponent = getUsernameOpponent();

		// send request to opponent
		boolean accepted = this.server.sendRequestTo(usernameOpponent, this.username);
		if (accepted) {
			this.requestPending = true;
			this.sendMessage("Request accepted.");
		} else
			this.sendMessage("Request rejected.");
	}

	private String getUsernameOpponent() {

		List<String> usernames = getUserNamesOfOpponents();

		// wait for users to connect
		usernames = ifNoUsersConnectedWait(usernames);

		// ask user to choose opponent
		this.sendMessage("Chose from 0 to " + (usernames.size() - 1) + ". \nConnected users: " + usernames);

		// get index of opponent
		int userIndex = getValidInteger();
		while (userIndex < 0 || userIndex >= usernames.size()) {
			this.sendMessage(
					"Invalid index. Chose from 0 to " + (usernames.size() - 1) + ". Connected users: " + usernames);
			userIndex = getValidInteger();
		}
		return usernames.get(userIndex);
	}

	private int getValidInteger() {

		int userIndex = -1;
		while (!this.exit) {
			this.sendMessage(" Enter an integer: ");
			try {
				userIndex = Integer.parseInt(readFromUser());
				break; // Exit loop on successful parsing
			} catch (NumberFormatException e) {
				this.sendMessage("Invalid input. Please enter a valid integer.");
			}
		}
		return userIndex;
	}

	private List<String> ifNoUsersConnectedWait(List<String> usernames) {
		if (usernames.size() == 0) {
			this.sendMessage("No other users connected. Wait for someone to connect.");
		}
		while (usernames.size() == 0) { // ovo se moze rijesiti i sa wait() i notify()
			try {
				sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			usernames = getUserNamesOfOpponents();

		}
		return usernames;
	}

	/*
	 * private void getUsername(List<String> usernames) throws IOException { // Ask
	 * for username until it is unique this.sendMessage("Enter your username: ");
	 * while (usernames.contains(this.username = fromUser.readLine())) {
	 * this.sendMessage("Username is taken. Please choose a different username: ");
	 * }
	 *
	 * }
	 */

	private List<String> getUserNamesOfOpponents() {
		List<String> usernames = this.server.getUserNames();
		usernames.remove(this.username);
		return usernames;
	}

	private void sendClientMessages() throws IOException {
		String clientMessage;
		do {
			// Read message from user
			clientMessage = fromUser.readLine();
			if (clientMessage == null)
				break;

			// Broadcast the message
			this.currentChannel.publish(this, "[" + this.username + "]: " + clientMessage);

		} while (!clientMessage.equals("bye"));

		// remove the user and subscribe to menu
		this.currentChannel.unsubscribe(this);
		// this.server.subscribeToMenu(this);
	}

	void sendMessage(String message) {
		if (this.toUser != null)
			this.toUser.println(message);
		else
			System.out.println("Error sending message to user " + this.username);
	}

	public void receiveMessage(String message) {
		this.sendMessage(message);
	}

	public String readFromUser() {
		String response = null;
		try {
			response = fromUser.readLine();
		} catch (IOException e) {
			System.out.println("Error reading from user: " + e.getMessage());
			this.exit = true;
		}
		return response;
	}

	String getNickname() {
		return this.username;
	}

	public boolean receveRequest(String message, String usernameOpponent) {

		this.requestPending = true;

		// show request to user
		this.sendMessage(message);

		// get response
		String response = readFromUser();
		System.out.println(response);
		if (response.equals("yes")) {
			// get opponent
			UserThread opponent = this.server.getUserByName(usernameOpponent);

			// create channel and subscribe users to it
			Channel channel = new Channel(this.username + " " + usernameOpponent, this, opponent);
			this.server.addChannel(channel);

			// set current channel
			opponent.currentChannel = channel;
			this.currentChannel = channel;

			return true;
		}

		else
			return false;

	}

	private void initiateRequest() {

	}

	public void subscribeToChannel(String channelName) {
		Channel channel = this.server.getChannelByName(channelName);
		if (channel != null)
			this.currentChannel = channel;
		else
			this.sendMessage("Channel " + channelName + " does not exist.");

	}

	public void unsubscribeFromChannel(Channel channel) {
		subscribedChannels.remove(channel);
		channel.unsubscribe(this);
	}

	public void sendMessageToChannel(String channelName, String message) {
		// Find the channel and publish the message
		// Handle interruptions if necessary
	}

	public String getUsername() {
		return this.username;
	}
}