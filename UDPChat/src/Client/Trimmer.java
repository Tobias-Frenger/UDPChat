package Client;

import java.io.IOException;
import java.util.UUID;

/*
 * This class trims incoming messages from the server
 * in order for the client to interpret them correctly
 * 
 * @author a16tobfr
 */

public class Trimmer {
	private Client client;

	public Trimmer(Client client) {
		this.client = client;
	};
	
	protected void messageTrimmer() {
		if (client.getMessage().contains("-sack%")) {
			sendAckToServerAndTrim();
		}
		
		if (!client.getMessage().contains("-ack%")) {
			if (!client.getMessage().contains("-leave%") 
					&& !client.getMessage().contains("-reconnect%")
					&& !client.getMessage().contains("-connection%") 
					&& !client.getMessage().contains("-list%")
					&& !client.getMessage().contains("-sack%") 
					&& !client.getMessage().contains("-ack%")
					&& !client.getMessage().contains("-disconnect%")) {
				trimGenericMessages();
			} else if (client.getMessage().contains("-leave%")) {
				trimLeaveMessage();
			} else if (client.getMessage().contains("-reconnect%")) {
				trimReconnectMessage();
			} else if (client.getMessage().contains("-connection%")) {
				trimConnectionMessage();
			} else if (client.getMessage().contains("-list%")) {
				trimListMessage();
			} else if (client.getMessage().contains("-disconnect%")) {
				trimDisconnectMessage();
			}
		}
	}
	
	protected void ackMessageTrimmer() {
		if (client.getMessage().contains("-sack%")) {
			client.setMessage(client.getMessage().replace("-sack%", ""));
		}
		if (client.getMessage().contains("/list")) {
			client.setMessage(client.getMessage().replace("/list", "-ignore%"));
		}
		String[] temp = client.getMessage().split("-ID%");
		client.setSpecialID(temp[0]);
		client.setMessage(client.getMessage().replace(client.getMessage(), "-ack%"));
	}
	
	private void sendAckToServerAndTrim() {
		try {
			String sackID = UUID.randomUUID().toString();
			client.getConnection().sendChatMessage(client.getName() + "-sack%" + sackID + "-ID%");
			client.setMessage(client.getMessage().replace("-sack%", ""));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void trimGenericMessages() {
		String[] extractID = client.getMessage().split("-ID%");
		String idtemp[] = extractID[0].split(" ->");
		client.setSpecialID(idtemp[1]);
		client.setMessage(client.getMessage().replace(client.getSpecialID(), ""));
		client.setMessage(client.getMessage().replace("-ID%", ""));
		if (client.getMessage().contains("/join")) {
			client.setMessage(client.getMessage().replace(client.getMessage(), "-ignore%"));
		}
	}
	
	private void trimLeaveMessage() {
		String[] extractID = client.getMessage().split("-ID%");
		String[] temp = extractID[0].split(" final note: ");
		client.setSpecialID(temp[1]);
		client.setMessage(client.getMessage().replace(temp[1] + "-ID%", ""));
		client.setMessage(client.getMessage().replace("-leave%", ""));
	}
	
	private void trimReconnectMessage() {
		String[] extractID = client.getMessage().split("-reconnect%");
		client.setSpecialID(extractID[1]);
		client.setMessage(client.getMessage().replace(client.getSpecialID(), ""));
		client.setMessage(client.getMessage().replaceAll("-reconnect%", ""));
		if (!client.getConnection().getHeartBeat()) {
			client.getConnection().setHeartBeat(true);
			new HeartBeat(client).start();
		}
	}
	
	private void trimConnectionMessage() {
		String[] extractID = client.getMessage().split("-name%");
		String[] temp = extractID[1].split(client.getName());
		String[] temp1 = temp[0].split("-ID%");
		client.setSpecialID(temp1[0]);
		client.setMessage(client.getMessage()
				.replace("-name%" + client.getSpecialID() + "-ID%" + "-connection%", " has connected"));
	}
	
	private void trimListMessage() {
		String[] extractID = client.getMessage().split("-list%");
		String[] temp = extractID[1].split("-ID%");
		if (client.getMessage().contains("/list")) {
			client.setMessage(client.getMessage().replace("/list", ""));
		}
		client.setSpecialID(temp[0]);
		client.setMessage(client.getMessage().replaceAll("-list%" + client.getSpecialID() + "-ID%", ""));
	}
	
	private void trimDisconnectMessage() {
		String[] temp0 = client.getMessage().split("-disconnect");
		String[] temp1 = temp0[1].split("-ID%");
		client.setSpecialID(temp1[0]);
		client.setMessage(temp0[0]);
	}
}
