/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ClientConnection {

	static double TRANSMISSION_FAILURE_RATE = 0.0;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private boolean isAlive = false;

	protected ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
		System.out.println("NEW CONNECTION: " + name);
	}

	protected boolean getIsAlive() {
		return isAlive;
	}
	
	protected void clientIsAlive() {
		isAlive = true;
		System.out.println("CLIENT IS ALIVE " + isAlive);
	}
	
	// Checks if the client is still alive.
	// disconnects client otherwise
	protected void isAliveCounter(Server server, String name) {
		Thread thread = new Thread() {
			
			@Override
			public void run() {
				String clientName = name;
				int sleepInMs = 3000;
				
				while (true) {
					System.out.println("Server-side: " + name + " is alive");
					try {
						sleep(sleepInMs);
						if (isAlive) {
							System.out.println("AAAAAAA ALIVE = " + isAlive);
							isAlive = false;
						} else {
							System.out.println("AAAAAAA ALIVE = " + isAlive);
							System.out.println("Disconnecting " + clientName);
							server.disconnectClient();
							break;
						}
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}
	/*
	 * TODO
	 * Implement atleast-once solution
	 * research why -Salive% is being sent to disconnected users.
	 */
	protected void sendMessage(String message, DatagramSocket socket) throws IOException {
		System.out.println("[SERVER] sendMessage() - " + message);
		// artificially produces loss of messages
		Random generator = new Random();
		try {
			double failure = generator.nextDouble();
			if (failure > TRANSMISSION_FAILURE_RATE) {
				DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, m_address,
						m_port);
				socket.send(packet);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	protected String getName() {
		return m_name;
	}

	protected boolean hasName(String testName) {
		return testName.equals(m_name);
	}

}