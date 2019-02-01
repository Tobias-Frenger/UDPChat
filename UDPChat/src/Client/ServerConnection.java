/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author brom
 */
public class ServerConnection {
	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private DatagramSocket m_socket = null;
	private InetAddress m_serverAddress = null;
	private int m_serverPort = -1;
	private Client client;
	private boolean heartBeat = true;
	private boolean m_ack = false;
	
	HashMap<String, Boolean> messageMap = new HashMap<String, Boolean>();
	private ArrayList<String> threadID = new ArrayList<>();
	
	public void setAck(boolean bool) {
		m_ack = bool;
	}
	public boolean getAck()
	{
		return m_ack;
	}
	public ServerConnection(String hostName, int port, Client client) throws SocketException {
		m_serverPort = port;
		this.client = client;
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		m_socket = new DatagramSocket();
	}

	public void setHeartBeat(Boolean bool) {
		heartBeat = bool;
	}

	public boolean hasHeartBeat() {
		return heartBeat;
	}

	public HashMap<String,Boolean> getMessageMap() {
		return messageMap;
	}

	public boolean handshake(String name) throws IOException {
		sendChatMessage(name);
		receiveChatMessage();
		new HeartBeat(this, client).start();
		// * return false if connection failed (e.g., if user name was taken)
		return true;
	}

	public String receiveChatMessage() throws IOException {
		// Receiving message:
		byte[] buf = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		m_socket.receive(packet);
		// unMarshalling message:
		String message = new String(packet.getData(), 0, packet.getLength());
		return message;
	}

	public int getPort() {
		return m_serverPort;
	}

	public InetAddress getAddress() {
		return m_serverAddress;
	}

	public DatagramSocket getSocket() {
		return m_socket;
	}

	public void sendChatMessage(String message) throws IOException {
		sendAtleastOnce(message);
	}

	private String retrieveUniqueID(String message) {
		String[] temp = message.split("-ID%");
		temp[0] = temp[0].replace(client.getName() + "-name%", "");
		return temp[0];
	}

	private void sendAtleastOnce(String message) {
		// artificially produces loss of messages
		Random generator = new Random();
		String uniqueID = retrieveUniqueID(message);
		threadID.add(uniqueID);
		messageMap.put(uniqueID, false);
//		setAcknowledge(messageMap.get(uniqueID));
		Thread thread = new Thread() {
			String threadMessageID = uniqueID;
			int sleepInMs = 110;
			int maxAttempts = 17;
			int attempt = 0;

			public void run() {
				while (!getAck()) {
					System.out.println("CLIENT CHECKS IF TRUE: " + getMessageMap().get(threadMessageID));
					try {
						attempt++;
						double failure = generator.nextDouble();
						if (failure > TRANSMISSION_FAILURE_RATE) {
							if (message.contains("/leave")) {
								setHeartBeat(false);
							}
							DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length,
									m_serverAddress, m_serverPort);
							m_socket.send(packet);
						}
						sleep(sleepInMs);
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
					// stops sending after maxAttempts ->
					// log(10^9)/log(TRANSMISSION_FAILURE_RATE)
					if (attempt == maxAttempts) {
						System.out.println("max attempts was reached: " + attempt);
						break;
					}
//					ack = messageMap.get(threadMessageID);
				}
				setAck(false);
				System.out.println(messageMap.get(threadMessageID).booleanValue());
				System.out.println("sendAtleastOnce() - Thread ending");
			}
		};
		thread.start();
	}
}
