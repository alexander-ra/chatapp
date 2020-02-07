package edu.uni.ruse.client;

import static org.junit.Assert.assertEquals;

import edu.uni.ruse.server.Server;
import org.junit.Test;

public class ClientTest {
	
	@Test
	public void testCreateClient() {
		Client client = new Client("client");
		assertEquals("client", client.getName());
	}
	
	@Test
	public void testCreateClientWithAddress() {
		Client client = new Client("client", "127.0.0.1", 7000);
		assertEquals(7000, client.getServerPort());
	}
	
	@Test
	public void testSuccesfullConnectToServer() throws InterruptedException {
		boolean succesfullConnection;
		Client client = new Client("client", "127.0.0.1", 7000);
		Server server = new Server(7000, "127.0.0.1");
		server.setPort(server.getFirstUnocupiedPort());
		server.startServer();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				server.getNewConnection();
				
			}
		}).start();
		client.setServerPort(server.getPort());
		succesfullConnection = client.connectToServer();
		
		assertEquals(true, succesfullConnection);
		server.stopServer();
	}
	
	@Test
	public void testUnsuccesfullConnectToUnknownServer() throws InterruptedException {
		boolean succesfullConnection;
		Client client = new Client("client", "127.0.0.50", 7000);
		succesfullConnection = client.connectToServer();
		
		assertEquals(false, succesfullConnection);
	}
	
	@Test
	public void testUnsuccesfullConnectToServer() throws InterruptedException {
		boolean succesfullConnection;
		Client client = new Client("client[]", "127.0.0.1", 7000);
		Server server = new Server(7000, "127.0.0.1");
		server.setPort(server.getFirstUnocupiedPort());
		server.startServer();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				server.getNewConnection();
				
			}
		}).start();
		client.setServerPort(server.getPort());
		succesfullConnection = client.connectToServer();
		
		assertEquals(false, succesfullConnection);
		server.stopServer();
	}

}
