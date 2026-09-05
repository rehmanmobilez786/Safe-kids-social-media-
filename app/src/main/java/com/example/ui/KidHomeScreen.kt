package com.example.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.VideoItem
import com.example.ui.theme.*
import com.example.viewmodel.SafeKidViewModel

/**
 * Facebook-style Video Feed and Watch experience for SafeKid.
 * Delivers an authentic Facebook Watch / News Feed interface with
 * in-feed video playback, creator profiles, Facebook reactions,
 * stories tray, and screen time management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidHomeScreen(
    viewModel: SafeKidViewModel,
    isUrdu: Boolean,
    onNavigateToParentUnlock: () -> Unit
) {
    val allVideos by viewModel.allVideos.collectAsState()
    val config by viewModel.config.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedContentType by viewModel.selectedContentType.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val isTimeLimitExceeded by viewModel.isTimeLimitExceeded.collectAsState()
    val searchAlertMessage by viewModel.searchAlertMessage.collectAsState()
    val likedVideoIds by viewModel.likedVideoIds.collectAsState()
    val savedVideoIds by viewModel.savedVideoIds.collectAsState()
    val followedChannels by viewModel.followedChannels.collectAsState()
    val activeInFeedPlayerId by viewModel.activeInFeedPlayerId.collectAsState()
    val activeFeedTab by viewModel.activeFeedTab.collectAsState()
    val kidComments by viewModel.kidComments.collectAsState()

    var showSearchField by remember { mutableStateOf(false) }
    var commentSheetVideoId by remember { mutableStateOf<Int?>(null) }
    var shareNoticeMessage by remember { mutableStateOf<String?>(null) }

    val allowedPlatforms = remember(config.allowedPlatforms) {
        config.allowedPlatforms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // Filter videos based on platform, category, search query, content type (Videos vs Reels), and tab
    val filteredVideos = remember(
        allVideos, selectedPlatform, selectedCategory, searchQuery, allowedPlatforms, activeFeedTab, savedVideoIds, selectedContentType
    ) {
        allVideos.filter { video ->
            // Platform allowance
            val isAllowedPlatform = allowedPlatforms.any { it.equals(video.platform, ignoreCase = true) }
            // Specific selected platform filter
            val matchesPlatform = selectedPlatform == null || 
                                  selectedPlatform == "All" || 
                                  video.platform.equals(selectedPlatform, ignoreCase = true)
            // Category filter
            val matchesCategory = selectedCategory == null || 
                                  selectedCategory == "All" || 
                                  video.category.equals(selectedCategory, ignoreCase = true)
            // Search query filter
            val matchesSearch = searchQuery.isBlank() || 
                                video.title.contains(searchQuery, ignoreCase = true) ||
                                video.category.contains(searchQuery, ignoreCase = true)
            // Saved tab filter
            val matchesTab = if (activeFeedTab == "Saved") savedVideoIds.contains(video.id) else true

            // Content type filter (Videos vs Reels)
            val isReel = video.duration.startsWith("0:") || 
                         video.title.contains("Reel", ignoreCase = true) || 
                         video.title.contains("Short", ignoreCase = true) ||
                         video.title.contains("60s", ignoreCase = true) ||
                         video.platform.equals("TikTok", ignoreCase = true)
            val matchesContentType = when (selectedContentType) {
                "Reels" -> isReel
                "Videos" -> !isReel
                else -> true
            }

            isAllowedPlatform && matchesPlatform && matchesCategory && matchesSearch && matchesTab && matchesContentType
        }
    }

    // Screen Time Over Limit Display
    if (isTimeLimitExceeded) {
        TimeLimitExceededOverlay(
            isUrdu = isUrdu,
            onUnlock = onNavigateToParentUnlock,
            viewModel = viewModel
        )
        return
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                // Facebook Top App Bar with dynamic channel branding
                FacebookTopAppBar(
                    isUrdu = isUrdu,
                    selectedPlatform = selectedPlatform,
                    onSearchToggle = { showSearchField = !showSearchField },
                    onLanguageToggle = { viewModel.toggleLanguage() },
                    onParentUnlock = onNavigateToParentUnlock,
                    elapsedSeconds = elapsedSeconds,
                    totalLimitMinutes = config.dailyTimeLimitMinutes
                )

                // Facebook Search Expandable Bar
                AnimatedVisibility(
                    visible = showSearchField || searchQuery.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    FacebookSearchRow(
                        searchQuery = searchQuery,
                        onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                        isUrdu = isUrdu
                    )
                }

                // Facebook Tabs Bar (Watch, Feed, Saved)
                FacebookTabsBar(
                    activeTab = activeFeedTab,
                    onTabSelected = { viewModel.setActiveFeedTab(it) },
                    savedCount = savedVideoIds.size,
                    isUrdu = isUrdu
                )

                HorizontalDivider(color = FacebookDivider, thickness = 1.dp)
            }
        },
        containerColor = FacebookFeedBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FacebookFeedBg),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. All Social Media Channel Buttons (Facebook, YouTube, TikTok, Instagram, Dailymotion)
            item {
                SocialChannelsButtonBar(
                    allowedPlatforms = allowedPlatforms,
                    selectedPlatform = selectedPlatform,
                    onPlatformSelected = { viewModel.setPlatform(it) },
                    isUrdu = isUrdu
                )
            }

            // 2. Content Type Selector Bar (All, Videos 📺, Reels ⚡)
            item {
                ContentTypePillBar(
                    selectedType = selectedContentType,
                    onTypeSelected = { viewModel.setContentType(it) },
                    isUrdu = isUrdu
                )
            }

            // 3. Category Tray (All, Science, Education, Cartoons, Rhymes)
            item {
                FacebookStoriesCategoryTray(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setCategory(it) },
                    isUrdu = isUrdu
                )
            }

            // Notice if video was shared/saved
            if (shareNoticeMessage != null) {
                item {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = shareNoticeMessage!!,
                                color = Color(0xFF1B5E20),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { shareNoticeMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Close, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 4. Authentic Video Feed according to each Social Media Channel
            if (filteredVideos.isEmpty()) {
                item {
                    EmptyVideosState(isUrdu = isUrdu, hasSearch = searchQuery.isNotEmpty())
                }
            } else {
                items(filteredVideos, key = { it.id }) { video ->
                    when (video.platform.lowercase()) {
                        "youtube" -> {
                            YouTubeVideoCard(
                                video = video,
                                isPlayingInFeed = activeInFeedPlayerId == video.id,
                                isLiked = likedVideoIds.contains(video.id),
                                isSaved = savedVideoIds.contains(video.id),
                                isSubscribed = followedChannels.contains(getChannelNameForVideo(video)),
                                isUrdu = isUrdu,
                                onPlayInFeed = {
                                    if (activeInFeedPlayerId == video.id) {
                                        viewModel.playInFeed(null)
                                    } else {
                                        viewModel.playInFeed(video.id)
                                    }
                                },
                                onOpenFullscreenWatch = {
                                    viewModel.playInFeed(null)
                                    viewModel.selectVideo(video)
                                },
                                onToggleLike = { viewModel.toggleLike(video.id) },
                                onToggleSave = {
                                    viewModel.toggleSave(video.id)
                                    shareNoticeMessage = if (savedVideoIds.contains(video.id)) {
                                        if (isUrdu) "ویڈیو محفوظ شدہ لسٹ سے ہٹا دی گئی" else "Video removed from Saved"
                                    } else {
                                        if (isUrdu) "یوٹیوب ویڈیو محفوظ کر لی گئی! ⭐" else "YouTube video saved! ⭐"
                                    }
                                },
                                onToggleSubscribe = {
                                    viewModel.toggleFollowChannel(getChannelNameForVideo(video))
                                },
                                onShare = {
                                    shareNoticeMessage = if (isUrdu) "لنک کامیابی سے شیئر ہو گیا! 🔗" else "Safe link copied & shared! 🔗"
                                }
                            )
                        }
                        "tiktok" -> {
                            TikTokReelCard(
                                video = video,
                                isPlayingInFeed = activeInFeedPlayerId == video.id,
                                isLiked = likedVideoIds.contains(video.id),
                                isSaved = savedVideoIds.contains(video.id),
                                isUrdu = isUrdu,
                                onPlayInFeed = {
                                    if (activeInFeedPlayerId == video.id) {
                                        viewModel.playInFeed(null)
                                    } else {
                                        viewModel.playInFeed(video.id)
                                    }
                                },
                                onOpenFullscreenWatch = {
                                    viewModel.playInFeed(null)
                                    viewModel.selectVideo(video)
                                },
                                onToggleLike = { viewModel.toggleLike(video.id) },
                                onToggleSave = {
                                    viewModel.toggleSave(video.id)
                                    shareNoticeMessage = if (savedVideoIds.contains(video.id)) {
                                        if (isUrdu) "ٹک ٹاک ریل محفوظ لسٹ سے ہٹا دی گئی" else "TikTok reel removed from Saved"
                                    } else {
                                        if (isUrdu) "ٹک ٹاک ریل محفوظ کر لی گئی! ⚡" else "TikTok reel saved! ⚡"
                                    }
                                },
                                onOpenComments = {
                                    commentSheetVideoId = video.id
                                },
                                onShare = {
                                    shareNoticeMessage = if (isUrdu) "ٹک ٹاک ریل شیئر ہو گئی! 🔗" else "TikTok reel link shared! 🔗"
                                }
                            )
                        }
                        "instagram" -> {
                            InstagramReelCard(
                                video = video,
                                isPlayingInFeed = activeInFeedPlayerId == video.id,
                                isLiked = likedVideoIds.contains(video.id),
                                isSaved = savedVideoIds.contains(video.id),
                                isFollowed = followedChannels.contains(getChannelNameForVideo(video)),
                                isUrdu = isUrdu,
                                onPlayInFeed = {
                                    if (activeInFeedPlayerId == video.id) {
                                        viewModel.playInFeed(null)
                                    } else {
                                        viewModel.playInFeed(video.id)
                                    }
                                },
                                onOpenFullscreenWatch = {
                                    viewModel.playInFeed(null)
                                    viewModel.selectVideo(video)
                                },
                                onToggleLike = { viewModel.toggleLike(video.id) },
                                onToggleSave = {
                                    viewModel.toggleSave(video.id)
                                    shareNoticeMessage = if (savedVideoIds.contains(video.id)) {
                                        if (isUrdu) "انسٹاگرام ریل محفوظ لسٹ سے ہٹا دی گئی" else "Instagram reel removed from Saved"
                                    } else {
                                        if (isUrdu) "انسٹاگرام ریل محفوظ کر لی گئی! 🎨" else "Instagram reel saved! 🎨"
                                    }
                                },
                                onToggleFollow = {
                                    viewModel.toggleFollowChannel(getChannelNameForVideo(video))
                                },
                                onOpenComments = {
                                    commentSheetVideoId = video.id
                                },
                                onShare = {
                                    shareNoticeMessage = if (isUrdu) "انسٹاگرام ریل شیئر ہو گئی! 🔗" else "Instagram reel link shared! 🔗"
                                }
                            )
                        }
                        "dailymotion" -> {
                            DailymotionVideoCard(
                                video = video,
                                isPlayingInFeed = activeInFeedPlayerId == video.id,
                                isLiked = likedVideoIds.contains(video.id),
                                isSaved = savedVideoIds.contains(video.id),
                                isUrdu = isUrdu,
                                onPlayInFeed = {
                                    if (activeInFeedPlayerId == video.id) {
                                        viewModel.playInFeed(null)
                                    } else {
                                        viewModel.playInFeed(video.id)
                                    }
                                },
                                onOpenFullscreenWatch = {
                                    viewModel.playInFeed(null)
                                    viewModel.selectVideo(video)
                                },
                                onToggleLike = { viewModel.toggleLike(video.id) },
                                onToggleSave = {
                                    viewModel.toggleSave(video.id)
                                    shareNoticeMessage = if (savedVideoIds.contains(video.id)) {
                                        if (isUrdu) "ڈیلی موشن ویڈیو ہٹا دی گئی" else "Dailymotion video removed"
                                    } else {
                                        if (isUrdu) "ڈیلی موشن ویڈیو محفوظ کر لی گئی! 📽️" else "Dailymotion video saved! 📽️"
                                    }
                                },
                                onShare = {
                                    shareNoticeMessage = if (isUrdu) "ڈیلی موشن لنک شیئر ہو گیا! 🔗" else "Dailymotion link shared! 🔗"
                                }
                            )
                        }
                        else -> {
                            FacebookVideoPostCard(
                                video = video,
                                isPlayingInFeed = activeInFeedPlayerId == video.id,
                                isLiked = likedVideoIds.contains(video.id),
                                isSaved = savedVideoIds.contains(video.id),
                                isFollowed = followedChannels.contains(getChannelNameForVideo(video)),
                                commentsCount = 180 + (kidComments[video.id]?.size ?: 0),
                                isUrdu = isUrdu,
                                onPlayInFeed = {
                                    if (activeInFeedPlayerId == video.id) {
                                        viewModel.playInFeed(null)
                                    } else {
                                        viewModel.playInFeed(video.id)
                                    }
                                },
                                onOpenFullscreenWatch = {
                                    viewModel.playInFeed(null)
                                    viewModel.selectVideo(video)
                                },
                                onToggleLike = { viewModel.toggleLike(video.id) },
                                onToggleSave = {
                                    viewModel.toggleSave(video.id)
                                    shareNoticeMessage = if (savedVideoIds.contains(video.id)) {
                                        if (isUrdu) "ویڈیو محفوظ شدہ لسٹ سے ہٹا دی گئی" else "Video removed from Saved"
                                    } else {
                                        if (isUrdu) "ویڈیو محفوظ کر لی گئی ہے! ⭐" else "Video saved to Watchlist! ⭐"
                                    }
                                },
                                onToggleFollow = {
                                    viewModel.toggleFollowChannel(getChannelNameForVideo(video))
                                },
                                onOpenComments = {
                                    commentSheetVideoId = video.id
                                },
                                onShare = {
                                    shareNoticeMessage = if (isUrdu) "لنک کامیابی سے شیئر ہو گیا! 🔗" else "Safe link copied & shared! 🔗"
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // Kid Safe Comment Sheet
    if (commentSheetVideoId != null) {
        val videoId = commentSheetVideoId!!
        ModalBottomSheet(
            onDismissRequest = { commentSheetVideoId = null },
            containerColor = FacebookSurface,
            contentColor = Color.White
        ) {
            FacebookKidCommentsContent(
                isUrdu = isUrdu,
                customComments = kidComments[videoId] ?: emptyList(),
                onAddComment = { newComment ->
                    viewModel.addKidComment(videoId, newComment)
                }
            )
        }
    }

    // Safety Alert Dialog
    if (searchAlertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSearchAlert() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Shield",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "محفوظ فلٹر الرٹ" else "Safety Filter Alert",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = searchAlertMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearSearchAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                ) {
                    Text(text = if (isUrdu) "ٹھیک ہے" else "Okay")
                }
            }
        )
    }
}

/**
 * Facebook / Multi-Platform Top App Bar with authentic brand styling, search, language, and timer.
 */
@Composable
fun FacebookTopAppBar(
    isUrdu: Boolean,
    selectedPlatform: String? = null,
    onSearchToggle: () -> Unit,
    onLanguageToggle: () -> Unit,
    onParentUnlock: () -> Unit,
    elapsedSeconds: Int,
    totalLimitMinutes: Int
) {
    val totalLimitSeconds = totalLimitMinutes * 60
    val remainingSeconds = (totalLimitSeconds - elapsedSeconds).coerceAtLeast(0)
    val minutesLeft = remainingSeconds / 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Dynamic Brand Logo & Identity
        when (selectedPlatform?.lowercase()) {
            "youtube" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = YouTubeRed,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp).padding(2.dp)
                        )
                    }
                    Text(
                        text = "YouTube",
                        color = Color(0xFF0F0F0F),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = YouTubeRed,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isUrdu) "کڈز" else "Kids",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            "tiktok" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TikTok",
                        color = TikTokBlack,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = TikTokPink,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isUrdu) "ریلز ⚡" else "Reels ⚡",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            "instagram" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Instagram",
                        color = InstagramRed,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        modifier = Modifier.background(InstagramGradient, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = if (isUrdu) "ریلز" else "Reels",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            "dailymotion" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = DailymotionBlue,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "d",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "dailymotion",
                        color = DailymotionBlue,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
            else -> {
                // Facebook Logo typography
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "facebook",
                        color = FacebookBlue,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.2).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = FacebookBlue,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isUrdu) "کڈز واچ" else "Kids Watch",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Right side action pills
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search button
            Surface(
                shape = CircleShape,
                color = FacebookDivider,
                modifier = Modifier.size(38.dp)
            ) {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = FacebookTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Screen Timer Badge
            Surface(
                shape = RoundedCornerShape(19.dp),
                color = if (minutesLeft < 5) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                modifier = Modifier.height(38.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HourglassTop,
                        contentDescription = "Timer",
                        tint = if (minutesLeft < 5) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${minutesLeft}m",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (minutesLeft < 5) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        modifier = Modifier.testTag("remaining_timer")
                    )
                }
            }

            // Language Switcher
            Surface(
                shape = CircleShape,
                color = FacebookDivider,
                modifier = Modifier.size(38.dp)
            ) {
                IconButton(onClick = onLanguageToggle) {
                    Text(
                        text = if (isUrdu) "EN" else "اردو",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FacebookTextPrimary
                    )
                }
            }

            // Parent Unlock Button
            Surface(
                shape = CircleShape,
                color = FacebookLightBlue,
                modifier = Modifier.size(38.dp)
            ) {
                IconButton(
                    onClick = onParentUnlock,
                    modifier = Modifier.testTag("parent_unlock_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Parent Portal",
                        tint = FacebookBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Facebook Navigation Tabs Bar (Watch, Feed, Saved)
 */
@Composable
fun FacebookTabsBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    savedCount: Int,
    isUrdu: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Watch Tab (Default Facebook Watch video feed)
        FacebookTabItem(
            title = if (isUrdu) "واچ (Watch)" else "Watch",
            icon = Icons.Filled.OndemandVideo,
            isSelected = activeTab == "Watch",
            badgeCount = 0,
            onClick = { onTabSelected("Watch") },
            modifier = Modifier.weight(1f)
        )

        // Feed Tab
        FacebookTabItem(
            title = if (isUrdu) "فیڈ (Feed)" else "Feed",
            icon = Icons.Filled.Home,
            isSelected = activeTab == "Feed",
            badgeCount = 0,
            onClick = { onTabSelected("Feed") },
            modifier = Modifier.weight(1f)
        )

        // Saved Tab
        FacebookTabItem(
            title = if (isUrdu) "محفوظ ویڈیوز" else "Saved",
            icon = Icons.Filled.Bookmark,
            isSelected = activeTab == "Saved",
            badgeCount = savedCount,
            onClick = { onTabSelected("Saved") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FacebookTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) FacebookBlue else FacebookTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            if (badgeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = FacebookLoveRed,
                    modifier = Modifier
                        .size(16.dp)
                        .offset(x = 6.dp, y = (-4).dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$badgeCount",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) FacebookBlue else FacebookTextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(if (isSelected) FacebookBlue else Color.Transparent)
        )
    }
}

/**
 * Facebook Stories / Category Tray (Colorful Story Circles)
 */
@Composable
fun FacebookStoriesCategoryTray(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    isUrdu: Boolean
) {
    val categories = listOf(
        Triple("All", if (isUrdu) "تمام" else "All", Icons.Filled.AutoAwesome),
        Triple("Science", if (isUrdu) "سائنس" else "Science", Icons.Filled.Biotech),
        Triple("Education", if (isUrdu) "تعلیم" else "Education", Icons.Filled.School),
        Triple("Cartoon", if (isUrdu) "کارٹون" else "Cartoon", Icons.Filled.Mood),
        Triple("Rhymes", if (isUrdu) "نظمیں" else "Rhymes", Icons.Filled.MusicNote)
    )

    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(categories) { (catKey, label, icon) ->
                val isSelected = (selectedCategory == catKey) || (catKey == "All" && selectedCategory == null)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onCategorySelected(if (catKey == "All") null else catKey) }
                        .testTag("category_chip_$catKey")
                ) {
                    // Facebook Story circle with vibrant gradient ring
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    Brush.sweepGradient(
                                        listOf(
                                            FacebookBlue,
                                            Color(0xFF00C6FF),
                                            FacebookLoveRed,
                                            FacebookBlue
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(FacebookDivider, FacebookDivider)
                                    )
                                }
                            )
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) FacebookLightBlue else Color(0xFFF7F8FA),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) FacebookBlue else FacebookTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) FacebookBlue else FacebookTextPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Facebook Platform Filter Chips Row (All, YouTube, Facebook, Vimeo, TikTok)
 */
@Composable
fun FacebookPlatformFilterRow(
    allowedPlatforms: List<String>,
    selectedPlatform: String?,
    onPlatformSelected: (String?) -> Unit,
    isUrdu: Boolean
) {
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All Platforms" Chip
            item {
                FilterChip(
                    selected = selectedPlatform == null || selectedPlatform == "All",
                    onClick = { onPlatformSelected(null) },
                    label = {
                        Text(
                            text = if (isUrdu) "سب پلیٹ فارمز" else "All Platforms",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FacebookBlue,
                        selectedLabelColor = Color.White,
                        containerColor = FacebookFeedBg
                    )
                )
            }

            items(allowedPlatforms) { platform ->
                val isSelected = selectedPlatform?.equals(platform, ignoreCase = true) == true
                FilterChip(
                    selected = isSelected,
                    onClick = { onPlatformSelected(if (isSelected) null else platform) },
                    label = {
                        Text(
                            text = platform,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FacebookBlue,
                        selectedLabelColor = Color.White,
                        containerColor = FacebookFeedBg
                    )
                )
            }
        }
    }
}

/**
 * Facebook Search Row
 */
@Composable
fun FacebookSearchRow(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    isUrdu: Boolean
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = {
                Text(
                    text = if (isUrdu) "فیس بک واچ پر محفوظ ویڈیوز تلاش کریں..." else "Search safe Facebook Watch videos...",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = FacebookBlue
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            } else null,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kid_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FacebookBlue,
                unfocusedBorderColor = FacebookDivider,
                focusedContainerColor = FacebookFeedBg,
                unfocusedContainerColor = FacebookFeedBg
            ),
            singleLine = true
        )
    }
}

/**
 * Facebook Video Post Card:
 * Features a creator header, title caption, embedded in-feed video player,
 * like/comment counters, and Facebook reaction bar.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FacebookVideoPostCard(
    video: VideoItem,
    isPlayingInFeed: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowed: Boolean,
    commentsCount: Int,
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
    var userLiked by remember(isLiked) { mutableStateOf(isLiked) }
    var likeCount by remember(video.id) { mutableStateOf(1150 + (video.id * 149) % 2400) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_item_${video.id}"),
        shape = RoundedCornerShape(0.dp), // Facebook feed posts stretch edge-to-edge
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // 1. Facebook Post Header (Creator Profile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Creator Avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
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
                            text = channel.firstOrNull()?.uppercase() ?: "S",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FacebookTextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = FacebookBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isUrdu) "محفوظ ویڈیوز • ۲ گھنٹے" else "Safe Whitelist • 2h",
                                style = MaterialTheme.typography.labelSmall,
                                color = FacebookTextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "•", color = FacebookTextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = "Public",
                                tint = FacebookTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Follow Pill Button
                TextButton(
                    onClick = onToggleFollow,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isFollowed) FacebookFeedBg else FacebookLightBlue,
                        contentColor = if (isFollowed) FacebookTextSecondary else FacebookBlue
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (isFollowed) {
                            if (isUrdu) "فالو شدہ ✓" else "Following"
                        } else {
                            if (isUrdu) "+ فالو کریں" else "+ Follow"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // 2. Post Title / Caption
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = FacebookTextPrimary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "#${video.category} #${video.platform}Safe #KidsWatch",
                    color = FacebookBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. The Video Player Section (Facebook In-Feed Player)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isPlayingInFeed) {
                    // LIVE IN-FEED WEBVIEW VIDEO PLAYER
                    var isCardLoading by remember { mutableStateOf(true) }
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)

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
                            color = FacebookBlue,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Floating Fullscreen Watch expand button
                    IconButton(
                        onClick = onOpenFullscreenWatch,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White
                        )
                    }
                } else {
                    // PREVIEW BANNER WITH FACEBOOK PLAY BUTTON
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPlayInFeed() }
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF1E3C72),
                                        Color(0xFF2A5298)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Large Facebook Style Play Button
                        Surface(
                            shape = CircleShape,
                            color = FacebookBlue,
                            modifier = Modifier.size(56.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Duration Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.75f),
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

                        // Platform Pill
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = video.platform,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quick Play In-Feed / Fullscreen Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F8FA))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPlayInFeed,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingInFeed) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = FacebookBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlayingInFeed) {
                            if (isUrdu) "روکیں" else "Pause Feed"
                        } else {
                            if (isUrdu) "فیڈ میں چلائیں ▶️" else "Play in Feed ▶️"
                        },
                        color = FacebookBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                TextButton(
                    onClick = onOpenFullscreenWatch,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OndemandVideo,
                        contentDescription = null,
                        tint = FacebookTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isUrdu) "فیس بک واچ میں کھولیں ⛶" else "Open in Watch ⛶",
                        color = FacebookTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // 4. Facebook Engagement Counts Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reaction icons (Like & Love)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = FacebookLikeBlue, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Filled.ThumbUp, null, tint = Color.White, modifier = Modifier.padding(3.dp))
                    }
                    Spacer(modifier = Modifier.width((-3).dp))
                    Surface(shape = CircleShape, color = FacebookLoveRed, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.padding(3.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$likeCount",
                        color = FacebookTextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Comments & Views Count
                Text(
                    text = if (isUrdu) "$commentsCount تبصرے • 14K آراء" else "$commentsCount comments • 14K views",
                    color = FacebookTextSecondary,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = FacebookDivider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))

            // 5. Facebook Action Buttons Row (Like, Comment, Share, Save)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
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
                        tint = if (userLiked) FacebookBlue else FacebookTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (userLiked) {
                            if (isUrdu) "پسندیدہ" else "Liked"
                        } else {
                            if (isUrdu) "لائیک" else "Like"
                        },
                        color = if (userLiked) FacebookBlue else FacebookTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Comment Button
                TextButton(onClick = onOpenComments) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = FacebookTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUrdu) "تبصرہ" else "Comment",
                        color = FacebookTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Share Button
                TextButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = FacebookTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUrdu) "شیئر" else "Share",
                        color = FacebookTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Save Button
                TextButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) FacebookReactionYellow else FacebookTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSaved) {
                            if (isUrdu) "محفوظ" else "Saved"
                        } else {
                            if (isUrdu) "سیو" else "Save"
                        },
                        color = if (isSaved) FacebookReactionYellow else FacebookTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Empty Feed State
 */
@Composable
fun EmptyVideosState(isUrdu: Boolean, hasSearch: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = FacebookLightBlue
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (hasSearch) Icons.Filled.SearchOff else Icons.Filled.OndemandVideo,
                    contentDescription = null,
                    tint = FacebookBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isUrdu) {
                if (hasSearch) "اس تلاش میں کوئی ویڈیو نہیں ملی!" else "اس کیٹیگری میں کوئی ویڈیو دستیاب نہیں ہے"
            } else {
                if (hasSearch) "No videos found matching search!" else "No safe videos found in this feed"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = FacebookTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isUrdu) {
                "والدین کنٹرول پینل سے نئی ویڈیوز شامل کر سکتے ہیں۔"
            } else {
                "Parents can whitelist custom links in Parent Settings."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = FacebookTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Screen Time Exceeded Full Screen Overlay
 */
@Composable
fun TimeLimitExceededOverlay(
    isUrdu: Boolean,
    onUnlock: () -> Unit,
    viewModel: SafeKidViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FacebookDarkBg,
                        Color(0xFF0F141C)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = FacebookBlue.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = "Sleep Time",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isUrdu) "سونے کا وقت ہو گیا ہے! 💤" else "Bedtime! Screen Time Finished! 💤",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isUrdu) {
                    "آپ کے آج کا دیکھنے کا وقت ختم ہو چکا ہے۔ پیارے بچوں، اب آرام کریں اور کھیلیں!"
                } else {
                    "You have exhausted your daily screen time limit. Time to play or sleep!"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expired_unlock_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "Unlock", tint = Color.White)
                    Text(
                        text = if (isUrdu) "والدین کا سیکشن (پن کوڈ درج کریں)" else "Parent Entry (Enter PIN)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { viewModel.toggleLanguage() }
            ) {
                Text(
                    text = if (isUrdu) "English" else "اردو",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
