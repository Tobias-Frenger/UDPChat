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
 * -leave%		- used to detect when a client wants to leave the chat room
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
		String id = UUID.randomUUID().toString();
		if (m_connection.handshake("-ack%" + m_name + "-name%" + id + "-ID%" + "-connection%")) {
			listenForServerMessages();
		} else {
			System.err.println("Unable to connect to server");
		}
	}

	/*
	 * TODO fix /join - similar to /leave
	 */
	// extracting the special id from the message
	// removing unnecessary information from message
	private void messageTrimmer() {
		System.out.println("ms0 " + getMessage());
		if (!getMessage().contains("-leave%") 
				&& !getMessage().contains("-reconnect%")
				&& !getMessage().contains("-connection%") 
				&& !getMessage().contains("-list%")) {
			System.out.println("ms1 " + getMessage());
			String[] extractID = getMessage().split("-ID%");
			String idtemp[] = extractID[0].split(" -> ");
			idtemp[1] = idtemp[1].replace("-ID%", "");
			String[] temp = extractID[0].split(" -> ");
			setSpecialID(temp[1]);
			setMessage(getMessage().replace(getSpecialID(), ""));
			setMessage(getMessage().replace("-ID%", ""));
			// leave message
		} else if (getMessage().contains("-leave%")) {
			System.out.println("ms2 " + getMessage());
			String[] extractID = getMessage().split("-ID%");
			String[] temp = extractID[0].split(" final note: ");
			setSpecialID(temp[1]);
			setMessage(getMessage().replace(temp[1] + "-ID%", ""));
			setMessage(getMessage().replace("-leave%", ""));
			// reconnect message
		} else if (getMessage().contains("-reconnect%")) {
			System.out.println("ms3 " + getMessage());
			String[] extractID = getMessage().split("-reconnect%");
			setSpecialID(extractID[1]);
			setMessage(getMessage().replace(getSpecialID(), ""));
			if (!getConnection().getHeartBeat()) {
				getConnection().setHeartBeat(true);
				new HeartBeat(this).start();
			}
			System.out.println("JOIN ATTEMPT: " + getMessage());
		} else if (getMessage().contains("-connection%")) {
			System.out.println("ms4 " + getMessage());
			String[] extractID = getMessage().split("-connection%");
			String[] temp = extractID[1].split(getName());
			String[] temp1 = temp[0].split("-ID%");
			setSpecialID(temp1[0]);
			setMessage(getMessage().replace("-connection%" + getSpecialID() + "-ID%", ""));
		} else if (getMessage().contains("-list%")) {
			System.out.println("ms5 " + getMessage());
			String[] extractID = getMessage().split("-list%");
			String[] temp = extractID[1].split("-ID%");
			setSpecialID(temp[0]);
			setMessage(getMessage().replaceAll("-list%" + getSpecialID() + "-ID%", ""));
		}
	}

	private void ackMessageTrimmer() {
		System.out.println("MESSAGE C ACK PRE: " + getMessage());
		String[] temp = getMessage().split("-ID%");
		setSpecialID(temp[0]);
		setMessage(getMessage().replace(getSpecialID() + "-ID%", ""));
		System.out.println("MESSAGE C ACK POST: " + getMessage() + "\n" + " - " + getSpecialID());

	}

	private void displayMessage() {
		m_GUI.displayMessage(getMessage());
		System.out.println("message displayed");
	}

	private void disconnectSocket() {
		m_connection.getSocket().close();
		m_connection.getSocket().disconnect();
	}

	private void listenForServerMessages() throws IOException {
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
				System.out.println("incomming: " + getMessage());
				if (!getMessage().contains("-ack%")) {
					System.out.println("To Message Trimmer: " + getMessage());
					messageTrimmer();
					if (!messageDisplayNRMap.containsKey(getMessage() + getSpecialID())) {
						messageDisplayNRMap.put(getMessage() + getSpecialID(), counter);
					}
				} else {
					// extract specialID from message
					ackMessageTrimmer();
				}

				System.out.println("incomming specialID: " + getSpecialID());
				System.out.println("incomming(altered): " + getMessage());
				System.out.println(" - 1Does receiverKEY exist: " + receiverMap.containsKey(getSpecialID()));
				System.out.println(" - 2 map VALUE: " + receiverMap.get(getSpecialID()));
				if (!receiverMap.containsKey(getSpecialID())) {
					if (getMessage().contains("-ack%")) {
						System.out.println("ACK RECEIVED BY CLIENT");
						m_connection.setAck(true);
						receiverMap.put(getSpecialID(), false);
					}
					if (!getMessage().contains("-ack%")) {
						receiverMap.put(getSpecialID(), true);
					}
					System.out.println(" - 3 Map VALUE: " + receiverMap.get(getSpecialID()));
				}
				if (receiverMap.containsKey(getSpecialID())) {
					if (receiverMap.get(getSpecialID())
							&& messageDisplayNRMap.get(getMessage() + getSpecialID()) == 0) {
						messageDisplayNRMap.put(getMessage() + getSpecialID(),
								messageDisplayNRMap.get(getMessage() + getSpecialID()) + 1);
						System.out.println(" -% " + getMessage());
						System.out.println(getSpecialID() + " = " + m_connection.getMessageMap().get(getSpecialID()));
						if (!(getMessage().contains("-Salive%") || getMessage().contains("-ack%"))
								|| getMessage().contains("-socketDC%")) {
							displayMessage();
							receiverMap.put(getSpecialID(), false);
						}
						if (getMessage().contains("-socketDC%")) {
							disconnectSocket();
						}
					}
				}
				receiverMap.put(getSpecialID(), true);
			}
		} while (true);
	}

	private String getSpecialID() {
		return messageId;
	}

	private void setSpecialID(String string) {
		messageId = string;
	}

	private void setMessage(String string) {
		mess = string;
	}

	private String getMessage() {
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