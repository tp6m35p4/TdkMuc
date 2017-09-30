package ian.main.surveillance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.xml.ws.WebServiceException;

import com.sun.xml.internal.ws.Closeable;

import ian.main.MainStart;
import ian.main.mcu.MCU;

public class SurveillanceController implements Closeable {
	public static final int LISTEN_PORT = 5987;
	
    private ServerSocket server;
    private Socket socket;
    private Thread thread;
    
    
	public SurveillanceController start() throws IOException {
		server = new ServerSocket(LISTEN_PORT);
		thread = new Thread(new Th());
		thread.start();
		return this;
	}
	
	@Override
	public void close() throws WebServiceException {
		System.out.println("[SurveillanceController]: Stop listen...");
		try {
			if (socket != null) socket.close();
			server.close();
		} catch (IOException e) {
			throw new WebServiceException(e);
		}
	}
	
	
	private class Th implements Runnable {
		private InputStream is;
		private OutputStream os;
		
		@Override
		public void run() {
			byte[] cmd = new byte[1];
			
			try {
				while (true) {
					System.out.println("[SurveillanceController]: Listening at port " + String.valueOf(LISTEN_PORT) + " ...");
					socket = server.accept();
					System.out.println("[SurveillanceController]: Connected...");
					is = socket.getInputStream();
					os = socket.getOutputStream();
					
					while (!socket.isClosed()) {
						int tmpLen = is.read(cmd);
						if (tmpLen == -1) {
							break;
						} else if (tmpLen == 0) {
							continue;
						}
						processCmd(cmd[0]);
					}
					is.close();
					os.close();
					socket.close();
					System.out.println("[SurveillanceController]: Disconnected...");
				}
			} catch (SocketException e) {
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		private void processCmd(byte cmd) throws IOException {
			byte[] data;
			switch (cmd) {
			case Cmd.CMD_GET_INFO:
				data = MainStart.info.getData();
				break;
			case Cmd.CMD_GET_OTHER_INFO:
				data = MainStart.info.getOtherData();
				break;
			case Cmd.CMD_GET_PIC:
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput oo = new ObjectOutputStream(bos);
				oo.writeObject(MainStart.f);
				oo.flush();
				data = bos.toByteArray();
				oo.close();
				bos.close();
				break;
			case Cmd.CMD_GET_RPI_INFO:
				ByteBuffer buffer = ByteBuffer.allocate(45).order(ByteOrder.LITTLE_ENDIAN);
				buffer.putInt(MCU.step).putInt(MCU.setWantAlt);
				buffer.putInt(MainStart.cycleTime).put((byte)MainStart.msgStruct.level);
				buffer.putInt(MainStart.debug0).putInt(MainStart.debug1);
				buffer.putInt(MainStart.debug2).putInt(MainStart.debug3);
				buffer.putInt(MainStart.debug4).putInt(MainStart.debug5);
				buffer.putInt(MainStart.debug6).putInt(MainStart.debug7);
				
				
				data = buffer.array();
				break;
			case Cmd.CMD_GET_MSG:
				data = MainStart.msgStruct.msgStr.getBytes();
				break;
			case Cmd.CMD_SET_STATUS:
				data = new byte[]{0};
				int len = is.read();
				byte[] allData = new byte[len];
				while (is.available() < len);
				is.read(allData);
				ByteBuffer byteBuffer = ByteBuffer.wrap(allData).order(ByteOrder.LITTLE_ENDIAN);
				for (int i = 0; i < MainStart.extraInfo.length; i++) {
					MainStart.extraInfo[i] = byteBuffer.get();
				}
				// MainStart.ems = byteBuffer.get();
				break;
			default:
				data = new byte[]{0};
				break;
			}
			os.write(data.length);
			os.write(data);
			os.flush();
//			for (int i = 0; i < data.length; i++) {
//				System.out.printf("%d, ", data[i]);
//			}
//			System.out.println();
		}
		
	}




	
}
