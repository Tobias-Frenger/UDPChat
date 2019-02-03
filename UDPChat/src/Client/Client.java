package Client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/*
 * This class receives messages from the server
 * list of keywords:
 * -ack%		- acknowledgement sent from the server back to the client
 * -sack%		- acknowledgement sent from the client back to the server
 * -name%		- makes it easy to extract the client name from the message
 * -ID%			- enables extraction of the unique id sent by the client/server
 * -connection%	- used to detect connection messages
 * -reconnect%	- used to detect when a client wants to reconnect
 * -disconnect% - keyword that is received when a client leaves
 * -leave%		- used to detect when a client wants to leave the chat room (only sent to server)
 * -ignore%		- makes it so the message can be ignored by the rest of the listener method
 * -isAlive%	- sent by heart-beat algorithms
 */

public class Client implements ActionListener {
	private String m_name = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;
	private String messageId = "";
	private String mess;

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

	protected ChatGUI getGui() {
		return m_GUI;
	}

	private Client(String userName) {
		m_name = userName;
		// Start up GUI (runs in its own thread)
		m_GUI = new ChatGUI(this, m_name);
	}

	protected ServerConnection getConnection() {
		return m_connection;
	}

	protected String getName() {
		return m_name;
	}

	private void connectToServer(String hostName, int port) throws IOException {
		// Create a new server connection
		m_connection = new ServerConnection(hostName, port, this);
		String id = UUID.randomUUID().toString();
		if (m_connection.handshake("-ack%" + m_name + "-name%" + id + "-ID%" + "-connection%")) {
			listenForServerMessages();
		} else {
			System.err.println("Unable to connect to server");
		}
	}

	private void displayMessage() {
		m_GUI.displayMessage(getMessage());
	}

	private void disconnectSocket() {
		m_connection.getSocket().close();
		m_connection.getSocket().disconnect();
	}

	private void listenForServerMessages() throws IOException {
		Trimmer trim = new Trimmer(this);
		// Key = UUID.toString, Value = boolean set to false
		HashMap<String, Boolean> receiverMap = new HashMap<>();
		// makes sure that messages is only displayed once
		// Key = getMessage() + getSpecialID()
		HashMap<String, Integer> messageDisplayNRMap = new HashMap<>();
		int counter = 0;
		do {
			setMessage(m_connection.receiveChatMessage());
			// cleaning up incoming message
			if (getMessage().contains("-ID%")) {
				if (!getMessage().contains("-ack%") || getMessage().contains("-sack%")) {
					trim.messageTrimmer();
					if (!messageDisplayNRMap.containsKey(getMessage() + getSpecialID())) {
						messageDisplayNRMap.put(getMessage() + getSpecialID(), counter);
					}
				} else {
					// extract specialID from message
					trim.ackMessageTrimmer();
					if (!messageDisplayNRMap.containsKey(getMessage() + getSpecialID())) {
						messageDisplayNRMap.put(getMessage() + getSpecialID(), counter);
					}
				}
				// checks if message has already been encountered
				if (!getMessage().contains("-ignore%")) {
					if (!receiverMap.containsKey(getSpecialID())) {
						// checks if it is an acknowledgement message
						if (getMessage().contains("-ack%")) {
							m_connection.setAck(true);
							receiverMap.put(getSpecialID(), false);
						}
						// if not ack message
						// set value to true in hashMap so message can be sent
						if (!getMessage().contains("-ack%")) {
							receiverMap.put(getSpecialID(), true);
						}
					}
					// decide what to do with the message
					if (receiverMap.containsKey(getSpecialID())) {
						// has the message been displayed previously?
						if (receiverMap.get(getSpecialID())
								&& messageDisplayNRMap.get(getMessage() + getSpecialID()) == 0) {
							messageDisplayNRMap.put(getMessage() + getSpecialID(),
									messageDisplayNRMap.get(getMessage() + getSpecialID()) + 1);
							// is message supposed to be displayed?
							if (!(getMessage().contains("-Salive%") || getMessage().contains("-ack%"))
									|| getMessage().contains("-socketDC%")) {
								displayMessage();
								receiverMap.put(getSpecialID(), false);
							}
							// should the socket be disconnected
							/*
							 * TODO Check if this works correctly.
							 */
							if (getMessage().contains("-socketDC%")) {
								disconnectSocket();
							}
						}
					}
					// sets the key to false so that message will not
					// be able to be displayed a second time.
					receiverMap.put(getSpecialID(), true);
				}
			}
		} while (true);
	}

	protected String getSpecialID() {
		return messageId;
	}

	protected void setSpecialID(String string) {
		messageId = string;
	}

	protected void setMessage(String string) {
		mess = string;
	}

	protected String getMessage() {
		return mess;
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