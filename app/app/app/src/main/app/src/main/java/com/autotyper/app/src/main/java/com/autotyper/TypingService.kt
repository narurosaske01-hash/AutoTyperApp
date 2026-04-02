package com.autotyper

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TypingService : AccessibilityService() {

    companion object {
        var isTyping = false
        var textToType = ""
        var delayBetweenChars = 100
        var currentIndex = 0
        var onCompleteCallback: (() -> Unit)? = null

        fun startTyping() {
            isTyping = true
            currentIndex = 0
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        startTypingRunnable()
    }

    private fun startTypingRunnable() {
        typingRunnable = object : Runnable {
            override fun run() {
                if (!isTyping || currentIndex >= textToType.length) {
                    if (currentIndex >= textToType.length) {
                        isTyping = false
                        onCompleteCallback?.invoke()
                    }
                    return
                }
                
                val char = textToType[currentIndex].toString()
                typeChar(char)
                currentIndex++
                handler.postDelayed(this, delayBetweenChars.toLong())
            }
        }
        handler.post(typingRunnable!!)
    }

    private fun typeChar(char: String) {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        
        if (focusedNode != null) {
            val currentText = focusedNode.text?.toString() ?: ""
            val newText = currentText + char
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { isTyping = false }
    override fun onDestroy() { isTyping = false; super.onDestroy() }
}
