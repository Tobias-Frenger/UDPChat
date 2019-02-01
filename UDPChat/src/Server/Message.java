package Server;

import java.io.IOException;
import java.net.DatagramPacket;
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

	Message(Server server) {
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
	
	// Message that the server sends to the client(s) after receiving a /leave message
	public void messageLeave(String message, String name) throws IOException {
		setSender(name);
		setMessage(message);
		setMessage(getMessage().replaceAll("/leave", ""));
		setMessage(getMessage().replace(getSender() + " -> ", ""));
		if (!getMessage().isEmpty()) {
			broadcast(getSender() + " final note: " + getMessage() + "-leave%");
		}
		server.disconnectClient(getSender());
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
			// Finds the correct user by looking by finding the combination:
			// /tell <receiver name>
			if (message.contains("/tell " + c.getName())) {
				// RECIPENT MESSAGE
				message = message.replace("/tell " + c.getName(), "");
				message = message.replace("->", " whispers ->");
				System.out.println("messageTell(): " + message + " - - " + c.getName());
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
