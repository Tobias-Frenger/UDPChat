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
			
			//Retrieve message from client
			byte[] messageByte = new byte[1024];
			DatagramPacket dp = new DatagramPacket(messageByte, messageByte.length);
			m_socket.receive(dp);
			// Unmarshall message
			String message = new String(dp.getData(), 0, dp.getLength());
			
			
			ClientConnection c;
			
			// Retrieve the client name and put inside local string
			String[] tempy = message.split("-name%");
			String clientName = tempy[0];
			message = message.replace("-name%", " -> ");
			
			// CONNECTED TO SERVER
			if (message.contains("-connection%")) { 
				String[] mes = message.split("-connection%");
				addClient(mes[0], dp.getAddress(), dp.getPort());
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
					c = itr.next();
					c.sendMessage(mes[0] + " connected to the server", m_socket);
				}
			}
			if (message.contains("/connect") && !checkIfConnected(clientName)) {
				addClient(clientName, dp.getAddress(), dp.getPort());
				message = message.replace(message, clientName + " has reconnected");
			}
			// Check if sender off message is a connected client
			if (checkIfConnected(clientName)) {
				// PRINT LIST OF USERS TO SELF
				if (message.contains("/list")) { 
					sendPrivateMessage("---Chat room users---", clientName);
					for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
						c = itr.next();
						sendPrivateMessage("        - " + c.getName(), clientName);
					}
					sendPrivateMessage("-----------------------------", clientName);
				} 
				// SEND PRIVATE MESSAGE TO CLIENT
				else if (message.contains("/tell ")) { 
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
				} 
				// USER LEAVE CHAT
				else if (message.contains("/leave")) {
					message = message.replaceAll("/leave", "");
					message = message.replace(clientName + " -> ", "");
					if (!message.isEmpty()) {
						broadcast(clientName + " final note: " + message);
					}
					disconnectClient(clientName);
					m_socket.disconnect();
				} 
				// PRINT MESSAGE
				else { 
					broadcast(message);
				}
			}
		} while (true);
	}
	
	// Method to for the server to check if the sender is a connected client or not
	private boolean checkIfConnected(String name) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				return true; // client is connected
			}
		}
		return false; // client was not connected
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

	public boolean disconnectClient(String name) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			System.out.println(c.getName());
			if (c.hasName(name)) {
				broadcast(name + " disconnected");
				m_connectedClients.remove(c);
				return true;
			}
		}
		System.out.println("Client " + name + " not found\n" + "Unable to disconnect");
		return false;
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
