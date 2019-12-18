# AudioRecorder

## 录音Button

### XML引用
     <com.qr.core.library.audio.AudioRecorderButton
          android:id="@+id/arb_content"
          android:layout_width="match_parent"
          android:layout_height="wrap_content" />

### 代码设置
     AudioRecorderButton audioRecorderButton = footerViewHolder.getView(R.id.arb_content);
     File externalFilesDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
     File file = new File(externalFilesDir, "records/");
     audioRecorderButton.setDir(file);// 设置录音存储目录
     audioRecorderButton.setRecordStateChangedListener(new AudioRecorderButton.OnAudioRecordStateChangedListener() {
         @Override
         public void onPermissionDenied(String permission) {
             // 无录音相关权限  应该进行权限申请
         }
    
         @Override
         public void onStart(String filePath) {
             // 录音开始
             Log.d(TAG, "Start: " + filePath);
             Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
         }
    
         @Override
         public void onUpdate(String filePath, long duration) {
             // 录音中 duration是当前录音时长 100ms更新一次
             Log.d(TAG, "Update: " + filePath + " Duration: " + duration);
         }
    
         @Override
         public void onFinish(String filePath, long duration) {
             // 录音结束 duration是总录音时长
             Log.d(TAG, filePath);
             Toast.makeText(MainActivity.this, filePath, Toast.LENGTH_SHORT).show();
         }
    
         @Override
         public void onError(String error) {
             // 录音错误 
             Log.d(TAG, error);
             Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
         }
    
         @Override
         public void onCancel() {
             // 录音取消
             Log.d(TAG, "cancel");
             Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
         }
     }); 
     
## 播放TextView

### XML引用
    <com.qr.core.library.audio.AudioPlayerTextView
         android:id="@+id/aptv_content"
         android:layout_width="wrap_content"
         android:layout_height="wrap_content"
         android:layout_margin="8dp"
         android:background="@color/colorPrimary"
         android:textAlignment="center"
         android:textColor="#fff" />
         
### 代码设置
    AudioPlayerTextView audioPlayerTextView = contentViewHolder.getView(R.id.aptv_content);
    audioPlayerTextView.setDataSource(item); 
    
## 截图    

![Image text]()
![Image text]()
