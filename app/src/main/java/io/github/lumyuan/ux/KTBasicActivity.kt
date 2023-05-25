package io.github.lumyuan.ux

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.gyf.immersionbar.ImmersionBar
import io.github.lumyuan.ux.core.animation.setOnFeedbackListener
import io.github.lumyuan.ux.core.common.bind
import io.github.lumyuan.ux.core.ui.adapter.FastViewBindingRecyclerViewAdapter
import io.github.lumyuan.ux.databinding.ActivityKtBasicBinding
import io.github.lumyuan.ux.databinding.ItemBasicBinding

class KTBasicActivity : AppCompatActivity() {

    private val binding by bind(ActivityKtBasicBinding::inflate) {
        ImmersionBar.with(it)
            .transparentStatusBar()
            .transparentNavigationBar()
            .statusBarDarkFont(true)
            .navigationBarDarkIcon(true)
            .keyboardEnable(true)
            .init()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recyclerViewAdapter =
            FastViewBindingRecyclerViewAdapter<String?, ItemBasicBinding>(ItemBasicBinding::inflate) {adapter, binding, data, position ->
                binding.text1.text = data
                binding.root.setOnFeedbackListener {
                    Toast.makeText(this@KTBasicActivity, data, Toast.LENGTH_SHORT).show()
                }
                binding.delete.setOnFeedbackListener {
                    //删除项目
                    adapter.removeItem(position)
                    Toast.makeText(this, "已删除$data", Toast.LENGTH_SHORT).show()
                }
                binding.insert.setOnFeedbackListener {
                    //插入数据（向下）
                    adapter.addItem(position + 1, "插入数据测试：${position + 1}")
                }
            }

        binding.listView.run {
            layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            adapter = recyclerViewAdapter
        }

        val arrayList = ArrayList<String?>()
        for (i in 0 until 50) {
            arrayList.add("条目测试：$i")
        }
        //添加数据
        recyclerViewAdapter.addItems(0, arrayList)
    }
}