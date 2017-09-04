package ian.main.serial.exception;

public class TimeOutException extends ProgramException {
	public TimeOutException(int i) {
		super("time out getData()=" + String.valueOf(i));
	}

	private static final long serialVersionUID = 3425866839435797476L;
}
