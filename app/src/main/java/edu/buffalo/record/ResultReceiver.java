package edu.buffalo.record;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.util.Log;

import java.nio.Buffer;

public class ResultReceiver extends Service {
//    public static String RESULT_PUBLISH = "edu.buffalo.record.action.RESULT_PUBLISH";
    public static final int RESULT_PUBLISH = 1;
    private static final String TAG = "ResultReceiver";

    public ResultReceiver() {
    }

    public final Messenger mBuffer = new Messenger(new ResultHandler());
    private class ResultHandler extends Handler {
        public void handleMessage(Message msg) {
            switch(msg.what){
                case RESULT_PUBLISH:
                    BufferClass result = (BufferClass) msg.obj;
                    result.timeEnd = System.nanoTime();
                    Log.v("RESULT"," Result" + result.seq + " BeforeRead" + result.timeBeforeRead + " AfterRead" + result.timeSent +
                            " ProcStart" + result.timeProcStart + " FFTStart" + result.timeFFTStart +
                            " FFTEnd" + result.timeFFTEnd +
                            " ProcEnd" + result.timeProcEnd + " " + result.timeEnd);
                    break;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBuffer.getBinder();
    }
}
