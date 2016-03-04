package edu.buffalo.record;

/**
 * Created by girish on 3/3/16.
 */
public class ConfClass {
    public int volumeChange;
    //
    public int BandGains;
    public int QValue;
    public short volume;
    ConfClass(){
        BandGains = 0;
        QValue = 0;
        volumeChange = 0;
        volume = 0;
    }
    ConfClass(int volChange){
        volumeChange = volChange;
        volume = (short)volChange;
    }
}
