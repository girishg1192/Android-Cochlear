package edu.buffalo.record;

public class BufferClass{
    public short[] buffer;
    public Double[] result;
    public long timeSent;
    BufferClass(short[] buff, long time){
        buffer = buff;
        timeSent = time;
    }
    BufferClass(BufferClass frames, Double[] res){
        timeSent = frames.timeSent;
    }
}
