package edu.uni.ruse.server;


import static org.junit.Assert.assertEquals;

import java.io.IOException;

import edu.uni.ruse.client.Client;
import edu.uni.ruse.utilities.CodeMessages;
import edu.uni.ruse.utilities.InterfaceLang;
import edu.uni.ruse.utilities.MessagesManager;
import org.junit.Test;


public class ServerTest {

	@Test
	public void testServerStart() {
		Server server = new Server();
		server.startServer();
		assertEquals(true, server.isRunning());
		server.stopServer();
	}

	@Test
	public void testServerStartGivenPort() {
		Server server = new Server(7010);
		server.startServer();
		assertEquals(7010, server.getPort());
		server.stopServer();
	}

	@Test
	public void testServerStartGivenAddressAndPort() {
		Server server = new Server(7000, "127.0.0.2");
		server.startServer();
		assertEquals("127.0.0.2", server.getIpAddress());
		server.stopServer();
	}
	
	@Test
	public void testChangeServerPortBeforeStart() {
		Server server = new Server(7000, "127.0.0.2");
		server.setPort(7010);
		server.startServer();
		assertEquals(7010, server.getPort());
		server.stopServer();
	}
	
	@Test
	public void testChangeServerAddressBeforeStart() {
		Server server = new Server(7000, "127.0.0.2");
		server.setIpAddress("127.0.0.1");
		server.setPort(server.getFirstUnocupiedPort());
		server.startServer();
		assertEquals("127.0.0.1", server.getIpAddress());
		server.stopServer();
	}
	
	@Test
	public void testTryToChangeServerPortAftertart() {
		Server server = new Server(7000, "127.0.0.2");
		server.startServer();
		server.setPort(7010);
		assertEquals(7000, server.getPort());
		server.stopServer();
	}
	
	@Test
	public void testTryToChangeServerAddressAfterStart() {
		Server server = new Server(7000, "127.0.0.2");
		server.startServer();
		server.setIpAddress("127.0.0.1");
		assertEquals("127.0.0.2", server.getIpAddress());
		server.stopServer();
	}

	@Test
	public void testAutomaticPortSelection() {
		Server serverOne = new Server();
		serverOne.startServer();
		Server serverTwo = new Server();
		serverTwo.startServer();
		assertEquals(true, serverOne.getPort() < serverTwo.getPort());
		serverOne.stopServer();
		serverTwo.stopServer();
	}

	@Test
	public void testServerStop() {
		Server server = new Server();
		server.startServer();
		server.stopServer();
		assertEquals(false, server.isRunning());
		server.stopServer();
	}

	@Test
	public void testAcceptinhNewConnection() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			while (true) {
				server.getNewConnection();
			}
		};
		new Thread(connectionListener).start();
		Client clientInvalidName = new Client("client", server.getIpAddress(), server.getPort());
		assertEquals(true, clientInvalidName.connectToServer());
		server.stopServer();

	}

	@Test
	public void testRejectingNewConnectionExsistingName() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			while (true) {
				server.getNewConnection();
			}
		};
		new Thread(connectionListener).start();
		Client client = new Client("client", server.getIpAddress(), server.getPort());
		client.connectToServer();
		Client clientSameName = new Client("client", server.getIpAddress(), server.getPort());
		assertEquals(false, clientSameName.connectToServer());
		server.stopServer();

	}

	@Test
	public void testRejectingNewConnectionInvalidName() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			while (true) {
				server.getNewConnection();
			}
		};
		new Thread(connectionListener).start();
		Client clientInvalidName = new Client("client[]", server.getIpAddress(), server.getPort());
		assertEquals(false, clientInvalidName.connectToServer());
		server.stopServer();

	}

	@Test
	public void testRejectingNewConnectionShortName() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			while (true) {
				server.getNewConnection();
			}
		};
		new Thread(connectionListener).start();
		Client clientShortName = new Client("a", server.getIpAddress(), server.getPort());
		assertEquals(false, clientShortName.connectToServer());
		server.stopServer();

	}

	@Test
	public void testRejectingNewConnectionInvalidAndShortName() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			while (true) {
				server.getNewConnection();
			}
		};
		new Thread(connectionListener).start();
		Client clientInvalidName = new Client("[]", server.getIpAddress(), server.getPort());
		assertEquals(false, clientInvalidName.connectToServer());
		server.stopServer();

	}

	@Test
	public void testRemoveUser() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			while (true) {
				server.getNewConnection();
			}
		};
		new Thread(connectionListener).start();
		Client client = new Client("client", server.getIpAddress(), server.getPort());
		client.connectToServer();
		Thread.sleep(250);
		server.removeUser(client.getName());
		assertEquals(0, server.getNamesToConnections().size());
		server.stopServer();

	}

	@Test
	public void testProcessMessageToRemoveUser() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			server.getNewConnection();
		};
		new Thread(connectionListener).start();
		Client client = new Client("client", server.getIpAddress(), server.getPort());
		client.connectToServer();
		Thread.sleep(250);
		client.sendMessage(CodeMessages.REMOVEUSER.getMessage() + client.getName());
		server.collectNewMessages();
		server.processOldestMessage();
		assertEquals(0, server.getNamesToConnections().size());
		server.stopServer();

	}

	@Test
	public void testProcessMessageChangeLanguage() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			server.getNewConnection();
		};
		new Thread(connectionListener).start();
		Client client = new Client("client", server.getIpAddress(), server.getPort());
		client.connectToServer();
		client.setLanguage(InterfaceLang.BG);
		Thread.sleep(250);
		client.sendMessage(CodeMessages.CHANGE_LANG.getMessage() + client.getName());
		server.collectNewMessages();
		server.processOldestMessage();
		assertEquals(InterfaceLang.BG,
				server.getLangPreferences().get(server.getNamesToConnections().get(client.getName())));
		server.stopServer();

	}

	@Test
	public void testProcessMessageNormalMessage() throws InterruptedException, IOException {
		String message = "Hello";
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			for (int i = 0; i < 2; i++) {
				server.getNewConnection();
			}
		};
		new Thread(connectionListener).start();
		Client client = new Client("client", server.getIpAddress(), server.getPort());
		client.connectToServer();
		Client reciveingClient = new Client("receiver", server.getIpAddress(), server.getPort());
		reciveingClient.connectToServer();
		Thread.sleep(250);
		client.sendMessage(client.getName() + ": " + message);
		server.collectNewMessages();
		while (server.havesUnprocessedClientMessages()) {
			server.processOldestMessage();
			server.collectNewMessages();
			reciveingClient.receiveMessage();
		}
		Thread.sleep(250);
		reciveingClient.receiveMessage();
		reciveingClient.receiveMessage();
		reciveingClient.receiveMessage();
		reciveingClient.receiveMessage();
		reciveingClient.receiveMessage();
		assertEquals(true, reciveingClient.getReceivedMessage().endsWith(message));
	}

	@Test
	public void testProcessMessageChangeUserName() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			server.getNewConnection();
		};
		new Thread(connectionListener).start();
		Client client = new Client("clientFirstName", server.getIpAddress(), server.getPort());
		client.connectToServer();
		client.setLanguage(InterfaceLang.BG);
		Thread.sleep(250);
		client.sendMessage(client.getName() + ": " + CodeMessages.CHANGE_USERNAME.getMessage() + " " + "clientSecondName");
		server.collectNewMessages();
		server.processOldestMessage();
		assertEquals(true, server.getNamesToConnections().containsKey("clientSecondName"));
		server.stopServer();

	}

	@Test
	public void testProcessWhisperMessage() throws InterruptedException {
		Server server = new Server();
		server.startServer();
		Runnable connectionListener = () -> {
			server.getNewConnection();
			server.getNewConnection();
		};
		new Thread(connectionListener).start();
		Client clientOne = new Client("clientOne", server.getIpAddress(), server.getPort());
		clientOne.connectToServer();
		clientOne.setLanguage(InterfaceLang.BG);
		Thread.sleep(250);
		Client clientTwo = new Client("clientTwo", server.getIpAddress(), server.getPort());
		clientTwo.connectToServer();
		clientTwo.setLanguage(InterfaceLang.BG);
		Thread.sleep(250);

		clientOne.sendMessage(clientOne.getName() + ": " + CodeMessages.WHISPER.getMessage() + " " + clientTwo.getName() + " test");
		server.collectNewMessages();
		server.processOldestMessage();
		assertEquals(2, server.getNamesToConnections().size());
	}


}
