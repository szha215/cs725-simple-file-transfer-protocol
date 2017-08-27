package client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;



public class Client {
	
	public static boolean DEBUG = false;
	
	private static final String SERVER_ADDRESS = "localhost";
	private static final int WELCOME_PORT = 6789;
	
	private static final File DEFAULT_DIRECTORY = FileSystems.getDefault().getPath("res/ClientFolder").toFile().getAbsoluteFile();

	private Socket clientSocket;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private BufferedReader inFromUser;
	
	private BufferedInputStream dataInFromServer;
	private DataOutputStream dataOutToServer;
	
	Client() throws UnknownHostException, IOException{
		clientSocket = new Socket(SERVER_ADDRESS, WELCOME_PORT);
		
		// User input
		inFromUser = new BufferedReader(new InputStreamReader(System.in));

		// Messages in/out
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		
		// Data in/out
		dataInFromServer = new BufferedInputStream(clientSocket.getInputStream());
		dataOutToServer = new DataOutputStream(clientSocket.getOutputStream());
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
			
			// Other commands are fully server-side controlled
			switch(tokenizedClientSentence.nextToken().toUpperCase()) {
			case "RETR":
				retrCommand(clientSentence);
				continue;
				
			case "STOR":
				storCommand(clientSentence);
				continue;
				
			case "DONE":
				sendMessage(clientSentence);
				
				// Server has acknowledged disconnection
				serverSentence = readMessage();
				if (serverSentence.charAt(0) == '+') {
					System.out.println(serverSentence);
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
		} catch (IOException e) {}
	}

	private boolean retrCommand(String sentence) throws IOException {
		StringTokenizer tokenizedClientSentence = new StringTokenizer(sentence);
		tokenizedClientSentence.nextToken();  // Command
		
		// check for missing argument
		if (!tokenizedClientSentence.hasMoreTokens()) {
			System.err.println("Missing filename");
			
			return false;
		}

		String filename = tokenizedClientSentence.nextToken();
		
		// Directory in filenames are not allowed
		if (filename.indexOf('/') != -1 || filename.indexOf('\\') != -1) {
			System.err.println("Invalid filename");
			
			return false;
		}
		
		sendMessage(sentence);
		
		String serverSentence = readMessage();
		StringTokenizer tokenizedServerSentence = new StringTokenizer(serverSentence);
		
		char first = '\0';
		first = serverSentence.charAt(0);
		
		// File size
		if (first == ' ') {
			
			// Get file size
			long fileSize = Long.valueOf(tokenizedServerSentence.nextToken());
			System.out.println(String.format("File size is %d bytes", fileSize));
			
			// File fits, tell server to send
			if(fileCanFit(fileSize)){ 
				sendMessage("SEND");
				
				System.out.println("Waiting for file...");
				
			} else {
				sendMessage("STOP");
				System.out.println(readMessage());  // Server replies "aborted"
				
				return false;
			}
			
			// Receive file, append false
			receiveFile(filename, fileSize, false);
			System.out.println(String.format("File %s received", filename));
			
		} else {
			System.out.println(serverSentence);
			return false;
		}
		
		return true;
	}
	
	private boolean storCommand(String sentence) {
		StringTokenizer tokenizedClientSentence = new StringTokenizer(sentence);
		tokenizedClientSentence.nextToken();  // Command
		tokenizedClientSentence.nextToken();  // Argument (NEW, OLD, APP)
		
		// check for missing argument
		if (!tokenizedClientSentence.hasMoreTokens()) {
			System.err.println("Missing arguments");
			
			return false;
		}
		
		/*		step 0:	Check file validity	*/
		
		String filename = tokenizedClientSentence.nextToken();
		
		// Directory in filenames are not allowed
		if (filename.indexOf('/') != -1 || filename.indexOf('\\') != -1) {
			System.err.println("Invalid filename");
			
			return false;
		}
		
		// Specified file
		File file = new File(DEFAULT_DIRECTORY.toString() + "/" + filename);
		if (DEBUG) System.out.println("File of interest = " + file.toPath().toAbsolutePath().toString());
		
		// Specified file is not a file
		if (!file.isFile()) {
			System.out.println("File doesn't exist");
			return false;
			
		} else if (!file.canRead()) {
			System.out.println("File cannot be read");
			return false;
		}
		
		
		/*		step 1:	STOR request	*/
		
		sendMessage(sentence);
		
		String serverDecision = readMessage();
		System.out.println(serverDecision);
		
		// Server has denied STOR request
		if (serverDecision.charAt(0) != '+') {return false;}
		
		
		/*		step 2: Send file size	*/
		
		sendMessage(String.format("SIZE %s", String.valueOf(file.length())));
		
		serverDecision = readMessage();
		System.out.println(serverDecision);
		
		// Server cannot fit file
		if (serverDecision.charAt(0) != '+') {return false;}
		
		/*		step 3: Send file		*/
		
		sendFile(file);
		
		
		/*		step 4: Confirmation	*/
		
		serverDecision = readMessage();
		System.out.println(serverDecision);
		
		// Server could not save the file
		if (serverDecision.charAt(0) != '+') {return false;}
		
		return true;
	}

	/* Receive the file and store it to the default directory. This method overwrites existing file.
	 * 
	 * @param	filename	Name of the file to be written to.
	 * @param	fileSize	Size of the file expected to be received.
	 * @param	overwrite	Overwrites or append to file.
	 * @throw	IOException
	 * */
	private void receiveFile(String filename, long fileSize, boolean overwrite) throws IOException {
		File file = new File(DEFAULT_DIRECTORY.getPath().toString() + "/" + filename);
		FileOutputStream fileOutStream = new FileOutputStream(file, overwrite);
		BufferedOutputStream bufferedOutStream = new BufferedOutputStream(fileOutStream);

		// Read and write for all bytes
		for (int i = 0; i < fileSize; i++) {
			bufferedOutStream.write(dataInFromServer.read());
		}

		bufferedOutStream.close();
		fileOutStream.close();
	}
	
	/* Sends a file to the client's socket.
	 * 
	 * @param	file	The file that wishes to be sent.
	 * @return	success	Whether the file was sent.
	 * */
	private boolean sendFile(File file) {
		byte[] bytes = new byte[(int) file.length()];

		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bufferedInStream = new BufferedInputStream(new FileInputStream(file));
			
			if (DEBUG) System.out.println("Total file size to read (in bytes) : " + fis.available());

			int content = 0;
			
			// Read and send file until the whole file has been sent
			while ((content = bufferedInStream.read(bytes)) >= 0) {
				dataOutToServer.write(bytes, 0, content);

				if (DEBUG) System.out.println(content);
			}
			
			bufferedInStream.close();
			fis.close();
			dataOutToServer.flush();
	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		if (DEBUG) System.out.println("FILE SENT");

		return true;
	}
	
	/* Checks if a specified file size in bytes can fit in the current directory.
	 * 
	 * @param	fileSize	Size of the file to be stored, long byte.
	 * @return	canFit		Whether if the file can fit.
	 * @throw	IOException
	 * */
	private boolean fileCanFit(long fileSize) throws IOException {
		long freeSpace = Files.getFileStore(DEFAULT_DIRECTORY.toPath().toRealPath()).getUsableSpace();
		
		if (fileSize < freeSpace) {return true;}
		
		return false;
	}
	
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		System.out.println("Starting Client...");
		
		Client client = new Client();
		client.start();
	}
}
