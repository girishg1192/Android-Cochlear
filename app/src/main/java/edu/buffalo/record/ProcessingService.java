package edu.buffalo.record;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import FFTHelper.Complex;
import FFTHelper.ConfClass;
import FFTHelper.FFT;

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

    private static Queue<BufferClass> bufferQueue= new LinkedList<BufferClass>();
    private static Queue<ConfClass> messageQueue = new LinkedList<ConfClass>();

    public final Messenger mBuffer = new Messenger(new MessageHandler());
    public Messenger mConfigListener;

    //Processing params
    private static ConfClass currConfig;
    private static double window[];

    FFT fftClass = new FFT(2048);


    public ProcessingService() {
        currConfig = new ConfClass(10);
        int windowSize = 2048;
        window = new double[windowSize];
        for(int n=0;n<windowSize; n++) {
            window[n] = 0.49656 * Math.cos((2 * Math.PI * n) / (windowSize - 1)) + 0.076849 * Math.cos((4 * Math.PI * n) / (windowSize - 1));
        }
    }

    @Override
    public void onCreate() {
        bindService(new Intent(this, ResultReceiver.class), mResultConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        unbindService(mResultConnection);
        stopProcessing();
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
                    BufferClass buffer = (BufferClass)msg.obj;
                    Log.v(TAG, "Buffer " + buffer + " of size " + buffer.buffer.length);
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
        scheduledTask = scheduler.scheduleAtFixedRate(new ProcessingTask(), 0, 80, TimeUnit.MILLISECONDS);
    }
    private void stopProcessing(){
        //TODO clear out buffers and stuff
        while(!bufferQueue.isEmpty()){
            bufferQueue.remove();
        }
        scheduledTask.cancel(true);
    }

    private void addFramesToQueue(BufferClass buffer){
        bufferQueue.add(buffer);
    }
    private void addMessageToQueue(ConfClass conf){
        messageQueue.add(conf);
    }

    private class ProcessingTask implements Runnable{
        @Override
        public void run() {
            BufferClass buffer;
            //TODO while loop?
            if(!bufferQueue.isEmpty()){
                buffer = bufferQueue.remove();
                process(buffer);
            }
           /* else
                Log.v(TAG, "No pending frames");*/
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
/*
            else
                Log.v(TAG, "No pending messages");
*/
            //TODO check config change
        }
    }

    private void process(BufferClass buffer_){
        short[] buffer = buffer_.buffer;
        buffer_.timeProcStart = System.nanoTime();
        int windowSize = 2048; //Modify to allow multiple sampling rates

        double[] fftReal = new double[windowSize];
        double[] fftIm = new double[windowSize];
        double[] scaledValues = new double[windowSize];
        //double[] channelMagnitude = new double[22];
        ArrayList<Double> channelMagnitude = new ArrayList<>();

        //Apply volume
        for(int i=0; i<buffer.length; i++){
            double volMultiplier = ((double)currConfig.volume)/10;
            buffer[i] = (short) (volMultiplier * (double)buffer[i]);
        }
//        Log.e("Result", "Vol" + (System.currentTimeMillis() - procStart));
        //Blackman window
        for(int n=0; n<windowSize; n++){
            if(n<buffer.length) {
                scaledValues[n] = ((double) buffer[n] / Short.MAX_VALUE);
                scaledValues[n] = scaledValues[n] * window[n];
            }
            else
                scaledValues[n] = 0.0;
        }

//        Log.e("Result", "Windowing" + (System.currentTimeMillis() - procStart));

        //FFT
        for(int i=0; i<windowSize; i++){
            fftIm[i] = 0.0;
            if(i<buffer.length) {
                fftReal[i] = scaledValues[i];
            }
            else {
                fftReal[i] = 0.0;
            }
        }
        buffer_.timeFFTStart= System.nanoTime();
        fftClass.fft(fftReal, fftIm);
        buffer_.timeFFTEnd = System.nanoTime();

        //fftBins contains interleaved real and complex parts
//        Log.e("Result", "FFT " + (System.currentTimeMillis() - procStart));

        for(int channels = 0; channels<22; channels++){
            //8000hz max freq, 22 channels each channel has 8000/22 = 364hz
            // 7.8125 (8000/1024) hz per bin, number of bins for 364 hz = 47
            double magnitude = 0.0;
            for(int bin = channels*47; bin<(channels+1)*47 && bin<(windowSize/2); bin++){
                magnitude+= (fftReal[bin]*fftReal[bin]) + (fftIm[bin]*fftIm[bin]);
            }
            magnitude = Math.sqrt(magnitude);
            channelMagnitude.add(magnitude);
        }
//        Log.e("Result", "Bandpass:" + (System.currentTimeMillis() - procStart));

        Collections.sort(channelMagnitude);
//        buffer_.result = (Double[])channelMagnitude.toArray();
        buffer_.result = new Double[channelMagnitude.size()];
        for (int i = 0; i < buffer_.result.length; i++) {
            buffer_.result[i] = new Double((Double)(channelMagnitude.toArray())[i]);
        }
//        Log.e("Result", "Processing" + buffer_.seq + " " + (System.currentTimeMillis() - procStart));
        buffer_.timeProcEnd = System.nanoTime();

        //Sort the channels and select first few
        Message packedBuffer = Message.obtain(null, ResultReceiver.RESULT_PUBLISH, buffer_);
        try {
            mResult.send(packedBuffer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    private void configChange(ConfClass conf){
        currConfig.volume += conf.volumeChange;
        if(currConfig.volume>10)currConfig.volume = 10;
        if(currConfig.volume<0)currConfig.volume = 0;
        Log.v(TAG, "Config change change volume " + currConfig.volume);
    }

    Messenger mResult = null;
    boolean mBound = false;

    private ServiceConnection mResultConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if(service!=null) {
                mResult = new Messenger(service);
                mBound = true;
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mResult = null;
            mBound = false;
        }
    };
}

