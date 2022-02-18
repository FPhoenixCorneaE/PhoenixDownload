package com.fphoenixcorneae.download.ext

import android.app.Application
import com.fphoenixcorneae.download.DownloadInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.text.DecimalFormat

/** Application上下文，全局可使用 */
val applicationContext: Application by lazy { DownloadInitializer.sApplication }

/** 生命周期与 Application 一样长的协程，可以做一些后台作业 */
val globalScope: CoroutineScope by lazy {
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

/**
 * 格式化文件大小
 */
fun Long.formatSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    val tb = gb / 1024.0
    val df = DecimalFormat("0.00")
    val resultSize: String = when {
        tb > 1 -> {
            df.format(tb).plus("TB")
        }
        gb > 1 -> {
            df.format(gb).plus("GB")
        }
        mb > 1 -> {
            df.format(mb).plus("MB")
        }
        kb > 1 -> {
            df.format(kb).plus("KB")
        }
        else -> {
            df.format(this.toDouble()).plus("B")
        }
    }
    return resultSize
}

/**
 * 浮点数格式为小数点后两位
 */
fun Float.formatAs2DecimalPlaces(): Float {
    return DecimalFormat("0.00").format(this).toFloat()
}