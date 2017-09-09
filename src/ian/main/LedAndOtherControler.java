package ian.main;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.sun.prism.paint.Color;

public class LedAndOtherControler {
	private static final int LED_COUNT = 4;
	private static final byte I2C_ADDR = 0x12;
	
	private I2CDevice i2cDevice;
	
	private Color[] ledColors = new Color[LED_COUNT];
	private int ledMode = 0;
	private boolean isLedChange = false;
	
	public LedAndOtherControler setLed(int index, Color color) {
		ledColors[index] = color;
		return this;
	}
	public LedAndOtherControler setAllLed(Color color) {
		for (int i = 0; i < LED_COUNT; i++) {
			setLed(i, color);
		}
		return this;
	}
	
	
	public LedAndOtherControler updateLed() throws IOException {
		if (isLedChange) {
			if (ledMode == 0) {
				ByteBuffer buffer = ByteBuffer.allocate(LED_COUNT * 4);
				for (Color ledColor : ledColors) {
					ledColor.putBgraPreBytes(buffer);
				}
				i2cDevice.write(buffer.array());				
			} else {
				i2cDevice.write((byte) ledMode);
			}
			
		}
		return this;
	}
	public byte[] getSonar() throws IOException {
		byte[] data = new byte[6];
		i2cDevice.read(data, 0, data.length);
		return data;
	}
	public LedAndOtherControler init() throws UnsupportedBusNumberException, IOException {
		I2CBus i2c = I2CFactory.getInstance(I2CBus.BUS_2);
		i2cDevice = i2c.getDevice(I2C_ADDR);
		return this;
	}
}
