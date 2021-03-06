package com.ezreal.audiorecordbutton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;


/**
 * 自定义语音录制按钮
 */

@SuppressLint("AppCompatCustomView")
public class AudioRecordButton extends Button implements AudioRecordManager.OnAudioStateListener{

    private static final int STATE_NORMAL = 100001;//普通状态
    private static final int STATE_RECORDING = 100002;//录音中
    private static final int STATE_WANT_CANCEL = 100003;//取消录音
    private static final int CANCEL_HEIGHT = 50;//定义上滑取消距离

    private static final int MSG_AUDIO_PREPARED = 100004;
    private static final int MSG_VOICE_CHANGE = 100005;
    private static final int MSG_DIALOG_DISMISS = 100006;


    private int mCurrentState = STATE_NORMAL; // 当前状态，默认为正常
    private boolean isReady = false; //是否触发OnLongClick“长按触发”事件
    private boolean isRecording = false; //是否在录音状态
    private RecordDialogManager mDialogManager;
    private AudioRecordManager mAudioRecordManager;//录音管理类，AudioRecordManager.java创建
    private AudioManager mAudioManager;
    private String mAudioSaveDir;
    private long mRecordTime;//记录录音时长
    private OnRecordingListener mRecordingListener;//录音结果监听器，正常结束、发生错误
    private String mAudioFilePath;
    private boolean hasInit = false;

    public AudioRecordButton(Context context) {//自定义单参数按钮
        this(context, null);
    }

    public AudioRecordButton(Context context, AttributeSet attrs) {//自定义双参数按钮
        this(context, attrs, 0);
    }

    public AudioRecordButton(Context context, AttributeSet attrs, int defStyleAttr) {//自定义三参数按钮
        super(context, attrs, defStyleAttr);
        // 初始化按钮样式
        setBackgroundResource(R.drawable.record_button_normal);//加载背景
        setText(getResources().getString(R.string.press_record));
    }

    /**
     * 设置录音回调
     *
     * @param listener 回调监听
     */
    public void setRecordingListener(OnRecordingListener listener) {
        this.mRecordingListener = listener;
    }

    /**
     * 按钮初始化
     *
     * @param audioSaveDir 录音文件保存路径
     */
    public void init(String audioSaveDir) {
        mAudioSaveDir = audioSaveDir;
        // 初始化 dialog 管理器
        mDialogManager = new RecordDialogManager(getContext());
        // 获取音频管理，以申请音频焦点
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        // 初始化录音管理器
        mAudioRecordManager = AudioRecordManager.getInstance(mAudioSaveDir);

        mAudioRecordManager.setAudioStateListener(this);

        // 设置按钮长按事件监听，只有触发长按才开始准备录音
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 获取焦点
                int focus = mAudioManager.requestAudioFocus(null,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                if (focus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){//成功获取焦点
                    isReady = true;
                    mAudioRecordManager.prepareAudio();//准备录音
                }else if (focus == AudioManager.AUDIOFOCUS_REQUEST_FAILED){//获取焦点失败
                    if (mRecordingListener != null) {
                        mRecordingListener.recordError("AUDIO_FOCUS_REQUEST_FAILED");
                    }
                }
                return true;
            }
        });
        hasInit = true;//按钮初始化完成
    }

    // 子线程 runnable，每隔0.1秒获取音量大小，并记录录音时间
    private Runnable mGetVoiceLevelRunnable = new Runnable() {
        @Override
        public void run() {
            while (isRecording) {//在录音准备工作完成后，启动线程，isRecording被修改为true
                try {
                    Thread.sleep(100);
                    mRecordTime += 100;//录音时长
                    mHandler.sendEmptyMessage(MSG_VOICE_CHANGE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_AUDIO_PREPARED:
                    // 录音管理器 prepare 成功，开始录音并显示dialog
                    // 启动线程记录时间并获取音量变化
                    isRecording = true;
                    mDialogManager.showDialogRecord();
                    // 启动线程，每隔0.1秒获取音量大小
                    new Thread(mGetVoiceLevelRunnable).start();
                    break;
                case MSG_VOICE_CHANGE:
                    mDialogManager.updateVoiceLevel(mAudioRecordManager.getVoiceLevel(7));//修改音量
                    break;
                case MSG_DIALOG_DISMISS:
                    mDialogManager.dismissDialog();
                    break;
            }
        }
    };

    // 录音准备出错时回调
    @Override
    public void prepareError(String message) {
        if (mRecordingListener != null) {
            mRecordingListener.recordError(message);
        }
    }

    // 录音准备完成后回调
    @Override
    public void prepareFinish(String audioFilePath) {
        mAudioFilePath = audioFilePath;
        mHandler.sendEmptyMessage(MSG_AUDIO_PREPARED);
    }

    /**
     * 监听屏幕触摸事件
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!hasInit) {
            return true;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN://按下
                changeState(STATE_RECORDING);
                break;
            case MotionEvent.ACTION_MOVE://移动时
                if (isRecording) {
                    if (isWantToCancel(x, y)) {
                        changeState(STATE_WANT_CANCEL);//根据(x,y)坐标，判断是否想要取消
                    } else {
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP://抬起时
            case MotionEvent.ACTION_CANCEL:
                // 未触发 longClick,直接重置
                if (!isReady) {
                    reset();
                    return super.onTouchEvent(event);
                }
                // 触发了longClick，开始初始化录音，但是未初始化完成,或者录音时间太短
                if (!isRecording || mRecordTime < 0.8f) {
                    mDialogManager.showDialogToShort();
                    mAudioRecordManager.cancelAudio();
                    mHandler.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS, 1000);
                } else if (mCurrentState == STATE_RECORDING) {
                    mDialogManager.dismissDialog();
                    mAudioRecordManager.releaseAudio();
                    // 将录音文件路径和录音时长回调
                    if (mRecordingListener != null) {
                        mRecordingListener.recordFinish(mAudioFilePath, mRecordTime);//正常录制结束
                    }
                } else if (mCurrentState == STATE_WANT_CANCEL) {//想要取消状态
                    mDialogManager.dismissDialog();
                    mAudioRecordManager.cancelAudio();
                }
                reset();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void changeState(int state) {
        if (mCurrentState != state) {
            mCurrentState = state;
            if (state == STATE_NORMAL) {
                setText(getResources().getString(R.string.press_record));
                setBackgroundResource(R.drawable.record_button_normal);
            } else if (state == STATE_RECORDING) {
                setText(getResources().getString(R.string.release_end));
                setBackgroundResource(R.drawable.record_button_recoding);
                if (isRecording) {
                    mDialogManager.showRecording();
                }
            } else if (state == STATE_WANT_CANCEL) {
                setText(getResources().getString(R.string.release_cancel));
                setBackgroundResource(R.drawable.record_button_recoding);
                if (isRecording) {
                    mDialogManager.showDialogWantCancel();
                }
            }
        }
    }

    /**
     * 判断是否是要取消
     *
     * @param x 手指当前位置 x 坐标
     * @param y 手指当前位置 y 坐标
     */
    private boolean isWantToCancel(int x, int y) {
        return x < 0 || x > getWidth()
                || y < -CANCEL_HEIGHT || y > getHeight() + CANCEL_HEIGHT;
    }

    /**
     * 释放资源，释放音频焦点
     */
    private void reset() {
        isReady = false;
        isRecording = false;
        mRecordTime = 0;
        changeState(STATE_NORMAL);

        // 释放焦点
        if (mAudioManager != null){
            mAudioManager.abandonAudioFocus(null);
        }
    }

    /**
     * 获取当前录音文件保存路径
     *
     * @return 当前录音文件保存路径
     */
    public String getAudioSaveDir() {
        return mAudioSaveDir;
    }


    public interface OnRecordingListener {
        /**
         * 录音正常结束
         *
         * @param audioFilePath 录音文件绝对路径
         * @param recordTime    录音时长,ms
         */
        void recordFinish(String audioFilePath, long recordTime);

        /**
         * 录音发生错误
         *
         * @param message 错误提示
         */
        void recordError(String message);
    }

}
