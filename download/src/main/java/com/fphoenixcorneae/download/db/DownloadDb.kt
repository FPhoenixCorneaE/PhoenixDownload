package com.fphoenixcorneae.download.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fphoenixcorneae.download.const.Constant
import com.fphoenixcorneae.download.ext.applicationContext

/**
 * @desc：下载数据库
 * @date：2022/01/26 13:59
 */
@Database(entities = [DownloadEntity::class], version = 1)
internal abstract class DownloadDb : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var sInstance: DownloadDb? = null

        @Synchronized
        fun getInstance(): DownloadDb {
            return sInstance ?: synchronized(this) {
                sInstance ?: createDatabase().also { sInstance = it }
            }
        }

        private fun createDatabase(): DownloadDb {
            return Room.databaseBuilder(applicationContext, DownloadDb::class.java, Constant.DB_NAME_DOWNLOAD)
                // 数据库更新时删除数据重新创建
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}