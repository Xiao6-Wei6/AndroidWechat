package com.ezreal.audiorecordbutton;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 录音提示框
 */

public class RecordDialogManager {

    private Context mContext;
    private Dialog mDialog;
    private LayoutInflater mInflater;
    private ImageView mIvRecord;
    private ImageView mIvVoiceLevel;
    private TextView mTvTip;

    public RecordDialogManager(Context context){
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }
    /**
     * 用来设置Diaog布局，拿到控件的引用，显示Dialog。
     * */
    public void showDialogRecord(){
        //将布局应用于Dialog
        View view = mInflater.inflate(R.layout.dialog_audio_record_button,null);
        mDialog = new Dialog(mContext,R.style.Theme_Audio_Record_Button);
        mDialog.setContentView(view);


        mIvRecord = (ImageView) mDialog.findViewById(R.id.iv_record);//加载麦克风样式
        mIvVoiceLevel = (ImageView) mDialog.findViewById(R.id.iv_voice_level);//加载声音梯度样式
        mTvTip = (TextView) mDialog.findViewById(R.id.tv_dialog_tip);
        mDialog.show();
    }
    /**
     * ：更改Dialog状态为录音中状态
     * */
    public void showRecording(){
        if (mDialog != null && mDialog.isShowing()){
            mIvRecord.setImageResource(R.drawable.recorder);
            mIvVoiceLevel.setVisibility(View.VISIBLE);
            mTvTip.setText(mContext.getString(R.string.move_up_cancel));
        }
    }
    /**
     * 更改Dialog状态为录音时长过短状态。
     * */
    public void showDialogToShort(){
        if (mDialog != null && mDialog.isShowing()){
            mIvRecord.setImageResource(R.drawable.voice_to_short);
            mIvVoiceLevel.setVisibility(View.GONE);
            mTvTip.setText(mContext.getString(R.string.record_to_short));
        }
    }
    /**
     * 更改Dialog状态为想要取消状态。
     * */
    public void showDialogWantCancel(){
        if (mDialog != null && mDialog.isShowing()){
            mIvRecord.setImageResource(R.drawable.cancel);
            mIvVoiceLevel.setVisibility(View.GONE);
            mTvTip.setText(mContext.getString(R.string.release_cancel));
        }
    }

    /**
     * 根据音量大小更新 音量图标高度
     * 用来更新音量图片
     * @param level
     */
    public void updateVoiceLevel(int level){
        if (mDialog != null && mDialog.isShowing()){
            int resId = mContext.getResources().getIdentifier("v"+level,
                    "drawable",mContext.getPackageName());
            mIvVoiceLevel.setImageResource(resId);
        }
    }
    /**
     * 移除Dialog
     * */
    public void dismissDialog(){
        if (mDialog != null){
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
