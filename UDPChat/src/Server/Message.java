package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Iterator;

/*
 * This class handles all the messages 
 * that are being sent to the client
 * 
 * @author a16tobfr
 */

public class Message {
	Server server;

	Message(Server server) {
		this.server = server;
	}

	public void messageLeave(String message, String name) throws IOException {
		message = message.replaceAll("/leave", "");
		message = message.replace(name + " -> ", "");
		if (!message.isEmpty()) {
			broadcast(name + " final note: " + message);
		}
		server.disconnectClient(name);
	}

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
	
	private String receiveHeartbeat(DatagramPacket dp) {
		String message;
		message = new String(dp.getData(), 0, dp.getLength());
		return message;
	}
}
