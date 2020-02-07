package edu.uni.ruse.server;

import com.sun.org.apache.bcel.internal.classfile.Code;
import edu.uni.ruse.utilities.CodeMessages;
import edu.uni.ruse.utilities.InterfaceLang;
import edu.uni.ruse.utilities.MessagesManager;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Server class, that creates connections between multiple clients, receives and sends messages from them.
 *
 * @author Alexander Andreev
 */
public class Server {

    public static final int PORT_RANGE_MIN = 7000;
    private static final int PORT_RANGE_MAX = 7020;
    private static final int WAIT_INTERVAL_MS = 1000;
    private static final int MIN_USERNAME_LENGHT = 3;
    private static final int CONNECTION_QUEUE_LIMIT = 32;
    private static final InterfaceLang DEFAULT_LANGUAGE = InterfaceLang.EN;
    private ServerSocket serverSocket;
    private String ipAddress;
    private int port;
    private volatile Map<String, Socket> namesToConnections = new HashMap<>();
    private volatile Map<Socket, InterfaceLang> langPreferences = new HashMap<>();
    private volatile MessagesManager messagesManager = new MessagesManager(this);
    private volatile Socket connectionToBeAccepted;
    private String nameOfNextUserToJoin;
    private LocalDateTime currentTime;
    private Boolean isRunning = false;

    /**
     * Default constructor.
     */
    public Server() {
        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println(e);
            System.out.println("Could not retrieve localhost's address");
        }
        port = getFirstUnocupiedPort();
    }

    /**
     * Constructor with given port.
     *
     * @param port to start the server on.
     */
    public Server(int port) {
        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println(e);
            System.out.println("Could not retrieve localhost's address");
        }
        this.port = port;
    }

    /**
     * Constructor with given ip address and port.
     *
     * @param port      to start the server on.
     * @param ipAddress to start the server on.
     */
    public Server(int port, String ipAddress) {
        this.port = port;
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the first port that can be used to start the server.
     *
     * @return free port.
     */
    public int getFirstUnocupiedPort() {
        for (int i = PORT_RANGE_MIN; i <= PORT_RANGE_MAX; i++) {
            if (checkIfPortIsFree(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks if given number can be used as a port.
     *
     * @param port to be checked
     * @return port
     */
    private boolean checkIfPortIsFree(int port) {
        try (ServerSocket tempServerSocket = new ServerSocket(port)) {
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Port " + port + " is occupied. Will try to connect with next port number");
            return false;
        }
        System.out.println("Port " + port + " is free and will be taken by the server");
        return true;
    }

    /**
     * Starts the server on local host and on the specified port.
     */
    public void startServer() {
        try {
            InetAddress serverAddress;
            if (ipAddress == null) {
                ipAddress = InetAddress.getLocalHost().getHostAddress();
            }
            serverAddress = InetAddress.getByName(ipAddress);
            serverSocket = new ServerSocket(port, CONNECTION_QUEUE_LIMIT, serverAddress);
            isRunning = true;
            System.out.println("Server started on " + serverAddress.getHostAddress() + ":" + port);
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("I/O Exception while trying to start server.");
        }
    }

    /**
     * Stops the server.
     */
    public void stopServer() {
        try {
            for (String user : namesToConnections.keySet()) {
                removeUser(user);
            }
            serverSocket.close();
            isRunning = false;
            System.out.println("Server stopped.");
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("I/O Exception while trying to stop server.");
        }
    }

    /**
     * Accepts a new client.
     */
    public void getNewConnection() {
        try {
            connectionToBeAccepted = serverSocket.accept();
            String message = messagesManager.retrieveMessageFromClient(connectionToBeAccepted);
            if (message.startsWith(CodeMessages.CONREQUEST.getMessage())) {
                nameOfNextUserToJoin = getUserNameFromConnectionRequest(message);
                if (userCanJoin(nameOfNextUserToJoin)) {
                    acceptNewClient(nameOfNextUserToJoin, connectionToBeAccepted);
                } else {
                    rejectNewClient(nameOfNextUserToJoin, connectionToBeAccepted);
                }
            } else {
                System.out.println(
                        "The received connection request message was not a valid one, client will be disconnected.");
                connectionToBeAccepted.close();
            }
        } catch (IOException e) {
            if (isRunning) {
                System.out.println(e);
                System.out.println("I/O Exception while trying to accept new client.");
            }
        }
    }

    /**
     * Introduces client to the server and to the other users.
     *
     * @param name       of user to join
     * @param connection to be accepted
     * @throws IOException
     */
    private void acceptNewClient(String name, Socket connection) throws IOException {
        System.out.println("Accepting client: " + name);
        messagesManager.sendNewUserMessageToServerFrame(name);
        namesToConnections.put(name, connection);
        System.out.println("User '" + name + "' added to the list of users.");
        langPreferences.put(connection, DEFAULT_LANGUAGE);
        messagesManager.sendUserAddedToListMessageToServerFrame(name);
        System.out.println("User '" + name + "' was sended to the other online users.");
        messagesManager.sendAcceptedMessageToClient(connection);
        messagesManager.sendNewUserMessageToEveryone(name);
        messagesManager.sendMessageToServerFrame(
                getCurrentTime() + "Sended messages, notifying the connection of client " + name);
        messagesManager.sendAddUserMessageToEveryone(name);
        messagesManager.sendUsersListToClient(connection);
        messagesManager.sendWellcomeMessageToCLient(connection);
    }

    /**
     * Reject's a socket to join the server.
     *
     * @param connection to be rejected
     * @throws IOException
     */
    private void rejectNewClient(String name, Socket connection) throws IOException {
        System.out.println("Rejecting client connection request with name: " + name);
        messagesManager.sendDeclinedMessageToClient(connection);
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            System.out.println(e);
            System.out.println("InterruptedException while waiting for client to receive login message");
            Thread.currentThread().interrupt();
        }
        connection.close();
    }

    /**
     * Gets a user name from a connection request.
     *
     * @param request
     * @return user name
     */
    public String getUserNameFromConnectionRequest(String request) {
        return request.substring(CodeMessages.CONREQUEST.getMessage().length());
    }

    /**
     * Checks if a given user name can join the server.
     *
     * @param userName to join the server.
     * @return true if that user name can join the server.
     */
    public boolean userCanJoin(String userName) {
        if (isValidUsername(userName)) {
            for (Map.Entry<String, Socket> connection : namesToConnections.entrySet()) {
                if (connection.getKey().compareToIgnoreCase(userName) == 0) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if a given user name is at least a specified amount of characters long, and if it holds forbidden
     * characters '[' and ']'.
     *
     * @param userName to be checked
     * @return
     */
    private boolean isValidUsername(String userName) {
        return userName.length() >= MIN_USERNAME_LENGHT && !userName.contains("[") && !userName.contains("]");
    }

    /**
     * Removes a user from the server.
     *
     * @param userToBeRemoved
     */
    public void removeUser(String userToBeRemoved) {
        System.out.println("Removing user '" + userToBeRemoved + "' from server.");
        try {
            namesToConnections.get(userToBeRemoved).close();
            messagesManager
                    .sendMessageToServerFrame(getCurrentTime() + "Closed connection with client " + userToBeRemoved);
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("IOException while trying to end the connection with user: " + userToBeRemoved);
        }
        namesToConnections.remove(userToBeRemoved);
        langPreferences.remove(userToBeRemoved);
        messagesManager.sendRefreshMessageToServerFrame();
        System.out.println("User succesfully removed.");
    }

    /**
     * Changes an username, but the connection stays the same.
     *
     * @param oldName of user
     * @param newName of user
     */
    public void renameUser(String oldName, String newName) {
        if (!namesToConnections.containsKey(newName)) {
            System.out.println("Renaming user '" + oldName + " to " + newName + "' on the server.");
            try {
                messagesManager.sendMessageToClient(CodeMessages.CHANGE_USERNAME.getMessage() + newName, namesToConnections.get(oldName));
            } catch (IOException e) {
                System.out.println(e);
                System.out.println("User not found to have the name changed.");
            }
            Socket connection = namesToConnections.remove(oldName);
            namesToConnections.put(newName, connection);
            messagesManager.sendRemoveUserMessageToEveryone(oldName);
            messagesManager.sendAddUserMessageToEveryone(newName);
        } else {
            System.out.println("Cannot rename user '" + oldName + " to " + newName + "' on the server, because "
                    + newName + " already exists");
            try {
                messagesManager.sendMessageToClient("Cannot change name. Username " + newName + " already exists!", namesToConnections.get(oldName));
            } catch (IOException e) {
                System.out.println(e);
                System.out.println("User not found to send warning.");
            }

        }
    }

    /**
     * Gets the current time on the server, formatted as a string in the pattern [HH:MM:SS].
     *
     * @return currentTime as a string
     */
    public String getCurrentTime() {
        currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:MM:SS");
        return "[" + formatter.format(currentTime) + "]";
    }

    /**
     * Processes the oldest message in the queue of received messages.
     */
    public void processOldestMessage() {
        if (!messagesManager.getUnprocessedClientMessages().isEmpty()) {
            try {
                String message = messagesManager.getUnprocessedClientMessages().take();
                if (message.startsWith(CodeMessages.REMOVEUSER.getMessage())) {
                    String userToBeRemoved = message.substring(CodeMessages.REMOVEUSER.getMessage().length());
                    removeUser(userToBeRemoved);
                    messagesManager.sendUserLeftMessageToEveryone(userToBeRemoved);
                    messagesManager.sendRemoveUserMessageToEveryone(userToBeRemoved);
                } else if (message.startsWith(CodeMessages.CHANGE_LANG.getMessage())) {
                    String userToChangeLang = message.substring(CodeMessages.CHANGE_LANG.getMessage().length());
                    messagesManager.changeUserLanguage(namesToConnections.get(userToChangeLang));
                } else if (message.toLowerCase().indexOf(CodeMessages.CHANGE_USERNAME.getMessage()) != -1) {
                    String sender = message.substring(0, message.toLowerCase().indexOf(": " + CodeMessages.CHANGE_USERNAME.getMessage()));
                    String newName = message.substring(message.toLowerCase().indexOf(CodeMessages.CHANGE_USERNAME.getMessage()) +
                            CodeMessages.CHANGE_USERNAME.getMessage().length() + 1);
                    renameUser(sender, newName);
                    System.out.println(message);
                } else if (message.toLowerCase().indexOf(CodeMessages.WHISPER.getMessage()) != -1 &&
                        message.toLowerCase().indexOf(CodeMessages.WHISPER.getMessage()) < message.indexOf(" ", message.indexOf(" ") + 1)) {
                    String sender = message.substring(0, message.toLowerCase().indexOf(": " + CodeMessages.WHISPER.getMessage()));
                    String restOfMessage = message.substring(message.toLowerCase().indexOf(CodeMessages.WHISPER.getMessage()) +
                            CodeMessages.WHISPER.getMessage().length() + 1);
                    String receiver = restOfMessage.substring(0, restOfMessage.indexOf(" "));
                    restOfMessage = restOfMessage.substring(restOfMessage.indexOf(" "));

                    if (namesToConnections.containsKey(receiver) && namesToConnections.containsKey(sender)) {
                        try {
                            messagesManager.sendMessageToClient(getCurrentTime() + "Whisper from (" + sender + "):" + restOfMessage, getNamesToConnections().get(receiver), Color.MAGENTA);
                            messagesManager.sendMessageToClient(getCurrentTime() + "Whisper to (" + receiver + "):" + restOfMessage, getNamesToConnections().get(sender), Color.BLUE);

                        } catch (IOException e) {
                            System.out.println(e);
                            System.out.println("Error while trying to send whisper message");
                        }
                    } else {
                        System.out.println("Either sender (" + sender + ") or receiver (" + receiver +
                                ") does not exsist in the application while whisper message is trying to be send");
                    }
                } else {
                    messagesManager.sendMessageToAllUsers(getCurrentTime() + message);
                }
            } catch (InterruptedException e) {
                System.out.println(e);
                System.out.println("Interrupted exception while processing message from client");
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Goes trough each connection and if a connection has send a message, adds it to the queue to messages to process.
     */
    public void collectNewMessages() {
        namesToConnections.forEach((name, connection) -> {
            DataInputStream dataIn;
            String message;
            try {
                dataIn = new DataInputStream(connection.getInputStream());
                if (dataIn.available() > 0) {
                    message = dataIn.readUTF();
                    messagesManager.getUnprocessedClientMessages().put(message);
                }
            } catch (Exception e) {
                removeUser(name);
                messagesManager.sendMessageToServerFrame(
                        getCurrentTime() + "Cannot reach user " + name + ". User will be removed.");
                System.out.println(e);
                System.out.println("Exception while trying to read a message from client "
                        + name + ". Connection will be removed");
            }
        });
    }

    /**
     * Checks if the server has unprocessed client messages.
     *
     * @return true if the unprocessed client messages collection is empty
     */
    public boolean havesUnprocessedClientMessages() {
        return !messagesManager.getUnprocessedClientMessages().isEmpty();
    }

    /**
     * Checks if the server has unprocessed server messages.
     *
     * @return true if the unprocessed server messages collection is empty
     */
    public boolean havesUnprocessedServerMessages() {
        return !messagesManager.getUnprocessedServerMessages().isEmpty();
    }

    /**
     * Waits until the server had started.
     *
     * @throws InterruptedException
     */
    public void waitForServerToStart() throws InterruptedException {
        while (!isRunning) {
            Thread.sleep(WAIT_INTERVAL_MS);
        }
    }

    public Boolean isRunning() {
        return isRunning;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets a new IP Address if the server is not running. If the server is already started, the ip stays the same.
     *
     * @param ipAddress new ip address of the server
     */
    public void setIpAddress(String ipAddress) {
        if (!isRunning) {
            this.ipAddress = ipAddress;
        } else {
            System.out.println("Cannot change address while server is running. Stop the server to change ip or port.");
        }
    }

    public int getPort() {
        return port;
    }

    /**
     * Sets a new port number if the server is not running. If the server is already started, the port number stays the
     * same.
     *
     * @param port new port number of the server
     */
    public void setPort(int port) {
        if (!isRunning) {
            this.port = port;
        } else {
            System.out.println("Cannot change port while server is running. Stop the server to change ip or port.");
        }
    }

    public Socket getConnectionToBeAccepted() {
        return connectionToBeAccepted;
    }

    public Map<String, Socket> getNamesToConnections() {
        return namesToConnections;
    }

    public String getNameOfNextUserToJoin() {
        return nameOfNextUserToJoin;
    }

    public ArrayBlockingQueue<String> getUnprocessedServerMessages() {
        return messagesManager.getUnprocessedServerMessages();
    }

    public Map<Socket, InterfaceLang> getLangPreferences() {
        return langPreferences;
    }
}
