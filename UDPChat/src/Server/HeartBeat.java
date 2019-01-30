package Server;

import java.io.IOException;
import java.util.Iterator;

/*
 * This is an implementation of the heartbeat
 * algorithm which is used by the server in order
 * let the client know that the server is up
 * and running
 * 
 * @author a16tobfr
 */

public class HeartBeat extends Thread{
	private int sleepTimeInMs = 10000;
	private Server server;
	
	public HeartBeat(Server server) {
		this.server = server;
	}
	
	public void run() {
		String message = "-Salive%";
		while (true) {
			try {
				ClientConnection c = null;
				for (Iterator<ClientConnection> itr = server.getConnectedClients().iterator(); itr.hasNext();) {
					c = itr.next();
					server.messages().sendPrivateMessage(message, c.getName());
				}
				sleep(sleepTimeInMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
