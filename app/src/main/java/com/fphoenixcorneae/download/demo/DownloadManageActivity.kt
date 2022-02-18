package com.fphoenixcorneae.download.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class DownloadManageActivity : AppCompatActivity() {

    private val mDownloadInfos = listOf(
        DownloadInfo(
            "https://appdown.baidu.com/data/wisegame/8c68b991be872f73/b8948c68b991be872f730d9166acb736.apk",
            "王者荣耀"
        ),
        DownloadInfo(
            "https://gp-dev.cdn.bcebos.com/gp-dev/upload/file/source/3bda4c1f5a146d75baf6607122a2dc18.apk",
            "原神"
        ),
        DownloadInfo(
            "https://appdown.baidu.com/data/wisegame/0559ce9aadb185a6/07670559ce9aadb185a6c81bb4b6de34.apk",
            "和平精英"
        ),
        DownloadInfo(
            "https://appdown.baidu.com/data/wisegame/debfbf79e96ea448/95eadebfbf79e96ea448e9c93217f3ae.apk",
            "偶像梦幻祭2"
        ),
        DownloadInfo(
            "https://appdown.baidu.com/data/wisegame/1c74f98b7fe21f14/a88a1c74f98b7fe21f142228f2266030.apk",
            "QQ炫舞"
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_manage)
        findViewById<RecyclerView>(R.id.rvDownloadManage).apply {
            adapter = DownloadManageAdapter(mDownloadInfos)
        }
    }
}