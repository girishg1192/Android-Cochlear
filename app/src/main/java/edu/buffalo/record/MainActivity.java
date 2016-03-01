package edu.buffalo.record;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName = null;
    private static boolean firstPlay = true;
    private static boolean firstRecord = true;
    private final int STREAM_PLAY = 44100;

    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;

    private PlayButton mPlayButton = null;
    private MediaPlayer mPlayer = null;
    private PlayStuff mPlayStuff = null;

    public void recordStream(boolean startRecording){
        Log.v(LOG_TAG, "RecordStream" + startRecording);
        if(startRecording){
            if(firstRecord) {
                Log.v(LOG_TAG, "Create MediaRecorder");
                mRecorder = new MediaRecorder();
                firstRecord = false;
            }

            try {
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile(mFileName);
                mRecorder.prepare();
                Log.v(LOG_TAG, "Record start");
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRecorder.start();
        }
        else{
            mRecorder.stop();
            mRecorder.reset();
        }
    }
    public void playStream(boolean startPlayback){
        if(startPlayback){
            if(firstPlay){
                mPlayer = new MediaPlayer();
                firstPlay = false;
            }
            try {
                Log.v(LOG_TAG, "file name" + mFileName);
                mPlayer.setDataSource(mFileName);
                mPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mPlayer.start();
        }
        else{
            Log.v(LOG_TAG, "Stop playback");
            mPlayer.stop();
            mPlayer.reset();
        }
    }

    class RecordButton extends Button {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                recordStream(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }
    class PlayButton extends Button {
        boolean mStartPlay = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                playStream(mStartPlay);
                if (mStartPlay) {
                    setText("Stop Playback");
                } else {
                    setText("Start Playback");
                }
                mStartPlay = !mStartPlay;
            }
        };
        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start Playback");
            setOnClickListener(clicker);
        }
    }
    class PlayStuff extends Button {
        boolean mStart = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                playStream(mStart);
                if (mStart) {
                    setText("Stop Playback");
                } else {
                    setText("Start Playback");
                }
                mStart = !mStart;
            }
        };
        public PlayStuff(Context ctx) {
            super(ctx);
            setText("Start Loopback");
            setOnClickListener(clicker);
        }
    }
    private class LoopbackThread extends Thread{
        @Override
        public void run(){
            int bufferSize = AudioTrack.getMinBufferSize( 44100,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack play= new AudioTrack(AudioManager.STREAM_MUSIC, STREAM_PLAY,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
            short[] buffer = new short[bufferSize];
            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            }

    }
    public MainActivity(){
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/file.amr";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout ll = new LinearLayout(this);
        /*
        mRecordButton = (RecordButton)findViewById(R.id.recordButton);
        mPlayButton = (PlayButton) findViewById(R.id.playButton);
        *
        mRecordButton = new RecordButton(this);
        ll.addView(mRecordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        mPlayButton = new PlayButton(this);
        ll.addView(mPlayButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
                LinearLayout ll = new LinearLayout(this);
                */
        mPlayStuff = new PlayStuff(this);
        ll.addView(mPlayStuff,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        setContentView(ll);
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
