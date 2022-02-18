package com.fphoenixcorneae.download.db

import com.fphoenixcorneae.download.model.DownloadStatus
import com.fphoenixcorneae.download.ext.globalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @desc：DownloadDbHelper
 * @date：2022/02/08 14:16
 */
internal object DownloadDbHelper {

    /**
     * 下载准备
     */
    fun downloadPrepare(
        tag: String,
        url: String,
        name: String,
        localPath: String
    ) {
        globalScope.launch(Dispatchers.IO) {
            DownloadDb.getInstance().downloadDao().apply {
                if (queryByTag(tag) == null) {
                    insert(
                        DownloadEntity(
                            tag = tag,
                            url = url,
                            name = name,
                            localPath = localPath,
                            status = DownloadStatus.Prepare().status,
                            createDate = System.currentTimeMillis(),
                            lastModifiedTime = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    /**
     * 下载中
     */
    fun downloadProgress(
        tag: String,
        currentSize: Long,
        totalSize: Long,
        progress: Float
    ) {
        globalScope.launch(Dispatchers.IO) {
            DownloadDb.getInstance().downloadDao().apply {
                queryByTag(tag = tag)?.let {
                    it.currentSize = currentSize
                    it.totalSize = totalSize
                    it.progress = progress
                    it.status = DownloadStatus.Progress().status
                    it.errorMsg = ""
                    it.lastModifiedTime = System.currentTimeMillis()
                    update(it)
                }
            }
        }
    }

    /**
     * 下载暂停
     */
    fun downloadPause(tag: String) {
        globalScope.launch(Dispatchers.IO) {
            // 延迟10毫秒，避免被Progress状态覆盖掉
            delay(10)
            DownloadDb.getInstance().downloadDao().updateStatus(tag = tag, DownloadStatus.Pause())
        }
    }

    /**
     * 下载成功
     */
    fun downloadSuccess(tag: String) {
        globalScope.launch(Dispatchers.IO) {
            // 延迟10毫秒，避免被Progress状态覆盖掉
            delay(10)
            DownloadDb.getInstance().downloadDao().updateStatus(tag = tag, DownloadStatus.Success())
        }
    }

    /**
     * 下载取消
     */
    fun downloadCancel(tag: String) {
        globalScope.launch(Dispatchers.IO) {
            // 延迟10毫秒，避免被Progress状态覆盖掉
            delay(10)
            DownloadDb.getInstance().downloadDao().updateStatus(tag = tag, DownloadStatus.Cancel())
        }
    }

    /**
     * 下载错误
     */
    fun downloadError(tag: String, errorMsg: String?) {
        globalScope.launch(Dispatchers.IO) {
            // 延迟10毫秒，避免被Progress状态覆盖掉
            delay(10)
            DownloadDb.getInstance().downloadDao().updateStatus(tag = tag, DownloadStatus.Error(), errorMsg = errorMsg)
        }
    }

    /**
     * 根据tag查询
     */
    suspend fun queryByTag(tag: String): DownloadEntity? {
        return withContext(Dispatchers.IO) {
            DownloadDb.getInstance().downloadDao().queryByTag(tag = tag)
        }
    }

    /**
     * 查询所有
     */
    suspend fun queryAll(): List<DownloadEntity?>? {
        return withContext(Dispatchers.IO) {
            DownloadDb.getInstance().downloadDao().queryAll()
        }
    }

    /**
     * 根据tag查询currentSize
     */
    suspend fun queryCurrentSize(tag: String): Long {
        return withContext(Dispatchers.IO) {
            DownloadDb.getInstance().downloadDao().queryCurrentSize(tag = tag)
        }
    }

    /**
     * 根据tag查询totalSize
     */
    suspend fun queryTotalSize(tag: String): Long {
        return withContext(Dispatchers.IO) {
            DownloadDb.getInstance().downloadDao().queryTotalSize(tag = tag)
        }
    }

    /**
     * 监听下载数据变化
     * 注意：SQLite 数据库的内容更新通知功能是以表 (Table) 数据为单位，而不是以行 (Row) 数据为单位，因此只要是表中的数据有更新，它就触发内容更新通知。
     * 可以使用 Flow 的操作符，比如 distinctUntilChanged() 确保只有在当您关心的数据有更新时才会收到通知。
     */
    fun collectDownload(tag: String): Flow<DownloadEntity?> {
        return DownloadDb.getInstance().downloadDao().collectDownload(tag = tag).distinctUntilChanged()
    }
}