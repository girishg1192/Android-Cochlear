package edu.buffalo.record;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AudioRecordTest";

    private PlayStuff mPlayStuff = null;

    private IBinder recordBinder;
    private ServiceConnection recordService;
    private Context mContext;


    class PlayStuff extends Button {
        boolean mStart = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                Intent serviceIntent = new Intent(getContext(), RecorderService.class);
                if (mStart) {
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
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
