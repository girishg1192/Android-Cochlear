package edu.buffalo.record;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.security.Key;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AudioRecordTest";

    private PlayStuff mPlayStuff = null;

    //Binders for recording service
    private IBinder recordBinder;
    private ServiceConnection recordService = null;
    private Context mContext;

    //ConfigChannel
    Messenger mProcessingConfig = null;
    boolean mStart = false;




    class PlayStuff extends Button {

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                Intent serviceIntent = new Intent(getContext(), RecorderService.class);
                if (!mStart) {
                    serviceIntent.setAction(RecorderService.ACTION_START_RECORD);
                    setText("Stop Playback");
                } else {
                    serviceIntent.setAction(RecorderService.ACTION_STOP_RECORD);
                    setText("Start Playback");
                }
                startService(serviceIntent);
                mStart = !mStart;
            }
        };
        public PlayStuff(Context ctx) {
            super(ctx);
            setText("Start Loopback");
            setOnClickListener(clicker);
        }
    }

    public MainActivity(){
        recordService = new ServiceConnection(){

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        mContext = this;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindService(new Intent(this, RecorderService.class), recordService, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, ProcessingService.class), mProcessingConnection, Context.BIND_AUTO_CREATE);
        LinearLayout ll = new LinearLayout(this);

        mPlayStuff = new PlayStuff(this);
        ll.addView(mPlayStuff,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        setContentView(ll);
    }
    @Override
    protected void onDestroy(){
        if(recordService!=null){
            unbindService(recordService);
            recordService = null;
            recordBinder = null;
        }
        if(mProcessingConnection!=null){
            unbindService(mProcessingConnection);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection mProcessingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mProcessingConfig = new Messenger(service);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(mStart) {
            Log.v("VolumeChange", event.toString());
            Message volChange = null;
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                volChange = Message.obtain(null, ProcessingService.MESSAGE_CONFIG_CHANGE, -1);
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                volChange = Message.obtain(null, ProcessingService.MESSAGE_CONFIG_CHANGE, 1);
            }
            if (volChange != null) {
                try {
                    mProcessingConfig.send(volChange);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }
    //TODO volume receiver to send config change to processor
}
