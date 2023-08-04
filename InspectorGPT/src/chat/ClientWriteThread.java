package chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javafx.scene.control.TextField;

final class ClientWriteThread extends Thread {
	private final String username;
	private PrintWriter toServer;

	private TextField inputField; // Reference to the input field in the GUI

	private final Object lock = new Object();

	ClientWriteThread(String username, ClientSocket clientSocket, TextField inputField) {
		this.username = username;
		this.inputField = inputField;

		try {
			this.toServer = new PrintWriter(clientSocket.getSocket().getOutputStream(), true);
			this.toServer.flush();
		} catch (IOException ex) {
			System.out.println("Error getting output stream from user : " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	void notifyThread() {
		synchronized (lock) {
			lock.notify();
		}
	}

	@Override
	public void run() {
		// First send username, server expects that info
		this.toServer.println(this.username);

		String text;
		while (!Thread.currentThread().isInterrupted()) {

			synchronized (lock) {

				try {
					lock.wait();
				} catch (InterruptedException e) {
					System.out.println("ThreadWrite was interrupted while waiting.");
					break;

				}

				// Get text from the GUI input field
				text = inputField.getText();

				if (!text.isEmpty())
					// sent to server
					this.toServer.println(text);

				// Clear the input field
				inputField.clear();

			}

		}
	}
}
