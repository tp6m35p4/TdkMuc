package ian.main.mcu;

import ian.main.MainStart;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;


public class CyzClass {
	
	static MwcData info = MainStart.info;
	static MwcSetData setRc = MainStart.setRc;
	
	private static Mat cap() {
		
		Core.setErrorVerbosity(false);
        VideoCapture camera = new VideoCapture(0);
        camera.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, 500);
        camera.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, 500);
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if(!camera.isOpened()){
            System.out.println("Camera Error");
        }
        else {
            System.out.println("Camera OK");
        }
 
        Mat frame = new Mat();
        camera.read(frame);
		
		
		return frame;
	}
	
	public static void mode() {
		
		Mat f = cap();
		Mat grayImage = new Mat();
		Mat detectedEdges = new Mat();
		Imgproc.cvtColor(f, grayImage, Imgproc.COLOR_BGR2GRAY);
		Imgproc.blur(grayImage, detectedEdges, new Size(3, 3));
		
	}
}
