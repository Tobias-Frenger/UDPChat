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

/*
 * TODO
 * Clean up the code, split into new/existing classes if necessary
 */
public class Server {

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private DatagramSocket m_socket;
	private HashMap<String, Integer> clientsConnected = new HashMap<String, Integer>();
	private Message SMessage = new Message(this);
	private HeartBeat heartBeat;
	private String message = "";
	private String messageID = "";
	private String nameOfSender = "";
	private DatagramPacket datagramPacket;

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

	protected Message messages() {
		return SMessage;
	}

	protected ArrayList<ClientConnection> getConnectedClients() {
		return m_connectedClients;
	}

	// Creates a DatagramSocket with unique port number
	private Server(int portNumber) throws SocketException {
		m_socket = new DatagramSocket(portNumber);
		heartBeat = new HeartBeat(this);
		heartBeat.start();
	}

	private void ackMessageTrimmer() throws IOException {
		String[] extractID = getMessage().split("-ID%");
		String[] temp = extractID[0].split("-name%");
		String specialID = temp[1];
		System.out.println("S ACK RECIEVED: " + getMessage());
		System.out.println("SERVER SPECIALID: " + specialID);
		SMessage.sendPrivateMessage(specialID + "-ID%" + "-ack%", getSenderName());
		setMessage(getMessage().replace("-ack%", ""));
	}

	// Listens for messages and sends the correct type back to the client
	private void listenForClientMessages() throws IOException {
		System.out.println("Waiting for client messages... ");
		do {
			// Retrieve message from client
			setDatagramPacket(SMessage.retrieveMessage());
			// Unmarshal message
			setMessage(SMessage.unmarshalMessage(getDatagramPacket()));
			// Retrieve the client name and put inside local string
			setSenderName(getClientNameFromMessage());
			// Send acknowledgement - message was received
//			String messageID = getMessageID(getMessage(), getSenderName());

			if (getMessage().contains("-ack%")) {
				ackMessageTrimmer();
				getMessageIDFromMessage(getMessage(), getSenderName());
			}
			// Receive heart beat message
			if (getMessage().contains("-isAlive%")) {
				System.out.println("()Server detect: " + getMessage());
				SMessage.receiveHeartbeat(getDatagramPacket(), getSenderName());
			}
			// Removing key words and messageID from message
			if (!getMessage().contains("-connection%")) {
				setMessage(getMessage().replace("-name%", " -> "));
			} 
			// Respond in correct manner
			if (!getMessage().contains("-isAlive%")) {
				decisionBasedOnInput();
			}
		} while (true);
	}

	private String getMessageIDFromMessage(String message, String name) {
		String[] temp = message.split("-ID%");
		temp[0] = temp[0].replace("-ack%", " ");
		temp[0] = temp[0].replace(name + "-name%", "");
		messageID = temp[0];
		return temp[0];
	}

	private String getMessageID() {
		return messageID;
	}

	// Method that makes decisions based on the input
	private void decisionBasedOnInput() throws IOException {
		System.out.println(getMessage());
		// CONNECTED TO SERVER
		if (getMessage().contains("-connection%")) {
			addClient();
			// is the user connected?
			if (isConnected()) {
				setMessage(getMessage().replace(getMessage(), getSenderName() + " has connected" + "-connection%" + getMessageID() + "-ID%"));
			} else {
				setMessage(getMessage().replace(getMessage(), "User tried to connect but failed"));
			}
		}
		// is the user trying to reconnect?
		if (getMessage().contains("/join") && !isConnected()) {
			reconnectClient();
		}
		// Check if sender off message is a connected client
		if (isConnected()) {
			// requesting a list of users
			if (getMessage().contains("/list")) {
				setMessage(getMessage().replace(getSenderName() + " -> ", ""));
				setMessage(getMessage().replace("/list", ""));
				SMessage.printListOfUsers(getSenderName(), getMessage());
			}
			// send a private message
			else if (getMessage().contains("/tell ")) {
				SMessage.messageTell(getMessage(), getSenderName());
			}
			// is user trying to disconnect?
			else if (getMessage().contains("/leave")) {
				SMessage.messageLeave(getMessage(), getSenderName());
			}
			// PRINT MESSAGE
			else {
				SMessage.broadcast(getMessage());
			}
		}
	}

	// This method needs to be implemented
	// after the message is received for
	// the first time.
	private String getClientNameFromMessage() {
		String temp0 = getMessage();
		temp0 = temp0.replace("-connection%", "");
		temp0 = temp0.replace("-ack%", "");
		String[] temp1 = temp0.split("-ID%");
		String[] temp2 = temp1[0].split("-name%");
		return temp2[0];
	}

	// Reconnects disconnected user and returns message for broadcast
	private void reconnectClient() throws IOException {
		setMessage(getMessage().replace(getMessage(),
				getSenderName() + " has reconnected" + "-reconnect%" + getMessageID() + "-ID%"));
		addClient();
		System.out.println("--RECONNECT MESSAGE: " + getMessage());
	}

	// Method to for the server to check if the sender is a connected client or not
	private boolean isConnected() {
		ClientConnection c;
		if (clientsConnected.containsValue(getPort())) {
			for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
				c = itr.next();
				if (c.hasName(getSenderName())) {
					return true; // client is connected
				}
			}
		}
		return false; // client was not connected
	}

	protected boolean addClient() {
		ClientConnection c = null;
		if (!clientsConnected.containsKey(getSenderName())) {
			
			for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
				c = itr.next();
				if (c.hasName(getSenderName())) {
					System.out.println("CLIENT EXISTS");
					return false; // Already exists a client with this name
				}
			}
			c = new ClientConnection(getSenderName(), getAddress(), getPort());
			m_connectedClients.add(c);
			clientsConnected.put(getSenderName(), getPort());
			System.out.println(":::: " + getSenderName() + " WAS ADDED");
			// starts the counter which checks if a value has been updated or not
			c.isAliveCounter(this, getSenderName());
			return true;
		}
		return false;
	}

	protected boolean disconnectClient() throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			// disconnect
			if (c.hasName(getSenderName())) {
				SMessage.broadcast(getSenderName() + " disconnected");
				m_connectedClients.remove(c);
				clientsConnected.remove(getSenderName());
				SMessage.sendPrivateMessage("-socketDC%", c.getName());
				return true;
			}
		}
		System.out.println("Tried to disconnect " + getSenderName() + " but client was already disconnected");
		return false;
	}

	// Setter methods
	private void setMessage(String string) {
		message = string;
	}

	private void setSenderName(String string) {
		nameOfSender = string;
	}

	private void setDatagramPacket(DatagramPacket pack) {
		datagramPacket = pack;
	}

	// Getter methods
	private String getMessage() {
		return message;
	}

	private String getSenderName() {
		return nameOfSender;
	}

	private DatagramPacket getDatagramPacket() {
		return datagramPacket;
	}

	private int getPort() {
		return getDatagramPacket().getPort();
	}

	private InetAddress getAddress() {
		return getDatagramPacket().getAddress();
	}

	protected DatagramSocket getSocket() {
		return m_socket;
	}
}
