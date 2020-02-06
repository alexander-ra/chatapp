package edu.uni.ruse.utilities;

import edu.uni.ruse.server.Server;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * MessagesManager class that operates the messages between the server and it's clients.
 * 
 * @author Alexander Andreev
 */
public class MessagesManager {

	public static final int UNPROCESSED_MESSAGES_CAPACITY = 512;
	public static final String MSG_PREFIX_CONREQUEST = "CONNECTION_REQUEST:";
	public static final String MSG_PREFIX_ADDUSER = "ADD_USER:";
	public static final String MSG_PREFIX_REMOVEUSER = "REMOVE_USER:";
	public static final String MSG_CODE_CONN_ACCEPTED = "CONNECTION_ACCEPTED";
	public static final String MSG_CODE_CONN_DECLINED = "CONNECTION_DECLINED";
	public static final String MSG_CODE_REFRESH_USERLIST = "REFRESH_USERLIST";
	public static final String MSG_CODE_CHANGE_LANG = "CHANGE_LANGUAGE:";
	public static final String MSG_CODE_WHISPER = "/w";
	private Server server;
	private volatile ArrayBlockingQueue<String> unprocessedClientMessages;
	private volatile ArrayBlockingQueue<String> unprocessedServerMessages;

	/**
	 * Constructor for the class.
	 * 
	 * @param parent
	 *            server that the manager will operate it's messages.
	 */
	public MessagesManager(Server parent) {
		server = parent;
		unprocessedClientMessages = new ArrayBlockingQueue<>(UNPROCESSED_MESSAGES_CAPACITY, true);
		unprocessedServerMessages = new ArrayBlockingQueue<>(UNPROCESSED_MESSAGES_CAPACITY, true);
	}

	/**
	 *  Removes the color code before a message if it is irrelevant for its use
	 * @param message with color code
	 * @return message without color code
	 */
	public static Color getColorFromMessage(String message) {
		String colorString = message.substring(message.indexOf("*") + 1, message.indexOf("*", message.indexOf("*") + 1));
		if (colorString.contains("awt")) {
			return getColorFromJavaAwtString(colorString);
		} else {
			try {
				return (Color)Color.class.getField(colorString).get(null);
			} catch (IllegalAccessException e) {
				System.out.println(e);
			} catch (NoSuchFieldException e) {
				System.out.println(e);
				System.out.println("A color for a message was received that cannot be recognized");
			}
		}

		return Color.BLACK;
	}

	/**
	 *  Removes the color code before a message if it is irrelevant for its use
	 * @param message with color code
	 * @return message without color code
	 */
	public static String removeColorCodeFromMessage(String message) {
		int messageBeginning = message.indexOf("*", message.indexOf("*") + 1);
		return message.substring(messageBeginning + 1);
	}

	/**
	 * Sends a message to a client, connected to the server.
	 * 
	 * @param message
	 *            to be send
	 * @param receiver
	 *            to receive the message
	 * @throws IOException
	 *             if the output stream between the server and the client is damaged.
	 */
	public void sendMessageToClient(String message, Socket receiver) throws IOException {
		sendMessageToClient(message, receiver, Color.BLACK);
	}

	/**
	 * Sends a message to a client, connected to the server.
	 *
	 * @param message
	 *            to be send
	 * @param receiver
	 *            to receive the message
	 * @param messageColor
	 * 			  the color of the message to be displayed on the displayed on the client
	 * @throws IOException
	 *             if the output stream between the server and the client is damaged.
	 */
	public void sendMessageToClient(String message, Socket receiver, Color messageColor) throws IOException {
		message = "*" + messageColor + "*" + message;
		DataOutputStream dOut = new DataOutputStream(receiver.getOutputStream());
		dOut.writeUTF(message);
	}

	/**
	 * Gets the interface language that a specific user is on.
	 * 
	 * @param user
	 *            to get the language
	 * @return
	 */
	public InterfaceLang getLangFromSocket(Socket user) {
		return server.getLangPreferences().get(user);
	}

	/**
	 * Starts a new thread that sends a specific message to all connected to the server users.
	 * 
	 * @param message
	 *            to be send
	 */
	public void sendMessageToAllUsers(String message) {
		sendMessageToAllUsers(message, Color.BLACK);
	}

	/**
	 * Starts a new thread that sends a specific message to all connected to the server users.
	 *
	 * @param message
	 *            to be send
	 * @param messageColor
	 * 			  color of message to be send
	 */
	public void sendMessageToAllUsers(String message, Color messageColor) {
		Thread thread = new Thread(() -> server.getNamesToConnections().forEach((name, connection) -> {
			try {
				sendMessageToClient(message, connection, messageColor);
			} catch (Exception e) {
				server.removeUser(name);
				sendMessageToServerFrame(
						server.getCurrentTime() + "Cannot reach user " + name + ". User will be removed.");
				System.out.println(e);
				System.out.println(
						"Exception while trying to read a message from client " + name + ". Connection will be removed");
			}
		}));
		thread.start();
	}

	/**
	 * Starts a new thread that sends a specific message in two languages to all connected to the server users. Only one
	 * of the messages is send to each user, considering the current language that user has chosen.
	 * 
	 * @param bulMessage
	 *            message in Bulgarian
	 * @param engMessage
	 *            message in English
	 */
	public void sendBilingualMessageToAllUsers(String bulMessage, String engMessage) {
		Thread thread = new Thread(() -> server.getNamesToConnections().forEach((name, connection) -> {
			try {
				if (getLangFromSocket(connection) == InterfaceLang.BG) {
					sendMessageToClient(bulMessage, connection);
				} else if (getLangFromSocket(connection) == InterfaceLang.EN) {
					sendMessageToClient(engMessage, connection);
				}
			} catch (Exception e) {
				server.removeUser(name);
				sendMessageToServerFrame(
						server.getCurrentTime() + "Cannot reach user " + name + ". User will be removed.");
				System.out.println(e);
				System.out.println("Exception while trying to read a message from client "
						+ name + ". Connection will be removed");
			}
		}));
		thread.start();
	}

	/**
	 * Starts a new thread that sends a specific message in two languages to all connected to the server users. Only one
	 * of the messages is send to each user, considering the current language that user has chosen.
	 *
	 * @param bulMessage
	 *            message in Bulgarian
	 * @param engMessage
	 *            message in English
	 */
	public void sendBilingualMessageToAllUsers(String bulMessage, String engMessage, Color messageColor) {
		Thread thread = new Thread(() -> server.getNamesToConnections().forEach((name, connection) -> {
			try {
				if (getLangFromSocket(connection) == InterfaceLang.BG) {
					sendMessageToClient(bulMessage, connection, messageColor);
				} else if (getLangFromSocket(connection) == InterfaceLang.EN) {
					sendMessageToClient(engMessage, connection, messageColor);
				}
			} catch (Exception e) {
				server.removeUser(name);
				sendMessageToServerFrame(
						server.getCurrentTime() + "Cannot reach user " + name + ". User will be removed.");
				System.out.println(e);
				System.out.println("Exception while trying to read a message from client "
						+ name + ". Connection will be removed");
			}
		}));
		thread.start();
	}

	/**
	 * Updates the current language, that the user is on.
	 * 
	 * @param user
	 *            changed language
	 * @throws IOException
	 */
	public void changeUserLanguage(Socket user) {
		try {
			if (getLangFromSocket(user) == InterfaceLang.BG) {
				server.getLangPreferences().put(user, InterfaceLang.EN);
				sendMessageToClient(server.getCurrentTime() + "Language changed to english.", user);
			} else if (getLangFromSocket(user) == InterfaceLang.EN) {
				server.getLangPreferences().put(user, InterfaceLang.BG);
				sendMessageToClient(server.getCurrentTime() + "Езикът е зададен на български.", user);
			}
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("IOException while trying to change language preferences of user");
		}
	}

	/**
	 * Sends a message to the message area of the server's interface.
	 * 
	 * @param message
	 */
	public void sendMessageToServerFrame(String message) {
		try {
			unprocessedServerMessages.put(message);
		} catch (InterruptedException e) {
			System.out.println(e);
			System.out.println("InterruptedException while send message to server frame.");
		}
	}

	/**
	 * Sends a message to the server's interface, telling it to update the user list.
	 */
	public void sendRefreshMessageToServerFrame() {
		sendMessageToServerFrame(MSG_CODE_REFRESH_USERLIST);
	}

	/**
	 * Sends a message to the server's interface, telling that a user has connected successfully.
	 * 
	 * @param user
	 *            that joined the server.
	 */
	public void sendNewUserMessageToServerFrame(String user) {
		sendMessageToServerFrame(server.getCurrentTime() + "Client " + user + " has connected successfully.");
	}

	/**
	 * Sends a message to the server's interface, telling that a user was added to the list of users.
	 * 
	 * @param user
	 *            that was added to the list.
	 */
	public void sendUserAddedToListMessageToServerFrame(String user) {
		sendMessageToServerFrame(server.getCurrentTime() + "Client " + user + " was added to the list of users.");
	}

	/**
	 * Sends accepted code to a specific user, telling the user that it was accepted to the server.
	 * 
	 * @param client
	 *            that was accepted
	 * @throws IOException
	 */
	public void sendAcceptedMessageToClient(Socket client) throws IOException {
		sendMessageToClient(MSG_CODE_CONN_ACCEPTED, client);
	}

	/**
	 * Sends declined code to a specific user, telling the user that was declined to join the server.
	 * 
	 * @param client
	 *            that was declined
	 * @throws IOException
	 */
	public void sendDeclinedMessageToClient(Socket client) throws IOException {
		sendMessageToClient(MSG_CODE_CONN_DECLINED, client);
	}

	/**
	 * Send to everyone that a new user has joined the server.
	 * 
	 * @param user
	 *            that joined.
	 */
	public void sendNewUserMessageToEveryone(String user) {
		sendBilingualMessageToAllUsers(server.getCurrentTime() + "Потребител " + user + " се присъедини към сървъра.",
				server.getCurrentTime() + "User " + user + " joined the server.");
	}

	/**
	 * Sends a message to all users, telling the clients to add a specific user to their user list.
	 * 
	 * @param userToBeAdded
	 */
	public void sendAddUserMessageToEveryone(String userToBeAdded) {
		sendMessageToAllUsers(MSG_PREFIX_ADDUSER + userToBeAdded);
	}

	/**
	 * Send to everyone that a user has left the server.
	 * 
	 * @param user
	 *            that left
	 */
	public void sendUserLeftMessageToEveryone(String user) {
		sendBilingualMessageToAllUsers(server.getCurrentTime() + "Потребител " + user + " напусна сървъра.",
				server.getCurrentTime() + "User " + user + " has left the server.");
	}

	/**
	 * Sends a message to all users, telling the clients to remove a specific user from their user list.
	 * 
	 * @param userToBeRemoved
	 */
	public void sendRemoveUserMessageToEveryone(String userToBeRemoved) {
		sendMessageToAllUsers(MSG_PREFIX_REMOVEUSER + userToBeRemoved);
	}

	/**
	 * Sends a series of messages telling a specific user to add to it's user list all already connected users.
	 * 
	 * @param client
	 *            to receive the user list.
	 * @throws IOException
	 */
	public void sendUsersListToClient(Socket client) throws IOException {
		for (String user : server.getNamesToConnections().keySet()) {
			sendMessageToClient(MSG_PREFIX_ADDUSER + user, client);
		}
	}

	/**
	 * Sends a wellcome message to a client.
	 * 
	 * @param client
	 *            to receive the message.
	 * @throws IOException
	 */
	public void sendWellcomeMessageToCLient(Socket client) throws IOException {
		if (getLangFromSocket(client) == InterfaceLang.EN) {
			sendMessageToClient(server.getCurrentTime() + "Server: Wellcome to the ChatApp. Users online: "
					+ server.getNamesToConnections().entrySet().size(), client);
		} else if (getLangFromSocket(client) == InterfaceLang.BG) {
			sendMessageToClient(server.getCurrentTime() + "Сървър: Добре дошли в ChatApp. Потребители он-лайн: "
					+ server.getNamesToConnections().entrySet().size(), client);
		}
	}

	/**
	 * Retrieves a message from a client.
	 * 
	 * @param client
	 *            to receive the message from.
	 * @return the retrieved message
	 * @throws IOException
	 */
	public String retrieveMessageFromClient(Socket client) throws IOException {
		return new DataInputStream(client.getInputStream()).readUTF();
	}

	public static Color getColorFromJavaAwtString(String awtString) {
		int r = Integer.parseInt(awtString.substring(awtString.indexOf("r=") + 2, awtString.indexOf(",")));
		int g = Integer.parseInt(awtString.substring(awtString.indexOf("g=") + 2, awtString.indexOf(",", awtString.indexOf(",") + 1)));
		int b = Integer.parseInt(awtString.substring(awtString.indexOf("b=") + 2, awtString.indexOf("]")));
		return new Color(r, g, b);
	}

	public ArrayBlockingQueue<String> getUnprocessedClientMessages() {
		return unprocessedClientMessages;
	}

	public ArrayBlockingQueue<String> getUnprocessedServerMessages() {
		return unprocessedServerMessages;
	}
}
