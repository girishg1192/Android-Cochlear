package edu.buffalo.record;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ProcessingService extends Service {
    public static final String ACTION_START_PROCESS = "edu.buffalo.record.action.ACTION_START_PROCESS";
//    public static final String ACTION_PROCESS_FRAMES = "edu.buffalo.record.action.ACTION_PROCESS_FRAMES";
//    public static final String ACTION_CONFIG_CHANGE = "edu.buffalo.record.action.ACTION_CONFIG_CHANGE";
    private static final String TAG = "ProcessingService";

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;

    public ProcessingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
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
        scheduler.scheduleAtFixedRate(new ProcessingTask(), 25, 8, TimeUnit.MILLISECONDS);
    }
    private class ProcessingTask implements Runnable{

        @Override
        public void run() {
            //TODO check message queue for new frames
        }
    }
}
