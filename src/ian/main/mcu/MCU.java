package ian.main.mcu;

import java.awt.Color;
import java.io.IOException;
import java.util.Date;

import javax.xml.ws.WebServiceException;

import com.pi4j.io.gpio.exception.UnsupportedBoardType;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.sun.xml.internal.ws.Closeable;

import ian.main.LedAndOtherController;
import ian.main.MainStart;
import ian.main.capture.CaptureAdapter;
import ian.main.capture.OpenCameraFailedException;
import ian.main.serial.MwcSerialAdapter;
import ian.main.serial.exception.DataNotReadyException;
import ian.main.serial.exception.NoConnectedException;
import ian.main.serial.exception.TimeOutException;
import ian.main.serial.exception.UnknownErrorException;

public class MCU implements Closeable {
	
	public static final boolean isTest = false;
	
	/* 高度誤差範圍 */
	static final int altError = 20;
	
	static MwcSerialAdapter mwc;
	static LedAndOtherController loc;
	static CaptureAdapter ca;
	// static TempLed led;
	
	static MwcData info = MainStart.info;
	static MwcSetData setRc = MainStart.setRc;
	
	public static int step = 0;
	
	
	static int ledStep = 0;
	
	
	public static int setWantAlt = 0;

	
	static class ControlMode {
		static final int RELEASE = 0;
		static final int STOP = 1;
		static final int WORK = 2;
	}
	
	
	// ------------------throttle-------------------
	public static int throttleHoldValue;
	static int throttleValue = 0;
	// ------------------throttle-------------------
	
	
	// ------------------yaw------------------------
	static int yawMode = ControlMode.RELEASE;
	static int yawFixAngle = 0;
	// ------------------yaw------------------------
	
	
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
	static int armMode = ControlMode.RELEASE;
	static int baroMode = ControlMode.RELEASE;
	static int ledMode = ControlMode.RELEASE;
	// ------------------mode flag------------------
	
	static int choose(int index, int defaultData, int... data) {
		return index < data.length ? data[index] : defaultData;
	}
	public static class MsgIndex {
		public static class MsgStruct {
			public final int level;
			public final String msgStr;
			public MsgStruct(int level, String msgStr) {
				this.level = level;
				this.msgStr = msgStr;
			}
		}
		public static final MsgStruct STOP           = new MsgStruct(0, "等待");
		public static final MsgStruct RUN            = new MsgStruct(0, "動作中");
		
		public static final MsgStruct WAIT_MODE_1    = new MsgStruct(1, "等待 ok_to_arm...");
		public static final MsgStruct WAIT_MODE_2    = new MsgStruct(1, "等待 angle_mode...");
		public static final MsgStruct WAIT_MODE_3    = new MsgStruct(1, "等待 ok_to_arm 和 angle_mode...");
		
		public static final MsgStruct READY_FLY      = new MsgStruct(0, "準備起飛");
		public static final MsgStruct FORCE_FLY      = new MsgStruct(0, "起飛中");
		public static final MsgStruct WAIT_TO_BARO   = new MsgStruct(0, "等待至定高點");
		
		public static final MsgStruct FOLLOW_LINE    = new MsgStruct(0, "循線中");
		
		public static final MsgStruct LANDING        = new MsgStruct(0, "降落中");
		public static final MsgStruct LANDED         = new MsgStruct(0, "降落完成");
		
		
		public static final MsgStruct CAN_NOT_FLY    = new MsgStruct(2, "無法起飛");
		
		
	}
	
	
	static void stl() {
		switch (step) {
		case 0:
			step = 1;
			armMode = ControlMode.STOP;
			baroMode = ControlMode.STOP;
			
			MainStart.msgStruct = MsgIndex.RUN;
			break;
		case 1:
			switch ((info.ok_to_arm ? 1 : 0) + (info.angle_mode ? 2 : 0)) {
			case 3: MainStart.msgStruct = MsgIndex.RUN; step = 10; break;
			case 2: MainStart.msgStruct = MsgIndex.WAIT_MODE_1; break;
			case 1: MainStart.msgStruct = MsgIndex.WAIT_MODE_2; break;
			case 0: MainStart.msgStruct = MsgIndex.WAIT_MODE_3; break;
			default: throw new RuntimeException("unknown error.");
			}
			break;
		case 10: // 解鎖油門
			armMode = ControlMode.WORK;
			throttleValue = 1098;
			if (info.armed) {
				MainStart.msgStruct = MsgIndex.READY_FLY;
				createTimer();
				step = 11;				
			}
			break;
		case 11:
			if (timerOn(1500)) {
				step = 12;
				throttleValue = 1500;
				MainStart.msgStruct = MsgIndex.FORCE_FLY;
			}
			break;
		case 12: // 起飛
			if (info.altEstAlt < 10) {
				if (throttleValue < 1850) {
					throttleValue += 10;
				} else {
					step = 500;
				}
			} else if (info.altEstAlt > 20) {
				throttleHoldValue = throttleValue;
				createTimer();
				step = 1012;
			}
			break;
		case 1012:
			if (timerOn(100)) {
				step = 13;
				MainStart.msgStruct = MsgIndex.WAIT_TO_BARO;
			}
			break;
		case 13: // 設定高度200cm
			baroMode = ControlMode.WORK;
			if (info.baro_mode) {
				setWantAlt = 80;
				step = 14;
			}
			break;
		case 14: // 等待至200
			if (Math.abs(info.altEstAlt - setWantAlt) <= altError) {
				step = 15;
				MainStart.msgStruct = MsgIndex.FOLLOW_LINE;
			}
			break;
		case 15:
			createTimer();
			step = 16;
			break;
		case 16:
			if (info.extraRc[0] > 1700) {
				step = 100;
				yawMode = ControlMode.RELEASE;
			}
			yawMode = info.extraRc[1] > 1700 ? ControlMode.WORK : ControlMode.RELEASE;
			break;
		case 100: // 終點降落
			setWantAlt = 0;
			step = 101;
			MainStart.msgStruct = MsgIndex.LANDING;
			break;
		case 101:
			if (info.altEstAlt < 5) {
				step = 102;
			}
			break;
		case 102: // 上鎖油門
			throttleValue = 1098;
			armMode = ControlMode.STOP;
			baroMode = ControlMode.STOP;
			MainStart.msgStruct = MsgIndex.LANDED;
			break;
		case 500:
			throttleValue = 1098;
			armMode = ControlMode.STOP;
			baroMode = ControlMode.STOP;
			MainStart.msgStruct = MsgIndex.CAN_NOT_FLY;
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
		
		switch (yawMode) {
		case ControlMode.RELEASE:
			setRc.setYaw(0);
			break;
		case ControlMode.STOP:
			setRc.setYaw(1500);
			break;
		case ControlMode.WORK:
			int tmp = 1500 + yawFixAngle * 1;
			if (tmp > 1850) {
				tmp = 1850;
			}
			if (tmp < 1100) {
				tmp = 1100;
			}
			setRc.setYaw(tmp);
			break;
		default:
			setRc.setYaw(0);
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
			loc.setLed(0, Color.GREEN).setLed(1, Color.RED);
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
	
	
	static void extra() {
		yawFixAngle = MainStart.extraInfo[0] - info.att[2];
		
		if (yawFixAngle < -180) {
			yawFixAngle += 360;
		}
		if (yawFixAngle > 180) {
			yawFixAngle -= 360;
		}
		
		MainStart.debug0 = MainStart.extraInfo[0];
		MainStart.debug1 = info.att[2];
		
		MainStart.debug2 = yawFixAngle;
		
		MainStart.debug3 = yawMode;
	}
	
	
	public MCU setup() throws UnsupportedBoardType, IOException, InterruptedException, UnsupportedBusNumberException, OpenCameraFailedException {
		if (isTest) return this;
		ca = new CaptureAdapter().setup();
		mwc = new MwcSerialAdapter().open();
		loc = new LedAndOtherController().init();
		return this;
	}
	
	
	
	public boolean loop() {
		if (isTest) return true;
		
		
		ca.loop();
		
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
		
		
		
		
		extra();
		
		
		
		if (info.extraRc[2] < 1700) { // ems
			step = 0;
			
			throttleValue = 0;
			armMode = ControlMode.RELEASE;
			baroMode = ControlMode.RELEASE;
			ledMode = 0;
			
			MainStart.msgStruct = MsgIndex.STOP;
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
