package edu.uni.ruse.client.frames;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.text.*;

import edu.uni.ruse.utilities.BilingualMessages;
import edu.uni.ruse.client.Client;
import edu.uni.ruse.utilities.CodeMessages;
import edu.uni.ruse.utilities.InterfaceLang;
import edu.uni.ruse.utilities.MessagesManager;


/**
 * ClientFrame class, used to display the Client class's functionality.
 * 
 * @author Alexander Andreev
 */
public class ClientFrame extends JFrame {

	private static final long serialVersionUID = -8613537186396001161L;
	private static final int MESSAGE_MAX_LENGHT = 200;
	public static final int SERVER_RECONNECT_INTERVAL_MS = 10000;
	public static final int SERVER_RECONNECT_TRIES = 4;
	private static List<String> users;
	private Client client;
	private JScrollPane messagesScrollPane;
	private JTextPane messagesArea;
	private JTextArea usersTextArea;
	private JPanel messagesPanel;
	private JPanel rightPanel;
	private JPanel settingsPanel;
	private JTextField messageField;
	private JLabel charCountLabel;
	private JButton langButton;
	private JLabel usersOnlineLabel;
	private JButton sendButton;
	private JButton disconnectButton;
	private SimpleAttributeSet textStyle;

	/**
	 * Constructor of ClientFrame that creates a LoginFrame to get a valid user, and arranges the elements of the
	 * client.
	 */
	public ClientFrame() {
		try {
			logInToServer();
		} catch (InterruptedException e) {
			System.out.println(e);
			System.out.println("Thread interrupted while waiting for the client to log in.");
			Thread.currentThread().interrupt();
		}
		users = new ArrayList<>();
		initializeFrameComponents();
	}

	/**
	 * Main method that exchanges messages with other users via the chat server. If the connection with the server is
	 * interrupted an automatic try to reconnect is made.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		ClientFrame clientFrame = new ClientFrame();
		System.out.println("Starting client frame with client: " + clientFrame.client.getName());
		clientFrame.reVisualiseOnlineUsers();
		while (true) {
			try {
				clientFrame.client.receiveMessage();
				if (clientFrame.client.getReceivedMessage() != null) {
					processReceivedMessage(clientFrame);
				}
			} catch (IOException e) {
				System.out.println(e);
				System.out.println("Disconnected from server, trying to reconnect..");
				clientFrame.displaySystemMessageBilingual(BilingualMessages.LOST_CONNECTION);
				clientFrame.tryToReconnect();
			}
		}
	}

	/**
	 * Processes a message received from the server.
	 * 
	 * @param clientFrame
	 */
	private static void processReceivedMessage(ClientFrame clientFrame) {
		String message = MessagesManager.removeColorCodeFromMessage(clientFrame.client.getReceivedMessage());
		if (clientFrame.receivedMessageIsToAddUser()) {
			String userToAdd = message.substring(CodeMessages.ADDUSER.getMessage().length());
			if (!userToAdd.equals(clientFrame.client.getName())) {
				System.out.println("Adding user '" + userToAdd + "' to list of online users");
				users.add(userToAdd);
				clientFrame.reVisualiseOnlineUsers();
			}
		} else if (clientFrame.receivedMessageIsToRenameUser()) {
			String newName = message.substring(message.toLowerCase().indexOf(CodeMessages.CHANGE_USERNAME.getMessage()) +
					CodeMessages.CHANGE_USERNAME.getMessage().length());
			clientFrame.client.setName(newName);
			clientFrame.setTitle("ChatApp: " + newName);
		} else if (clientFrame.receivedMessageIsToRemoveUser()) {
			String userToRemove = message.substring(CodeMessages.REMOVEUSER.getMessage().length());
			users.remove(userToRemove);
			System.out.println("Removing user '" + userToRemove + "' to list of online users");
			clientFrame.reVisualiseOnlineUsers();
		} else {
			StyledDocument doc = clientFrame.messagesArea.getStyledDocument();
			try {
				clientFrame.textStyle.addAttribute(StyleConstants.Foreground, clientFrame.client.getCurrentColor());
				doc.insertString(doc.getLength(), clientFrame.client.getReceivedMessage() + System.lineSeparator(), clientFrame.textStyle);
				clientFrame.textStyle.removeAttribute(StyleConstants.Foreground);
			} catch (BadLocationException e) {
				System.out.println(e);
			}
			clientFrame.messagesScrollPane.getVerticalScrollBar()
					.setValue(clientFrame.messagesScrollPane.getVerticalScrollBar().getMaximum());
		}
	}

	/**
	 * Logs in to the server using the LoginFrame class.
	 * 
	 * @throws InterruptedException
	 */
	private void logInToServer() throws InterruptedException {
		new LoginFrame(this);
		while (client == null) {
			Thread.sleep(150);
		}
	}

	/**
	 * Makes specified number of tries to reconnect to the server between specified intervals.
	 */
	private void tryToReconnect() {
		System.out.println("Lost connection with server, trying to reconnect.");
		boolean recconnectSuccessful = false;
		int numberOfReconnectTries = 0;
		while (numberOfReconnectTries < SERVER_RECONNECT_TRIES && !recconnectSuccessful) {
			try {
				numberOfReconnectTries++;
				displaySystemMessageBilingual(BilingualMessages.TRY_TO_RECONNECT,
						"(" + numberOfReconnectTries + "/" + SERVER_RECONNECT_TRIES + ")..");
				Thread.sleep(SERVER_RECONNECT_INTERVAL_MS);
				recconnectSuccessful = client.connectToServer();
			} catch (InterruptedException e1) {
				System.out.println(e1);
				System.out.println("Thread interrupted while trying to reconnect.");
				Thread.currentThread().interrupt();
			}
		}
		if (recconnectSuccessful) {
			displaySystemMessageBilingual(BilingualMessages.SUCCESSFUL_RECONNECT);
		} else {
			displaySystemMessageBilingual(BilingualMessages.UNSUCCESSFUL_RECONNECT);
			System.out.println("Server disconnected while trying to read messages.");
			System.exit(0);
		}
	}

	/**
	 * Adds a message to the messagesArea considering the user's chosen language.
	 * 
	 * @param message
	 *            to be added
	 */
	private void displaySystemMessageBilingual(BilingualMessages message) {
		displaySystemMessageBilingual(message, "");
	}

	/**
	 * Adds a message to the messagesArea considering the user's chosen language. Adds a postfix string regardless of
	 * the chosen language.
	 * 
	 * @param message
	 *            message to be added
	 * @param postfix
	 *            to be appended to the bilingual message
	 */
	private void displaySystemMessageBilingual(BilingualMessages message, String postfix) {
		StyledDocument doc = messagesArea.getStyledDocument();
		try {
			if (client.getLanguage() == InterfaceLang.EN) {
				doc.insertString(doc.getLength(), message.inEnglish() + postfix + System.lineSeparator(), textStyle);
			} else if (client.getLanguage() == InterfaceLang.BG) {
				doc.insertString(doc.getLength(), message.inBulgarian() + postfix + System.lineSeparator(), textStyle);
			}
		} catch (BadLocationException e) {
			System.out.println(e);
		}
	}

	/**
	 * Sends a message to the server via the connection's output stream.
	 */
	public void sendMessageToServer() {
		String messageToSend = messageField.getText().trim();
		if (messageToSend.length() > 0) {
			messageToSend = messageToSend.substring(0, 1).toUpperCase() + messageToSend.substring(1);
			client.sendMessage(client.getName() + ": " + messageToSend);
			messageField.setText("");
		}
	}

	/**
	 * Shows a option pane, asking for confirmation to close the client.
	 */
	public void askToClose() {
		int chosedOption;
		if (client.getLanguage() == InterfaceLang.EN) {
			chosedOption = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit the client?",
					"Exit confirmation", JOptionPane.YES_NO_OPTION);
		} else {
			chosedOption = JOptionPane.showConfirmDialog(null, "Сигурни ли сте, че искате да напуснете клиента?",
					"Потвърждение за напускане", JOptionPane.YES_NO_OPTION);
		}
		if (chosedOption == JOptionPane.YES_OPTION) {
			displaySystemMessageBilingual(BilingualMessages.DISCONNECTING);
			client.sendMessage(CodeMessages.REMOVEUSER.getMessage() + client.getName());
			System.exit(0);
		}
	}

	/**
	 * Returns if the last received message is to remove user.
	 * 
	 * @return true if the message is telling to remove a specific user.
	 */
	private boolean receivedMessageIsToRemoveUser() {
		return client.getReceivedMessage().startsWith(CodeMessages.REMOVEUSER.getMessage());
	}

	private boolean receivedMessageIsToRenameUser() {
		String message = client.getReceivedMessage();
		return message.startsWith(CodeMessages.CHANGE_USERNAME.getMessage());
	}

	/**
	 * Returns if the last received message is to add user.
	 * 
	 * @return true if the message is telling to add a specific user.
	 */
	private boolean receivedMessageIsToAddUser() {
		return client.getReceivedMessage().startsWith(CodeMessages.ADDUSER.getMessage());
	}

	/**
	 * Updates the users text area with the clien's user list.
	 */
	public void reVisualiseOnlineUsers() {
		usersTextArea.setText("");
		usersTextArea.append(client.getName() + System.lineSeparator());
		for (String user : users) {
			usersTextArea.append(user + System.lineSeparator());
		}
	}

	/**
	 * Initializes all components of the client frame.
	 */
	private void initializeFrameComponents() {
		setupMainFrame();
		BorderLayout contentPaneLayout = new BorderLayout(0, 20);
		this.getContentPane().setLayout(contentPaneLayout);

		initializeMessagesPanel();
		initializeOnlineUsersPanel();
		initializeSettingsPanel();

		rightPanel.add(settingsPanel, BorderLayout.PAGE_END);

		this.setVisible(true);
	}

	/**
	 * Setups the main frame of the program.
	 */
	private void setupMainFrame() {
		this.setSize(800, 600);
		this.setTitle("ChatApp: " + client.getName());
		setLocationOnCenter();
		this.setResizable(true);
		this.setMinimumSize(new Dimension(600, 400));
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		FlowLayout layout = new FlowLayout();
		this.getContentPane().setLayout(layout);

		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				askToClose();
			}
		});
	}

	/**
	 * Sets the window at the middle of the user's screen.
	 */
	private void setLocationOnCenter() {
		Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
		int xPos = (screenDimension.width / 2) - (this.getWidth() / 2);
		int yPos = (screenDimension.height / 2) - (this.getHeight() / 2);
		this.setLocation(xPos, yPos);
	}

	/**
	 * Initializes the messages panel.
	 */
	private void initializeMessagesPanel() {
		BorderLayout messagesPanelLayout = new BorderLayout();
		messagesPanel = new JPanel(messagesPanelLayout);
		messagesArea = new JTextPane();
		messagesArea.setSize(350, 350);

		textStyle = new SimpleAttributeSet();
		//messagesArea.setLineWrap(true);
		messagesArea.setEditable(false);
		messagesArea.setVisible(true);
		messagesScrollPane = new JScrollPane(messagesArea);
		messagesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		messagesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		messagesPanel.add(messagesScrollPane, BorderLayout.CENTER);

		ChartypeListener chartypeListener = new ChartypeListener();
		messageField = new JTextField();
		messageField.setPreferredSize(new Dimension(350, 30));
		messageField.addKeyListener(chartypeListener);
		messagesPanel.add(messageField, BorderLayout.PAGE_END);

		this.getContentPane().add(messagesPanel, BorderLayout.CENTER);
	}

	/**
	 * Initializes the online users panel.
	 */
	private void initializeOnlineUsersPanel() {
		BorderLayout rightPanelLayout = new BorderLayout();
		rightPanel = new JPanel(rightPanelLayout);

		usersOnlineLabel = new JLabel(BilingualMessages.USERS_ONLINE.inSpecificLang(client.getLanguage()));
		rightPanel.add(usersOnlineLabel, BorderLayout.NORTH);
		usersTextArea = new JTextArea(13, 30);
		usersTextArea.setSize(350, messageField.getY() * 2);
		usersTextArea.setLineWrap(true);
		usersTextArea.setEditable(false);
		usersTextArea.setVisible(true);
		JScrollPane usersScrollPane = new JScrollPane(usersTextArea);
		usersScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		usersScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		usersScrollPane.setPreferredSize(new Dimension(200, 220));
		DefaultCaret caret = (DefaultCaret) usersTextArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		rightPanel.add(usersScrollPane, BorderLayout.CENTER);
		this.getContentPane().add(rightPanel, BorderLayout.LINE_END);
	}

	/**
	 * Initializes the settings panel.
	 */
	private void initializeSettingsPanel() {
		settingsPanel = new JPanel();
		GridLayout bottomRightPanelLayout = new GridLayout(2, 1);
		settingsPanel.setLayout(bottomRightPanelLayout);
		charCountLabel = new JLabel(
				BilingualMessages.CHARS_REMAINING.inSpecificLang(client.getLanguage()) + MESSAGE_MAX_LENGHT);
		charCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
		settingsPanel.add(charCountLabel);
		initializeButtonsPanel();
		rightPanel.add(settingsPanel, BorderLayout.PAGE_END);
	}

	/**
	 * Initializes the buttons panel.
	 */
	private void initializeButtonsPanel() {
		JPanel buttonsPanel = new JPanel();

		sendButton = new JButton(BilingualMessages.SEND.inSpecificLang(client.getLanguage()));
		MessageSendListener messageSendListener = new MessageSendListener();
		sendButton.addActionListener(messageSendListener);
		buttonsPanel.add(sendButton);

		disconnectButton = new JButton(BilingualMessages.DISCONNECT.inSpecificLang(client.getLanguage()));
		DisconnectListener disconnectListener = new DisconnectListener();
		disconnectButton.addActionListener(disconnectListener);
		buttonsPanel.add(disconnectButton);

		langButton = new JButton();
		langButton.setMaximumSize(new Dimension(50, 50));
		try {
			Image img = client.getLanguage().getFlag();
			langButton.setIcon(new ImageIcon(img));
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Could not retrieve flag for specific language, the button will be blank");
		}
		LanguageChangeListener languageChangeListener = new LanguageChangeListener();
		langButton.addActionListener(languageChangeListener);
		buttonsPanel.add(langButton);
		settingsPanel.add(buttonsPanel);
	}

	/**
	 * ChartypeListener implements KeyListener and ensures that the typed message will be no longer than the specified
	 * lenght. Also sends the message typed if the user presses "Enter".
	 * 
	 * @author Alexander Andreev
	 */
	private class ChartypeListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				sendMessageToServer();
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (messageField.getText().length() >= MESSAGE_MAX_LENGHT) {
				messageField.setText(messageField.getText().substring(0, MESSAGE_MAX_LENGHT));
			}
			charCountLabel.setText(BilingualMessages.CHARS_REMAINING.inSpecificLang(client.getLanguage())
					+ (MESSAGE_MAX_LENGHT - (messageField.getText().length())));
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}

	/**
	 * Switches the client's language between Bulgarian and English.
	 * 
	 * @author Alexander Andreev
	 */
	private class LanguageChangeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (client.getLanguage() == InterfaceLang.BG) {
				client.setLanguage(InterfaceLang.EN);
			} else if (client.getLanguage() == InterfaceLang.EN) {
				client.setLanguage(InterfaceLang.BG);
			}
			client.sendMessage(CodeMessages.CHANGE_LANG.getMessage() + client.getName());
			reloadInterfaceWithNewLanguage();
		}

		/**
		 * Updates the user interface considering the language change.
		 */
		private void reloadInterfaceWithNewLanguage() {
			charCountLabel.setText(BilingualMessages.CHARS_REMAINING.inSpecificLang(client.getLanguage())
					+ (MESSAGE_MAX_LENGHT - (messageField.getText().length())));
			usersOnlineLabel.setText(BilingualMessages.USERS_ONLINE.inSpecificLang(client.getLanguage()));
			sendButton.setText(BilingualMessages.SEND.inSpecificLang(client.getLanguage()));
			disconnectButton.setText(BilingualMessages.DISCONNECT.inSpecificLang(client.getLanguage()));

			try {
				Image img = client.getLanguage().getFlag();
				langButton.setIcon(new ImageIcon(img));
			} catch (IOException e1) {
				System.out.println(e1);
				System.out.println("Could not load lang flag.");
			}
		}
	}

	/**
	 * MessageSendListener that sends a message to the server.
	 * 
	 * @author Alexander Andreev
	 */
	private class MessageSendListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			sendMessageToServer();
		}
	}

	/**
	 * DisconnectListener that disconnects with the server.
	 * 
	 * @author Alexander Andreev
	 */
	private class DisconnectListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			askToClose();
		}
	}

	public void setClient(Client client) {
		if (this.client == null) {
			this.client = client;
		} else {
			System.out.println("Cannot set new client once a client is set. Client attribute has not changed.");
		}
	}
}