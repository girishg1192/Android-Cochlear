package edu.buffalo.record;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AudioRecordTest";

    private Button mPlayStuff = null;

    //Binders for recording service
    private IBinder recordBinder;
    private ServiceConnection recordService = null;
    private Context mContext;

    //ConfigChannel
    Messenger mProcessingConfig = null;
    boolean mStart = false;


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

        displayNewConfig(new ConfClass(10));
        mPlayStuff = (Button) findViewById(R.id.button);
        mPlayStuff.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendIntent();
            }
        });
    }
    private void sendIntent(){
        Intent serviceIntent = new Intent(this, RecorderService.class);
        if (!mStart) {
            serviceIntent.setAction(RecorderService.ACTION_START_RECORD);
            mPlayStuff.setText("Stop Playback");
        } else {
            serviceIntent.setAction(RecorderService.ACTION_STOP_RECORD);
            mPlayStuff.setText("Start Playback");
        }
        startService(serviceIntent);
        mStart = !mStart;
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
                try {
                    mProcessingConfig.send(Message.obtain(null, ProcessingService.MESSAGE_CONFIG, new Messenger(new MessageHandler())));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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
            Toast toast = null;
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                volChange = Message.obtain(null, ProcessingService.MESSAGE_CONFIG_CHANGE, new ConfClass(-1));
                toast = Toast.makeText(this, "Volume decrease", Toast.LENGTH_SHORT);
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                volChange = Message.obtain(null, ProcessingService.MESSAGE_CONFIG_CHANGE, new ConfClass(1));
                toast = Toast.makeText(this, "Volume increase", Toast.LENGTH_SHORT);
            }
            if (volChange != null) {
                try {
                    toast.show();
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

    private class MessageHandler extends Handler {
        public void handleMessage(Message msg) {
            Log.v("RecorderMainActivity", "Incoming config change");
            switch(msg.what){
                case ProcessingService.MESSAGE_CONFIG_CHANGE:
                    displayNewConfig((ConfClass)msg.obj);
                    break;
                default:
                    break;
            }
        }
    }
    private void displayNewConfig(ConfClass msg){
        TextView tv = (TextView) findViewById(R.id.text);
        String text = "BandGain = " + msg.BandGains + "\nQValue = " + msg.QValue + "\nVolume = " + msg.volume;
        tv.setText(text);
    }
}
