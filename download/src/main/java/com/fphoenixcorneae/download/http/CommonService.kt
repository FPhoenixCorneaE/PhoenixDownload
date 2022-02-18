package com.fphoenixcorneae.download.http

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * @desc：下载网络请求接口
 * @date：2022/01/27 09:46
 */
internal interface CommonService {

    /**
     * 断点下载
     */
    @Streaming
    @GET
    suspend fun download(
        @Url url: String,
        @Header("RANGE") start: String
    ): Response<ResponseBody?>
}