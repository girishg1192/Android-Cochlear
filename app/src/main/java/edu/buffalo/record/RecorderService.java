package edu.buffalo.record;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RecorderService extends Service {
    public static final String ACTION_START_RECORD = "edu.buffalo.record.action.ACTION_START_RECORD";
    public static final String ACTION_STOP_RECORD = "edu.buffalo.record.action.ACTION_STOP_RECORD";

    private static String TAG = "RecorderService";

    private int bufferSize;
    private AudioRecord record;
    private static boolean isRecording;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;

    private final IBinder binder = new Binder();

    public RecorderService() {
        bufferSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.v(TAG, "Buffer Size = " + bufferSize);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Service Bound");
        return binder;
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
    }

    public void handleActionRecord(){
        Log.v(TAG, "Recorder buffer" + bufferSize);
        record = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        if(record!=null) {
            record.startRecording();
            scheduledTask = scheduler.scheduleAtFixedRate(new AudioRecordTask(), 0, 50, TimeUnit.MILLISECONDS);
        }
    }
    private class AudioRecordTask implements Runnable{
        @Override
        public void run() {
            short[] buffer = new short[bufferSize];
            record.read(buffer, 0, bufferSize);
            Log.v(TAG , buffer + " read");
        }
    }
    @Override
    public void onDestroy(){
        record.stop();
        record = null;
        scheduledTask.cancel(true);
    }
}
