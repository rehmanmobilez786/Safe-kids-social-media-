package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen {
    object KidHome : Screen()
    data class VideoPlayer(val video: VideoItem) : Screen()
    object ParentUnlock : Screen()
    object ParentDashboard : Screen()
}

class SafeKidViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SafeKidRepository

    // Screen state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.KidHome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Navigation back stack (simplified)
    private val screenStack = mutableListOf<Screen>(Screen.KidHome)

    // Language state: true = Urdu, false = English
    private val _isUrdu = MutableStateFlow(true)
    val isUrdu: StateFlow<Boolean> = _isUrdu.asStateFlow()

    // Database content
    val allVideos: StateFlow<List<VideoItem>>
    val allLogs: StateFlow<List<ActivityLog>>
    val config: StateFlow<ParentConfig>

    // Kid Search / Platform Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<String?>(null) // null = All platforms shown in Facebook feed
    val selectedPlatform: StateFlow<String?> = _selectedPlatform.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>("All")
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Facebook Style Social States
    private val _likedVideoIds = MutableStateFlow<Set<Int>>(setOf(1, 3))
    val likedVideoIds: StateFlow<Set<Int>> = _likedVideoIds.asStateFlow()

    private val _savedVideoIds = MutableStateFlow<Set<Int>>(setOf(2))
    val savedVideoIds: StateFlow<Set<Int>> = _savedVideoIds.asStateFlow()

    private val _followedChannels = MutableStateFlow<Set<String>>(setOf("Urdu Kids Official", "National Geographic Kids"))
    val followedChannels: StateFlow<Set<String>> = _followedChannels.asStateFlow()

    private val _activeInFeedPlayerId = MutableStateFlow<Int?>(null)
    val activeInFeedPlayerId: StateFlow<Int?> = _activeInFeedPlayerId.asStateFlow()

    private val _activeFeedTab = MutableStateFlow("Watch") // "Watch", "Feed", "Saved"
    val activeFeedTab: StateFlow<String> = _activeFeedTab.asStateFlow()

    private val _selectedContentType = MutableStateFlow("All") // "All", "Videos", "Reels"
    val selectedContentType: StateFlow<String> = _selectedContentType.asStateFlow()

    private val _kidComments = MutableStateFlow<Map<Int, List<String>>>(emptyMap())
    val kidComments: StateFlow<Map<Int, List<String>>> = _kidComments.asStateFlow()

    // Screen Time Counters
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _isTimeLimitExceeded = MutableStateFlow(false)
    val isTimeLimitExceeded: StateFlow<Boolean> = _isTimeLimitExceeded.asStateFlow()

    // Custom alert warning for search filtering
    private val _searchAlertMessage = MutableStateFlow<String?>(null)
    val searchAlertMessage: StateFlow<String?> = _searchAlertMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SafeKidRepository(database.dao())

        // Setup states from repository Flows
        allVideos = repository.allVideos.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allLogs = repository.allLogs.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        // Generate base config if it does not exist
        config = repository.config.map { 
            it ?: ParentConfig() 
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), ParentConfig()
        )

        // Seed default safe kids educational videos if empty or missing any channels
        viewModelScope.launch {
            allVideos.collect { list ->
                val hasInstagram = list.any { it.platform.equals("Instagram", ignoreCase = true) }
                val hasDailymotion = list.any { it.platform.equals("Dailymotion", ignoreCase = true) }
                val hasFacebook = list.any { it.platform.equals("Facebook", ignoreCase = true) }
                val hasTikTok = list.any { it.platform.equals("TikTok", ignoreCase = true) }
                if (list.isEmpty() || !hasInstagram || !hasDailymotion || !hasFacebook || !hasTikTok) {
                    seedDefaultVideos()
                }
            }
        }

        // Sync allowed platforms to ensure all 5 platforms are supported
        viewModelScope.launch {
            config.collect { conf ->
                val list = conf.allowedPlatforms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val needed = listOf("Facebook", "YouTube", "TikTok", "Instagram", "Dailymotion")
                if (needed.any { !list.contains(it) }) {
                    val merged = (list + needed).distinct().joinToString(",")
                    repository.saveConfig(conf.copy(allowedPlatforms = merged))
                }
            }
        }

        // Screen time controller loop
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                // Increment elapsed time only when kid is actively on kid screen
                val current = _currentScreen.value
                val exceeded = _isTimeLimitExceeded.value
                
                if ((current is Screen.KidHome || current is Screen.VideoPlayer) && !exceeded) {
                    val currentConfig = config.value
                    _elapsedSeconds.update { it + 1 }
                    
                    // Check if time limit hit
                    if (_elapsedSeconds.value >= currentConfig.dailyTimeLimitMinutes * 60) {
                        _isTimeLimitExceeded.value = true
                        logActivity("TIME_OUT", "Child daily time limit reached: ${currentConfig.dailyTimeLimitMinutes} minutes")
                    }
                }
            }
        }
    }

    fun toggleLanguage() {
        _isUrdu.update { !it }
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeLast()
        } else {
            _currentScreen.value = Screen.KidHome
        }
    }

    fun setPlatform(platform: String?) {
        _selectedPlatform.value = platform
        _selectedCategory.value = "All" // Reset category on platform change
        platform?.let {
            logActivity("NAVIGATE", "Browsed platform: $it", it)
        }
    }

    fun setContentType(type: String) {
        _selectedContentType.value = type
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    // Safety checks for Child Search queries
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) return

        val currentConfig = config.value
        val keywords = currentConfig.blockedKeywords.split(",").map { it.trim().lowercase() }
        val matchedKeyword = keywords.firstOrNull { query.lowercase().contains(it) }

        if (matchedKeyword != null && currentConfig.isSafeFiltersEnabled) {
            // Screened blocked search query
            _searchQuery.value = "" // clear dangerous search query
            val warningUrdu = "یہ تلاش محفوظ نہیں ہے! آئیے کچھ تعلیمی ویڈیوز دیکھیں!"
            val warningEnglish = "This search is blocked! Let's explore science or animals instead!"
            _searchAlertMessage.value = if (_isUrdu.value) warningUrdu else warningEnglish
            
            // Log block attempt in Parent Portal Room Database
            viewModelScope.launch {
                repository.insertLog(ActivityLog(
                    activityType = "BLOCKED_ATTEMPT",
                    detail = "Blocked Search: '$query' (Keyword: '$matchedKeyword')"
                ))
            }
        } else {
            _searchAlertMessage.value = null
        }
    }

    fun clearSearchAlert() {
        _searchAlertMessage.value = null
    }

    fun selectVideo(video: VideoItem) {
        logActivity("WATCH", "Opened Video: ${video.title}", video.platform)
        navigateTo(Screen.VideoPlayer(video))
    }

    fun toggleLike(videoId: Int) {
        val current = _likedVideoIds.value.toMutableSet()
        if (current.contains(videoId)) {
            current.remove(videoId)
        } else {
            current.add(videoId)
        }
        _likedVideoIds.value = current
    }

    fun toggleSave(videoId: Int) {
        val current = _savedVideoIds.value.toMutableSet()
        if (current.contains(videoId)) {
            current.remove(videoId)
        } else {
            current.add(videoId)
        }
        _savedVideoIds.value = current
    }

    fun toggleFollowChannel(channelName: String) {
        val current = _followedChannels.value.toMutableSet()
        if (current.contains(channelName)) {
            current.remove(channelName)
        } else {
            current.add(channelName)
        }
        _followedChannels.value = current
    }

    fun playInFeed(videoId: Int?) {
        _activeInFeedPlayerId.value = videoId
    }

    fun setActiveFeedTab(tab: String) {
        _activeFeedTab.value = tab
    }

    fun addKidComment(videoId: Int, comment: String) {
        val currentMap = _kidComments.value.toMutableMap()
        val list = (currentMap[videoId] ?: emptyList()).toMutableList()
        list.add(comment)
        currentMap[videoId] = list
        _kidComments.value = currentMap
    }

    // Parental configuration changes
    fun updatePin(newPin: String) {
        viewModelScope.launch {
            repository.saveConfig(config.value.copy(parentPin = newPin))
            logActivity("PIN_CHANGE", "Parent changed entry PIN code")
        }
    }

    fun updateTimeLimit(minutes: Int) {
        viewModelScope.launch {
            val updated = config.value.copy(dailyTimeLimitMinutes = minutes)
            repository.saveConfig(updated)
            // Re-evaluate limit
            _isTimeLimitExceeded.value = _elapsedSeconds.value >= minutes * 60
            logActivity("CONFIG_UPDATE", "Daily screen time limit set to $minutes minutes")
        }
    }

    fun resetDailyTimeUsage() {
        _elapsedSeconds.value = 0
        _isTimeLimitExceeded.value = false
        logActivity("TIME_RESET", "Parent reset the screen timer")
    }

    fun togglePlatformAccess(platform: String, isAllowed: Boolean) {
        viewModelScope.launch {
            val currentPlatforms = config.value.allowedPlatforms.split(",").map { it.trim() }.toMutableList()
            if (isAllowed) {
                if (!currentPlatforms.contains(platform)) currentPlatforms.add(platform)
            } else {
                currentPlatforms.remove(platform)
            }
            val newListString = currentPlatforms.joinToString(",")
            repository.saveConfig(config.value.copy(allowedPlatforms = newListString))
            
            // If currently selected platform got blocked, fallback
            if (!isAllowed && _selectedPlatform.value == platform) {
                _selectedPlatform.value = currentPlatforms.firstOrNull()
            }
            logActivity("CONFIG_UPDATE", "Access to platform $platform set to $isAllowed")
        }
    }

    fun toggleSafeFilters(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveConfig(config.value.copy(isSafeFiltersEnabled = enabled))
            logActivity("CONFIG_UPDATE", "Safe Filters set to: $enabled")
        }
    }

    fun updateBlockedKeywords(keywordsCommaList: String) {
        viewModelScope.launch {
            repository.saveConfig(config.value.copy(blockedKeywords = keywordsCommaList))
            logActivity("CONFIG_UPDATE", "Custom blocked keyword list updated")
        }
    }

    fun addCustomVideo(title: String, url: String, platform: String, category: String) {
        viewModelScope.launch {
            val cleanUrl = formatEmbedUrl(url, platform)
            val newVideo = VideoItem(
                title = title,
                url = cleanUrl,
                platform = platform,
                category = category,
                isCustom = true
            )
            repository.insertVideo(newVideo)
            logActivity("VIDEO_ADD", "Parent whitelisted video: $title", platform)
        }
    }

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            repository.deleteVideo(video.id)
            logActivity("VIDEO_DELETE", "Parent removed video: ${video.title}", video.platform)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun logActivity(type: String, detail: String, platform: String? = null) {
        viewModelScope.launch {
            repository.insertLog(ActivityLog(
                activityType = type,
                detail = detail,
                platform = platform
            ))
        }
    }

    // Helper functions
    private fun formatEmbedUrl(url: String, platform: String): String {
        // Formats regular links to secure embed players to bypass signin screens
        if (platform == "YouTube" && url.contains("watch?v=")) {
            val id = url.substringAfter("watch?v=").substringBefore("&")
            return "https://www.youtube.com/embed/$id"
        } else if (platform == "YouTube" && url.contains("youtu.be/")) {
            val id = url.substringAfter("youtu.be/").substringBefore("?")
            return "https://www.youtube.com/embed/$id"
        } else if (platform == "Dailymotion" && url.contains("dai.ly/")) {
            val id = url.substringAfter("dai.ly/").substringBefore("?")
            return "https://www.dailymotion.com/embed/video/$id"
        } else if (platform == "Dailymotion" && url.contains("dailymotion.com/video/")) {
            val id = url.substringAfter("dailymotion.com/video/").substringBefore("?").substringBefore("_")
            return "https://www.dailymotion.com/embed/video/$id"
        } else if (platform == "Vimeo" && !url.contains("player.vimeo.com")) {
            val id = url.substringAfterLast("/")
            return "https://player.vimeo.com/video/$id"
        }
        return url
    }

    private suspend fun seedDefaultVideos() {
        val defaultVideos = listOf(
            // --- YouTube (Videos & Shorts) ---
            VideoItem(
                title = "ہماری کائنات - بچوں کے لیے معلوماتی ویڈیو (Solar System Urdu)",
                url = "https://www.youtube.com/embed/n4p_qV8Uu5c",
                platform = "YouTube",
                category = "Education",
                duration = "6:15"
            ),
            VideoItem(
                title = "Learn Solar System and Planets for Kids",
                url = "https://www.youtube.com/embed/libKVRa01L8",
                platform = "YouTube",
                category = "Education",
                duration = "8:20"
            ),
            VideoItem(
                title = "Urdu Alphabets Song (حروفِ تہجی نظم) - Nursery Rhyme",
                url = "https://www.youtube.com/embed/qg_v03L49YI",
                platform = "YouTube",
                category = "Rhymes",
                duration = "4:30"
            ),
            VideoItem(
                title = "YouTube Shorts: 10 Fun Science Tricks in 60s",
                url = "https://player.vimeo.com/video/334237912",
                platform = "YouTube",
                category = "Science",
                duration = "0:58"
            ),
            VideoItem(
                title = "YouTube Shorts: Super Cute Animals Running in Meadow",
                url = "https://player.vimeo.com/video/547281902",
                platform = "YouTube",
                category = "Cartoon",
                duration = "0:45"
            ),
            VideoItem(
                title = "National Geographic Kids: Playful Giant Panda Bears",
                url = "https://player.vimeo.com/video/547281902",
                platform = "YouTube",
                category = "Science",
                duration = "3:45"
            ),

            // --- Facebook (Videos & Reels) ---
            VideoItem(
                title = "Facebook Science: DIY Magic Floating Ink Experiment",
                url = "https://player.vimeo.com/video/224237912",
                platform = "Facebook",
                category = "Science",
                duration = "3:50"
            ),
            VideoItem(
                title = "Facebook Watch: Learn Animals and Birds Sounds for Toddlers",
                url = "https://player.vimeo.com/video/14828114",
                platform = "Facebook",
                category = "Rhymes",
                duration = "4:00"
            ),
            VideoItem(
                title = "Facebook Reels: Funny Baby Animals Laughs & Play",
                url = "https://player.vimeo.com/video/14828114",
                platform = "Facebook",
                category = "Cartoon",
                duration = "0:45"
            ),
            VideoItem(
                title = "Facebook Reels: 60 Seconds Quick Origami Butterfly",
                url = "https://player.vimeo.com/video/110993096",
                platform = "Facebook",
                category = "Education",
                duration = "0:52"
            ),

            // --- TikTok (Reels & Short Videos) ---
            VideoItem(
                title = "TikTok Reels: Awesome Volcano Erupt Science Lesson",
                url = "https://player.vimeo.com/video/334237912",
                platform = "TikTok",
                category = "Science",
                duration = "0:50"
            ),
            VideoItem(
                title = "TikTok Reels: How Rain and Cloud Formation Works",
                url = "https://player.vimeo.com/video/110993096",
                platform = "TikTok",
                category = "Education",
                duration = "0:55"
            ),
            VideoItem(
                title = "TikTok Reels: Amazing Paper Plane That Flies Forever",
                url = "https://player.vimeo.com/video/110993096",
                platform = "TikTok",
                category = "Education",
                duration = "0:42"
            ),
            VideoItem(
                title = "TikTok Reels: Magnetic Fluid & Magic Slime Science",
                url = "https://player.vimeo.com/video/224237912",
                platform = "TikTok",
                category = "Science",
                duration = "0:58"
            ),

            // --- Instagram (Reels & Videos) ---
            VideoItem(
                title = "Instagram Reels: 🎨 Easy Finger Painting Art for Kids",
                url = "https://player.vimeo.com/video/110993096",
                platform = "Instagram",
                category = "Education",
                duration = "0:45"
            ),
            VideoItem(
                title = "Instagram Reels: 🐘 Wild Safari Animals Amazing Facts",
                url = "https://player.vimeo.com/video/547281902",
                platform = "Instagram",
                category = "Science",
                duration = "0:50"
            ),
            VideoItem(
                title = "Instagram Reels: 🚀 Space Rocket Launch Animation Short",
                url = "https://player.vimeo.com/video/334237912",
                platform = "Instagram",
                category = "Science",
                duration = "0:52"
            ),
            VideoItem(
                title = "Instagram IGTV: Piper - Award Winning Cute Bird Animation",
                url = "https://player.vimeo.com/video/547281902",
                platform = "Instagram",
                category = "Cartoon",
                duration = "3:30"
            ),

            // --- Dailymotion (Kids & Educational Videos) ---
            VideoItem(
                title = "Dailymotion Kids: The Honest Woodcutter - Classic Moral Urdu Story",
                url = "https://www.dailymotion.com/embed/video/x7tg55b",
                platform = "Dailymotion",
                category = "Education",
                duration = "5:15"
            ),
            VideoItem(
                title = "Dailymotion Kids: Five Little Ducks Nursery Rhyme Adventure",
                url = "https://www.dailymotion.com/embed/video/x8jkeec",
                platform = "Dailymotion",
                category = "Rhymes",
                duration = "3:20"
            ),
            VideoItem(
                title = "Dailymotion Shorts: Fast Math Magic Trick - Multiply in Seconds",
                url = "https://player.vimeo.com/video/334237912",
                platform = "Dailymotion",
                category = "Science",
                duration = "0:48"
            )
        )

        val currentList = repository.allVideos.first()
        val existingTitles = currentList.map { it.title }.toSet()
        for (video in defaultVideos) {
            if (!existingTitles.contains(video.title)) {
                repository.insertVideo(video)
            }
        }
        
        // Log seeding
        repository.insertLog(ActivityLog(
            activityType = "SYSTEM",
            detail = "Database updated with Facebook, YouTube, TikTok, Instagram & Dailymotion videos and reels"
        ))
    }
}
