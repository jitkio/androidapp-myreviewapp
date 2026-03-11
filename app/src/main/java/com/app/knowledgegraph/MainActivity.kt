package com.app.knowledgegraph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.app.knowledgegraph.ui.navigation.MainNavigation
import com.app.knowledgegraph.ui.theme.KnowledgeGraphTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as KnowledgeGraphApp).container

        setContent {
            KnowledgeGraphTheme {
                MainNavigation(container = container)
            }
        }
    }
}
