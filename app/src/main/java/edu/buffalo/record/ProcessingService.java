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
    int windowSize;
    private int seqNo;

    FFT fftClass = new FFT(128);


    public ProcessingService() {
        currConfig = new ConfClass(10);
        windowSize = 128;
        window = new double[windowSize];
        for(int n=0;n<windowSize; n++) {
            window[n] = 0.49656 * Math.cos((2 * Math.PI * n) / (windowSize - 1)) + 0.076849 * Math.cos((4 * Math.PI * n) / (windowSize - 1));
        }
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
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
                    short[] buffer = (short[])msg.obj;
                    for(int bytesRead = 0; bytesRead<buffer.length; bytesRead+=windowSize){
                        short[] windowedBuffer = new short[windowSize];
                        System.arraycopy(buffer, bytesRead, windowedBuffer, 0, windowSize);
                        addFramesToQueue(windowedBuffer);
                    }
                    break;
                case MESSAGE_CONFIG_CHANGE:
                    ConfClass message = new ConfClass((String)msg.obj);
                    Log.e(TAG, (String)msg.obj);
                    addMessageToQueue(message);
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

    private void startProcessing(){
        scheduledTask = scheduler.scheduleAtFixedRate(new ProcessingTask(), 0, 8, TimeUnit.MILLISECONDS);
    }
    private void stopProcessing(){
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
//                if(bytesProcessed != bufferSize) {
                    buffer = bufferQueue.remove();
                    //TODO send striped data
//                }
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
                    mConfigListener.send(Message.obtain(null,MESSAGE_CONFIG_CHANGE, currConfig.toString()));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void process(short[] buffer){
        long timeProcStart = System.nanoTime();
//        int windowSize = 128; //Modify to allow multiple sampling rates
        double[] fftReal = new double[windowSize];
        double[] fftIm = new double[windowSize];
        double[] scaledValues = new double[windowSize];
        ArrayList<Double> channelMagnitude = new ArrayList<>();

        //Apply volume
        for(int i=0; i<buffer.length; i++){
            double volMultiplier = ((double)currConfig.volume)/10;
            buffer[i] = (short) (volMultiplier * (double)buffer[i]);
        }
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
        long timeFFTStart= System.nanoTime();
        fftClass.fft(fftReal, fftIm);
        long timeFFTEnd = System.nanoTime();

        //fftBins contains interleaved real and complex parts
//        Log.e("Result", "FFT " + (System.currentTimeMillis() - procStart));
        //Bandpass filter
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

        Collections.sort(channelMagnitude);
        Double[] result = new Double[channelMagnitude.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Double((Double)(channelMagnitude.toArray())[i]);
        }
//        Log.e("Result", "Processing" + buffer_.seq + " " + (System.currentTimeMillis() - procStart));
        long timeProcEnd = System.nanoTime();

        //Sort the channels and select first few

        Intent sendResults = new Intent(this, ResultReceiver.class);

        sendResults.setAction(ResultReceiver.ACTION_RESULT_PUBLISH);
        sendResults.putExtra("OutputBuff", result);
        sendResults.putExtra("Seq", seqNo++);

        long[] times = new long[4];
//        times[0] = buffer_.timeBeforeRead;
//        times[1] = buffer_.timeSent; //same as afterRead
        times[0] = timeProcStart;
        times[1] = timeFFTStart;
        times[2] = timeFFTEnd;
        times[3] = timeProcEnd;

        sendResults.putExtra("Times", times);
        sendBroadcast(sendResults);
    }
    private void configChange(ConfClass conf){
        currConfig.volume += conf.volumeChange;
        if(currConfig.volume>10)currConfig.volume = 10;
        if(currConfig.volume<0)currConfig.volume = 0;
        Log.v(TAG, "Config change change volume " + currConfig.volume);
    }

    Messenger mResult = null;
    boolean mBound = false;

}

