package Client;

import java.io.IOException;
import java.util.UUID;

public class Trimmer {
	private Client client;

	public Trimmer(Client client) {
		this.client = client;
	};

	protected void messageTrimmer() {

		if (client.getMessage().contains("-sack%")) {
			System.out.println("ms6 " + client.getMessage());
			System.out.println("-sack%MESSAGE CONTAINS: " + client.getMessage());
			try {
				// send ack message to server
				System.out.println("sending -sack% message to server");
				String sackID = UUID.randomUUID().toString();
				client.getConnection().sendChatMessage(client.getName() + "-sack%" + sackID + "-ID%");
				client.setMessage(client.getMessage().replace("-sack%", ""));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!client.getMessage().contains("-ack%")) {
			if (!client.getMessage().contains("-leave%") && !client.getMessage().contains("-reconnect%")
					&& !client.getMessage().contains("-connection%") && !client.getMessage().contains("-list%")
					&& !client.getMessage().contains("-sack%") && !client.getMessage().contains("-ack%")
					&& !client.getMessage().contains("-disconnect%")) {
				System.out.println("ms1 " + client.getMessage());
				String[] extractID = client.getMessage().split("-ID%");
				String idtemp[] = extractID[0].split(" ->");
				client.setSpecialID(idtemp[1]);
				client.setMessage(client.getMessage().replace(client.getSpecialID(), ""));
				client.setMessage(client.getMessage().replace("-ID%", ""));
				if (client.getMessage().contains("/join")) {
					client.setMessage(client.getMessage().replace(client.getMessage(), "-ignore%"));
				}
				// leave message
			} else if (client.getMessage().contains("-leave%")) {
				System.out.println("ms2 " + client.getMessage());
				String[] extractID = client.getMessage().split("-ID%");
				String[] temp = extractID[0].split(" final note: ");
				client.setSpecialID(temp[1]);
				client.setMessage(client.getMessage().replace(temp[1] + "-ID%", ""));
				client.setMessage(client.getMessage().replace("-leave%", ""));
				// reconnect message
			} else if (client.getMessage().contains("-reconnect%")) {
				System.out.println("ms3 " + client.getMessage());
				String[] extractID = client.getMessage().split("-reconnect%");
				client.setSpecialID(extractID[1]);
				client.setMessage(client.getMessage().replace(client.getSpecialID(), ""));
				client.setMessage(client.getMessage().replaceAll("-reconnect%", ""));
				if (!client.getConnection().getHeartBeat()) {
					client.getConnection().setHeartBeat(true);
					new HeartBeat(client).start();
				}
				System.out.println("JOIN ATTEMPT: " + client.getMessage());
			} else if (client.getMessage().contains("-connection%")) {
				System.out.println("ms4 " + client.getMessage());
				String[] extractID = client.getMessage().split("-name%");
				String[] temp = extractID[1].split(client.getName());
				String[] temp1 = temp[0].split("-ID%");
				client.setSpecialID(temp1[0]);
				System.out.println("MS4 SPECIALID: " + client.getSpecialID());
				client.setMessage(client.getMessage()
						.replace("-name%" + client.getSpecialID() + "-ID%" + "-connection%", " has connected"));
				System.out.println("MS4 MESSAGE: " + client.getMessage());
			} else if (client.getMessage().contains("-list%")) {
				System.out.println("ms5 " + client.getMessage());
				String[] extractID = client.getMessage().split("-list%");
				String[] temp = extractID[1].split("-ID%");
				if (client.getMessage().contains("/list")) {
					client.setMessage(client.getMessage().replace("/list", ""));
				}
				client.setSpecialID(temp[0]);
				client.setMessage(client.getMessage().replaceAll("-list%" + client.getSpecialID() + "-ID%", ""));
			} else if (client.getMessage().contains("-disconnect%")) {
				System.out.println("ms7");
				String[] temp0 = client.getMessage().split("-disconnect");
				String[] temp1 = temp0[1].split("-ID%");
				client.setSpecialID(temp1[0]);
				client.setMessage(temp0[0]);
			}
		}
	}
	
	public void ackMessageTrimmer() {
		System.out.println("MESSAGE C ACK PRE: " + client.getMessage());
		if (client.getMessage().contains("-sack%")) {
			client.setMessage(client.getMessage().replace("-sack%", ""));
		}
		if (client.getMessage().contains("/list")) {
			client.setMessage(client.getMessage().replace("/list", "-ignore%"));
		}
		String[] temp = client.getMessage().split("-ID%");
		client.setSpecialID(temp[0]);
		client.setMessage(client.getMessage().replace(client.getMessage(), "-ack%"));
		System.out.println("MESSAGE C ACK POST: " + client.getMessage() + "\n" + " - " + client.getSpecialID());
	}
}
