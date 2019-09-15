import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class GameClient extends Thread{


	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private Socket s = null; 
	public	GameClient(String hostname, int port) throws IOException {
		try {
			System.out.println("Trying to connect to " + hostname + ": " + port);
			 s = new Socket(hostname, port);
			System.out.println("Connected to " + hostname + ": " + port);
			ois = new ObjectInputStream(s.getInputStream());
			oos = new ObjectOutputStream(s.getOutputStream());
			Scanner scan = new Scanner(System.in);
			
		} catch (IOException ioe) {
			System.out.println("ioe in GameClient constructor: " + ioe.getMessage());
			throw new IOException(); 
		} catch (NumberFormatException nfe) {
			System.out.println("nfe in GameClient constructor");
			throw new NumberFormatException(); 
		}
	}
	public void run() {
		try {
			while(true) {
				
				ChatMessage cm = (ChatMessage)ois.readObject();
				System.out.println(cm.getUsername() + ": " + cm.getMessage());
			}
		} catch (IOException ioe) {
			System.out.println("ioe in ChatClient.run(): " + ioe.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println("cnfe: " + cnfe.getMessage());
		}
	}
	
	public static void main(String [] args) throws ClassNotFoundException, IOException {
		Scanner console = new Scanner(System.in);
		boolean invalid = true; 
		GameClient gc = null; 
		while(invalid) {
			System.out.print("Enter a hostname: ");
			String hostname = console.nextLine(); 
			System.out.print("Enter port number: ");
			try {
				int port = Integer.parseInt(console.nextLine()); 
				//convert user input and connect to host 
				invalid = false; 
				gc = new GameClient(hostname,port);
			} catch (NumberFormatException nfe) {
				invalid = true; 
			} catch (IOException ioe) {
				invalid = true; 
			} 
		}
		while(true) {
			ChatMessage cm = (ChatMessage)gc.ois.readObject();
			System.out.println(cm.getUsername() + ": " + cm.getMessage());
			if(cm != null) {
				if(cm.getType() != null && cm.getType().contentEquals("PROMPT")) {
					String input = console.nextLine(); 
					ChatMessage response = new ChatMessage("Player", input); 
					response.setType("RESPONSE");
					gc.oos.writeObject(response);
					gc.oos.flush();
				} else if(cm.getType() != null && cm.getType().contentEquals("UPDATE")) {
					System.out.println(cm.gameboard);
					System.out.println(cm.clues);
				} else if (cm.getType() != null && cm.getType().contentEquals("ACROSS_INPUT")) {
					String input = console.nextLine(); 
					ChatMessage across_response = new ChatMessage("Player",input);
					across_response.setType("ACROSS_RESPONSE");
					gc.oos.writeObject(across_response);
					gc.oos.flush();
				} else if(cm.getType() != null && cm.getType().contentEquals("NUM_REQUEST")) {
					boolean valid = false; 
					String input = ""; 
					while(!valid) {
						try {
							 input = console.nextLine(); 
							int guess_num = Integer.parseInt(input);
							valid = true; 
						} catch(NumberFormatException nfe) {
							System.out.println("your input must be a number");
							valid = false; 
						}
						
					}
					ChatMessage across_response = new ChatMessage("Player",input);
					across_response.setType("NUM_RESPONSE");
					gc.oos.writeObject(across_response);
					gc.oos.flush();
				} else if(cm.getType() != null && cm.getType().contentEquals("GUESS_REQUEST")) {
					//System.out.println("IN GUESS");
					String input = console.nextLine(); 
					ChatMessage guess_response = new ChatMessage("Player",input); 
					guess_response.setType("GUESS_RESPONSE");
					gc.oos.writeObject(guess_response);
					gc.oos.flush(); 
				} else if (cm.getType() != null && cm.getType().contentEquals("GAMEOVER")) { 
					System.exit(0); 
				}
			}
		}
	}
}
