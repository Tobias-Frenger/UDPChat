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

	protected ChatGUI getGui() {
		return m_GUI;
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
//	private void ackMessageTrimmer() {
//		System.out.println("MESSAGE C ACK PRE: " + getMessage());
//		if (getMessage().contains("-sack%")) {
//			setMessage(getMessage().replace("-sack%", ""));
//		}
//		if (getMessage().contains("/list")) {
//			setMessage(getMessage().replace("/list", "-ignore%"));
//		}
//		String[] temp = getMessage().split("-ID%");
//		setSpecialID(temp[0]);
//		setMessage(getMessage().replace(getMessage(), "-ack%"));
//		System.out.println("MESSAGE C ACK POST: " + getMessage() + "\n" + " - " + getSpecialID());
//	}

	private void displayMessage() {
		m_GUI.displayMessage(getMessage());
		System.out.println("message displayed - " + getMessage());
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
				System.out.println("incomming: " + getMessage());
				if (!getMessage().contains("-ack%") || getMessage().contains("-sack%")) {
					System.out.println("To Message Trimmer: " + getMessage());
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

				System.out.println("incomming specialID: " + getSpecialID());
				System.out.println("incomming(altered): " + getMessage());
				System.out.println(" - 1Does receiverKEY exist: " + receiverMap.containsKey(getSpecialID()));
				System.out.println(" - 2 map VALUE: " + receiverMap.get(getSpecialID()));
				// checks if message has already been encountered
				if (!getMessage().contains("-ignore%")) {
					if (!receiverMap.containsKey(getSpecialID())) {
						// checks if it is an acknowledgement message
						if (getMessage().contains("-ack%")) {
							System.out.println("ACK RECEIVED BY CLIENT");
							m_connection.setAck(true);
							receiverMap.put(getSpecialID(), false);
						}
						// if not ack message
						// set value to true in hashMap so message can be sent
						if (!getMessage().contains("-ack%")) {
							receiverMap.put(getSpecialID(), true);
						}
						System.out.println(" - 3 Map VALUE: " + receiverMap.get(getSpecialID()));
					}
					// decide what to do with the message
					if (receiverMap.containsKey(getSpecialID())) {
						// has the message been displayed previously?
						if (receiverMap.get(getSpecialID())
								&& messageDisplayNRMap.get(getMessage() + getSpecialID()) == 0) {
							messageDisplayNRMap.put(getMessage() + getSpecialID(),
									messageDisplayNRMap.get(getMessage() + getSpecialID()) + 1);
							System.out.println(" -% " + getMessage());
							System.out
									.println(getSpecialID() + " = " + m_connection.getMessageMap().get(getSpecialID()));
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