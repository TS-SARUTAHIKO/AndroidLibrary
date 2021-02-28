package com.xxxsarutahikoxxx.android.androidlibrary.UI


import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import com.xxxsarutahikoxxx.android.androidlibrary.R
import com.xxxsarutahikoxxx.android.androidlibrary.Recognizer.Recognizer
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
class RecognizerButton(context : Context, attrs : AttributeSet?, defStyle : Int) : MultiModeButton(context, attrs, defStyle) {
    constructor(context : Context, attrs : AttributeSet?) : this(context, attrs, 0)
    constructor(context : Context) : this(context, null)


    // Button のコールバック固定処理。コールバックを新たに設定してもセッターによりこの関数が内包される形でセットされる。
    private val onButtonPlay : (view: MultiModeButton, mode: Int) -> Unit = { _, _ ->
        keepScreenOn = true
        recognizer.recognize()
    }
    private val onButtonPause : (view: MultiModeButton, mode: Int) -> Unit = { _, _ ->
        keepScreenOn = false
        recognizer.cancel()
    }
    private val onButtonModeChanged : (view: MultiModeButton, mode: Int) -> Unit = { _, mode ->
        when( mode ){
            MODE_CONTINUOUS -> recognizer { errorLimit = continuousErrorLimit }
            MODE_SINGLE     -> recognizer { errorLimit = -1 }
        }
    }

    /** ボタンが [isPlaying] 状態に変化した時のコールバック */
    override var onPlay: (view: MultiModeButton, mode: Int) -> Unit = onButtonPlay
        set(value) { field = { view, mode -> onButtonPlay(view, mode) ; value(view, mode) ; } }
    /** ボタンが [isIdling] 状態に変化した時のコールバック */
    override var onPause: (view: MultiModeButton, mode: Int) -> Unit = onButtonPause
        set(value) { field = { view, mode -> onButtonPause(view, mode) ; value(view, mode) ; } }
    override var onModeChanged: (view: MultiModeButton, mode: Int) -> Unit = onButtonModeChanged
        set(value) { field = { view, mode -> onButtonModeChanged(view, mode) ; value(view, mode) ; } }


    // Recognizer のコールバック固定処理。コールバックを新たに設定してもセッターによりこの関数が内包される形でセットされる。
    private val onRecognizerResult : (candidates : List<String>) -> Unit = {
        onResult(mode, it)
    }
    // Recognizer のコールバック固定処理。コールバックを新たに設定してもセッターによりこの関数が内包される形でセットされる。
    private val onRecognizerEnd : (success : Boolean) -> Unit = {
        pause()
    }

    // Recognizer 関係
    private val recognizer : Recognizer = object : Recognizer(context){
        override var onResult : (candidates : List<String>)->(Unit) = onRecognizerResult
            set(value) { field = { candidates -> onRecognizerResult(candidates) ; value(candidates) ; } }
        override var onEnd : (Boolean) -> Unit = onRecognizerEnd
            set(value) { field = { success -> onRecognizerEnd(success) ; value(success) ; } }
    }
    fun recognizer( func : Recognizer.()->(Unit) ) = recognizer.func()


    /**  */
    var continuousErrorLimit = 1
    /** 音声認識の認識結果 */
    var onResult : (mode : Int, result : List<String>)->(Unit)= { _, _ ->  }


    init {
        setResource(MODE_CONTINUOUS, STATE_PLAYING, R.drawable.button_round_blue_with, false)
        setResource(MODE_CONTINUOUS, STATE_IDLING, R.drawable.button_round_gray, false)
        setResource(MODE_SINGLE, STATE_PLAYING, R.drawable.button_round_blue, false)
        setResource(MODE_SINGLE, STATE_IDLING, R.drawable.button_round_gray, false)

        context.obtainStyledAttributes(attrs, R.styleable.MultiModeButton, 0, defStyle).run {
            mode = getInt(R.styleable.MultiModeButton_default_mode, 0)

            recycle()
        }
    }
    override fun onSaveInstanceState(): Parcelable {
        pause()
        return super.onSaveInstanceState()
    }


    companion object {
        const val MODE_CONTINUOUS = 0
        const val MODE_SINGLE = 1
    }
}