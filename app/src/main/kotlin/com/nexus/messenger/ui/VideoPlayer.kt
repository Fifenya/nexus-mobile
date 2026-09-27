package com.nexus.messenger.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import com.nexus.messenger.R
import com.nexus.messenger.VideoFullscreenActivity

class VideoPlayer(context: Context) : FrameLayout(context) {

    private val videoView: VideoView
    private val playBtn: ImageView
    private var videoUrl: String = ""

    init {
        setBackgroundColor(0xFF000000.toInt())
        clipToOutline = true

        videoView = VideoView(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isClickable = false
            isFocusable = false
        }
        addView(videoView)

        playBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_play)
            imageTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                setColor(0x99000000.toInt())
                shape = GradientDrawable.OVAL
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            elevation = dp(6).toFloat()
        }
        addView(playBtn, LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })

        setOnClickListener {
            if (videoUrl.isNotEmpty()) {
                val intent = Intent(context, VideoFullscreenActivity::class.java)
                intent.putExtra("videoUrl", videoUrl)
                context.startActivity(intent)
            }
        }
    }

    fun setVideoUrl(url: String) {
        videoUrl = url
        val fullUrl = if (url.startsWith("http") || url.startsWith("mock://")) url else com.nexus.messenger.data.Store.apiBase + url
        videoView.setVideoURI(Uri.parse(fullUrl))
        videoView.setOnPreparedListener { mp ->
            mp.start()
            mp.pause()
            mp.seekTo(0)
        }
    }

    fun release() {
        videoView.stopPlayback()
    }
}