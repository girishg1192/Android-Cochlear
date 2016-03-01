package edu.buffalo.record;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class RecordService extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_START_RECORD = "edu.buffalo.record.action.ACTION_START_RECORD";
    public static final String ACTION_STOP_RECORD = "edu.buffalo.record.action.ACTION_STOP_RECORD";
    private static String TAG = "RecordService";
    private int bufferSize;
    private Thread recordThread;
    private AudioRecord record;
    private static boolean isRecording;

    private static final String EXTRA_PARAM1 = "edu.buffalo.record.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "edu.buffalo.record.extra.PARAM2";

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionRecord(Context context, String param1, String param2) {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_START_RECORD);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public RecordService() {
        super("RecordService");
        bufferSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.v("RecordService", "Buffer Size = " + bufferSize);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.v(TAG, intent.getAction());
            if (ACTION_START_RECORD.equals(action)) {
                handleActionRecord();
            }else if(ACTION_STOP_RECORD.equals(action)){
                handleActionStop();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionRecord() {
        if(!isRecording) {
            record = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            record.startRecording();
            recordThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "run thread");
                    recordAudio();
                }
            }, "Record");
            isRecording = true;
            recordThread.start();
        }
    }
    private void recordAudio(){
        while(isRecording){
            short[] buffer = new short[bufferSize];
            record.read(buffer, 0, bufferSize);
            Log.v(TAG , buffer + " read");
        }
    }
    private void handleActionStop(){
        //TODO stop recording thread
        if(isRecording){
            Log.v(TAG, "Stop Recording");
            record.release();
            isRecording = false;
            recordThread = null;
        }
    }
}
