package edu.buffalo.record;

public class BufferClass{
    public short[] buffer;
    public Double[] result;
    public long timeSent;
    public int seq;
    BufferClass(short[] buff, long time, int seq_){
        buffer = buff;
        timeSent = time;
        seq=seq_;
    }
    BufferClass(BufferClass frames, Double[] res){
        timeSent = frames.timeSent;
    }
}
