package com.ezreal.ezchat.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ezreal.ezchat.R;
import com.ezreal.ezchat.bean.AddFriendNotify;
import com.ezreal.ezchat.handler.NimFriendHandler;
import com.ezreal.ezchat.handler.NimUserHandler;
import com.ezreal.ezchat.handler.NimSysMsgHandler;
import com.ezreal.ezchat.handler.NimSysMsgHandler.SystemMessageListener;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.friend.FriendService;
import com.netease.nimlib.sdk.msg.SystemMessageService;
import com.netease.nimlib.sdk.msg.constant.SystemMessageStatus;
import com.netease.nimlib.sdk.msg.constant.SystemMessageType;
import com.netease.nimlib.sdk.msg.model.SystemMessage;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;
import com.suntek.commonlibrary.adapter.OnItemClickListener;
import com.suntek.commonlibrary.adapter.OnItemLongClickListener;
import com.suntek.commonlibrary.adapter.RViewHolder;
import com.suntek.commonlibrary.adapter.RecycleViewAdapter;
import com.suntek.commonlibrary.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by wudeng on 2017/8/29.
 */

public class CheckNotifyListActivity extends BaseActivity {

    public static final int RESULT_HAVE_CHANGE = 0x4000;
    private static final int LOAD_MESSAGE_COUNT = 500;
    @BindView(R.id.rcv_notify_list)
    RecyclerView mRecyclerView;
    private List<AddFriendNotify> mNotifyInfoList;
    private RecycleViewAdapter<AddFriendNotify> mAdapter;
    private boolean haveChange = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(R.color.app_blue_color);
        setContentView(R.layout.activity_check_notify);
        setTitleBar("????????????", true, true);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        mNotifyInfoList = new ArrayList<>();
        mAdapter = new RecycleViewAdapter<AddFriendNotify>(this, mNotifyInfoList) {
            @Override
            public int setItemLayoutId(int position) {
                return R.layout.item_check_notify;
            }

            @Override
            public void bindView(RViewHolder holder, final int position) {
                final AddFriendNotify item = mNotifyInfoList.get(position);
                SystemMessage message = item.getMessage();
                NimUserInfo userInfo = item.getUserInfo();
                if (userInfo != null) {
                    holder.setImageByUrl(CheckNotifyListActivity.this, R.id.iv_head_picture,
                            userInfo.getAvatar(), R.mipmap.bg_img_defalut);
                    holder.setText(R.id.tv_name, userInfo.getName());
                } else {
                    holder.setImageResource(R.id.iv_head_picture, R.mipmap.bg_img_defalut);
                    holder.setText(R.id.tv_name, message.getFromAccount());
                }

                holder.setText(R.id.tv_content, message.getContent());

                TextView tv_status = holder.getTextView(R.id.tv_status);
                TextView tv_agree = holder.getTextView(R.id.tv_do_agree);
                TextView tv_refuse = holder.getTextView(R.id.tv_do_refuse);
                if (message.getStatus() == SystemMessageStatus.declined) {
                    tv_agree.setVisibility(View.GONE);
                    tv_refuse.setVisibility(View.GONE);
                    tv_status.setText("?????????");
                    tv_status.setVisibility(View.VISIBLE);
                } else if (message.getStatus() == SystemMessageStatus.passed) {
                    tv_agree.setVisibility(View.GONE);
                    tv_refuse.setVisibility(View.GONE);
                    tv_status.setText("?????????");
                    tv_status.setVisibility(View.VISIBLE);
                } else if (message.getStatus() == SystemMessageStatus.init) {
                    tv_status.setText("");
                    tv_status.setVisibility(View.GONE);
                    tv_agree.setVisibility(View.VISIBLE);
                    tv_refuse.setVisibility(View.VISIBLE);
                }
                tv_agree.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dealRequest(item.getMessage().getFromAccount(), position, true);
                    }
                });

                tv_refuse.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dealRequest(item.getMessage().getFromAccount(), position, false);
                    }
                });
            }
        };
        mAdapter.setItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(RViewHolder holder, int position) {
                ignoreRequest(position);
            }
        });

        mAdapter.setItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(RViewHolder holder, int position) {
                showAccountInfo(position);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        // Tool Bar
        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // ???????????????????????????????????????????????????????????????
        NimSysMsgHandler.getInstance().setMessageListener(new SystemMessageListener() {
            @Override
            public void addFriendNotify() {
                loadAddFriendNotify();
            }
        });

        loadAddFriendNotify();
    }

    /**
     * ??????????????????????????????
     */
    private void loadAddFriendNotify() {
        List<SystemMessageType> types = new ArrayList<>();
        types.add(SystemMessageType.AddFriend);

        // ????????????????????????????????????
        List<SystemMessage> msgList = NIMClient.getService(SystemMessageService.class)
                .querySystemMessageByTypeBlock(types, 0, LOAD_MESSAGE_COUNT);

        // ?????????????????????????????????????????????????????? item ??????
        AddFriendNotify notify;
        List<AddFriendNotify> notifyInfoList = new ArrayList<>();
        for (SystemMessage message : msgList) {
            // ?????????????????????????????????????????????????????????
            if (message.getStatus() == SystemMessageStatus.ignored) {
                continue;
            }
            NimUserInfo userInfo = NimUserHandler.getInstance().getUserInfo();
            //???????????????????????????????????????????????????????????????
            if (userInfo == null) {
                return;
            }

            notify = new AddFriendNotify();
            notify.setMessage(message);
            notify.setMyFriend(NimFriendHandler.getInstance().CheckIsMyFriend(message.getFromAccount()));
            notify.setUserInfo(userInfo);
            notifyInfoList.add(notify);
        }

        // ????????????
        mNotifyInfoList.clear();
        mNotifyInfoList.addAll(notifyInfoList);
        mAdapter.notifyDataSetChanged();

        // ???????????????????????????????????????
        NIMClient.getService(SystemMessageService.class)
                .resetSystemMessageUnreadCountByType(types);
    }

    /**
     * ??????????????????
     *
     * @param account  ????????????
     * @param position ????????????
     * @param agree    ????????????
     */
    private void dealRequest(String account, int position, final boolean agree) {
        final SystemMessage message = mNotifyInfoList.get(position).getMessage();
        NIMClient.getService(FriendService.class)
                .ackAddFriendRequest(account, agree).setCallback(new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void param) {
                SystemMessageStatus status;
                if (agree) {
                    status = SystemMessageStatus.passed;
                } else {
                    status = SystemMessageStatus.declined;
                }
                NIMClient.getService(SystemMessageService.class)
                        .setSystemMessageStatus(message.getMessageId(), status);
                message.setStatus(status);
                mAdapter.notifyDataSetChanged();
                haveChange = true;
            }

            @Override
            public void onFailed(int code) {
                ToastUtils.showMessage(CheckNotifyListActivity.this, "????????????,code:" + code);
            }

            @Override
            public void onException(Throwable exception) {
                ToastUtils.showMessage(CheckNotifyListActivity.this, "????????????:" + exception.getMessage());
            }
        });
    }

    /**
     * ???????????????????????????
     * @param position ????????????
     */
    private void ignoreRequest(final int position) {
        new AlertDialog.Builder(this)
                .setMessage("???????????????????????????")
                .setCancelable(true)
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SystemMessage message = mNotifyInfoList.get(position).getMessage();
                        NIMClient.getService(SystemMessageService.class)
                                .setSystemMessageStatus(message.getMessageId(),
                                        SystemMessageStatus.ignored);
                        mNotifyInfoList.remove(position);
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();

    }

    private void showAccountInfo(int position){
        AddFriendNotify notifyInfo = mNotifyInfoList.get(position);
        Intent intent = new Intent(this,FriendInfoActivity.class);
        intent.putExtra("NimUserInfo",notifyInfo.getUserInfo());
        if (notifyInfo.isMyFriend()){
            intent.putExtra("FLAG",FriendInfoActivity.FLAG_SHOW_FRIEND);
        }else {
            intent.putExtra("FLAG",FriendInfoActivity.FLAG_ADD_FRIEND);
        }
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (haveChange){
            haveChange = false;
            this.setResult(RESULT_HAVE_CHANGE);
        }
    }
}
