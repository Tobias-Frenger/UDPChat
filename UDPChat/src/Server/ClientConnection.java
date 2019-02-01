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

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
		System.out.println("NEW CONNECTION: " + name);
	}

	public void clientIsAlive() {
		isAlive = true;
	}
	
	// Checks if the client is still alive.
	// disconnects client otherwise
	public void isAliveCounter(Server server, String name) {
		Thread thread = new Thread() {
			
			@Override
			public void run() {
				String clientName = name;
				int sleepInMs = 3000;
				while (true) {
					System.out.println("Server-side: " + clientName + " is alive");
					try {
						sleep(sleepInMs);
						if (isAlive) {
							isAlive = false;
						} else {
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
	 */
	public void sendMessage(String message, DatagramSocket socket) throws IOException {
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

	public String getName() {
		return m_name;
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

}