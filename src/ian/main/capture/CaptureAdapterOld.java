package ian.main.capture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import ian.main.MainStart;

public class CaptureAdapterOld {
	
	public static final boolean isSkip = true;
	public static final boolean isPyDebugPrint = false;
	
	
	public static final boolean AUTO_RUN_PY = false;
	
	public static final String PYTHON_CMD = "python ./p/openimage.py";
	public static final int LISTEN_PORT = 6666;
	
	public static final byte CMD_GET_DATA = 110;
	public static final byte CMD_GET_EXTRA_DATA = 111;
	
	public static final int TIME_OUT = 10000;
	
	
	private ServerSocket server;
    private Socket socket;
    private InputStream is;
	private OutputStream os;
	
	private BufferedReader read;
	private Process proc;
	
	private static void print(String info) {
		MainStart.print("CaptureAdapter", info);
	}
	
	public CaptureAdapterOld setup() throws IOException {
		if (isSkip) return this;
		print("Listening at port " + String.valueOf(LISTEN_PORT) + " ...");
		server = new ServerSocket(LISTEN_PORT);
		if (AUTO_RUN_PY) {
			print("Starting python...");
			proc = Runtime.getRuntime().exec(PYTHON_CMD);
	        read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		}
		
        print("Waiting python.");
        socket = server.accept();
        server.close();
        is = socket.getInputStream();
        os = socket.getOutputStream();
        socket.setSoTimeout(TIME_OUT);
        print("Connected python.");
        return this;
	}
	public void loop() throws IOException {
		if (isSkip) return;
		
		if (AUTO_RUN_PY) {
			try {
				while (read.ready()) {
				    print("[py]: " + read.readLine());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (!proc.isAlive()) {
				throw new IOException("python is exit");
			}
		}
		
		
		MainStart.info.setCaptureData(cmd(CMD_GET_DATA));
		
		MainStart.captureExtraInfo = cmd();
	}
	
	private byte[] cmd(byte cmd) throws IOException {
		if (isPyDebugPrint) print("send...");
		os.write(cmd);
		os.flush();
		return cmd();
	}
	
	private byte[] cmd() throws IOException {
		byte[] buffer = new byte[4];
		byte[] data;
		int index = 0;
		int tmp;
		if (isPyDebugPrint) print("recive...");
		while (true) {
			tmp = is.read(buffer, index, 1);
			// System.out.printf("%d, %d\n",tmp,is.available());
			if (isPyDebugPrint) print("catch len " + String.valueOf(index));
			if (tmp == 0) continue;
			else if (tmp == -1) throw new IOException("socket closed.");
			if (++index < buffer.length) continue;
			
			int len = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
			
			index = 0;
			if (isPyDebugPrint) print("catch len = " + String.valueOf(len));
			data = new byte[len];
			
			while (index < len) {
				tmp = is.read(data, index, data.length - index);
				if (tmp == -1) throw new IOException("socket closed.");
				index += tmp;
				if (isPyDebugPrint) print("catch " + String.valueOf(index));
			}
			if (isPyDebugPrint) print("catch all data");
			
			break;
		}
		if (isPyDebugPrint) print(Arrays.toString(data));
		return data;
	}

	public void close() throws IOException {
		if (isSkip) return;
		if (!socket.isClosed()) {
			os.write('q');
			os.flush();
		}
		if (is != null) is.close();
		if (os != null) os.close();
		if (socket != null) socket.close();
		print("Disconnected python.");
		if (AUTO_RUN_PY) {
			print("Wait pyth to exit.");
			while (proc.isAlive());
			proc.destroy();
		}
		
		print("Closed.");
	}
}
