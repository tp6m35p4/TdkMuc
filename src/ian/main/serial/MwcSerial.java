package ian.main.serial;

import java.io.IOException;
import java.util.Date;

import com.pi4j.io.gpio.exception.UnsupportedBoardType;
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;

import ian.main.serial.exception.DataNotReadyException;
import ian.main.serial.exception.NoConnectedException;
import ian.main.serial.exception.TimeOutException;
import ian.main.serial.exception.UnknownErrorException;

public class MwcSerial {
	
	private final Serial serial = SerialFactory.createInstance();
	private final Queue q = new Queue();
	
	public MwcSerial() {
		
	}
	
	
	private static final long TIME_OUT = 100;
    private byte[] getData(boolean isBin) throws IOException, TimeOutException, DataNotReadyException, UnknownErrorException {
        long timeSpan = new Date().getTime();
        long delay = 0;

        while (delay < TIME_OUT && tryToGetData() != 0) {
            delay = new Date().getTime() - timeSpan;
        }
        if (delay >= TIME_OUT) {
            throw new TimeOutException(tryToGetData());
        }
        return readByteArray(isBin);
    }

    public byte[] getData(byte cmd) throws IOException, NoConnectedException, TimeOutException, DataNotReadyException, UnknownErrorException {
        sendByteArray(cmd);
        return getData(false);
    }

    public void setData(byte cmd, byte[] data) throws IOException, NoConnectedException, TimeOutException, DataNotReadyException, UnknownErrorException {
        sendByteArray(cmd, data);
        getData(false);
    }
	
	public void open() throws UnsupportedBoardType, IOException, InterruptedException {
		q.clear();
		
		SerialConfig config = new SerialConfig()
        		.device("/dev/ttyS0")
        		.baud(Baud._115200)
        		.dataBits(DataBits._8)
        		.parity(Parity.NONE)
        		.stopBits(StopBits._1)
        		.flowControl(FlowControl.NONE);
		serial.open(config);
	}
	public void close() throws IllegalStateException, IOException {
		serial.close();
		q.clear();
	}
	
	private void sendByteArray(byte cmd) throws IOException, NoConnectedException {
        sendByteArray(cmd, null);
    }

	private void sendByteArray(byte cmd, byte[] data) throws NoConnectedException, IOException {
        if (!serial.isOpen()) {
            throw new NoConnectedException();
        }
        byte crc = cmd;
        if (data != null) {
            crc ^= (byte)data.length;
            for (byte i : data) {
                crc ^= i;
            }
        }



        serial.write(new byte[]{'$', 'M', '<'});
        serial.write(data != null ? (byte) data.length : 0);
        serial.write(cmd);
        if (data != null) {
        	serial.write(data);
        }
        serial.write(crc);
        serial.flush();
//        Log.i("sendByteArray", "----------------------------");
//        Log.i("sendByteArray", "cmd = " + String.valueOf(cmd) + " , crc = " + String.valueOf(crc));
//        if (data != null) {
//            Log.i("sendByteArray", "len = " + String.valueOf(data.length));
//            Log.i("sendByteArray", Arrays.toString(data));
//        }
//        Log.i("sendByteArray", "----------------------------");

    }

    @SuppressWarnings("unused")
    private byte[] readByteArray(boolean isBin) throws DataNotReadyException, UnknownErrorException {
        if (q.checkData() >= 10) {
            throw new DataNotReadyException();
        }
        byte[] bfr;
        if (isBin) {
        	bfr = new byte[q.length()];
    		if (q.pop(bfr, bfr.length) != bfr.length) {
    			throw new UnknownErrorException();
    		}
        } else {
        	q.skip(3);
            bfr = new byte[q.pop()];
            byte cmd = q.pop();
            if (q.pop(bfr, bfr.length) != bfr.length) {
                throw new UnknownErrorException();
            }
            byte crc = q.pop();
//            Log.i("readByteArray", "----------------------------");
//            Log.i("readByteArray", "cmd = " + String.valueOf(cmd) + " , crc = " + String.valueOf(crc));
//            Log.i("readByteArray", "len = " + String.valueOf(bfr.length));
//            Log.i("readByteArray", Arrays.toString(bfr));
//            Log.i("readByteArray", "----------------------------");
        }
        

        return bfr;
    }

    private int tryToGetData() throws IOException {
        while (!serial.isClosed() &&serial.available() > 0 && !q.isFull()) {
        	// System.out.println(serial.available());
            q.push(serial.read());
        }
        return q.checkData();
    }

    
	
	
    public static class Cmd {
        public static final byte MSP_PRIVATE           = 1        ;  //in+out message      to be used for a generic framework : MSP + function code (LIST/GET/SET) + data. no code yet

        public static final byte MSP_IAN               = 50       ;  //out message
        public static final byte MSP_RPI               = 51       ;  //out message
        public static final byte MSP_SET_ALT_HOLD      = 52       ;  //out message
        
        public static final byte MSP_IDENT             = 100      ;  //out message         multitype + multiwii version + protocol version + capability variable
        public static final byte MSP_STATUS            = 101      ;  //out message         cycletime & errors_count & sensor present & box activation & current setting number
        public static final byte MSP_RAW_IMU           = 102      ;  //out message         9 DOF
        public static final byte MSP_SERVO             = 103      ;  //out message         8 servos
        public static final byte MSP_MOTOR             = 104      ;  //out message         8 motors
        public static final byte MSP_RC                = 105      ;  //out message         8 rc chan and more
        public static final byte MSP_RAW_GPS           = 106      ;  //out message         fix, numsat, lat, lon, alt, speed, ground course
        public static final byte MSP_COMP_GPS          = 107      ;  //out message         distance home, direction home
        public static final byte MSP_ATTITUDE          = 108      ;  //out message         2 angles 1 heading
        public static final byte MSP_ALTITUDE          = 109      ;  //out message         altitude, variometer
        public static final byte MSP_ANALOG            = 110      ;  //out message         vbat, powermetersum, rssi if available on RX
        public static final byte MSP_RC_TUNING         = 111      ;  //out message         rc rate, rc expo, rollpitch rate, yaw rate, dyn throttle PID
        public static final byte MSP_PID               = 112      ;  //out message         P I D coeff (9 are used currently)
        public static final byte MSP_BOX               = 113      ;  //out message         BOX setup (number is dependant of your setup)
        public static final byte MSP_MISC              = 114      ;  //out message         powermeter trig
        public static final byte MSP_MOTOR_PINS        = 115      ;  //out message         which pins are in use for motors & servos, for GUI
        public static final byte MSP_BOXNAMES          = 116      ;  //out message         the aux switch names
        public static final byte MSP_PIDNAMES          = 117      ;  //out message         the PID names
        public static final byte MSP_WP                = 118      ;  //out message         get a WP, WP# is in the payload, returns (WP#, lat, lon, alt, flags) WP#0-home, WP#16-poshold
        public static final byte MSP_BOXIDS            = 119      ;  //out message         get the permanent IDs associated to BOXes
        public static final byte MSP_SERVO_CONF        = 120      ;  //out message         Servo settings
        public static final byte MSP_NAV_STATUS        = 121      ;  //out message         Returns navigation status
        public static final byte MSP_NAV_CONFIG        = 122      ;  //out message         Returns navigation parameters
        public static final byte MSP_CELLS             = 130 - 256;  //out message         FRSKY Battery Cell Voltages
        public static final byte MSP_SET_RAW_RC        = 200 - 256;  //in message          8 rc chan
        public static final byte MSP_SET_RAW_GPS       = 201 - 256;  //in message          fix, numsat, lat, lon, alt, speed
        public static final byte MSP_SET_PID           = 202 - 256;  //in message          P I D coeff (9 are used currently)
        public static final byte MSP_SET_BOX           = 203 - 256;  //in message          BOX setup (number is dependant of your setup)
        public static final byte MSP_SET_RC_TUNING     = 204 - 256;  //in message          rc rate, rc expo, rollpitch rate, yaw rate, dyn throttle PID
        public static final byte MSP_ACC_CALIBRATION   = 205 - 256;  //in message          no param
        public static final byte MSP_MAG_CALIBRATION   = 206 - 256;  //in message          no param
        public static final byte MSP_SET_MISC          = 207 - 256;  //in message          powermeter trig + 8 free for future use
        public static final byte MSP_RESET_CONF        = 208 - 256;  //in message          no param
        public static final byte MSP_SET_WP            = 209 - 256;  //in message          sets a given WP (WP#,lat, lon, alt, flags)
        public static final byte MSP_SELECT_SETTING    = 210 - 256;  //in message          Select Setting Number (0-2)
        public static final byte MSP_SET_HEAD          = 211 - 256;  //in message          define a new heading hold direction
        public static final byte MSP_SET_SERVO_CONF    = 212 - 256;  //in message          Servo settings
        public static final byte MSP_SET_MOTOR         = 214 - 256;  //in message          PropBalance function
        public static final byte MSP_SET_NAV_CONFIG    = 215 - 256;  //in message          Sets nav config parameters - write to the eeprom
        public static final byte MSP_SET_ACC_TRIM      = 239 - 256;  //in message          set acc angle trim values
        public static final byte MSP_ACC_TRIM          = 240 - 256;  //out message         get acc angle trim values
        public static final byte MSP_BIND              = 241 - 256;  //in message          no param
        public static final byte MSP_EEPROM_WRITE      = 250 - 256;  //in message          no param
        public static final byte MSP_DEBUGMSG          = 253 - 256;  //out message         debug string buffer
        public static final byte MSP_DEBUG             = 254 - 256;  //out message         debug1,debug2,debug3,debug4
    }
}
