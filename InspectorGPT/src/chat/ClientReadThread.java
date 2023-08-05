package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
				String response = this.fromServer.readLine(); // this is blocking IO

				if (response == null) {
					Platform.runLater(() -> {
						System.out.println("Connection lost");
						chatClientGUI.appendToChatArea("Connection lost!");
					});
					return;
				}

				// Update the GUI with the received message
				Platform.runLater(() -> {
					chatClientGUI.appendToChatArea(response);
				});

			} catch (IOException ex) {
				System.out.println("ReadThread was interrupted while waiting for server input.");
				break;
			}
		}

		System.err.println("ReadThread [" + this.username + "] closing...");

	}
}