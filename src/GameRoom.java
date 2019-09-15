
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.Random;

public class GameRoom {
	static List<ServerThread> serverThreads = Collections.synchronizedList(new ArrayList<ServerThread>());
	static int numplayers = 0;
	static PuzzleMaker pm;
	boolean valid = false;
	boolean started = false;
	boolean gameover = false;
	private int turn = 0;

	public GameRoom(int port) {
		try {
			System.out.println("Binding to port " + port);
			ServerSocket ss = new ServerSocket(port);
			System.out.println("Bound to port " + port);
			serverThreads = new Vector<ServerThread>();
			System.out.println("Waiting for players to join");
			while (true) {
				Socket s = ss.accept(); // blocking
				System.out.println("Connection from: " + s.getInetAddress());
				ServerThread st = new ServerThread(s, this);
				serverThreads.add(st);
				if (numplayers == serverThreads.size()) {
					ChatMessage cm = new ChatMessage("Game", "The game will now begin!");
					cm.setType("BROADCAST");
					broadcastAll(cm);
					started = true;
					begin();
				} else if (serverThreads.size() == 1) {
					ChatMessage cm = new ChatMessage("Game", "How many players will be there?");
					cm.setType("PROMPT");
					st.sendMessage(cm);
				} else if (serverThreads.size() > 1 && serverThreads.size() < numplayers) {
					ChatMessage cm = new ChatMessage("Game", "There's a game waiting for you");
					cm.setType("DM");
					st.sendMessage(cm);
				}
				if (serverThreads.size() < numplayers) {
					int players_left = numplayers - serverThreads.size();
					ChatMessage cm = new ChatMessage("Game", "Waiting for " + players_left + " more player(s)");
					cm.setType("DM");
					st.sendMessage(cm);

				}
				if(started && serverThreads.size() > numplayers) {
					ChatMessage block = new ChatMessage("Game","Maximum capacity for this game is " + numplayers + ". Please try again later");
					block.setType("GAMEOVER");
					st.sendMessage(block);
				}
			}
		} catch (IOException ioe) {
			// System.out.println("ioe in ChatRoom constructor: " + ioe.getMessage());
			System.out.println("Waititng for players to join...");
		}
	}
	//----------USED MONITORS TO MAKE SURE THE SHARED RESOURCE IS PROPERLY ACCESSED--------------
	synchronized public void incrementTurn() {
		turn++; 
	}
	public int getTurn() {
		return turn; 
	}
	public void begin() {
		gameover = false; 
		//System.out.println("IN BEGIN GAME");
		// randomly choose gamefile within gamedata
		File dir = new File("gamedata");
		File[] files = dir.listFiles();
		ArrayList<File> fileset = new ArrayList<File>(Arrays.asList(files));
		Random rand = new Random();
		boolean malformatted = true;
		System.out.println("Reading random game file.");
		while (malformatted) {
			File selected = null;
			try {
				if(fileset.isEmpty()) {
					break; 
				}
				int fileindex = rand.nextInt(fileset.size()); 
				if(fileindex == fileset.size()) {
					fileindex--; 
				}
				selected = fileset.get(fileindex);
				String gamefile = selected.getName();
				pm = new PuzzleMaker("gamedata/" + gamefile);
				malformatted = false;
				System.out.println("Gamefile read successfully");
				pm.get_es();
				pm.makepuzzle();
				pm.make_gameboard();
			} catch (CustomException ce) {
				System.out.println("we have a malformatted file" + "\n" + "Removing " + selected.getName() + " from gamedata. " + "\n");
				fileset.remove(selected);
			}
		}
		if (fileset.isEmpty()) {
			System.out.println("All gamedata is corrupted");
			ChatMessage cantstart = new ChatMessage("Game","All gamedata is corrupted"); 
			cantstart.setType("GAMEOVER");
			broadcastAll(cantstart);
		} else {
			ChatMessage gm = new ChatMessage("Game", "Here's the board");
			System.out.println(gm.getMessage());
			gm.setType("UPDATE");
			gm.gameboard = pm.toString();
			gm.clues = pm.printClues();
			broadcastAll(gm);
			// tell everybody who's turn it is
			ChatMessage to_all = new ChatMessage("Game", "It's Player 1's turn");
			broadcastAll(to_all);
			// tell player one to enter some input
			ChatMessage input = new ChatMessage("Game", "Would you like to answer across or down?");
			input.setType("ACROSS_INPUT");
			serverThreads.get(0).sendMessage(input);
		}

	}

	public void broadcast(ChatMessage cm, ServerThread st) {
		if (cm != null) {
			//System.out.println("Player: " + cm.getMessage());
			for (ServerThread threads : serverThreads) {
				if (threads != st) {
					threads.sendMessage(cm);
				}
			}
		}
	}

	public void broadcastAll(ChatMessage cm) {
		if (cm != null) {
			for (ServerThread threads : serverThreads) {
				threads.sendMessage(cm);
			}
		}
	}

	// --------------THIS FUNCTION SWTICHES TURNS BETWEEN THE PLAYERS-----------------
	public void handoff(int index) {
		//System.out.println("in handoff function" + index);
		ChatMessage input = new ChatMessage("Game", "Would you like to answer across or down?");
		input.setType("ACROSS_INPUT");
		serverThreads.get(index).sendMessage(input);
	}

	public void update_board(Crossword cword) {
		if (cword != null) {
			System.out.println("Sending gameboard...");
			pm.update_gameboard(cword);
			ChatMessage gm = new ChatMessage("Game", "Here's the board");
			gm.setType("UPDATE");
			gm.gameboard = pm.toString();
			gm.clues = pm.printClues();
			broadcastAll(gm);
		}
	}

	public void game_over() {
		System.out.println("The game has concluded \n Sending scores. ");
		int winner = 0;
		int top_score = serverThreads.get(0).points;
		String final_scores = "Final Score " + "\n";
		for (int i = 0; i < serverThreads.size(); i++) {
			final_scores += "Player " + (i + 1) + " - " + serverThreads.get(i).points + " correct answers" + "\n";
			if (top_score < serverThreads.get(i).points) {
				top_score = serverThreads.get(i).points;
				winner = i;
			}
		}
		final_scores += "\n" + "Player " + (winner + 1) + " is the winner.";

		ChatMessage Gameover = new ChatMessage("Game", final_scores);
		Gameover.setType("GAMEOVER");
		broadcastAll(Gameover);
		while (!serverThreads.isEmpty()) {
			//System.out.println("trying to get rid of all the threads");
			serverThreads.remove(0);
		}
		System.out.println("Waiting for players to join...");
		started = false; 
		valid = false; 
	}
	public void consoleprint(String msg) {
		
	}
	public static void main(String[] args) {
		GameRoom cr = new GameRoom(3456);
	}
}
