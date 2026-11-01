package com.giuli.tejido

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.giuli.tejido.ui.home.HomeScreen
import com.giuli.tejido.ui.inventory.InventoryScreen
import com.giuli.tejido.ui.project.ProjectScreen
import com.giuli.tejido.ui.theme.GiuliTheme
import com.giuli.tejido.ui.update.UpdateScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GiuliTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onOpenProject = { id -> nav.navigate("project/$id") },
                                onOpenUpdates = { nav.navigate("updates") },
                                onOpenInventory = { nav.navigate("inventory") },
                            )
                        }
                        composable(
                            route = "project/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
                        ) {
                            ProjectScreen(onBack = { nav.popBackStack() })
                        }
                        composable("inventory") {
                            InventoryScreen(onBack = { nav.popBackStack() })
                        }
                        composable("updates") {
                            UpdateScreen(onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
