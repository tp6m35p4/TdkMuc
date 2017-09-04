package ian.main.serial;

public class Queue {
    private static final int BUFFER_LEN = 1024;

    private byte buffer[];
    private int sIndex, eIndex;

    {
        buffer = new byte[BUFFER_LEN];
        sIndex = 0;
        eIndex = 0;
    }

    int length() {
        return eIndex > sIndex ? sIndex - eIndex + BUFFER_LEN : sIndex - eIndex;
    }

    void skip(int count) {
        if (length() < count) {
            throw new ArrayIndexOutOfBoundsException();
        }
        eIndex += count;
        fixIndex();
    }

    void clear() {
        sIndex = 0;
        eIndex = 0;
    }

    boolean isFull() {
        return length() >= BUFFER_LEN - 1;
    }

    void push(byte data) {
        if (isFull()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        buffer[sIndex++] = data;
        fixIndex();
    }
    void push(byte bfr[]) {
    	for (byte each : bfr) {
    		push(each);
    	}
    }

    byte pop() {
        if (length() == 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte output = buffer[eIndex++];
        fixIndex();
        return output;
    }
    int pop(byte bfr[], int length) {
        int output = Math.min(length(), length);
        for (int i = 0; i < output; i++) {
            bfr[i] = pop();
        }
        return output;
    }

    byte get(int index) {
        if (index >= length()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        index += eIndex;
        index = fixIndex(index);
        return buffer[index];
    }
    
    int checkData() {
        if (length() == 0) {
            return 99;
        }
        if (length() > 0 && get(0) != '$') {
            skip(1);
            return 10;
        }
        if (length() > 1 && get(1) != 'M') {
            skip(1);
            return 11;
        }
        if (length() > 2 && get(2) != '>') {
            skip(1);
            return 12;
        }

        if (length() < 4) {
            return 13;
        }
        byte len = get(3);
        if (length() < len + 6) {
            return 14;
        }
        byte crc = len;
        for (byte i = 4; i < len + 5; i++) {
            crc ^= get(i);
        }
        if (get(len + 5) != crc) {
            // Log.w("checkData_CRC", "cal : " + String.valueOf(crc) + " , get : " + String.valueOf(q.get(len + 5)));
        	clear();
            return 15;
        }
        return 0;

    }

    private void fixIndex() {
        sIndex = fixIndex(sIndex);
        eIndex = fixIndex(eIndex);
    }

    private int fixIndex(int index) {
        while (index >= BUFFER_LEN) {
            index -= BUFFER_LEN;
        }
        while (index < 0) {
            index += BUFFER_LEN;
        }
        return index;
    }

}