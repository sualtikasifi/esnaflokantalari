package com.esnaflokantalari.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.esnaflokantalari.app.navigation.AppNavigation
import com.esnaflokantalari.app.ui.AppViewModel
import com.esnaflokantalari.app.ui.theme.EsnafLokantalariTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        // Veri yüklenene kadar açılış ekranı kalsın; uygulama boş liste ile
        // bir an "boşmuş" gibi görünmesin.
        var contentReady = false
        splashScreen.setKeepOnScreenCondition { !contentReady }
        lifecycleScope.launch {
            viewModel.cities.collect { cities ->
                if (cities.isNotEmpty()) contentReady = true
            }
        }

        // Açılış ekranı sertçe kaybolmasın: logo hafifçe büyüyüp solarak çıksın.
        splashScreen.setOnExitAnimationListener { provider ->
            val icon = provider.iconView
            val fade = ObjectAnimator.ofFloat(provider.view, View.ALPHA, 1f, 0f)
            val scaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.12f)
            val scaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.12f)

            AnimatorSet().apply {
                playTogether(fade, scaleX, scaleY)
                duration = 420L
                interpolator = AccelerateInterpolator()
                doOnEnd { provider.remove() }
                start()
            }
        }

        enableEdgeToEdge()
        setContent {
            EsnafLokantalariTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

private fun AnimatorSet.doOnEnd(action: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = action()
    })
}
