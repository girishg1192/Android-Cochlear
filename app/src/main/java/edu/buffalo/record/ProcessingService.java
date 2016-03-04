package edu.buffalo.record;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ProcessingService extends Service {
    public static final String ACTION_START_PROCESS = "edu.buffalo.record.action.ACTION_START_PROCESS";
    public static final int MESSAGE_START_PROCESS = 1;
    public static final int MESSAGE_CONTAINS_BUFFER = 2;
    public static final int MESSAGE_CONFIG_CHANGE = 3;
    public static final int MESSAGE_STOP_PROCESS = 4;
    public static final int MESSAGE_CONFIG = 5;

//    public static final String ACTION_PROCESS_FRAMES = "edu.buffalo.record.action.ACTION_PROCESS_FRAMES";
//    public static final String ACTION_CONFIG_CHANGE = "edu.buffalo.record.action.ACTION_CONFIG_CHANGE";
    private static final String TAG = "ProcessingService";
    private boolean mStarted = false;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;

    private static Queue<short[]> bufferQueue= new LinkedList<short[]>();
    private static Queue<ConfClass> messageQueue = new LinkedList<ConfClass>();

    public final Messenger mBuffer = new Messenger(new MessageHandler());
    public Messenger mConfigListener;

    //Processing params
    private static ConfClass currConfig;
    private static double window[];

    public ProcessingService() {
        currConfig = new ConfClass(10);
        int windowSize = 2048;
        window = new double[windowSize];
        for(int n=0;n<windowSize; n++) {
            window[n] = 0.49656 * Math.cos((2 * Math.PI * n) / (windowSize - 1)) + 0.076849 * Math.cos((4 * Math.PI * n) / (windowSize - 1));
        }
    }
    private class MessageHandler extends Handler {
        public void handleMessage(Message msg){
            Log.v(TAG, "Incoming Message " + msg.what);
            switch(msg.what){
                case MESSAGE_START_PROCESS:
                    mStarted = true;
                    startProcessing();
                    break;
                case MESSAGE_CONTAINS_BUFFER:
                    short[] buffer = (short[])msg.obj;
                    Log.v(TAG, "Buffer " + buffer + " of size " + buffer.length);
                    addFramesToQueue(buffer);
                    break;
                case MESSAGE_CONFIG_CHANGE:
                    addMessageToQueue((ConfClass) msg.obj);
                    break;
                case MESSAGE_STOP_PROCESS:
                    mStarted = false;
                    stopProcessing();
                    break;
                case MESSAGE_CONFIG:
                    mConfigListener = (Messenger) msg.obj;
                    break;

            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "Returning Binder to Processing Service");
        return mBuffer.getBinder();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent!=null){
            Log.v(TAG, "Intent received " + intent.getAction());
            if(ACTION_START_PROCESS.equals(intent.getAction())){
                Log.v(TAG, "Starting processing action");
                startProcessing();
            }
        }
        return START_STICKY;
    }
    private void startProcessing(){
        scheduledTask = scheduler.scheduleAtFixedRate(new ProcessingTask(), 5, 8, TimeUnit.MILLISECONDS);
    }
    private void stopProcessing(){
        //TODO clear out buffers and stuff
        while(!bufferQueue.isEmpty()){
            bufferQueue.remove();
        }
        scheduledTask.cancel(true);
    }

    private void addFramesToQueue(short[] buffer){
        bufferQueue.add(buffer);
    }
    private void addMessageToQueue(ConfClass conf){
        messageQueue.add(conf);
    }

    private class ProcessingTask implements Runnable{
        @Override
        public void run() {
            short[] buffer;
            //TODO while loop?
            if(!bufferQueue.isEmpty()){
                buffer = bufferQueue.remove();
                process(buffer);
            }
            else
                Log.v(TAG, "No pending frames");
            if(!messageQueue.isEmpty()){
                while(!messageQueue.isEmpty()) {
                    ConfClass config = messageQueue.remove();
                    configChange(config);
                }
                try {
                    mConfigListener.send(Message.obtain(null,MESSAGE_CONFIG_CHANGE, currConfig));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            else
                Log.v(TAG, "No pending messages");
            //TODO check config change
        }
    }

    private void process(short[] buffer){
        Log.e(TAG, "Do some processing with " + buffer + " " + buffer.length);
        int windowSize = 2048; //Modify to allow multiple sampling rates
        Complex[] fftBins = new Complex[windowSize];
        double[] scaledValues = new double[windowSize];
        double[] channelMagnitude = new double[22];

        //Apply volume
        for(int i=0; i<buffer.length; i++){
            double volMultiplier = ((double)currConfig.volume)/10;
            buffer[i] = (short) (volMultiplier * (double)buffer[i]);
        }
        Log.v(TAG, "here 1");
        //Blackman window
        for(int n=0; n<windowSize; n++){
            if(n<buffer.length) {
                scaledValues[n] = ((double) buffer[n] / Short.MAX_VALUE);
                scaledValues[n] = scaledValues[n] * window[n];
            }
            else
                scaledValues[n] = 0.0;
        }

        //FFT
        for(int i=0; i<windowSize; i++){
            if(i<buffer.length) {
                fftBins[i] = new Complex(scaledValues[i], 0);
            }
            else {
                fftBins[i] = new Complex(0,0);
            }
        }
        FFT fftClass = new FFT();
        fftBins = fftClass.fft(fftBins);
        //fftBins contains interleaved real and complex parts
        for(int channels = 0; channels<22; channels++){
            //8000hz max freq, 22 channels each channel has 8000/22 = 364hz
            // 7.8125 (8000/1024) hz per bin, number of bins for 364 hz = 47
            double magnitude = 0.0;
            for(int bin = channels*47; bin<(channels+1)*47 && bin<(windowSize/2); bin++){
                double abs = fftBins[bin].abs();
                magnitude+= abs*abs;
            }
            channelMagnitude[channels] = magnitude;
        }

        //Sort the channels and select first few
        //TODO send to receiver

        Log.v(TAG, "fftResult" + fftBins.length);
    }
    private void configChange(ConfClass conf){
        currConfig.volume += conf.volumeChange;
        if(currConfig.volume>10)currConfig.volume = 10;
        if(currConfig.volume<0)currConfig.volume = 0;
        Log.v(TAG, "Config change change volume " + currConfig.volume);

    }
}
