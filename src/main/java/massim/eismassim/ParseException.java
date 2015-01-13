package massim.eismassim;

public class ParseException extends Exception {

	private String path="";
	
	public ParseException(String message) {
		super(message);
	}

	public ParseException(String path, String message) {
		super(message);
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}

}
