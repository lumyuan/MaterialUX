package io.github.lumyuan.ux;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.widget.Toast;

import com.gyf.immersionbar.ImmersionBar;

import java.util.ArrayList;

import io.github.lumyuan.ux.bottomnavigationview.widget.BottomNavigationView;
import io.github.lumyuan.ux.databinding.ActivityMainBinding;
import io.github.lumyuan.ux.topbar.widget.TopBar;
import io.github.lumyuan.ux.ui.PagerAdapterForFragment;
import io.github.lumyuan.ux.ui.fragments.BlankFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private ArrayList<TopBar.Item> topBarItems = new ArrayList<>();
    private ArrayList<PagerAdapterForFragment.Page> pages = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImmersionBar.with(this)
                .transparentStatusBar()
                .transparentNavigationBar()
                .statusBarDarkFont(true)
                .navigationBarDarkIcon(true)
                .keyboardEnable(true)
                .init();

        pages.add(
                new PagerAdapterForFragment.Page(
                        BlankFragment.newInstance("", ""), null
                )
        );

        pages.add(
                new PagerAdapterForFragment.Page(
                        BlankFragment.newInstance("", ""), null
                )
        );

        pages.add(
                new PagerAdapterForFragment.Page(
                        BlankFragment.newInstance("", ""), null
                )
        );

        binding.viewpager.setAdapter(
                new PagerAdapterForFragment(
                        pages.toArray(new PagerAdapterForFragment.Page[3]),
                        getSupportFragmentManager()
                )
        );

        topBarItems.add(
                new TopBar.Item(
                        "首页",
                        "欢迎使用",
                        null,
                        null
                )
        );
        topBarItems.add(
                new TopBar.Item(
                        "模块",
                        null,
                        R.drawable.ic_module,
                        null
                )
        );
        topBarItems.add(
                new TopBar.Item(
                        "我的",
                        "个人中心",
                        R.mipmap.ic_launcher,
                        R.drawable.ic_mine
                )
        );

        //设置标题栏数据
        binding.topBar.setupData(topBarItems);
        //绑定ViewPager
        binding.topBar.setupViewpager(binding.viewpager);


        //创建导航按钮
        //推荐数量：3~5，太多会挤压内部view，太少有点空
        BottomNavigationView.ItemView v1 = binding.navigationView.newItemView();
        v1.setText("首页"); //导航条设置标题
        v1.setImageResource(R.drawable.ic_home); //设置导航条图标
        binding.navigationView.addItemView(v1); //将导航条添加到导航栏

        BottomNavigationView.ItemView v2 = binding.navigationView.newItemView();
        v2.setText("模块");
        v2.setImageResource(R.drawable.ic_module);
        binding.navigationView.addItemView(v2);

        BottomNavigationView.ItemView v3 = binding.navigationView.newItemView();
        v3.setText("我的");
        v3.setImageResource(R.drawable.ic_mine);
        binding.navigationView.addItemView(v3);

        //导航栏绑定ViewPager
        binding.navigationView.setupViewpager(binding.viewpager);

        //TopBar两个按钮的点击事件
        binding.topBar.setFirstMenuOnClickListener(((view, position) -> {
            Toast.makeText(this, "当前位置：" + (position + 1), Toast.LENGTH_SHORT).show();
            return null;
        }));

        binding.topBar.setSecondsMenuOnClickListener(((view, position) -> {
            Toast.makeText(this, "当前位置：" + (position + 1), Toast.LENGTH_SHORT).show();
            return null;
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}