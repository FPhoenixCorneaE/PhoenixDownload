package com.fphoenixcorneae.download.db

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fphoenixcorneae.download.const.Constant

/**
 * @desc：下载信息数据库实体
 * @date：2022/01/26 11:31
 */
@Keep
@Entity(tableName = Constant.TABLE_NAME_DOWNLOAD)
data class DownloadEntity(
    /** 主键，自增长 */
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    /** 下载标识 */
    var tag: String? = null,
    /** 下载地址 */
    var url: String? = null,
    /** 文件路径 */
    var localPath: String? = null,
    /** 文件名称 */
    var name: String? = null,
    /** 当前长度 */
    var currentSize: Long = 0,
    /** 总共长度 */
    var totalSize: Long = 0,
    /** 下载进度 */
    var progress: Float = 0f,
    /** 下载状态 */
    var status: Int = 0,
    /** 错误信息 */
    var errorMsg: String? = null,
    /** 创建时间 */
    var createDate: Long = 0,
    /** 文件最后修改时间 */
    var lastModifiedTime: Long = 0
)