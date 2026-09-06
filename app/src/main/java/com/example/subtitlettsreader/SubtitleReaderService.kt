package com.example.subtitlettsreader

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class SubtitleReaderService : AccessibilityService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lastSpokenText: String = ""

    companion object {
        private const val TAG = "SubtitleReader"
        private const val BOTTOM_THRESHOLD_RATIO = 0.70f
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
        Log.d(TAG, "Service kết nối thành công")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("vi", "VN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            tts?.setSpeechRate(1.15f)
            isTtsReady = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val bottomThreshold = (screenHeight * BOTTOM_THRESHOLD_RATIO).toInt()

        val detectedSubtitles = mutableListOf<String>()
        scanSubtitlesAtBottom(rootNode, bottomThreshold, detectedSubtitles)

        if (detectedSubtitles.isNotEmpty()) {
            val fullText = detectedSubtitles.joinToString(" ").trim()
            if (fullText.length >= 2 && fullText != lastSpokenText) {
                lastSpokenText = fullText
                speakOut(fullText)
            }
        }

        rootNode.recycle()
    }

    private fun scanSubtitlesAtBottom(
        node: AccessibilityNodeInfo?, 
        thresholdY: Int, 
        resultList: MutableList<String>
    ) {
        if (node == null) return

        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (rect.top >= thresholdY && !node.text.isNullOrBlank()) {
            val content = node.text.toString().trim()
            if (!node.isClickable && content.length > 1) {
                resultList.add(content)
            }
        }

        for (i in 0 until node.childCount) {
            scanSubtitlesAtBottom(node.getChild(i), thresholdY, resultList)
        }
        node.recycle()
    }

    private fun speakOut(text: String) {
        if (!isTtsReady || tts == null) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SUB_STREAM_ID")
        Log.d(TAG, "Đang đọc: $text")
    }

    override fun onInterrupt() {
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
