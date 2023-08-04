package chat;

import java.io.*;
import java.net.Socket;

import javafx.application.Platform;

final class ClientReadThread extends Thread {
	private BufferedReader fromServer;
	private String username;
	private ClientGUI chatClientGUI; // Reference to the GUI

	ClientReadThread(String username, ClientSocket clientSocket, ClientGUI chatClientGUI) {
		this.username = username;
		this.chatClientGUI = chatClientGUI;

		try {
			this.fromServer = new BufferedReader(new InputStreamReader(clientSocket.getSocket().getInputStream()));
		} catch (IOException ex) {
			System.out.println("Error getting input stream from server : " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	@Override
	public void run() {
		// Continuously receive and print messages from the server
		while (!Thread.currentThread().isInterrupted()) {
			try {
				// Wait for message and print it
				String response = this.fromServer.readLine();

				if (response == null) {
					Platform.runLater(() -> {
						chatClientGUI.appendToChatArea("\rConnection lost!");
					});
					return;
				}

				// Update the GUI with the received message
				Platform.runLater(() -> {
					chatClientGUI.appendToChatArea("\r" + response);
				});

				// Print prompt
				System.out.printf("\r[%s]: read thread", this.username);
			} catch (IOException ex) {
				System.out.println("Error reading from server: " + ex.getMessage());
				ex.printStackTrace();
				break;
			}
		}

		System.out.println("ThreadRead was interrupted while working.");

	}
}