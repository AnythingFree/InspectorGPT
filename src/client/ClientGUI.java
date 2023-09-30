package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Text;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ClientGUI extends Application {

	private String name;
	TextArea chatArea;
	TextArea chatArea2;
	TextField inputField;
	private Thread wt;
	private ThreadMessageListener messageListenerThread;
	private _ClientSocket clientSocket;
	private PrintWriter writer;
	private volatile List<String> userList = null;
	private Stage primaryStage;
	private SceneBuilder sceneBuilder;
	volatile Label player1Time;
	volatile Label player2Time;
	private ChessClockClient clock;
	TableView<_LeaderboardEntry> leaderboardTable;

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

	// ============================ Glavni prozor / Meni ====================
	private void setMainWindow() {
		primaryStage.setTitle("Client: " + this.name);
		Scene scene = this.sceneBuilder.getMainScene();
		primaryStage.setScene(scene);

		getUpdateTableThread(scene);

		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});

	}

	private void getUpdateTableThread(Scene scene) {
		Thread updateTableThread = new Thread(() -> {
			while (primaryStage.getScene() == scene) {
				try {

					refreshTableCommand();
					Thread.sleep(1000);

				} catch (InterruptedException e) {
					System.out.println("updateTableThread interrupted");
				}
			}
			System.out.println("updateTableThread stopped");
		});
		updateTableThread.start();
	}

	// ==============FUNCTIONS (zovu se iz sceneBuildera)=====
	void handleOption1() {
		System.out.println("Option 1 (General Chat) clicked!");
		// ask to change scene to scene1
		writer.println("{type:scene; scene:option1}");
		// subscribe to general channel
		writer.println("{type:subscribe; channelName:general}");
	}

	void handleOption2() {
		System.out.println("Option 2 (Play Game) clicked");
		// get free users
		writer.println("{type:usernames; data:free}");
		// ask to change scene to scene2
		writer.println("{type:scene; scene:option2}");
	}

	void handleOption3() {
		System.out.println("Option 3 (Watch other Games) clicked");
		// get free channels
		writer.println("{type:channels; data:free}");
		// ask to change scene to scene3
		writer.println("{type:scene; scene:option3}");
	}

	// ===========SCENES (zovu se iz THREADMESSAGELISTENER)=================
	void getScene1() {
		primaryStage.setTitle("General Chat: " + this.name);
		Scene scene = this.sceneBuilder.getChatScene();
		primaryStage.setScene(scene);
		getWt();
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	void getScene2() {
		primaryStage.setTitle("Play game: " + this.name);
		Scene scene = this.sceneBuilder.getOption2Scene(getUserList());
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	// kada u Scene2 izabere igraca aktivira se ova scena: playGameScene
	void playGameScene() {
		System.out.println("Playing game");

		// playGame se zove iz threadMessageListenera
		getGameScene();

		// start clock
		startClock();

		// say hello
		writer.println("{type:scene; scene:gameMode}");
	}

	void getScene3() {
		primaryStage.setTitle("Watch other Games (menu): " + this.name);
		Scene scene = this.sceneBuilder.getOption3Scene(this.channels);
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});
	}

	void watchChatScene(String selectedChannel) {
		primaryStage.setTitle("Watch other Games: " + this.name);
		Scene scene = this.sceneBuilder.getWatchScene();
		primaryStage.setScene(scene);

		// subscribe to channel
		writer.println("{type:subscribe; channelName:" + selectedChannel + "}");
		System.out.println("subscribed to channel: " + selectedChannel);
		getWt();
	}

	// ====== pomocne za playGameScene jer izgleda ljepse ====================

	private void getGameScene() {
		primaryStage.setTitle("Its on: " + this.name);
		Scene scene = this.sceneBuilder.getGameScene();

		primaryStage.setScene(scene);
		getWt();
		primaryStage.setOnCloseRequest(e -> {
			closeApp();
		});

	}

	private void startClock() {
		// start thread for labels
		this.clock = new ChessClockClient(this, 60 * 10); // koliko vremena bi trebao da dobije sa servera,
															// ne da bude hardkodovano
		this.clock.startUpdateThread();
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

	// ====== dijalog koji pita da li zelis da igras igru sa igracem.. ===========
	void showGameRequestDialog(String usernameOpponent) {

		// Show the dialog and handle the user's response
		boolean result = this.sceneBuilder.showGameRequestDialog(usernameOpponent, this.name);
		if (result) {
			writer.println("{type:response; answer:yes; opponent:" + usernameOpponent + "}");
		} else {
			writer.println("{type:response; answer:no; opponent:" + usernameOpponent + "}");

		}
	}

	// ======================================================
	void goBackToMainWindow() {
		if (this.primaryStage.getTitle().contains("Its on")) {
			// Ask for confirmation before proceeding
			getConfirmationDialog();
		} else {
			// No confirmation needed, proceed with the action
			writer.println("{type:unsubscribe}");
			closeWT();
			setMainWindow();
		}
	}

	private void getConfirmationDialog() {
		Alert confirmationDialog = new Alert(AlertType.CONFIRMATION);
		confirmationDialog.setTitle("Confirmation");
		confirmationDialog.setHeaderText("Confirm Action");
		confirmationDialog.setContentText("Are you sure you want to go back to the main window?");

		// Show the confirmation dialog and wait for the user's response
		confirmationDialog.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				// User confirmed, proceed with the action
				writer.println("{type:unsubscribe}");
				closeWT();
				setMainWindow();
			} else {
				// User canceled, do nothing or handle as needed
			}
		});
	}

	void sendRequest(String selectedItem) {
		writer.println("{type:request; opponent:" + selectedItem + "}");

	}

	void sendMessage() {
		send(this.chatArea);
	}

	void sendMessage2() {
		send(this.chatArea2);
	}

	private void send(TextArea area) {
		String message = inputField.getText();
		if (!message.isEmpty()) {

			// if in game mode, switch player
			if (this.primaryStage.getTitle().contains("Its on")) {
				switchPlayer();
			}

			// send to server by invoking or notifying writeThread
			if (this.wt.isAlive())
				((ThreadClientWrite) this.wt).notifyThread();
			else
				System.out.println("wt nije ziv");

			String parsedMessage = parseMessageToFitScreen(message);

			// write to chat
			area.appendText("[" + this.name + "]: " + parsedMessage + "\n");

			// clear the inputfield
			inputField.clear();

		}
	}

	private String parseMessageToFitScreen(String message) {
		// parse message so that on every 50th character it adds \n
		String parsedMessage = "";
		int counter = 0;
		for (int i = 0; i < message.length(); i++) {

			if (counter >= 50 && message.charAt(i) == ' ') {
				parsedMessage += "\n";
				counter = 0;
			} else {
				if (message.charAt(i) == '\n') {
					counter = 0;
				}
				parsedMessage += message.charAt(i);
				counter++;
			}

		}
		return parsedMessage;
	}

	synchronized void setUserList(List<String> _getUserList) {
		this.userList = _getUserList;
	}

	private synchronized List<String> getUserList() {
		return this.userList;
	}

	void appendToChatArea(String string, TextArea area) {
		string = string.replace("\\n", "\n");
		String parsedMessage = parseMessageToFitScreen(string);
		area.appendText(parsedMessage + "\n");
	}

	void disableInputField() {
		this.inputField.setDisable(true);
	}

	void enableInputField() {
		this.inputField.setDisable(false);
	}

	void goBackToOption2() {
		showNotification("He said no...");
		handleOption2();
	}

	void showNotification(String message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Notification: " + this.name);
		alert.setHeaderText("This is a notification");

		message = message.replace("\\n", "\n");
		alert.setContentText(message);

		alert.showAndWait();
	}

	synchronized void updateTimeLabels(String time1, String time2) {
		int time1Int = Integer.parseInt(time1);
		int time2Int = Integer.parseInt(time2);
		this.player1Time.setText(" Player1: " + _getTimeFormatting(time1Int));
		this.player2Time.setText(" Player2: " + _getTimeFormatting(time2Int));

		this.clock.updateTime(time1Int, time2Int);
		this.clock.updateTurn();
	}

	private String _getTimeFormatting(int seconds) {
		int minutes = seconds / 60;
		int remainingSeconds = seconds % 60;

		return String.format("%02d:%02d", minutes, remainingSeconds);
	}

	void switchPlayer() {
		writer.println("{type:gameOptions; option:switchPlayer}");
	}

	void disableButtons() {
		if (this.primaryStage.getTitle().contains("Its on")) {
			this.inputField.setDisable(true);
			Button timerButton = (Button) this.primaryStage.getScene().lookup("#TimerButton");
			timerButton.setDisable(true);
			Button foundButton = (Button) this.primaryStage.getScene().lookup("#SendButton");
			foundButton.setDisable(true);
		}
	}

	void enableButtons() {
		this.inputField.setDisable(false);
		Button timerButton = (Button) this.primaryStage.getScene().lookup("#TimerButton");
		timerButton.setDisable(false);
		Button foundButton = (Button) this.primaryStage.getScene().lookup("#SendButton");
		foundButton.setDisable(false);
	}

	public void stopClock(String timeLeft) {
		int time = Integer.parseInt(timeLeft);
		this.clock.stop(time);
	}

	public void refreshTableCommand() {
		writer.println("{type:refreshTable}");
	}

	public void refreshTable(String dataTable) {

		ArrayList<_LeaderboardEntry> entries = getEntries(dataTable);
		// Clear the existing data in the TableView
		this.leaderboardTable.getItems().clear();

		// Add new entries to the TableView
		this.leaderboardTable.getItems().addAll(entries);

		// refreash the table
		this.leaderboardTable.refresh();
	}

	private ArrayList<_LeaderboardEntry> getEntries(String dataTable) {
		ArrayList<_LeaderboardEntry> entries = new ArrayList<_LeaderboardEntry>();
		// iterate through dataTable {usernames:scores}, add username and socres to
		// entries
		dataTable = dataTable.replaceAll("\\[", "");
		dataTable = dataTable.replaceAll("\\]", "");

		String[] keyValuePairs = dataTable.split(";");

		for (String pair : keyValuePairs) {
			String[] keyValue = pair.split(":", 2);

			if (!keyValue.equals("") && keyValue.length == 2) {
				String username = keyValue[0].trim();
				String score = keyValue[1].trim();
				if (!username.equals("null"))
					entries.add(new _LeaderboardEntry(username, Integer.valueOf(score)));
			}
		}

		return entries;
	}

	// ===========================================================

	private List<String> channels;

	public List<String> getChannelsList() {
		return this.channels;
	}

	public void setChannels(List<String> channelNames) {
		TableView<String> channelTableView = new TableView<>();

		TableColumn<String, String> imeKanalaColumn = new TableColumn<>();
		imeKanalaColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));

		ObservableList<String> channels = FXCollections.observableArrayList(channelNames);
		channelTableView.setItems(channels);

		this.channels = channelNames;
		// System.out.println("getChannels" + " " + channelNames);
	}

	public void watchChatScene(){
		this.sceneBuilder.getChatScene();
	}
	public void setTitle(String string) {
		this.primaryStage.setTitle(string);

	}
}
