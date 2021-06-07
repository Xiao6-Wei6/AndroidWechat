package com.itheima.imclient95;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.ashokvarma.bottomnavigation.BadgeItem;
import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.itheima.imclient95.utils.FragmentFactory;
import com.itheima.imclient95.view.AddFriendActivity;
import com.itheima.imclient95.view.BaseActivity;
import com.itheima.imclient95.view.BaseFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class MainActivity extends BaseActivity {

    private String[] titles = {"消息","联系人","动态"};
    private Toolbar toolbar;
    private TextView tv_title;
    private BottomNavigationBar bottomNavigationBar;
    private BadgeItem badgeItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.tb_toolbar);
        tv_title = (TextView) findViewById(R.id.tv_title);
        bottomNavigationBar = (BottomNavigationBar) findViewById(R.id.bottom_navigation_bar);
        initToolbar();
        initBottomNavigationBar();
        initFirstFragment();
    }

    /**
     * 初始化第一个fragment
     */
    private void initFirstFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if(fragments!=null && fragments.size()>0){
            //说明有之前缓存的fragment 处理fragment重影的问题
            for(int i = 0;i<fragments.size();i++){
                transaction.remove(fragments.get(i));
            }
            transaction.commit();
        }

        BaseFragment fragment = FragmentFactory.getFragment(0);
        transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fl_container,fragment,"0").commit();
    }

    private void initBottomNavigationBar() {
        BottomNavigationItem conversationItem = new BottomNavigationItem(R.mipmap.conversation_selected_2, "消息");
        //BadgeItem 底部导航图标 右上角的圆圈文字
        badgeItem = new BadgeItem();
        updateBadgeItem();
        //右上角的圆圈文字 显示出来
        conversationItem.setBadgeItem(badgeItem);
//        item.setActiveColor(getResources().getColor(R.color.btn_normal));
//        item.setInActiveColor(getResources().getColor(R.color.inactive));
        bottomNavigationBar.addItem(conversationItem);

        BottomNavigationItem item = new BottomNavigationItem(R.mipmap.contact_selected_2, "联系人");
//        item.setActiveColor(getResources().getColor(R.color.btn_normal));
//        item.setInActiveColor(getResources().getColor(R.color.inactive));
        bottomNavigationBar.addItem(item);
        item = new BottomNavigationItem(R.mipmap.plugin_selected_2, "动态");
//        item.setActiveColor(getResources().getColor(R.color.btn_normal));
//        item.setInActiveColor(getResources().getColor(R.color.inactive));
        bottomNavigationBar.addItem(item);

        //设置选中时的颜色
        bottomNavigationBar.setActiveColor(R.color.btn_normal);
        // 设置没被选中时的颜色
        bottomNavigationBar.setInActiveColor(R.color.inactive);

        //可以修改第一次加载时被选中的tab的 位置
      //  bottomNavigationBar.setFirstSelectedPosition(1);
        //初始化
        bottomNavigationBar.initialise();

        bottomNavigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener(){
            @Override
            public void onTabSelected(int position) {
                //被选中了
                Log.e("Mainactivity","onTabSelected"+position);
                //通过postion拿到fragment
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                BaseFragment fragment = FragmentFactory.getFragment(position);
                if(fragment.isAdded()){
                    //如果这个fragment 已经被add进来 显示一下
                    transaction.show(fragment).commit();
                }else{
                   // 如果这个fragment 没有被add进来 那么把fragmentadd到主界面中
                    transaction.add(R.id.fl_container,fragment,position+"").commit();
                }
                tv_title.setText(titles[position]);
            }
            @Override
            public void onTabUnselected(int position) {
                Log.e("Mainactivity","onTabUnselected"+position);
                //没被选中
                //通过position找到fragment隐藏起来
                getSupportFragmentManager().beginTransaction().hide(FragmentFactory.getFragment(position)).commit();
            }
            @Override
            public void onTabReselected(int position) {
            }
        });
    }

    public void updateBadgeItem() {
        //获取所有的未读条目数量
        int unreadMessageCount = EMClient.getInstance().chatManager().getUnreadMessageCount();
        if(unreadMessageCount==0){
            badgeItem.hide(true);
        }else if(unreadMessageCount>99){
            badgeItem.show(true);
            badgeItem.setText(String.valueOf(99));
        }else{
            badgeItem.show(true);
            badgeItem.setText(String.valueOf(unreadMessageCount));
        }

    }

    private void initToolbar() {
        //如果不设置title 默认会把appname显示在toolbar上
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        //如果不设置 左边的返回图标就消失了
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //创建一个菜单
        //找到菜单布局填充器
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main,menu);
        MenuBuilder builder = (MenuBuilder) menu;
        //设置图标可见
        // builder.setOptionalIconsVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId){
            case R.id.menu_item_addfriend:
                startActivity(AddFriendActivity.class,false);
                break;
            case R.id.menu_item_scan:
                showToast("扫一扫");
                break;
            case R.id.menu_item_about:
                showToast("关于");
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    void onGetMessageEvent(List<EMMessage> messages){
        //更新未读消息的图标
        updateBadgeItem();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBadgeItem();
    }
}
