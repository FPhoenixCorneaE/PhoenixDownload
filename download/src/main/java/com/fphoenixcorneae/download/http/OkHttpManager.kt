package com.fphoenixcorneae.download.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * @desc：OkHttpManager
 * @date：2022/01/27 10:55
 */
internal class OkHttpManager private constructor() {

    /**
     * 断点下载
     */
    suspend fun download(url: String, start: Long): ResponseBody? {
        return withContext(Dispatchers.IO) {
            val commonService = RetrofitFactory.getInstance().createService(CommonService::class.java)
            val response = commonService.download(url = url, start = "bytes=$start-")
            response.body()
        }
    }

    /**
     * 获取头部信息
     */
    suspend fun downloadHead(url: String, start: Long): Response? {
        return withContext(Dispatchers.IO) {
            val okHttpClient = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .header("RANGE", "bytes=$start-")
                .head()
                .build()
            runCatching {
                okHttpClient.newCall(request).execute()
            }.onFailure {
                it.printStackTrace()
            }.getOrNull()
        }
    }

    companion object {
        @Volatile
        private var sInstance: OkHttpManager? = null

        fun getInstance(): OkHttpManager {
            return sInstance ?: synchronized(this) {
                sInstance ?: OkHttpManager().also { sInstance = it }
            }
        }
    }
}