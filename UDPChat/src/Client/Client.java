package Client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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

	private void listenForServerMessages() throws IOException {
		// Use the code below once m_connection.receiveChatMessage() has been
		// implemented properly.
		do {
			String message = m_connection.receiveChatMessage();
			if (!(message.contains("-Salive%") || message.contains("-ack%"))
					|| message.contains("-socketDC%")) {
				m_GUI.displayMessage(message);
			} else if (message.contains("-socketDC%")) {
				m_connection.getSocket().close();
				m_connection.getSocket().disconnect();
			} else if (message.contains("-ack%")) {
				System.out.println("ACK RECEIVED BY CLIENT");
				String[] id = message.split("-ack%");
				System.out.println("client-side: " + id[0]);
				m_connection.getMessageMap().put(id[0], true);
//				m_connection.getMessageMap().get(id[0]).equals(true);
				System.out.println("CLIENT SET TO TRUE " + m_connection.getMessageMap().get(id[0]).booleanValue());
				m_connection.setAck(true);
			}
		} while (true);
	}

	// Sole ActionListener method; acts as a callback from GUI when user hits enter
	// in input field

	@Override
	public void actionPerformed(ActionEvent e) {
		// Since the only possible event is a carriage return in the text input field,
		// the text in the chat input field can now be sent to the server.
		try {
			String uniqueID = UUID.randomUUID().toString();
			m_connection.sendChatMessage("-ack%" + m_name + "-name%" + uniqueID + "-ID%" + m_GUI.getInput());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		m_GUI.clearInput();
	}
}