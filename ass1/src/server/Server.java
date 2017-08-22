package server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Server {
	
	private ServerSocket welcomeSocket;
	private static final int WELCOME_PORT = 6789;

	private HashMap<String, String> users;
	private JSONArray userList;
	
	private boolean authenticated = false;

	Server() throws IOException{
		readUserList();
		welcomeSocket = new ServerSocket(WELCOME_PORT);
		users = new HashMap<String, String>();
	}

	public void start() throws IOException{

		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			
			Thread t = new SFTPConnection(connectionSocket, userList);
			t.start();			
		}	
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

	
	public static void main(String[] args) throws IOException {
		System.out.println("Starting Server...");
		Server server = new Server();

		server.start();
	}
	

}
