package com.xxxsarutahikoxxx.android.androidlibrary.UI


import android.content.Context
import android.util.AttributeSet
import com.xxxsarutahikoxxx.android.androidlibrary.R
import com.xxxsarutahikoxxx.android.androidlibrary.Recognizer.Recognizer
import com.xxxsarutahikoxxx.android.androidlibrary.toast
import java.util.*

/**
 * 録音用ボタン
 *
 * 連続・単発録音の録音モード（モード変更は長押し）
 *
 * 録音開始・終了のビープ音の消去設定
 *
 * マニュフェストでパーミッション（録音・インターネット）を設定すること
 * */
class RecognizerButton(context : Context, attrs : AttributeSet?, defStyleAttr : Int) : MultiModeButton(context, attrs, defStyleAttr) {
    constructor(context : Context, attrs : AttributeSet?) : this(context, attrs, 0)
    constructor(context : Context) : this(context, null)


    // Button のコールバック固定処理。コールバックを新たに設定してもセッターによりこの関数が内包される形でセットされる。
    private val onButtonPlay : (view: MultiModeButton, mode: Int) -> Unit = { _, _ ->
        keepScreenOn = true
        errorCount = 0

        recognizer.recognize()
    }
    private val onButtonPause : (view: MultiModeButton, mode: Int) -> Unit = { _, _ ->
        keepScreenOn = false

        errorCount = errorLimit
        recognizer.cancel()
        errorCount = 0
    }
    // Recognizer のコールバック固定処理。コールバックを新たに設定してもセッターによりこの関数が内包される形でセットされる。
    private val onRecognizerResult : (list : List<String>) -> Unit = {
        errorCount = 0
        onResult(mode, it)
    }
    private val onRecognizerEnd : (Boolean)->(Unit) = { success ->
        when {
            mode == MODE_SINGLE -> { pause() }
            mode == MODE_CONTINUOUS && success -> { recognizer.recognize() }
            mode == MODE_CONTINUOUS && ! success -> {
                errorCount ++
                if( errorCount > errorLimit ){
                    pause()
                }else{
                    recognizer.recognize()
                }
            }
        }
    }
    //


    // Recognizer 関係
    private val recognizer : Recognizer = object : Recognizer(context){
        override var onResult : (list : List<String>) -> Unit = onRecognizerResult
            set(value) { field = { list -> onRecognizerResult(list) ; value(list) ; } }
        override var onEnd : (Boolean) -> Unit = onRecognizerEnd
            set(value) { field = { success -> onRecognizerEnd(success) ; value(success) ; } }
    }
    fun recognizer( func : Recognizer.()->(Unit) ) = recognizer.func()
    /** 音声認識の開始ビープ音 */
    var preRing : Boolean
        get() = recognizer.preRing
        set(value) { recognizer.preRing = value }
    /** 音声認識の終了ビープ音 */
    var postRing : Boolean
        get() = recognizer.postRing
        set(value) { recognizer.postRing = value }
    /** 音声認識に用いる言語設定 */
    var language : Locale
        get() = recognizer.language
        set(value) { recognizer.language = value }

    /** ボタンが [isPlaying] 状態に変化した時のコールバック */
    override var onPlay: (view: MultiModeButton, mode: Int) -> Unit = onButtonPlay
        set(value) { field = { view, mode -> onButtonPlay(view, mode) ; value(view, mode) ; } }
    /** ボタンが [isIdling] 状態に変化した時のコールバック */
    override var onPause: (view: MultiModeButton, mode: Int) -> Unit = onButtonPause
        set(value) { field = { view, mode -> onButtonPause(view, mode) ; value(view, mode) ; } }


    //
    private var errorCount = 0
    /** 連続認識時の無音状態での認識終了を許容する回数 */
    var errorLimit = 1

    /** 音声認識の認識結果 */
    var onResult : (mode : Int, result : List<String>)->(Unit)= { _, _ ->  }


    init {
        setResource(MODE_CONTINUOUS, STATE_PLAYING, R.drawable.button_round_blue_with, false)
        setResource(MODE_CONTINUOUS, STATE_IDLING, R.drawable.button_round_gray, false)
        setResource(MODE_SINGLE, STATE_PLAYING, R.drawable.button_round_blue, false)
        setResource(MODE_SINGLE, STATE_IDLING, R.drawable.button_round_gray, false)

        if( mode == -1 ) mode = 0
    }

    companion object {
        const val MODE_CONTINUOUS = 0
        const val MODE_SINGLE = 1
    }
}