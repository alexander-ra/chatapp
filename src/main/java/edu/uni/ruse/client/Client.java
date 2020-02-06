package edu.uni.ruse.client;

import edu.uni.ruse.utilities.InterfaceLang;
import edu.uni.ruse.utilities.MessagesManager;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Client class that can connect to a server and receive messages from it.
 * 
 * @author Alexander Andreev
 */
public class Client {

	private static final int WAIT_INTERVAL_MS = 1000;
	private static final String MSG_PREFIX_CONREQUEST = "CONNECTION_REQUEST:";
	private static final String MSG_CODE_CONN_ACCEPTED = "CONNECTION_ACCEPTED";
	private static final InterfaceLang DEFAULT_LANGUAGE = InterfaceLang.EN;
	private InterfaceLang language = DEFAULT_LANGUAGE;
	private Socket connection;
	private String name;
	private String serverAddress;
	private int serverPort;
	private DataInputStream din;
	private DataOutputStream dout;
	private String receivedMessage;
	private Color currentColor;

	/**
	 * Default constructor with name variable for the name of the client.
	 */
	public Client(String name) {
		this.name = name;
	}

	/**
	 * Constructor, holding user name and IP of the server to connect to.
	 * 
	 */
	public Client(String name, String ip, int port) {
		this.name = name;
		serverAddress = ip;
		serverPort = port;
	}

	/**
	 * Tries to connect to a server with IP and port retrieved from the object's attributes. If the connection is
	 * successful input and output streams with the server are created.
	 * 
	 * @throws InterruptedException
	 *             if thread is interrupted while trying to connect to the server.
	 */
	public boolean connectToServer() throws InterruptedException {
		boolean succesfullConnection = false;
		try {
			System.out.println("Trying to connect to server " + serverAddress + ":" + serverPort);
			connection = new Socket(serverAddress, serverPort);
			din = new DataInputStream(connection.getInputStream());
			dout = new DataOutputStream(connection.getOutputStream());
			if (acceptedFromServer()) {
				System.out.println("Cleint " + name + " has connected successfully.");
				succesfullConnection = true;
			} else {
				System.out.println("Cleint " + name + " was not able to connect.");
			}
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Server with that address is not found");
		}
		if (!succesfullConnection && connection != null) {
			try {
				connection.close();
				din.close();
				dout.close();
			} catch (IOException e) {
				System.out.println(e);
				System.out.println("IOException while closing socket and streams after unsuccesfull connection.");
			}
		}
		return succesfullConnection;
	}

	/**
	 * Sends a connection request to the server and retrieves an answer from it, showing if the server will accept the
	 * client.
	 * 
	 * @return boolean indicating if the server accepted the client.
	 * @throws InterruptedException
	 */
	public boolean acceptedFromServer() {
		sendMessage(MSG_PREFIX_CONREQUEST + name);
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			System.out.println(e);
			System.out.println("Thread Intrrupted while waiting for answer of connection request");
			Thread.currentThread().interrupt();
		}
		try {
			receiveMessage();
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("IOException while trying to be accepted from server.");
		}
		receivedMessage = MessagesManager.removeColorCodeFromMessage(receivedMessage);
		return MSG_CODE_CONN_ACCEPTED.equals(receivedMessage);
	}

	/**
	 * Blocks the current thread that the object is on, until a connection is made to a server.
	 * 
	 * @throws InterruptedException
	 *             if the thread is interrupted while waiting.
	 */
	public void waitToConnect() throws InterruptedException {
		while (connection == null) {
			Thread.sleep(WAIT_INTERVAL_MS);
		}
	}

	/**
	 * Sends a message to the server trough the connection's output stream.
	 * 
	 * @param message
	 *            to be sent.
	 */
	public void sendMessage(String message) {
		try {
			dout.writeUTF(message);
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("I/O exception while trying to send message to server.");
		}
	}

	/**
	 * Receives a string message trough the socket's input stream.
	 * 
	 * @throws IOException
	 */
	public void receiveMessage() throws IOException {
		receivedMessage = din.readUTF();
		setCurrentColor(MessagesManager.getColorFromMessage(receivedMessage));
		receivedMessage = MessagesManager.removeColorCodeFromMessage(receivedMessage);
	}

	public String getReceivedMessage() {
		return receivedMessage;
	}

	public Socket getConnection() {
		return connection;
	}

	public String getName() {
		return name;
	}

	public InterfaceLang getLanguage() {
		return language;
	}

	public void setLanguage(InterfaceLang language) {
		this.language = language;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	/**
	 * Sets the server to connect's ip if the client is not already connected.
	 * 
	 * @param serverAddress
	 *            of server to connect
	 */
	public void setServerAddress(String serverAddress) {
		if (connection == null) {
			this.serverAddress = serverAddress;
		} else {
			System.out.println(
					"Cannot change ip address while connected to server. Stop the connection to change ip or port.");
		}
	}

	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Sets the server to connect's port if the client is not already connected.
	 * 
	 * @param serverPort
	 *            of server to connect
	 */
	public void setServerPort(int serverPort) {
		if (connection == null) {
			this.serverPort = serverPort;
		} else {
			System.out.println("Cannot change port while connected to server. Stop the connection to change ip or port.");
		}
	}

	public Color getCurrentColor() {
		return currentColor;
	}

	public void setCurrentColor(Color currentColor) {
		this.currentColor = currentColor;
	}
}
