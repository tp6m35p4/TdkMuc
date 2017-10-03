package ian.main.capture;

import org.opencv.core.Mat;

import ian.main.MainStart;

public class CaptureCalculator {
	public static void cal(Mat f) {
		int status = 0;
		int deltaX = 0;
		int deltaY = 0;
		double angle = 0;
		
		
		
		
		
		
		
		
		storeInfo((byte)status, (short)deltaX, (short)deltaY, angle);
	}
	
	public static void storeInfo(byte status, Short deltaX, Short deltaY, double angle) {
		MainStart.info.captureStatus = status;
		MainStart.info.captureDeltaX = deltaX;
		MainStart.info.captureDeltaY = deltaY;
		
		
		
		// angle range : [-180.00,180.00]
		if (angle < 0) {
			angle += 180;
		}
		// angle range : [0.00,180.00]
		angle -= 90;
		// angle range : [-90.00,90.00]
		angle *= 100;
		// angle range : [-9000,9000]
		
		short angle2 = (short) angle;
		int delta = angle2 - MainStart.info.captureAngle;
		// delta range : [-36000,36000]
		if (delta < -18000) {
			delta += 36000;
		}
		if (delta > 18000) {
			delta -= 36000;
		}
		// delta range : [-18000,18000]
		if (Math.abs(delta) > 9000) {
			if (angle2 < 0) {
				angle2 += 18000;
			}
			if (angle2 > 0) {
				angle2 -= 18000;
			}
		}
		MainStart.info.captureAngle = angle2;
	}
}
