package com.fphoenixcorneae.download.db

import androidx.room.*
import com.fphoenixcorneae.download.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * @desc：下载数据访问对象
 * @date：2022/01/26 14:54
 */
@Dao
internal interface DownloadDao {
    /**
     * 插入
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vararg downloadEntity: DownloadEntity)

    /**
     * 更新
     */
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(vararg downloadEntity: DownloadEntity)

    /**
     * 根据tag更新下载状态
     */
    @Transaction
    suspend fun updateStatus(tag: String, downloadStatus: DownloadStatus, errorMsg: String? = null) {
        queryByTag(tag)?.let { downloadEntity ->
            downloadEntity.status = downloadStatus.status
            downloadEntity.errorMsg = errorMsg
            update(downloadEntity)
        }
    }

    /**
     * 删除
     */
    @Delete
    suspend fun delete(vararg downloadEntity: DownloadEntity)

    /**
     * 根据tag删除
     */
    @Transaction
    suspend fun delete(vararg tags: String) {
        tags.forEach { tag ->
            queryByTag(tag)?.let { downloadEntity ->
                delete(downloadEntity)
            }
        }
    }

    /**
     * 根据tag查询
     */
    @Query("SELECT * FROM download WHERE tag = :tag")
    suspend fun queryByTag(tag: String): DownloadEntity?

    /**
     * 查询所有
     */
    @Query("SELECT * FROM download ORDER BY id ASC")
    suspend fun queryAll(): List<DownloadEntity?>?

    /**
     * 根据tag查询currentSize
     */
    @Transaction
    suspend fun queryCurrentSize(tag: String): Long {
        return queryByTag(tag = tag)?.currentSize ?: 0
    }

    /**
     * 根据tag查询totalSize
     */
    @Transaction
    suspend fun queryTotalSize(tag: String): Long {
        return queryByTag(tag = tag)?.totalSize ?: 0
    }

    /**
     * 根据tag观察数据变化
     */
    @Query("SELECT * FROM download WHERE tag = :tag")
    fun collectDownload(tag: String): Flow<DownloadEntity?>
}