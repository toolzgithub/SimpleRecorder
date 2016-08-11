package net.sunniwell.simplerecorder;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
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
	private File mFilePath = null;// 文件夹路径
	private ListView mLvPlaylist;// 录音列表
	private PlaylistAdapter mPlaylistAdapter;
	private List<File> mFiles = new ArrayList<>();// 录音文件集合

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		searchFiles();
		initData();
		setPlaylistClickListener();
	}

	/**
	 * 功能：检索录音文件夹，获得所有的文件
	 * 
	 * @author 郑鹏超
	 * @时间 2016年8月10日
	 */
	private void searchFiles() {
		mFiles.addAll(Arrays.asList(mFilePath.listFiles()));// 获得所有文件
		// Log.d("LY", mFiles.length + "个");
		// for (File file : mFiles) {
		//// Log.d("LY", file.getName());
		// }
	}

	private void initView() {
		mLvPlaylist = (ListView) findViewById(R.id.lv_playlist);
		mBtnRecording = (Button) findViewById(R.id.btn_recording);
		mBtnPlay = (Button) findViewById(R.id.btn_play);
		mFilePath = getFilePath();
	}

	/**
	 * 功能：获得存储路径
	 * 
	 * @author 郑鹏超
	 * @时间 2016年8月11日
	 */
	private File getFilePath() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			String path = Environment.getExternalStorageDirectory().getAbsolutePath();
			File filePath = new File(path + "/JYLY");
			if (!filePath.exists()) {
				filePath.mkdirs();
			}
			return filePath;
		}
		return null;
	}

	/**
	 * 功能：初始化数据
	 * 
	 * @author 郑鹏超
	 * @时间 2016年8月11日
	 */
	private void initData() {
		mPlaylistAdapter = new PlaylistAdapter(mFiles);
		mLvPlaylist.setAdapter(mPlaylistAdapter);
	}

	private void setPlaylistClickListener() {
		mBtnRecording.setOnClickListener(this);
		mBtnPlay.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_recording:
			// 判断录音按钮的状态，根据相应的状态处理事务
			// mBtnRecording.setText(R.string.wait_for);
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
			// mBtnPlay.setText(R.string.wait_for);
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
		// 设置sdcard的路径

		if (mFilePath == null) {
			Toast.makeText(getApplicationContext(), R.string.no_sd, Toast.LENGTH_SHORT).show();
		} else {
			Timestamp tamp = new Timestamp(System.currentTimeMillis());
			mFileName = mFilePath + "/LY" + tamp + ".3gp";
			Log.d("LY", mFileName);
			Toast.makeText(getApplicationContext(), mFileName, 1).show();
			// 设置录制的音频文件的保存位置。
			mRecorder.setOutputFile(mFileName);
		}
		// 设置所录制的声音的编码格式。
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		try {
			mRecorder.prepare();
		} catch (Exception e) {
			Log.e(TAG, getString(R.string.e_recording));
			e.printStackTrace();
		}
		mRecorder.start();// 开始录音
	}

	/**
	 * 停止录音
	 */
	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();// 释放资源
		File file = new File(mFileName);
		Log.d("LY", file.getName());
		mFiles.add(file);
		mPlaylistAdapter.notifyDataSetChanged();
		mFileName = mFilePath.toString();
		// Log.e("LY", mFileName);
		mRecorder = null;
	}

	/**
	 * 开始播放
	 */
	private void startPlay() {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(mFileName);// 设置多媒体数据来源
			mPlayer.prepare(); // 准备
			mPlayer.start(); // 开始
		} catch (IOException e) {
			Log.e(TAG, getString(R.string.e_play));
		}
		// 播放完成，改变按钮状态
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

	/**
	 * 功能：录音列表适配器
	 * 
	 * @author 郑鹏超
	 */
	private class PlaylistAdapter extends BaseAdapter {

		private List<File> files;

		/**
		 * 功能：构造方法用来传递要显示的数据
		 * 
		 * @author 郑鹏超
		 * @时间 2016年8月11日
		 */
		public PlaylistAdapter(List<File> files) {
			this.files = files;
		}

		@Override
		public int getCount() {
			return files.size();
		}

		@Override
		public File getItem(int position) {
			return files.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			File file = getItem(position);
			PlaylistViews playlistViews;
			if (convertView == null) {
				convertView = View.inflate(MainActivity.this, R.layout.itme_lv_playlist, null);
				playlistViews = new PlaylistViews();
				playlistViews.mTvRecordingName = (TextView) convertView.findViewById(R.id.tv_recording_name);
				playlistViews.mTvRecordingDuration = (TextView) convertView.findViewById(R.id.tv_recording_duration);
				convertView.setTag(playlistViews);
			} else {
				playlistViews = (PlaylistViews) convertView.getTag();
			}
			playlistViews.mTvRecordingName.setText(file.getName());
			return convertView;
		}

	}

	/**
	 * 功能：用来优化listview的类
	 * 
	 * @author 郑鹏超
	 */
	private class PlaylistViews {
		TextView mTvRecordingName;
		TextView mTvRecordingDuration;
	}

}
