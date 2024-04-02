package memory.utils;
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.media.MediaPlayer
import com.example.memorygame.R


class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var playbackPosition: Int = 0

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = MusicBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.the_deli_flowers)
        mediaPlayer?.isLooping = true
        mediaPlayer?.seekTo(playbackPosition)
        mediaPlayer?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaPlayer?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        mediaPlayer?.setVolume(leftVolume, rightVolume)
    }

    fun pause () {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                playbackPosition = it.currentPosition
            }
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    // Inside MusicService
    fun resumePlayback() {
        mediaPlayer?.start()
    }
}
