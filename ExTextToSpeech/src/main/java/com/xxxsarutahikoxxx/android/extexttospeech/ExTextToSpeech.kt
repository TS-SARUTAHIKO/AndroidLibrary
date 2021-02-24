package com.xxxsarutahikoxxx.android.extexttospeech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*


/**
 * 発話するテキストごとに言語設定を行うための [TextToSpeech] のラッパークラス
 *
 * [speak] / [stop] 意外の独自処理のための関数は持たない。
 * [TextToSpeech] の関数を呼ぶ場合は [TTS] を用いて TTS {  } と表記すること
 * */
open class ExTextToSpeech(context : Context, listener : TextToSpeech.OnInitListener) : UtteranceProgressListener() {
    constructor(context : Context) : this(context, TextToSpeech.OnInitListener { })

    private val TTS = TextToSpeech(context, listener).apply { setOnUtteranceProgressListener(this@ExTextToSpeech) }
    fun TTS( func : TextToSpeech.()->(Any?) ) = TTS.func()

    private val sequences : MutableList<TTSTask> = mutableListOf()

    fun speak(text : CharSequence, locale : Locale? = null, queueMode : Int = TextToSpeech.QUEUE_ADD, params : Bundle = Bundle(), utteranceId : String = "NO-ID"){
        when( queueMode ){
            TextToSpeech.QUEUE_FLUSH -> {
                sequences.clear() // TTS.stop() を行わないことで音のブツ切れを回避する

                speak( TTSTask(locale, text, queueMode, params, utteranceId) )
            }
            TextToSpeech.QUEUE_ADD -> {
                if( TTS.isSpeaking ){
                    sequences.add( TTSTask(locale, text, queueMode, params, utteranceId) )
                }else{
                    speak( TTSTask(locale, text, queueMode, params, utteranceId) )
                }
            }
        }
    }
    private fun speakFirst(){
        if( sequences.isNotEmpty() ) speak(sequences.removeAt(0))
    }
    private fun speak(task : TTSTask){
        TTS {
            language = task.locale
            speak(task.text, task.queueMode, task.params, task.utteranceId)
        }
    }
    fun stop(){
        sequences.clear()
        TTS { stop() }
    }


    override fun onStart(utteranceId : String) {}
    override fun onDone(utteranceId : String) {
        speakFirst()
    }
    override fun onError(utteranceId : String) {}

    private inner class TTSTask(val locale : Locale?, val text : CharSequence, val queueMode : Int, val params : Bundle, val utteranceId : String)
}
