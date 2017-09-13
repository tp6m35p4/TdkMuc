package ian.main;

import java.awt.Color;
import java.io.IOException;
import java.util.Date;

import com.pi4j.io.gpio.exception.UnsupportedBoardType;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import ian.main.serial.MwcSerialAdapter;

public class MainStart {
	/* 高度誤差範圍 */
	static final int altError = 0;
	/* stl test mode */
	static final boolean isStlTest = true;
	
	static MwcSerialAdapter mwc;
	static LedAndOtherController loc;
	
	static MwcData info = new MwcData();
	static MwcSetData setRc = new MwcSetData();
	
	static int step = 0;
	
	
	static int ledStep = 0;
	
	
	static int setWantAlt = 0;
	
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
	/*
	 * 1 : 1100
	 * 2 : 1800
	 * 3 : 1500
	 * other : 0
	 */
	static int throttleMode = 0;
	static int setWantAltMode = 0;
	static int ledMode = 0;
	// ------------------mode flag------------------
	
	static void stlTest() {

		
		switch (step) {
		case 0:
			step = 1;
			ledMode = 3;
			createTimer();
			break;
		case 1:
			if (timerOn(5000)) {
				step = 2;
			}
			break;
		case 2:
			step = 3;
			ledMode = 2;
			System.out.println("ok");
			break;
		}
	}
	
	static void stl() {
		switch (step) {
		case 0:
			step = 1;
			
			break;
		case 1:
			if (info.ok_to_arm && info.angle_mode) {
				step = 10;
			}
			break;
		case 10: // 解鎖油門
			armMode = 1;
			throttleMode = 1;
			if (info.armed) {
				createTimer();
				step = 11;				
			}
			break;
		case 11:
			if (timerOn(1500)) {
				step = 12;
			}
			break;
		case 12: // 起飛
			throttleMode = 2;
			if (info.altEstAlt > 5) {
				step = 13;
			}
			break;
		case 13: // 設定高度120cm
			throttleMode = 3;
			baroMode = 1;
			if (info.baro_mode) {
				setWantAltMode = 1;
				setWantAlt = 120;
				step = 14;
			}
			break;
		case 14: // 等待至120
			if (Math.abs(info.altEstAlt - 120) <= altError) {
				step = 15;
			}
			break;
		case 15:
			step = 100;
			break;
		case 100: // 終點降落
			setWantAltMode = 1;
			setWantAlt = 0;
			step = 101;
			break;
		case 101:
			if (info.altEstAlt < 5) {
				step = 102;
			}
			break;
		case 102: // 上鎖油門
			throttleMode = 1;
			armMode = 0;
			baroMode = 0;
			break;
		default:
			break;
		}
	}
	static void mode() {
		setRc.setAux1(armMode == 1  ? 1900 : 1100);
		setRc.setAux2(baroMode == 1 ? 1900 : 1100);
		
		
		
		switch (throttleMode) {
		case 1:
			setRc.setThrottle(1100);
			break;
		case 2:
			setRc.setThrottle(1500);
			break;
		case 3:
			setRc.setThrottle(1800);
			break;
		default:
			setRc.setThrottle(0);
			break;
		}
		
		
		CyzClass.mode();
		
		switch (ledMode) {
		case 1:
			loc.setAllLed(Color.BLACK);
			if (getTime() - ledStepTime > 200) {
				ledStepTime = getTime();
				loc.setLed(ledStep, Color.RED);
				if (++ledStep >= 2) {
					ledStep = 0;
				}
			}
			break;
		case 2:
			loc.setAllLed(Color.GREEN);
			break;
		case 3:
			loc.setLed(0, Color.RED).setLed(1, Color.GREEN);
			break;
		default:
			loc.setAllLed(Color.BLACK);
			break;
		}
		
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
	public static void run(String[] args) {
		try {
			mwc = new MwcSerialAdapter().open();
			loc = new LedAndOtherController().init();
		} catch (UnsupportedBoardType | IOException | InterruptedException | UnsupportedBusNumberException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		while (true) {
			setRc.reset();
			
			info.rc[7] = 1850;
//			try {
//				info.setData(mwc.getRpi());
//				mwcError = 0;
//			} catch(NoConnectedException | TimeOutException | DataNotReadyException | UnknownErrorException | IOException e) {
//				if (mwcError == 0) {
//					mwcErrorTime = getTime();
//				}
//				mwcError++;
//				e.printStackTrace();
//			}
			
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
			
			
			
			
			if (info.rc[7] < 1700) { // ems
				step = 0;
				
				throttleMode = 0;
				armMode = 0;
				baroMode = 0;
				setWantAltMode = 0;
				ledMode = 0;
			} else {
				try {
					if (isStlTest) {
						stlTest();
					} else {
						stl();
					}
					mode();
					modeError = 0;
				} catch(Exception e) {
					modeError++;
					e.printStackTrace();
				}
				
			}
			
//			try {
//				if (mwcError == 0) {
//					mwc.setRc(setRc.getData());
//					if (setWantAltMode == 1) {
//						mwc.setAltHold(setWantAlt);
//						setWantAltMode = 0;
//					}					
//				}
//			} catch (DataNotReadyException | NoConnectedException | TimeOutException | UnknownErrorException | IOException e) {
//				e.printStackTrace();
//			}
			
			if (ledError != 0 || modeError != 0 || (mwcError != 0 && (getTime() - mwcErrorTime > 800))) {
				System.out.print("ledError = ");
				System.out.println(ledError);
				System.out.print("modeError = ");
				System.out.println(modeError);
				System.out.print("mwcError = ");
				System.out.println(mwcError);
				
				break;
			}
			try {
				loc.updateLed();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
		try {
			mwc.close();
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		run(args);
		// test(args);
	}
	public static void test(String[] args) {
		
	}
}
