package Client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class Client implements ActionListener {
	// TEST COMMENT COMMIT
	private String m_name = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.println("Usage: java Client serverhostname serverportnumber username");
			System.exit(-1);
		}
		try {
			Client instance = new Client(args[2]);
			instance.connectToServer(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Client(String userName) {
		m_name = userName;
		// Start up GUI (runs in its own thread)
		m_GUI = new ChatGUI(this, m_name);
	}

	public ServerConnection getConnection() {
		return m_connection;
	}

	public String getName() {
		return m_name;
	}

	private void connectToServer(String hostName, int port) throws IOException {
		// Create a new server connection
		m_connection = new ServerConnection(hostName, port, this);
		System.out.println("client-side connect");
		String id = UUID.randomUUID().toString();
		if (m_connection.handshake("-ack%" + m_name + "-name%" + id + "-ID%" + "-connection%")) {
			listenForServerMessages();
		} else {
			System.err.println("Unable to connect to server");
		}
	}

	/*
	 * TODO
	 * Make code more readable
	 * split into functions
	 * NEED SEPERATE HASHMAP FOR RECEIVER ID'S
	 */
	private void listenForServerMessages() throws IOException {
		HashMap<String, Boolean> receiverMap = new HashMap<>();
		do {
			String message = m_connection.receiveChatMessage();
			// cleaning up incomming message
			if (message.contains("-ID%")) {
				System.out.println("incomming: " + message);
				String specialID = "";
				if (!message.contains("-ack%")) {
					// extracting the special id from the message
					// removing unecessary information from message
					String[] extractID = message.split("-ID%");
					String idtemp[] = extractID[0].split(" -> ");
					idtemp[1] = idtemp[1].replace("-ID%", "");
					specialID = idtemp[1];
					String[] temp = extractID[0].split(" -> ");
					specialID = temp[1];
					message = message.replace(specialID, "");
					message = message.replace("-ID%", "");
					System.out.println("----\nMESSAGE C NO ACK: " + message + "\n" + specialID + "\n----");
				} else {
					System.out.println("MESSAGE C ACK PRE: " + message);
					String[] temp = message.split("-ID%");
					specialID = temp[0];
					message = message.replace(specialID + "-ID%", "");
					System.out.println("MESSAGE C ACK POST: " + message + "\n" + " - " + specialID);
				}

				System.out.println("incomming specialID: " + specialID);
				System.out.println("incomming(altered): " + message);
				
				if (m_connection.getMessageMap().get(specialID)) {
					
					System.out.println(specialID + " = " + m_connection.getMessageMap().get(specialID) + " - ignore");
				}
				if (!m_connection.getMessageMap().get(specialID) || !receiverMap.containsKey(specialID)) {
					
					System.out.println(specialID + " = " + m_connection.getMessageMap().get(specialID));
					if (!(message.contains("-Salive%") || message.contains("-ack%"))
							|| message.contains("-socketDC%")) {
						m_GUI.displayMessage(message);
					}
					if (message.contains("-socketDC%")) {
						m_connection.getSocket().close();
						m_connection.getSocket().disconnect();
					}
					if (message.contains("-ack%")) {
						System.out.println("ACK RECEIVED BY CLIENT");
						m_connection.setAck(true);
					}
					if (!message.contains("-ack%")) {
						m_connection.getMessageMap().put(specialID, true);
					}
				}
				receiverMap.put(specialID, true);
			}
		} while (true);
	}

	// Sole ActionListener method; acts as a callback from GUI when user hits enter
	// in input field
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			// creates a unique id that is being sent to the server
			String uniqueID = UUID.randomUUID().toString();
			m_connection.sendChatMessage("-ack%" + m_name + "-name%" + uniqueID + "-ID%" + m_GUI.getInput());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		m_GUI.clearInput();
	}
}