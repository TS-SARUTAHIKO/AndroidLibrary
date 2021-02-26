package com.xxxsarutahikoxxx.android.androidlibrary.TextToSpeech

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
open class TextSpeech(context : Context, listener : TextToSpeech.OnInitListener) {
    constructor(context : Context) : this(context, TextToSpeech.OnInitListener { })

    // TextToSpeech 設定
    private val utteranceListener : UtteranceProgressListener = object : UtteranceProgressListener(){
        override fun onAudioAvailable(utteranceId: String, audio: ByteArray) {
            this@TextSpeech.onAudioAvailable(utteranceId, audio)
        }
        override fun onBeginSynthesis(utteranceId: String, sampleRateInHz: Int, audioFormat: Int, channelCount: Int) {
            this@TextSpeech.onBeginSynthesis(utteranceId, sampleRateInHz, audioFormat, channelCount)
        }
        override fun onStart(utteranceId: String) {
            this@TextSpeech.onStart(utteranceId)
        }
        override fun onError(utteranceId: String) {

        }
        override fun onError(utteranceId: String, errorCode: Int) {
            this@TextSpeech.onError(utteranceId, errorCode)
        }
        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            this@TextSpeech.onRangeStart(utteranceId, start, end, frame)
        }

        override fun onDone(utteranceId: String) {
            if( sequences.isNotEmpty() && sequences[0].utteranceId == utteranceId ){
                sequences.removeAt(0)

                if( sequences.isNotEmpty() ) {
                    speak(sequences[0])
                }else{
                    onEndOfUtterance(0)
                }
            }

            this@TextSpeech.onDone(utteranceId)
        }
        override fun onStop(utteranceId: String, interrupted: Boolean) {
            this@TextSpeech.onStop(utteranceId, interrupted)

            onEndOfUtterance(1)
        }
    }
    private val TTS = TextToSpeech(context, listener).apply {
        setOnUtteranceProgressListener(utteranceListener)
    }
    fun TTS( func : TextToSpeech.()->(Any?) ) = TTS.func()

    // TTS-Task 関係
    protected val sequences : MutableList<TTSTask> = mutableListOf()
    private var IDcount : Int = 0

    /** TTS のタスクを追加する */
    fun speak(text : CharSequence, locale : Locale? = null, queueMode : Int = TextToSpeech.QUEUE_ADD, params : Bundle = Bundle(), utteranceId : String = "auto:${IDcount++}"){
        when( queueMode ){
            TextToSpeech.QUEUE_FLUSH -> {
                sequences.clear() // TTS.stop() を行わないことで音のブツ切れを回避する

                sequences.add( TTSTask(locale, text, queueMode, params, utteranceId) )
                speak(sequences[0])
            }
            TextToSpeech.QUEUE_ADD -> {
                sequences.add( TTSTask(locale, text, queueMode, params, utteranceId) )

                if( sequences.size == 1 ) speak(sequences[0])
            }
        }
    }
    private fun speak(task : TTSTask){
        TTS {
            if( language != task.locale ) language = task.locale

            speak(task.text, task.queueMode, task.params, task.utteranceId)
        }
    }
    /** TTS のタスクを消去する */
    fun stop(){
        sequences.clear()
        TTS { stop() }
    }


    // コールバック関数
    open var onStart : (utteranceId : String)->(Unit) = {  }
    open var onDone : (utteranceId : String)->(Unit) = {  }
    open var onError : (utteranceId: String, errorCode: Int)->(Unit) = { _, _ -> }
    open var onStop : (utteranceId : String, interrupted: Boolean)->(Unit) = { _, _ -> }
    open var onRangeStart : (utteranceId: String, start: Int, end: Int, frame: Int)->(Unit) = { _, _, _, _ -> }
    open var onBeginSynthesis : (utteranceId: String, sampleRateInHz: Int, audioFormat: Int, channelCount: Int)->(Unit) = { _, _, _, _ -> }
    open var onAudioAvailable : (utteranceId: String, audio: ByteArray)->(Unit) = { _, _ -> }

    /**
     * 発話がキャンセルされたか、発話が終了してキューが空になった場合に呼ばれる
     *
     * code=0 : 正常終了
     *
     * code=1 : stop による終了
     * */
    open var onEndOfUtterance : (code : Int)->(Unit) = {  }


    protected inner class TTSTask(val locale : Locale?, val text : CharSequence, val queueMode : Int, val params : Bundle, val utteranceId : String)
}
