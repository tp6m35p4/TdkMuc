package ian.main;

public class MwcSetData {
	private short roll,pitch,yaw,throttle,aux1,aux2,aux3,aux4;
	public void reset() {
		roll = 0;
		pitch = 0;
		yaw = 0;
		throttle = 0;
		aux1 = 0;
		aux2 = 0;
		aux3 = 0;
		aux4 = 0;
	}
	public short[] getData() {
		return new short[]{roll,pitch,yaw,throttle,aux1,aux2,aux3,aux4};
	}
	public void setData(short[] data) {
		roll = data[0];
		pitch = data[1];
		yaw = data[2];
		throttle = data[3];
		aux1 = data[4];
		aux2 = data[5];
		aux3 = data[6];
		aux4 = data[7];
	}
	public short getRoll() {
		return roll;
	}
	public void setRoll(int roll) {
		this.roll = (short) roll;
	}
	public short getPitch() {
		return pitch;
	}
	public void setPitch(int pitch) {
		this.pitch = (short) pitch;
	}
	public short getYaw() {
		return yaw;
	}
	public void setYaw(int yaw) {
		this.yaw = (short) yaw;
	}
	public short getThrottle() {
		return throttle;
	}
	public void setThrottle(int throttle) {
		this.throttle = (short) throttle;
	}
	public short getAux1() {
		return aux1;
	}
	public void setAux1(int aux1) {
		this.aux1 = (short) aux1;
	}
	public short getAux2() {
		return aux2;
	}
	public void setAux2(int aux2) {
		this.aux2 = (short) aux2;
	}
	public short getAux3() {
		return aux3;
	}
	public void setAux3(int aux3) {
		this.aux3 = (short) aux3;
	}
	public short getAux4() {
		return aux4;
	}
	public void setAux4(int aux4) {
		this.aux4 = (short) aux4;
	}
	
}
