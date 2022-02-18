package com.fphoenixcorneae.download.pool

import com.fphoenixcorneae.download.model.DownloadTask
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * @desc：下载任务池
 * @date：2022/01/27 15:39
 */
class DownloadTaskPool private constructor() {

    /** 下载任务Map */
    private val mDownloadTaskMap = ConcurrentHashMap<String, DownloadTask>()

    /**
     * 添加下载任务
     */
    fun addDownloadTask(
        tag: String,
        url: String,
        name: String,
        localPath: String
    ) {
        mDownloadTaskMap[tag] = DownloadTask(
            tag = tag,
            url = url,
            name = name,
            localPath = localPath
        )
    }

    /**
     * 设置下载任务协程上下文
     */
    fun setDownloadTaskCoroutineContext(
        tag: String,
        coroutineContext: CoroutineContext
    ) {
        mDownloadTaskMap[tag]?.coroutineContext = coroutineContext
    }

    /**
     * 获取下载任务
     */
    fun getDownloadTask(tag: String): DownloadTask? {
        return mDownloadTaskMap[tag]
    }

    /**
     * 获取所有下载任务
     */
    fun getAllDownloadTask(): List<DownloadTask?> {
        return mDownloadTaskMap.flatMap {
            listOf(it.value)
        }
    }

    /**
     * 移除下载任务
     */
    fun removeDownloadTask(tag: String) {
        mDownloadTaskMap.remove(key = tag)
    }

    /**
     * 暂停下载任务
     */
    fun pauseDownloadTask(tag: String) {
        val downloadTask = getDownloadTask(tag = tag)
        val coroutineContext = downloadTask?.coroutineContext
        if (coroutineContext?.isActive == true) {
            coroutineContext.cancel()
        }
    }

    /**
     * 暂停所有下载任务
     */
    fun pauseAllDownloadTask() {
        mDownloadTaskMap.forEach {
            pauseDownloadTask(tag = it.key)
        }
    }

    /**
     * 取消下载任务, 先暂停再移除
     */
    fun cancelDownloadTask(tag: String) {
        pauseDownloadTask(tag = tag)
        removeDownloadTask(tag = tag)
    }

    /**
     * 任务是否活跃
     */
    fun isActiveTask(tag: String): Boolean? {
        return getDownloadTask(tag = tag)?.coroutineContext?.isActive
    }

    companion object {
        @Volatile
        private var sInstance: DownloadTaskPool? = null

        fun getInstance(): DownloadTaskPool {
            return sInstance ?: synchronized(this) {
                sInstance ?: DownloadTaskPool().also { sInstance = it }
            }
        }
    }
}