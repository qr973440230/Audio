package com.qr.core.library.audio;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioPlayerTextView extends AppCompatTextView {
    private static final String TAG = AudioPlayerTextView.class.getSimpleName();
    private static List<AudioPlayerTextView> audioPlayerTextViews = new ArrayList<>();


    public AudioPlayerTextView(Context context) {
        super(context, null);
    }


    public AudioPlayerTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 默认16sp
        setTextSize(16);
        setPadding(0, 0, 8, 0);
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (AudioPlayerTextView audioPlayerTextView : audioPlayerTextViews) {
                    if (audioPlayerTextView == AudioPlayerTextView.this) {
                        continue;
                    }

                    // 所有其他AudioPlayerTextView暂停播放
                    audioPlayerTextView.pause();
                }

                // 设置点击事件
                if (!hasPrepared) {
                    // 没有准备好，点击无效
                    return;
                }

                if (mediaPlayer.isPlaying()) {
                    pause();
                } else {
                    play();
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        audioPlayerTextViews.add(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        audioPlayerTextViews.remove(this);
    }

    static public void pauseAll() {
        for (AudioPlayerTextView audioPlayerTextView : audioPlayerTextViews) {
            audioPlayerTextView.pause();
        }
    }

    static public void releaseAll() {
        for (AudioPlayerTextView audioPlayerTextView : audioPlayerTextViews) {
            audioPlayerTextView.release();
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        // 啥也不做 禁止 防止覆盖当前
    }

    // 播放相关
    private MediaPlayer mediaPlayer;
    private boolean hasPrepared = false;

    public void setDataSource(String url) {
        if (mediaPlayer == null) {
            // 设置MediaPlayer
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        mediaPlayer.reset();
                        stopAnimate();
                        stopUpdateDuration();
                        hasPrepared = false;
                        return false;
                    }
                });
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        hasPrepared = true;
                        initDuration();
                        initAnimate();
                    }
                });
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopUpdateDuration();
                        stopAnimate();
                        initAnimate();
                        initDuration();
                    }
                });
            } catch (Exception e) {
                Log.d(TAG, "Init MediaPlayer Failure: " + e.getMessage());
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        }

        if (mediaPlayer == null) {
            Log.d(TAG, "MediaPlayer == null");
            return;
        }

        try {
            hasPrepared = false;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "MediaPlayer SetDataSource Error: " + e.getMessage());
        }
    }

    public void play() {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }

        try {
            mediaPlayer.start();
            preUpdateTime = System.currentTimeMillis();
            startAnimate();
            startUpdateDuration();
        } catch (Exception e) {
            Log.d(TAG, "Play Failure : " + e.getMessage());
        }
    }

    public void pause() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            return;
        }

        try {
            mediaPlayer.pause();
            stopUpdateDuration();
            stopAnimate();
        } catch (Exception e) {
            Log.d(TAG, "Pause Failure : " + e.getMessage());
        }
    }

    public void release() {
        if (mediaPlayer == null) {
            return;
        }

        try {
            mediaPlayer.stop();
        } catch (Exception e) {
            Log.d(TAG, "Stop Failure : " + e.getMessage());
        } finally {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // 倒计时相关
    private long leftDuration;
    private long preUpdateTime;
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            startUpdateDuration();
        }
    };

    private void initDuration() {
        leftDuration = mediaPlayer.getDuration();
        if (leftDuration <= 0) {
            setText("00:00");
            return;
        }

        long sec = leftDuration / 1000;
        long m = sec / 60;
        long s = sec % 60;
        setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
    }

    private void startUpdateDuration() {
        long currentTimeMillis = System.currentTimeMillis();
        long deltaTime = currentTimeMillis - preUpdateTime;
        preUpdateTime = currentTimeMillis;
        leftDuration -= deltaTime;

        if (leftDuration <= 0) {
            setText("00:00");
            return;
        }

        long sec = leftDuration / 1000;
        long m = sec / 60;
        long s = sec % 60;
        setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
        timeHandler.postDelayed(timeRunnable, 1000);
    }

    private void stopUpdateDuration() {
        timeHandler.removeCallbacks(timeRunnable);
    }


    // 声音动画相关
    private int[] drawLefts = new int[]{
            R.drawable.audio_volume_play_level_1,
            R.drawable.audio_volume_play_level_2,
            R.drawable.audio_volume_play_level_3,
    };

    private Handler animateHandle = new Handler();
    private Runnable animateRunnable = new Runnable() {
        @Override
        public void run() {
            startAnimate();
        }
    };
    private int count = 0;

    private void initAnimate() {
        count = 0;
        setDrawableLeft(drawLefts[1]);
    }

    private void startAnimate() {
        setDrawableLeft(drawLefts[count % drawLefts.length]);
        ++count;
        animateHandle.postDelayed(animateRunnable, 300);
    }

    private void stopAnimate() {
        animateHandle.removeCallbacks(animateRunnable);
    }

    private void setDrawableLeft(@DrawableRes int resId) {
        Drawable drawable = getResources().getDrawable(resId);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        setCompoundDrawables(drawable, null, null, null);
    }
}
