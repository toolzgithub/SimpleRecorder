package net.sunniwell.simplerecorder;

import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * 简易录音主类，主要负责开始录音，停止录音，播放录音，停止播放功能
 * 
 * @author 郑鹏超
 * @时间 2016年7月19号
 */
public class MainActivity extends Activity implements OnClickListener {

	private Button mBtnRecording;// 录音按钮
	private Button mBtnPlay;// 播放按钮
	private boolean mIsRecordingState = false;// 是否是录音状态
	private boolean mIsPlayState = false;// 是否是播放状态
	private MediaRecorder mRecorder = null;// 录音操作对象
	private MediaPlayer mPlayer = null;// 媒体播放器对象
	private String mFileName = null;// 录音存储路径
	private String TAG = getClass().getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		click();
	}

	private void initView() {
		mBtnRecording = (Button) findViewById(R.id.btn_recording);
		mBtnPlay = (Button) findViewById(R.id.btn_play);
		// 设置sdcard的路径
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
			mFileName += "/wohaoshuai.3gp";
		}
	}

	private void click() {
		mBtnRecording.setOnClickListener(this);
		mBtnPlay.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_recording:
			// 判断录音按钮的状态，根据相应的状态处理事务
			mBtnRecording.setText(R.string.wait_for);
			mBtnRecording.setEnabled(false);
			if (mIsRecordingState) {
				stopRecording();
				mBtnRecording.setText(R.string.start_recording);
			} else {
				startRecording();
				mBtnRecording.setText(R.string.stop_recording);
			}
			mIsRecordingState = !mIsRecordingState;
			mBtnRecording.setEnabled(true);
			break;
		case R.id.btn_play:
			// 判断播放按钮的状态，根据相应的状态处理事务
			mBtnPlay.setText(R.string.wait_for);
			mBtnPlay.setEnabled(false);
			if (mIsPlayState) {
				stopPlay();
				mBtnPlay.setText(R.string.start_play);
			} else {
				startPlay();
				mBtnPlay.setText(R.string.stop_play);
			}
			mIsPlayState = !mIsPlayState;
			mBtnPlay.setEnabled(true);
			break;

		default:
			break;
		}
	}

	/**
	 * 开始录音
	 */
	private void startRecording() {
		mRecorder = new MediaRecorder();
		// 设置声音来源
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		// 设置所录制的音视频文件的格式。(3gp)
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		// 设置录制的音频文件的保存位置。
		if (mFileName == null) {
			Toast.makeText(getApplicationContext(), R.string.no_sd, Toast.LENGTH_SHORT).show();
		} else {
			mRecorder.setOutputFile(mFileName);
			Log.d(TAG, mFileName);
		}
		// 设置所录制的声音的编码格式。
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			mRecorder.prepare();
		} catch (Exception e) {
			Log.e(TAG, getString(R.string.e_recording));
		}
		mRecorder.start();// 开始录音
	}

	/**
	 * 停止录音
	 */
	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();//释放资源
		mRecorder = null;
	}

	/**
	 * 开始播放
	 */
	private void startPlay() {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(mFileName);//设置多媒体数据来源
			mPlayer.prepare(); //准备 
            mPlayer.start();  //开始
		} catch (IOException e) {
			Log.e(TAG,getString(R.string.e_play));  
		}
		//播放完成，改变按钮状态
		mPlayer.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				mIsPlayState = !mIsPlayState;
				mBtnPlay.setText(R.string.start_play);
			}
		});
	}

	/**
	 * 停止播放
	 */
	private void stopPlay() {
		 mPlayer.release();  
         mPlayer = null;  
	}

}

