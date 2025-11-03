package com.example.audioguideai.domain

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR
import android.util.Log
import java.util.*

interface SpeechEngine {
    fun speak(text: String)
    fun stop()
    fun setSpeed(multiplier: Float)
    fun setPitch(pitch: Float)
    fun isSpeaking(): Boolean
    val isLanguageSupported: Boolean
}

class AndroidTtsEngine(context: Context) : SpeechEngine {
    private val ctx = context
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var languageSupported = false
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ru", "RU"))
                languageSupported = result != TextToSpeech.LANG_MISSING_DATA && 
                                   result != TextToSpeech.LANG_NOT_SUPPORTED
                
                if (!languageSupported) {
                    Log.e("AndroidTtsEngine", "Russian language not supported")
                    // Можно показать диалог с инструкцией установки языка
                } else {
                    Log.d("AndroidTtsEngine", "TTS initialized with Russian language")
                }
                isInitialized = true
            } else {
                Log.e("AndroidTtsEngine", "TTS initialization failed")
                isInitialized = true
            }
        }
    }
    
    override fun speak(text: String) {
        if (!isInitialized || !languageSupported) {
            Log.w("AndroidTtsEngine", "TTS not ready or language not supported")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utter_${System.currentTimeMillis()}")
    }
    
    override fun stop() {
        tts?.stop()
    }
    
    override fun setSpeed(multiplier: Float) {
        tts?.setSpeechRate(multiplier)
    }
    
    override fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }
    
    override fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }
    
    override val isLanguageSupported: Boolean
        get() = languageSupported
    
    fun showLanguageInstallDialog() {
        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        ctx.startActivity(intent)
    }
    
    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
