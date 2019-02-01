package Client;

import java.io.IOException;
import java.net.DatagramPacket;

/*
 * This is an implementation of the heartbeat
 * algorithm which is used by every client in 
 * order to let the server know that the client
 * is alive
 * 
 * @author a16tobfr
 */

public class HeartBeat extends Thread {
	private int sleepTimeInMs = 1000;
	private ServerConnection serverConnection;
	private Client client;

	public HeartBeat(ServerConnection serverConnection, Client client) {
		this.serverConnection = serverConnection;
		this.client = client;
	}
	
	@Override
	public void run() {
		String name = client.getName();
		String message = name + "-isAlive%";
		while (serverConnection.hasHeartBeat()) {
			try {
				DatagramPacket packet = new DatagramPacket(
							message.getBytes(), 
							message.getBytes().length,
							serverConnection.getAddress(), 
							serverConnection.getPort()
						);
				serverConnection.getSocket().send(packet);
				sleep(sleepTimeInMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
