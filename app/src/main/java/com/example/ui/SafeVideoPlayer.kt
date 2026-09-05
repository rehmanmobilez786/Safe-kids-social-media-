package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.VideoItem
import com.example.ui.theme.*

/**
 * Facebook Watch style video player screen.
 * Delivers an authentic Facebook Watch experience with creator follow,
 * reaction counters, interactive Likes, kid-safe comments, and Up Next video queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafeVideoPlayer(
    video: VideoItem,
    allVideos: List<VideoItem> = emptyList(),
    isUrdu: Boolean,
    onBack: () -> Unit,
    onSelectNextVideo: (VideoItem) -> Unit = {},
    isLiked: Boolean = false,
    onToggleLike: () -> Unit = {},
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    followedChannels: Set<String> = emptySet(),
    onToggleFollowChannel: (String) -> Unit = {},
    comments: List<String> = emptyList(),
    onAddComment: (String) -> Unit = {}
) {
    var isLoading by remember(video.id) { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var userLiked by remember(isLiked) { mutableStateOf(isLiked) }
    var likeCount by remember(video.id) { mutableStateOf(1240 + (video.id * 173) % 2500) }
    var userSaved by remember(isSaved) { mutableStateOf(isSaved) }

    val channelName = getChannelNameForVideo(video)
    val isFollowed = followedChannels.contains(channelName)

    // Intercept back button if fullscreen custom view is active
    BackHandler(enabled = customView != null) {
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }

    if (customView != null) {
        // Fullscreen mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val platformColor = when (video.platform.lowercase()) {
                        "youtube" -> Color(0xFFFF0000)
                        "tiktok" -> Color(0xFF00F2FE)
                        "instagram" -> Color(0xFFE1306C)
                        "dailymotion" -> Color(0xFF0066DC)
                        else -> FacebookBlue
                    }
                    val platformIcon = when (video.platform.lowercase()) {
                        "youtube" -> Icons.Filled.PlayArrow
                        "tiktok" -> Icons.Filled.Audiotrack
                        "instagram" -> Icons.Filled.CameraAlt
                        "dailymotion" -> Icons.Filled.VideoLibrary
                        else -> Icons.Filled.ThumbUp
                    }
                    val platformTitle = when (video.platform.lowercase()) {
                        "youtube" -> if (isUrdu) "یوٹیوب کڈز" else "YouTube Kids"
                        "tiktok" -> if (isUrdu) "ٹک ٹاک ریلز" else "TikTok Reels"
                        "instagram" -> if (isUrdu) "انسٹاگرام ریلز" else "Instagram Reels"
                        "dailymotion" -> if (isUrdu) "ڈیلی موشن" else "Dailymotion Kids"
                        else -> if (isUrdu) "فیس بک واچ" else "Facebook Watch"
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = platformColor,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = platformIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = platformTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = platformColor.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isUrdu) "محفوظ" else "SAFE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("video_player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isUrdu) "پیچھے جائیں" else "Go Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Reload button
                    IconButton(
                        onClick = {
                            isLoading = true
                            val (html, baseUrl) = generatePlayerPayload(video)
                            webViewInstance?.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.testTag("video_player_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isUrdu) "تازہ کریں" else "Reload",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FacebookDarkBg,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FacebookDarkBg)
        ) {
            // 1. Facebook 16:9 Video Frame
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                                // Accept cookies for video streaming
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    defaultTextEncodingName = "UTF-8"
                                    cacheMode = WebSettings.LOAD_DEFAULT

                                    val defaultUa = userAgentString
                                    if (defaultUa.contains("; wv")) {
                                        userAgentString = defaultUa.replace("; wv", "")
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        if (newProgress >= 70) {
                                            isLoading = false
                                        }
                                    }

                                    override fun onShowCustomView(
                                        view: View?,
                                        callback: CustomViewCallback?
                                    ) {
                                        customView = view
                                        customViewCallback = callback
                                    }

                                    override fun onHideCustomView() {
                                        customViewCallback?.onCustomViewHidden()
                                        customView = null
                                        customViewCallback = null
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val uri = request?.url ?: return false
                                        val host = uri.host.orEmpty().lowercase()
                                        val scheme = uri.scheme.orEmpty().lowercase()

                                        if (request.isForMainFrame == false) {
                                            return false
                                        }

                                        val isAllowed = host.contains("youtube.com") ||
                                                host.contains("youtube-nocookie.com") ||
                                                host.contains("googlevideo.com") ||
                                                host.contains("ytimg.com") ||
                                                host.contains("vimeo.com") ||
                                                host.contains("vimeocdn.com") ||
                                                host.contains("facebook.com") ||
                                                host.contains("fbcdn.net") ||
                                                scheme == "data" ||
                                                scheme == "about"

                                        if (isAllowed) return false
                                        return true
                                    }
                                }

                                webViewInstance = this
                                val (html, baseUrl) = generatePlayerPayload(video)
                                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                            }
                        },
                        update = { view ->
                            webViewInstance = view
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("secure_webview_player")
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = FacebookBlue,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // 2. Facebook Watch Post Details
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FacebookSurface)
                        .padding(16.dp)
                ) {
                    // Creator Channel Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Channel Avatar
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                FacebookBlue,
                                                Color(0xFF00C6FF)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channelName.firstOrNull()?.uppercase() ?: "K",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = channelName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = FacebookBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isUrdu) "محفوظ مواد • ۲ گھنٹے پہلے" else "Safe Content • 2h ago",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = "Public",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // Follow Button
                        Button(
                            onClick = { onToggleFollowChannel(channelName) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowed) Color(0xFF3A3B3C) else FacebookBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (isFollowed) {
                                    if (isUrdu) "فالو شدہ ✓" else "Following ✓"
                                } else {
                                    if (isUrdu) "+ فالو کریں" else "+ Follow"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Video Title
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tags
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FacebookBlue.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "#${video.category}",
                                color = Color(0xFF64B5F6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${video.platform} Safe Embed",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Facebook Engagement Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reaction Badges
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = FacebookLikeBlue, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.ThumbUp, null, tint = Color.White, modifier = Modifier.padding(3.dp))
                            }
                            Spacer(modifier = Modifier.width((-2).dp))
                            Surface(shape = CircleShape, color = FacebookLoveRed, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.padding(3.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$likeCount",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = if (isUrdu) "${180 + comments.size} تبصرے • 24K آراء" else "${180 + comments.size} comments • 24K views",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Facebook Action Row (Like, Comment, Share, Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Like Button
                        TextButton(
                            onClick = {
                                userLiked = !userLiked
                                likeCount += if (userLiked) 1 else -1
                                onToggleLike()
                            }
                        ) {
                            Icon(
                                imageVector = if (userLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (userLiked) FacebookBlue else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (userLiked) {
                                    if (isUrdu) "پسندیدہ" else "Liked"
                                } else {
                                    if (isUrdu) "لائیک" else "Like"
                                },
                                color = if (userLiked) FacebookBlue else Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Comment Button
                        TextButton(
                            onClick = { showCommentsSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comment",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUrdu) "تبصرہ" else "Comment",
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Save Button
                        TextButton(
                            onClick = {
                                userSaved = !userSaved
                                onToggleSave()
                            }
                        ) {
                            Icon(
                                imageVector = if (userSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (userSaved) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (userSaved) {
                                    if (isUrdu) "محفوظ" else "Saved"
                                } else {
                                    if (isUrdu) "محفوظ کریں" else "Save"
                                },
                                color = if (userSaved) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. Facebook Watch "Up Next" Queue (Next safe recommended videos)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.OndemandVideo,
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isUrdu) "آگے چلنے والی ویڈیوز (Facebook Watch)" else "Up Next on Facebook Watch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3A3B3C)
                        ) {
                            Text(
                                text = if (isUrdu) "آٹو پلے آن" else "Autoplay ON",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Up Next safe video items
            val upNextVideos = allVideos.filter { it.id != video.id }
            items(upNextVideos, key = { it.id }) { nextVideo ->
                FacebookWatchNextCard(
                    video = nextVideo,
                    isUrdu = isUrdu,
                    onClick = { onSelectNextVideo(nextVideo) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Kid Safe Comments Bottom Sheet
    if (showCommentsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            containerColor = FacebookSurface,
            contentColor = Color.White
        ) {
            FacebookKidCommentsContent(
                isUrdu = isUrdu,
                customComments = comments,
                onAddComment = { newComment ->
                    onAddComment(newComment)
                }
            )
        }
    }
}

/**
 * Facebook Watch Mini Card for Up Next Queue
 */
@Composable
fun FacebookWatchNextCard(
    video: VideoItem,
    isUrdu: Boolean,
    onClick: () -> Unit
) {
    val channel = getChannelNameForVideo(video)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("watch_next_item_${video.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FacebookSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF2C3E50),
                                Color(0xFF000000)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                // Duration tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = video.duration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Channel
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = FacebookBlue,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isUrdu) "محفوظ چینل • واچ فیڈ" else "Safe Channel • Watch Feed",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * Kid safe wholesome comments bottom sheet
 */
@Composable
fun FacebookKidCommentsContent(
    isUrdu: Boolean,
    customComments: List<String>,
    onAddComment: (String) -> Unit
) {
    val sampleKidComments = listOf(
        if (isUrdu) "بہت زبردست اور معلوماتی ویڈیو ہے! ⭐" else "Very cool and informative video! ⭐",
        if (isUrdu) "مجھے یہ کہانی بہت پسند آئی! 💖" else "I really loved this story! 💖",
        if (isUrdu) "سائنس کے تجربات بہت کمال کے تھے! 🚀" else "The science experiment was amazing! 🚀",
        if (isUrdu) "پیارے کارٹون اور اچھی نظم! 🎈" else "Cute cartoons and nice rhyme! 🎈"
    )

    val quickReactions = listOf(
        "👍 " + if (isUrdu) "بہت خوب" else "Awesome!",
        "❤️ " + if (isUrdu) "پسند آیا" else "Loved it!",
        "🌟 " + if (isUrdu) "شاندار" else "Super!",
        "😊 " + if (isUrdu) "مزہ آیا" else "Fun!"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUrdu) "بچوں کے محفوظ تبصرے 💬" else "Safe Kids Comments 💬",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2E7D32)
            ) {
                Text(
                    text = if (isUrdu) "محفوظ ماحول ✓" else "Kid Safe ✓",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick reaction pill chips
        Text(
            text = if (isUrdu) "فوری تاثر منتخب کریں:" else "Tap to add reaction:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickReactions.forEach { reaction ->
                Button(
                    onClick = { onAddComment(reaction) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3B3C)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Text(
                        text = reaction,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comments list
        val allDisplayComments = customComments + sampleKidComments
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allDisplayComments) { comment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A3B3C), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FacebookBlue,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "K",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isUrdu) "ننھا دوست (Safe Kid)" else "Kid Friend",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9)
                        )
                        Text(
                            text = comment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extracts a YouTube video ID from various YouTube URL formats.
 */
fun extractYouTubeId(url: String): String? {
    return try {
        if (url.contains("youtube.com/embed/")) {
            url.substringAfter("youtube.com/embed/").substringBefore("?").substringBefore("/")
        } else if (url.contains("youtube-nocookie.com/embed/")) {
            url.substringAfter("youtube-nocookie.com/embed/").substringBefore("?").substringBefore("/")
        } else if (url.contains("watch?v=")) {
            url.substringAfter("watch?v=").substringBefore("&").substringBefore("?")
        } else if (url.contains("youtu.be/")) {
            url.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
        } else if (url.contains("/shorts/")) {
            url.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Extracts a Vimeo video ID from Vimeo URL formats.
 */
fun extractVimeoId(url: String): String? {
    return try {
        if (url.contains("player.vimeo.com/video/")) {
            url.substringAfter("player.vimeo.com/video/").substringBefore("?").substringBefore("/")
        } else if (url.contains("vimeo.com/")) {
            url.substringAfter("vimeo.com/").substringBefore("?").substringBefore("/")
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Extracts a Dailymotion video ID from various URL formats.
 */
fun extractDailymotionId(url: String): String? {
    return try {
        if (url.contains("dailymotion.com/embed/video/")) {
            url.substringAfter("dailymotion.com/embed/video/").substringBefore("?").substringBefore("/")
        } else if (url.contains("dailymotion.com/video/")) {
            url.substringAfter("dailymotion.com/video/").substringBefore("?").substringBefore("_")
        } else if (url.contains("dai.ly/")) {
            url.substringAfter("dai.ly/").substringBefore("?").substringBefore("/")
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Generates an optimized HTML wrapper and corresponding baseUrl for embedding
 * video streams without triggering referer or configuration errors (e.g., YouTube Error 153).
 */
fun generatePlayerPayload(video: VideoItem): Pair<String, String> {
    val url = video.url.trim()
    val isYouTube = video.platform.equals("YouTube", ignoreCase = true) ||
            url.contains("youtube") || url.contains("youtu.be")
    val isDailymotion = video.platform.equals("Dailymotion", ignoreCase = true) ||
            url.contains("dailymotion") || url.contains("dai.ly")
    val isVimeo = video.platform.equals("Vimeo", ignoreCase = true) ||
            url.contains("vimeo")
    val isFacebook = video.platform.equals("Facebook", ignoreCase = true) ||
            url.contains("facebook.com")
    val isTikTok = video.platform.equals("TikTok", ignoreCase = true) ||
            url.contains("tiktok")
    val isInstagram = video.platform.equals("Instagram", ignoreCase = true) ||
            url.contains("instagram")

    if (isYouTube) {
        val ytid = extractYouTubeId(url) ?: url.substringAfterLast("/").substringAfterLast("=")
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        background-color: #000000;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .player-container {
                        position: relative;
                        width: 100%;
                        height: 100%;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: 0;
                    }
                </style>
            </head>
            <body>
                <div class="player-container">
                    <iframe 
                        src="https://www.youtube-nocookie.com/embed/$ytid?autoplay=1&playsinline=1&rel=0&modestbranding=1&enablejsapi=1" 
                        title="YouTube Video Player"
                        frameborder="0" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                        referrerpolicy="strict-origin-when-cross-origin"
                        allowfullscreen>
                    </iframe>
                </div>
            </body>
            </html>
        """.trimIndent()
        return html to "https://www.youtube-nocookie.com"
    } else if (isDailymotion) {
        val dmId = extractDailymotionId(url) ?: url.substringAfterLast("/").substringBefore("?")
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        background-color: #000000;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .player-container {
                        position: relative;
                        width: 100%;
                        height: 100%;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: 0;
                    }
                </style>
            </head>
            <body>
                <div class="player-container">
                    <iframe 
                        src="https://www.dailymotion.com/embed/video/$dmId?autoplay=1&mute=0" 
                        title="Dailymotion Video Player"
                        frameborder="0" 
                        allow="autoplay; fullscreen; picture-in-picture" 
                        allowfullscreen>
                    </iframe>
                </div>
            </body>
            </html>
        """.trimIndent()
        return html to "https://www.dailymotion.com"
    } else if (isVimeo) {
        val vimeoId = extractVimeoId(url) ?: url.substringAfterLast("/")
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        background-color: #000000;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .player-container {
                        position: relative;
                        width: 100%;
                        height: 100%;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: 0;
                    }
                </style>
            </head>
            <body>
                <div class="player-container">
                    <iframe 
                        src="https://player.vimeo.com/video/$vimeoId?autoplay=1&playsinline=1&title=0&byline=0&portrait=0" 
                        title="Vimeo Video Player"
                        frameborder="0" 
                        allow="autoplay; fullscreen; picture-in-picture" 
                        allowfullscreen>
                    </iframe>
                </div>
            </body>
            </html>
        """.trimIndent()
        return html to "https://player.vimeo.com"
    } else if (isFacebook) {
        val encodedUrl = Uri.encode(url)
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        background-color: #000000;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    iframe {
                        width: 100%;
                        height: 100%;
                        border: 0;
                    }
                </style>
            </head>
            <body>
                <iframe 
                    src="https://www.facebook.com/plugins/video.php?href=$encodedUrl&show_text=false&autoplay=true&mute=0" 
                    scrolling="no" 
                    frameborder="0" 
                    allowfullscreen="true" 
                    allow="autoplay; clipboard-write; encrypted-media; picture-in-picture; web-share">
                </iframe>
            </body>
            </html>
        """.trimIndent()
        return html to "https://www.facebook.com"
    } else {
        val isDirectFile = url.endsWith(".mp4", ignoreCase = true) || 
                           url.endsWith(".webm", ignoreCase = true) ||
                           url.endsWith(".m3u8", ignoreCase = true)

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body {
                        width: 100%;
                        height: 100%;
                        background-color: #000000;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    video, iframe {
                        width: 100%;
                        height: 100%;
                        border: 0;
                    }
                </style>
            </head>
            <body>
                ${if (isDirectFile) {
                    """<video controls autoplay playsinline src="$url"></video>"""
                } else {
                    """<iframe src="$url" allow="autoplay; fullscreen" allowfullscreen></iframe>"""
                }}
            </body>
            </html>
        """.trimIndent()
        val base = if (url.startsWith("http")) url else "https://localhost"
        return html to base
    }
}

/**
 * Derives a friendly channel name for each video
 */
fun getChannelNameForVideo(video: VideoItem): String {
    return when {
        video.title.contains("Solar System", ignoreCase = true) || video.title.contains("ہماری کائنات") -> "Kids Science Hub"
        video.title.contains("Urdu", ignoreCase = true) || video.title.contains("حروف") -> "Urdu Kids Official"
        video.title.contains("National Geographic", ignoreCase = true) || video.title.contains("Panda") -> "NatGeo Kids Safe"
        video.title.contains("Volcano", ignoreCase = true) || video.title.contains("Water Cycle") -> "Science Fun Lab"
        video.title.contains("Piper", ignoreCase = true) || video.title.contains("Cartoon") -> "Pixar Kids Shorts"
        video.title.contains("Origami", ignoreCase = true) || video.title.contains("Craft") -> "Creative Kids Crafts"
        video.title.contains("Floating Ink", ignoreCase = true) -> "DIY Science & Crafts"
        video.title.contains("Animals", ignoreCase = true) -> "Baby Animal Wonders"
        else -> "${video.platform} Safe Kids"
    }
}
