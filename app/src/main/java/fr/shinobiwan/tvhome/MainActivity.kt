package fr.shinobiwan.tvhome

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import fr.shinobiwan.tvhome.ui.HomeScreen

class MainActivity:ComponentActivity() {
    private val model:HomeViewModel by viewModels()
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { val state by model.state.collectAsStateWithLifecycle();HomeScreen(state,model) }
    }
    override fun onResume(){super.onResume();model.resume()}
    override fun onPause(){model.pause();super.onPause()}
}
