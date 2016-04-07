package codespecs;

public class PostconditionFailureException extends CodeSpecsException {
	private static final long serialVersionUID = 1L;

	public PostconditionFailureException(String message) {
		super(message);
	}
}
