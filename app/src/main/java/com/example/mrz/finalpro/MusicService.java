package com.example.mrz.finalpro;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class MusicService extends Service {
    private IBinder mBinder;
    private MediaPlayer mPlayer;

    public static final int PLAY_MUSIC = 11;
    public static final int STOP_MUSIC = 12;

    private int mState;
    private static final int UNINITIALIZED = 0;
    private static final int PREPARED = 1;
    private static final int STOPPED = 2;
    private static final int PAUSED = 3;
    private static final int PLAYING = 4;
    private static final int INITIALIZED = 5;

    private static final String rootPath = "/data/";
    private String namePlaying;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("service", "music service created!");
        mBinder = new MyBinder();
        mPlayer = new MediaPlayer();
        mState = UNINITIALIZED;
        namePlaying = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(mState == PLAYING || mState == PAUSED)
            mPlayer.stop();
        mPlayer.release();
        mState = UNINITIALIZED;
        return true;
    }

    public class MyBinder extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply,
                                     int flags) throws RemoteException {
            if(code == PLAY_MUSIC) {
                String musicName = data.readString();
                if(musicName == null) {
                    Log.d("service", "no new file");
                    play(null);
                } else if(musicName.equals(namePlaying)) {
                    if(mState == STOPPED)
                        play(null);
                    else
                        return false;
                } else {
                    namePlaying = musicName;
                    String url = rootPath + musicName;
                    Log.d("service", url);
                    play(url);
                }
            } else if(code == STOP_MUSIC) {
                stop();
            } else {
                return false;
            }
            return true;
        }
    }

    private class MyOnPreparedListener implements MediaPlayer.OnPreparedListener {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mState = PREPARED;
            Log.d("service", "prepared");
            mPlayer.start();
            mState = PLAYING;
        }
    }

    private void play(String url) {
        if(url != null) {   //means there is a request that service should change the music
            mPlayer.reset();
            mState = UNINITIALIZED;
            try {
                mPlayer.setDataSource(url);
                mState = INITIALIZED;
            } catch (Exception e) {
                e.printStackTrace();
            }
            mPlayer.setOnPreparedListener(new MyOnPreparedListener());
            try {
                mPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(mState == PAUSED) {
            mPlayer.start();
            mState = PLAYING;
        } else if(mState == PLAYING) {
            mPlayer.pause();
            mState = PAUSED;
        } else if(mState == STOPPED) {
            try {
                mPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stop() {
        if(mState == PLAYING || mState == PAUSED) {
            mPlayer.stop();
            mState = STOPPED;
        }
    }
}
