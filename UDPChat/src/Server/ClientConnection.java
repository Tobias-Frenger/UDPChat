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
	private boolean ackFromClient = false;

	protected ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	protected void setAck(boolean bool) {
		ackFromClient = false;
	}

	private boolean getAck() {
		return ackFromClient;
	}

	protected boolean getIsAlive() {
		return isAlive;
	}

	protected void clientIsAlive() {
		isAlive = true;
	}

	// Checks if the client is still alive.
	// disconnects client otherwise
	protected void isAliveCounter(Server server, String name) {
		Thread thread = new Thread() {

			@Override
			public void run() {
//				String clientName = name;
				int sleepInMs = 3000;

				while (true) {
					try {
						sleep(sleepInMs);
						if (isAlive) {
							isAlive = false;
						} else {
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
	 * TODO Implement 'at-least-once' solution
	 */
	protected void sendMessage(String message, DatagramSocket socket) throws IOException {
		sendAtleastOnce(message, socket);
		System.out.println("[SERVER] sendMessage() - " + message);
		// artificially produces loss of messages
//		Random generator = new Random();
//		try {
//			double failure = generator.nextDouble();
//			if (failure > TRANSMISSION_FAILURE_RATE) {
//				DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, m_address,
//						m_port);
//				socket.send(packet);
//			}
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
	}

	private void sendAtleastOnce(String message, DatagramSocket socket) {
		Thread thread = new Thread() {
			int sleepInMs = 110;
			int maxAttempts = 12;
			int attempt = 0;  
			String serverAck = "-sack%";
			String sendMessage = serverAck + message;
			@Override
			public void run() {
				while (!getAck()) {
					attempt++;
					Random generator = new Random();
					try {
						double failure = generator.nextDouble();
						if (failure > TRANSMISSION_FAILURE_RATE) {
							DatagramPacket packet = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length,
									m_address, m_port);
							socket.send(packet);
						}
						sleep(sleepInMs);
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
					// stops sending after maxAttempts
					if (attempt == maxAttempts) {
						break;
					}
				}
				setAck(false);
				System.out.println("[serverEndSend]");
				System.out.println("[server]sendAtleastOnce() - Thread ending");
			}
		};
		thread.start();
	}

	protected String getName() {
		return m_name;
	}

	protected boolean hasName(String testName) {
		return testName.equals(m_name);
	}

}