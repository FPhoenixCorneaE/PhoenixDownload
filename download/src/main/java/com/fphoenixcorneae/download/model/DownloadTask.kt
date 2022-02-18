package com.fphoenixcorneae.download.model

import androidx.annotation.Keep
import kotlin.coroutines.CoroutineContext

/**
 * @desc：下载任务
 * @date：2022/01/27 14:24
 */
@Keep
data class DownloadTask(
    /** 下载任务作用域 */
    var coroutineContext: CoroutineContext? = null,
    /** 下载标识 */
    val tag: String,
    /** 下载地址 */
    val url: String,
    /** 文件名称 */
    val name: String,
    /** 下载的文件保存本地路径 */
    val localPath: String,
)
