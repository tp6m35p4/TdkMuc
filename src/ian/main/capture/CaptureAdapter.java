package ian.main.capture;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class CaptureAdapter {
	private VideoCapture camera;
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	public CaptureAdapter setup() throws OpenCameraFailedException {
		
		// Core.setErrorVerbosity(false);
        camera = new VideoCapture();
        camera.open(0);
        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, 500);
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, 500);
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        if(!camera.isOpened()){
            throw new OpenCameraFailedException();
        }
		return this;
	}
	public void loop() {
		Mat frame = new Mat();
        camera.read(frame);
        
	}
}
