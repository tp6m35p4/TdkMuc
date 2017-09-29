package ian.main.serial;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.pi4j.io.gpio.exception.UnsupportedBoardType;

import ian.main.serial.exception.DataNotReadyException;
import ian.main.serial.exception.NoConnectedException;
import ian.main.serial.exception.TimeOutException;
import ian.main.serial.exception.UnknownErrorException;

public class MwcSerialAdapter {
	private final MwcSerial serial = new MwcSerial();
	public MwcSerialAdapter() {
		
	}
	public MwcSerialAdapter open() throws UnsupportedBoardType, IOException, InterruptedException {
		serial.open();
		return this;
	}
	public void close() throws IllegalStateException, IOException {
		serial.close();
	}
	
	public short[] getRc() throws IOException, NoConnectedException, TimeOutException, DataNotReadyException, UnknownErrorException {
		short outt[] = new short[8];
		ByteBuffer.wrap(serial.getData(MwcSerial.Cmd.MSP_RC))
				.order(ByteOrder.LITTLE_ENDIAN)
				.asShortBuffer()
				.get(outt);
		return outt;
	}
	public void setRc(short[] data) throws IOException, DataNotReadyException, NoConnectedException, TimeOutException, UnknownErrorException {
		if (data == null || data.length != 8) {
			throw new DataNotReadyException("data length = " + data != null ? String.valueOf(data.length) : "null");
		}
		ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
		for (short each : data) {
			buffer.putShort(each);
		}
		serial.setData(MwcSerial.Cmd.MSP_SET_RAW_RC, buffer.array());
	}
	
	public byte[] getRpi() throws NoConnectedException, TimeOutException, DataNotReadyException, UnknownErrorException, IOException {
		return serial.getData(MwcSerial.Cmd.MSP_RPI);
	}
	
	public byte[] getDebug() throws NoConnectedException, TimeOutException, DataNotReadyException, UnknownErrorException, IOException {
		return serial.getData(MwcSerial.Cmd.MSP_DEBUG);
	}
	
	
}
