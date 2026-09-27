package com.nexus.messenger

import android.app.Activity
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import com.nexus.messenger.ui.dp
import java.util.Locale

class VideoFullscreenActivity : Activity() {

    private lateinit var videoView: VideoView
    private lateinit var playBtn: ImageView
    private lateinit var pauseBtn: ImageView
    private lateinit var progress: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var controls: LinearLayout
    private lateinit var backBtn: ImageView
    private val handler = Handler(Looper.getMainLooper())

    private var isPlaying = false
    private var duration = 0
    private var videoUrl: String = ""

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        videoUrl = intent.getStringExtra("videoUrl") ?: run { finish(); return }

        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
        }

        videoView = VideoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setVideoURI(Uri.parse(videoUrl))
        }
        root.addView(videoView)

        controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(16), dp(24), dp(16))
            visibility = View.VISIBLE
        }

        backBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_arrow_back)
            imageTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                setColor(0x99000000.toInt())
                shape = GradientDrawable.OVAL
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        backBtn.setOnClickListener { finish() }
        root.addView(backBtn, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            setMargins(dp(16), dp(16), dp(16), dp(16))
        })

        val btnRow = LinearLayout(this).apply {
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

        btnRow.addView(playBtn, LinearLayout.LayoutParams(dp(64), dp(64)))
        btnRow.addView(pauseBtn, LinearLayout.LayoutParams(dp(64), dp(64)))
        controls.addView(btnRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val progressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }

        currentTime = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            text = "0:00"
        }

        progress = SeekBar(this).apply {
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

        totalTime = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            text = "0:00"
        }

        progressRow.addView(currentTime, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        progressRow.addView(progress, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            leftMargin = dp(12); rightMargin = dp(12)
        })
        progressRow.addView(totalTime, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        controls.addView(progressRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        root.addView(controls, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM
        })

        setContentView(root)

        videoView.setOnPreparedListener { mp ->
            duration = mp.duration
            progress.max = duration
            totalTime.text = fmtTime(duration / 1000)
            mp.start()
            isPlaying = true
            playBtn.visibility = View.GONE
            pauseBtn.visibility = View.VISIBLE
            handler.post(updateProgress)
            
            hideControlsDelayed()
        }

        videoView.setOnCompletionListener {
            isPlaying = false
            playBtn.visibility = View.VISIBLE
            pauseBtn.visibility = View.GONE
            videoView.seekTo(0)
            progress.progress = 0
            currentTime.text = "0:00"
            handler.removeCallbacks(updateProgress)
            controls.visibility = View.VISIBLE
        }

        videoView.setOnErrorListener { _, _, _ ->
            totalTime.text = "Ошибка воспроизведения"
            true
        }

        root.setOnClickListener {
            if (controls.visibility == View.VISIBLE) {
                controls.visibility = View.GONE
                backBtn.visibility = View.GONE
            } else {
                controls.visibility = View.VISIBLE
                backBtn.visibility = View.VISIBLE
                hideControlsDelayed()
            }
        }
    }

    private fun roundButton(drawableRes: Int): ImageView {
        return ImageView(this).apply {
            setImageResource(drawableRes)
            imageTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                setColor(0x99000000.toInt())
                shape = GradientDrawable.OVAL
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            elevation = dp(4).toFloat()
        }
    }

    private fun hideControlsDelayed() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    private val hideControlsRunnable = Runnable {
        if (isPlaying) {
            controls.visibility = View.GONE
            backBtn.visibility = View.GONE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgress)
        handler.removeCallbacks(hideControlsRunnable)
        videoView.stopPlayback()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun fmtTime(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }
}