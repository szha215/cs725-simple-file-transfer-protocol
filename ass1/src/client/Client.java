package client;

import java.io.*;
import java.net.*;


public class Client {
	
	private static final String SERVER_ADDRESS = "localhost";
	private static final int WELCOME_PORT = 6789;

	private Socket clientSocket;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private BufferedReader inFromUser;
	
	Client() throws UnknownHostException, IOException{
		inFromUser = new BufferedReader(new InputStreamReader(System.in));

		clientSocket = new Socket(SERVER_ADDRESS, WELCOME_PORT);
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
	}

	public void start() throws IOException{
		String sentence;

//		outToServer.writeBytes(" \n");
		
		while(true) {
			System.out.println(readMessage(inFromServer));
			sentence = inFromUser.readLine();
			sendMessage(outToServer, sentence);
			
//			System.out.println(inFromServer.readLine());
//			sentence = inFromUser.readLine();
//			outToServer.writeBytes(sentence + '\n');
//			
//			sentence = inFromServer.readLine();
			if (sentence.equals("DONE")) {
				if (readMessage(inFromServer).charAt(0) == '+') {
					System.out.println("DONE, closing socket");
					break;
				}
			} else {
				System.out.println(sentence);
			}
		}
		
		clientSocket.close();
	}

	/* Reads one character at a time into a buffer, return the buffer when '\0' 
	 * is received.
	 * 
	 * @param buffer	The BufferedReader object associated with the socket.
	 * @return sentence	Complete message String, without the '\0' character.
	 * */
	private String readMessage(BufferedReader buffer) {
		String sentence = "";
		int character = 0;
		
		while (true){
			try {
				character = buffer.read();  // Read one character
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// '\0' detected, return sentence.
			if (character == 0) {
				break;
			}
			
			// Concatenate char into sentence.
			sentence = sentence.concat(Character.toString((char)character));
		}

		return sentence;
	}

	/* Concatenate a '\0' character at the end of the string and send the string
	 * to the output stream to the server.
	 * 
	 * @param stream	The DataOutputStream object associated with the socket.
	 * @param sentence	Complete message String, without the '\0' character.
	 * */
	private void sendMessage(DataOutputStream stream, String sentence) throws IOException{
		stream.writeBytes(sentence.concat(Character.toString('\0')));
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		System.out.println("Starting Client...");
		Client client = new Client();

		client.start();
	}
}
