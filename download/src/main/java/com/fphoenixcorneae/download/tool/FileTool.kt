package com.fphoenixcorneae.download.tool

import com.fphoenixcorneae.download.db.DownloadDbHelper
import com.fphoenixcorneae.download.model.DownloadStatus
import com.fphoenixcorneae.download.ext.formatAs2DecimalPlaces
import com.fphoenixcorneae.download.ext.loge
import com.fphoenixcorneae.download.ext.logi
import com.fphoenixcorneae.download.pool.DownloadTaskPool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * @desc：文件读写工具
 * @date：2021/11/19 14:44
 */
object FileTool {

    /**
     * 8K的缓存
     */
    private const val TRANSFER_BUFFER_SIZE = 8 * 1024

    /**
     * "r": 以只读方式打开。调用结果对象的任何 write 方法都将导致抛出 IOException。
     * "rw": 打开以便读取和写入。
     * "rws": 打开以便读取和写入。相对于 "rw"，"rws" 还要求对“文件的内容”或“元数据”的每个更新都同步写入到基础存储设备。
     * "rwd": 打开以便读取和写入，相对于 "rw"，"rwd" 还要求对“文件的内容”的每个更新都同步写入到基础存储设备。
     */
    private const val RANDOM_ACCESS_FILE_MODE = "rwd"

    /** 下载任务池 */
    private val mDownloadTaskPool by lazy { DownloadTaskPool.getInstance() }

    /**
     * 下载文件到本地
     * @param tag String
     * @param filePath String
     * @param currentSize Long
     * @param responseBody ResponseBody
     * @param downloadStatus DownloadStatus
     */
    suspend fun saveFile2Local(
        tag: String,
        filePath: String,
        currentSize: Long,
        responseBody: ResponseBody,
        downloadStatus: MutableStateFlow<DownloadStatus>? = null,
    ) {
        runCatching {
            if (createFile(filePath = filePath).not()) {
                // 创建文件失败, 回调下载错误
                val errorMsg =
                    "download tag: $tag, error: create new file [$filePath] failed.".also {
                        it.loge()
                    }
                downloadStatus?.value = DownloadStatus.Error(
                    tag = tag,
                    message = errorMsg
                )
                // 取消任务
                mDownloadTaskPool.cancelDownloadTask(tag = tag)
                // 更新数据库
                DownloadDbHelper.downloadError(tag = tag, errorMsg = errorMsg)
                return
            }
            // 创建文件成功, 执行保存文件操作
            saveToFile(
                tag = tag,
                filePath = filePath,
                currentSize = currentSize,
                responseBody = responseBody,
                downloadStatus = downloadStatus
            )
        }.onFailure {
            "download tag: $tag, error in create new file: $it.".loge()
            // 出现异常，回调下载错误
            downloadStatus?.value = DownloadStatus.Error(tag = tag, message = it.message)
            // 取消任务
            mDownloadTaskPool.cancelDownloadTask(tag = tag)
            // 更新数据库
            DownloadDbHelper.downloadError(tag = tag, errorMsg = it.message)
        }
    }

    /**
     * @param currentSize Long
     * @param responseBody ResponseBody
     * @param filePath String
     * @param tag String
     * @param downloadStatus DownloadStatus
     * RandomAccessFile使用参考：http://www.javased.com/index.php?api=java.io.RandomAccessFile
     */
    suspend fun saveToFile(
        tag: String,
        filePath: String,
        currentSize: Long,
        responseBody: ResponseBody,
        downloadStatus: MutableStateFlow<DownloadStatus>? = null,
    ) {
        // RandomAccessFile既可以读取文件内容，也可以向文件输出数据。
        // 同时，RandomAccessFile支持“随机访问”的方式，程序快可以直接跳转到文件的任意地方来读写数据。
        // RandomAccessFile的一个重要使用场景就是网络请求中的多线程下载及断点续传。
        var randomAccessFile: RandomAccessFile? = null
        var responseByteStream: InputStream? = null
//        var channel: FileChannel? = null
        try {
            // 获取文件总长度
            val fileTotalSize = getFileTotalSize(currentSize, responseBody)
            randomAccessFile = RandomAccessFile(filePath, RANDOM_ACCESS_FILE_MODE)
            responseByteStream = responseBody.byteStream()
            // 文件过大，channel.map()会出现异常：java.io.IOException: Map failed.
//            channel = saveRandomAccessFile.channel
//            val mappedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, currentSize, fileTotalSize - currentSize)

            randomAccessFile.setLength(fileTotalSize - currentSize)
            randomAccessFile.seek(currentSize)
            val transferBuffer = ByteArray(TRANSFER_BUFFER_SIZE)
            var numBytesRead: Int
            var lastProgress = 0f
            // 当前已保存的长度
            var currentSaveLength = currentSize
            while (responseByteStream.read(transferBuffer).also { numBytesRead = it } != -1) {
//                mappedBuffer.put(transferBuffer, 0, numBytesRead)
                // 写缓存数据到文件
                randomAccessFile.write(transferBuffer, 0, numBytesRead)
                currentSaveLength += numBytesRead

                // 计算百分比
                val progress = (currentSaveLength.toFloat() / fileTotalSize * 100).formatAs2DecimalPlaces()
                if (lastProgress != progress) {
                    lastProgress = progress
                    // 记录已经下载的长度，回调下载中
                    val isCompleted = currentSaveLength == fileTotalSize
                    "download tag: $tag, progress: $progress, currentSize: $currentSaveLength, totalSize: $fileTotalSize, isCompleted: $isCompleted.".logi()
                    withContext(Dispatchers.Main) {
                        downloadStatus?.value = DownloadStatus.Progress(
                            tag = tag,
                            progress = progress,
                            currentSize = currentSaveLength,
                            totalSize = fileTotalSize,
                            isCompleted = isCompleted,
                        )
                    }

                    // 加锁保证已下载的正确性
                    synchronized(this) {
                        // 更新数据库
                        DownloadDbHelper.downloadProgress(
                            tag = tag,
                            currentSize = currentSaveLength,
                            totalSize = fileTotalSize,
                            progress = progress
                        )

                        if (isCompleted) {
                            // 回调下载完成
                            "download tag: $tag, success: localPath: $filePath totalSize: $fileTotalSize.".logi()
                            downloadStatus?.value = DownloadStatus.Success(
                                tag = tag,
                                localPath = filePath,
                                totalSize = fileTotalSize
                            )
                            // 文件保存完成，移除任务
                            mDownloadTaskPool.removeDownloadTask(tag = tag)
                            // 更新数据库
                            DownloadDbHelper.downloadSuccess(tag = tag)
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            // ignore: kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled; job=StandaloneCoroutine{Cancelling}@c5bcce9
        } catch (e: Exception) {
            "download tag: $tag, error in save file: $e.".loge()
            // 出现异常，回调下载错误
            downloadStatus?.value = DownloadStatus.Error(tag = tag, message = e.message)
            // 下载失败，取消任务
            mDownloadTaskPool.cancelDownloadTask(tag = tag)
            // 更新数据库
            DownloadDbHelper.downloadError(tag = tag, errorMsg = e.message)
        } finally {
            randomAccessFile?.closeQuietly()
            responseByteStream?.closeQuietly()
//            channel?.closeQuietly()
        }
    }

    /**
     * 创建文件夹
     * @param filePath String
     * @return Boolean
     */
    private fun createFile(filePath: String): Boolean {
        val file = File(filePath)
        if (file.exists().not()) {
            return file.createNewFile()
        }
        return true
    }

    /**
     * 数据总长度
     * @param currentSize Long
     * @param responseBody ResponseBody
     * @return Long
     */
    private fun getFileTotalSize(
        currentSize: Long,
        responseBody: ResponseBody
    ) =
        if (currentSize == 0L) {
            responseBody.contentLength()
        } else {
            currentSize + responseBody.contentLength()
        }
}

