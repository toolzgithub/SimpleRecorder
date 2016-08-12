package net.sunniwell.simplerecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 简易录音主类，主要负责开始录音，停止录音，播放录音，停止播放功能
 * 
 * @author 郑鹏超
 * @时间 2016年7月19号
 */
public class MainActivity extends Activity implements OnClickListener {

	protected static final int PLAY_PROGRESSBAR = 99;// 播放进度条what
	private Button mBtnRecording;// 录音按钮
	private Button mBtnPlay;// 播放按钮
	private boolean mIsRecordingState = false;// 是否是录音状态
	private boolean mIsPlayState = true;// 是否是播放状态
	private MediaRecorder mRecorder = null;// 录音操作对象
	private MediaPlayer mPlayer = null;// 媒体播放器对象
	private String mFileName = null;// 录音存储路径
	private String TAG = getClass().getSimpleName();
	private File mFilePath = null;// 文件夹路径
	private ListView mLvPlaylist;// 录音列表
	private PlaylistAdapter mPlaylistAdapter;
	private List<File> mFiles = new ArrayList<>();// 录音文件集合
	private Chronometer mTimer; // 计时器
	private LinearLayout mLlRecording;
	private LinearLayout mLlPlay;
	private File mFile;// 正在播放的文件
	private Button mBtnStopPlay;
	private ProgressBar mPbPlay;// 播放进度条
	private Handler mHandler = new Handler();
	private Runnable mUpdateProgressBarRunnable;// 子线程更新进度条

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
		mLlRecording = (LinearLayout) findViewById(R.id.ll_recording);
		mLlPlay = (LinearLayout) findViewById(R.id.ll_play);
		mLvPlaylist = (ListView) findViewById(R.id.lv_playlist);
		mBtnRecording = (Button) findViewById(R.id.btn_recording);
		mBtnPlay = (Button) findViewById(R.id.btn_play);
		mBtnStopPlay = (Button) findViewById(R.id.btn_stop_play);
		mPbPlay = (ProgressBar) findViewById(R.id.pb_play);
		mPbPlay.setProgress(0);
		mFilePath = getFilePath();
		timing();
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
		mBtnStopPlay.setOnClickListener(this);
		mLvPlaylist.setOnItemClickListener(new LvPlaylistItmeListener());
		mLvPlaylist.setOnItemLongClickListener(new LvPlaylistItmeLongListener());
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_recording:
			// 判断录音按钮的状态，根据相应的状态处理事务
			// mBtnRecording.setText(R.string.wait_for);
			mBtnRecording.setEnabled(false);
			if (mIsRecordingState) {
				mBtnRecording.setText(R.string.start_recording);
				stopRecording();
			} else {
				mBtnRecording.setText(R.string.stop_recording);
				startRecording();
			}
			mIsRecordingState = !mIsRecordingState;
			mBtnRecording.setEnabled(true);
			break;

		case R.id.btn_play: // 判断播放按钮的状态，根据相应的状态处理事务 //
			mBtnPlay.setEnabled(false);
			if (mIsPlayState) {
				pausePlay();
				mBtnPlay.setText(R.string.continue_play);
			} else {
				mPlayer.start();
				mBtnPlay.setText(R.string.pause_play);
			}
			mIsPlayState = !mIsPlayState;
			mBtnPlay.setEnabled(true);
			break;
		case R.id.btn_stop_play: // 停止播放
			stopPlay();
			mIsPlayState = !mIsPlayState;
			mBtnPlay.setText(R.string.pause_play);
			mLlRecording.setVisibility(View.VISIBLE);
			mLlPlay.setVisibility(View.GONE);
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
			// Toast.makeText(getApplicationContext(), mFileName, 1).show();
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
		mTimer.setBase(SystemClock.elapsedRealtime()); // 清空计时器
		mTimer.start();
	}

	/**
	 * 创建计时器
	 */
	private void timing() {
		// 获得计时器对象
		mTimer = (Chronometer) this.findViewById(R.id.c_duration);
	}

	/**
	 * 停止录音
	 */
	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();// 释放资源
		mTimer.stop();// 停止计时
		mTimer.setBase(SystemClock.elapsedRealtime());
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
	private void startPlay(File fileName) {
		if (mPlayer != null) {
			stopPlay();
		}
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(new FileInputStream(fileName).getFD());// 设置多媒体数据来源
			mPlayer.prepare(); // 准备
			mPlayer.start(); // 开始
			updateProgressBar();
		} catch (IOException e) {
			Log.e(TAG, getString(R.string.e_play));
		}
		// 播放完成，改变按钮状态
		mPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				mLlRecording.setVisibility(View.VISIBLE);
				mLlPlay.setVisibility(View.GONE);
				mIsPlayState = !mIsPlayState;
				mBtnPlay.setText(R.string.pause_play);
			}
		});
	}

	/**
	 * 功能：更新进度条
	 * 
	 * @author 郑鹏超
	 * @时间 2016年8月12日
	 */
	private void updateProgressBar() {
		mPbPlay.setMax(mPlayer.getDuration());// 设置进度条最大值
		mUpdateProgressBarRunnable = new Runnable() {
			@Override
			public void run() {
				mPbPlay.setProgress(mPlayer.getCurrentPosition());
				if (mPlayer.getCurrentPosition() <= mPlayer.getDuration()) {
					mHandler.postDelayed(mUpdateProgressBarRunnable, 100);// 发送异步消息，实现实时更新进度条
				}
			}
		};
		mHandler.post(mUpdateProgressBarRunnable);// 开启子线程更新进度条
	}

	/**
	 * 暂停播放
	 */
	private void pausePlay() {
		mPlayer.pause();
	}

	/**
	 * 停止播放
	 */
	private void stopPlay() {
		mPlayer.release();
		mPlayer = null;
		mHandler.removeCallbacksAndMessages(null);// 清除或有Handler消息
		mPbPlay.setProgress(0);
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
			SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");// 初始化Formatter的转换格式。
			String time = "";
			if (getRecordingTime(file) > 1000) {
				time = formatter.format(getRecordingTime(file));
			} else {
				time = "<00:01";
			}
			playlistViews.mTvRecordingDuration.setText(time);
			return convertView;
		}

		/**
		 * 功能：获得音频文件的时长
		 * 
		 * @author 郑鹏超
		 * @param file
		 * @return
		 * @时间 2016年8月11日
		 */
		private int getRecordingTime(File file) {
			MediaPlayer mediaPlayer = new MediaPlayer();
			int time = -1;
			try {
				System.out.println(file.toString());
				mediaPlayer.setDataSource(new FileInputStream(file).getFD());// 设置多媒体数据来源
																				// getFD()返回底层文件描述符
				mediaPlayer.prepare(); // 准备
				time = mediaPlayer.getDuration();
				mediaPlayer.release();
				mediaPlayer = null;
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, getString(R.string.e_play));
			}
			return time;
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

	/**
	 * 功能：LvPlaylist的单条目点击监听，用于播放录音
	 * 
	 * @author 郑鹏超
	 */
	private class LvPlaylistItmeListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mLlRecording.setVisibility(View.GONE);
			mLlPlay.setVisibility(View.VISIBLE);
			mFile = mFiles.get(position);
			startPlay(mFile);
		}
	}

	/**
	 * 功能：LvPlaylist的单条目长按监听，用于删除录音文件
	 * 
	 * @author 郑鹏超
	 */
	private class LvPlaylistItmeLongListener implements OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			showUninstallDialog(position);
			return true;
		}

	}

	/**
	 * 功能：删除窗口
	 * 
	 * @author 郑鹏超
	 */
	private void showUninstallDialog(final int position) {
		final File file = mFiles.get(position);
		AlertDialog.Builder builder = new Builder(MainActivity.this);
		builder.setMessage("你是否要删除“" + file.getName() + "”?");
		builder.setTitle("删除");
		builder.setIcon(android.R.drawable.ic_menu_delete);
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				file.delete();
				mFiles.remove(position);
				mPlaylistAdapter.notifyDataSetChanged();
			}
		});
		builder.create().show();
	}

}
