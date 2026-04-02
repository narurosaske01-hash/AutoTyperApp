package com.autotyper

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var textInput: EditText
    private lateinit var speedSeekBar: SeekBar
    private lateinit var speedText: TextView
    private lateinit var delaySeekBar: SeekBar
    private lateinit var delayText: TextView
    private lateinit var startBtn: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textInput = findViewById(R.id.textInput)
        speedSeekBar = findViewById(R.id.speedSeekBar)
        speedText = findViewById(R.id.speedText)
        delaySeekBar = findViewById(R.id.delaySeekBar)
        delayText = findViewById(R.id.delayText)
        startBtn = findViewById(R.id.startBtn)
        statusText = findViewById(R.id.statusText)

        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val wpm = progress + 30
                speedText.text = "Скорость: $wpm WPM"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        delaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                delayText.text = "Задержка: $progress сек"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        startBtn.setOnClickListener {
            val text = textInput.text.toString()
            if (text.isEmpty()) {
                Toast.makeText(this, "Введите текст!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val wpm = speedSeekBar.progress + 30
            val delay = delaySeekBar.progress
            val delayMs = (60000 / (wpm * 6)).coerceIn(25, 350)

            if (!isAccessibilityServiceEnabled()) {
                statusText.text = "⚠️ Включите специальные возможности"
                openAccessibilitySettings()
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
                return@setOnClickListener
            }

            val intent = Intent(this, FloatingButtonService::class.java)
            intent.putExtra("text", text)
            intent.putExtra("delayMs", delayMs)
            intent.putExtra("startDelay", delay)
            startService(intent)

            statusText.text = "✅ Сервис запущен! Переключись в другое приложение"
            startBtn.isEnabled = false
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/.TypingService"
        return try {
            val settings = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            settings?.contains(service) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, "Включите Автотайпер в списке служб", Toast.LENGTH_LONG).show()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivity(intent)
        Toast.makeText(this, "Разрешите показывать поверх других приложений", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityServiceEnabled() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))) {
            startBtn.isEnabled = true
            statusText.text = "✅ Готов к работе"
        }
    }
}
