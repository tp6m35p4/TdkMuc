package ian.main.mcu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MwcData {
	public static final int DATA_LEN = 57;
	public static final int OTHER_DATA_LEN = 12;
	
	
	public short[] motor = new short[8];
    public short[] rc = new short[8];
    public short[] servo = new short[8];
    public short[] att = new short[3];
    public int altEstAlt;
    public short altVario;
    public int altHold;
    public boolean ok_to_arm;
    public boolean angle_mode;
    public boolean armed;
    public boolean baro_mode;
    public short[] debug = new short[4];
    
    public short sonarFront;
    public short sonarLeft;
    public short sonarRight;
    public short[] extraRc = new short[3];
    
    
    public MwcData setData(byte[] data) {
    	if (data.length != DATA_LEN) {
    		throw new RuntimeException("DATA_LEN = " + String.valueOf(DATA_LEN) + " , data.length = " + String.valueOf(data.length));
    	}
    	ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < motor.length; i++) {
            motor[i] = byteBuffer.getShort();
        }
        for (int i = 0; i < rc.length; i++) {
            rc[i] = byteBuffer.getShort();
        }
        for (int i = 0; i < att.length; i++) {
            att[i] = byteBuffer.getShort();
        }
        altEstAlt = byteBuffer.getInt();
        altVario = byteBuffer.getShort();
        altHold = byteBuffer.getInt();
        for (int i = 0; i < 4; i++) {
        	debug[i] = byteBuffer.getShort();
        }
        
        byte tmp = byteBuffer.get();
        ok_to_arm  = (tmp & (1 << 0)) != 0;
        angle_mode = (tmp & (1 << 1)) != 0;
        armed      = (tmp & (1 << 2)) != 0;
        baro_mode  = (tmp & (1 << 3)) != 0;
        
        
        return this;
    }
    
    public byte[] getData() {
    	ByteBuffer byteBuffer = ByteBuffer.allocate(DATA_LEN).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < motor.length; i++) {
        	byteBuffer.putShort(motor[i]);
        }
    	for (int i = 0; i < rc.length; i++) {
    		byteBuffer.putShort(rc[i]);
    	}
    	for (int i = 0; i < att.length; i++) {
    		byteBuffer.putShort(att[i]);
    	}
    	byteBuffer.putInt(altEstAlt);
    	byteBuffer.putShort(altVario);
    	byteBuffer.putInt(altHold);
    	for (int i = 0; i < 4; i++) {
        	byteBuffer.putShort(debug[i]);
        }
    	
    	byte tmp = 0;
    	if (ok_to_arm ) tmp |= (1 << 0);
    	if (angle_mode) tmp |= (1 << 1);
    	if (armed     ) tmp |= (1 << 2);
    	if (baro_mode ) tmp |= (1 << 3);
    	byteBuffer.put(tmp);
    	
    	return byteBuffer.array();
    }
    
    public MwcData setOtherData(byte[] data) {
    	if (data.length != OTHER_DATA_LEN) {
    		throw new RuntimeException("OTHER_DATA_LEN = " + String.valueOf(OTHER_DATA_LEN) + " , data.length = " + String.valueOf(data.length));
    	}
    	ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    	sonarFront = byteBuffer.getShort();
    	sonarLeft = byteBuffer.getShort();
    	sonarRight = byteBuffer.getShort();
    	for (int i = 0; i < 3; i++) {
    		extraRc[i] = byteBuffer.getShort();
    	}
    	return this;
    }
    
    public byte[] getOtherData() {
    	ByteBuffer byteBuffer = ByteBuffer.allocate(OTHER_DATA_LEN).order(ByteOrder.LITTLE_ENDIAN);
    	
    	byteBuffer.putShort(sonarFront);
    	byteBuffer.putShort(sonarLeft);
    	byteBuffer.putShort(sonarRight);
    	for (int i = 0; i < 3; i++) {
    		byteBuffer.putShort(extraRc[i]);
    	}
    	
    	
    	return byteBuffer.array();
    }
}
