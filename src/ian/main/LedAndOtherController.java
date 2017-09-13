package ian.main;

import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class LedAndOtherController {
	private static final int LED_COUNT = 2;
	private static final byte I2C_ADDR = 0x12;
	
	private I2CDevice i2cDevice;
	
	private Color[] ledColors = new Color[LED_COUNT];
	private boolean isLedChange = false;
	
	{
		for (int i = 0; i < ledColors.length; i++) {
			ledColors[i] = new Color(0);
		}
	}
	
	
	public LedAndOtherController setLed(int index, Color color) {
		if (ledColors[index].getRGB() != color.getRGB()) {
			ledColors[index] = color;
			isLedChange = true;
		}
		return this;
	}
	public LedAndOtherController setAllLed(Color color) {
		for (int i = 0; i < LED_COUNT; i++) {
			setLed(i, color);
		}
		return this;
	}
	
	
	
	
	public LedAndOtherController updateLed() throws IOException {
		if (isLedChange) {
			isLedChange = false;
			ByteBuffer buffer = ByteBuffer.allocate(LED_COUNT * 4).order(ByteOrder.BIG_ENDIAN);
			for (Color ledColor : ledColors) {
				buffer.putInt(ledColor.getRGB());
			}
			i2cDevice.write(buffer.array());
			
		}
		return this;
	}
	public byte[] getSonar() throws IOException {
		byte[] data = new byte[6];
		i2cDevice.read(data, 0, data.length);
		return data;
	}
	public LedAndOtherController init() throws UnsupportedBusNumberException, IOException {
		I2CBus i2c = I2CFactory.getInstance(I2CBus.BUS_1);
		i2cDevice = i2c.getDevice(I2C_ADDR);
		return this;
	}
}
