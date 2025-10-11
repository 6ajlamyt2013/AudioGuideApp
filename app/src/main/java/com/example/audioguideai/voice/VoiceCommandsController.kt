package com.example.audioguideai.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableSharedFlow

class VoiceCommandsController(private val context: Context) : RecognitionListener {
    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .build()

    private var sr: SpeechRecognizer? = null
    val events = MutableSharedFlow<Command>(extraBufferCapacity = 1)

    enum class Command { STOP, NEXT }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        if (sr == null) sr = SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(this@VoiceCommandsController) }
        requestAudioFocus()
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        sr?.startListening(i)
    }

    fun stop() {
        sr?.stopListening(); sr?.destroy(); sr = null; abandonAudioFocus()
    }

    private fun requestAudioFocus() { am.requestAudioFocus(focusRequest) }
    private fun abandonAudioFocus() { am.abandonAudioFocusRequest(focusRequest) }

    override fun onResults(results: Bundle) { handle(results) }
    override fun onPartialResults(partialResults: Bundle) { handle(partialResults) }
    private fun handle(b: Bundle) {
        val list = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        val txt = list.joinToString(" ").lowercase()
        if (txt.contains(context.getString(com.example.audioguideai.R.string.voice_stop))) events.tryEmit(Command.STOP)
        if (txt.contains(context.getString(com.example.audioguideai.R.string.voice_next))) events.tryEmit(Command.NEXT)
    }

    override fun onReadyForSpeech(p0: Bundle?) {}
    override fun onError(error: Int) {}
    override fun onBeginningOfSpeech() {}
    override fun onEndOfSpeech() {}
    override fun onBufferReceived(p0: ByteArray?) {}
    override fun onEvent(p0: Int, p1: Bundle?) {}
    override fun onRmsChanged(p0: Float) {}
}
