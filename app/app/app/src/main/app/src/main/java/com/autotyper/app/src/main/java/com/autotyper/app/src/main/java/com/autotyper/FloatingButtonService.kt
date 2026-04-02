package com.autotyper

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.TextView
import android.widget.Toast

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var progressText: TextView
    
    private var textToType = ""
    private var delayMs = 100
    private var startDelay = 0
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingButton()
        startForeground()
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("autotyper", "AutoTyper", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            
            val notification = Notification.Builder(this, "autotyper")
                .setContentTitle("Автотайпер")
                .setContentText("Готов к печати")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .build()
            startForeground(1, notification)
        }
    }

    private fun createFloatingButton() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_button, null)
        progressText = floatingView.findViewById(R.id.progressText)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200
        
        windowManager.addView(floatingView, params)
        
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
                        startTypingProcess()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startTypingProcess() {
        if (isRunning) {
            isRunning = false
            TypingService.isTyping = false
            progressText.text = "⏹️ Остановлено"
            return
        }
        
        if (textToType.isEmpty()) {
            Toast.makeText(this, "Нет текста", Toast.LENGTH_SHORT).show()
            return
        }
        
        isRunning = true
        var countdown = startDelay
        progressText.text = "⏰ $countdown сек"
        
        val countdownHandler = Handler(Looper.getMainLooper())
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (countdown > 0 && isRunning) {
                    countdown--
                    progressText.text = "⏰ $countdown сек"
                    countdownHandler.postDelayed(this, 1000)
                } else if (isRunning) {
                    progressText.text = "⌨️ ПЕЧАТАЮ..."
                    TypingService.textToType = textToType
                    TypingService.delayBetweenChars = delayMs
                    TypingService.onCompleteCallback = {
                        Handler(Looper.getMainLooper()).post {
                            isRunning = false
                            progressText.text = "✅ Готово"
                            Handler(Looper.getMainLooper()).postDelayed({
                                progressText.text = "▶️ Нажми для старта"
                            }, 2000)
                        }
                    }
                    TypingService.startTyping()
                }
            }
        }
        countdownHandler.post(countdownRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        textToType = intent?.getStringExtra("text") ?: ""
        delayMs = intent?.getIntExtra("delayMs", 100) ?: 100
        startDelay = intent?.getIntExtra("startDelay", 0) ?: 0
        progressText.text = "▶️ Нажми для старта"
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        TypingService.isTyping = false
    }
}
