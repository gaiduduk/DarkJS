package com.droid.djs.node;

import com.droid.gdb.Bytes;
import com.droid.gdb.DiskManager;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class DataInputStream extends InputStream {

    // TODO setString buffer size > MAX_STORAGE_DATA_IN_DB
    private static final int BUFFER_SIZE = NodeStorage.MAX_STORAGE_DATA_IN_DB;
    private byte type;
    public long start;
    public long length;
    private long currentPosition;
    private NodeStorage storage = NodeStorage.getInstance();
    private FileReader fileReader;

    public DataInputStream(byte type, long start, long length) {
        this.type = type;
        this.start = start;
        this.length = length;
        currentPosition = 0;
    }

    public boolean hasNext() {
        boolean nextExist = currentPosition < length;
        if (!nextExist) currentPosition = 0;
        return nextExist;
    }

    private byte[] readFromDb() {
        byte[] data = storage.getData(start, currentPosition, (int) Math.min(BUFFER_SIZE, length));
        currentPosition += data.length;
        return data;
    }

    private char[] readFromFs() {
        try {
            if (fileReader == null)
                fileReader = new FileReader(DiskManager.getInstance().getFileById(start));
            char[] buf = new char[BUFFER_SIZE];
            int readiedChars = fileReader.read(buf);
            if ((readiedChars) > 0) {
                if (readiedChars < BUFFER_SIZE)
                    buf = Arrays.copyOf(buf, readiedChars); // removeSector zero bytes
                currentPosition += readiedChars;
                return buf;
            }
            if (currentPosition == length) {
                fileReader.close();
                fileReader = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String readString() {
        StringBuilder stringBuilder = new StringBuilder();
        while (hasNext()) {
            char[] buffer;
            if (length < NodeStorage.MAX_STORAGE_DATA_IN_DB)
                buffer = Bytes.toCharArray(readFromDb());
            else
                buffer = readFromFs();
            if (buffer != null)
                stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    public char[] readChars() {
        return readString().toCharArray();
    }

    public Object getObject() {
        String string = readString();
        switch (type) {
            case NodeType.BOOL:
                return Boolean.valueOf(string);
            case NodeType.NUMBER:
                return Double.valueOf(string);
            default:
                return string;
        }
    }

    @Override
    public int read() throws IOException {
        // TODO !!!! rewrite
        long oldPosition = currentPosition;
        int _char = readChars()[(int) oldPosition];
        currentPosition = oldPosition + 1;
        return _char;
    }

    @Override
    public int read(byte[] b) throws IOException {
        // TODO !!!! rewrite
        // Multithreading read
        if (currentPosition == length){
            currentPosition = 0;
            return -1;
        }
        long oldPosition = currentPosition;
        byte[] data = Bytes.fromString(readString());
        int minLength = Math.min(data.length, b.length);
        System.arraycopy(data, (int) oldPosition, b, 0, minLength);
        currentPosition = oldPosition + minLength;
        if (currentPosition > length){
            currentPosition = length;
        }
        return minLength;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        // TODO !!!! rewrite
        long oldPosition = currentPosition;
        byte[] data = Bytes.fromString(readString());
        System.arraycopy(data, off, b, 0, len);
        currentPosition = oldPosition;
        return len;
    }

    @Override
    public long skip(long n) throws IOException {
        currentPosition += n;
        return n;
    }

    public long length(){
        return length;
    }
}
