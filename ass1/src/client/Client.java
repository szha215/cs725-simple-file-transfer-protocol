package client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;



public class Client {
	
	private static final String SERVER_ADDRESS = "localhost";
	private static final int WELCOME_PORT = 6789;
	
	private static final File DEFAULT_DIRECTORY = FileSystems.getDefault().getPath("ClientFolder").toFile().getAbsoluteFile();

	private Socket clientSocket;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private BufferedReader inFromUser;
	
	private BufferedInputStream dataInFromServer;
	private BufferedOutputStream dataOutToServer;
	
	private boolean loggedIn = false;
	
	Client() throws UnknownHostException, IOException{
		inFromUser = new BufferedReader(new InputStreamReader(System.in));

		clientSocket = new Socket(SERVER_ADDRESS, WELCOME_PORT);
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		
		dataInFromServer = new BufferedInputStream(clientSocket.getInputStream());
		dataOutToServer = new BufferedOutputStream(clientSocket.getOutputStream());
	}

	public void start() throws IOException{
		String serverSentence;
		String clientSentence;
		
		StringTokenizer tokenizedClientSentence;
		
		serverSentence = readMessage();
		System.out.println(serverSentence);
		
		while(true) {
			
			clientSentence = inFromUser.readLine();
			tokenizedClientSentence = new StringTokenizer(clientSentence);
			
			switch(tokenizedClientSentence.nextToken().toUpperCase()) {
			case "RETR":
				retrClientCommand(clientSentence);
				continue;
				
			case "DONE":
				if (readMessage().charAt(0) == '+') {
					System.out.println("DONE, closing socket");
					clientSocket.close();
					
					return;
				}
				
			default:
				sendMessage(clientSentence);
				break;
			}	
			
			serverSentence = readMessage();
			System.out.println(serverSentence);

		}
		
	}
	
	private boolean retrClientCommand(String sentence) throws IOException {
		StringTokenizer tokenizedClientSentence = new StringTokenizer(sentence);
		tokenizedClientSentence.nextToken();  // RETR command
		
		String filename = "";
		
		if (tokenizedClientSentence.hasMoreTokens()) {
			filename = tokenizedClientSentence.nextToken();
		} else {
			System.err.println("Missing filename");
			
			return false;
		}
		
		sendMessage(sentence);
		String serverSentence = readMessage();
		StringTokenizer tokenizedServerSentence = new StringTokenizer(serverSentence);
		char first = '\0';
		
		first = serverSentence.charAt(0);
		
		if (first == ' ') {
			
			long fileSize = Long.valueOf(tokenizedServerSentence.nextToken());
			System.out.println(String.format("File size = %d bytes. Type \"SEND\" or \"STOP\"", fileSize));
			
			String clientSentence = inFromUser.readLine();
			
			if (clientSentence.toUpperCase().equals("SEND")) {
				sendMessage("SEND");
			} else {
				sendMessage(clientSentence);
				System.out.println(readMessage());  // Server replies "aborted"
				
				return false;
			}

			
			if (receiveFile(filename, fileSize)) {
				System.out.println(String.format("File %s received", filename));
			}
		} else {
			System.out.println(serverSentence);
			return false;
		}
		
		
		return true;
	}
	

	/* Reads one character at a time into a buffer, return the buffer when '\0' 
	 * is received.
	 * 
	 * @param buffer	The BufferedReader object associated with the socket.
	 * @return sentence	Complete message String, without the '\0' character.
	 * */
	private String readMessage() {
		String sentence = "";
		int character = 0;
		
		while (true){
			try {
				character = inFromServer.read();  // Read one character
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
		
		// '!' detected, log in
		if (sentence.charAt(0) == '!') {
			loggedIn = true;
			System.out.println("client logged in");
		} else if (sentence.charAt(0) == '-') {
			loggedIn = false;
			System.out.println("client logged off");
		}

		return sentence;
	}

	/* Concatenate a '\0' character at the end of the string and send the string
	 * to the output stream to the server.
	 * 
	 * @param stream	The DataOutputStream object associated with the socket.
	 * @param sentence	Complete message String, without the '\0' character.
	 * */
	private void sendMessage(String sentence){
		try {
			outToServer.writeBytes(sentence.concat(Character.toString('\0')));
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}
	
	private boolean receiveFile(String filename, long fileSize) throws IOException {
		byte[] bytes = new byte[(int) fileSize];
		
//		Path file = Paths.get(DEFAULT_DIRECTORY.getPath().toString() + "/" + filename);
		File file = new File(DEFAULT_DIRECTORY.getPath().toString() + "/" + filename);
		FileOutputStream fileOutStream = new FileOutputStream(file);
		BufferedOutputStream bufferedOutStream = new BufferedOutputStream(fileOutStream);
		
		int count = 0;
		
		for (int i = 0; i < fileSize; i++) {
			bufferedOutStream.write(dataInFromServer.read());
			System.out.print(i+",");
		}
		
//		while((count = dataInFromServer.read(bytes)) < fileSize) {
//			System.out.println(count);
//			bufferedOutStream.write(bytes);
//		}
		
//		Files.write(file, bytes, StandardOpenOption.WRITE);
		
		
		bufferedOutStream.close();
		fileOutStream.close();
		
		return true;
	}
	
	private boolean sendFile(long fileSize) {
		
		return true;
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		System.out.println("Starting Client...");
		Client client = new Client();

		client.start();
	}
}
