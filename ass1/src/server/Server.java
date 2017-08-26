package server;

import java.io.*;
import java.net.*;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Server {
	
	private ServerSocket welcomeSocket;
	private static final int WELCOME_PORT = 6789;

	private JSONArray userList;

	Server() throws IOException{
		readUserList();
		welcomeSocket = new ServerSocket(WELCOME_PORT);
	}

	public void start() throws IOException{

		while (true) {
			
			// Wait for an incoming connection, and fork a thread to handle it
			Socket connectionSocket = welcomeSocket.accept();
			
			Thread t = new SFTPConnection(connectionSocket, userList);
			t.start();			
		}	
	}

	private void readUserList(){
		JSONParser parser = new JSONParser();

		try {
			userList = (JSONArray) parser.parse(new FileReader("res/userlist.json"));			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Starting Server...");
		
		Server server = new Server();
		server.start();
	}
}
