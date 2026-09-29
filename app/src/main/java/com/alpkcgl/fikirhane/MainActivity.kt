package com.alpkcgl.fikirhane

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alpkcgl.fikirhane.ui.navigation.AppNavHost
import com.alpkcgl.fikirhane.ui.theme.FikirhaneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FikirhaneApp).container
        setContent {
            FikirhaneTheme {
                AppNavHost(authRepository = container.authRepository)
            }
        }
    }
}
