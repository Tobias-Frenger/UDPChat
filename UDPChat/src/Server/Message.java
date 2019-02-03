package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * This class handles multiple message types.
 * 
 * @author a16tobfr
 */

public class Message {
	private Server server;
	private String message = "";
	private String senderOfMessageName = "";

	protected Message(Server server) {
		this.server = server;
	}

	private void setSender(String string) {
		senderOfMessageName = string;
	}

	private String getSender() {
		return senderOfMessageName;
	}

	private void setMessage(String string) {
		message = string;
	}

	private String getMessage() {
		return message;
	}

	// Message that the server sends to the client(s) after receiving a /leave
	// message
	protected void messageLeave(String message, String name) throws IOException {
		setSender(name);
		setMessage(message);
		setMessage(getMessage().replaceAll("/leave", ""));
		setMessage(getMessage().replace(getSender() + " -> ", ""));
		if (!getMessage().isEmpty()) {
			broadcast(getSender() + " final note: " + getMessage() + "-leave%");
		}
		server.disconnectClient();
	}

	// Message that the server sends out when a client sends a /tell message
	protected void messageTell(String message, String name) throws IOException {
		ClientConnection c;
		setMessage(message);
		setSender(name);
		String recipentName = "";
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
			// Finds the correct user by looking by finding the combination:
			// /tell <receiver name>
			c = itr.next();
			recipentName = c.getName();
			if (getMessage().contains("/tell " + recipentName)) {
				// RECIPENT MESSAGE
				setMessage(getMessage().replace("/tell " + c.getName(), ""));
				setMessage(getMessage().replace(" -> ", " whispers ->"));
				sendPrivateMessage(getMessage(), c.getName());
				// SENDER MESSAGE
				setMessage(getMessage().replace(getSender(), "You"));
				setMessage(getMessage().replace("whispers", "whisper to " + c.getName()));
				sendPrivateMessage(getMessage(), getSender());
			}
		}
	}

	protected void sendPrivateMessage(String message, String name) throws IOException {
		setMessage(message);
		ClientConnection c;
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				c.sendMessage(getMessage(), server.getSocket());
			}
		}
	}

	protected void broadcast(String message) throws IOException {
		setMessage(message);
		ClientConnection c;
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
			c = itr.next();
			c.sendMessage(getMessage(), server.getSocket());
		}
	}

	protected void printListOfUsers(String name, String message) throws IOException {
		String keyWord = "-list%";
		String top = "---Chat room users---";
		String bot = "\n-----------------------------";
		String nameRow = "        - ";
		String result;
		setMessage(message);
		setSender(name);
		ArrayList<String> names = new ArrayList<>();
		ClientConnection c;
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
			c = itr.next();
			names.add(nameRow + c.getName());
		}
		result = top;
		for (int i = 0; i < names.size(); i++) {
			result += ("\n" + names.get(i));
		}
		result += bot;
		sendPrivateMessage(result + keyWord + getMessage(), getSender());
	}

	protected DatagramPacket retrieveMessage() {
		byte[] messageByte = new byte[1024];
		DatagramPacket dp = new DatagramPacket(messageByte, messageByte.length);
		try {
			server.getSocket().receive(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dp;
	}

	protected String unmarshalMessage(DatagramPacket datap) {
		String message = new String(datap.getData(), 0, datap.getLength());
		return message;
	}

	// receives heart-beat and sets the client to isAlive in clientConnection
	protected String receiveHeartbeat(DatagramPacket dp, String name) {
		ClientConnection c;
		String message = unmarshalMessage(dp);
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
			c = itr.next();
			if (message.contains(c.getName())) {
				c.clientIsAlive();
			}
		}
		return message;
	}
}
