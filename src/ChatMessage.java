
import java.io.Serializable;

public class ChatMessage implements Serializable {
	public static final long serialVersionUID = 1;
	private String username;
	private String message;
	String type; 
	String gameboard; 
	String clues; 
	
	public ChatMessage(String username, String message) {
		this.username = username;
		this.message = message;
	}
	public ChatMessage(String type) {
		this.type = type; 
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getUsername() {
		return username;
	}
	public String getMessage() {
		return message;
	}
}
