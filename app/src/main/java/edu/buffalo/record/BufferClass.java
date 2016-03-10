package edu.buffalo.record;

public class BufferClass{
    public short[] buffer;
    public Double[] result;
    public long timeBeforeRead;
    public long timeSent; //same as afterRead
    public long timeProcStart;
    public long timeFFTStart;
    public long timeFFTEnd;
    public long timeProcEnd;
    public long timeEnd;
    public int seq;
    BufferClass(short[] buff, long timeBefore, long timeAfterRead, int seq_){
        buffer = buff;
        timeBeforeRead = timeBefore;
        timeSent = timeAfterRead;
        seq=seq_;
    }
    public void printTime(){

    }
    BufferClass(BufferClass frames, Double[] res){
        timeSent = frames.timeSent;
    }
}
