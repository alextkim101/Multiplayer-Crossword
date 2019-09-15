
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class PuzzleMaker {
	private int total_length;
	private ArrayList<Crossword> across_list;
	private ArrayList<Crossword> down_list;
	ArrayList<Crossword> total_list;
	Stack<Crossword> used_words;
	ArrayList<Crossword> words_left;
	private int rows = 0;
	private int cols = 0;
	int cropped_rows = 0; 
	int cropped_cols = 0; 
	String[][] board;
	String[][] gameboard;
	boolean badfile; 

	public String[][] getBoard() {
		return board;
	}

	public void setBoard(String[][] board) {
		this.board = board;
	}

	public PuzzleMaker(String filename) throws CustomException {
		badfile = false; 
		across_list = new ArrayList<Crossword>();
		down_list = new ArrayList<Crossword>();
		total_list = new ArrayList<Crossword>();
		words_left = new ArrayList<Crossword>();
		used_words = new Stack<Crossword>();
		FileReader fr = null;
		BufferedReader br = null;
		int acount = 0; 
		int dcount = 0; 
		try {
			fr = new FileReader(filename);
			br = new BufferedReader(fr);
			String line = br.readLine();
			boolean across = false;
			while (line != null) {
				if (line.equalsIgnoreCase("ACROSS")) {
					acount++; 
					across = true;
				} else if (line.equalsIgnoreCase("DOWN")) {
					dcount++; 
					across = false;
				} else if (line.contains("|")) {
					Crossword new_word = new Crossword(line, across);
					total_list.add(new_word);
					words_left.add(new_word);
					total_length++;
					if (new_word.isAcross()) {
						across_list.add(new_word);
					} else {
						down_list.add(new_word);
					}
				}
				line = br.readLine();
			}
			if(acount != 1 || dcount != 1) {
				throw new CustomException("The file is malformatted"); 
			}
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (CustomException ce) {
			throw ce; 
		} catch (NumberFormatException nfe ) {
			throw new CustomException("The file  malformatted"); 
		} finally {
			try {
				br.close();
				fr.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	public int getTotal_length() {
		return total_length;
	}

	public void setTotal_length(int total_length) {
		this.total_length = total_length;
	}

	public ArrayList<Crossword> getAcross_list() {
		return across_list;
	}

	public void setAcross_list(ArrayList<Crossword> across_list) {
		this.across_list = across_list;
	}

	public ArrayList<Crossword> getDown_list() {
		return down_list;
	}

	public void setDown_list(ArrayList<Crossword> down_list) {
		this.down_list = down_list;
	}

	// --------------------------actual functions-----------------------------
	public void get_es() {
		for (int i = 0; i < across_list.size(); i++) {
			cols += across_list.get(i).getWord().length();
		}
		cols *= 2;
		for (int i = 0; i < down_list.size(); i++) {
			rows += down_list.get(i).getWord().length();
		}
		rows *= 2;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getCols() {
		return cols;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

	public void print_board() {
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (board[i][j].equals(" ")) {
					System.out.print("_");
				} else {
					System.out.print(board[i][j]);
				}
			}
			System.out.println();
		}
		System.out.println();
	}

	public void makepuzzle() {
		across_list.sort(new sort_crosswords());
		down_list.sort(new sort_crosswords());
		total_list.sort(new sort_crosswords());
		words_left.sort(new sort_crosswords());

		// --------initialized board----------
		board = new String[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				board[i][j] = " ";
			}
		}
		Crossword start_word = total_list.get(0);
		start_word.row = rows/2; 
		start_word.col = cols/2; 
//		for (int i = 0; i < words_left.size(); i++) {
//			System.out.println(words_left.get(i).getWord());
//		}
		//System.out.println(words_left.size());
		// -----------place start word on the puzzle------------
		int index = 0;
		if (start_word.isAcross()) {
			for (int i = cols / 2; i < cols / 2 + start_word.getWord().length(); i++) {
				board[rows / 2][i] = start_word.getWord().charAt(index) + "";
				index++;
			}

		} else {
			for (int i = rows / 2; i < rows / 2 + start_word.getWord().length(); i++) {
				board[i][cols / 2] = start_word.getWord().charAt(index) + "";
				index++;
			}
		}
		used_words.push(start_word);
		words_left.remove(start_word);
		//this.print_board();

		int size = across_list.size() + down_list.size() + 1;

		if (helper(board, size) == false) {
			System.out.println("could not generate a board");
		}

	}

	public boolean safe(String[][] board, Crossword crossword, int x, int y, int index) {
		String word = crossword.getWord();

		//System.out.println("currenty checking " + word + "| across: " + crossword.isAcross());
		if (crossword.isAcross()) {
			if (y - index < 0) {
				return false;
			} else if (y + word.length() - 1 - index > rows) {
				return false;
			} else {
				if (!board[x][y - index - 1].contentEquals(" ")) {
					return false;
				}
				for (int i = y - index; i < y; i++) {
					if (!board[x][i].equals(" ")) {
						return false;
					}
					if (!board[x - 1][i].contentEquals(" ")) {
						return false;
					}
					if (!board[x + 1][i].contentEquals(" ")) {
						return false;
					}
				}
				if (!board[x][y + word.length() - index].contentEquals(" ")) {
					return false;
				}
				for (int i = y + 1; i < y + word.length() - index; i++) {
					if (!board[x][i].equals(" ")) {
						return false;
					}
					if (!board[x - 1][i].contentEquals(" ")) {
						return false;
					}
					if (!board[x + 1][i].contentEquals(" ")) {
						return false;
					}
				}
			}
		} else {

			if (x - index < 0) {
				return false;
			} else if (x + word.length() - 1 - index > cols) {
				return false;
			} else {
				if (!board[x - index - 1][y].contentEquals(" ")) {
					return false;
				}
				for (int i = x - index; i < x; i++) {
					if (!board[i][y].equals(" ")) {
						//System.out.println("flag 1 at " + i);
						return false;
					}
					if (!board[i][y - 1].contentEquals(" ")) {
						//System.out.println("flag 2");

						return false;
					}
					if (!board[i][y + 1].contentEquals(" ")) {
						//System.out.println("flag 3");
						return false;
					}
				}
				if (!board[x + word.length() - index][y].contentEquals(" ")) {
					return false;
				}
				for (int i = x + 1; i < x + word.length() - index; i++) {
					if (!board[i][y].equals(" ")) {
						//System.out.println("flag 4");

						return false;
					}
					if (!board[i][y - 1].contentEquals(" ")) {
						//System.out.println("flag 5");

						return false;
					}
					if (!board[i][y + 1].contentEquals(" ")) {
						//System.out.println("flag 6");

						return false;
					}
				}
			}
		}
		return true;
	}

	public boolean helper(String[][] board, int size) {
		if (words_left.isEmpty()) {
			return true;
		} else {
			for (int k = 0; k < words_left.size(); k++) {// loop through all the unused_words
				String cur_word = words_left.get(k).getWord();
				for (int l = 0; l < cur_word.length(); l++) {// loop through the letters of the word
					for (int i = 0; i < rows; i++) {
						for (int j = 0; j < cols; j++) {
							if (board[i][j].contentEquals(cur_word.charAt(l) + "")) {// if there's a potential 'cross'
																						// on the board
								//System.out.println(cur_word + " intersection at: " + i + " " + j + " with "
										//+ board[i][j] + " for " + l);
								if (safe(board, words_left.get(k), i, j, l)) {// see if it's safe to place
									// place tiles
									place_word(board, words_left.get(k), i, j, l);// place the word
									// words_left.remove(words_left.get(k)); //remove the word from words_left
									if (helper(board, size) == true) {
										return true; // recursively call another
									}
									//System.out.println("had to backtrack");
									remove(board, used_words.peek(), i, j, l);
									// remove board
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	public void place_word(String[][] board, Crossword cword, int x, int y, int index) {
		String word = cword.getWord();
		used_words.push(cword);
		words_left.remove(cword);
		if (cword.isAcross()) {
			cword.row = x;
			cword.col = y - index;
			int j = 0;
			for (int i = y - index; i < y; i++) {

				board[x][i] = word.charAt(j) + "";
				j++;
			}
			j++;
			for (int i = y + 1; i < y + word.length() - index; i++) {

				board[x][i] = word.charAt(j) + "";
				j++;
			}
		} else {
			cword.row = x - index;
			cword.col = y;
			int j = 0;
			for (int i = x - index; i < x; i++) {

				board[i][y] = word.charAt(j) + "";

				j++;
			}
			j++;
			for (int i = x + 1; i < x + word.length() - index; i++) {

				board[i][y] = word.charAt(j) + "";
				j++;
			}
		}
		//print_board();
	}

	public void remove(String[][] board, Crossword cword, int x, int y, int index) {
		String word = cword.getWord();
		if (cword.isAcross()) {
			int j = 0;
			for (int i = y - index; i < y; i++) {

				board[x][i] = " ";
				j++;
			}
			j++;
			for (int i = y + 1; i < y + word.length() - index; i++) {

				board[x][i] = " ";
				j++;
			}
		} else {
			int j = 0;
			for (int i = x - index; i < x; i++) {

				board[i][y] = " ";

				j++;
			}
			j++;
			for (int i = x + 1; i < x + word.length() - index; i++) {

				board[i][y] = " ";
				j++;
			}
		}
		used_words.pop();
		words_left.add(cword);
		//print_board();
	}

	public void make_gameboard() {
		List<Integer> clist = new ArrayList<>();
		List<Integer> rlist = new ArrayList<>();
		int cs = 0;
		int ce = 0;
		int rs = 0;
		int re = 0;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (!board[i][j].contentEquals(" ")) {
					rlist.add(i);
					clist.add(j);
				}
			}
		}
		List<Integer> rsortedlist = new ArrayList<>(rlist);
		List<Integer> csortedlist = new ArrayList<>(clist);
		Collections.sort(rsortedlist);
		Collections.sort(csortedlist);
		cs = csortedlist.get(0);
		ce = csortedlist.get(csortedlist.size() - 1);
		rs = rsortedlist.get(0);
		re = rsortedlist.get(rsortedlist.size() - 1);
		//System.out.println("rows: " + (re - rs) + " cols: " + (ce - cs));
		gameboard = new String[re - rs + 1][ce - cs + 1];
		boolean in = false;
		int rcounter = 0;
		int ccounter = 0;

		for (int i = rs; i <= re; i++) {
			for (int j = cs; j <= ce; j++) {
				for (int k = 0; k < total_list.size(); k++) {

					if (i == total_list.get(k).row && j == total_list.get(k).col) {
						//System.out.println(total_list.get(k).getWord() + ": " + total_list.get(k).isGuessed());
						gameboard[rcounter][ccounter] = total_list.get(k).getNum() + "_";
						total_list.get(k).row = total_list.get(k).row - rs; 
						total_list.get(k).col = total_list.get(k).col - cs; 

						in = true;
					}
				}
				if (!in) {
					if (!board[i][j].contentEquals(" ")) {
						gameboard[rcounter][ccounter] = " " + "_";
					} else {
						gameboard[rcounter][ccounter] = "  ";

					}
				}
				in = false;

				ccounter++;
			}
			ccounter = 0;
			rcounter++;
		}
		Crossword test_word = total_list.get(1); 
		
		for(int i = 0; i <  test_word.getWord().length(); i++) {
		
		}
		cropped_rows = re-rs+1; 
		cropped_cols = ce-cs+1; 
//		for (int i = 0; i < re - rs + 1; i++) {
//			for (int j = 0; j < ce - cs + 1; j++) {
//				System.out.print(gameboard[i][j]);
//			}
//			System.out.println();
//		}
	}
	
	public void print_gameboard() {
		for (int i = 0; i < cropped_rows; i++) {
			for (int j = 0; j < cropped_cols; j++) {
				System.out.print(gameboard[i][j]);
			}
			System.out.println();
		}
	}
	
	public void update_gameboard(Crossword cword) {
		
		for(int i = 0; i < across_list.size(); i++) {
			
		}
		int srow = cword.row; 
		int scol = cword.col; 
		int index = 0; 
		if(cword.isAcross()) {
			across_list.remove(cword); 
			for(int i = scol; i < scol+cword.getWord().length(); i++) {
				if(i == scol) {
					gameboard[srow][i] = cword.getNum() + "" + cword.getWord().charAt(index) ;
				} else {
					gameboard[srow][i] = " " + cword.getWord().charAt(index); 
							
				}
				index++; 
			}
		} else {
			down_list.remove(cword); 
			for(int i = srow; i < srow+cword.getWord().length(); i++) {
				if(i == srow) {
					gameboard[i][scol] = cword.getNum() + "" + cword.getWord().charAt(index) ;
				} else {
					gameboard[i][scol] = " " + cword.getWord().charAt(index); 
							
				}
				index++; 
			}
		}
	}
	public String toString() {
		String gboard = "\n";
		for (int i = 0; i < cropped_rows; i++) {
			for (int j = 0; j < cropped_cols; j++) {
				gboard+=gameboard[i][j];
			}
			gboard+="\n"; 
		}
		return gboard; 
	}
	public String printClues() {
		String clues = "ACROSS" + "\n"; 
		for(int i = 0; i < across_list.size(); i++) {
			clues+=across_list.get(i).getNum() + " " + across_list.get(i).getClue() + "\n"; 
		}
		clues+="DOWN" + "\n"; 
		for(int i = 0; i < down_list.size(); i++) {
			clues+=down_list.get(i).getNum() + " " + down_list.get(i).getClue() + "\n"; 
		}
		return clues; 
	}
	public String[][] getGameboard() {
		return gameboard;
	}

	public void setGameboard(String[][] gameboard) {
		this.gameboard = gameboard;
	}

}

class sort_crosswords implements Comparator<Crossword> {
	public int compare(Crossword a, Crossword b) {

		return b.getWord().length() - a.getWord().length();
	}

}
