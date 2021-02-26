package com.xxxsarutahikoxxx.android.androidlibrary.Recognizer


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.xxxsarutahikoxxx.android.androidlibrary.getPermissions
import com.xxxsarutahikoxxx.android.androidlibrary.out
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * 音声認識のための [SpeechRecognizer] のラッパークラス
 *
 * 開始音・終了音のキャンセル機能を追加
 *
 * イベントリスナーをコールバック関数形式で追加
 * */
open class Recognizer(
    val context : Context,

    /** 音声認識時の開始の通知音を鳴らすかどうか */
    var preRing : Boolean = true,
    /** 音声認識時の終了の通知音を鳴らすかどうか */
    var postRing : Boolean = false,
    /** 読み取りを行う言語 */
    var language : Locale = Locale.ENGLISH
){
    private var onReadyRorRecognition = requestPermission()
    private var recognizer : SpeechRecognizer? = null

    /** 作動中の [recognizer] が存在するかどうか */
    val isActive get() = (recognizer != null)

    /** 音声認識(発音チェック)用のパーミッションを要求する */
    private fun requestPermission() : Boolean {
        return (context as Activity).getPermissions(Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO)
    }
    /** [SpeechRecognizer]の設定を行う */
    private fun SpeechRecognizer.setup(){
        setRecognitionListener(object : RecognitionListener {
            override fun onBufferReceived(buffer: ByteArray) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}

            override fun onReadyForSpeech(params: Bundle?) {
                recState = RecState.Recording

                thread {
                    Thread.sleep(DELAY_ON_PRERINF)
                    reloadRingVolume()
                }
            }
            override fun onBeginningOfSpeech() {
                this@Recognizer.onBeginningOfSpeech()
            }
            override fun onEndOfSpeech() {
                this@Recognizer.onEndOfSpeech()
            }
            override fun onError(error: Int) {
                onEndOfRecognition()
                onEnd(false)
            }
            override fun onPartialResults(results: Bundle?) {
                val result : ArrayList<String> = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()

                if( result.isNotEmpty() ){
                    onPartialResults(result.toList())
                }
            }

            override fun onResults(results: Bundle?) {
                val mResult : ArrayList<String> = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()

                onEndOfRecognition()

                onResult(mResult.toList())
                onEnd(true)
            }
        })
    }
    /**
     * 終了処理、リソースの解放とミュート解除を行う
     * */
    fun release(){
        recognizer?.destroy()
        unmuteRing()
    }
    /**
     * 音声認識を中断する
     * */
    fun cancel(){
        recognizer?.cancel()

        onEndOfRecognition()
        onEnd(false)
    }

    /** 音声認識を開始する */
    fun recognize(){
        if( ! onReadyRorRecognition ){
            onReadyRorRecognition = requestPermission()
        }else{
            onPrepare()

            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply { setup() }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "$language");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 4)
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // 音声認識スタート
            onStart()

            reloadRingVolume(RecState.Preparing)

            recognizer?.startListening(intent)
        }
    }
    /**
     * 音声認識の終了時に呼び出される
     * */
    private fun onEndOfRecognition(){
        recState = RecState.Idling
        recognizer?.destroy()
        recognizer = null

        thread {
            Thread.sleep(DELAY_ON_POSTRINF)
            reloadRingVolume()
        }
    }


    // イベントリスナー関数
    /** 音声認識の開始前に呼び出される。コールバック用。 */
    open var onPrepare : ()->(Unit) = {  }
    /** 音声認識の開始時に呼び出される関数。コールバック用。 */
    open var onStart : ()->(Unit) = {  }
    /** 音声入力が始まった時に呼び出される関数。コールバック用 */
    open var onBeginningOfSpeech : ()->(Unit) = {  }
    /** 音声入力が終わった時に呼び出される関数。コールバック用 */
    open var onEndOfSpeech : ()->(Unit) = {  }
    /** 音声認識の途中結果を取得時に呼び出される関数。コールバック用。 */
    open var onPartialResults : (candidates : List<String>)->(Unit) = {  }
    /** 音声認識が成功した時に呼び出される関数。コールバック用。 */
    open var onResult : (candidates : List<String>)->(Unit) = {  }
    /** 音声認識が終了した時に呼び出される関数（エラー終了 or キャンセル success == false・正常終了 success == true）。コールバック用。 */
    open var onEnd : (success : Boolean)->(Unit) = {  }
    /** 状態の切り替え時に呼び出される関数。コールバック用 */
    open var onStateChanged : (pre : RecState, post : RecState)->(Unit) = { _, _ -> }


    // ミュート制御関係

    /** Recognizer の状態を表すクラス */
    enum class RecState {
        Idling, Preparing, Recording
    }
    /** 現在の Recognizer の状態 */
    private var recState : RecState = RecState.Idling
        set(value) {
            val pre = field
            field = value

            onStateChanged(pre, value)
        }

    private val audio = ContextCompat.getSystemService(context, AudioManager::class.java)
    /** ミュート前の音量 */
    private var ringVolume : Int = -1

    /** ミュート状態にする */
    fun muteRing(){
        audio?.let {
            val volume = it.getStreamVolume(RECOGNIZER_RING_STREAM)

            if( volume != 0 ){
                ringVolume = volume
                it.setStreamVolume(RECOGNIZER_RING_STREAM, 0, 0)
            }
        }
    }
    /** ミュート状態を解除する */
    fun unmuteRing(){
        if( ringVolume != -1 ) {
            audio?.let {
                it.setStreamVolume(RECOGNIZER_RING_STREAM, ringVolume, 0)
                ringVolume = -1
            }
        }
    }

    /**
     * 現在の状態に合わせてミュートの On/Off を切り替える
     *
     * [state] が指定されている場合は状態を変化させてから実行する
     * */
    private fun reloadRingVolume(state : RecState? = null){
        if( state != null ){
            recState = state
        }

        val ring = when( recState ){
            RecState.Idling -> { true }
            RecState.Preparing -> { preRing }
            RecState.Recording -> { postRing }
        }
        if( ring ){
            unmuteRing()
        }else{
            muteRing()
        }
    }

    companion object {
        /** 音声認識の開始・終了時の通知音が鳴るストリーム */
        var RECOGNIZER_RING_STREAM = AudioManager.STREAM_RING

        /** 開始時のアラームをミュートするための調整時間 */
        var DELAY_ON_PRERINF : Long = 500
        /** 終了時のアラームをミュートするための調整時間 */
        var DELAY_ON_POSTRINF : Long = 1000
    }
}