package ian.main;

import java.io.IOException;
import java.util.Date;

import javax.xml.ws.WebServiceException;

import com.pi4j.io.gpio.exception.UnsupportedBoardType;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.sun.xml.internal.ws.Closeable;

import ian.main.serial.MwcSerialAdapter;
import ian.main.serial.exception.DataNotReadyException;
import ian.main.serial.exception.NoConnectedException;
import ian.main.serial.exception.TimeOutException;
import ian.main.serial.exception.UnknownErrorException;

public class MCU implements Closeable {
	
	public static final boolean isTest = true;
	
	/* 高度誤差範圍 */
	static final int altError = 20;
	
	static MwcSerialAdapter mwc;
	static LedAndOtherController loc;
	// static TempLed led;
	
	static MwcData info = MainStart.info;
	static MwcSetData setRc = MainStart.setRc;
	
	public static int step = 0;
	
	
	static int ledStep = 0;
	
	
	public static int setWantAlt = 0;

	
	
	// ----------------throttle---------------------
	public static int throttleHoldValue;
	static int throttleValue = 0;
	// ----------------throttle---------------------
	
	
	// ------------------time-----------------------
	static long mwcErrorTime = 0;
	static long ledUpdateTime = 0;
	static long ledStepTime = 0;
	// ------------------time-----------------------
	
	
	// ------------------error flag-----------------
	static int mwcError = 0;
	static int ledError = 0;
	static int modeError = 0;
	// ------------------error flag-----------------
	
	
	// ------------------mode flag------------------
	static int armMode = 0;
	static int baroMode = 0;
	// ------------------mode flag------------------
	
	static int choose(int index, int defaultData, int... data) {
		return index < data.length ? data[index] : defaultData;
	}
	
	static void setMsg(int index) {
		switch (index) {
		case 0:
			MainStart.level = 0;
			MainStart.msgStr = "Stop.";
			break;
		case 1:
			MainStart.level = 0;
			MainStart.msgStr = "Run.";
			break;
			
		case 2:
			MainStart.level = 1;
			MainStart.msgStr = "Wait ok_to_arm...";
			break;
		case 3:
			MainStart.level = 1;
			MainStart.msgStr = "Wait angle_mode...";
			break;
		case 4:
			MainStart.level = 1;
			MainStart.msgStr = "Wait ok_to_arm and angle_mode...";
			break;
		default:
			throw new RuntimeException("unknown error.");
		}
	}
	
	
	static void stl() {
		switch (step) {
		case 0:
			step = 1;
			armMode = 1;
			baroMode = 1;
			
			setMsg(1);
			break;
		case 1:
			if (info.ok_to_arm && info.angle_mode) {
				step = 10;
			} else {
				int tmp = (info.ok_to_arm ? 1 : 0) + (info.angle_mode ? 2 : 0);
				switch (tmp) {
				case 0: setMsg(1); break;
				case 1: setMsg(2); break;
				case 2: setMsg(3); break;
				case 3: setMsg(4); break;
				default: throw new RuntimeException("unknown error.");
				}
				if (!info.ok_to_arm) {
					System.out.println("waitting ok_to_arm...");
				}
				if (!info.angle_mode) {
					System.out.println("waitting angle_mode...");
				}
			}
			break;
		case 10: // 解鎖油門
			armMode = 2;
			throttleValue = 1098;
			if (info.armed) {
				createTimer();
				step = 11;				
			}
			break;
		case 11:
			if (timerOn(1500)) {
				step = 12;
				throttleValue = 1500;
			}
			break;
		case 12: // 起飛
			if (info.altEstAlt == 0) {
				throttleValue += 10;
			}
			
			if (info.altEstAlt > 20) {
				throttleHoldValue = throttleValue;
				createTimer();
				step = 1012;
			}
			break;
		case 1012:
			if (timerOn(100)) {
				step = 13;
			}
			break;
		case 13: // 設定高度200cm
			baroMode = 2;
			if (info.baro_mode) {
				setWantAlt = 80;
				step = 14;
			}
			break;
		case 14: // 等待至200
			if (Math.abs(info.altEstAlt - setWantAlt) <= altError) {
				step = 15;
			}
			break;
		case 15:
			createTimer();
			step = 16;
			break;
		case 16:
			if (timerOn(15000)) {
				step = 100;
			}
			break;
		case 100: // 終點降落
			setWantAlt = 0;
			step = 101;
			break;
		case 101:
			if (info.altEstAlt < 5) {
				step = 102;
			}
			break;
		case 102: // 上鎖油門
			throttleValue = 1098;
			armMode = 1;
			baroMode = 1;
			break;
		default:
			break;
		}
	}
	static void mode() {
		
		setRc.setAux1(choose(armMode , 0, 0, 1098, 1898));
		
		setRc.setAux3(choose(baroMode, 0, 0, 1500, 1898));
		
		if (info.baro_mode) {
			int offset = setWantAlt - info.altHold;
			
			if (Math.abs(offset) < 0) {
				offset = 0;
			}
			
			setRc.setThrottle(offset + throttleHoldValue);
		} else {
			setRc.setThrottle(throttleValue);
		}
		
		
		
		
		CyzClass.mode();
		
//		switch (ledMode) {
//		case 1:
//			loc.setAllLed(Color.BLACK);
//			if (getTime() - ledStepTime > 200) {
//				ledStepTime = getTime();
//				loc.setLed(ledStep, Color.RED);
//				if (++ledStep >= 2) {
//					ledStep = 0;
//				}
//			}
//			break;
//		case 2:
//			loc.setAllLed(Color.GREEN);
//			break;
//		case 3:
//			loc.setLed(0, Color.RED).setLed(1, Color.GREEN);
//			break;
//		default:
//			loc.setAllLed(Color.BLACK);
//			break;
//		}
		
	}
	
	static long time;
	static void createTimer() {
		time = getTime();
	}
	static boolean timerOn(long millis) {
		return getTime() - time >= millis;
	}
	
	static long getTime() {
		return new Date().getTime();
	}
	
	
	public MCU setup() throws UnsupportedBoardType, IOException, InterruptedException, UnsupportedBusNumberException {
		if (isTest) return this;
		mwc = new MwcSerialAdapter().open();
		loc = new LedAndOtherController().init();
		return this;
	}
	
	
	
	public boolean loop() {
		if (isTest) return true;
		
		setRc.reset();
		
		try {
			info.setData(mwc.getRpi());
			mwcError = 0;
		} catch(NoConnectedException | TimeOutException | DataNotReadyException | UnknownErrorException | IOException e) {
			if (mwcError == 0) {
				mwcErrorTime = getTime();
			}
			mwcError++;
			e.printStackTrace();
		}
		
		if (getTime() - ledUpdateTime > 250) {
			try {
				info.setOtherData(loc.getSonar());
				ledError = 0;
			} catch(IOException e) {
				ledError++;
				e.printStackTrace();
			}
			ledUpdateTime = getTime();
		}
		
		
		
		if (info.extraRc[2] < 1700) { // ems
			step = 0;
			
			throttleValue = 0;
			armMode = 0;
			baroMode = 0;
			
			MainStart.level = 1;
			MainStart.msgStr = "Stop.";
		} else {
			try {
				stl();
				mode();
				modeError = 0;
			} catch(Exception e) {
				modeError++;
				e.printStackTrace();
			}
			
		}
		
		try {
			mwc.setRc(setRc.getData());
		} catch (DataNotReadyException | NoConnectedException | TimeOutException | UnknownErrorException | IOException e) {
			e.printStackTrace();
		}
		
		
		if (ledError != 0 || modeError != 0 || (mwcError != 0 && (getTime() - mwcErrorTime > 800))) {
			System.out.printf("ledError = %d\nmodeError = %d\nmwcError = %d\n", ledError, modeError, mwcError);
			return false;
		}
		try {
			loc.updateLed();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	@Override
	public void close() throws WebServiceException {
		if (isTest) return;
		try {
			mwc.close();
		} catch (IllegalStateException | IOException e) {
			throw new WebServiceException(e);
		}
		try {
			loc.close();
		} catch (IOException e) {
			throw new WebServiceException(e);
		}
	}
}
