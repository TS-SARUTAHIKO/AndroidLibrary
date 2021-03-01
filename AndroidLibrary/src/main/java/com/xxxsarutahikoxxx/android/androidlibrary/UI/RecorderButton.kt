package com.xxxsarutahikoxxx.android.androidlibrary.UI

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.xxxsarutahikoxxx.android.androidlibrary.R
import com.xxxsarutahikoxxx.android.androidlibrary.out
import com.xxxsarutahikoxxx.android.androidlibrary.toast
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

/**
 * 音声録音のためのボタン
 *
 * 録音完了時には [onRecord] が呼ばれるので必要な処理はそこで行うこと
 *
 * デフォルトでは [playAudio] により最後に保存された音声の再生を行う
 * */
class RecorderButton(context : Context, attrs : AttributeSet?, defStyle : Int) : MultiModeButton(context, attrs, defStyle) {
    constructor(context : Context, attrs : AttributeSet?) : this(context, attrs, 0)
    constructor(context : Context) : this(context, null)

    private var recorder : MediaRecorder? = null
    private val tempFilePath : File by lazy { context.getFileStreamPath("temp RecordAudio.3gp") }
    private var recordingTime : Long = System.currentTimeMillis()


    init {
        setResource(0, STATE_PLAYING, R.drawable.button_round_blue, false)
        setResource(0, STATE_IDLING, R.drawable.button_round_gray, false)

        mode = 0
    }
    /** 最後に保存された音声ファイルの再生を行う */
    fun playAudio(){
        if( tempFilePath.exists() ){
            MediaPlayer.create(context, Uri.fromFile(tempFilePath)).apply {
                setOnPreparedListener {
                    start()
                }
            }
        }
    }

    val onPlayImpl : (view: MultiModeButton, mode: Int) -> Unit = { _, _ ->
        recordingTime = System.currentTimeMillis()

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(tempFilePath.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            prepare()
            start()
        }
    }
    val onPauseImpl : (view: MultiModeButton, mode: Int) -> Unit = { _, _ ->
        recorder?.apply {
            stop()
            release()
        }
        recorder = null

        onRecord(this, tempFilePath, recordingTime)
    }

    override var onPlay: (view: MultiModeButton, mode: Int) -> Unit = onPlayImpl
        set(value) { field = { view, mode -> onPlayImpl(view, mode) ; value(view, mode) } }
    override var onPause: (view: MultiModeButton, mode: Int) -> Unit = onPauseImpl
        set(value) { field = { view, mode -> onPauseImpl(view, mode) ; value(view, mode) } }


    /** 録音完了時の動作。コールバック用関数。デフォルトでは音声の再生を行う。 */
    var onRecord : (view : View, file : File, time : Long)->(Unit) = { _, _, _ -> playAudio() }
}
