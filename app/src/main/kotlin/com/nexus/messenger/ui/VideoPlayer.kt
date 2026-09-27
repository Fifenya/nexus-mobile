package com.nexus.messenger.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import com.nexus.messenger.R
import com.nexus.messenger.data.Store
import java.util.Locale

class VideoPlayer(context: Context) : FrameLayout(context) {

    private val videoView: VideoView
    private val playBtn: ImageView
    private val pauseBtn: ImageView
    private val progress: SeekBar
    private val currentTime: TextView
    private val totalTime: TextView
    private val controls: LinearLayout
    private val handler = Handler(Looper.getMainLooper())

    private var isPlaying = false
    private var duration = 0

    private val updateProgress = object : Runnable {
        override fun run() {
            if (isPlaying) {
                val pos = videoView.currentPosition
                progress.setProgress(pos, false)
                currentTime.text = fmtTime(pos / 1000)
                handler.postDelayed(this, 500)
            }
        }
    }

    init {
        setBackgroundColor(0xFF000000.toInt())
        clipToOutline = true

        videoView = VideoView(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        addView(videoView)

        controls = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(12))
            visibility = View.VISIBLE
        }

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        playBtn = roundButton(R.drawable.ic_play)
        playBtn.setOnClickListener {
            videoView.start()
            isPlaying = true
            playBtn.visibility = View.GONE
            pauseBtn.visibility = View.VISIBLE
            handler.post(updateProgress)
        }

        pauseBtn = roundButton(R.drawable.ic_pause)
        pauseBtn.visibility = View.GONE
        pauseBtn.setOnClickListener {
            videoView.pause()
            isPlaying = false
            pauseBtn.visibility = View.GONE
            playBtn.visibility = View.VISIBLE
            handler.removeCallbacks(updateProgress)
        }

        btnRow.addView(playBtn, LinearLayout.LayoutParams(dp(56), dp(56)))
        btnRow.addView(pauseBtn, LinearLayout.LayoutParams(dp(56), dp(56)))
        controls.addView(btnRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val progressRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }

        currentTime = TextView(context).apply {
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            text = "0:00"
        }

        progress = SeekBar(context).apply {
            max = 1000
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(0xFFDC2626.toInt())
            thumbTintList = android.content.res.ColorStateList.valueOf(0xFFDC2626.toInt())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val newPos = (p * duration) / 1000
                        videoView.seekTo(newPos)
                        currentTime.text = fmtTime(newPos / 1000)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        totalTime = TextView(context).apply {
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            text = "0:00"
        }

        progressRow.addView(currentTime, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        progressRow.addView(progress, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            leftMargin = dp(8); rightMargin = dp(8)
        })
        progressRow.addView(totalTime, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        controls.addView(progressRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        addView(controls, LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM
        })

        videoView.setOnPreparedListener { mp ->
            duration = mp.duration
            progress.max = duration
            totalTime.text = fmtTime(duration / 1000)
            mp.start()
            mp.pause()
        }

        videoView.setOnCompletionListener {
            isPlaying = false
            playBtn.visibility = View.VISIBLE
            pauseBtn.visibility = View.GONE
            videoView.seekTo(0)
            progress.progress = 0
            currentTime.text = "0:00"
            handler.removeCallbacks(updateProgress)
        }

        videoView.setOnErrorListener { _, _, _ ->
            totalTime.text = "ошибка"
            true
        }

        setOnClickListener {
            controls.visibility = if (controls.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun roundButton(drawableRes: Int): ImageView {
        return ImageView(context).apply {
            setImageResource(drawableRes)
            imageTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                setColor(0x99000000.toInt())
                shape = GradientDrawable.OVAL
            }
            setPadding(dp(14), dp(14), dp(14), dp(14))
            elevation = dp(4).toFloat()
        }
    }

    fun setVideoUrl(url: String) {
        val fullUrl = if (url.startsWith("http") || url.startsWith("mock://")) url else Store.apiBase + url
        videoView.setVideoURI(Uri.parse(fullUrl))
    }

    fun release() {
        handler.removeCallbacks(updateProgress)
        videoView.stopPlayback()
    }

    private fun fmtTime(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }
}