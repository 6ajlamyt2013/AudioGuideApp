package com.example.audioguideai.domain

import android.content.Context

interface SpeechEngine {
    fun speak(text: String)
    fun stop()
    fun setSpeed(multiplier: Float)
}

class AndroidTtsEngine(context: Context) : SpeechEngine {
    private val tts = android.speech.tts.TextToSpeech(context) {}
    override fun speak(text: String) { tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "utter") }
    override fun stop() { tts.stop() }
    override fun setSpeed(multiplier: Float) { tts.setSpeechRate(multiplier) }
}
