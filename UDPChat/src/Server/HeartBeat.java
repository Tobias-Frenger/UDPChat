package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class HeartBeat extends Thread{
	private DatagramSocket socket;
	private InetAddress iadr;
	private int port;
	
	public HeartBeat(DatagramSocket socket, InetAddress iadr, int port) {
		this.socket = socket;
		this.iadr = iadr;
		this.port = port;
	}
	
	private DatagramPacket heartbeatMessage;
	private int heartBeatIntervallInMs = 10000;
	
	public void run() {
		byte[] b = new byte[256];
		heartbeatMessage = new DatagramPacket(b, b.length, iadr, port);
		
		while(true) {
			try {
				socket.send(heartbeatMessage);
				sleep(heartBeatIntervallInMs);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
}
