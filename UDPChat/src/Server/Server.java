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
		setMessageID(specialID);
		// might need to remove setMessageID() here
		SMessage.sendPrivateMessage(getMessage(), getSenderName());
		setMessage(getMessage().replace("-ack%", ""));
	}

	private void setMessageID(String string) {
		messageID = string;
	}

	private void serverAckMessageTrimmer() {
		// get name
		String[] temp0 = getMessage().split("-sack%");
		setSenderName(temp0[0]);
		// get id
		String[] temp1 = temp0[1].split("-ID%");
		setMessageID(temp1[0]);
		setMessage(getMessage().replace(getMessage(), "-sack%"));
	}

	private void setAckReceived() {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(getSenderName())) {
				c.setAck(false);
			}
		}
	}

	// Listens for messages and sends the correct type back to the client
	private void listenForClientMessages() throws IOException {
		// key = UUID.toString, value = message
		HashMap<String, String> alreadyHandledMap = new HashMap<>();
		System.out.println("Waiting for client messages... ");
		do {
			// Retrieve message from client
			setDatagramPacket(SMessage.retrieveMessage());
			// Unmarshal message
			setMessage(SMessage.unmarshalMessage(getDatagramPacket()));
			// Check if message is an acknowledgement
			if (getMessage().contains("-sack%")) {
				serverAckMessageTrimmer();
				setAckReceived();
			}
			// Send acknowledgement - message was received
			if (getMessage().contains("-ack%")) {
				ackMessageTrimmer();
				getMessageIDFromMessage(getMessage(), getSenderName());
			}
			if (!getMessage().contains("-sack%")) {
				setSenderName(getClientNameFromMessage());
				// avoids handling of already handled messages
				if (!alreadyHandledMap.containsKey(messageID) || getMessage().contains("-isAlive%")) {
					alreadyHandledMap.put(getMessageID(), getMessage());
					// Receive heart-beat message
					if (getMessage().contains("-isAlive%")) {
						SMessage.receiveHeartbeat(getDatagramPacket(), getSenderName());
					}
					// handle connection messages
					if (!getMessage().contains("-connection%")) {
						setMessage(getMessage().replace("-name%", " -> "));
					}
					// handle
					if (!getMessage().contains("-isAlive%")) {
						decisionBasedOnInput();
					}
				}
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
		// CONNECTED TO SERVER
		if (getMessage().contains("-connection%")) {
			addClient();
			// is the user connected?
			if (isConnected()) {
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
					return false; // Already exists a client with this name
				}
			}
			c = new ClientConnection(getSenderName(), getAddress(), getPort());
			m_connectedClients.add(c);
			clientsConnected.put(getSenderName(), getPort());
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
				SMessage.broadcast(getSenderName() + " disconnected" + "-disconnect%" + getMessageID() + "-ID%");
				m_connectedClients.remove(c);
				clientsConnected.remove(getSenderName());
				SMessage.sendPrivateMessage("-socketDC%", c.getName());
				return true;
			}
		}
		return false;
	}

	// Setter methods
	private void setMessage(String string) {
		message = string;
	}

	protected void setSenderName(String string) {
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