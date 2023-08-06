package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ClientGUI extends Application {

	private String name;
	private TextArea chatArea;
	private TextField inputField;

	private Thread rt;
	private Thread wt;

	private ClientSocket clientSocket;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {

		// connect to server
		try {

			clientSocket = new ClientSocket("localhost", 5000);

			System.out.println("Connected to the chat server @ " +  5000);

			// get connected users
			BufferedReader read = new BufferedReader(new InputStreamReader(clientSocket.getSocket().getInputStream()));
			String usernames = read.readLine();

			// make connected users list
			List<String> userList = getUserList(usernames);

			// Create and initiate TextInputDialog for username input
			textInputDialogForUsername(userList);

			// main window
			setMainWindow(primaryStage);
			primaryStage.show();

			// Dispatch threads
			getWt(clientSocket);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void setMainWindow(Stage primaryStage) {

		primaryStage.setTitle("Chat Client");
		BorderPane layout = new BorderPane();

		chatArea = new TextArea();
		chatArea.setEditable(false);
		layout.setCenter(chatArea);

		inputField = new TextField();
		Button sendButton = new Button("Send");
		sendButton.setOnAction(e -> sendMessage());
		HBox inputBox = new HBox(inputField, sendButton);
		layout.setBottom(inputBox);

		Scene scene = new Scene(layout, 400, 300);
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(e -> {

			this.clientSocket.close();

			this.rt.interrupt();
			this.wt.interrupt();

			Platform.exit();
			System.exit(0);
		});
	}

	private void textInputDialogForUsername(List<String> userList) {

		// create and manage usernameDialog
		TextInputDialog usernameDialog = new TextInputDialog();
		usernameDialog.setTitle("Username Input");
		usernameDialog.setHeaderText("Usernames in use: " + userList);
		usernameDialog.setContentText("Please enter your username:");

		final boolean[] validUsername = { false };
		while (!validUsername[0]) {

			// Show the username dialog and wait for the response
			usernameDialog.showAndWait().ifPresent(username -> {

				if (username.isEmpty()) {
					// If username is empty, show the dialog again
					usernameDialog.setHeaderText("Username cannot be empty.");

				} else if (userList.contains(username)) {
					// If usernmae is taken, show the dialog again
					usernameDialog.setHeaderText("Username is taken. Please choose a different username.");

				} else {
					// Set the entered username and exit the loop
					this.name = username;
					validUsername[0] = true;
				}

			});

			// If the user clicked Cancel, exit the application
			if (usernameDialog.getResult() == null) {
				Platform.exit();
				System.exit(0);
			}
		}
	}

	private List<String> getUserList(String usernames) {
		List<String> userList = new ArrayList<>();
		if (!usernames.equals("[null]")) {

			// Remove brackets
			usernames = usernames.substring(1, usernames.length() - 1);

			// Split by ", " to get a list of usernames
			userList = new ArrayList<>(Arrays.asList(usernames.split(", ")));

		} else {
			userList = new ArrayList<>();
		}
		return userList;
	}

	private void getWt(ClientSocket clientSocket) {
		this.rt = new ClientReadThread(this.name, clientSocket, this);
		this.wt = new ClientWriteThread(this.name, clientSocket, this.inputField);
		rt.start();
		wt.start();

	}

	private void sendMessage() {
		String message = inputField.getText();
		if (!message.isEmpty()) {

			// send to server by invoking or notifying writeThread
			if (this.wt.isAlive())
				((ClientWriteThread) this.wt).notifyThread();
			else
				System.out.println("wt nije ziv");

			// write to chat
			appendToChatArea("[" + this.name + "]: " + message + "\n");

			// clear the inputfield
			inputField.clear();
		}
	}

	public void appendToChatArea(String message) {
		chatArea.appendText(message + "\n");
	}
}
