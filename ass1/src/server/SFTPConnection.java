package server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.json.simple.*;


public class SFTPConnection extends Thread{

	public static boolean DEBUG = false;
	
	private static final File DEFAULT_DIRECTORY = FileSystems.getDefault().getPath("ServerFolder").toFile().getAbsoluteFile();
	
	private Socket connectionSocket;
	
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;
	
	private BufferedInputStream dataInFromClient;
	private BufferedOutputStream bufferedOutToClient;
	private DataOutputStream dataOutToClient;
	
	private JSONArray userList;

	private ArrayList<JSONObject> potentialMatches;

	private boolean accountAuthenticated = false;
	private boolean passwordAuthenticated = false;
	private boolean loggedIn = false;

	private File currentDirectory = DEFAULT_DIRECTORY;
	
	private String transmissionType = "B";
	
	
	SFTPConnection(Socket connectionSocket, JSONArray userList) throws IOException{
		this.connectionSocket = connectionSocket;
		this.userList = userList;
		
		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		
		dataInFromClient = new BufferedInputStream(connectionSocket.getInputStream());
		bufferedOutToClient = new BufferedOutputStream(connectionSocket.getOutputStream());
		dataOutToClient = new DataOutputStream(bufferedOutToClient);
		
		potentialMatches = new ArrayList<JSONObject>();
	}
	
	@Override
	public void run() {
		String clientSentence = "";
		
		// Send greeting 
		sendMessage("+CS725 SFTP Service");
		
		while(true) {

			// Close thread if connection is closed
			if (connectionSocket.isClosed()) {
				System.out.println(String.format("connection to %s CLOSED", connectionSocket.getRemoteSocketAddress().toString()));
				
				return;
			}

			clientSentence = readMessage();

			if (clientSentence.length() >= 4) {
				switch(clientSentence.substring(0, 4).toUpperCase()) {
				case "USER":
					userCommand(clientSentence);
					break;
					
				case "ACCT":
					acctCommand(clientSentence, true);
					break;

				case "PASS":
					passCommand(clientSentence, true);
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
					if (DEBUG) System.out.println("done command");
					sendMessage("+CS725 closing connection...");

					return;  // Connection done, stop connection

				case "RETR":
					retrCommand(clientSentence);
					break;

				case "STOR":
					storCommand(clientSentence);					
					break;

				default:
					if (DEBUG) System.out.println("Invalid command");
					sendMessage("- INVALID");
					break;
				}
			} else if (clientSentence.length() < 4) {
				if (DEBUG) System.out.println("Command too short: " + clientSentence);
				sendMessage("- Invalid command");
			}

		}
	}
	
	public boolean loggedOn() {
		return loggedIn;
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
			} catch (Exception e) {
				
				// Socket closed by client
				try {
					connectionSocket.close();
				} catch (IOException e1) {}
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
			
			// Socket closed by client
			try {
				connectionSocket.close();
			} catch (IOException e1) {}
		}
	}
	
	private void logOn() {
		if (DEBUG) System.out.println("logging on");

		potentialMatches.clear();
		
		accountAuthenticated = false;
		passwordAuthenticated = false;
		loggedIn = true;
	}
	
	private void logOff() {
		if (DEBUG) System.out.println("logging off");

		potentialMatches.clear();
		
		accountAuthenticated = false;
		passwordAuthenticated = false;
		loggedIn = false;
	}

	private boolean userCommand(String clientSentence) {
		StringTokenizer tokentizedSentence = new StringTokenizer(clientSentence);
		tokentizedSentence.nextToken();  // Command
		
		// No username in arguments
		if (!tokentizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
		}
		
		String userId = tokentizedSentence.nextToken();
		if (DEBUG) System.out.println("user command, input: " + userId);
		
		// Check if user exists by iterating through user list
		Iterator<JSONObject> iterator = userList.iterator();
		
        while (iterator.hasNext()) {
        	JSONObject user = iterator.next();
        	
        	// Match found
        	if (user.get("userId").equals(userId.trim())) {
        		if (user.get("account").equals("") && user.get("password").equals("")) {
        			logOn();
        			sendMessage(String.format("!%s logged in", userId));
        		} else {
        			sendMessage("+User-id valid, send account and password");
        		}
        		
        		return true;
        	}
        }

		// Too short or invalid
        sendMessage(String.format("-Invalid user-id, try again"));
		return false;
	}

	private boolean acctCommand(String clientSentence, boolean reply) {
		if (DEBUG) System.out.println("acct command");
		
		StringTokenizer tokentizedSentence = new StringTokenizer(clientSentence);
		tokentizedSentence.nextToken();  // Command
		
		// No account in arguments
		if (!tokentizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
		}

		String acct = tokentizedSentence.nextToken();
		
		// Password has been previously sent and checked
		if (passwordAuthenticated) {
			if (acctForPass(acct)) {
				logOn();
				if (reply) sendMessage("! Account valid, logged-in");
				
				return true;
				
			} else {
				if (reply) sendMessage("-Invalid account, try again");					
			}

		// The number of accounts that match
		} else if (acctExists(acct) > 0){
			if (reply) sendMessage("+Account valid, send password");
			accountAuthenticated = true;
			
			return true;
			
		} else {
			if (reply) sendMessage("-Invalid account, try again");
		}
		
		return false;
	}
	
	private boolean passCommand(String clientSentence, boolean reply) {
		if (DEBUG) System.out.println("pass command");
		
		StringTokenizer tokentizedSentence = new StringTokenizer(clientSentence);
		tokentizedSentence.nextToken();  // Command
		
		// No password in arguments
		if (!tokentizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
		}
		
		String pass = tokentizedSentence.nextToken();
		
		// Account has been previously sent and checked
		if (accountAuthenticated) {
			if (passForAcct(pass)) {
				logOn();
				if (reply) sendMessage("! Logged in");
				
				return true;
			} else {
				if (reply) sendMessage("-Wrong password, try again");		
			}
			
		// Password doesn't belong to any account
		} else if (passExists(pass) > 0) {
			if (reply) sendMessage("+Send account");
			passwordAuthenticated = true;

		} else {
			if (reply) sendMessage("-Wrong password, try again");
		}

			
		return false;
	}
	
	private boolean typeCommand(String clientSentence) {
		if (DEBUG) System.out.println("type command");
		
		if (!loggedOn()) {
			sendMessage("-Not logged in");
			
			return false;
		}
		
		StringTokenizer tokentizedSentence = new StringTokenizer(clientSentence);
		tokentizedSentence.nextToken();  // Command
		
		// No type in arguments
		if (!tokentizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
		}
		
		String type = tokentizedSentence.nextToken().toUpperCase();
		
		if (!type.equals("A") && !type.equals("B") && !type.equals("C")) {
			sendMessage("-Invalid arguments");
			
			return false;
		}
		
		String reply = "";
		
		switch(type.toUpperCase()) {
		case "A":
			reply = "Ascii";
			break;
			
		case "B":
			reply = "Binary";
			break;
			
		case "C":
			reply = "Continuous";
			break;
		}
		
		// Update the transmission type
		transmissionType = type;
		if (DEBUG) System.out.println("Transmission type is now " + transmissionType);
		
		sendMessage(String.format("+Using %s mode", reply));
		
		
		return true;
	}

	private boolean listCommand(String clientSentence) {
		if (DEBUG) System.out.println("list command");
		
		if (!loggedOn()) {
			sendMessage("-Not logged in");
			
			return false;
		}
		
		String outputList = "+\n./\n../\n";  // Current and parent directories
		
		String mode = "";
		File path = currentDirectory;
		
		StringTokenizer tokentizedSentence = new StringTokenizer(clientSentence);
		tokentizedSentence.nextToken();  // Command
		
		// No type in arguments
		if (!tokentizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
		}
		
		mode = tokentizedSentence.nextToken().toUpperCase();
		
		if (!mode.equals("F") && !mode.equals("V")) {
			sendMessage("-Invalid arguments");
			
			if (DEBUG) System.out.println("`" + mode + "`");
			
			return false;
		}
			
		try {
			path = new File(currentDirectory.toString() + "/" + tokentizedSentence.nextToken());
			
			if (!path.isDirectory()) {
				sendMessage(String.format("-Not a directory"));
				
				return false;
			}
		} catch (NoSuchElementException e) {
			// missing second argument, i.e. current directory
		}
		
		// Dateformat for verbose print
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy kk:mm");
		
		File files[] = path.listFiles();
		
		// Go through each file in the directory
		for (File f : files) {
			String filename = f.getName();
			
			if (f.isDirectory()) {
				filename = filename.concat("/");
			}
			
			// verbose, get information on the file
			if (mode.equals("V")) {
				long modifiedTime = f.lastModified();
				String modifiedDate = dateFormat.format(new Date(modifiedTime));
				String size = String.valueOf(f.length());
				String owner = "";

				try {
					 FileOwnerAttributeView attr = Files.getFileAttributeView(f.toPath(), FileOwnerAttributeView.class);
					 owner = attr.getOwner().getName();
				} catch (IOException e) {	
					e.printStackTrace();
				}

				// print structure:   filename   modified time    size    owner
				outputList = outputList.concat(String.format("%-30s %-20s %10s %20s \r\n", filename, modifiedDate, size, owner));
			
			// non verbose, filename only
			} else {
				outputList = outputList.concat(String.format("%s \r\n", filename));
			}
		}
		
		sendMessage(outputList);
		
		return true;
	}
	
	private boolean cdirCommand(String clientSentence) {
		if (DEBUG) System.out.println("cdir command");
		if (DEBUG) System.out.println("Current dir: " + currentDirectory.toString());
		
		String newDirName = "";
		
		StringTokenizer tokenizedSentence = new StringTokenizer(clientSentence);
		tokenizedSentence.nextToken();  // Command
		
		// check for missing argument
		if (!tokenizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
			
			return false;
		}
		
		newDirName = tokenizedSentence.nextToken();
		
		// Add / for directory
		if (newDirName.charAt(0) != '/') {
			newDirName = String.format("/%s", newDirName);
		}
		
		if (newDirName.charAt(newDirName.length()-1) != '/') {
			newDirName = newDirName.concat("/");
		}
		
		File newDir = new File(currentDirectory.toString().concat(newDirName)).toPath().normalize().toFile();
		
		// Client trying access folder above allocated "root" folder.
		if (newDir.compareTo(DEFAULT_DIRECTORY.getAbsoluteFile()) < 0){
			sendMessage("-Can't connect to directory because permission denied");
			
			return false;
		}
		
		// Specified directory is not a directory
		if (!newDir.isDirectory()) {
			sendMessage("-Can't connect to directory because no such directory exists");
			
			return false;
		}
		
		// Replace portion of the path to ~
		String newDirReply = String.format("~%s", newDir.toString().substring(DEFAULT_DIRECTORY.toString().length()));
		
		// Already logged in
		if (loggedOn()) {
			currentDirectory = newDir;
			sendMessage(String.format("!Changed working dir to %s", newDirReply));
			
			if (DEBUG) System.out.println("Current dir: " + currentDirectory);
			
			return true;
			
		// Need to log in
		} else {
			sendMessage(String.format("+directory ok, send account/password", newDir));
			
			// Run CDIR authentication procedure
			if (cdirAuthenticate()) {
				currentDirectory = newDir;
				sendMessage(String.format("!Changed working dir to %s", newDirReply));
				
				return true;
			}
		}
		
		return false;
	}
	
	private boolean killCommand(String clientSentence) {
		if (DEBUG) System.out.println("kill command");
		
		if (!loggedOn()) {
			sendMessage("-Not logged in");
			
			return false;
		}
		
		StringTokenizer tokenizedSentence = new StringTokenizer(clientSentence);
		tokenizedSentence.nextToken();  // Command
		
		// check for missing argument
		if (!tokenizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
			
			return false;
		}
		
		String filename = tokenizedSentence.nextToken();
		
//			if (filename.contains("^[<>|:&]+$")) {
//				sendMessage("-Not deleted because filename contains reserved symbols");
//			}
		
		if (DEBUG) System.out.println("Current dir = " + currentDirectory.toString());
		
		Path path = new File(currentDirectory.toString().concat("/").concat(filename)).toPath();
		
		try {
			Files.delete(path);
			sendMessage(String.format("+%s deleted", filename));
			
			return true;
			
		} catch (NoSuchFileException x) {
		    sendMessage("-Not deleted because no such file exists in the directory");
		    
		} catch (IOException x) {
		    sendMessage("-Not deleted because it's protected");
		}
		
		return false;
	}
	
	private boolean nameCommand(String clientSentence) {
		if (DEBUG) System.out.println("name command");
		
		if (!loggedOn()) {
			sendMessage("-Not logged in");
			
			return false;
		}
		
		StringTokenizer tokenizedSentence = new StringTokenizer(clientSentence);
		tokenizedSentence.nextToken();  // Command
		
		// check for missing argument
		if (!tokenizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
			
			return false;
		}
		
		String oldFilename = tokenizedSentence.nextToken();
		File oldFile = new File(currentDirectory.toString() + "/" + oldFilename);
		
		// Check if file exists
		if (!oldFile.isFile()) {
			sendMessage(String.format("-Can't find %s", oldFilename));
			
			return false;
		}
		
		sendMessage(String.format("+File exists"));
		
		// Wait for TOBE command
		String newClientSentence = readMessage();
		
		if (!newClientSentence.substring(0, 4).toUpperCase().equals("TOBE")) {
			sendMessage(String.format("-File wasn't renamed because command was not \"TOBE\""));
		
			return false;
		}
		
		// Get new filename from argument
		String newFilename = newClientSentence.substring(5, newClientSentence.length());
		File newFile = new File(currentDirectory.toString() + "/" + newFilename);
		
		// Check if the new filename is already taken
		if (newFile.exists()) {
			sendMessage(String.format("-File wasn't renamed because new file name already exists"));
		
			return false;
		}
		
		// Rename
		if (oldFile.renameTo(newFile)) {
			sendMessage(String.format("+%s renamed to %s", oldFilename, newFilename));
		} else {
			sendMessage(String.format("-File wasn't renamed because it's protected"));
		}
		
		return true;
	}
	
	private boolean retrCommand(String clientSentence) {
		if (DEBUG) System.out.println("retr command");
		
		StringTokenizer tokenizedSentence = new StringTokenizer(clientSentence);
		tokenizedSentence.nextToken();  // Command
		
		// Check for missing argument
		if (!tokenizedSentence.hasMoreTokens()) {
			sendMessage("-Missing argument");
			
			return false;
		}
		
		String filename = tokenizedSentence.nextToken();
		
		// Specified file
		File file = new File(currentDirectory.toString() + "/" + filename);
		if (DEBUG) System.out.println("File of interest = " + file.toPath().toAbsolutePath().toString());
		
		// Specified file is not a file
		if (!file.isFile()) {
			sendMessage("-File doesn't exist");
			
			return false;
		}
		
		// Get file size
		long fileSize = file.length();
		sendMessage(String.format(" %s", String.valueOf(fileSize)));

		String clientDecision = readMessage().toUpperCase();

		// Client no longer wants the file
		if (clientDecision.equals("STOP")) {
			sendMessage("+ok, RETR aborted");
			
			return false;

		// Client sent other unwanted replies
		} else if (!clientDecision.equals("SEND")) {
			sendMessage("-Invalid response");
			
			return false;
		}
		
		byte[] bytes = new byte[(int) fileSize];

		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bufferedInStream = new BufferedInputStream(new FileInputStream(file));
			
			if (DEBUG) System.out.println("Total file size to read (in bytes) : " + fis.available());

			int content = 0;
			
			// Read and send file until the whole file has been sent
			while ((content = bufferedInStream.read(bytes)) >= 0) {
				dataOutToClient.write(bytes, 0, content);

				if (DEBUG) System.out.println(content);
			}
			
			bufferedInStream.close();
			dataOutToClient.flush();
	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (DEBUG) System.out.println("FILE SENT");
		
		return true;
	}
	
	private boolean storCommand(String clientSentence) {
		if (DEBUG) System.out.println("stor command");
		
		String filename, clientDecision;
		
		try {
			filename = clientSentence.substring(5, clientSentence.length());
		} catch (IndexOutOfBoundsException e) {
			sendMessage("-Invalid filename");
			
			return false;
		}
		
		
		
		
		return false;
	}
	
	/*********************************** Helper Methods *****************************************/
	
	
	
	/* Authenticate client for CDIR command.
	 * 
	 * @return	authenticated	Whether authentication was successful
	 * */
	private boolean cdirAuthenticate() {
		
		while(true) {
			String clientSentence = readMessage();
			
			if (clientSentence.length() >= 4) {
				switch(clientSentence.substring(0, 4).toUpperCase()) {
				case "ACCT":
					if (!acctCommand(clientSentence, false)) {
						sendMessage("-invalid account");
						return false;
					}
					
					if (loggedOn()) {
						return true;
					} else {
						sendMessage("+account ok, send password");
					}
					
					break;
	
				case "PASS":
					if (!passCommand(clientSentence, false)) {
						sendMessage("-invalid password");
						return false;
					}
					
					if (loggedOn()) {
						return true;
					} else {
						sendMessage("+password ok, send account");
					}
					
					break;
	
				default:
					sendMessage("-invalid command");
					return false;
				}
				
			} else {
				sendMessage("-invalid command");
				return false;
			}
		}
	}
	
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
		
		// There were no matches from previously entered password
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
		
		// There were no matches from previously entered account
		if (potentialMatches.isEmpty()) {
			return false;
		}
		
		// Check if acct matches an account in the potential matches
		Iterator<JSONObject> iterator = potentialMatches.iterator();
		
        while (iterator.hasNext()) {
        	JSONObject user = iterator.next();
        	
        	// Match found
        	if (user.get("password").equals(pass.trim())) {
        		if (DEBUG) System.out.println("pass matches pot. matches");
        		
        		return true;
        	}
        }
		
		return false;
	}
	
	
}
