package com.auraai.overlay.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.auraai.overlay.R
import com.auraai.overlay.ai.AiProvider
import com.auraai.overlay.ai.ProviderManager
import com.auraai.overlay.ui.MainActivity

class OverlayService : Service() {

    companion object {
        var isRunning = false
        const val CHANNEL_ID = "auraai_overlay"
        const val NOTIF_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var isPanelOpen = false
    private var stealthMode = false

    private val handler = Handler(Looper.getMainLooper())
    private var currentQuestion = ""

    // ── LIFECYCLE ────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        showBubble()
    }

    override fun onDestroy() {
        isRunning = false
        removeBubble()
        removePanel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    // ── NOTIFICATION ─────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "AuraAI Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AuraAI bubble aktif"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AuraAI Aktif")
            .setContentText("Bubble berjalan — tap untuk buka app")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    // ── BUBBLE VIEW ───────────────────────────────────────────────
    private fun showBubble() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bubbleView = inflater.inflate(R.layout.view_bubble, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }

        setupBubbleDrag(bubbleView!!, params)
        setupBubbleClick()

        windowManager.addView(bubbleView, params)

        // Pulse animation
        val pulse = AnimationUtils.loadAnimation(this, R.anim.bubble_pulse)
        bubbleView?.startAnimation(pulse)
    }

    private fun setupBubbleDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (initialTouchX - event.rawX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBubbleClick() {
        bubbleView?.setOnClickListener {
            if (isPanelOpen) closePanel() else openPanel()
        }
    }

    private fun removeBubble() {
        bubbleView?.let { windowManager.removeView(it); bubbleView = null }
    }

    // ── PANEL VIEW ────────────────────────────────────────────────
    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        panelView = inflater.inflate(R.layout.view_panel, null)

        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 80
        }

        setupPanelButtons()
        windowManager.addView(panelView, params)

        // Slide up animation
        val slideUp = TranslateAnimation(0f, 0f, 300f, 0f).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
        }
        panelView?.startAnimation(slideUp)
    }

    private fun setupPanelButtons() {
        val panel = panelView ?: return

        // Input soal
        val etQuestion = panel.findViewById<EditText>(R.id.etQuestion)
        val tvAnswer = panel.findViewById<TextView>(R.id.tvAnswer)
        val tvStatus = panel.findViewById<TextView>(R.id.tvStatus)
        val progressBar = panel.findViewById<ProgressBar>(R.id.progressBar)
        val switchStealth = panel.findViewById<Switch>(R.id.switchStealth)

        // Tombol aksi
        panel.findViewById<Button>(R.id.btnAnswerThis).setOnClickListener {
            val q = etQuestion.text.toString().trim()
            if (q.isBlank()) { tvStatus.text = "Ketik soalnya dulu!"; return@setOnClickListener }
            currentQuestion = q
            askAI(q, tvAnswer, tvStatus, progressBar)
        }

        panel.findViewById<Button>(R.id.btnExplain).setOnClickListener {
            if (currentQuestion.isBlank()) { tvStatus.text = "Jawab soal dulu!"; return@setOnClickListener }
            val prompt = "Jelaskan langkah-langkah penyelesaian soal berikut secara detail:\n$currentQuestion"
            askAI(prompt, tvAnswer, tvStatus, progressBar)
        }

        panel.findViewById<Button>(R.id.btnClear).setOnClickListener {
            etQuestion.setText("")
            tvAnswer.text = ""
            tvStatus.text = "Ketik atau paste soal di atas"
            currentQuestion = ""
        }

        panel.findViewById<Button>(R.id.btnClose).setOnClickListener {
            closePanel()
        }

        // Stealth toggle
        stealthMode = ProviderManager.getPrefs(this).getBoolean("stealth_mode", false)
        switchStealth.isChecked = stealthMode
        switchStealth.setOnCheckedChangeListener { _, checked ->
            stealthMode = checked
            ProviderManager.getPrefs(this).edit().putBoolean("stealth_mode", checked).apply()
            if (!stealthMode) tvStatus.text = "Mode samaran ${if (checked) "aktif" else "nonaktif"}"
        }
    }

    private fun askAI(
        prompt: String,
        tvAnswer: TextView,
        tvStatus: TextView,
        progressBar: ProgressBar
    ) {
        val prefs = ProviderManager.getPrefs(this)
        val providerId = prefs.getString("selected_provider", "auto") ?: "auto"
        val provider = AiProvider.entries.firstOrNull { it.id == providerId } ?: AiProvider.AUTO

        handler.post {
            progressBar.visibility = android.view.View.VISIBLE
            tvStatus.text = "🤖 Memproses via ${provider.displayName}..."
            tvAnswer.text = ""
        }

        Thread {
            try {
                val systemPrompt = if (prefs.getBoolean("lang_id", true)) {
                    "Kamu adalah asisten ujian cerdas. Jawab soal berikut dengan tepat dan ringkas dalam Bahasa Indonesia. Jika pilihan ganda, sebutkan opsi yang benar. Jika essay, berikan jawaban lengkap."
                } else {
                    "You are a smart exam assistant. Answer questions accurately and concisely."
                }
                val fullPrompt = "$systemPrompt\n\nSoal: $prompt"
                val answer = ProviderManager.ask(prefs, provider, fullPrompt)

                handler.post {
                    progressBar.visibility = android.view.View.GONE
                    tvAnswer.text = answer
                    if (!stealthMode) {
                        tvStatus.text = "✅ Dijawab via ${provider.displayName}"
                    } else {
                        tvStatus.text = ""
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    progressBar.visibility = android.view.View.GONE
                    tvStatus.text = "❌ Error: ${e.message}"
                }
            }
        }.start()
    }

    private fun closePanel() {
        isPanelOpen = false
        val slideDown = TranslateAnimation(0f, 0f, 0f, 300f).apply {
            duration = 200
            interpolator = AccelerateInterpolator()
        }
        slideDown.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(a: Animation?) {}
            override fun onAnimationRepeat(a: Animation?) {}
            override fun onAnimationEnd(a: Animation?) { removePanel() }
        })
        panelView?.startAnimation(slideDown)
    }

    private fun removePanel() {
        panelView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            panelView = null
        }
    }
}
