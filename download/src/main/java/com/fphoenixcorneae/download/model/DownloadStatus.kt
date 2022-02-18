package com.fphoenixcorneae.download.model

import androidx.annotation.Keep

/**
 * @desc：下载状态
 * @date：2022/01/26 11:24
 */
@Keep
sealed class DownloadStatus(open val tag: String? = null, val status: Int) {
    /**
     * 未在任务队列中
     */
    object Default : DownloadStatus(tag = null, status = 0)

    /**
     * 下载准备
     */
    data class Prepare(
        override val tag: String? = null
    ) : DownloadStatus(tag = tag, status = 1)

    /**
     * 下载中
     * @param tag         标识
     * @param progress    进度
     * @param currentSize 当前长度
     * @param totalSize   总共长度
     * @param isCompleted 是否完成
     */
    data class Progress(
        override val tag: String? = null,
        val progress: Float = 0f,
        val currentSize: Long = 0,
        val totalSize: Long = 0,
        val isCompleted: Boolean = false
    ) : DownloadStatus(tag = tag, status = 2)

    /**
     * 下载成功
     * @param localPath 本地路径
     * @param totalSize 总共长度
     */
    data class Success(
        override val tag: String? = null,
        val localPath: String? = null,
        val totalSize: Long = 0
    ) : DownloadStatus(tag = tag, status = 3)

    /**
     * 下载暂停
     */
    data class Pause(
        override val tag: String? = null
    ) : DownloadStatus(tag = tag, status = 4)

    /**
     * 下载取消
     */
    data class Cancel(
        override val tag: String? = null
    ) : DownloadStatus(tag = tag, status = 5)

    /**
     * 下载出错
     * @param message 错误信息
     */
    data class Error(
        override val tag: String? = null,
        val message: String? = null
    ) : DownloadStatus(tag = tag, status = 6)
}
