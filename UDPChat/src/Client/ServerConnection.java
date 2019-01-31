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
//	private HeartBeat heartBeat;

	public ServerConnection(String hostName, int port) throws SocketException {
		m_serverPort = port;
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		m_socket = new DatagramSocket();
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
			message = message.replace(message, "");
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
