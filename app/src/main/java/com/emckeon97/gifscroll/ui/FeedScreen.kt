package com.emckeon97.gifscroll.ui

import android.os.Build
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.emckeon97.gifscroll.data.AppContainer
import com.emckeon97.gifscroll.data.KlipyService
import com.emckeon97.gifscroll.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** ImageLoader with animated-GIF support. */
@Composable
fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}

/** The meme feed: one full-screen swipeable meme/GIF/video per page. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(container: AppContainer, modifier: Modifier = Modifier) {
    var items by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val uid = container.authManager.userId.collectAsState().value ?: "local"
    val signedIn by container.authManager.isSignedIn.collectAsState()

    LaunchedEffect(reloadKey, uid, signedIn) {
        loading = true
        error = null
        container.repostService.refresh(uid, signedIn)
        container.likeManager.refresh(uid, signedIn)
        val fetched = withContext(Dispatchers.IO) {
            try {
                KlipyService.memeFeed()
            } catch (e: Exception) {
                Log.e("GifScroll", "Feed load failed", e)
                error = e.message ?: e.toString()
                emptyList()
            }
        }
        // Rank by the user's liked keywords.
        items = fetched.sortedByDescending { container.likeManager.score(it.title) }
        if (items.isEmpty() && error == null) {
            error = "Klipy returned no usable GIFs."
        }
        loading = false
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        when {
            loading -> CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = Color.White
            )
            error != null && items.isEmpty() -> {
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Couldn't load memes", color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error ?: "",
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { reloadKey++ }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                // Interleave ad pages following the repeating pattern (null = ad slot).
                val pages = remember(items) {
                    buildList<FeedItem?> {
                        var sinceAd = 0
                        var patternIndex = 0
                        for (item in items) {
                            add(item)
                            sinceAd++
                            if (sinceAd >= Ads.AD_PATTERN[patternIndex % Ads.AD_PATTERN.size]) {
                                add(null)
                                sinceAd = 0
                                patternIndex++
                            }
                        }
                    }
                }
                val pagerState = rememberPagerState(pageCount = { pages.size })
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val entry = pages[page]
                    if (entry != null) {
                        FeedPage(
                            item = entry,
                            container = container,
                            isPlaying = pagerState.currentPage == page
                        )
                    } else {
                        AdPage()
                    }
                }
            }
        }
    }
}

/** Big laugh-react burst, centered, shown on double-tap (matches iOS). */
@Composable
fun LaughBurst(onDone: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        delay(400)
        alpha.animateTo(0f, tween(300))
        onDone()
    }
    Icon(
        imageVector = Icons.Filled.SentimentVerySatisfied,
        contentDescription = null,
        tint = Color.Yellow,
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
    )
}

@Composable
fun FeedPage(item: FeedItem, container: AppContainer, isPlaying: Boolean) {
    val context = LocalContext.current
    var liked by remember(item.id) {
        mutableStateOf(container.likeManager.isLiked(item.id))
    }
    var showComments by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    val reposts by container.repostService.reposts.collectAsState()
    val uid = container.authManager.userId.collectAsState().value ?: "local"
    val signedIn by container.authManager.isSignedIn.collectAsState()
    val alreadyShared = reposts.any { it.itemId == item.id }

    // Double-tap to laugh-react.
    var burstKey by remember { mutableIntStateOf(0) }
    val doubleTapLike: () -> Unit = {
        if (!container.likeManager.isLiked(item.id)) {
            container.likeManager.toggleLike(item.id, item.title, item.url, item.kind.name, uid, signedIn)
            liked = true
        }
        burstKey++
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (item.kind == FeedItem.Kind.VIDEO) {
            VideoPage(
                url = item.url,
                isPlaying = isPlaying,
                onDoubleTap = doubleTapLike
            )
        } else {
            AsyncImage(
                model = item.url,
                contentDescription = item.title,
                imageLoader = rememberGifImageLoader(),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTapGestures(onDoubleTap = { doubleTapLike() })
                    },
                contentScale = ContentScale.Fit
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircleButton(
                icon = if (liked) Icons.Filled.SentimentVerySatisfied
                else Icons.Filled.SentimentSatisfied,
                tint = if (liked) Color.Yellow else Color.White,
                onClick = {
                    container.likeManager.toggleLike(item.id, item.title, item.url, item.kind.name, uid, signedIn)
                    liked = container.likeManager.isLiked(item.id)
                }
            )
            CircleButton(
                icon = Icons.Filled.ChatBubbleOutline,
                onClick = { showComments = true }
            )
            CircleButton(
                icon = Icons.Filled.Flag,
                onClick = { showReport = true }
            )
            Box {
                CircleButton(
                    icon = Icons.Filled.Share,
                    onClick = { showShareMenu = true }
                )
                DropdownMenu(
                    expanded = showShareMenu,
                    onDismissRequest = { showShareMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (alreadyShared) "Remove from my page" else "Share to my page") },
                        onClick = {
                            showShareMenu = false
                            if (alreadyShared) {
                                reposts.firstOrNull { it.itemId == item.id }?.let {
                                    container.repostService.unshare(it, uid, signedIn)
                                }
                            } else {
                                container.repostService.share(item, uid, signedIn)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share via…") },
                        onClick = {
                            showShareMenu = false
                            shareUrl(context, item.url)
                        }
                    )
                }
            }
        }

        // Floating wordmark, Instagram-style.
        GifScrollLogo(
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .zIndex(5f)
        )

        // Centered laugh burst on double-tap.
        key(burstKey) {
            if (burstKey > 0) {
                Box(Modifier.fillMaxSize().zIndex(10f), contentAlignment = Alignment.Center) {
                    LaughBurst(onDone = { burstKey = 0 })
                }
            }
        }
    }

    if (showComments) {
        CommentsSheet(
            postId = null,
            gifId = item.id,
            container = container,
            onDismiss = { showComments = false }
        )
    }
    if (showReport) {
        ReportDialog(
            target = ReportTarget.Gif(item.id),
            container = container,
            onDismiss = { showReport = false }
        )
    }
}
