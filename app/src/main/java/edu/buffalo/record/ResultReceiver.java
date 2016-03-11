package edu.buffalo.record;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


public class ResultReceiver extends BroadcastReceiver {
    public static String ACTION_RESULT_PUBLISH = "edu.buffalo.record.action.RESULT_PUBLISH";
    private static final String TAG = "ResultReceiver";

    public ResultReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent!=null){
            if(intent.getAction().equals(ACTION_RESULT_PUBLISH)){
                Bundle publishedValues = intent.getExtras();
                if(publishedValues!=null){
                    long[] times=publishedValues.getLongArray("Times");
                    int seq = publishedValues.getInt("Seq");
                    long timeEnd = System.nanoTime();
                    if(times!=null) {
                     /*   Log.v(TAG, " Result" + seq + " ProcStart" + times[0] + " FFTStart" + times[1] +
                                " FFTEnd" + times[2] + " ProcEnd" + times[3] + " " + timeEnd);*/
                        Log.v(TAG, times[0] + " " + times[1] +
                                " " + times[2] + " " + times[3] + " " + timeEnd);
                    }
                }
            }
        }
    }

}
