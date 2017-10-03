package ian.main.capture;

import java.io.IOException;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import ian.main.MainStart;

public class CaptureAdapter {
	
	public static final boolean isSkip = true;
	private VideoCapture camera;
	private Mat f;
	static {
		if (!isSkip) {
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		}
	}
	
	
	private static void print(String info) {
		MainStart.print("CaptureAdapter", info);
	}
	
	public CaptureAdapter setup() throws IOException {
		if (isSkip) return this;
		f = new Mat();
		camera = new VideoCapture(0);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		camera.open(0);
		camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, 500);
		System.out.println(camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, 500));
        return this;
	}
	public void loop() throws IOException {
		if (isSkip) return;
		camera.read(f);
		CaptureCalculator.cal(f);
		
	}

	public void close() throws IOException {
		if (isSkip) return;
		camera.release();
		print("Closed.");
	}
}
