@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)




package com.example.a0725.screens




import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.io.File




/**
 * 全域單例 Cache，避免重複建立導致錯誤。
 * 這樣所有影片播放器都能共享快取，下載一次就不會再下載。
 */
object VideoCacheManager {
    private var simpleCache: SimpleCache? = null




    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheSize: Long = 100 * 1024 * 1024 // 設定快取上限 100MB
            val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
            val databaseProvider = StandaloneDatabaseProvider(context)
            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }
}




@Composable
fun SimpleVideoBox(
    uri: Uri,
    modifier: Modifier = Modifier,
    showController: Boolean = true,
    isPlaying: Boolean = false, // 🔥 新增：外部控制是否播放 (預設 false 避免搶資源)
    onPositionChanged: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current




    // 1. 建立 ExoPlayer (使用 remember 確保生命週期內只建立一次)
    val exoPlayer = remember {
        // 設定 Cache DataSource
        val cache = VideoCacheManager.getCache(context)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))




        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)




        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory) // 套用快取工廠
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 1f
                playWhenReady = isPlaying // 依照傳入參數決定是否自動播
            }
    }




    // 2. 監聽 isPlaying 狀態變化
    // 這樣可以在開 Dialog 時，讓外面的影片暫停，釋放解碼器壓力
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }




    // 3. 切換影片來源 (只有當 URI 真的變換時才執行)
    // 使用 remember(uri) 判斷，如果 URI 字串沒變，就不會重新 setMediaItem
    LaunchedEffect(uri) {
        val currentMedia = exoPlayer.currentMediaItem
        // 簡單判斷：如果現在播的跟傳進來的不一樣，才重新載入
        if (currentMedia?.localConfiguration?.uri != uri) {
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
    }




    // 4. 回報進度 (維持原樣)
    LaunchedEffect(exoPlayer, onPositionChanged) {
        if (onPositionChanged == null) return@LaunchedEffect
        while (true) {
            if (exoPlayer.isPlaying) {
                onPositionChanged(exoPlayer.currentPosition)
            }
            delay(100L) // 降低更新頻率以節省資源
        }
    }




    // 5. 釋放資源
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }




    // 6. UI
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = showController
                // 修正全螢幕切換時的 Layout 問題
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setKeepContentOnPlayerReset(true)
                setBackgroundColor(android.graphics.Color.BLACK) // 避免閃白
            }
        }
    )
}









