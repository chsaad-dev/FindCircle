package com.example.findcircle.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.compose.ui.unit.dp
import com.example.findcircle.ui.addpost.AddPostScreen
import com.example.findcircle.ui.auth.LoginScreen
import com.example.findcircle.ui.auth.RegisterScreen
import com.example.findcircle.ui.home.HomeScreen
import com.example.findcircle.ui.map.MapScreen
import com.example.findcircle.ui.chat.ChatListScreen
import com.example.findcircle.ui.chat.ChatMessageScreen
import com.example.findcircle.ui.postdetail.PostDetailScreen
import com.example.findcircle.ui.profile.ProfileScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String, val title: String? = null, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    
    // Bottom Nav items
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Map : Screen("map", "Map", Icons.Default.LocationOn)
    object AddPost : Screen("add_post", "Add", Icons.Default.AddCircle)
    object Messages : Screen("messages", "Messages", Icons.Default.Email)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    
    // Screens with arguments
    object Chat : Screen("chat/{chatId}/{otherUserName}") {
        fun createRoute(chatId: String, otherUserName: String) = "chat/$chatId/$otherUserName"
    }
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    
    object History : Screen("history")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Map,
    Screen.AddPost,
    Screen.Messages,
    Screen.Profile
)

@Composable
fun FindCircleNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier,
        enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) },
        popEnterTransition = { fadeIn(animationSpec = tween(400)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) }
    ) {
        composable(Screen.Splash.route) {
            com.example.findcircle.ui.auth.SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { 
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { 
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit = {}) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if bottom bar should be shown
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Map.route,
        Screen.AddPost.route,
        Screen.Messages.route,
        Screen.Profile.route
    )
    
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val currentDestination = navBackStackEntry?.destination
                    
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title!!) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    onNavigateToPostDetail = { postId ->
                        navController.navigate(Screen.PostDetail.createRoute(postId))
                    }
                ) 
            }
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.AddPost.route) { 
                AddPostScreen(
                    onPostSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                ) 
            }
            composable(
                route = Screen.PostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                PostDetailScreen(
                    postId = postId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Messages.route) { 
                ChatListScreen(
                    onChatClick = { chatId, otherUserName ->
                        navController.navigate(Screen.Chat.createRoute(chatId, otherUserName))
                    }
                ) 
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("otherUserName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
                
                ChatMessageScreen(
                    chatId = chatId,
                    otherUserName = otherUserName,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) { 
                ProfileScreen(
                    onLogout = onLogout,
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                ) 
            }
            composable(Screen.History.route) { 
                com.example.findcircle.ui.history.HistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
        }
    }
}
