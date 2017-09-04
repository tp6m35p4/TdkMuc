package ian.main.serial.exception;

public class DataNotReadyException extends ProgramException {
	public DataNotReadyException() {
		super("DataNotReadyException");
	}
	public DataNotReadyException(String string) {
		super(string);
	}

	private static final long serialVersionUID = 8115229530234656600L;
}
