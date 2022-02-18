package com.fphoenixcorneae.download.http

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.internal.closeQuietly
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * @desc：RetrofitFactory
 * @date：2022/01/27 09:56
 */
internal class RetrofitFactory private constructor() {

    /** KeyStore */
    private var mKeyStore: KeyStore? = null

    /** X509TrustManager */
    private val mX509TrustManager by lazy {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(mKeyStore)
        val trustManagers = trustManagerFactory.trustManagers
        check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
            "Unexpected default trust managers:${Arrays.toString(trustManagers)}"
        }
        trustManagers[0] as X509TrustManager
    }

    /** SslSocketFactory */
    private val mSslSocketFactory by lazy {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(mX509TrustManager), SecureRandom())
        sslContext.socketFactory
    }

    /** OkHttpClient 实例 */
    private val mOkHttpClient by lazy {
        OkHttpClient.Builder()
            // 日志拦截器
            .addInterceptor(HttpLoggingInterceptor {
                Log.d("OkHttp", it)
            }.apply {
                setLevel(HttpLoggingInterceptor.Level.BASIC)
            })
            .sslSocketFactory(mSslSocketFactory, mX509TrustManager)
            //超时时间 连接、读、写
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /** Retrofit 实例 */
    private val mRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.baidu.com")
            .client(mOkHttpClient)
            .build()
    }

    /**
     * https请求时初始化证书
     */
    fun setCertificate(vararg certificates: InputStream) {
        mKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        mKeyStore?.load(null)
        val certificateFactory = CertificateFactory.getInstance("X.509")
        certificates.forEachIndexed { index, certificate ->
            val certificateAlias = (index + 1).toString()
            mKeyStore?.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate))
            certificate.closeQuietly()
        }
    }

    /**
     * 创建Service
     */
    fun <T> createService(service: Class<T>): T {
        return mRetrofit.create(service)
    }

    companion object {
        @Volatile
        private var sInstance: RetrofitFactory? = null

        /**
         * 获取单例
         */
        fun getInstance(): RetrofitFactory {
            return sInstance ?: synchronized(this) {
                sInstance ?: RetrofitFactory().also { sInstance = it }
            }
        }
    }
}