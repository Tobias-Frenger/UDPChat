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

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	public void sendMessage(String message, DatagramSocket socket) throws IOException {

		Random generator = new Random();
		double failure = generator.nextDouble();
		System.out.println(message);
		if (failure > TRANSMISSION_FAILURE_RATE) {
			// TODO: send a message to this client using socket.
			DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, m_address,
					m_port);

			socket.send(packet);

		} else {
			// Message got lost
			System.out.println("Message lost in the void - CC");
		}

	}

	public String getName() {
		return m_name;
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

}