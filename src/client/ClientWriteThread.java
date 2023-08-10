package client;

import java.io.IOException;
import java.io.PrintWriter;

import javafx.scene.control.TextField;

final class ClientWriteThread extends Thread {
	private final String username;
	private PrintWriter toServer;

	private TextField inputField; // Reference to the input field in the GUI

	private final Object lock = new Object();

	ClientWriteThread(String username, _ClientSocket clientSocket, TextField inputField) {
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
		String text;
		String message;
		while (!Thread.currentThread().isInterrupted()) {

			synchronized (lock) {

				try {
					lock.wait();

					// Get text from the GUI input field and send it to the server
					text = inputField.getText();
					message = "[" + this.username + "]: " + text;
					toServer.println("{type:publish; message:" + message + "}");

				} catch (InterruptedException e) {
					System.out.println("WriteThread was interrupted while in waiting state.");
					break;
				}

			}

		}
		System.err.println("WriteThread [" + this.username + "] closing...");
	}
}
