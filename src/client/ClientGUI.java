package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClientGUI extends Application {

	private String name;
	TextArea chatArea;
	TextField inputField;
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

	// ==============FUNCTIONS (zovu se izsceneBuildera)=====
	void handleOption1() {
		System.out.println("Option 1 clicked");
		// ask to change scene to scene1
		writer.println("{type:scene; scene:option1}");
		// subscribe to general channel
		writer.println("{type:subscribe; channelName:general}");
	}

	void handleOption2() {
		System.out.println("Option 2 clicked");
		// get free users
		writer.println("{type:usernames; data:free}");
		// ask to change scene to scene2
		writer.println("{type:scene; scene:option2}");
	}

	void handleOption3() {
		System.out.println("Option 3 clicked");
	}

	// ===========SCENES (zovu se iz THREADMESSAGELISTENER)=================
	void getScene1() {
		primaryStage.setTitle("Option 1: " + this.name);
		Scene scene = this.sceneBuilder.getChatScene();
		primaryStage.setScene(scene);
		getWt();
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	void getScene2() {
		primaryStage.setTitle("Option 2: " + this.name);
		Scene scene = this.sceneBuilder.getOption2Scene(getUserList());
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	private void getGameScene() {
		primaryStage.setTitle("Its on: " + this.name);
		Scene scene = this.sceneBuilder.getChatScene();

		BorderPane root = (BorderPane) scene.getRoot();
		root.setTop(new HBox());

		scene.setRoot(root);
		primaryStage.setScene(scene);
		getWt();
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});

	}

	void playGame() {
		System.out.println("Playing game");

		// playGame se zove iz threadMessageListenera
		getGameScene();
		// start clock

		// say hello
		writer.println("{type:scene; scene:gameMode}");
	}

	// =============================================================================
	void showGameRequestDialog(String usernameOpponent) {

		// Show the dialog and handle the user's response
		boolean result = this.sceneBuilder.showGameRequestDialog(usernameOpponent, this.name);
		if (result) {
			writer.println("{type:response; answer:yes; opponent:" + usernameOpponent + "}");
		} else {
			writer.println("{type:response; answer:no; opponent:" + usernameOpponent + "}");

		}
	}

	// ===================POMOCNE FUNKCIJE================================
	// ====================PRIVATE FUNC======================

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

	void goBackToOption2() {
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
