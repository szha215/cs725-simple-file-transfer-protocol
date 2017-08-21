package server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Server {
	
	private ServerSocket welcomeSocket;
	private static final int WELCOME_PORT = 6789;

	private HashMap<String, String> users;
	private JSONArray userList;

	Server() throws IOException{
		readUserList();
		welcomeSocket = new ServerSocket(WELCOME_PORT);
		users = new HashMap<String, String>();
	}

	public void start() throws IOException{
		String clientSentence = "";

		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			boolean authenticated = false;

			sendMessage(outToClient, "+MIT-XX SFTP Service");

			while(true){
				
				// while(!clientSentence.contains("EXIT")) {
				clientSentence = readMessage(inFromClient);

				if (clientSentence.length() >= 4) {
					switch(clientSentence.substring(0, 4)) {
					case "USER":
						System.out.println("user command");
						break;
						
					case "ACCT":
						System.out.println("acct command");
						break;

					case "PASS":
						System.out.println("pass command");
						break;

					case "TYPE":
						System.out.println("type command");
						break;

					case "LIST":
						System.out.println("list command");
						break;

					case "CDIR":
						System.out.println("cdir command");
						break;

					case "KILL":
						System.out.println("kill command");
						break;

					case "NAME":
						System.out.println("name command");
						break;

					case "DONE":
						System.out.println("done command");
						break;

					case "RETR":
						System.out.println("retr command");
						break;

					case "STOR":
						System.out.println("stor command");
						break;

					default:
						System.out.println("Invalid command");
						break;
					}
				} else if (clientSentence.length() < 4) {
					System.out.println("Command too short");
					
				}


				// outToClient.writeBytes("Username: " + '\n');
				// String username = inFromClient.readLine();
				
				// outToClient.writeBytes("Password: " + '\n');
				// String password = inFromClient.readLine();

				
				// System.out.println("Received: " + username + " " + password);
				
				// if (username.equals("szha") && password.equals("215")) {
				// 	outToClient.writeBytes("Authenticated." + '\n');
				// 	System.out.println("szha logged in");
				// } else if (username.equals("EXIT")) {
				// 	outToClient.writeBytes("EXIT" + '\n');
				// } else {
				// 	outToClient.writeBytes("Authentication failed." + '\n');
				// }
			}
		}	
	}


	private static String createSentence(String input) {
		return input.concat("\0");
	}
	
	/* Reads one character at a time into a buffer, return the buffer when '\0' 
	 * is received. Blocking until '\0' has been received.
	 * 
	 * @param buffer	The BufferedReader object associated with the socket.
	 * @return sentence	Complete message String, without the '\0' character.
	 * */
	private static String readMessage(BufferedReader buffer) {
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
				return sentence;
			}
			
			// Concatenate char into sentence.
			sentence = sentence.concat(Character.toString((char)character));
		}
	}
	
	/* Concatenate a '\0' character at the end of the string and send the string
	 * to the output stream to the server.
	 * 
	 * @param stream	The DataOutputStream object associated with the socket.
	 * @param sentence	Complete message String, without the '\0' character.
	 * */
	private static void sendMessage(DataOutputStream stream, String sentence) throws IOException{
		stream.writeBytes(sentence.concat(Character.toString('\0')));
	}

	private void readUserList(){
		JSONParser parser = new JSONParser();

		try {
			userList = (JSONArray) parser.parse(new FileReader("res/userlist.json"));
			System.out.println(userList);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}

	private boolean initAuthenticate() {
		
		
		return true;
	}

	private boolean authenticate(String sentence){

		
		return true;
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println("Starting Server...");
		Server server = new Server();

		server.start();
	}
	

}
