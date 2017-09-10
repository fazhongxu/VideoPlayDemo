package com.xxl.videoplaydemo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xxl.videoplaydemo.utils.PixelUtil;
import com.xxl.videoplaydemo.view.CustomVideoView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.video_view)
    CustomVideoView videoView;
    @BindView(R.id.sb_progress)
    SeekBar sbProgress;
    @BindView(R.id.tv_current_play_time)
    TextView tvCurrentPlayTime;
    @BindView(R.id.tv_total_play_time)
    TextView tvTotalPlayTime;
    @BindView(R.id.iv_screen_orientation)
    ImageView ivScreenOrientation;
    @BindView(R.id.rl_container)
    RelativeLayout rlContainer;
    private static final int VIDEO_HANDLER = 100;
    private boolean mIsCanAjust = true;//是否可以调节音量和亮度
    private static final int CRITICAL_VALUE = 30;//手指滑动屏幕响应调节亮度和音量的临界值
    private int mScreenWidth;
    private int mScreenHeight;
    private static final String TAG = "VideoDemo";

    private static final String VIDEO_PATH = "http://2449.vod.myqcloud.com/2449_43b6f696980311e59ed467f22794e792.f20.mp4";
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case VIDEO_HANDLER:
                    int totalDuration = videoView.getDuration();
                    int currentPosition = videoView.getCurrentPosition();
                    updateVideoDuration(tvTotalPlayTime, totalDuration);
                    updateVideoDuration(tvCurrentPlayTime, currentPosition);
                    sbProgress.setMax(totalDuration);
                    sbProgress.setProgress(currentPosition);
                    break;
            }
            mHandler.sendEmptyMessageDelayed(VIDEO_HANDLER, 500);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initData();
        initEvent();
    }

    private void initData() {
        videoView.setVideoPath(VIDEO_PATH);
        videoView.start();
        mHandler.sendEmptyMessage(VIDEO_HANDLER);
        getScreenSize();
    }

    /**
     * 获取屏幕宽高
     */
    private void getScreenSize() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;

        Log.e(TAG, "screenWidth" + mScreenWidth);
        Log.e(TAG, "screenHeight" + mScreenHeight);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(VIDEO_HANDLER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView.isPlaying()) {
            mHandler.sendEmptyMessage(VIDEO_HANDLER);
        }
    }

    private void initEvent() {
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeMessages(VIDEO_HANDLER);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                videoView.seekTo(seekBar.getProgress());
                mHandler.sendEmptyMessage(VIDEO_HANDLER);
            }
        });
    }

    private void updateVideoDuration(TextView textView, int duration) {
        int totalSecond = duration / 1000;
        int second = totalSecond % 60;
        int minute = (totalSecond / 60) % 60;
        int hour = totalSecond / 3600;

        String time = "";
        if (hour != 0) {
            time = String.format("%02d:%02d:%02d", hour, minute, second);
        } else {
            time = String.format("%02d:%02d", minute, second);
        }
        textView.setText(time);
    }


    float x = 0;
    float y = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - x;//x方向偏移量
                float deltaY = event.getY() - y;
                float absDeltaX = Math.abs(deltaX);
                float absDeltaY = Math.abs(deltaY);
                if (absDeltaY > CRITICAL_VALUE && absDeltaY > CRITICAL_VALUE) {//滑动的距离大于设置的临界值
                    if (absDeltaY > absDeltaX) {//是上下方向滑动
                        mIsCanAjust = true;
                    } else {
                        mIsCanAjust = false;
                    }
                } else if (absDeltaY > CRITICAL_VALUE && absDeltaX < CRITICAL_VALUE) {
                    mIsCanAjust = true;
                } else if (absDeltaY < CRITICAL_VALUE && absDeltaX > CRITICAL_VALUE) {
                    mIsCanAjust = false;
                }
                if (mIsCanAjust) {
                    if (x < mScreenWidth / 2) {//可以调节音量（设置右边调节音量，左边调节亮度
                        if (deltaY > 0) {
                            Log.e(TAG, "减小亮度");
                        } else {
                            Log.e(TAG, "增大亮度");
                        }
                        adjustmentBrightness(-deltaY);
                    } else if (x > mScreenWidth / 2) {//可以调节音量
                        if (deltaY > 0) {//是从上往下滑动，即减小亮度
                            Log.e(TAG, "减小音量");
                        } else {
                            Log.e(TAG, "增大音量");
                        }
                        adjusmentVolume(-deltaY);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    /**
     * 音量调节
     */
    private void adjusmentVolume(float deltaY) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);//获取系统音量最大值
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);//获取系统当前音量
        int index = (int) (deltaY / mScreenHeight * streamMaxVolume * 3);//乘以3是为了增加偏移量
        int volume = Math.max(currentVolume + index, 0);
        Toast.makeText(this, "当前音量"+volume, Toast.LENGTH_SHORT).show();
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    /**
     * 亮度调节
     */
    private void adjustmentBrightness(float deltaY) {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        float screenBrightness = attributes.screenBrightness;//当前屏幕亮度
        float index = deltaY / mScreenHeight / 3;// 除以3是为了减弱调节的亮度
        screenBrightness += index;
        if (screenBrightness > 1.0f) {//防止滑动超过亮度范围滑动一段时间亮度不改变
            screenBrightness = 1.0f;
        }
        if (screenBrightness < 0.01f) {
            screenBrightness = 0.0f;
        }
        attributes.screenBrightness = screenBrightness;
        Toast.makeText(this, "当前亮度"+String.format("%2.0f",screenBrightness * 100)+"%", Toast.LENGTH_SHORT).show();
        getWindow().setAttributes(attributes);
    }

    @OnClick(R.id.iv_screen_orientation)
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_screen_orientation:
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setVideoSize(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ivScreenOrientation.setBackgroundResource(R.drawable.ic_portrait);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            setVideoSize(ViewGroup.LayoutParams.MATCH_PARENT, PixelUtil.dip2px(this, 240.0f));
            ivScreenOrientation.setBackgroundResource(R.drawable.ic_full_screen);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }

    /**
     * 设置视频大小
     *
     * @param width
     * @param height
     */
    private void setVideoSize(int width, int height) {
        //设置视频的宽高
        ViewGroup.LayoutParams videoViewLP = videoView.getLayoutParams();
        videoViewLP.width = width;
        videoViewLP.height = height;
        videoView.setLayoutParams(videoViewLP);
        //设置视屏外面一层布局的宽高，因为外面一层布局有进度条和布局和控件
        ViewGroup.LayoutParams rlContainerLP = rlContainer.getLayoutParams();
        rlContainerLP.width = width;
        rlContainerLP.height = height;
        rlContainer.setLayoutParams(rlContainerLP);
    }
}
