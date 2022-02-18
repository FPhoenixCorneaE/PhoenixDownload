package com.fphoenixcorneae.download

import com.fphoenixcorneae.download.db.DownloadDbHelper
import com.fphoenixcorneae.download.db.DownloadEntity
import com.fphoenixcorneae.download.model.DownloadStatus
import com.fphoenixcorneae.download.ext.*
import com.fphoenixcorneae.download.http.OkHttpManager
import com.fphoenixcorneae.download.pool.DownloadTaskPool
import com.fphoenixcorneae.download.pool.ThreadPoolProxy
import com.fphoenixcorneae.download.tool.FileTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * @desc：DownloadManager
 * @date：2022/01/27 14:12
 */
class DownloadManager private constructor() {

    /** 网络请求 */
    private val mOkHttpManager by lazy { OkHttpManager.getInstance() }

    /** 下载任务池 */
    private val mDownloadTaskPool by lazy { DownloadTaskPool.getInstance() }

    /**
     * 下载
     * @param tag String 标识
     * @param url String 下载的url
     * @param saveName String 保存的名字
     * @param savePath String 保存的路径
     * @param reDownload Boolean 如果文件已存在是否需要重新下载 默认false-不需要重新下载
     * @param downloadStatus DownloadStatus 下载回调
     */
    fun download(
        tag: String,
        url: String,
        saveName: String,
        savePath: String? = null,
        reDownload: Boolean = false,
        downloadStatus: MutableStateFlow<DownloadStatus>? = null
    ) {
        "download url: $url".logi()
        // 已经在队列中, 并且协程是活跃的, 表示正在下载中...
        val isActiveTask = mDownloadTaskPool.isActiveTask(tag = tag)
        if (isActiveTask == true) {
            "download tag: $tag already in the queue.".logi()
            return
        }
        // 已经在队列中, 但协程不再活跃了, 移除掉该下载任务
        if (isActiveTask == false) {
            "download tag: $tag already in the queue, but it's no longer active. removed from the download task pool.".logi()
            mDownloadTaskPool.removeDownloadTask(tag = tag)
        }
        // 保存名称为空, 回调下载错误
        if (saveName.isBlank()) {
            "download tag: $tag, error: file save name is empty.".loge()
            downloadStatus?.value = DownloadStatus.Error(
                tag = tag,
                message = "download tag: $tag file save name is empty."
            )
            return
        }
        val filePath = if (savePath.isNullOrBlank()) {
            "${applicationContext.filesDir.absolutePath}/$saveName"
        } else {
            "$savePath/$saveName"
        }
        val file = File(filePath)
        var (currentSize, totalSize) = runBlocking {
            DownloadDbHelper.queryCurrentSize(tag = tag) to DownloadDbHelper.queryTotalSize(tag = tag)
        }

        if (file.exists().not()) {
            // 文件不存在
            "download tag: $tag file not exists.".logi()
            currentSize = 0
        } else if (currentSize == totalSize) {
            // 文件已存在, 并且已下载完成
            if (reDownload) {
                // 重新下载
                "download tag: $tag file exists, but download again.".logi()
                currentSize = 0
            } else {
                // 不需要重新下载, 回调下载成功
                "download tag: $tag success: file exists, re-download no need.".logi()
                downloadStatus?.value = DownloadStatus.Success(
                    tag = tag,
                    localPath = file.absolutePath,
                    totalSize = file.length()
                )
                return
            }
        }

        // 回调下载准备
        "download tag: $tag prepare, currentSize: $currentSize.".logi()
        downloadStatus?.value = DownloadStatus.Prepare(tag = tag)
        // 添加到下载任务池
        mDownloadTaskPool.addDownloadTask(
            tag = tag,
            url = url,
            name = saveName,
            localPath = filePath
        )
        // 插入下载数据到数据库
        DownloadDbHelper.downloadPrepare(
            tag = tag,
            url = url,
            name = saveName,
            localPath = filePath
        )

        // 开始下载
        runCatching {
            ThreadPoolProxy.getInstance()
                .threadPoolExecutor
                .asCoroutineDispatcher()
                .let {
                    val downloadJob = globalScope.launch(it) {
                        val responseBody = mOkHttpManager.download(url = url, start = currentSize)
                        if (responseBody == null) {
                            // 响应体为空, 回调下载错误
                            val errorMsg =
                                "download tag: $tag, error: response body is null, please check download url.".also {
                                    it.loge()
                                }
                            downloadStatus?.value = DownloadStatus.Error(
                                tag = tag,
                                message = errorMsg
                            )
                            // 下载失败，取消任务
                            mDownloadTaskPool.cancelDownloadTask(tag = tag)
                            // 更新数据库
                            DownloadDbHelper.downloadError(tag = tag, errorMsg = errorMsg)
                        } else {
                            // 保存文件到本地
                            FileTool.saveFile2Local(
                                tag = tag,
                                filePath = filePath,
                                currentSize = currentSize,
                                responseBody = responseBody,
                                downloadStatus = downloadStatus
                            )
                        }
                    }
                    // 设置下载任务协程上下文
                    mDownloadTaskPool.setDownloadTaskCoroutineContext(tag = tag, coroutineContext = downloadJob)
                }
        }.onFailure {
            "download tag: $tag, error in network request: $it.".loge()
            it.printStackTrace()
            // 出现异常，回调下载错误
            downloadStatus?.value = DownloadStatus.Error(tag = tag, message = it.message)
            // 下载失败，取消任务
            mDownloadTaskPool.cancelDownloadTask(tag = tag)
            // 更新数据库
            DownloadDbHelper.downloadError(tag = tag, errorMsg = it.message)
        }
    }

    /**
     * 根据tag获得下载队列中的data
     */
    fun getDownloadData(tag: String): DownloadEntity? {
        return runBlocking {
            DownloadDbHelper.queryByTag(tag = tag)
        }
    }

    /**
     * 获得数据库中所有下载数据
     */
    fun getAllDownloadData(): List<DownloadEntity?>? {
        return runBlocking {
            DownloadDbHelper.queryAll()
        }
    }

    /**
     * 暂停
     */
    fun stopDownload(tag: String, downloadStatus: MutableStateFlow<DownloadStatus>? = null) {
        mDownloadTaskPool.pauseDownloadTask(tag = tag)
        DownloadDbHelper.downloadPause(tag = tag)
        downloadStatus?.value = DownloadStatus.Pause(tag = tag)
    }

    /**
     * 暂停所有下载中的任务
     */
    fun stopAllDownload() {
        "onApplicationDestroyed".logd("ApplicationLifecycle")
        // 程序退出，暂停所有正在下载的任务
        val allDownloadingTask = mDownloadTaskPool.getAllDownloadTask()
        allDownloadingTask.forEach {
            it?.let {
                stopDownload(it.tag)
            }
        }
    }

    /**
     * 继续
     */
    fun continueDownload(tag: String, downloadStatus: MutableStateFlow<DownloadStatus>? = null) {
        globalScope.launch(Dispatchers.IO) {
            DownloadDbHelper.queryByTag(tag = tag)?.let {
                if (it.tag != null && it.url != null && it.name != null) {
                    download(tag = it.tag!!, url = it.url!!, saveName = it.name!!, downloadStatus = downloadStatus)
                }
            }
        }
    }

    /**
     * 取消
     */
    fun cancelDownload(tag: String, downloadStatus: MutableStateFlow<DownloadStatus>? = null) {
        mDownloadTaskPool.cancelDownloadTask(tag = tag)
        DownloadDbHelper.downloadCancel(tag = tag)
        downloadStatus?.value = DownloadStatus.Cancel(tag = tag)
    }

    /**
     * 监听下载数据变化
     * 注意：有延迟, 数据变化越多, 延迟越大。
     */
    suspend fun collectDownload(tag: String, block: (DownloadEntity?) -> Unit) {
        DownloadDbHelper.collectDownload(tag = tag).collect {
            block(it)
        }
    }

    companion object {
        @Volatile
        private var sInstance: DownloadManager? = null

        fun getInstance(): DownloadManager {
            return sInstance ?: synchronized(this) {
                sInstance ?: DownloadManager().also { sInstance = it }
            }
        }
    }
}