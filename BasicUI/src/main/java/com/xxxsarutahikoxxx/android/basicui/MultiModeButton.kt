package com.xxxsarutahikoxxx.android.basicui

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatImageButton
import java.io.Serializable

/**
 * 状態とモードの管理機能をもったボタン
 *
 * 状態は Play/Idling の２つ、モードは設定された背景用のリソースによって可変長
 *
 * 状態は [play] / [pause] / [toggleState] のいずれかによって変化する。
 * 状態が変化する前に [prepareBeforePlay] / [prepareBeforePause] によってチェックが行われ、返り値が false ならば状態は変化しない。
 *
 * モードは [nextMode] / [previousMode] / [mode] のいずれかによって変化する。
 *
 * ボタンに表示される画像は [setResource] によって設定したものが用いられる。
 * Play中に停止用のボタン(□)、Pause中に再生用のボタン(▷)を表示したいならば src_play=□, src_pause=▷ と設定する。
 *
 * --- How to ---
 *
 * 初期状態でクリック(setOnClickListener)で play / pause の切り替え。
 * 長押し(setOnLongClickListener)でモードの切り替え([nextMode]) を行うようにリスナーが設定されている。
 *
 * ボタンアクションに連動した処理は(setOnClickListener)ではなく、状態変化を [onPlay] / [onPause] で監視してを行うこと。
 *
 * XMLファイルで src_play, src_pause, src_play1, src_pause1, src_play2, src_pause2 を設定して最大３モードまでを作成する。
 * さらにモードを追加する場合は [addResource] で画像リソースを追加する
 *
 * --- コールバック関数 ---
 *
 * [onPlay] : プレイ状態になった時に呼ばれる
 *
 * [onPause] : 待機状態になった時に呼ばれる
 *
 * [onStateChanged] : 状態が変化した時に呼ばれる
 *
 * [prepareBeforePlay] : プレイ状態に変化する前に呼ばれる。この関数が false を返すと状態は変化しない
 *
 * [prepareBeforePause] : 待機状態に変化する前に呼ばれる。この関数が false を返すと状態は変化しない
 *
 * [onModeChanged] : モードが変化した時に呼ばれる
 *
 * [prepareBeforeMode] : モードが変化する前に呼ばれる。この関数が false を返すと状態は変化しない
 *
 * [onRevalidate] : 状態やモードが変化してリソース設定が行われた時に呼ばれる
 * */
class MultiModeButton(context: Context, attrs: AttributeSet?, defStyle : Int) : AppCompatImageButton(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)


    // State 関係
    private var state : Int = 0
        set(value) {
            if( state == value ) return

            when( value ){
                STATE_IDLING -> {
                    if( prepareBeforePause(this) ){
                        field = value
                        revalidate()
                        onStateChanged(this, mode, value)
                        onPause(this, mode)
                    }
                }
                STATE_PLAYING -> {
                    if( prepareBeforePlay(this) ){
                        field = value
                        revalidate()
                        onStateChanged(this, mode, value)
                        onPlay(this, mode)
                    }
                }
            }
        }
    val isPlaying : Boolean get() = (state == STATE_PLAYING)
    val isIdling : Boolean get() = (state == STATE_IDLING)

    var prepareBeforePlay : (MultiModeButton)->(Boolean) = { true }
    var prepareBeforePause : (MultiModeButton)->(Boolean) = { true }

    fun play(){ state = STATE_PLAYING }
    fun pause(){ state = STATE_IDLING }
    fun toggleState(){
        if( isPlaying ) pause() else play()
    }

    var onStateChanged : (view : MultiModeButton, state : Int, mode : Int)->(Unit) = { _, _, _ -> }
    var onPlay : (view : MultiModeButton, mode : Int)->(Unit) = { _, _ -> }
    var onPause : (view : MultiModeButton, mode : Int)->(Unit) = { _, _ -> }


    // Mode 関係
    var mode : Int = 0
        set(value) {
            if( value !in 0.until(modeCount) || field == value ) return

            if( prepareBeforeMode(this) ){
                field = value

                revalidate()
                onModeChanged(this, value, state)
            }
        }
    val modeCount : Int
        get() = resourcesMap.maxOf { it.key.first } + 1

    fun nextMode(){
        mode = (mode + 1) % modeCount
    }
    fun previousMode(){
        mode = (modeCount + mode - 1) % modeCount
    }

    var prepareBeforeMode : (view : MultiModeButton)->(Boolean) = { true }
    var onModeChanged : (view : MultiModeButton, state : Int, mode : Int)->(Unit) = { _, _, _ -> }


    // Resources 関係
    private val resourcesMap : MutableMap<Pair<Int, Int>, Int?> = mutableMapOf()
    fun setResource(mode : Int, state : Int, id : Int?){
        resourcesMap[ mode to state ] = id
    }
    fun getResource(mode : Int, state : Int) : Int? {
        return resourcesMap[ mode to state ]
    }
    fun addResource(playing : Int?, idling : Int?){
        val mode = modeCount
        setResource(mode, STATE_PLAYING, playing)
        setResource(mode, STATE_IDLING, idling)
    }
    fun getResource(mode : Int) : Pair<Int?, Int?> {
        return getResource(mode, STATE_PLAYING) to getResource(mode, STATE_IDLING)
    }

    // 描画関係
    fun revalidate(){
        getResource(mode, state)?.let { setImageResource(it) }

        onRevalidate(this)
    }
    var onRevalidate : (view : MultiModeButton)->(Unit) = {  }


    init {
        val (mode, state) = context.obtainStyledAttributes(attrs, R.styleable.MultiModeButton, 0, defStyle).run {
            val mode = getInt(R.styleable.MultiModeButton_default_mode, 0)
            val state = getInt(R.styleable.MultiModeButton_default_state, STATE_IDLING)

            getResourceId(R.styleable.MultiModeButton_src_play, -1).apply { if( this != -1 ) setResource(0, STATE_PLAYING, this) }
            getResourceId(R.styleable.MultiModeButton_src_pause, -1).apply { if( this != -1 ) setResource(0, STATE_IDLING, this) }
            getResourceId(R.styleable.MultiModeButton_src_play1, -1).apply { if( this != -1 ) setResource(1, STATE_PLAYING, this) }
            getResourceId(R.styleable.MultiModeButton_src_pause1, -1).apply { if( this != -1 ) setResource(1, STATE_IDLING, this) }
            getResourceId(R.styleable.MultiModeButton_src_play2, -1).apply { if( this != -1 ) setResource(2, STATE_PLAYING, this) }
            getResourceId(R.styleable.MultiModeButton_src_pause2, -1).apply { if( this != -1 ) setResource(2, STATE_IDLING, this) }

            recycle()

            mode to state
        }

        setOnClickListener { toggleState() }
        setOnLongClickListener { nextMode() ; true }

        this.mode = mode
        this.state = state
    }

    // Save & Restore
    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putSerializable("ClassName", this@MultiModeButton::class.java)
            putParcelable("Parent", super.onSaveInstanceState())

            putSerializable("MultiModeButton_Resources", resourcesMap as Serializable)
            putInt("MultiModeButton_Mode", mode)
            putInt("MultiModeButton_State", state)
        }
    }
    override fun onRestoreInstanceState(state: Parcelable?) {
        if( state is Bundle){
            if( state.getSerializable("ClassName") != this::class.java ) throw RuntimeException("Illegal State ${state}")
            super.onRestoreInstanceState( state.getParcelable("Parent") )

            resourcesMap.putAll(
                 (state.getSerializable("MultiModeButton_Resources") as Map<Pair<Int, Int>, Int?>)
            )
            this.mode = state.getInt("MultiModeButton_Mode")
            this.state = state.getInt("MultiModeButton_State")
        }else{
            super.onRestoreInstanceState(state)
        }
    }


    companion object {
        const val STATE_IDLING = 0
        const val STATE_PLAYING = 1
    }
}