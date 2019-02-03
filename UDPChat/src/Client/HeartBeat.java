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
	private Client client;
	public HeartBeat(Client client) {
		this.client = client;
	}
	
	@Override
	public void run() {
		String name = client.getName();
		String message = name + "-isAlive%";
		while (client.getConnection().getHeartBeat()) {
			try {
				DatagramPacket packet = new DatagramPacket(
							message.getBytes(), 
							message.getBytes().length,
							client.getConnection().getAddress(), 
							client.getConnection().getPort()
						);
				client.getConnection().getSocket().send(packet);
				System.out.println(client.getConnection().getHeartBeat());
				sleep(sleepTimeInMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
