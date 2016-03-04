package edu.buffalo.record;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RecorderService extends Service {
    public static final String ACTION_START_RECORD = "edu.buffalo.record.action.ACTION_START_RECORD";
    public static final String ACTION_STOP_RECORD = "edu.buffalo.record.action.ACTION_STOP_RECORD";
    private static int seqNo = 0;

    private static String TAG = "RecorderService";

    //Variables for RecorderService, AudioRecord scheduledTask run on executor scheduler
    private int bufferSize;
    private AudioRecord record;
    private static boolean isRecording;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;


    //Messenger for processing service
    Messenger mProcessing = null;
    public boolean mBound;

    public RecorderService() {
        bufferSize = AudioRecord.getMinBufferSize(16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.v(TAG, "Buffer Size = " + bufferSize);
        mBound = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Service Bound");
        return new Binder();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Intent received = " + intent.getAction());
        if (intent != null) {
            final String action = intent.getAction();
            Log.v(TAG, intent.getAction());
            if (ACTION_START_RECORD.equals(action)) {
                handleActionRecord();
            }else if(ACTION_STOP_RECORD.equals(action)){
                handleActionStop();
            }
        }
        return START_STICKY;
    }

    private void handleActionStop() {
        Log.v(TAG, "Stop recording");
        record.stop();
        scheduledTask.cancel(true);
        Message stopProcess = Message.obtain(null, ProcessingService.MESSAGE_STOP_PROCESS);
        try {
            mProcessing.send(stopProcess);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void handleActionRecord(){
        Log.v(TAG, "Recorder buffer" + bufferSize);
        record = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        if(record!=null) {
            record.startRecording();
            //Start up the processingService
            Message startProcess = Message.obtain(null, ProcessingService.MESSAGE_START_PROCESS);
            try {
                mProcessing.send(startProcess);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            scheduledTask = scheduler.scheduleAtFixedRate(new AudioRecordTask(), 0, 120, TimeUnit.MILLISECONDS);
        }
    }
    private class AudioRecordTask implements Runnable{
        @Override
        public void run() {
            short[] buffer = new short[bufferSize];
            long time = System.currentTimeMillis();
            Log.e(TAG, "initial time " + time);
            record.read(buffer, 0, bufferSize);
            Log.e(TAG, "initial time 2 " + (time-System.currentTimeMillis()));
            BufferClass buffObject = new BufferClass(buffer, time, seqNo++);
            Message packedBuffer = Message.obtain(null, ProcessingService.MESSAGE_CONTAINS_BUFFER, buffObject);
            try {
                //Send the buffer to Processing service
                mProcessing.send(packedBuffer);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Log.v(TAG , buffer + " read " + buffer.length);
        }
    }
    @Override
    public void onCreate(){
        Log.e(TAG, "Binding Processing service");
        bindService(new Intent(this, ProcessingService.class), mProcessingConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onDestroy(){
        record.stop();
        record = null;
        scheduledTask.cancel(true);
        if(mBound){
            Message stopProcess = Message.obtain(null, ProcessingService.MESSAGE_STOP_PROCESS);
            try {
                mProcessing.send(stopProcess);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            unbindService(mProcessingConnection);
            mProcessing = null;
            mProcessingConnection = null;
            mBound = false;
        }
    }

    private ServiceConnection mProcessingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if(service!=null) {
                mProcessing = new Messenger(service);
                mBound = true;
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mProcessing = null;
            mBound = false;
        }
    };
}
