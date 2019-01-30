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
	private int sleepTimeInMs = 10000;
	private ServerConnection serverConnection;
	private String name = "";

	public HeartBeat(ServerConnection serverConnection, String name) {
		this.serverConnection = serverConnection;
		this.name = name;
	}

	public void run() {
		String message = name + "-isAlive%";
		while (true) {
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
