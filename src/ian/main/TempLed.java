package ian.main;

import java.util.Timer;
import java.util.TimerTask;

import javax.xml.ws.WebServiceException;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.sun.xml.internal.ws.Closeable;

public class TempLed implements Closeable {
	private static final Pin LED_R = RaspiPin.GPIO_05;
	private static final Pin LED_G = RaspiPin.GPIO_04;
	private static final Pin LED_B = RaspiPin.GPIO_03;
	
	private static final int PERIOD = 100;
	
	
	
	private GpioController gpio;
	private GpioPinDigitalOutput pinR;
	private GpioPinDigitalOutput pinG;
	private GpioPinDigitalOutput pinB;
	
	
	
	Timer t;
	TimerTask tt;
	
	private boolean rr = false;
	private boolean gg = false;
	private boolean bb = false;
	
	
	private boolean isToggleR = false;
	private boolean isToggleG = false;
	private boolean isToggleB = false;
	

	public void setMode(boolean r, boolean g, boolean b, boolean rT, boolean gT, boolean bT) {
		if (r == rr &&
			g == gg &&
			b == bb &&
			rT == isToggleR &&
			gT == isToggleG &&
			bT == isToggleB) {
			return;
		}
			
		if (r) pinR.high(); else pinR.low();
		if (g) pinG.high(); else pinG.low();
		if (b) pinB.high(); else pinB.low();
		rr = r;
		gg = g;
		bb = b;
		isToggleR = rT;
		isToggleG = gT;
		isToggleB = bT;
		
	}

	{
		gpio = GpioFactory.getInstance();
		pinR = gpio.provisionDigitalOutputPin(LED_R, "ledR", PinState.LOW);
		pinG = gpio.provisionDigitalOutputPin(LED_G, "ledG", PinState.LOW);
		pinB = gpio.provisionDigitalOutputPin(LED_B, "ledB", PinState.LOW);
		
		t = new Timer();
		tt = new TimerTask() {
			
			@Override
			public void run() {
				if (isToggleR) pinR.toggle();
				if (isToggleG) pinG.toggle();
				if (isToggleB) pinB.toggle();
			}
		};
		t.scheduleAtFixedRate(tt, 0, PERIOD);
		
		
		pinR.setShutdownOptions(true, PinState.LOW);
		pinG.setShutdownOptions(true, PinState.LOW);
		pinB.setShutdownOptions(true, PinState.LOW);
		
	}

	@Override
	public void close() throws WebServiceException {
		tt.cancel();
		t.cancel();
		gpio.shutdown();
	}
}
