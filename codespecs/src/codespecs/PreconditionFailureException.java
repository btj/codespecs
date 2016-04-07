package codespecs;

public class PreconditionFailureException extends CodeSpecsException {
	private static final long serialVersionUID = 1L;

	public PreconditionFailureException(String message) {
		super(message);
	}
}
