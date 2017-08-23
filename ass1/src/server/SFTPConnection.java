package server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.*;


public class SFTPConnection extends Thread{
	
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;
	
	private JSONArray userList;

	private ArrayList<JSONObject> potentialMatches;

	private boolean accountAuthenticated = false;
	private boolean passwordAuthenticated = false;
	private boolean authenticated = false;
	
	
	SFTPConnection(Socket connectionSocket, JSONArray userList) throws IOException{
		this.userList = userList;
		
		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		
		potentialMatches = new ArrayList<JSONObject>();
		authenticated = false;
	}
	
	@Override
	public void run() {
		String clientSentence = "";
		
		// Send greeting 
		sendMessage("+CS725 SFTP Service");
		
		while(true) {

			clientSentence = readMessage();

			if (clientSentence.length() >= 4) {
				switch(clientSentence.substring(0, 4)) {
				case "USER":
					userCommand(clientSentence);
					break;
					
				case "ACCT":
					acctCommand(clientSentence);
					break;

				case "PASS":
					passCommand(clientSentence);
					break;

				case "TYPE":
					typeCommand(clientSentence);
					break;

				case "LIST":
					listCommand(clientSentence);
					break;

				case "CDIR":
					cdirCommand(clientSentence);
					break;

				case "KILL":
					killCommand(clientSentence);
					break;

				case "NAME":
					nameCommand(clientSentence);
					break;

				case "DONE":
					System.out.println("done command");
					sendMessage("+CS725 closing connection...");
					return;  // Connection done, stop connection

				case "RETR":
					retrCommand(clientSentence);
					break;

				case "STOR":
					storCommand(clientSentence);					
					break;

				default:
					System.out.println("Invalid command");
					sendMessage("- INVALID");
					break;
				}
			} else if (clientSentence.length() < 4) {
				System.out.println("Command too short");
				sendMessage("- INVALID");
			}

		}
	}
	

	/* Reads one character at a time into a buffer, return the buffer when '\0' 
	 * is received. Blocking until '\0' has been received.
	 * 
	 * @param	buffer		The BufferedReader object associated with the socket.
	 * @return	sentence	Complete message String, without the '\0' character.
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
	 * to the output stream to the server. If the first character is '-', server
	 * will log the client off.
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
	
	private void logOn() {
		System.out.println("logging on");
		authenticated = true;
		accountAuthenticated = false;
		passwordAuthenticated = false;
		potentialMatches.clear();
	}
	
	private void logOff() {
		System.out.println("logging off");
		authenticated = false;
		accountAuthenticated = false;
		passwordAuthenticated = false;
		potentialMatches.clear();
	}
	
	public boolean loggedOn() {
		return authenticated;
	}
	
	
	private boolean userCommand(String clientSentence) {
		try {
			String userId = clientSentence.substring(5, clientSentence.length());
		
			System.out.println("user command, input: " + userId);
			
			// Check if user exists by iterating through user list
			Iterator<JSONObject> iterator = userList.iterator();
			
	        while (iterator.hasNext()) {
	        	JSONObject user = iterator.next();
	        	
	        	// Match found
	        	if (user.get("userId").equals(userId.trim())) {
	        		System.out.println("User exists!");
	        		if (user.get("account").equals("") && user.get("password").equals("")) {
	        			logOn();
	        			sendMessage(String.format("!%s logged in", userId));
	        		} else {
	        			sendMessage("+User-id valid, send account and password");
	        		}
	        		
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

	private boolean acctCommand(String clientSentence) {
		System.out.println("acct command");
		
		try {
			String acct = clientSentence.substring(5, clientSentence.length());
			
			if (passwordAuthenticated) {
				if (acctForPass(acct)) {
					logOn();
					sendMessage("! Account valid, logged-in");
					
					return true;
					
				} else {
					sendMessage("-Invalid account, try again");					
				}
	
			} else if (acctExists(acct) > 0){
				sendMessage("+Account valid, send password");
				accountAuthenticated = true;
				
				return true;
				
			} else {
				sendMessage("-Invalid account, try again");
			}
		} catch (IndexOutOfBoundsException e) {
			// Acct command too short
			System.out.println("acctCommand, IndexOutOfBounds");
			
			sendMessage("-Invalid account, try again");
			
			return false;
		}
		
		return false;
	}
	
	private boolean passCommand(String clientSentence) {
		System.out.println("pass command");
		
		try {
			String pass = clientSentence.substring(5, clientSentence.length());
			
			if (accountAuthenticated) {
				if (passForAcct(pass)) {
					logOn();
					sendMessage("! Logged in");
					
					return true;
				} else {
					sendMessage("-Wrong password, try again");		
				}
				
			} else if (passExists(pass) > 0) {
				sendMessage("+Send account");
				passwordAuthenticated = true;

			} else {
				sendMessage("-Wrong password, try again");
				
			}
		} catch (IndexOutOfBoundsException e) {
			// Acct command too short
			System.out.println("passCommand, IndexOutOfBounds");
			
			sendMessage("-Invalid account, try again");
			
			return false;
		}
			
		return false;
	}
	
	private boolean typeCommand(String clientSentence) {
		System.out.println("type command");
		
		return false;
	}

	private boolean listCommand(String clientSentence) {
		System.out.println("list command");
		
		return false;
	}
	
	private boolean cdirCommand(String clientSentence) {
		System.out.println("cdir command");
		
		return false;
	}
	
	private boolean killCommand(String clientSentence) {
		System.out.println("kill command");
		
		return false;
	}
	
	private boolean nameCommand(String clientSentence) {
		System.out.println("name command");
		
		return false;
	}
	
	private boolean retrCommand(String clientSentence) {
		System.out.println("retr command");
		
		return false;
	}
	
	private boolean storCommand(String clientSentence) {
		System.out.println("stor command");
		
		return false;
	}
	
	/****************************************************************************/
	
	
	/* Checks the user list, add account JSON object that matches
	 * to potential match list and return the number of matches.
	 * 
	 * @param	acct	Account name in String.
	 * @return	count	Number of accounts that matches the name.
	 * */
	private int acctExists(String acct) {
		int count = 0;
		
		// Check if acct exists by iterating through user list
		Iterator<JSONObject> iterator = userList.iterator();
		
        while (iterator.hasNext()) {
        	JSONObject user = iterator.next();
        	
        	// Match found, append to potential matches list
        	if (user.get("account").equals(acct.trim())) {
        		potentialMatches.add(user);
        		count++;
        	}
        }
        
        return count;
	}

	/* Checks the user list, add account JSON object that matches
	 * to potential match list and return the number of matches.
	 * 
	 * @param	pass	Password in String.
	 * @return	count	Number of accounts that matches the password.
	 * */
	private int passExists(String pass) {
		int count = 0;
		
		// Check if pass exists by iterating through user list
		Iterator<JSONObject> iterator = userList.iterator();
		
        while (iterator.hasNext()) {
        	JSONObject user = iterator.next();
        	
        	// Match found, append to potential matches list
        	if (user.get("password").equals(pass.trim())) {
        		potentialMatches.add(user);
        		count++;
        	}
        }
		
		return count;
	}
	
	/* Checks if the specified account is for an account in the potential
	 * matches list.
	 * 
	 * @param	acct	Account in String
	 * @return	match	Account matches boolean
	 * */
	private boolean acctForPass(String acct) {
		if (potentialMatches.isEmpty()) {
			return false;
		}
		
		// Check if acct matches an account in the potential matches
		Iterator<JSONObject> iterator = potentialMatches.iterator();
		
        while (iterator.hasNext()) {
        	JSONObject user = iterator.next();
        	
        	// Match found
        	if (user.get("account").equals(acct.trim())) {
        		System.out.println("acct matches pot. matches");
        		
        		return true;
        	}
        }
		
		return false;
	}
	
	/* Checks if the specified password is for an account in the potential
	 * matches list.
	 * 
	 * @param	pass	Password in String
	 * @return	match	Password matches boolean
	 * */
	private boolean passForAcct(String pass) {
		if (potentialMatches.isEmpty()) {
			return false;
		}
		
		// Check if acct matches an account in the potential matches
		Iterator<JSONObject> iterator = potentialMatches.iterator();
		
        while (iterator.hasNext()) {
        	JSONObject user = iterator.next();
        	
        	// Match found
        	if (user.get("password").equals(pass.trim())) {
        		System.out.println("pass matches pot. matches");
        		
        		return true;
        	}
        }
		
		return false;
	}
	
	
}
