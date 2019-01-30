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
import java.util.Random;

/**
 *
 * @author brom
 */
public class ServerConnection {

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.0;

	private DatagramSocket m_socket = null;
	private InetAddress m_serverAddress = null;
	private int m_serverPort = -1;
	private HeartBeat heartBeat;
	private Client client;

	public ServerConnection(String hostName, int port, Client client) throws SocketException {
		m_serverPort = port;
		this.client = client;
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		m_socket = new DatagramSocket();
		heartBeat = new HeartBeat(this,client.getName());
		System.out.println(hostName);
		heartBeat.start();
//		heartBeat(hostName);
	}

	public boolean handshake(String name) throws IOException {
		sendChatMessage(name);
		receiveChatMessage();
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
		if (message.contains("-Salive%")) {
			System.out.println("sAlive");
		}
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
	
//	private void heartBeat(String name) {
//		Thread heartBeat = new Thread() {
//			String message = name + "-Calive%";
//			int sleepTimeInMs = 10000;
//			public void run() {
//				while (true) {
//					try {
//						DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, m_serverAddress, m_serverPort);
//						m_socket.send(packet);
//						sleep(sleepTimeInMs);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//
//		};
//		heartBeat.start();
//	}

	public void sendChatMessage(String message) throws IOException {
		Random generator = new Random();
		double failure = generator.nextDouble();

		if (failure > TRANSMISSION_FAILURE_RATE) {
			// TODO:
			// * marshal message if necessary
			DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, m_serverAddress,
					m_serverPort);
			// * send a chat message to the server
			m_socket.send(packet);
		} else {
			System.out.println("Message lost in the void - SC");
			// Message got lost
		}
	}
}
