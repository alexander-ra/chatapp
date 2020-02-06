package edu.uni.ruse.server.frames;

import edu.uni.ruse.server.Server;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultCaret;

/**
 * ServerFrame class, used to display the Server class's functionality.
 * 
 * @author Alexander Andreev
 */
public class ServerFrame extends JFrame {

	private static final long serialVersionUID = 5900179919035839831L;
	private static final String MSG_CODE_REFRESH_USERLIST = "REFRESH_USERLIST";
	private transient ServerMessagesManagerWorker serverMessagesManager;
	private transient MessagesCollectorWorker messagesCollector;
	private transient NewConnectionsWatcherWorker connectionsWatcher = new NewConnectionsWatcherWorker();
	private Server server;
	private JTextArea messageArea;
	private JScrollPane messagesScrollPane;
	private JTextArea onlineUsersTextArea;
	private JButton startButton;
	private JButton stopButton;
	private JCheckBox serverSetupCheckBox;
	private JPanel serverSetupPanel;
	private JTextField ipField;
	private JTextField portField;

	/**
	 * Constructor of ServerFrame that creates and arranges the elements of the window.
	 */
	public ServerFrame() {
		server = new Server();
		initializeComponents();
		this.setVisible(true);
	}

	/**
	 * Main method that creates and builds the server's interface, waits the server to start, gets connections from
	 * clients and exchanges messages with them until the server is stopped.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ServerFrame serverFrame = new ServerFrame();
		System.out.println("Server frame launched.");
		try {
			serverFrame.server.waitForServerToStart();
		} catch (InterruptedException e) {
			System.out.println(e);
			System.out.println("InterruptedException while waiting for connections.");
			Thread.currentThread().interrupt();
		}
		while (true) {
			if (serverFrame.server.isRunning()) {
				while (serverFrame.server.havesUnprocessedClientMessages()) {
					serverFrame.server.processOldestMessage();
				}
			} else {
				try {
					serverFrame.server.waitForServerToStart();
				} catch (InterruptedException e) {
					System.out.println(e);
					System.out.println("Thread interrupted while waiting the server to start.");
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/**
	 * MessagesCollectorWorker is a SwingWorker that collects messages, send from the server's clients.
	 * 
	 * @author Alexander Andreev
	 */
	private class MessagesCollectorWorker extends SwingWorker<Void, Void> {
		@Override
		public Void doInBackground() throws IOException, InterruptedException {
			while (server.isRunning()) {
				server.collectNewMessages();
				Thread.sleep(50);
			}
			return null;
		}
	};

	/**
	 * ServerMessagesManagerWorker is a SwingWorker that processes server messages, from the oldest to the newest.
	 * 
	 * @author Alexander Andreev
	 */
	private class ServerMessagesManagerWorker extends SwingWorker<Void, Void> {
		@Override
		public Void doInBackground() throws IOException, InterruptedException {
			while (server.isRunning()) {
				if (server.havesUnprocessedServerMessages()) {
					String message = server.getUnprocessedServerMessages().poll();
					if (MSG_CODE_REFRESH_USERLIST.equals(message)) {
						revisualiseUsers();
					} else {
						messageArea.append(message + System.lineSeparator());
						messagesScrollPane.getVerticalScrollBar()
								.setValue(messagesScrollPane.getVerticalScrollBar().getMaximum());
					}
				}
			}
			return null;
		}
	};

	/**
	 * NewConnectionsWatcherWorker is a SwingWorker that listens for new connections.
	 * 
	 * @author Alexander Andreev
	 */
	private class NewConnectionsWatcherWorker extends SwingWorker<Void, Void> {
		@Override
		public Void doInBackground() throws IOException {
			while (server.isRunning()) {
				server.getNewConnection();
				revisualiseUsers();
			}
			return null;
		}
	};

	/**
	 * Updates the online users text area.
	 */
	public void revisualiseUsers() {
		onlineUsersTextArea.setText("");
		for (String user : server.getNamesToConnections().keySet()) {
			onlineUsersTextArea.append(user + System.lineSeparator());
		}
	}

	/**
	 * Initializes the server frame's components.
	 */
	private void initializeComponents() {
		setupMainFrame();
		BorderLayout contentPaneLayout = new BorderLayout(0, 20);
		this.getContentPane().setLayout(contentPaneLayout);

		initializeOnlineUsersPanel();
		initializeMessagesScrollPane();
		initializeBottomPanel();
	}

	/**
	 * Setups the main frame of the program.
	 */
	private void setupMainFrame() {
		this.setSize(800, 600);
		this.setMinimumSize(new Dimension(800, 600));
		this.setTitle("ChatApp Server");
		setLocationOnCenter();
		this.setResizable(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				askToClose();
			}
		});

		FlowLayout layout = new FlowLayout();
		this.getContentPane().setLayout(layout);
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
	 * Shows a option pane, asking for confirmation to close the server.
	 */
	public void askToClose() {
		int chosedOption;
		chosedOption = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit the server?",
				"Exit confirmation", JOptionPane.YES_NO_OPTION);
		if (chosedOption == JOptionPane.YES_OPTION) {
			System.exit(0);
		}
	}

	/**
	 * Initializes the elements connected with the online user's part of the interface.
	 */
	private void initializeOnlineUsersPanel() {
		BorderLayout onlineUsersPanelLayout = new BorderLayout();
		Panel onlineUsersPanel = new Panel(onlineUsersPanelLayout);

		onlineUsersTextArea = new JTextArea(13, 30);
		onlineUsersTextArea.setSize(350, 450);
		onlineUsersTextArea.setLineWrap(true);
		onlineUsersTextArea.setEditable(false);
		onlineUsersTextArea.setVisible(false);
		JScrollPane usersScrollPane = new JScrollPane(onlineUsersTextArea);
		usersScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		usersScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		usersScrollPane.setPreferredSize(new Dimension(200, 220));
		DefaultCaret usersCaret = (DefaultCaret) onlineUsersTextArea.getCaret();
		usersCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		onlineUsersPanel.add(usersScrollPane, BorderLayout.CENTER);

		JLabel onlineUsers = new JLabel("Users online:");
		onlineUsersPanel.add(onlineUsers, BorderLayout.NORTH);

		this.getContentPane().add(onlineUsersPanel, BorderLayout.WEST);
	}

	/**
	 * Initializes the server frame's messages scroll pane.
	 */
	private void initializeMessagesScrollPane() {
		messageArea = new JTextArea(13, 30);
		messageArea.setSize(350, 350);
		messageArea.setLineWrap(true);
		messageArea.setEditable(false);
		messageArea.setVisible(false);
		messagesScrollPane = new JScrollPane(messageArea);
		messagesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		messagesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		DefaultCaret messagesCaret = (DefaultCaret) messageArea.getCaret();
		messagesCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		this.getContentPane().add(messagesScrollPane, BorderLayout.CENTER);
	}

	/**
	 * Initializes the bottom part of the server's interface, including the start and stop buttons, and the server setup
	 * panel.
	 */
	private void initializeBottomPanel() {
		FlowLayout bottomPanelLayout = new FlowLayout();
		Panel bottomPanel = new Panel(bottomPanelLayout);
		bottomPanel.setMinimumSize(new Dimension(800, 40));
		bottomPanel.setPreferredSize(new Dimension(800, 40));
		bottomPanel.setMaximumSize(new Dimension(10000, 50));

		startButton = new JButton("Start");
		StartListener startListener = new StartListener();
		startButton.addActionListener(startListener);
		bottomPanel.add(startButton);

		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		StopListener stopListener = new StopListener();
		stopButton.addActionListener(stopListener);
		bottomPanel.add(stopButton);

		initializeServerSetupElements(bottomPanel);
		this.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
	}

	/**
	 * Initializes the server setup elements.
	 */
	private void initializeServerSetupElements(Panel bottomPanel) {
		serverSetupCheckBox = new JCheckBox("Setup server location");
		CheckboxListener checkboxListener = new CheckboxListener();
		serverSetupCheckBox.addActionListener(checkboxListener);
		bottomPanel.add(serverSetupCheckBox);

		serverSetupPanel = new JPanel();
		serverSetupPanel.setVisible(false);

		JLabel ipLabel = new JLabel("IP: ");
		serverSetupPanel.add(ipLabel);

		ipField = new JTextField(server.getIpAddress());
		ipField.setPreferredSize(new Dimension(100, 20));
		serverSetupPanel.add(ipField);

		JLabel portLabel = new JLabel("Port: ");
		serverSetupPanel.add(portLabel);

		portField = new JTextField(String.valueOf(Server.PORT_RANGE_MIN));
		portField.setPreferredSize(new Dimension(40, 20));
		serverSetupPanel.add(portField);
		bottomPanel.add(serverSetupPanel);
	}

	/**
	 * StartListener class that implements ActionListener and is used to start the server when the action is performed.
	 * 
	 * @author Alexander Andreev
	 */
	public class StartListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!server.isRunning()) {
				stopButton.setEnabled(true);
				startButton.setEnabled(false);
				if (serverSetupCheckBox.isSelected()) {
					server.setIpAddress(ipField.getText());
					server.setPort(Integer.parseInt(portField.getText()));
				}
				serverSetupCheckBox.setEnabled(false);
				ipField.setEnabled(false);
				portField.setEnabled(false);
				server.startServer();
				startSwingWorkers();
				messageArea.setVisible(true);
				onlineUsersTextArea.setVisible(true);
				messageArea.append(server.getCurrentTime() + "Starting server on address: " + server.getIpAddress()
						+ ":" + server.getPort() + System.lineSeparator());
			}
		}

		/**
		 * Starts all the SwingWorker classes of the server frame.
		 */
		public void startSwingWorkers() {
			serverMessagesManager = new ServerMessagesManagerWorker();
			serverMessagesManager.execute();
			messagesCollector = new MessagesCollectorWorker();
			messagesCollector.execute();
			connectionsWatcher = new NewConnectionsWatcherWorker();
			connectionsWatcher.execute();
		}
	}

	/**
	 * StopListener class that implements ActionListener and is used to stop the server when the action is performed.
	 * 
	 * @author Alexander Andreev
	 */
	public class StopListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (server.isRunning()) {
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				serverSetupCheckBox.setEnabled(true);
				ipField.setEnabled(true);
				portField.setEnabled(true);
				stopSwingWorkers();
				server.stopServer();
				messageArea.append(server.getCurrentTime() + "Server stopped." + System.lineSeparator());
			}
		}

		/**
		 * Stops all the SwingWorker classes of the server frame.
		 */
		public void stopSwingWorkers() {
			serverMessagesManager.cancel(true);
			messagesCollector.cancel(true);
			connectionsWatcher.cancel(true);
		}
	}

	/**
	 * CheckboxListener that gives the user the option to setup the server location (ip and port).
	 * 
	 * @author Alexander Andreev
	 */
	public class CheckboxListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (serverSetupCheckBox.isSelected()) {
				serverSetupPanel.setVisible(true);
			} else {
				serverSetupPanel.setVisible(false);
			}
		}
	}
}
