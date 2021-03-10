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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/**
 * 音声認識のための [SpeechRecognizer] のラッパークラス
 *
 * マニフェストファイルに権限を付加すること(RECORD_AUDIO・INTERNET)
 *
 * 開始音・終了音のキャンセル機能を追加
 *
 * 連続認識機能を追加
 *
 * イベントリスナーをコールバック関数形式で追加
 * */
open class Recognizer(
    val context : Context,

    /** 音声認識時の開始の通知音を鳴らすかどうか */
    var startRing : Boolean = true,
    /** 音声認識時の終了の通知音を鳴らすかどうか */
    var endRing : Boolean = false,
    /** 連続認識時の無音での認識終了の許容回数 */
    var errorLimit : Int = -1,

    /** 読み取りを行う言語 */
    var locale : Locale = Locale.getDefault(),
    /** オフラインで音声認識を行うかどうか */
    var offline : Boolean = false,
    /** 途中結果を取得するかどうか */
    var partialResults : Boolean = false,
    /** 取得する結果の個数 */
    var maxResults : Int = 4
){
    private var onReadyRorRecognition = requestPermission()
    private var recognizer : SpeechRecognizer? = null

    /** 作動中の [recognizer] が存在するかどうか */
    val isActive get() = (recognizer != null)

    /** 音声認識(発音チェック)用のパーミッションを要求する */
    private fun requestPermission() : Boolean {
        val list = if( offline ) listOf(Manifest.permission.RECORD_AUDIO) else listOf(Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO)

        return (context as Activity).getPermissions( *list.toTypedArray() )
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
                    Thread.sleep(DELAY_ON_START_RING)
                    reloadRingVolume()
                }
            }

            override fun onBeginningOfSpeech() {
                this@Recognizer.onBeginningOfSpeech()
            }
            override fun onEndOfSpeech() {
                this@Recognizer.onEndOfSpeech()
            }
            override fun onPartialResults(results: Bundle?) {
                val result : ArrayList<String> = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()

                if( result.isNotEmpty() ) onPartialResults(result.toList())
            }
            override fun onResults(results: Bundle?) {
                val mResult : ArrayList<String> = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                onResult(mResult.toList())

                onEndOfRecognition(true)
            }

            override fun onError(error: Int) {
                onEndOfRecognition(false)
            }
        })
    }

    /**
     * 音声認識を中断する
     * */
    fun cancel(){
        if( isActive ){
            errorCount = errorLimit // 連続再生を抑制する
            recognizer?.cancel()
        }
    }
    /**
     * 終了処理、リソースの解放とミュート解除を行う
     * */
    fun release(){
        recognizer?.destroy()
        unmuteRing()
    }

    /** 音声認識を開始する */
    fun recognize(){
        if( isActive ) return

        if( ! onReadyRorRecognition ){
            onReadyRorRecognition = requestPermission()
        }else{
            if( onPrepare() ){
                startRecognize()
                onStart(true)
            }
        }
    }
    /** 音声認識を行う */
    private fun startRecognize(){
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply { setup() }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "${locale.language}");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialResults)
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, offline)

        reloadRingVolume(RecState.Preparing)

        recognizer?.startListening(intent)
    }

    var errorCount = 0

    /**
     * 音声認識の終了時に呼び出される
     * */
    private fun onEndOfRecognition(success : Boolean){
        recState = RecState.Idling
        recognizer?.destroy()
        recognizer = null

        thread {
            Thread.sleep(DELAY_ON_END_RING)
            reloadRingVolume()
        }

        if( success ) errorCount = 0 else if( errorCount != Int.MAX_VALUE ) errorCount ++
        if( errorCount <= errorLimit && (errorCount != Int.MAX_VALUE) ){
            startRecognize()
            onStart(false)
        }else{
            onEnd(success)
        }
    }


    // イベントリスナー関数
    /** 音声入力が始まった時に呼び出される関数。コールバック用 */
    open var onBeginningOfSpeech : ()->(Unit) = {  }
    /** 音声入力が終わった時に呼び出される関数。コールバック用 */
    open var onEndOfSpeech : ()->(Unit) = {  }
    /** 音声認識の途中結果を取得時に呼び出される関数。コールバック用。 */
    open var onPartialResults : (candidates : List<String>)->(Unit) = {  }
    /** 音声認識が成功した時に呼び出される関数。コールバック用。 */
    open var onResult : (candidates : List<String>)->(Unit) = {  }

    /** 音声認識の開始前に呼び出される。この関数が false を返すと音声認識は開始されない。連続認識でも一度のみ。 */
    open var onPrepare : ()->(Boolean) = { true }
    /** 音声認識の開始時に呼び出される関数。beginning = true ならば（連続認識の）最初の一回目。コールバック用。 */
    open var onStart : (beginning : Boolean)->(Unit) = {  }
    /** 音声認識が終了した時に呼び出される関数。連続認識でも一度のみ。（エラー終了 or キャンセル success == false・正常終了 success == true）。コールバック用。 */
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
            RecState.Preparing -> { startRing }
            RecState.Recording -> { endRing }
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
        var DELAY_ON_START_RING : Long = 500
        /** 終了時のアラームをミュートするための調整時間 */
        var DELAY_ON_END_RING : Long = 1000
    }
}