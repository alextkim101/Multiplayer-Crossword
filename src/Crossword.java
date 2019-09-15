
public class Crossword {
	private int num; 
	private boolean across; //true is across and false is down 
	private boolean guessed; 
	public boolean isGuessed() {
		return guessed;
	}
	public void setGuessed(boolean guessed) {
		this.guessed = guessed;
	}
	private String word;
	private String clue; 
	int row; 
	int col; 
	public Crossword(String line, boolean across) throws NumberFormatException, CustomException {
		guessed = false; 
		this.across = across; 
		String[] tokens = line.split("\\|"); 
		if(tokens == null || tokens.length != 3) {
			throw new CustomException("The file is not formatted correctly");
		}
		try {
			num = Integer.parseInt(tokens[0]); 
			word = tokens[1]; 
			if(word.length() != word.trim().length()) {
				throw new CustomException("The answer cannot contain any whitespaces");
			}
			clue = tokens[2]; 
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace(); 
			throw nfe; 
		} 
	}
	public String getClue() {
		return clue;
	}
	public void setClue(String clue) {
		this.clue = clue;
	}
	public int getNum() {
		return num;
	}
	public void setNum(int num) {
		this.num = num;
	}
	public boolean isAcross() {
		return across;
	}
	public void setAcross(boolean across) {
		this.across = across;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	} 
	

}
