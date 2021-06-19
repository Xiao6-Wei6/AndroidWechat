package com.ezreal.audiorecordbutton;

import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 录音管理类
 */

public class AudioRecordManager {

    private static AudioRecordManager instance;//控制声音录制的对象
    private MediaRecorder mMediaRecorder;//音频采集对象
    //private String mAudioDir;
    private final String mAudioDir;//文件夹名称
    private String mCurrentFilePath;//当前音频文件路径
    private OnAudioStateListener mStateListener;
    private boolean hasPrepare = false;//MediaRecorder是否进入prepare状态,状态为true才能调用stop和release方法

    /**
     * 单例，录音管理器初始化
     * @return AudioRecordManager
     */
    public static AudioRecordManager getInstance(String audioDir){ //获取单例,录音管理器
        if (instance == null){
            synchronized (AudioRecordManager.class){
                if (instance == null){
                    instance = new AudioRecordManager(audioDir);
                }
            }
        }
        return instance;
    }

    public void setAudioStateListener(OnAudioStateListener listener){
        mStateListener = listener;
    }
    /**
     * 准备
     */
    public void prepareAudio(){
        try {
            hasPrepare = false;
            File dir = new File(mAudioDir);//创建文件夹
            if (!dir.exists()){//如果不存在，就创建
                dir.mkdirs();
            }
            mMediaRecorder = new MediaRecorder();
            String fileName = UUID.randomUUID().toString() + ".arm";//随机生成文件名称
            File file = new File(dir,fileName);
            mCurrentFilePath = file.getAbsolutePath();
            // 设置输出路径
            mMediaRecorder.setOutputFile(mCurrentFilePath);
            // 设置音频源,麦克风
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // 设置输出格式
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            // 设置音频编码
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            // 准备
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            //准备结束
            hasPrepare = true;

            if (mStateListener != null){
                mStateListener.prepareFinish(mCurrentFilePath);//准备完毕
            }
        } catch (IOException e) {
            if (mStateListener != null){
                mStateListener.prepareError(e.getMessage());//准备出错
            }
            e.printStackTrace();
        }

    }

    /**
     * 获取音量等级
     */
    public int getVoiceLevel(int maxLevel){
        try {
            if (hasPrepare){
                // mMediaRecorder.getMaxAmplitude = 0 - 32767
                return maxLevel * mMediaRecorder.getMaxAmplitude() / 32768 + 1;
            }
        }catch (Exception e){
            return 1;
        }
        return 1;
    }
    /**
     * 重置
     */
    public void releaseAudio(){
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
        hasPrepare = false;
    }
    /**
     * 取消
     */
    public void cancelAudio(){
        releaseAudio();
        if (mCurrentFilePath != null){
            File file = new File(mCurrentFilePath);
            file.delete();//删除产生的文件
        }
    }
    private AudioRecordManager(String audioDir){
        mAudioDir = audioDir;
    }

    /**
     * 准备完毕接口
     */
    public interface OnAudioStateListener{
        void prepareError(String message);
        void prepareFinish(String audioFilePath);
    }
}
