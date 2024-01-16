package com.hwinzniej.musichelper.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hwinzniej.musichelper.MainActivity
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.data.model.Music
import com.hwinzniej.musichelper.utils.MyVibrationEffect
import com.hwinzniej.musichelper.utils.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File
import java.io.FileWriter

class ScanPage(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val openDirectoryLauncher: ActivityResultLauncher<Uri?>,
    val db: MusicDatabase,
    componentActivity: ComponentActivity
) : PermissionResultHandler {
    val scanResult = mutableStateListOf<String>()
    val showLoadingProgressBar = mutableStateOf(false)
    val showConflictDialog = mutableStateOf(false)
    var progressPercent = mutableIntStateOf(-1)
    var lastIndex = 0
    var exportResultFile = mutableStateOf(false)
    val musicAllList: ArrayList<Music> = ArrayList()
    var selectedExportFormat = mutableIntStateOf(0)

    /**
     * 功能入口，初始化变量
     */

    fun init() {
        musicAllList.clear()
        lastIndex = 0
        requestPermission()
    }


    /**
     * 请求存储权限
     */

//==================== Android 11+ 使用====================

    @RequiresApi(Build.VERSION_CODES.R)
    private val requestPermissionLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Environment.isExternalStorageManager()) {
            checkFileExist(250L)
        } else {
            Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //Android 11+
            if (Environment.isExternalStorageManager()) {
                checkFileExist()
            } else {
                Toast.makeText(context, R.string.request_permission_toast, Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                requestPermissionLauncher.launch(intent)
            }
        } else { //Android 10-
            if (allPermissionsGranted()) {
                checkFileExist()
            } else {
                Toast.makeText(context, R.string.request_permission_toast, Toast.LENGTH_SHORT)
                    .show()
                ActivityCompat.requestPermissions(
                    context as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

//==================== Android 11+ 使用====================

//==================== Android 10- 使用====================

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    context, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onPermissionResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                checkFileExist(250L)
            } else {
                Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

//==================== Android 10- 使用====================

    /**
     * 检查文件是否已存在
     */
    fun checkFileExist(delay: Long = 0L) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            delay(delay)
            if (exportResultFile.value) {
                if (selectedExportFormat.intValue == 0) {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "${getString(context, R.string.result_file_name)}.db"
                        )
                        if (file.exists()) {
                            showConflictDialog.value = true
                            val fileDb = SQLiteDatabase.openOrCreateDatabase(file, null)
                            val lastChars = fileDb.rawQuery("SELECT COUNT(id) FROM music", null)
                            lastChars.moveToFirst()
                            lastIndex = lastChars.getInt(0)
                            lastChars.close()
                            fileDb.close()
                        } else {
                            db.musicDao().deleteAll()
                            openDirectoryLauncher.launch(null)
                        }
                    }
                } else {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "${getString(context, R.string.result_file_name)}.txt"
                        )
                        if (file.exists()) {
                            showConflictDialog.value = true
                            val lastChars = Tools().readLastNChars(file, 8)
                            lastIndex =
                                lastChars.substring(
                                    lastChars.lastIndexOf("#") + 1,
                                    lastChars.length
                                ).toInt()
                        } else {
                            db.musicDao().deleteAll()
                            openDirectoryLauncher.launch(null)
                        }
                    }
                }
            } else {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val musicCount = db.musicDao().getMusicCount()
                    if (musicCount != 0) {
                        showConflictDialog.value = true
                        lastIndex = musicCount - 1
                    } else
                        openDirectoryLauncher.launch(null)
                }
            }
        }
    }

    fun userChoice(choice: Int) {
        when (choice) {
            1 -> {  //文件冲突，追加
                showConflictDialog.value = false
                lastIndex++
                openDirectoryLauncher.launch(null)
            }

            2 -> {  //文件冲突，覆盖
                showConflictDialog.value = false
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (exportResultFile.value) {
                        if (selectedExportFormat.intValue == 0) {
                            File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                "${getString(context, R.string.result_file_name)}.db"
                            ).delete()
                        } else {
                            File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                "${getString(context, R.string.result_file_name)}.txt"
                            ).delete()
                        }
                    }
                    db.musicDao().deleteAll()
                }
                lastIndex = 0
                openDirectoryLauncher.launch(null)
            }
        }
    }

    /**
     * 获取用户选择需要操作的目录的Uri
     */

    fun handleUri(uri: Uri?) {
        if (uri == null) {
            return
        }

        scanResult.clear()
        val absolutePath: String = Tools().uriToAbsolutePath(uri)
        val directory = File(absolutePath)
        showLoadingProgressBar.value = true
        progressPercent.intValue = 0
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            delay(300L)
            scanDirectory(directory)
        }
    }

    /**
     * 递归扫描所选目录及其子目录
     */
    fun scanDirectory(directory: File, isRootCall: Boolean = true) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    // 如果是目录，递归地遍历
                    scanDirectory(file, false)
                } else {
                    // 如果是文件，进行处理
                    val curFile = File(file.path)
                    try {
                        AudioFileIO.read(curFile)
                    } catch (e: Exception) {
                        continue
                    }
                    handleFile(curFile)
                }
            }
        }
        if (isRootCall) {
            saveAndExport()
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                showLoadingProgressBar.value = false
                Toast.makeText(
                    context, R.string.scan_complete, Toast.LENGTH_SHORT
                ).show()
                MyVibrationEffect(
                    context,
                    (context as MainActivity).enableHaptic.value
                ).done()
            }
        }
    }


    /**
     * 处理扫描到的音乐文件
     */
    fun handleFile(file: File) {
        // 在这里处理文件
        val audioFile: AudioFile
        try {
            audioFile = AudioFileIO.read(file)
        } catch (e: Exception) {
            return
        }
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            scanResult.add(0, file.name)
        }
        val tag = audioFile.tag
        getTag(tag, file.path)

    }

    /**
     * 将扫描到音乐的标签信息写入到文件中
     */
    @Synchronized
    private fun getTag(tag: Tag, filePath: String) {
        val songName = tag.getFirst(FieldKey.TITLE)
        val artistName = tag.getFirst(FieldKey.ARTIST)
        val albumName = tag.getFirst(FieldKey.ALBUM)
        val releaseYear = tag.getFirst(FieldKey.YEAR)
        val trackNumber = tag.getFirst(FieldKey.TRACK)
        val albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST)
        val genre = tag.getFirst(FieldKey.GENRE)

        musicAllList.add(
            Music(
                lastIndex,
                songName,
                artistName,
                albumName,
                filePath,
                releaseYear,
                trackNumber,
                albumArtist,
                genre
            )
        )
        increment()
    }

    fun saveAndExport() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.musicDao().insertAll(*musicAllList.toTypedArray())
            if (exportResultFile.value) {
                if (selectedExportFormat.intValue == 0) {
                    exportToDb()
                } else {
                    exportToTxt()
                }
            }
        }
    }

    fun exportToDb() {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "${getString(context, R.string.result_file_name)}.db"
        )
        val outputDb = SQLiteDatabase.openOrCreateDatabase(file, null)
        outputDb.execSQL("CREATE TABLE IF NOT EXISTS music (id INTEGER PRIMARY KEY, song TEXT, artist TEXT, album TEXT, absolutePath TEXT, releaseYear TEXT, trackNumber TEXT, albumArtist TEXT, genre TEXT)")
        for (music in musicAllList) {
            outputDb.execSQL(
                "INSERT INTO music VALUES (${music.id}, '${
                    music.song.replace("'", "''")
                }', '${
                    music.artist.replace("'", "''")
                }', '${
                    music.album.replace("'", "''")
                }', '${
                    music.absolutePath.replace("'", "''")
                }', '${
                    music.releaseYear.replace("'", "''")
                }', '${
                    music.trackNumber.replace("'", "''")
                }', '${
                    music.albumArtist.replace("'", "''")
                }', '${
                    music.genre.replace("'", "''")
                }')"
            )
        }
        outputDb.close()
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "${getString(context, R.string.result_file_name)}.db-journal"
        ).delete()
    }

    fun exportToTxt() {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "${getString(context, R.string.result_file_name)}.txt"
        )
        val fileWriter = FileWriter(file, true)
        for (music in musicAllList) {
            fileWriter.write(
                "${music.song}#*#${music.artist}#*#${music.album}#*#${music.absolutePath}#*#${music.id}\n"
            )
        }
        fileWriter.close()
    }

    /**
     * 歌曲数量计数器
     */
    @Synchronized
    private fun increment() {
        ++progressPercent.intValue
        ++lastIndex
    }
}