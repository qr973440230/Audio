package com.qr.core.library.audio;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.UUID;

public class AudioRecorderButton extends AppCompatButton {
    private static final String TAG = AudioRecorderButton.class.getSimpleName();


    public AudioRecorderButton(Context context) {
        this(context, null);
    }

    public AudioRecorderButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.audio_btn_recorder_normal);
        setText(R.string.audio_str_recorder_normal);


        // 解决RecyclerView吞噬触摸事件的问题
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
        });
    }

    // 触摸逻辑和状态
    // 滑动距离
    private static final int DISTANCE_Y_CANCEL = 100;
    // 状态
    private static final int STATE_NORMAL = 1;
    private static final int STATE_RECORDING_NORMAL = 2;
    private static final int STATE_RECORDING_CANCEL = 3;
    private static final int STATE_CANCEL = 4;
    private static final int STATE_FINISH = 5;
    // 当前状态
    private int curState = STATE_NORMAL;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 显示Dialog
                // 检查权限
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                    // 无录音权限 发生错误
                    if (recordStateChangedListener != null) {
                        recordStateChangedListener.onPermissionDenied(Manifest.permission.RECORD_AUDIO);
                    }

                    return false;
                }
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    if (recordStateChangedListener != null) {
                        recordStateChangedListener.onPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }

                    return false;
                }
                startRecord();
                changeState(STATE_RECORDING_NORMAL);
                break;
            case MotionEvent.ACTION_MOVE:
                if (curState == STATE_RECORDING_NORMAL) {
                    if (wantToCancel(x, y)) {
                        updateNoticeText(R.string.audio_str_recorder_recording_cancel);
                        changeState(STATE_RECORDING_CANCEL);
                    }
                } else {
                    if (!wantToCancel(x, y)) {
                        updateNoticeText(R.string.audio_str_recorder_recording_normal);
                        changeState(STATE_RECORDING_NORMAL);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
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
                cancelRecord();
                changeState(STATE_CANCEL);
                break;
        }

        return super.onTouchEvent(event);
    }

    private boolean wantToCancel(int x, int y) {
        if (x < 0 || x > getWidth()) {
            return true;
        }

        return y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL;
    }

    private void changeState(int state) {
        if (curState == state) {
            return;
        }
        curState = state;

        switch (curState) {
            case STATE_NORMAL:
                setBackgroundResource(R.drawable.audio_btn_recorder_normal);
                setText(R.string.audio_str_recorder_normal);
                break;
            case STATE_RECORDING_NORMAL:
                setBackgroundResource(R.drawable.audio_btn_recording);
                setText(R.string.audio_str_recorder_recording);
                break;
            case STATE_RECORDING_CANCEL:
                setBackgroundResource(R.drawable.audio_btn_recording);
                setText(R.string.audio_str_recorder_recording_cancel);
                break;
            case STATE_CANCEL:
                setBackgroundResource(R.drawable.audio_btn_recorder_normal);
                setText(R.string.audio_str_recorder_normal);
                break;
            case STATE_FINISH:
                setBackgroundResource(R.drawable.audio_btn_recorder_normal);
                setText(R.string.audio_str_recorder_normal);
                break;
        }
    }

    // 录音相关
    private static final int MAX_LENGTH = 1000 * 60 * 10;// 最大录音时长1000*60*10;
    private MediaRecorder mediaRecorder;
    private File dir;
    private String filePath;
    private long startTime;

    public void setDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.dir = dir;
    }

    private void startRecord() {
        if (mediaRecorder != null) {
            // 释放原先的资源
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }

        // 创建新得MediaPlayer
        mediaRecorder = new MediaRecorder();
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            if (dir == null) {
                dir = getContext().getExternalCacheDir();
            }

            File file = new File(dir, generateFileName());
            filePath = file.getAbsolutePath();
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.setMaxDuration(MAX_LENGTH);
            mediaRecorder.prepare();
            mediaRecorder.start();

            startTime = System.currentTimeMillis();
            Log.e(TAG, "Start Path: " + filePath + "StartTime: " + startTime);
            if (recordStateChangedListener != null) {
                recordStateChangedListener.onStart(filePath);
            }

            // 录音开启成功 显示Dialog
            showRecordingDialog();

            // 开始更新状态
            updateMicStatus();
        } catch (Exception e) {
            e.printStackTrace();
            if (recordStateChangedListener != null) {
                recordStateChangedListener.onError(e.getMessage());
            }

            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            filePath = null;
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        }
    }

    private void stopRecord() {
        // 关闭Dialog
        dismissDialog();

        if (mediaRecorder == null) {
            return;
        }

        try {
            mediaRecorder.stop();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            if (duration < 1000) {
                throw new RuntimeException("录制时间太短 : " + duration + "ms");
            }

            if (recordStateChangedListener != null) {
                recordStateChangedListener.onFinish(filePath, duration);
            }

            Log.d(TAG, "Stop: " + filePath);
        } catch (Exception e) {
            Log.d(TAG, "Error: " + e.getMessage());
            if (recordStateChangedListener != null) {
                recordStateChangedListener.onError(e.getMessage());
            }

            // 录音发生错误 删除错误文件
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        } finally {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            filePath = null;
        }
    }

    private void cancelRecord() {
        // 关闭Dialog
        dismissDialog();

        if (mediaRecorder == null) {
            return;
        }

        try {
            mediaRecorder.stop();
            if (recordStateChangedListener != null) {
                recordStateChangedListener.onCancel();
            }
            Log.d(TAG, "Cancel: " + filePath);
        } catch (Exception e) {
            Log.d(TAG, "Error: " + e.getMessage());
            if (recordStateChangedListener != null) {
                recordStateChangedListener.onError(e.getMessage());
            }
        } finally {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;

            // 删除文件
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            filePath = null;
        }
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

        int level = getVoiceLevel(levelResource.length);
        updateVoiceLevel(level);

        if (recordStateChangedListener != null) {
            recordStateChangedListener.onUpdate(filePath, System.currentTimeMillis() - startTime);
        }

        // 间隔取样时间
        int SPACE = 100;
        mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
    }

    private int getVoiceLevel(int maxLevel) {
        if (mediaRecorder == null) {
            return 0;
        }
        try {
            return maxLevel * mediaRecorder.getMaxAmplitude() / 32768;
        } catch (Exception ignored) {
        }

        return 0;
    }

    // Dialog相关
    private static int[] levelResource = {
            R.drawable.audio_volume_record_level_1,
            R.drawable.audio_volume_record_level_2,
            R.drawable.audio_volume_record_level_3,
            R.drawable.audio_volume_record_level_4,
            R.drawable.audio_volume_record_level_5,
            R.drawable.audio_volume_record_level_6,
            R.drawable.audio_volume_record_level_7,
            R.drawable.audio_volume_record_level_8,
    };

    private Dialog dialog;
    private AppCompatImageView ivLevel;
    private AppCompatTextView tvNotice;

    private void showRecordingDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        dialog = new Dialog(context);
        Window window = dialog.getWindow();
        if (window == null) {
            dialog = null;
            return;
        }

        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.audio_dialog_record_audio, null);
        ivLevel = view.findViewById(R.id.iv_level);
        tvNotice = view.findViewById(R.id.tv_notis);
        window.requestFeature(Window.FEATURE_NO_TITLE);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setDimAmount(0.0f);
        window.setLayout(180, 180);
        dialog.setContentView(view);
        dialog.show();
    }

    private void updateVoiceLevel(int level) {
        if (dialog != null && dialog.isShowing() && ivLevel != null) {
            ivLevel.setImageResource(levelResource[level]);
        }
    }

    private void updateNoticeText(@StringRes int id) {
        if (dialog != null && dialog.isShowing() && tvNotice != null) {
            tvNotice.setText(id);
        }
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }

    // 状态通知相关
    private OnAudioRecordStateChangedListener recordStateChangedListener;

    public void setRecordStateChangedListener(OnAudioRecordStateChangedListener recordStateChangedListener) {
        this.recordStateChangedListener = recordStateChangedListener;
    }

    public interface OnAudioRecordStateChangedListener {
        void onPermissionDenied(String permission);

        void onStart(String filePath);

        void onUpdate(String filePath, long duration);

        void onFinish(String filePath, long duration);

        void onError(String error);

        void onCancel();
    }
}
