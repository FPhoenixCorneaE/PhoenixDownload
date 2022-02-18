package com.fphoenixcorneae.download.pool

import com.fphoenixcorneae.download.ext.loge
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @desc：自定义的下载线程池
 * @date：2022/01/26 16:15
 */
class ThreadPoolProxy private constructor() {

    /** 线程池执行者 */
    private var mThreadPoolExecutor: ThreadPoolExecutor? = null

    /** 线程工厂 */
    private val mThreadFactory: ThreadFactory = object : ThreadFactory {
        private val mThreadNo = AtomicInteger()
        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, "DownloadTask#" + mThreadNo.getAndIncrement())
        }
    }

    /** 拒绝策略处理 */
    private val mRejectedExecutionHandler: RejectedExecutionHandler = RejectedExecutionHandler { r, executor ->
        "ThreadPool RejectedExecutionException: Task $r rejected from $executor".loge()
        throw RejectedExecutionException("Task $r rejected from $executor")
    }

    /** 工作队列 */
    private val mWorkQueue: BlockingQueue<Runnable> by lazy {
        if (usePriorityBlockingQueue) {
            PriorityBlockingQueue()
        } else {
            LinkedBlockingQueue(QUEUE_SIZE)
        }
    }

    /** 可同时下载的任务数（核心线程数） */
    var corePoolSize: Int = CORE_POOL_SIZE
        set(value) {
            if (value <= 0) {
                return
            }
            field = value
        }

    /** 缓存队列的大小（最大线程数） */
    var maxPoolSize: Int = MAX_POOL_SIZE
        set(value) {
            if (value <= 0 || value < corePoolSize) {
                return
            }
            field = value
        }

    /** 空闲线程存活时间，秒 */
    var keepAliveTime: Long = KEEP_ALIVE
        set(value) {
            if (value <= 0) {
                return
            }
            field = value
        }

    /** 是否使用优先级队列 */
    var usePriorityBlockingQueue: Boolean = false

    /** 线程池执行者 */
    val threadPoolExecutor: ThreadPoolExecutor
        @Synchronized
        get() {
            if (mThreadPoolExecutor == null || mThreadPoolExecutor?.isShutdown == true) {
                mThreadPoolExecutor = ThreadPoolExecutor(
                    corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS,
                    mWorkQueue, mThreadFactory, mRejectedExecutionHandler
                )
            }
            return mThreadPoolExecutor!!
        }

    companion object {
        /** CPU核心数 */
        private val CPU_COUNT = Runtime.getRuntime().availableProcessors()

        /** 可同时下载的任务数（核心线程数）*/
        private val CORE_POOL_SIZE = 2.coerceAtLeast((CPU_COUNT - 1).coerceAtMost(4));

        /** 缓存队列的大小（最大线程数），默认为 最大线程数为手机CPU数量*2+1 */
        private val MAX_POOL_SIZE = CPU_COUNT * 2 + 1

        /** 非核心线程闲置的超时时间（秒），如果超时则会被回收 */
        private const val KEEP_ALIVE = 60L

        /** 等待队列大小 默认为 128 */
        private const val QUEUE_SIZE = 128

        @Volatile
        private var sInstance: ThreadPoolProxy? = null

        fun getInstance(): ThreadPoolProxy {
            return sInstance ?: synchronized(this) {
                sInstance ?: ThreadPoolProxy().also { sInstance = it }
            }
        }
    }
}