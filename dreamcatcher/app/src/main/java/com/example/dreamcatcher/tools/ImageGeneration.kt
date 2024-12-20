package com.example.dreamcatcher.tools

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.dreamcatcher.network.RetrofitInstance
import com.example.dreamcatcher.network.ImageRequest
import kotlinx.coroutines.launch


@Composable
fun ImageGeneration(prompt: String, onImageGenerated: (String) -> Unit) {
    val context = LocalContext.current
    val isLoading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(prompt) {
        if (prompt.isNotEmpty()) {
            isLoading.value = true
            scope.launch {
                try {
                    // Call the OpenAI API to generate an image based on the prompt
                    val response = RetrofitInstance.openAIImageAPI.generateImage(
                        ImageRequest(prompt = prompt, n = 1, size = "512x512")
                    )
                    if (response.data.isNotEmpty()) {
                        val remoteImageUrl = response.data[0].url
                        val localImagePath = downloadImage(
                            context = context,
                            imageUrl = remoteImageUrl,
                            fileName = "dream_${System.currentTimeMillis()}.png"
                        )

                        if (localImagePath != null) {
                            onImageGenerated(localImagePath)
                        } else {
                            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(context, "No image generated", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading.value = false
                }
            }
        }
    }
}
