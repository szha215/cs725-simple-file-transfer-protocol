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
		String sentence;
		String filename = "";
		long fileSize = 0;
		
		StringTokenizer tokenizedServerSentence;
		
		while(true) {
			System.out.println("SOCKET CLOSED? " + clientSocket.isClosed());
			
			serverSentence = readMessage(inFromServer);
			System.out.println(serverSentence);
			
			if (serverSentence.charAt(0) == ' ') {
				tokenizedServerSentence = new StringTokenizer(serverSentence);
				
				fileSize = Long.valueOf(tokenizedServerSentence.nextToken());
				System.out.println("File size = " + fileSize);
				
				sentence = inFromUser.readLine();
				sendMessage(outToServer, sentence);
				
//				System.out.println(readMessage(inFromServer));
				
				if (receiveFile(filename, fileSize)) {
					System.out.print(String.format("File %s received", filename));

					continue;
				}
			}
			
			sentence = inFromUser.readLine();
			
			sendMessage(outToServer, sentence);
			
			
			StringTokenizer tokenizedLine = new StringTokenizer(sentence);
			
			switch(tokenizedLine.nextToken().toUpperCase()) {
			case "DONE":
				if (readMessage(inFromServer).charAt(0) == '+') {
					System.out.println("DONE, closing socket");
					clientSocket.close();
					
					return;
				}
				
				break;
			case "RETR":
				try {
					filename = tokenizedLine.nextToken();
				} catch (NoSuchElementException e) {
					continue;
				}
				
				break;
			}
			

			
		}
		
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
			
			// '!' detected, log in
			if (character == '!') {
				loggedIn = true;
				System.out.println("client logged in");
			} else if (character == '-') {
				loggedIn = false;
				System.out.println("client logged off");
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
	
	private boolean receiveFile(String filename, long fileSize) throws IOException {
		byte[] bytes = new byte[(int) fileSize];
		
		File file = new File(DEFAULT_DIRECTORY.getPath().toString() + "/" + filename);
//		FileOutputStream fileOutStream = new FileOutputStream(file);
		BufferedOutputStream bufferedOutStream = new BufferedOutputStream(new FileOutputStream(file));
		
		int count = 0;
		while((count = dataInFromServer.read(bytes)) > 0) {
			bufferedOutStream.write(dataInFromServer.read());
		}
		
		bufferedOutStream.close();
		
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
