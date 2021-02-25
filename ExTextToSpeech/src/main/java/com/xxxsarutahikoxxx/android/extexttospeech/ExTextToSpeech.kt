package com.xxxsarutahikoxxx.android.extexttospeech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.CallSuper
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

    protected val sequences : MutableList<TTSTask> = mutableListOf()


    private var IDcount : Int = 0
    fun speak(text : CharSequence, locale : Locale? = null, queueMode : Int = TextToSpeech.QUEUE_ADD, params : Bundle = Bundle(), utteranceId : String = "auto:$IDcount"){
        when( queueMode ){
            TextToSpeech.QUEUE_FLUSH -> {
                sequences.clear() // TTS.stop() を行わないことで音のブツ切れを回避する

                speak( TTSTask(locale, text, queueMode, params, utteranceId) )
            }
            TextToSpeech.QUEUE_ADD -> {
                sequences.add( TTSTask(locale, text, queueMode, params, utteranceId) )

                if( sequences.size == 1 ) speakFirst()
            }
        }
    }
    private fun speakFirst(){
        speak(sequences[0])
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
    @CallSuper
    override fun onDone(utteranceId : String) {
        if( sequences.isNotEmpty() && sequences[0].utteranceId == utteranceId ){
            sequences.removeAt(0)
        }

        if( sequences.isNotEmpty() ) {
            speakFirst()
        }else{
            onEndOfUtterance(0)
        }
    }
    override fun onError(utteranceId : String) {}

    @CallSuper
    override fun onStop(utteranceId: String?, interrupted: Boolean) {
        super.onStop(utteranceId, interrupted)
        onEndOfUtterance(1)
    }

    /** 発話がキャンセルされたか、発話が終了してキューが空になった場合に呼ばれる */
    open fun onEndOfUtterance(code : Int){

    }


    protected inner class TTSTask(val locale : Locale?, val text : CharSequence, val queueMode : Int, val params : Bundle, val utteranceId : String)
}
