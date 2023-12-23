package com.hwinzniej.musichelper

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.room.Room
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hwinzniej.musichelper.data.database.MusicDatabase
import com.hwinzniej.musichelper.pages.ScanPage
import com.hwinzniej.musichelper.pages.ScanPageUi
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.darkSaltColors
import com.moriafly.salt.ui.lightSaltColors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var openDirectoryLauncher: ActivityResultLauncher<Uri?>
    private lateinit var scanPage: ScanPage
    lateinit var db: MusicDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                scanPage.handleUri(uri)
            }

        db = Room.databaseBuilder(
            applicationContext, MusicDatabase::class.java, "music"
        ).build()

        scanPage = ScanPage(this, this, openDirectoryLauncher, db)


        setContent {
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val colors = if (isSystemInDarkTheme) {
                darkSaltColors()
            } else {
                lightSaltColors()
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
//            scanResult.value = getString(R.string.scan_result_hint)
            CompositionLocalProvider {
                SaltTheme(
                    colors = colors
                ) {
                    TransparentSystemBars()
                    Pages(this, scanPage)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        scanPage.onPermissionResult(requestCode, permissions, grantResults)
    }
}

/**
 * UI
 */

@OptIn(ExperimentalFoundationApi::class, UnstableSaltApi::class)
@Composable
private fun Pages(mainActivity: MainActivity, scanPage: ScanPage) {
    val context = LocalContext.current
    val pages = listOf("0", "1", "2", "3")
    val pageState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()

    ) {
        HorizontalPager(
            state = pageState, modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp)
        ) { page ->
            when (page) {
                0 -> {
                    ScanPageUi(
                        scanPage,
                        scanPage.scanResult,
                        scanPage.showLoadingProgressBar,
                        scanPage.progressPercent,
                        scanPage.showConflictDialog,
                        scanPage.conflictDialogResult
                    )
                }

                1 -> {
                    ConvertPageUi()
                }

                2 -> {
                    ProcessPageUi()
                }

                3 -> {
                    AboutPageUi()
                }
            }

        }

        BottomBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomBarItem(
                state = pageState.currentPage == 0,
                onClick = {
                    coroutineScope.launch { pageState.animateScrollToPage(0) }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = context.getString(R.string.scan_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 1,
                onClick = {
//                    scanPage.getMusicList()
                    coroutineScope.launch { pageState.animateScrollToPage(1) }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = context.getString(R.string.convert_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 2,
                onClick = {
                    coroutineScope.launch { pageState.animateScrollToPage(2) }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = context.getString(R.string.process_function_name)
            )
            BottomBarItem(
                state = pageState.currentPage == 3,
                onClick = {
                    coroutineScope.launch { pageState.animateScrollToPage(3) }
                },
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                text = context.getString(R.string.about_function_name)
            )
        }
    }
}


@Composable
private fun ConvertPageUi() {
    Text(text = "测试1")
}

@Composable
private fun ProcessPageUi() {
    Text(text = "测试2")
}

@Composable
private fun AboutPageUi() {
    Text(text = "测试3")
}

@Composable
fun TransparentSystemBars() {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isSystemInDarkTheme()
    val statusBarColor = SaltTheme.colors.background
    val navigationBarColor = SaltTheme.colors.subBackground
    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = useDarkIcons,
        )

        systemUiController.setNavigationBarColor(
            color = navigationBarColor,
            darkIcons = useDarkIcons,
            navigationBarContrastEnforced = false
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    MusicHelperTheme {
//        MainUI(MainActivity())
//    }
//}