package com.xxxsarutahikoxxx.android.basicui

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatImageButton
import java.io.Serializable

class MultiModeButton(context: Context, attrs: AttributeSet?, defStyle : Int) : AppCompatImageButton(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)


    // State 関係
    private var state : Int = 0
        set(value) {
            field = value

            when( value ){
                STATE_IDLING -> {
                    revalidate()
                    onStateChanged(this, mode, value)
                    onPause(this, mode)
                }
                STATE_PLAYING -> {
                    revalidate()
                    onStateChanged(this, mode, value)
                    onPlay(this, mode)
                }
            }
        }
    val isPlaying : Boolean get() = (state == STATE_PLAYING)
    val isIdling : Boolean get() = (state == STATE_IDLING)

    fun play(){ state = STATE_PLAYING }
    fun pause(){ state = STATE_IDLING }
    fun toggleState(){
        if( isPlaying ) pause() else play()
    }

    var onStateChanged : (view : MultiModeButton, state : Int, mode : Int)->(Unit) = { _, _, _ -> }
    var onPlay : (view : MultiModeButton, mode : Int)->(Unit) = { _, _ -> }
    var onPause : (view : MultiModeButton, mode : Int)->(Unit) = { _, _ -> }


    // Mode 関係
    private var mode : Int = 0
        set(value) {
            if( value !in 0.until(modeCount) || field == value ) return

            field = value

            revalidate()
            onModeChanged(this, value, state)
        }
    val modeCount : Int
        get() = resourcesMap.maxOf { it.key.first } + 1

    fun nextMode(){
        if( isIdling ) mode = (mode + 1) % modeCount
    }
    fun previousMode(){
        if( isIdling ) mode = (modeCount + mode - 1) % modeCount
    }

    var onModeChanged : (view : MultiModeButton, state : Int, mode : Int)->(Unit) = { _, _, _ -> }


    // Resources 関係
    private val resourcesMap : MutableMap<Pair<Int, Int>, Int?> = mutableMapOf()
    fun setResource(mode : Int, state : Int, id : Int?){
        resourcesMap[ mode to state ] = id
    }
    fun getResource(mode : Int, state : Int) : Int? {
        return resourcesMap[ mode to state ]
    }

    // 描画関係
    protected fun revalidate(){
        getResource(mode, state)?.let { setImageResource(it) }

        onRevalidate(this)
    }
    var onRevalidate : (view : MultiModeButton)->(Unit) = {  }


    init {
        val (mode, state) = context.obtainStyledAttributes(attrs, R.styleable.MultiModeButton, 0, defStyle).run {
            val mode = getInt(R.styleable.MultiModeButton_default_mode, 0)
            val state = getInt(R.styleable.MultiModeButton_default_state, 0)

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
        setOnLongClickListener { nextMode() ; false }

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