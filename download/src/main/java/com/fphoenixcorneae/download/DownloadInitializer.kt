package com.fphoenixcorneae.download

import android.app.Application
import androidx.core.content.FileProvider

/**
 * @desc：UpDownloadProvider
 * @date：2022/01/27 18:00
 */
class DownloadInitializer : FileProvider() {

    companion object {
        lateinit var sApplication: Application
    }

    override fun onCreate(): Boolean {
        sApplication = context?.applicationContext as Application
        return true
    }
}