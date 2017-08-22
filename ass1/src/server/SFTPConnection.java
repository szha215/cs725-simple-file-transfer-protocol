package server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class SFTPConnection extends Thread{
	
	private Socket connectionSocket;
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;
	
	private JSONArray userList;
	private ArrayList<JSONObject> potentialMatches;
	private boolean accountAuthenticated = false;
	private boolean passwordAuthenticated = false;
	
	private boolean authenticated = false;
	
	
	SFTPConnection(Socket connectionSocket, JSONArray userList) throws IOException{
		this.connectionSocket = connectionSocket;
		this.userList = userList;
		
		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		
		potentialMatches = new ArrayList<JSONObject>();
		authenticated = false;
	}
	
	@Override
	public void run() {
		String clientSentence = "";
		
		sendMessage("+MIT-XX SFTP Service");
		
		while(true) {

			// while(!clientSentence.contains("EXIT")) {
			clientSentence = readMessage();

			if (clientSentence.length() >= 4) {
				switch(clientSentence.substring(0, 4)) {
				case "USER":
					userCommand(clientSentence);
					break;
					
				case "ACCT":
					System.out.println("acct command");
					authenticated = authenticate(clientSentence);
					break;

				case "PASS":
					System.out.println("pass command");
					authenticated = authenticate(clientSentence);
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
					sendMessage("+Connection closing...");
					return;  // Connection done, stop connection

				case "RETR":
					System.out.println("retr command");
					break;

				case "STOR":
					System.out.println("stor command");
					break;

				default:
					System.out.println("Invalid command");
					sendMessage("- INVALID");
					break;
				}
			} else if (clientSentence.length() < 4) {
				System.out.println("Command too short");
				sendMessage("TOO SHORT");
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
	

	/* Reads one character at a time into a buffer, return the buffer when '\0' 
	 * is received. Blocking until '\0' has been received.
	 * 
	 * @param buffer	The BufferedReader object associated with the socket.
	 * @return sentence	Complete message String, without the '\0' character.
	 * */
	private String readMessage() {
		String sentence = "";
		int character = 0;
		
		while (true){
			try {
				character = inFromClient.read();  // Read one character
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
	 * @param sentence	Complete message String, without the '\0' character.
	 * */
	private void sendMessage(String sentence){
		if (sentence.charAt(0) == '-') {
			logOff();
		}
		
		try {
			outToClient.writeBytes(sentence.concat(Character.toString('\0')));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void logOff() {
		authenticated = false;
		potentialMatches.clear();
	}
	
	private boolean initAuthenticate() {

		return true;
	}
	
	private boolean userCommand(String clientSentence) {
		try {
			String userId = clientSentence.substring(5, clientSentence.length());
		
			System.out.println("user command, input: " + userId);
			
			// User already logged in
			if (authenticated) {
				sendMessage(String.format("!%s logged in", userId));
			}
			
			// Check if user exists by iterating through the JSON objects
			Iterator<JSONObject> iterator = userList.iterator();
			
	        while (iterator.hasNext()) {
	        	JSONObject user = iterator.next();
	        	
	        	// Match found
	        	if (user.get("userId").equals(userId.trim())) {
	        		System.out.println("User exists!");
	        		sendMessage(String.format("+%s valid, send account and password", userId));
	        		
	        		return true;
	        	}
	        }
		} catch (IndexOutOfBoundsException e) {
			// User command too short
			System.out.println("userCommand, IndexOutOfBounds");
		}
        
		// Too short or invalid
        sendMessage(String.format("-Invalid user-id, try again"));
		return false;
	}

	private boolean authenticate(String sentence){
		boolean userMatch = false;
		boolean passMatch = false;
		JSONObject jObj;
		
		System.out.println("Authenticating...");
		
        Iterator<JSONObject> iterator = userList.iterator();
        while (iterator.hasNext()) {
        	jObj = iterator.next();
        	System.out.println("in " + sentence.substring(5).trim());
        	if (jObj.get("userId").equals(sentence.substring(5).trim())) {
        		System.out.println(jObj.get("userId"));
        	}
        }
		
        System.out.println("Authentication complete");
        
//        sendMessage(, "+");
        
		return true;
	}
	
}
