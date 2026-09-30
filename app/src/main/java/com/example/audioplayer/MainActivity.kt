package com.example.audioplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        private const val DEFAULT_DIRECTORY = "DCIM"
        private const val PREFS_NAME = "AudioPlayerPrefs"
        private const val PREF_DIRECTORY = "target_directory"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvNowPlaying: TextView
    private lateinit var btnPlayPause: Button
    private lateinit var nowPlayingBar: android.view.View

    private var currentItems: List<FileItem> = emptyList()
    private var adapter: FileAdapter? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioList: List<FileItem.Audio> = emptyList()
    private var currentIndex: Int = -1
    private var isPlaying: Boolean = false
    private var currentDirectory: File? = null

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "设置目录")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                showDirectorySettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDirectorySettingsDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentDir = prefs.getString(PREF_DIRECTORY, DEFAULT_DIRECTORY)

        val editText = EditText(this).apply {
            setText(currentDir)
            hint = "输入目录名，如：DCIM"
        }

        AlertDialog.Builder(this)
            .setTitle("设置扫描目录")
            .setMessage("输入要扫描的目录名称（位于存储根目录下）")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newDir = editText.text.toString().trim()
                if (newDir.isNotEmpty()) {
                    prefs.edit().putString(PREF_DIRECTORY, newDir).apply()
                    loadFilesFromDirectory()
                    Toast.makeText(this, "已切换到: $newDir", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadFilesFromDirectory()
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
                loadFilesFromDirectory()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                tvEmpty.text = getString(R.string.permission_denied)
                tvEmpty.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun loadFilesFromDirectory() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val targetDir = prefs.getString(PREF_DIRECTORY, DEFAULT_DIRECTORY) ?: DEFAULT_DIRECTORY

        val baseDir = Environment.getExternalStorageDirectory()
        currentDirectory = File(baseDir, targetDir)

        if (currentDirectory?.exists() == true && currentDirectory?.isDirectory == true) {
            loadDirectory(currentDirectory!!)
        } else {
            currentItems = emptyList()
            updateUI()
        }
    }

    private fun loadDirectory(directory: File) {
        val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "amr")

        val items = mutableListOf<FileItem>()

        // 添加返回上级目录选项
        if (directory.parentFile != null && directory.absolutePath != Environment.getExternalStorageDirectory().absolutePath) {
            items.add(FileItem.Folder("..", directory.parentFile!!.absolutePath))
        }

        // 获取并排序文件夹和文件
        val files = directory.listFiles() ?: emptyArray()
        val folders = files.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
        val audioFiles = files.filter { it.isFile && it.extension.lowercase() in audioExtensions }.sortedBy { it.name.lowercase() }

        // 先添加文件夹
        folders.forEach { folder ->
            items.add(FileItem.Folder(folder.name, folder.absolutePath))
        }

        // 再添加音频文件
        audioFiles.forEach { file ->
            items.add(FileItem.Audio(file.name, file.absolutePath))
        }

        currentItems = items
        updateUI()
    }

    private fun updateUI() {
        if (currentItems.isEmpty()) {
            tvEmpty.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        } else {
            tvEmpty.visibility = android.view.View.GONE
            recyclerView.visibility = android.view.View.VISIBLE
            adapter = FileAdapter(
                currentItems,
                onFolderClick = { folder ->
                    val dir = File(folder.path)
                    if (dir.exists() && dir.isDirectory) {
                        loadDirectory(dir)
                    }
                },
                onAudioClick = { audio ->
                    playAudio(audio)
                }
            )
            recyclerView.adapter = adapter
        }
    }

    private fun playAudio(audio: FileItem.Audio) {
        // 收集当前目录下的所有音频文件
        currentAudioList = currentItems.filterIsInstance<FileItem.Audio>()
        val index = currentAudioList.indexOfFirst { it.path == audio.path }
        if (index < 0) return

        releasePlayer()
        currentIndex = index
        isPlaying = true

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audio.path)
                setOnCompletionListener {
                    playNext()
                }
                setOnErrorListener { _, _, _ ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "播放出错: ${audio.name}", Toast.LENGTH_SHORT).show()
                        playNext()
                    }
                    true
                }
                prepare()
                start()
            }

            adapter?.setCurrentPlaying(audio.path)
            updateNowPlayingUI()
        } catch (e: Exception) {
            Toast.makeText(this, "无法播放: ${e.message}", Toast.LENGTH_SHORT).show()
            isPlaying = false
            updateNowPlayingUI()
        }
    }

    private fun playNext() {
        if (currentIndex < currentAudioList.size - 1) {
            playAudio(currentAudioList[currentIndex + 1])
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
        if (currentIndex >= 0 && currentIndex < currentAudioList.size) {
            mediaPlayer?.start()
            isPlaying = true
            updateNowPlayingUI()
        } else if (currentAudioList.isNotEmpty()) {
            playAudio(currentAudioList[0])
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
        if (currentIndex >= 0 && currentIndex < currentAudioList.size) {
            nowPlayingBar.visibility = android.view.View.VISIBLE
            tvNowPlaying.text = "${getString(R.string.now_playing)}: ${currentAudioList[currentIndex].name}"
            btnPlayPause.text = if (isPlaying) getString(R.string.pause) else getString(R.string.play)
        } else {
            nowPlayingBar.visibility = android.view.View.GONE
        }
    }

    override fun onBackPressed() {
        // 如果当前不在根目录，返回上级目录
        val baseDir = Environment.getExternalStorageDirectory()
        if (currentDirectory != null && currentDirectory?.absolutePath != baseDir.absolutePath) {
            currentDirectory?.parentFile?.let { parent ->
                loadDirectory(parent)
                return
            }
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
