package com.qr.core.library.audio.recorder;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class AudioRecorderButton extends AppCompatButton {
    private static final String TAG = AudioRecorderButton.class.getSimpleName();

    //手指滑动 距离
    private static final int DISTANCE_Y_CANCEL = 100;

    //状态
    private static final int STATE_NORMAL = 1;
    private static final int STATE_RECORDING_NORMAL = 2;
    private static final int STATE_RECORDING_CANCEL = 3;
    private static final int STATE_CANCEL = 4;
    private static final int STATE_FINISH = 5;

    //当前状态
    private int curState = STATE_NORMAL;

    public AudioRecorderButton(Context context) {
        this(context, null);
    }

    public AudioRecorderButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.btn_recorder_normal);
        setText(R.string.str_recorder_normal);

        dir = context.getExternalCacheDir();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 显示Dialog
                showRecordingDialog();
                startRecord();
                changeState(STATE_RECORDING_NORMAL);
                break;
            case MotionEvent.ACTION_MOVE:
                if (curState == STATE_RECORDING_NORMAL) {
                    if (wantToCancel(x, y)) {
                        updateNoticeText(R.string.str_recorder_recording_cancel);
                        changeState(STATE_RECORDING_CANCEL);
                    }
                } else {
                    if (!wantToCancel(x, y)) {
                        updateNoticeText(R.string.str_recorder_recording_normal);
                        changeState(STATE_RECORDING_NORMAL);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                dismissDialog();
                if (curState == STATE_RECORDING_NORMAL) {
                    // 录制结束
                    stopRecord();
                    changeState(STATE_FINISH);
                } else {
                    // 录制取消
                    cancelRecord();
                    changeState(STATE_CANCEL);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                dismissDialog();
                cancelRecord();
                changeState(STATE_CANCEL);
                break;
        }

        return super.onTouchEvent(event);
    }

    private boolean wantToCancel(int x, int y) {
        //如果左右滑出 button
        if (x < 0 || x > getWidth()) {
            return true;
        }

        //如果上下滑出 button  加上我们自定义的距离
        return y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL;
    }

    //改变状态
    private void changeState(int state) {
        if (curState == state) {
            return;
        }
        curState = state;

        switch (curState) {
            case STATE_NORMAL:
                setBackgroundResource(R.drawable.btn_recorder_normal);
                setText(R.string.str_recorder_normal);
                break;
            case STATE_RECORDING_NORMAL:
                setBackgroundResource(R.drawable.btn_recording);
                setText(R.string.str_recorder_recording_normal);
                break;
            case STATE_RECORDING_CANCEL:
                setBackgroundResource(R.drawable.btn_recording);
                setText(R.string.str_recorder_recording_cancel);
                break;
            case STATE_CANCEL:
                setBackgroundResource(R.drawable.btn_recorder_normal);
                setText(R.string.str_recorder_normal);
                break;
            case STATE_FINISH:
                setBackgroundResource(R.drawable.btn_recorder_normal);
                setText(R.string.str_recorder_normal);
                break;
        }
    }

    // 录音相关
    private static final int MAX_LENGTH = 1000 * 60 * 10;// 最大录音时长1000*60*10;
    private MediaRecorder mediaRecorder;
    private File dir;
    private String filePath;

    public void setDir(File dir) {
        this.dir = dir;
    }

    public void startRecord() {
        mediaRecorder = new MediaRecorder();
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            File file = new File(dir, generateFileName());
            filePath = file.getAbsolutePath();
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.setMaxDuration(MAX_LENGTH);
            mediaRecorder.prepare();
            mediaRecorder.start();

            long startTime = System.currentTimeMillis();
            updateMicStatus();
            Log.e(TAG, "Start Path: " + filePath + "StartTime: " + startTime);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            mediaRecorder.release();
            dismissDialog();
            if (onAudioRecordStateChangedListener != null) {
                onAudioRecordStateChangedListener.onError(e.getMessage());
            }
            mediaRecorder = null;
            filePath = null;
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            mediaRecorder.release();
            dismissDialog();
            if (onAudioRecordStateChangedListener != null) {
                onAudioRecordStateChangedListener.onError(e.getMessage());
            }
            mediaRecorder = null;
            filePath = null;
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        }
    }

    public void stopRecord() {
        if (mediaRecorder == null) {
            return;
        }

        try {
            mediaRecorder.stop();
            if (onAudioRecordStateChangedListener != null) {
                onAudioRecordStateChangedListener.onFinish(filePath);
            }
            Log.d(TAG, "Stop: " + filePath);
        } catch (RuntimeException e) {
            Log.d(TAG, "Error: " + e.getMessage());
            if (onAudioRecordStateChangedListener != null) {
                onAudioRecordStateChangedListener.onError(e.getMessage());
            }
            // 错误删除文件
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        } finally {
            mediaRecorder.release();
            mediaRecorder = null;
            filePath = null;
        }
    }

    public void cancelRecord() {
        try {
            mediaRecorder.stop();
            if (onAudioRecordStateChangedListener != null) {
                onAudioRecordStateChangedListener.onCancel();
            }
            Log.d(TAG, "Cancel: " + filePath);
        } catch (RuntimeException e) {
            Log.d(TAG, "Error: " + e.getMessage());
            if (onAudioRecordStateChangedListener != null) {
                onAudioRecordStateChangedListener.onError(e.getMessage());
            }
        } finally {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        // 删除文件
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        filePath = null;
    }

    private static String generateFileName() {
        return UUID.randomUUID().toString() + ".amr";
    }

    private final Handler mHandler = new Handler();
    private Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            updateMicStatus();
        }
    };

    private void updateMicStatus() {
        if (mediaRecorder == null) {
            return;
        }
        int level = getVoiceLevel();
        updateVoiceLevel(level);

        // 间隔取样时间
        int SPACE = 100;
        mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
    }

    private int getVoiceLevel() {
        if (mediaRecorder == null) {
            return 1;
        }
        try {
            return 8 * mediaRecorder.getMaxAmplitude() / 32768 + 1;
        } catch (Exception ignored) {
        }

        return 1;
    }

    private OnAudioRecordStateChangedListener onAudioRecordStateChangedListener;

    public void setOnAudioRecordStateChangedListener(OnAudioRecordStateChangedListener onAudioRecordStateChangedListener) {
        this.onAudioRecordStateChangedListener = onAudioRecordStateChangedListener;
    }

    public OnAudioRecordStateChangedListener getOnAudioRecordStateChangedListener() {
        return onAudioRecordStateChangedListener;
    }

    public interface OnAudioRecordStateChangedListener {
        void onFinish(String filePath);

        void onError(String error);

        void onCancel();
    }

    // Dialog相关
    private Dialog dialog;
    private AppCompatImageView ivLevel;
    private AppCompatTextView tvNotice;

    private void showRecordingDialog() {
        Context context = getContext();
        dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_record_audio, null);
        ivLevel = view.findViewById(R.id.iv_level);
        tvNotice = view.findViewById(R.id.tv_notis);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0.0f);
        dialog.getWindow().setLayout(180, 180);
        dialog.setContentView(view);
        dialog.show();
    }

    private void updateVoiceLevel(int level) {
        if (dialog != null && dialog.isShowing()) {
            switch (level) {
                case 1:
                    ivLevel.setImageResource(R.drawable.volume_level_1);
                    break;
                case 2:
                    ivLevel.setImageResource(R.drawable.volume_level_2);
                    break;
                case 3:
                    ivLevel.setImageResource(R.drawable.volume_level_3);
                    break;
                case 4:
                    ivLevel.setImageResource(R.drawable.volume_level_4);
                    break;
                case 5:
                    ivLevel.setImageResource(R.drawable.volume_level_5);
                    break;
                case 6:
                    ivLevel.setImageResource(R.drawable.volume_level_6);
                    break;
                case 7:
                    ivLevel.setImageResource(R.drawable.volume_level_7);
                    break;
                case 8:
                    ivLevel.setImageResource(R.drawable.volume_level_8);
                    break;
            }
        }
    }

    private void updateNoticeText(String text) {
        if (dialog != null && dialog.isShowing()) {
            tvNotice.setText(text);
        }
    }

    private void updateNoticeText(@StringRes int id) {
        if (dialog != null && dialog.isShowing()) {
            tvNotice.setText(id);
        }
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }
}
