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
import java.util.HashMap;
import java.util.Iterator;

public class Server {

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private DatagramSocket m_socket;
	private HashMap<String, Integer> clientsConnected = new HashMap<String, Integer>();

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

	// Creates a DatagramSocket with unique port number
	private Server(int portNumber) throws SocketException {
		m_socket = new DatagramSocket(portNumber);
	}

	// Listens for messages and sends the correct type back to the client
	private void listenForClientMessages() throws IOException {
		System.out.println("Waiting for client messages... ");
		do {
			// Retrieve message from client
			DatagramPacket dp = retrieveMessage();
			// Unmarshal message
			String message = unmarshalMessage(dp);
			// Retrieve the client name and put inside local string
			String clientName = getClientName(message);
			// Removing key word from message
			message = message.replace("-name%", " -> ");
			// Respond in correct manner
			decisionBasedOnInput(dp, message, clientName);
		} while (true);
	}
	
	// Method that makes decisions based on the input
	private void decisionBasedOnInput(DatagramPacket datapack, String message, String name) throws IOException {
		// CONNECTED TO SERVER
		if (message.contains("-connection%")) {
			connectClientToServer(message, datapack);
			if (isConnected(name, datapack.getPort())) {
				message = message.replaceAll(message, name + " has connected");
			} else {
				message = message.replaceAll(message, "User tried to connect but failed");
			}
		}

		if (message.contains("/join") && !isConnected(name, datapack.getPort())) {
			message = reconnectClient(message, name, datapack);
		}
		// Check if sender off message is a connected client
		if (isConnected(name, datapack.getPort())) {
			// PRINT LIST OF USERS TO SELF
			if (message.contains("/list")) {
				printListOfUsers(name);
			}
			// SEND PRIVATE MESSAGE TO CLIENT
			else if (message.contains("/tell ")) {
				messageTell(message, name);
			}
			// USER LEAVE CHAT
			else if (message.contains("/leave")) {
				messageLeave(message, name);
			}
			// PRINT MESSAGE
			else {
				broadcast(message);
			}
		}
	}

	private String getClientName(String message) {
		message = message.replace("-connection%", "");
		String[] temp = message.split("-name%");
		return temp[0];
	}

	private void connectClientToServer(String message, DatagramPacket datap) {
		String[] mes = message.split("-connection%");
		addClient(mes[0], datap.getAddress(), datap.getPort());
	}

	// reconnects disconnected user and returns message for broadcast
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
	private boolean isConnected(String name, int port) {
		ClientConnection c;
		if (clientsConnected.containsValue(port)) {
			for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
				c = itr.next();
				if (c.hasName(name)) {
					return true; // client is connected
				}
			}
		}
		return false; // client was not connected
	}

	public boolean addClient(String name, InetAddress address, int port) {
		ClientConnection c = null;
		if (!clientsConnected.containsKey(name)) {
			clientsConnected.put(name, port);
			for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
				c = itr.next();
				if (c.hasName(name)) {
					return false; // Already exists a client with this name
				}
			}
			c = new ClientConnection(name, address, port);
			m_connectedClients.add(c);
			return true;
		}
		return false;
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
//					e.printStackTrace();
//				} catch (IOException e) {
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
