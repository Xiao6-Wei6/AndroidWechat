package com.ezreal.audiorecordbutton;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.PlaybackParams;

/**
 * 语音播放控制类
 */

public class AudioPlayManager {

    private static MediaPlayer sMediaPlayer;
    private static boolean isPause;//暂停状态标志
    private static AudioManager sAudioManager;//控制系统声音的对象

    /**
     * 播放音频文件
     * @param context  上下文参数
     * @param path     音频文件路径
     * @param listener 播放监听器
     */
    public static void playAudio(Context context, final String path, final OnPlayAudioListener listener) {//构造播放实例

        if (sAudioManager == null) {
            sAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);//AUDIO_SERVICE = "audio"
        }

        assert sAudioManager != null;
        sAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);//设置声音播放模式，

        if (sMediaPlayer == null) {
            sMediaPlayer = new MediaPlayer();
        } else {
            if (sMediaPlayer.isPlaying()) {//查询是否正在播放
                sMediaPlayer.stop();//停止播放
            }
            sMediaPlayer.reset();
        }

        sMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {//音频转码解码完成，会马上开始播放了
                mp.start();
                if (listener != null) {
                    listener.onPlay();
                }
            }
        });

        sMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {//播放过程中出错,参数为出错原因描述
                if (listener != null) {
                    listener.onError("播放出错,错误码:" + what);
                }
                return false;
            }
        });

        sMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (listener != null) {
                    listener.onComplete();// 播放结束
                }
            }
        });


        try {
            int focus = sAudioManager.requestAudioFocus(null,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);//获取音频焦点,短暂性获得焦点
            if (focus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {//焦点请求成功，处理焦点请求成功
                sMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);// AudioManager.STREAM_MUSIC表示使用扬声器模式
                sMediaPlayer.setDataSource(path);//设置音频来源
                sMediaPlayer.prepare();
            } else if (focus == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {//焦点请求失败
                if (listener != null) {
                    listener.onError("播放出错:" + "AUDIOFOCUS_REQUEST_FAILED");
                }
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onError("播放出错:" + e.getMessage());
            }
            release();
        }

    }

    /**
     * 暂停播放
     * 请在 context 生命周期 onPause 中调用
     * 可在需要的地方手动调用
     */
    public static void pause() {
        if (sMediaPlayer != null && sMediaPlayer.isPlaying()) { //正在播放的时候
            sMediaPlayer.pause();
            isPause = true;
        }

        if (sAudioManager != null) {
            sAudioManager.abandonAudioFocus(null);//放弃焦点
        }
    }

    /**
     * 恢复播放
     * 请在 context 生命周期 onResume 中调用
     * 可在需要的地方手动调用
     */
    public static void resume() {
        if (sMediaPlayer != null && isPause) {
            if (sAudioManager != null) {
                int focus = sAudioManager.requestAudioFocus(null,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                if (focus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    sMediaPlayer.start();
                    isPause = false;
                }
            }

        }
    }

    /**
     * 释放资源
     * 请在 context 生命周期 onDestroy 中调用
     * 可在需要的地方手动调用
     */
    public static void release() {
        if (sMediaPlayer != null) {
            sMediaPlayer.release();
            sMediaPlayer = null;
        }

        if (sAudioManager != null) {
            sAudioManager.abandonAudioFocus(null);
            sAudioManager = null;
        }
    }


    public interface OnPlayAudioListener {
        void onPlay();

        void onComplete();

        void onError(String message);
    }
}
