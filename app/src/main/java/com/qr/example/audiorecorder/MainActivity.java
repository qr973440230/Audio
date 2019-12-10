package com.qr.example.audiorecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qr.core.library.audio.AudioPlayerTextView;
import com.qr.core.library.audio.AudioRecorderButton;
import com.qr.library.adapter.QuickAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    @SuppressLint("CheckResult")
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
        RecyclerView recyclerView = findViewById(R.id.rv_content);
        List<String> list = new ArrayList<>();
        QuickAdapter<String> quickAdapter = new QuickAdapter<>(R.layout.item_content, -1, R.layout.footer_rab, list);
        quickAdapter.setOnContentConvertListener(new QuickAdapter.OnContentConvertListener<String>() {
            @Override
            public void onContentConvert(QuickAdapter.ContentViewHolder contentViewHolder, String item) {
                AudioPlayerTextView audioPlayerTextView = contentViewHolder.getView(R.id.aptv_content);
                audioPlayerTextView.setDataSource(item);
            }
        });
        quickAdapter.setOnFooterConvertListener(new QuickAdapter.OnFooterConvertListener() {
            @Override
            public void onFooterConvert(QuickAdapter.FooterViewHolder footerViewHolder) {
                AudioRecorderButton audioRecorderButton = footerViewHolder.getView(R.id.arb_content);
                File externalFilesDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                File file = new File(externalFilesDir, "records/");
                audioRecorderButton.setDir(file);
                audioRecorderButton.setRecordStateChangedListener(new AudioRecorderButton.OnAudioRecordStateChangedListener() {
                    @Override
                    public void onStart(String filePath) {
                        Log.d(TAG, "Start: " + filePath);
                        Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onUpdate(String filePath, long duration) {
                        Log.d(TAG, "Update: " + filePath + " Duration: " + duration);
                    }

                    @Override
                    public void onFinish(String filePath, long duration) {
                        Log.d(TAG, filePath);
                        quickAdapter.addData(filePath);
                        Toast.makeText(MainActivity.this, filePath, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, error);
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "cancel");
                        Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        recyclerView.setAdapter(quickAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}
