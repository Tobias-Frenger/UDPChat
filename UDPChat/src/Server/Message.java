package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Iterator;

/*
 * This class handles all the messages 
 * that are being sent to the client 
 * from the server 
 * 
 * @author a16tobfr
 */

/*
 * TODO
 * Make class more fitting to the class description
 */
public class Message {
	Server server;

	Message(Server server) {
		this.server = server;
	}
	
	// Message that the server sends to the client(s) after receiving a /leave message
	public void messageLeave(String message, String name) throws IOException {
		message = message.replaceAll("/leave", "");
		message = message.replace(name + " -> ", "");
		if (!message.isEmpty()) {
			broadcast(name + " final note: " + message);
		}
		server.disconnectClient(name);
	}
	
	/*
	 * TODO
	 * Fix on client side.
	 * Messages not output to GUI.
	 */
	// Message that the server sends out when a client sends a /tell message
	public void messageTell(String message, String name) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
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
	
	public void sendPrivateMessage(String message, String name) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				c.sendMessage(message, server.getSocket());
			}
		}
	}

	public void broadcast(String message) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
			c = itr.next();
			c.sendMessage(message, server.getSocket());
		}
	}

	/*
	 * TODO
	 * Fix method to send message instead of name
	 * uniqueID needs to be sent along with the message
	 */
	public void printListOfUsers(String name) throws IOException {
		ClientConnection c;
		sendPrivateMessage("---Chat room users---", name);
		for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
			c = itr.next();
			sendPrivateMessage("        - " + c.getName(), name);
		}
		sendPrivateMessage("-----------------------------", name);
	}
	
	public DatagramPacket retrieveMessage() {
		byte[] messageByte = new byte[1024];
		DatagramPacket dp = new DatagramPacket(messageByte, messageByte.length);
		try {
			server.getSocket().receive(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dp;
	}

	public String unmarshalMessage(DatagramPacket datap) {
		String message = new String(datap.getData(), 0, datap.getLength());
		return message;
	}

	public String receiveHeartbeat(DatagramPacket dp, String name) {
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
