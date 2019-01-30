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

	private DatagramPacket retrieveMessage() {
		byte[] messageByte = new byte[1024];
		DatagramPacket dp = new DatagramPacket(messageByte, messageByte.length);
		try {
			m_socket.receive(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dp;
	}

	private String unmarshalMessage(DatagramPacket datap) {
		String message = new String(datap.getData(), 0, datap.getLength());
		return message;
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

			// Retrieve message from client
			DatagramPacket dp = retrieveMessage();
			// Unmarshal message
			String message = unmarshalMessage(dp);

			// Retrieve the client name and put inside local string
			String clientName = retrieveClientName(message);
			// Removing name from message
			message = message.replace("-name%", " -> ");

			// CONNECTED TO SERVER
			if (message.contains("-connection%")) {
				connectClientToServer(message, dp);
				message = message.replaceAll(message, clientName + " has connected");
			}
			if (message.contains("/join") && !checkIfConnected(clientName)) {
				message = reconnectClient(message, clientName, dp);
			}
			// Check if sender off message is a connected client
//			if (checkIfConnected(clientName)) {
			// PRINT LIST OF USERS TO SELF
			if (message.contains("/list")) {
				printListOfUsers(clientName);
			}
			// SEND PRIVATE MESSAGE TO CLIENT
			else if (message.contains("/tell ")) {
				messageTell(message, clientName);
			}
			// USER LEAVE CHAT
			else if (message.contains("/leave")) {
				messageLeave(message, clientName);
			}
			// PRINT MESSAGE
			else {
				broadcast(message);
			}
//			}
		} while (true);
	}

	private String retrieveClientName(String message) {
		message = message.replace("-connection%", "");
		String[] temp = message.split("-name%");

		return temp[0];
	}

	private void connectClientToServer(String message, DatagramPacket datap) {
		String[] mes = message.split("-connection%");
		addClient(mes[0], datap.getAddress(), datap.getPort());
		
	}
	
	private String reconnectClient(String message, String name, DatagramPacket dp) {
		addClient(name, dp.getAddress(), dp.getPort());
		message = message.replace(message, name + " has reconnected");
		return message;
	}

	private void printListOfUsers(String name) throws IOException {
		ClientConnection c;
		sendPrivateMessage("---Chat room users---", name);
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			sendPrivateMessage("        - " + c.getName(), name);
		}
		sendPrivateMessage("-----------------------------", name);
	}

	private void messageLeave(String message, String name) throws IOException {
		message = message.replaceAll("/leave", "");
		message = message.replace(name + " -> ", "");
		if (!message.isEmpty()) {
			broadcast(name + " final note: " + message);
		}
		disconnectClient(name);
	}

	private void messageTell(String message, String name) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (message.contains("/tell " + c.getName())) {
				// RECIPENT MESSAGE
				message = message.replace("/tell " + c.getName(), "");
				message = message.replace("->", " whispers ->");
				sendPrivateMessage(message, c.getName());
				// SENDER MESSAGE
				message = message.replace(name, "You");
				message = message.replace("whispers", "whisper to " + c.getName());
				sendPrivateMessage(message, name);
			}
		}
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
		return true;
	}
	
//	private void heartBeat() {
//		Thread heartBeat = new HeartBeat() {
//		public void run() {
//			byte[] b = new byte[256];
//			heartbeatMessage = new DatagramPacket(b, b.length, iadr, port);
//			
//			while(true) {
//				try {
//					socket.send(heartbeatMessage);
//					sleep(heartBeatIntervallInMs);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		};
//		}
//	}

	public boolean disconnectClient(String name) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			System.out.println(c.getName());
			if (c.hasName(name)) {
				broadcast(name + " disconnected");
				m_connectedClients.remove(c);
				m_socket.disconnect();
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
