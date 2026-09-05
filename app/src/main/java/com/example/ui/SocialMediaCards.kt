package com.example.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

// Authentic Brand Colors
val YouTubeRed = Color(0xFFFF0000)
val TikTokBlack = Color(0xFF000000)
val TikTokCyan = Color(0xFF00F2FE)
val TikTokPink = Color(0xFFFE2C55)
val InstagramPurple = Color(0xFF833AB4)
val InstagramRed = Color(0xFFFD1D1D)
val InstagramOrange = Color(0xFFFCB045)
val DailymotionBlue = Color(0xFF0066DC)
val DailymotionDark = Color(0xFF0F172A)

val InstagramGradient = Brush.linearGradient(
    listOf(InstagramPurple, InstagramRed, InstagramOrange)
)

/**
 * Prominent Channel Selector Bar with dedicated buttons for all social media channels:
 * Facebook, YouTube, TikTok, Instagram, Dailymotion.
 */
@Composable
fun SocialChannelsButtonBar(
    allowedPlatforms: List<String>,
    selectedPlatform: String?,
    onPlatformSelected: (String?) -> Unit,
    isUrdu: Boolean
) {
    val platforms = listOf(
        ChannelMeta("All", if (isUrdu) "تمام چینلز" else "All Channels", Color(0xFF333333), Icons.Filled.GridView, null),
        ChannelMeta("Facebook", if (isUrdu) "فیس بک" else "Facebook", FacebookBlue, Icons.Filled.ThumbUp, null),
        ChannelMeta("YouTube", if (isUrdu) "یوٹیوب" else "YouTube", YouTubeRed, Icons.Filled.PlayArrow, null),
        ChannelMeta("TikTok", if (isUrdu) "ٹک ٹاک" else "TikTok", TikTokBlack, Icons.Filled.Audiotrack, null),
        ChannelMeta("Instagram", if (isUrdu) "انسٹاگرام" else "Instagram", InstagramRed, Icons.Filled.CameraAlt, InstagramGradient),
        ChannelMeta("Dailymotion", if (isUrdu) "ڈیلی موشن" else "Dailymotion", DailymotionBlue, Icons.Filled.VideoLibrary, null)
    )

    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // Header label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isUrdu) "سوشل میڈیا چینلز (Social Media Channels):" else "Social Media Channels:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FacebookTextSecondary
                )
                if (selectedPlatform != null && selectedPlatform != "All") {
                    Text(
                        text = if (isUrdu) "فعال چینل: $selectedPlatform" else "Active: $selectedPlatform",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FacebookBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Buttons row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(platforms) { meta ->
                    val isSelected = if (meta.key == "All") {
                        selectedPlatform == null || selectedPlatform == "All"
                    } else {
                        selectedPlatform.equals(meta.key, ignoreCase = true)
                    }

                    // Check if channel is allowed in parental config
                    val isAllowed = meta.key == "All" || allowedPlatforms.any { it.equals(meta.key, ignoreCase = true) }
                    if (isAllowed) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) {
                                if (meta.gradient != null) Color.Transparent else meta.brandColor
                            } else {
                                Color(0xFFF1F3F5)
                            },
                            modifier = Modifier
                                .height(38.dp)
                                .then(
                                    if (isSelected && meta.gradient != null) {
                                        Modifier.background(meta.gradient, RoundedCornerShape(20.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    onPlatformSelected(if (meta.key == "All") null else meta.key)
                                }
                                .testTag("channel_button_${meta.key}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Channel icon badge
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else meta.brandColor,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = meta.icon,
                                            contentDescription = meta.label,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = meta.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else FacebookTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ChannelMeta(
    val key: String,
    val label: String,
    val brandColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val gradient: Brush? = null
)

/**
 * Content Type Toggle Bar: All vs Videos 📺 vs Reels ⚡
 */
@Composable
fun ContentTypePillBar(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    isUrdu: Boolean
) {
    val types = listOf(
        Triple("All", if (isUrdu) "تمام مواد" else "All Content", Icons.Filled.GridView),
        Triple("Videos", if (isUrdu) "ویڈیوز 📺" else "Videos 📺", Icons.Filled.PlayCircleFilled),
        Triple("Reels", if (isUrdu) "ریلز ⚡" else "Reels ⚡", Icons.Filled.Bolt)
    )

    Surface(
        color = Color(0xFFF7F8FA),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { (key, label, icon) ->
                val isSelected = selectedType.equals(key, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Color.White else Color.Transparent,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { onTypeSelected(key) }
                        .testTag("content_type_pill_$key")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                if (key == "Reels") TikTokPink else FacebookBlue
                            } else {
                                FacebookTextSecondary
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) FacebookTextPrimary else FacebookTextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic YouTube Kids Original Video Card
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeVideoCard(
    video: VideoItem,
    isPlayingInFeed: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isSubscribed: Boolean,
    isUrdu: Boolean,
    onPlayInFeed: () -> Unit,
    onOpenFullscreenWatch: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onShare: () -> Unit
) {
    val channel = getChannelNameForVideo(video)
    var likeCount by remember(video.id) { mutableStateOf(24500 + (video.id * 183) % 45000) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("youtube_video_${video.id}"),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // YouTube Red Banner Label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = YouTubeRed,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "YouTube Kids",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = YouTubeRed
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = if (isUrdu) "محفوظ مواد ✓" else "Kid Safe ✓",
                        color = YouTubeRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 16:9 Video Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isPlayingInFeed) {
                    var isCardLoading by remember { mutableStateOf(true) }
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                CookieManager.getInstance().setAcceptCookie(true)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
                                        if (newProgress >= 70) isCardLoading = false
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isCardLoading = false
                                    }
                                }

                                val (html, baseUrl) = generatePlayerPayload(video)
                                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isCardLoading) {
                        CircularProgressIndicator(
                            color = YouTubeRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    // YouTube Thumbnail View
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPlayInFeed() }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = YouTubeRed,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isUrdu) "ویڈیو چلانے کے لیے دبائیں" else "Tap to Play on YouTube",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Duration badge bottom right
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = video.duration,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Fullscreen button
                IconButton(
                    onClick = onOpenFullscreenWatch,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // YouTube Video Info & Channel Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Channel Avatar
                Surface(
                    shape = CircleShape,
                    color = YouTubeRed,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = channel.firstOrNull()?.uppercase() ?: "Y",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F0F0F),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel,
                            fontSize = 12.sp,
                            color = Color(0xFF606060),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF606060),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• 1.2M views • 2 weeks ago",
                            fontSize = 11.sp,
                            color = Color(0xFF606060)
                        )
                    }
                }

                // Subscribe Button
                Button(
                    onClick = onToggleSubscribe,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) Color(0xFFF2F2F2) else YouTubeRed,
                        contentColor = if (isSubscribed) Color(0xFF0F0F0F) else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (isSubscribed) {
                            if (isUrdu) "سبسکرائبڈ ✓" else "Subscribed"
                        } else {
                            if (isUrdu) "سبسکرائب" else "Subscribe"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // YouTube Action Chips Row (Like, Dislike, Share, Save)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Like / Dislike pill
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isLiked) Color(0xFFFFEBEE) else Color(0xFFF2F2F2),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.clickable { onToggleLike() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Like",
                                    tint = if (isLiked) YouTubeRed else Color(0xFF0F0F0F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${(likeCount + if (isLiked) 1 else 0) / 1000}K",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLiked) YouTubeRed else Color(0xFF0F0F0F)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            VerticalDivider(modifier = Modifier.height(14.dp), color = Color(0xFFD0D0D0))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = Color(0xFF0F0F0F),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Share Button
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF2F2F2),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { onShare() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = Color(0xFF0F0F0F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUrdu) "شیئر" else "Share",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F0F0F)
                            )
                        }
                    }
                }

                // Save / Playlist Button
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSaved) Color(0xFFFFEBEE) else Color(0xFFF2F2F2),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { onToggleSave() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (isSaved) YouTubeRed else Color(0xFF0F0F0F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSaved) {
                                    if (isUrdu) "محفوظ شدہ" else "Saved"
                                } else {
                                    if (isUrdu) "محفوظ کریں" else "Save"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSaved) YouTubeRed else Color(0xFF0F0F0F)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Authentic TikTok Reels Card (9:16 Vertical Immersive Short Format)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TikTokReelCard(
    video: VideoItem,
    isPlayingInFeed: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isUrdu: Boolean,
    onPlayInFeed: () -> Unit,
    onOpenFullscreenWatch: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit
) {
    val channel = getChannelNameForVideo(video)
    var likeCount by remember(video.id) { mutableStateOf(42300 + (video.id * 219) % 80000) }

    // Vinyl record rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("tiktok_reel_${video.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TikTokBlack),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(TikTokBlack)
        ) {
            // Live In-Feed Player / Webview
            if (isPlayingInFeed) {
                var isCardLoading by remember { mutableStateOf(true) }
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                loadWithOverviewMode = true
                                useWideViewPort = true
                            }
                            val (html, baseUrl) = generatePlayerPayload(video)
                            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Vertical video preview canvas
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPlayInFeed() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(2.dp, TikTokCyan, CircleShape)
                            .padding(4.dp)
                            .border(2.dp, TikTokPink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Reel",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isUrdu) "ٹک ٹاک ریل چلائیں ⚡" else "Tap to Play TikTok Reel ⚡",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Header: TikTok Kids Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TikTok",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reels",
                            color = TikTokPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onOpenFullscreenWatch,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }
            }

            // Right-Side Floating Action Column (Authentic TikTok Action Strip)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Profile Avatar with pink plus badge
                Box(contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        shape = CircleShape,
                        color = TikTokPink,
                        modifier = Modifier
                            .size(42.dp)
                            .border(1.5.dp, Color.White, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = channel.firstOrNull()?.uppercase() ?: "T",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = TikTokPink,
                        modifier = Modifier
                            .size(16.dp)
                            .offset(y = 6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Follow",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Heart / Like
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) TikTokPink else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "${(likeCount + if (isLiked) 1 else 0) / 1000}K",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comment
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onOpenComments,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Comment,
                            contentDescription = "Comments",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "1.2K",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bookmark / Save
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleSave,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isSaved) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = if (isSaved) "Saved" else "Save",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Share
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Share",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Spinning Vinyl Music Disc
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .rotate(if (isPlayingInFeed) rotation else 0f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(shape = CircleShape, color = TikTokPink, modifier = Modifier.size(14.dp)) {}
                    }
                }
            }

            // Bottom-Left Info Overlay (Creator, Caption, Music Sound Ticker)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.78f)
                    .padding(start = 14.dp, bottom = 14.dp)
            ) {
                // Creator Handle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${channel.replace(" ", "").lowercase()}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Verified",
                        tint = TikTokCyan,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Caption / Title
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Hashtags
                Text(
                    text = "#fyp #kids #safe #${video.category} #reels",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Audio track ticker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Sound",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Original Sound - SafeKid Audio Lab",
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Authentic Instagram Reels Card
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramReelCard(
    video: VideoItem,
    isPlayingInFeed: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowed: Boolean,
    isUrdu: Boolean,
    onPlayInFeed: () -> Unit,
    onOpenFullscreenWatch: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit
) {
    val channel = getChannelNameForVideo(video)
    var likeCount by remember(video.id) { mutableStateOf(18200 + (video.id * 141) % 35000) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("instagram_reel_${video.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Instagram Reels Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reels",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Instagram",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Video Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(Color.Black)
            ) {
                if (isPlayingInFeed) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                val (html, baseUrl) = generatePlayerPayload(video)
                                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPlayInFeed() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isUrdu) "انسٹاگرام ریل دیکھیں" else "Watch Instagram Reel",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fullscreen button
                IconButton(
                    onClick = onOpenFullscreenWatch,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }

                // Right Action Strip (Instagram Heart, Comment, Send, Bookmark)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Heart
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onToggleLike, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) InstagramRed else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = "${(likeCount + if (isLiked) 1 else 0) / 1000}K",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Comment
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onOpenComments, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comment",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(text = "480", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share (Paper airplane)
                    IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Save Bookmark
                    IconButton(onClick = onToggleSave, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom Left Details
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.78f)
                        .padding(start = 14.dp, bottom = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Instagram story ring around avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(1.5.dp, InstagramGradient, CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(InstagramPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = channel.firstOrNull()?.uppercase() ?: "I",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = channel.replace(" ", "_").lowercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                            color = Color.Transparent,
                            modifier = Modifier.clickable { onToggleFollow() }
                        ) {
                            Text(
                                text = if (isFollowed) "Following" else "Follow",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Audio",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$channel • Original audio",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic Dailymotion Video Card
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DailymotionVideoCard(
    video: VideoItem,
    isPlayingInFeed: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isUrdu: Boolean,
    onPlayInFeed: () -> Unit,
    onOpenFullscreenWatch: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit
) {
    val channel = getChannelNameForVideo(video)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dailymotion_video_${video.id}"),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Dailymotion Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = DailymotionBlue,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "d",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "dailymotion",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DailymotionBlue
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DailymotionBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "HD 1080p",
                        color = DailymotionBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 16:9 Video Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isPlayingInFeed) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                val (html, baseUrl) = generatePlayerPayload(video)
                                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPlayInFeed() }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DailymotionBlue,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isUrdu) "ڈیلی موشن پر چلائیں" else "Play on Dailymotion",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onOpenFullscreenWatch,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }
            }

            // Dailymotion Info and Actions
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DailymotionDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "• ${video.duration}", fontSize = 12.sp, color = Color(0xFF64748B))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onToggleLike,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiked) DailymotionBlue else Color(0xFFF1F5F9),
                                contentColor = if (isLiked) Color.White else DailymotionDark
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isLiked) "Liked" else "Like", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onToggleSave,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSaved) DailymotionBlue else Color(0xFFF1F5F9),
                                contentColor = if (isSaved) Color.White else DailymotionDark
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isSaved) "Saved" else "Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share", tint = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}
