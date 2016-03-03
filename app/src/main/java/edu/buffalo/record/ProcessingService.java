package edu.buffalo.record;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
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
//    public static final String ACTION_PROCESS_FRAMES = "edu.buffalo.record.action.ACTION_PROCESS_FRAMES";
//    public static final String ACTION_CONFIG_CHANGE = "edu.buffalo.record.action.ACTION_CONFIG_CHANGE";
    private static final String TAG = "ProcessingService";

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;
    private class bufferObject{
        public short[] buffer;
    }
    private static Queue<short[]> bufferQueue= new LinkedList<short[]>();

    public final Messenger mBuffer = new Messenger(new MessageHandler());

    public ProcessingService() {
    }
    private class MessageHandler extends Handler {
        public void handleMessage(Message msg){
            Log.v(TAG, "Incoming Message " + msg.what);
            switch(msg.what){
                case MESSAGE_START_PROCESS:
                    startProcessing();
                    break;
                case MESSAGE_CONTAINS_BUFFER:
                    short[] buffer = (short[])msg.obj;
                    Log.v(TAG, "Buffer " + buffer + " of size " + buffer.length);
                    addFramesToQueue(buffer);
                    break;
                case MESSAGE_CONFIG_CHANGE:
                    //TODO config change functions
                    break;
                case MESSAGE_STOP_PROCESS:
                    stopProcessing();
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
        scheduledTask = scheduler.scheduleAtFixedRate(new ProcessingTask(), 0, 8, TimeUnit.SECONDS);
    }
    private void stopProcessing(){
        //TODO clear out buffers and stuff
        scheduledTask.cancel(true);
    }

    private void addFramesToQueue(short[] buffer){
        bufferQueue.add(buffer);
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
                Log.v(TAG, "Nothing to do");
            //TODO check config change
        }
    }
    private void process(short[] buffer){
        Log.e(TAG, "Do some processing with " + buffer);
    }
}
