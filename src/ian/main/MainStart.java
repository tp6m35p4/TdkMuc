package ian.main;

import java.io.IOException;
import java.util.Date;

import javax.xml.ws.WebServiceException;

import com.pi4j.io.gpio.exception.UnsupportedBoardType;

import ian.main.serial.MwcSerialAdapter;
import ian.main.serial.exception.DataNotReadyException;
import ian.main.serial.exception.NoConnectedException;
import ian.main.serial.exception.TimeOutException;
import ian.main.serial.exception.UnknownErrorException;

public class MainStart {
	static MwcData info = new MwcData();
	static MwcSetData setRc = new MwcSetData();
	
	static int step = 0;
	static int wantFront = 0;
	
	static boolean armMode = false;
	static boolean baroMode = false;
	
	
	/*
	 * 1 : 1100
	 * 2 : 1800
	 * 3 : 1500
	 * other : 0
	 */
	static int throttleMode = 0;
	
	
	
	/* 高度誤差範圍 */
	static final int altError = 0;
	
	static void stl(MwcSerialAdapter mwc) throws NoConnectedException, TimeOutException, DataNotReadyException, UnknownErrorException, IOException {
		switch (step) {
		case 0:
			step = 1;
			
			throttleMode = 0;
			
			armMode = false;
			baroMode = false;
			
			
			break;
		case 1:
			if (info.ok_to_arm && info.angle_mode) {
				step = 10;
			}
			break;
		case 10: // 解鎖油門
			armMode = true;
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
			baroMode = true;
			if (info.baro_mode) {
				mwc.setAltHold(120);
				step = 14;
			}
			break;
		case 14: // 等待至120
			if (Math.abs(info.altEstAlt - 120) < altError) {
				step = 15;
			}
			break;
		case 15:
			step = 100;
			break;
		case 100: // 終點降落
			mwc.setAltHold(0);
			if (info.altEstAlt < 5) {
				step = 101;
			}
			break;
		case 101: // 上鎖油門
			throttleMode = 1;
			armMode = false;
			baroMode = false;
			break;
		default:
			break;
		}
	}
	static void mode() {
		setRc.setAux1(armMode  ? 1900 : 1100);
		setRc.setAux2(baroMode ? 1900 : 1100);
		
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
	public static void main(String[] args) {
		
		
		try(MwcSerialAdapter mwc = new MwcSerialAdapter().open()) {
			while (true) {
				setRc.reset();
				info.setData(mwc.getRpi());
				
				
				if (info.rc[7] < 1700) { // ems
					step = 0;
				} else {
					stl(mwc);
					mode();
				}
				
				mwc.setRc(setRc.getData());
			}
			
		} catch (WebServiceException | UnsupportedBoardType | IOException | InterruptedException | IllegalStateException | UnknownErrorException | DataNotReadyException | TimeOutException | NoConnectedException e) {
			e.printStackTrace();
		} finally {
			
		}
	}
}
