package com.emckeon97.gifscroll.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.emckeon97.gifscroll.data.AppContainer
import com.emckeon97.gifscroll.data.GiphyService
import com.emckeon97.gifscroll.model.Gif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** ImageLoader with animated-GIF support. */
@Composable
fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }
}

/** The comedy GIF feed: one full-screen swipeable GIF per page. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(container: AppContainer, modifier: Modifier = Modifier) {
    var gifs by remember { mutableStateOf<List<Gif>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val fetched = withContext(Dispatchers.IO) {
            try {
                GiphyService.comedyFeed()
            } catch (e: Exception) {
                emptyList()
            }
        }
        // Rank by the user's liked keywords.
        gifs = fetched.sortedByDescending { container.likeManager.score(it.title) }
        loading = false
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        when {
            loading -> CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = Color.White
            )
            else -> {
                val pagerState = rememberPagerState(pageCount = { gifs.size })
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    GifPage(gif = gifs[page], container = container)
                }
            }
        }
    }
}

@Composable
fun GifPage(gif: Gif, container: AppContainer) {
    val context = LocalContext.current
    var liked by remember(gif.id) {
        mutableStateOf(container.likeManager.isLiked(gif.id))
    }
    var showComments by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = gif.url,
            contentDescription = gif.title,
            imageLoader = rememberGifImageLoader(),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

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
                    container.likeManager.toggleLike(gif.id, gif.title)
                    liked = container.likeManager.isLiked(gif.id)
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
            CircleButton(
                icon = Icons.Filled.Share,
                onClick = { shareUrl(context, gif.url) }
            )
        }
    }

    if (showComments) {
        CommentsSheet(
            postId = null,
            gifId = gif.id,
            container = container,
            onDismiss = { showComments = false }
        )
    }
    if (showReport) {
        ReportDialog(
            target = ReportTarget.Gif(gif.id),
            container = container,
            onDismiss = { showReport = false }
        )
    }
}
