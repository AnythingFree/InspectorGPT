package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ClientGUI extends Application {

	private String name;
	TextArea chatArea;
	TextField inputField;

	// private Thread rt;
	private Thread wt;
	private ThreadMessageListener messageListenerThread;

	private _ClientSocket clientSocket;
	private PrintWriter writer;

	private volatile List<String> userList = null;

	private Stage primaryStage;

	private SceneBuilder sceneBuilder;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {

		// connect to server
		try {
			this.primaryStage = primaryStage;
			this.sceneBuilder = new SceneBuilder(this, primaryStage);

			clientSocket = new _ClientSocket("localhost", 5000);
			System.out.println("Connected to the chat server @ " + 5000);

			// communicate to server
			this.writer = new PrintWriter(clientSocket.getSocket().getOutputStream(), true);
			this.messageListenerThread = new ThreadMessageListener(clientSocket, this);
			messageListenerThread.start();

			// ask for usernames
			writer.println("{type:usernames; data:all}");

			// Create and initiate TextInputDialog for username input
			_UsernameInputDialog usernameInputDialog = new _UsernameInputDialog();
			this.name = usernameInputDialog.getUsername(getUserList());

			// Send username to server
			writer.println("{type:setName; name:" + this.name + "}");

			// main window
			setMainWindow();
			primaryStage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// ============================================================================
	private void setMainWindow() {
		primaryStage.setTitle("Chat Client: " + this.name);
		Scene scene = this.sceneBuilder.getMainScene();
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	// =======================SCENES============================================
	void handleOption1() {
		// Code to handle Option 1
		System.out.println("Option 1 clicked");
		primaryStage.setTitle("Option 1: " + this.name);

		Scene scene = this.sceneBuilder.getChatScene();
		primaryStage.setScene(scene);

		getWt();

		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});

		// subscribe to general channel
		writer.println("{type:subscribe; channelName:general}");
		// ask for usernames in channel
		// writer.println("{type:usernames; data:inChannel; channelName:general}");
	}

	void handleOption2() {
		// Code to handle Option 2
		System.out.println("Option 2 clicked");

		// get free users
		writer.println("{type:usernames; data:free}");
		// zbog kasnjenja
		writer.println("{type:scene; scene:option2}");
	}

	void getScene2() {
		Scene scene = this.sceneBuilder.getOption2Scene(getUserList());
		primaryStage.setScene(scene);

		// Set the close request handler
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	void handleOption3() {
		// Code to handle Option 3
		System.out.println("Option 3 clicked");
	}

	void playGame() {
		// Code to handle Game
		System.out.println("Playing game");
		primaryStage.setTitle("Its on: " + this.name); // Set the window title for Option 1

		Scene scene = this.sceneBuilder.getChatScene();
		primaryStage.setScene(scene);

		getWt();

		// Set the close request handler
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});

		// say hello
		writer.println("{type:gameMode}");

	}

	// =============================================================================
	void showGameRequestDialog(String usernameOpponent) {

		// Show the dialog and handle the user's response
		boolean result = this.sceneBuilder.showGameRequestDialog(usernameOpponent, this.name);
		if (result) {
			// User accepted the game request
			// System.out.println("Prihvatam");
			writer.println("{type:response; answer:yes; opponent:" + usernameOpponent + "}");
		} else {
			// User declined the game request
			// System.out.println("Odbijam");
			writer.println("{type:response; answer:no; opponent:" + usernameOpponent + "}");

		}
	}

	// ====================PRIVATE FUNC=====================================

	private void getWt() {
		this.wt = new ThreadClientWrite(this.name, this.clientSocket, this.inputField);
		wt.start();

	}

	private void closeWT() {
		if (this.wt != null && this.wt.isAlive()) {
			this.wt.interrupt();
			try {
				wt.join();
			} catch (InterruptedException e1) {
				System.out.println("Error u zatvaranju wt: " + e1.getMessage());
			}

		}
	}

	private void closeApp() {
		this.writer.println("close");
		this.clientSocket.close();
		closeWT();
		this.writer.close();
		Platform.exit();
		System.exit(0);
	}

	// ==================PACKAGE VISIBILITY FUNC=================================
	void goBackToMainWindow() {
		writer.println("{type:unsubscribe}");
		closeWT();
		setMainWindow();
	}

	void sendRequest(String selectedItem) {
		writer.println("{type:request; opponent:" + selectedItem + "}");

	}

	void sendMessage() {
		String message = inputField.getText();
		if (!message.isEmpty()) {

			// send to server by invoking or notifying writeThread
			if (this.wt.isAlive())
				((ThreadClientWrite) this.wt).notifyThread();
			else
				System.out.println("wt nije ziv");

			// write to chat
			this.chatArea.appendText("[" + this.name + "]: " + message + "\n");

			// clear the inputfield
			inputField.clear();
		}
	}

	synchronized void setUserList(List<String> _getUserList) {
		this.userList = _getUserList;
	}

	private synchronized List<String> getUserList() {
		return this.userList;
	}

	void appendToChatArea(String string) {
		this.chatArea.appendText(string + "\n");
	}

	void disableInputField() {
		this.inputField.setDisable(true);
	}

	void option2() {
		showNotification("He said no...");
		handleOption2();
	}

	void showNotification(String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Notification: " + this.name);
		alert.setHeaderText("This is a notification");
		alert.setContentText(message);

		alert.showAndWait();
	}

}
