package com.fphoenixcorneae.download.demo

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fphoenixcorneae.download.DownloadManager
import com.fphoenixcorneae.download.model.DownloadStatus
import com.fphoenixcorneae.download.ext.formatSize
import com.fphoenixcorneae.download.ext.globalScope
import kotlinx.coroutines.launch

class DownloadManageAdapter(private val downloadInfos: List<DownloadInfo>?) :
    RecyclerView.Adapter<DownloadManageAdapter.ViewHolder>() {

    private lateinit var mContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mContext = parent.context
        return ViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.recycler_item_download_manage, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        downloadInfos?.run {
            holder.tvName.text = get(position).name
            holder.btnDownload.setOnClickListener {
                (it as TextView).apply {
                    when (text) {
                        "开始下载" -> {
                            DownloadManager.getInstance()
                                .download(get(position).url, get(position).url, "${get(position).name}.apk")
                        }
                        "继续下载" -> {
                            DownloadManager.getInstance().continueDownload(get(position).url)
                        }
                        "暂停下载" -> {
                            DownloadManager.getInstance().stopDownload(get(position).url)
                        }
                        "重新下载" -> {
                            DownloadManager.getInstance()
                                .download(
                                    get(position).url,
                                    get(position).url,
                                    "${get(position).name}.apk",
                                    reDownload = true
                                )
                        }
                    }
                }
            }
            globalScope.launch {
                DownloadManager.getInstance().collectDownload(get(position).url) {
                    holder.pbProgress.progress = it?.progress?.toInt() ?: 0
                    holder.tvProgress.text = "${it?.progress ?: 0f}%"
                    when (it?.status) {
                        DownloadStatus.Prepare().status -> {
                            holder.btnDownload.setBackgroundColor(
                                ContextCompat.getColor(
                                    mContext,
                                    android.R.color.holo_orange_light
                                )
                            )
                            holder.btnDownload.text = "准备下载"
                            holder.btnDownload.isEnabled = false
                        }
                        DownloadStatus.Progress().status -> {
                            holder.tvSize.text =
                                it.currentSize.formatSize().plus("/").plus(it.totalSize.formatSize())
                            holder.btnDownload.setBackgroundColor(
                                ContextCompat.getColor(
                                    mContext,
                                    android.R.color.holo_purple
                                )
                            )
                            holder.btnDownload.text = "暂停下载"
                            holder.btnDownload.isEnabled = true
                        }
                        DownloadStatus.Pause().status -> {
                            holder.tvSize.text =
                                it.currentSize.formatSize().plus("/").plus(it.totalSize.formatSize())
                            holder.btnDownload.setBackgroundColor(
                                ContextCompat.getColor(
                                    mContext,
                                    android.R.color.holo_blue_light
                                )
                            )
                            holder.btnDownload.text = "继续下载"
                            holder.btnDownload.isEnabled = true
                        }
                        DownloadStatus.Cancel().status -> {
                            holder.btnDownload.setBackgroundColor(
                                ContextCompat.getColor(
                                    mContext,
                                    android.R.color.holo_blue_light
                                )
                            )
                            holder.btnDownload.text = "重新下载"
                            holder.btnDownload.isEnabled = true
                        }
                        DownloadStatus.Success().status -> {
                            holder.tvSize.text =
                                it.currentSize.formatSize().plus("/").plus(it.totalSize.formatSize())
                            holder.btnDownload.setBackgroundColor(
                                ContextCompat.getColor(
                                    mContext,
                                    android.R.color.holo_green_dark
                                )
                            )
                            holder.btnDownload.text = "重新下载"
                            holder.btnDownload.isEnabled = true
                        }
                        DownloadStatus.Error().status -> {
                            holder.tvSize.text =
                                it.currentSize.formatSize().plus("/").plus(it.totalSize.formatSize())
                            holder.btnDownload.setBackgroundColor(
                                ContextCompat.getColor(
                                    mContext,
                                    android.R.color.holo_red_light
                                )
                            )
                            holder.btnDownload.text = "重新下载"
                            holder.btnDownload.isEnabled = true
                        }
                        else -> {
                            holder.btnDownload.setBackgroundColor(
                                ContextCompat.getColor(
                                    mContext,
                                    android.R.color.holo_blue_light
                                )
                            )
                            holder.btnDownload.text = "开始下载"
                            holder.btnDownload.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return downloadInfos?.size ?: 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvSize: TextView = itemView.findViewById(R.id.tvSize)
        val btnDownload: Button = itemView.findViewById(R.id.btnDownload)
        val pbProgress: ProgressBar = itemView.findViewById(R.id.pbProgress)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
    }
}

