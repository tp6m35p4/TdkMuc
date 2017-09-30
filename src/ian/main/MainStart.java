package ian.main;

import java.io.IOException;
import java.util.Date;

import javax.xml.ws.WebServiceException;

import org.opencv.core.Mat;

import com.pi4j.io.gpio.exception.UnsupportedBoardType;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import ian.main.capture.OpenCameraFailedException;
import ian.main.mcu.MCU;
import ian.main.mcu.MwcData;
import ian.main.mcu.MwcSetData;
import ian.main.mcu.MCU.MsgIndex;
import ian.main.mcu.MCU.MsgIndex.MsgStruct;
import ian.main.surveillance.SurveillanceController;

public class MainStart {
	
	
	public static final String CMD_EXIT = "q";
	public static final int BUFFER_SIZE = CMD_EXIT.length();
	
	public static MwcData info = new MwcData();
	public static MwcSetData setRc = new MwcSetData();

	
	
	
	public static byte[] extraInfo = new byte[8];
	
	public static int cycleTime;
	public static int debug0, debug1, debug2, debug3, debug4, debug5, debug6, debug7;
	
	public static MsgStruct msgStruct = MsgIndex.STOP;
	
	
	
	public static Mat f;
	
	
	
	
	public static void run(String[] args) {
		byte[] buffer = new byte[BUFFER_SIZE];
		boolean isAlive = true;
		try (SurveillanceController sc = new SurveillanceController().start();
				MCU mcu = new MCU().setup()) {
			long time = new Date().getTime();
			while (isAlive) {
				if (!mcu.loop()) break;
				
				while (System.in.available() > 0) {
					for (int i = 1; i < BUFFER_SIZE; i++) {
						buffer[i - 1] = buffer[i];
					}
					System.in.read(buffer, BUFFER_SIZE - 1, 1);
					
					if (new String(buffer).equals(CMD_EXIT)) {
						isAlive = false;
						break;
					}
				}
				long time2 = new Date().getTime();
				cycleTime = (int) (time2 - time);
				time = time2;
			}
			System.out.println("bye~");
		} catch (WebServiceException | IOException | UnsupportedBoardType | InterruptedException | UnsupportedBusNumberException | OpenCameraFailedException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		run(args);
		System.exit(0);
	}
	
//	public static void test(String[] args) {
//		
//		try {
//			mwc = new MwcSerialAdapter().open();
//			
//			
//		} catch (UnsupportedBoardType | IOException | InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		while(true) {
//			try {
//				short[] a = mwc.getRc();
//				for (short aa : a) {
//					System.out.print(aa);
//					System.out.print(" , ");
//				}
//				System.out.println();
//				Thread.sleep(100);
//			} catch (NoConnectedException | TimeOutException | DataNotReadyException | UnknownErrorException
//					| IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
//	}
	
//	public static void test2(String[] args) {
//		
//		try {
//			System.out.println("start.");
//			TempLed tempLed = new TempLed();
//			System.out.println("2");
//			// tempLed.setMode(2);
//			Thread.sleep(1000);
//			tempLed.close();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
}






























