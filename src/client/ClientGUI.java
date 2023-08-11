package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ClientGUI extends Application {

	private String name;
	private TextArea chatArea;
	private TextField inputField;

	// private Thread rt;
	private Thread wt;
	private ThreadMessageListener messageListenerThread;

	private _ClientSocket clientSocket;
	private PrintWriter writer;

	private volatile List<String> userList;

	private Stage primaryStage;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {

		// connect to server
		try {
			this.primaryStage = primaryStage;

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
			this.name = usernameInputDialog.getUsername(userList);

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
		BorderPane layout = new BorderPane();

		// Create an HBox for the buttons
		HBox buttonBox = new HBox(10); // 10 is the spacing between buttons

		// Create buttons for the options
		Button option1Button = new Button("Option 1");
		option1Button.setOnAction(e -> handleOption1());

		Button option2Button = new Button("Option 2");
		option2Button.setOnAction(e -> handleOption2());

		Button option3Button = new Button("Option 3");
		option3Button.setOnAction(e -> handleOption3());

		// Add buttons to the buttonBox
		buttonBox.getChildren().addAll(option1Button, option2Button, option3Button);
		layout.setTop(buttonBox);

		Scene scene = new Scene(layout, 400, 300);
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	private void handleOption1() {
		// Code to handle Option 1
		System.out.println("Option 1 clicked");

		primaryStage.setTitle("Option 1: " + this.name); // Set the window title for Option 1
		BorderPane layout = new BorderPane();

		// create the chatArea
		this.chatArea = new TextArea();
		this.chatArea.setEditable(false);
		// put it in the center
		layout.setCenter(this.chatArea);

		// create inputField, sendButton
		this.inputField = new TextField();
		Button sendButton = new Button("Send");
		sendButton.setOnAction(e -> sendMessage());
		HBox inputBox = new HBox(this.inputField, sendButton);
		// backButton
		Button backButton = new Button("Back");
		backButton.setOnAction(e -> goBackToMainWindow());
		// Create a VBox to hold both the back button and the input box
		HBox bottomContainer = new HBox(inputBox, backButton);
		bottomContainer.setSpacing(10); // Set spacing between components

		// put them on bottom
		layout.setBottom(bottomContainer);

		Scene scene = new Scene(layout, 400, 300);
		primaryStage.setScene(scene);

		// Dispatch threads
		// this.messageListenerThread.interrupt();
		getWt();

		// Set the close request handler
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});

		// subscribe to general channel
		writer.println("{type:subscribe; channelName:general}");
		// ask for usernames in channel
		writer.println("{type:usernames; data:inChannel; channelName:general}");
		// print usernames
		// appendToChatArea("Connected users: " + this.userList);
	}

	private void handleOption2() {
		// Code to handle Option 2
		System.out.println("Option 2 clicked");

		// signal to server option 2
		writer.println("{type:usernames; data:free}");

		BorderPane userListLayout = new BorderPane();

		// Create a ListView to display the user list
		ListView<String> userListView = new ListView<>();
		userListView.getItems().addAll(userList);

		// Create a button to send a request
		Button sendRequestButton = new Button("Send Request");
		sendRequestButton.setOnAction(e -> {
			String selectedUser = userListView.getSelectionModel().getSelectedItem();
			if (selectedUser != null) {
				sendRequest(selectedUser);

				// Disable the button and show a waiting message
				sendRequestButton.setDisable(true);
				userListView.setDisable(true);

				// Display a waiting message
				Label waitingLabel = new Label("Waiting for " + selectedUser + " to respond...");
				userListLayout.setCenter(waitingLabel);
			}
		});

		userListLayout.setCenter(userListView);
		// backButton
		Button backButton = new Button("Back");
		backButton.setOnAction(e -> goBackToMainWindow());
		// Create a VBox to hold both the back button and the input box
		HBox bottomContainer = new HBox(sendRequestButton, backButton);
		bottomContainer.setSpacing(10); // Set spacing between components

		// put them on bottom
		userListLayout.setBottom(bottomContainer);

		Scene scene = new Scene(userListLayout, 300, 200);
		primaryStage.setScene(scene);

		// Set the close request handler
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	private void handleOption3() {
		// Code to handle Option 3
		System.out.println("Option 3 clicked");
	}

	// ===========================================================================

	public void showGameRequestDialog(String usernameOpponent) {
		// Create and configure the dialog
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Game Request: " + this.name);
		alert.setHeaderText("Incoming Game Request");
		alert.setContentText("You have an incoming game request from " + usernameOpponent + ". Do you want to accept?");

		// Add buttons to the dialog
		ButtonType acceptButton = new ButtonType("Accept");
		ButtonType declineButton = new ButtonType("Decline");
		alert.getButtonTypes().setAll(acceptButton, declineButton);

		// Show the dialog and handle the user's response
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == acceptButton) {
			// User accepted the game request
			// Handle game initiation here
			System.out.println("Prihvatam");
			writer.println("{type:response; answer:yes; opponent:" + usernameOpponent + "}");
		} else {
			// User declined the game request
			System.out.println("Odbijam");
			writer.println("{type:response; answer:no; opponent:" + usernameOpponent + "}");

		}
	}

	// ============================================
	public void playGame() {
		// Code to handle Option 1
		System.out.println("Playing game");

		primaryStage.setTitle("Its on: " + this.name); // Set the window title for Option 1
		BorderPane layout = new BorderPane();

		this.chatArea = new TextArea();
		this.chatArea.setEditable(false);
		layout.setCenter(this.chatArea);

		this.inputField = new TextField();
		Button sendButton = new Button("Send");
		sendButton.setOnAction(e -> sendMessage());
		HBox inputBox = new HBox(this.inputField, sendButton);
		// backButton
		Button backButton = new Button("Back");
		backButton.setOnAction(e -> goBackToMainWindow()); // MOZDA NEKA POTVRDA
		// Create a VBox to hold both the back button and the input box
		HBox bottomContainer = new HBox(inputBox, backButton);
		bottomContainer.setSpacing(10); // Set spacing between components

		// put them on bottom
		layout.setBottom(bottomContainer);

		Scene scene = new Scene(layout, 400, 300);
		primaryStage.setScene(scene);

		// Dispatch threads
		// this.messageListenerThread.interrupt(); // ovo mi mozda pojede prvu poruku za
		// kanal
		getWt();

		// Set the close request handler
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});

		// say hello
		writer.println("{type:gameMode}");

	}

	// =========================================================
	private void getWt() {
		// this.rt = new ClientReadThread(this.name, clientSocket, this);
		this.wt = new ClientWriteThread(this.name, this.clientSocket, this.inputField);
		// rt.start();
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

	private void goBackToMainWindow() {
		writer.println("{type:unsubscribe}");
		closeWT();
		setMainWindow();
	}

	private void sendRequest(String selectedItem) {
		writer.println("{type:request; opponent:" + selectedItem + "}");

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
			this.chatArea.appendText("[" + this.name + "]: " + message + "\n");

			// clear the inputfield
			inputField.clear();
		}
	}

	public void setUserList(List<String> _getUserList) {
		this.userList = _getUserList;
	}

	public void appendToChatArea(String string) {
		this.chatArea.appendText(string + "\n");
	}

	public void disableInputField() {
		this.inputField.setDisable(true);
	}

	public void option2() {
		showNotification("He said no...");
		handleOption2();
	}

	public void showNotification(String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Notification: " + this.name);
		alert.setHeaderText("This is a notification");
		alert.setContentText(message);

		alert.showAndWait();
	}

}
