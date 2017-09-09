package ian.main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MwcData {
	public short[] motor = new short[8];
    public short[] rc = new short[8];
    public short[] servo = new short[8];
    public short[] att = new short[3];
    public int altEstAlt;
    public short altVario;
    public boolean ok_to_arm;
    public boolean angle_mode;
    public boolean armed;
    public boolean baro_mode;
    
    public int sonarFront;
    public int sonarLeft;
    public int sonarRight;
    
    
    public MwcData setData(byte[] data) {
    	ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < rc.length; i++) {
            rc[i] = byteBuffer.getShort();
        }
        for (int i = 0; i < att.length; i++) {
            att[i] = byteBuffer.getShort();
        }
        altEstAlt = byteBuffer.getInt();
        altVario = byteBuffer.getShort();
        
        byte tmp = byteBuffer.get();
        ok_to_arm  = (tmp & (1 << 0)) != 0;
        angle_mode = (tmp & (1 << 1)) != 0;
        armed      = (tmp & (1 << 2)) != 0;
        baro_mode  = (tmp & (1 << 3)) != 0;
        return this;
    }
    
    public MwcData setOtherData(byte[] data) {
    	
    	return this;
    }
}
