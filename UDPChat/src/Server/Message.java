package Server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Iterator;

public class Message {
	String message;
	String name;
	DatagramSocket socket;
	ArrayList<ClientConnection> cc;
	
	public Message(
			String message, 
			String name, 
			DatagramSocket socket, 
			ArrayList<ClientConnection> cc) 
	{
		this.message = message;
		this.name = name;
		this.socket = socket;
		this.cc = cc;
	}
	
	public void sendPrivateMessage(String message, String toName) throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = cc.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(toName)) {
				c.sendMessage(message, socket);
			}
		}
	}
	
	public void broadcast() throws IOException {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = cc.iterator(); itr.hasNext();) {
			c = itr.next();
			c.sendMessage(message, socket);
		}
	}
}
