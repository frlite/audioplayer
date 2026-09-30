package com.example.audioplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TARGET_DIRECTORY = "DCIM"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvNowPlaying: TextView
    private lateinit var btnPlayPause: Button
    private lateinit var nowPlayingBar: android.view.View

    private var audioFiles: List<AudioFile> = emptyList()
    private var adapter: AudioFileAdapter? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentIndex: Int = -1
    private var isPlaying: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissionAndLoad()
    }

    private fun initViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvNowPlaying = findViewById(R.id.tvNowPlaying)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        nowPlayingBar = findViewById(R.id.nowPlayingBar)

        recyclerView.layoutManager = LinearLayoutManager(this)

        btnPlayPause.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                resumeAudio()
            }
        }
    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadAudioFiles()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAudioFiles()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                tvEmpty.text = getString(R.string.permission_denied)
                tvEmpty.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun loadAudioFiles() {
        val dcimDir = File(Environment.getExternalStorageDirectory(), TARGET_DIRECTORY)
        val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "amr")

        if (dcimDir.exists() && dcimDir.isDirectory) {
            val files = dcimDir.listFiles { file ->
                file.isFile && file.extension.lowercase() in audioExtensions
            }?.map { AudioFile(it.name, it.absolutePath) }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()

            audioFiles = files
        } else {
            audioFiles = emptyList()
        }

        if (audioFiles.isEmpty()) {
            tvEmpty.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        } else {
            tvEmpty.visibility = android.view.View.GONE
            recyclerView.visibility = android.view.View.VISIBLE
            adapter = AudioFileAdapter(audioFiles) { position ->
                playAudio(position)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun playAudio(index: Int) {
        if (index < 0 || index >= audioFiles.size) return

        releasePlayer()
        currentIndex = index
        isPlaying = true

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFiles[index].path)
                setOnCompletionListener {
                    playNext()
                }
                setOnErrorListener { _, _, _ ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "播放出错: ${audioFiles[index].name}", Toast.LENGTH_SHORT).show()
                        playNext()
                    }
                    true
                }
                prepare()
                start()
            }

            adapter?.setCurrentPlaying(index)
            updateNowPlayingUI()
        } catch (e: Exception) {
            Toast.makeText(this, "无法播放: ${e.message}", Toast.LENGTH_SHORT).show()
            isPlaying = false
            updateNowPlayingUI()
        }
    }

    private fun playNext() {
        if (currentIndex < audioFiles.size - 1) {
            playAudio(currentIndex + 1)
        } else {
            isPlaying = false
            releasePlayer()
            updateNowPlayingUI()
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        updateNowPlayingUI()
    }

    private fun resumeAudio() {
        if (currentIndex >= 0 && currentIndex < audioFiles.size) {
            mediaPlayer?.start()
            isPlaying = true
            updateNowPlayingUI()
        } else if (audioFiles.isNotEmpty()) {
            playAudio(0)
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }

    private fun updateNowPlayingUI() {
        if (currentIndex >= 0 && currentIndex < audioFiles.size) {
            nowPlayingBar.visibility = android.view.View.VISIBLE
            tvNowPlaying.text = "${getString(R.string.now_playing)}: ${audioFiles[currentIndex].name}"
            btnPlayPause.text = if (isPlaying) getString(R.string.pause) else getString(R.string.play)
        } else {
            nowPlayingBar.visibility = android.view.View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
