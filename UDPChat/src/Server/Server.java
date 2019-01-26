package Server;

import java.io.IOException;
import java.net.DatagramPacket;
//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private DatagramSocket m_socket;

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Usage: java Server portnumber");
			System.exit(-1);
		}
		try {
			Server instance = new Server(Integer.parseInt(args[0]));
			instance.listenForClientMessages();
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Server(int portNumber) throws SocketException {
		// TODO: create a socket, attach it to port based on portNumber, and assign it
		// to m_socket
		m_socket = new DatagramSocket(portNumber);
	}

	private void listenForClientMessages() throws IOException {
		System.out.println("Waiting for client messages... ");

		do {
			// TODO: Listen for client messages.
			// On reception of message, do the following:
			// * Unmarshal message
			// * Depending on message type, either
			// - Try to create a new ClientConnection using addClient(), send
			// response message to client detailing whether it was successful
			// - Broadcast the message to all connected users using broadcast()
			// - Send a private message to a user using sendPrivateMessage()
			byte[] bytis = new byte[1024];
			DatagramPacket dp = new DatagramPacket(bytis, bytis.length);
			m_socket.receive(dp);
			String message = new String(dp.getData(), 0, dp.getLength());
			ClientConnection c;

			String[] tempy = message.split("-name%");
			String clientName = tempy[0];
			message = message.replace("-name%", " -> ");

			if (message.contains("-connection%")) { // CONNECTED TO SERVER

				String[] mes = message.split("-connection%");
				addClient(mes[0], dp.getAddress(), dp.getPort());

				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
					c = itr.next();
					c.sendMessage(mes[0] + " connected to the server", m_socket);
				}
			} else if (message.contains("/list")) { // PRINT LIST OF USERS (TO SELF)
				sendPrivateMessage("---Chat room users---", clientName);
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
					c = itr.next();
					sendPrivateMessage("        - " + c.getName(), clientName);
				}
				sendPrivateMessage("-----------------------------", clientName);
			} else if (message.contains("/tell ")) { // SEND PRIVATE MESSAGE
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
					c = itr.next();
					if (message.contains("/tell " + c.getName())) {
						// RECIPENT MESSAGE
						message = message.replace("/tell " + c.getName(), "");
						message = message.replace("->", " whispers ->");
						sendPrivateMessage(message, c.getName());
						// SENDER MESSAGE
						message = message.replace(clientName, "You");
						message = message.replace("whispers", "whisper to " + c.getName());
						sendPrivateMessage(message, clientName);
					}
				}
			} else if (message.contains("/leave")) {
				message = message.replaceAll("/leave", "");
				broadcast(clientName + " disconnected");
				message = message.replace(clientName + " -> ", "");
				if (!message.isEmpty()) {
					broadcast(clientName + " final note: " + message);
				}
			} else { // PRINT MESSAGE
				broadcast(message);
			}
		} while (true);
	}

	public boolean addClient(String name, InetAddress address, int port) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				return false; // Already exists a client with this name
			}
		}
		m_connectedClients.add(new ClientConnection(name, address, port));
		System.out.println("Client added");
		return true;
	}

	public void sendPrivateMessage(String message, String name) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				c.sendMessage(message, m_socket);
			}
		}
	}

	public void broadcast(String message) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			c.sendMessage(message, m_socket);
		}
	}
}
