package com.fphoenixcorneae.download.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fphoenixcorneae.download.DownloadManager
import com.fphoenixcorneae.download.model.DownloadStatus
import com.fphoenixcorneae.download.ext.globalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val mDownloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Default)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        ThreadPool.getInstance()
//            .threadPoolExecutor
//            .asCoroutineDispatcher()
//            .use {
//                for (index in 0..200) {
//                    globalScope.launch(it) {
//                        if (Thread.currentThread() == Looper.getMainLooper().thread) {
//                            "index: $index current thread is in main thread".logi("ThreadPool")
//                        }
//                        "$it  index: $index".logd("ThreadPool")
//                    }
//                }
//            }

        val url = "https://down.qq.com/qqweb/PCQQ/PCQQ_EXE/QQ9.5.2.27905.exe"
        val saveName = url.substring(url.lastIndexOf("/") + 1)

        val sinaWeibo = "https://gdown.baidu.com/data/wisegame/f3d5883394f7dca0/cb09f3d5883394f7dca0ac895d6c9693.apk"
        val sinaWeiboSavaName = sinaWeibo.substring(sinaWeibo.lastIndexOf("/") + 1)

        findViewById<View>(R.id.btnStartDownload).setOnClickListener {
            DownloadManager.getInstance()
                .download(
                    tag = url,
                    url = url,
                    saveName = saveName,
                    reDownload = true,
                    downloadStatus = mDownloadStatus,
                )
            DownloadManager.getInstance()
                .download(
                    tag = sinaWeibo,
                    url = sinaWeibo,
                    saveName = sinaWeiboSavaName,
                    reDownload = true,
                    downloadStatus = mDownloadStatus,
                )
        }
        findViewById<View>(R.id.btnStopDownload).setOnClickListener {
            DownloadManager.getInstance().stopDownload(tag = url, downloadStatus = mDownloadStatus)
            DownloadManager.getInstance().stopDownload(tag = sinaWeibo, downloadStatus = mDownloadStatus)
        }
        findViewById<View>(R.id.btnContinueDownload).setOnClickListener {
            DownloadManager.getInstance().continueDownload(tag = url, downloadStatus = mDownloadStatus)
            DownloadManager.getInstance().continueDownload(tag = sinaWeibo, downloadStatus = mDownloadStatus)
        }
        findViewById<View>(R.id.btnCancelDownload).setOnClickListener {
            DownloadManager.getInstance().cancelDownload(tag = url, downloadStatus = mDownloadStatus)
            DownloadManager.getInstance().cancelDownload(tag = sinaWeibo, downloadStatus = mDownloadStatus)
        }

        globalScope.launch {
            mDownloadStatus.collect {
                Log.d("DownloadManager", "$it")
                if (it.tag == url && it is DownloadStatus.Progress) {
                    findViewById<ProgressBar>(R.id.pbProgressQq).progress = it.progress.toInt()
                    findViewById<TextView>(R.id.tvProgressQq).text = "${it.progress}%"
                } else if (it.tag == sinaWeibo && it is DownloadStatus.Progress) {
                    findViewById<ProgressBar>(R.id.pbProgressSina).progress = it.progress.toInt()
                    findViewById<TextView>(R.id.tvProgressSina).text = "${it.progress}%"
                }
            }
        }
        globalScope.launch {
            DownloadManager.getInstance().collectDownload(tag = url) {
                Log.d("collectDownload", "$it")
//                findViewById<ProgressBar>(R.id.pbProgressQq).progress = it?.progress?.toInt() ?: 0
//                findViewById<TextView>(R.id.tvProgressQq).text = "${it?.progress ?: 0f}%"
            }
        }
        globalScope.launch {
            DownloadManager.getInstance().collectDownload(tag = sinaWeibo) {
                Log.d("collectDownload", "$it")
//                findViewById<ProgressBar>(R.id.pbProgressSina).progress = it?.progress?.toInt() ?: 0
//                findViewById<TextView>(R.id.tvProgressSina).text = "${it?.progress ?: 0f}%"
            }
        }

        findViewById<View>(R.id.btnDownloadManage).setOnClickListener {
            startActivity(Intent(this, DownloadManageActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DownloadManager.getInstance().stopAllDownload()
    }
}