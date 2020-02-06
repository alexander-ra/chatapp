package edu.uni.ruse.client.frames;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.uni.ruse.client.Client;


/**
 * ClientFrame class, used to display the Client class's functionality.
 * 
 * @author Alexander Andreev
 */
public class LoginFrame extends JFrame {

	/**
	 * Comment for serialVersionUID.
	 */
	private static final long serialVersionUID = -815722663117353690L;
	private static final int DEFAULT_PORT = 7000;
	private static final int PORT_MIN = 0;
	private static final int PORT_MAX = 65536;
	private String defaultIpAddress;
	private ClientFrame parentFrame;
	private LoginFrame thisFrame;
	private Client client;
	private static boolean logedIn = false;
	private JTextField usernameField;
	private JTextField ipField;
	private JTextField portField;
	private JPanel serverSetupPanel;
	private JCheckBox serverSetupCheckBox;
	private Boolean accepted;

	/**
	 * Constructor of ClientFrame that creates and arranges the elements of the window.
	 */
	public LoginFrame(ClientFrame parent) {
		this.parentFrame = parent;
		try {
			defaultIpAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			defaultIpAddress = "127.0.0.1";
			System.out.println(
					"Could not retrieve default localhost address to set it as a default ip address. 127.0.0.1 is set as an IP.");
		}
		initializeComponents();
		thisFrame = this;

	}

	private void initializeComponents() {
		setupMainFrame();

		JLabel usernameLabel = new JLabel("Username:");
		this.getContentPane().add(usernameLabel);

		usernameField = new JTextField();
		usernameField.setPreferredSize(new Dimension(210, 30));
		this.getContentPane().add(usernameField);

		JButton connectionButton = new JButton("Connect");
		ConnectionListener connectionListener = new ConnectionListener();
		connectionButton.addActionListener(connectionListener);
		this.getContentPane().add(connectionButton);

		serverSetupCheckBox = new JCheckBox("Setup server location");
		CheckboxListener checkboxListener = new CheckboxListener();
		serverSetupCheckBox.addActionListener(checkboxListener);
		this.getContentPane().add(serverSetupCheckBox);

		initializeServerSetupPanel();
		this.setVisible(true);
	}

	/**
	 * Setups the main frame of the program.
	 */
	private void setupMainFrame() {
		this.setSize(300, 100);
		this.setTitle("Login screen");
		setLocationOnCenter();
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

	private void initializeServerSetupPanel() {
		serverSetupPanel = new JPanel();
		serverSetupPanel.setPreferredSize(new Dimension(300, 200));

		JLabel ipLabel = new JLabel("IP: ");
		serverSetupPanel.add(ipLabel);

		ipField = new JTextField(defaultIpAddress);
		ipField.setPreferredSize(new Dimension(100, 30));
		serverSetupPanel.add(ipField);

		JLabel portLabel = new JLabel("Port: ");
		serverSetupPanel.add(portLabel);

		portField = new JTextField(String.valueOf(DEFAULT_PORT));
		portField.setPreferredSize(new Dimension(40, 30));
		serverSetupPanel.add(portField);

		serverSetupPanel.setVisible(false);
		this.getContentPane().add(serverSetupPanel);
	}

	/**
	 * ConnectionListener that connects to a specific server. If the connection is successful the login frame is
	 * disposed and the client is transferred to the parent frame(ClientFrame).
	 * 
	 * @author Alexander Andreev
	 */
	public class ConnectionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			client = new Client(usernameField.getText());
			if (serverSetupCheckBox.isSelected()) {
				setCustomServerAddressValues();
			} else {
				client.setServerPort(DEFAULT_PORT);
				client.setServerAddress(defaultIpAddress);
			}

			if (client.getServerPort() > PORT_MIN && client.getServerPort() < PORT_MAX) {
				try {
					accepted = client.connectToServer();
				} catch (InterruptedException e1) {
					System.out.println(e);
					System.out.println("Thread interrupted while trying to connect to server");
					Thread.currentThread().interrupt();
				}
				if (accepted) {
					parentFrame.setClient(client);
					JOptionPane.showMessageDialog(parentFrame,
							"Connected to server with username: " + client.getName());
					logedIn = true;
					dispose();
				} else {
					System.out.println("The user's username is already taken or invalid.");
					JOptionPane.showMessageDialog(null,
							"The username is taken or invalid, please enter a new one. The following characters are forbidden: ']', '['");
				}
			} else {
				JOptionPane.showMessageDialog(null, "Port number not in accepted limits. Please enter a new one");
				portField.setText("");
			}
		}

		/**
		 * Server address values are set from the specified text fields.
		 */
		private void setCustomServerAddressValues() {
			if (portIsValid()) {
				client.setServerPort(Integer.parseInt(portField.getText()));
			} else {
				System.out.println("Invalid port entered. The default one will be used");
				client.setServerPort(DEFAULT_PORT);
			}
			if (ipAddressIsValid()) {
				client.setServerAddress(ipField.getText());
			} else {
				System.out.println("Invalid ip entered. The default one will be used");
				client.setServerAddress(defaultIpAddress);
			}
		}

		/**
		 * Checks if the ipField holds a valid ip address value.
		 * 
		 * @return true if the value is a valid ip address.
		 */
		private boolean ipAddressIsValid() {
			return ipField.getText().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		}

		/**
		 * Checks if the portField holds a valid port value.
		 * 
		 * @return true if the value is a valid port.
		 */
		private boolean portIsValid() {
			return portField.getText().matches("[0-9]+");
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
				enableServerSettings();
			} else {
				disableServerSettings();
			}
		}

		/**
		 * Enables the additional setting to setup server ip and port.
		 */
		private void enableServerSettings() {
			thisFrame.setSize(300, 140);
			serverSetupPanel.setVisible(true);
		}

		/**
		 * Disables the additional setting to setup server ip and port.
		 */
		private void disableServerSettings() {
			thisFrame.setSize(300, 100);
			serverSetupPanel.setVisible(false);
		}
	}
}