package com.example.dealoptimizer

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dealoptimizer.presentation.ui.BottomNavigationBar
import com.example.dealoptimizer.presentation.ui.CalculatorScreen
import com.example.dealoptimizer.presentation.ui.CouponScreen
import com.example.dealoptimizer.presentation.ui.DealOptimizerTheme
import com.example.dealoptimizer.presentation.ui.ProductScreen
import com.example.dealoptimizer.presentation.ui.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DealOptimizerTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Product.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Screen.Product.route) {
                ProductScreen()
            }
            composable(Screen.Coupon.route) {
                CouponScreen()
            }
            composable(Screen.Calculator.route) {
                CalculatorScreen()
            }
        }
    }
}
