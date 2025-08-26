package com.locationsharing.app

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class FFApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.LOGGING_ENABLED) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize EmojiCompat off the main thread to prevent ANR
        initializeEmojiCompat()
        
        Timber.d("FFinder Application started")
    }
    
    private fun initializeEmojiCompat() {
        applicationScope.launch {
            try {
                val config = BundledEmojiCompatConfig(this@FFApplication)
                    .setReplaceAll(false)
                    .setUseEmojiAsDefaultStyle(false)
                
                EmojiCompat.init(config)
                Timber.d("EmojiCompat initialized successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize EmojiCompat")
            }
        }
    }
}
