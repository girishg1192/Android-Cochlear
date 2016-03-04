package edu.buffalo.record;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ResultReceiver extends Service {
    public static String RESULT_PUBLISH = "edu.buffalo.record.action.RESULT_PUBLISH";

    public ResultReceiver() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            if(RESULT_PUBLISH.equals(intent.getAction())){
                //TODO
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
