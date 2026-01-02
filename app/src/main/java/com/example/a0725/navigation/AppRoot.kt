package com.example.a0725.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.a0725.R
import com.example.a0725.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val currentRoute = currentRoute(nav)

    Scaffold(

        bottomBar = {
            if (currentRoute != Routes.LOGIN && currentRoute != Routes.SIGN) {

                // 🔸 底部 bar：白底 + 上方淡灰線 + 一點陰影
                Surface(
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp // 陰影強度，覺得不夠可以再調高
                ) {
                    Column {
                        // 和內容區分的一條淡灰線
                        Divider(
                            color = Color(0xFFE0E0E0),
                            thickness = 0.8.dp
                        )

                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            val selectedColor = Color(0xFFFFA000) // 橘色
                            val unselectedColor = Color.Black

                            NavigationBarItem(
                                selected = currentRoute == Routes.HOME,
                                onClick = { nav.navigateSingleTopTo(Routes.HOME) },
                                icon = { Icon(painterResource(R.drawable.home), null) },
                                label = { Text("首頁") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = selectedColor,
                                    selectedTextColor = selectedColor,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor,
                                    indicatorColor = Color.Transparent
                                )
                            )
                            NavigationBarItem(
                                selected = currentRoute == Routes.ANALYSIS,
                                onClick = { nav.navigateSingleTopTo(Routes.ANALYSIS) },
                                icon = { Icon(painterResource(R.drawable.analysis), null) },
                                label = { Text("分析") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = selectedColor,
                                    selectedTextColor = selectedColor,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor,
                                    indicatorColor = Color.Transparent
                                )
                            )
                            NavigationBarItem(
                                selected = currentRoute == Routes.OTHER,
                                onClick = { nav.navigateSingleTopTo(Routes.OTHER) },
                                icon = { Icon(painterResource(R.drawable.other), null) },
                                label = { Text("其他") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = selectedColor,
                                    selectedTextColor = selectedColor,
                                    unselectedIconColor = unselectedColor,
                                    unselectedTextColor = unselectedColor,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.HOME)     { MainScreen(nav) }
            composable(Routes.ANALYSIS) { AnalysisScreen(nav) }
            composable(Routes.PERSONAL) { PersonalDataScreen(nav) }
            composable(Routes.RECORD)   { RecordScreen(nav) }
            composable(Routes.SCHEDULE) { ScheduleScreen(nav) }
            composable(Routes.LOGIN)    { LoginScreen(nav) }
            composable(Routes.SEARCH)   { SerchScreen(nav) }
            composable(Routes.SIGN)     { SignScreen(nav) }
            composable(Routes.OTHER)    { OtherScreen(nav) }
            composable(Routes.ABOUT_DEVS)   { AboutDevsScreen(nav) }
            composable(Routes.DATA_SOURCES) { DataSourcesScreen(nav) }
            composable(Routes.FEEDBACK)     { FeedbackScreen(nav) }
            composable(Routes.FAQ)          { FAQScreen(nav) }
            composable(Routes.TERMS)        { TermsScreen(nav) }
            composable(Routes.CONTACT)      { ContactScreen(nav) }
            composable(Routes.PRIVACY_POLICY) { PrivacyPolicyScreen(nav) } // 新增這行

            composable(Routes.SKELETON) { SkeletonScreen(nav) }
            composable(Routes.GAIT) { GaitScreen(nav) }
            composable(Routes.TOOL) { ToolScreen(nav) }
            composable(Routes.SKELETON) { SkeletonScreen(nav) }
            composable(Routes.GAIT) { GaitScreen(nav) }
            composable(Routes.TOOL) { ToolScreen(nav) }
        }
    }
}

fun NavController.navigateSingleTopTo(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun currentRoute(nav: NavController): String? =
    nav.currentBackStackEntryAsState().value?.destination?.route
