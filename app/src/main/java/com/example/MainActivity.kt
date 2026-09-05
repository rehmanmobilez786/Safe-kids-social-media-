package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.KidHomeScreen
import com.example.ui.ParentPanelScreen
import com.example.ui.SafeVideoPlayer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SafeKidViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: SafeKidViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isUrdu by viewModel.isUrdu.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifier = Modifier.fillMaxSize()
                    when (val screen = currentScreen) {
                        is Screen.KidHome -> {
                            KidHomeScreen(
                                viewModel = viewModel,
                                isUrdu = isUrdu,
                                onNavigateToParentUnlock = {
                                    viewModel.navigateTo(Screen.ParentUnlock)
                                }
                            )
                        }
                        is Screen.VideoPlayer -> {
                            val allVideos by viewModel.allVideos.collectAsState()
                            val likedVideoIds by viewModel.likedVideoIds.collectAsState()
                            val savedVideoIds by viewModel.savedVideoIds.collectAsState()
                            val followedChannels by viewModel.followedChannels.collectAsState()
                            val kidComments by viewModel.kidComments.collectAsState()

                            SafeVideoPlayer(
                                video = screen.video,
                                allVideos = allVideos,
                                isUrdu = isUrdu,
                                onBack = {
                                    viewModel.navigateBack()
                                },
                                onSelectNextVideo = { nextVideo ->
                                    viewModel.selectVideo(nextVideo)
                                },
                                isLiked = likedVideoIds.contains(screen.video.id),
                                onToggleLike = { viewModel.toggleLike(screen.video.id) },
                                isSaved = savedVideoIds.contains(screen.video.id),
                                onToggleSave = { viewModel.toggleSave(screen.video.id) },
                                followedChannels = followedChannels,
                                onToggleFollowChannel = { channel -> viewModel.toggleFollowChannel(channel) },
                                comments = kidComments[screen.video.id] ?: emptyList(),
                                onAddComment = { comment -> viewModel.addKidComment(screen.video.id, comment) }
                            )
                        }
                        is Screen.ParentUnlock, is Screen.ParentDashboard -> {
                            ParentPanelScreen(
                                viewModel = viewModel,
                                isUrdu = isUrdu,
                                onBack = {
                                    viewModel.navigateBack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
