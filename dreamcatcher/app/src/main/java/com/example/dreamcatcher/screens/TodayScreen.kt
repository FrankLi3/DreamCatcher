package com.example.dreamcatcher.screens

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import com.example.dreamcatcher.Dream
import com.example.dreamcatcher.DreamDao
import com.example.dreamcatcher.MainViewModel
import com.example.dreamcatcher.tools.ImageGeneration
import com.example.dreamcatcher.tools.MicRecord
import com.example.dreamcatcher.R
import com.example.dreamcatcher.tools.MoodDisplay
import com.example.dreamcatcher.tools.downloadImage
import com.example.dreamcatcher.tools.fetchEmotion
import com.google.gson.Gson
import kotlinx.coroutines.launch


open class TodayViewModel(private val dreamDao: DreamDao) : ViewModel() {
    val spokenTextState = mutableStateOf("Press the microphone to speak")
    val isRecordingState = mutableStateOf(false)
    val topMoods = mutableStateOf(emptyList<Pair<String, Int>>())
    val isMoodDisplayed = mutableStateOf(false)
    val dailyImages = mutableStateOf<List<String>>(emptyList())

    fun updateSpokenText(newText: String) {
        spokenTextState.value = newText
    }

    fun setRecordingState(isRecording: Boolean) {
        isRecordingState.value = isRecording
    }

    fun saveDream(
        context: Context,
        userId: Int,
        aiImageURL: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val content = spokenTextState.value
            val userExists = dreamDao.getUserById(userId) != null

            if (content.isEmpty()) {
                onComplete(false)
                return@launch
            }

            if (!userExists) {
                onComplete(false)
                return@launch
            }

            val localImagePath = if (aiImageURL.startsWith("/data/")) {
                aiImageURL
            } else {
                // Download the image
                downloadImage(context, aiImageURL, "dream_${System.currentTimeMillis()}.jpg")
            }

            if (localImagePath == null) {
                onComplete(false)
                return@launch
            }

            try {
                val emotion = fetchEmotion(content)
                val allMood = Gson().toJson(emotion)
                val calculatedTopMoods = emotion.sortedByDescending { it.score }
                    .take(4)
                    .map { it.label to (it.score * 100).toInt() }

                topMoods.value = calculatedTopMoods
                isMoodDisplayed.value = true

                val newDream = Dream(
                    userId = userId,
                    title = "Generated Dream",
                    content = content,
                    mood = allMood,
                    aiImageURL = localImagePath
                )
                dreamDao.insertDream(newDream)
                dailyImages.value += localImagePath

                // Save dream successfully
                onComplete(true)
            } catch (e: Exception) {
                // Error saving dream
                Log.e("TodayViewModel", "Error saving dream: ${e.message}")
                onComplete(false)
            }
        }
    }

    fun resetState() {
        spokenTextState.value = "Press the microphone to speak"
        isRecordingState.value = false
        topMoods.value = emptyList()
        isMoodDisplayed.value = false
        dailyImages.value = emptyList()
    }


}


@SuppressLint("StateFlowValueCalledInComposition") // Suppress warning for using StateFlow in Composable
@Composable
fun TodayScreen(todayViewModel: TodayViewModel, mainViewModel: MainViewModel) {
    val spokenTextState = todayViewModel.spokenTextState
    val isRecordingState = todayViewModel.isRecordingState
    val topMoods = todayViewModel.topMoods
    val isMoodDisplayed = todayViewModel.isMoodDisplayed
    val generatedImageUrl = remember { mutableStateOf<String?>(null) }
    val isEditingState = remember { mutableStateOf(false) }
    val isGeneratingImage = remember { mutableStateOf(false) }
    val dailyImages = todayViewModel.dailyImages
    val context = LocalContext.current
    val loggedInUser = mainViewModel.loggedInUser.value

    LaunchedEffect(Unit) {
        todayViewModel.resetState()
    }

    val icons = listOf(
        "Paint" to R.drawable.paint,
        "Accept" to R.drawable.accept,
        "Regenerate" to R.drawable.regenerate,
        "Edit" to R.drawable.edit
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when {
                isGeneratingImage.value -> {
                    Text(
                        text = "Generating Image...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                dailyImages.value.isNotEmpty() -> {
                    Image(
                        painter = rememberAsyncImagePainter(model = dailyImages.value.last()),
                        contentDescription = "Dream Image",
                        modifier = Modifier
                            .size(360.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                else -> {
                    Text("Press Paint to generate a dream image")
                }
            }
        }

        // Middle Section
        if (isMoodDisplayed.value) {
            MoodDisplay(moods = topMoods.value)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    iconRes = icons[0].second,
                    description = "Paint",
                    label = "Paint",
                    onClick = {
                        Log.d("TodayScreen", "Paint button clicked")
                        isGeneratingImage.value = true
                    },
                    enabled = spokenTextState.value.isNotEmpty() && spokenTextState.value != "Press the microphone to speak"
                            && spokenTextState.value != "Didn't catch that. Please try again."
                )
                IconButton(
                    iconRes = icons[1].second,
                    description = "Accept",
                    label = "Accept",
                    onClick = {
                        val userId = loggedInUser?.userId ?: return@IconButton
                        val imageUrl = generatedImageUrl


                        todayViewModel.saveDream(
                            context = context,
                            userId = userId,
//                            aiImageURL = generatedImageUrl.value ?: "",
                            aiImageURL = imageUrl.value ?: "",
                            onComplete = { success ->
                                if (!success) Log.e("TodayScreen", "Error saving dream")
                            }
                        )
                    }
                )
                IconButton(
                    iconRes = icons[3].second,
                    description = "Edit",
                    label = "Edit",
                    onClick = { isEditingState.value = true }
                )
            }
        }

        // Bottom Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (isEditingState.value) {
                EditDialog(
                    spokenText = spokenTextState.value,
                    onDismiss = { isEditingState.value = false },
                    onSave = { updatedText ->
                        todayViewModel.updateSpokenText(updatedText)
                        isEditingState.value = false
                    }
                )
            } else {
                MicRecord(
                    spokenTextState = todayViewModel.spokenTextState,
                    isRecording = todayViewModel.isRecordingState
                )
            }
        }

        if (isGeneratingImage.value) {
            ImageGeneration(
                prompt = spokenTextState.value,
                onImageGenerated = { imageUrl ->
                    generatedImageUrl.value = imageUrl
                    todayViewModel.dailyImages.value += imageUrl
                    isGeneratingImage.value = false
                }
            )
        }
    }
}


@Composable
fun IconButton(
    iconRes: Int,
    description: String,
    label: String,
    size: Dp = 48.dp,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .clickable(enabled = enabled) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            modifier = Modifier.size(size),
            tint = if (enabled) Color.Unspecified else Color.Gray
        )

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun EditDialog(
    spokenText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var editText by remember { mutableStateOf(spokenText) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Edit Your Dream") },
        text = {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                label = { Text("Your Dream", color = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(editText) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Cancel")
            }
        }
    )
}


class TodayViewModelFactory(private val dreamDao: DreamDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodayViewModel(dreamDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


