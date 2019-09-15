import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerThread extends Thread {

	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private GameRoom cr;
	boolean across = false;
	int clue_num = 0;
	Crossword cw = null;
	int points = 0;
	String orientation = "";
	public ServerThread(Socket s, GameRoom cr) {
		try {
			this.cr = cr;
			oos = new ObjectOutputStream(s.getOutputStream());
			ois = new ObjectInputStream(s.getInputStream());
			this.start();
		} catch (IOException ioe) {
			System.out.println("ioe in ServerThread constructor: " + ioe.getMessage());
		}
	}
	//----------------USED MONITORS TO MAKE SURE ONLY ONE CLIENT WRITES TO THE STREAM AT A TIME--------------------
	synchronized public void sendMessage(ChatMessage cm) {
		try {
			oos.writeObject(cm);
			oos.flush();
		} catch (IOException ioe) {
			System.out.println("ioe: " + ioe.getMessage());
		}
	}

	public void run() {
		try {
			while (true) {

				//System.out.println("IN THREAD RUN");

				ChatMessage cm = (ChatMessage) ois.readObject();
				//System.out.println(cm.getUsername() + ": " + cm.getMessage());

				if (!cr.started && GameRoom.serverThreads.size() == 1) {
					//System.out.println(cm.getUsername() + ": " + cm.getMessage());
					try {
						int numplayers = Integer.parseInt(cm.getMessage());
						GameRoom.numplayers = numplayers;
						System.out.println("Number of players: " + GameRoom.numplayers);
					} catch (NumberFormatException nfe) {
						nfe.printStackTrace();
					}
				}
				if (!cr.started && GameRoom.serverThreads.size() < GameRoom.numplayers) {
					int players_left = GameRoom.numplayers - GameRoom.serverThreads.size();
					ChatMessage waiting = new ChatMessage("Game", "Waiting for " + players_left + " more player(s)");
					System.out.println(waiting.getMessage());
					cm.setType("DM");
					this.sendMessage(waiting);

				}
				// --------not sure if this code is ever reached-------------
				if (!cr.started && GameRoom.serverThreads.size() == GameRoom.numplayers) {
					cr.begin();
					cr.started = true;
				}
				if (cm.getType().contentEquals("ACROSS_RESPONSE")) {
					boolean empty = false;
					//System.out.println("yea we in here");
					String user_input = cm.getMessage();
					if (!user_input.equalsIgnoreCase("a") && !user_input.equalsIgnoreCase("d")) {
						ChatMessage error_msg = new ChatMessage("Game", "your input can only be \'a\' or \'d\'");
						error_msg.setType("ACROSS_INPUT");
						this.sendMessage(error_msg);
					} else {
						if (user_input.contentEquals("a")) {
							if (cr.pm.getAcross_list().isEmpty()) {
								empty = true;
								ChatMessage emptyflag = new ChatMessage("Game",
										"Across list is empty please choose from down list");
								emptyflag.setType("ACROSS_INPUT");
								this.sendMessage(emptyflag);
							}
							across = true;
						} else {
							if (cr.pm.getDown_list().isEmpty()) {
								empty = true;
								ChatMessage emptyflag = new ChatMessage("Game",
										"Down list is empty please choose from across list");
								emptyflag.setType("ACROSS_INPUT");
								this.sendMessage(emptyflag);
							}
							across = false;
						}
						if (!empty) {
							ChatMessage num_request = new ChatMessage("Game", "which number");
							num_request.setType("NUM_REQUEST");
							this.sendMessage(num_request);
						}
					}
				} else if (cm.getType().contentEquals("NUM_RESPONSE")) {
					boolean found = false;
					orientation = "";
					clue_num = Integer.parseInt(cm.getMessage());
					if (across) {
						orientation = "across";
						for (int i = 0; i < cr.pm.getAcross_list().size(); i++) {
							if (cr.pm.getAcross_list().get(i).getNum() == clue_num) {
								found = true;
								cw = cr.pm.getAcross_list().get(i);
							}
						}
					} else {
						orientation = "down";
						for (int i = 0; i < cr.pm.getDown_list().size(); i++) {
							if (cr.pm.getDown_list().get(i).getNum() == clue_num) {
								found = true;
								cw = cr.pm.getDown_list().get(i);
							}
						}
					}
					if (found) {
						ChatMessage guess_request = new ChatMessage("Game",
								"What is your guess for " + clue_num + " " + orientation);
						guess_request.setType("GUESS_REQUEST");
						this.sendMessage(guess_request);
					} else {
						ChatMessage num_request = new ChatMessage("Game",
								"that number wasn't found" + "\n" + "Which number?");
						num_request.setType("NUM_REQUEST");
						this.sendMessage(num_request);
					}
				} else if (cm.getType().contentEquals("GUESS_RESPONSE")) {
					boolean correct = false;
					// broadcast to all other players what has been guessed
					int player = cr.getTurn() % GameRoom.serverThreads.size();
					ChatMessage all = new ChatMessage("Game", "Player " + (player + 1) + " guessed " + "\"" + cm.getMessage() + "\"" + " for " + clue_num + " " + orientation);
					System.out.println(all.getMessage());
					cr.broadcast(all, this);

					if (across) {
						if (cw.getWord().equalsIgnoreCase(cm.getMessage())) {
							correct = true;
							cr.pm.getAcross_list().remove(cw);
						}
					} else {
						if (cw.getWord().equalsIgnoreCase(cm.getMessage())) {
							correct = true;
							cr.pm.getDown_list().remove(cw);
						}
					}
					if (correct) {
						ChatMessage check = new ChatMessage("Game", "That is correct!");
						System.out.println(check.getMessage());
						cr.broadcastAll(check);
						if (GameRoom.pm.getAcross_list().isEmpty() && GameRoom.pm.getDown_list().isEmpty()) {
							cr.gameover = true;
							cr.update_board(cw);
							cr.game_over();
						} else {
							this.points++;
							//System.out.println("Player got the answer right");
							int index = cr.getTurn() % GameRoom.serverThreads.size();
							cr.update_board(cw);
							cr.handoff(index);
						}
					} else {
						ChatMessage check = new ChatMessage("Game", "That is incorrect!");
						System.out.println(check.getMessage());
						cr.broadcastAll(check);
						//System.out.println("Player got the answer wrong");
						cr.incrementTurn();
						int index = cr.getTurn() % GameRoom.serverThreads.size();
						ChatMessage broadcast = new ChatMessage("Game", "It's Player " + (index + 1) + "\'s turn!");
						System.out.println(broadcast.getMessage());
						cr.broadcastAll(broadcast);
						cr.handoff(index);
					}
				}
			}
		} catch (IOException ioe) {
			//System.out.println("ioe in ServerThread.run(): " + ioe.getMessage());
		} catch (ClassNotFoundException cnfe) {
			System.out.println("cnfe: " + cnfe.getMessage());
		}
	}
}
