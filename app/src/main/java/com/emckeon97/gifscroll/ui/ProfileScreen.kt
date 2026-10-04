package com.emckeon97.gifscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.emckeon97.gifscroll.data.AppContainer
import com.emckeon97.gifscroll.data.LikeManager
import com.emckeon97.gifscroll.model.FeedItem
import com.emckeon97.gifscroll.model.Post
import com.emckeon97.gifscroll.model.Repost
import kotlinx.coroutines.launch

/** The signed-in personal page: header, stats, and Uploads/Shared tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val signedIn by container.authManager.isSignedIn.collectAsState()
    val uid = container.authManager.userId.collectAsState().value ?: "local"
    val email by container.authManager.email.collectAsState()
    val name by container.authManager.displayName.collectAsState()
    val posts by container.postService.posts.collectAsState()
    val reposts by container.repostService.reposts.collectAsState()
    val scope = rememberCoroutineScope()

    var tab by remember { mutableIntStateOf(0) } // 0 = uploads, 1 = shared, 2 = favorites
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var selectedRepost by remember { mutableStateOf<Repost?>(null) }
    var confirmDeletePost by remember { mutableStateOf<Post?>(null) }
    var selectedLiked by remember { mutableStateOf<LikeManager.LikedMeme?>(null) }
    var likesTick by remember { mutableIntStateOf(0) }

    val myUploads = remember(posts, uid) { container.postService.myPosts(uid) }
    val favorites = remember(likesTick) {
        container.likeManager.likedMemes().values.filter { it.url != null }
    }

    LaunchedEffect(uid, signedIn) {
        container.postService.refresh()
        container.repostService.refresh(uid, signedIn)
        container.likeManager.refresh(uid, signedIn)
    }

    Column(modifier.fillMaxSize().background(Color.Black)) {
        // Header.
        Column(
            Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                name ?: "anon",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            email?.let { Text(it, color = Color.Gray, fontSize = 13.sp) }
        }
        // Stats.
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStat(count = myUploads.size, label = "Uploads")
            ProfileStat(count = reposts.size, label = "Shared")
            ProfileStat(count = container.likeManager.likeCount(), label = "Likes")
        }
        // Tabs.
        PrimaryTabRow(
            selectedTabIndex = tab,
            containerColor = Color.Black,
            contentColor = Color.White
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Uploads") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Shared") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Favorites") })
        }
        // Content.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> if (myUploads.isEmpty()) {
                    EmptyTab("No uploads yet", "Post from the Upload tab to see your memes here.")
                } else {
                    UploadGrid(myUploads, container, onPick = { selectedPost = it })
                }
                1 -> if (reposts.isEmpty()) {
                    EmptyTab(
                        "Nothing shared yet",
                        "Tap the share button on any meme to add it to your page."
                    )
                } else {
                    RepostGrid(reposts, onPick = { selectedRepost = it })
                }
                2 -> if (favorites.isEmpty()) {
                    EmptyTab(
                        "No favorites yet",
                        "Tap the laugh button on any meme to save it here."
                    )
                } else {
                    LikedGrid(favorites, onPick = { selectedLiked = it })
                }
            }
        }
        OutlinedButton(
            onClick = { container.authManager.signOut() },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Sign Out", color = Color.Red)
        }
    }

    selectedPost?.let { post ->
        PostDetailDialog(
            post = post,
            container = container,
            onDismiss = { selectedPost = null },
            onDelete = { confirmDeletePost = post; selectedPost = null }
        )
    }
    selectedLiked?.let { liked ->
        LikedDetailDialog(
            liked = liked,
            onDismiss = { selectedLiked = null },
            onUnlike = {
                container.likeManager.toggleLike(liked.id, liked.title, null, null, uid, signedIn)
                likesTick++
                selectedLiked = null
            }
        )
    }
    selectedRepost?.let { repost ->
        RepostDetailDialog(
            repost = repost,
            onDismiss = { selectedRepost = null },
            onRemove = {
                container.repostService.unshare(repost, uid, signedIn)
                selectedRepost = null
            }
        )
    }
    confirmDeletePost?.let { post ->
        AlertDialog(
            onDismissRequest = { confirmDeletePost = null },
            title = { Text("Delete this post?") },
            text = { Text("It will be removed from your page and the community feed.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.postService.deletePost(post) }
                    confirmDeletePost = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeletePost = null }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
}

@Composable
private fun ProfileStat(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyTab(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun UploadGrid(
    uploads: List<Post>,
    container: AppContainer,
    onPick: (Post) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(uploads, key = { it.id }) { post ->
            val model: Any? = post.imageUrl ?: container.postService.fileFor(post)
            AsyncImage(
                model = model,
                contentDescription = post.caption,
                imageLoader = rememberGifImageLoader(),
                modifier = Modifier.aspectRatio(1f).clickable { onPick(post) },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun RepostGrid(reposts: List<Repost>, onPick: (Repost) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(reposts, key = { it.id }) { repost ->
            val isVideo = repost.kind == FeedItem.Kind.VIDEO.name
            Box(Modifier.aspectRatio(1f).clickable { onPick(repost) }) {
                if (isVideo) {
                    Box(
                        Modifier.fillMaxSize().background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = repost.url,
                        contentDescription = repost.title,
                        imageLoader = rememberGifImageLoader(),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/** Full-screen view of one of the user's uploads, with delete. */
@Composable
private fun PostDetailDialog(
    post: Post,
    container: AppContainer,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val model: Any? = post.imageUrl ?: container.postService.fileFor(post)
            AsyncImage(
                model = model,
                contentDescription = post.caption,
                imageLoader = rememberGifImageLoader(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleButton(icon = Icons.Filled.Close, onClick = onDismiss)
                CircleButton(icon = Icons.Filled.Delete, tint = Color.Red, onClick = onDelete)
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (post.caption.isNotEmpty()) {
                    Text(post.caption, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                }
                Text("${post.likeCount} laughs", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

/** Full-screen view of one shared meme, with remove-from-page. */
@Composable
private fun RepostDetailDialog(
    repost: Repost,
    onDismiss: () -> Unit,
    onRemove: () -> Unit
) {
    val isVideo = repost.kind == FeedItem.Kind.VIDEO.name
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (isVideo) {
                VideoPage(url = repost.url, isPlaying = true)
            } else {
                AsyncImage(
                    model = repost.url,
                    contentDescription = repost.title,
                    imageLoader = rememberGifImageLoader(),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleButton(icon = Icons.Filled.Close, onClick = onDismiss)
                CircleButton(icon = Icons.Filled.Delete, tint = Color.Red, onClick = onRemove)
            }
            if (repost.title.isNotEmpty()) {
                Text(
                    repost.title,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LikedGrid(
    favorites: List<LikeManager.LikedMeme>,
    onPick: (LikeManager.LikedMeme) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(favorites, key = { it.id }) { liked ->
            val isVideo = liked.kind == FeedItem.Kind.VIDEO.name
            Box(Modifier.aspectRatio(1f).clickable { onPick(liked) }) {
                if (isVideo) {
                    Box(
                        Modifier.fillMaxSize().background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = liked.url,
                        contentDescription = liked.title,
                        imageLoader = rememberGifImageLoader(),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/** Full-screen view of a favorited meme, with unlike. */
@Composable
private fun LikedDetailDialog(
    liked: LikeManager.LikedMeme,
    onDismiss: () -> Unit,
    onUnlike: () -> Unit
) {
    val isVideo = liked.kind == FeedItem.Kind.VIDEO.name
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (isVideo) {
                VideoPage(url = liked.url ?: "", isPlaying = true)
            } else {
                AsyncImage(
                    model = liked.url,
                    contentDescription = liked.title,
                    imageLoader = rememberGifImageLoader(),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleButton(icon = Icons.Filled.Close, onClick = onDismiss)
                CircleButton(
                    icon = Icons.Filled.SentimentSatisfied,
                    tint = Color.Yellow,
                    onClick = onUnlike
                )
            }
            if (liked.title.isNotEmpty()) {
                Text(
                    liked.title,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }
    }
}
