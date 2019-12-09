package com.qr.example.audiorecorder;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.qr.core.library.audio.recorder.AudioRecorderButton;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        init();
                    }
                });
    }

    private void init() {
        AudioRecorderButton audioRecorderButton = findViewById(R.id.arb);
        File externalFilesDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(externalFilesDir, "records/");
        audioRecorderButton.setOnAudioRecordStateChangedListener(new AudioRecorderButton.OnAudioRecordStateChangedListener() {
            @Override
            public void onFinish(String filePath) {
                Log.d(TAG,filePath);
            }

            @Override
            public void onError(String error) {
                Log.d(TAG,error);
            }

            @Override
            public void onCancel() {
                Log.d(TAG,"cancel");
            }
        });
    }
}
