package build.pluto.buildgit;

public class GitException extends Exception {

	private static final long serialVersionUID = 3484624156349321124L;

	public GitException(String msg) {
		super(msg);
	}

	public GitException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
