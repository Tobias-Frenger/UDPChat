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

	public ServerConnection(String hostName, int port) throws SocketException {
		m_serverPort = port;

		// TODO:
		// DONE * get address of host based on parameters and assign it to
		// m_serverAddress
		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		// DONE * set up socket and assign it to m_socket
		m_socket = new DatagramSocket();
	}

	public boolean handshake(String name) throws IOException {
		// TODO:
		// * marshal connection message containing user name
		// * send message via socket
		sendChatMessage(name);
		// * receive response message from server
		// * unmarshal response message to determine whether connection was successful
		receiveChatMessage();
		// * return false if connection failed (e.g., if user name was taken)
		return true;
	}

	public String receiveChatMessage() throws IOException {
		// TODO:
		// * receive message from server
		// * unmarshal message if necessary
		// Note that the main thread can block on receive here without
		// problems, since the GUI runs in a separate thread
		// Update to return message contents
		// Receiving message:
		byte[] buf = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		m_socket.receive(packet);
		// unMarshalling message:
		String message = new String(packet.getData(), 0, packet.getLength());
		return message;
	}

	public void sendChatMessage(String message) throws IOException {
		Random generator = new Random();
		double failure = generator.nextDouble();

		if (failure > TRANSMISSION_FAILURE_RATE) {
			// TODO:
			DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, m_serverAddress,
					m_serverPort);
			// * marshal message if necessary
			m_socket.send(packet);
			// * send a chat message to the server
		} else {
			System.out.println("Message lost in the void - SC");
			// Message got lost
		}
	}
}
