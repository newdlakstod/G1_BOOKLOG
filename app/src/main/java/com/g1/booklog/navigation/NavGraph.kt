package com.g1.booklog.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalConfiguration
import com.g1.booklog.ui.theme.ThinCalendarIcon
import com.g1.booklog.ui.theme.ThinFriendsIcon
import com.g1.booklog.ui.theme.ThinHomeIcon
import com.g1.booklog.ui.theme.ThinLibraryIcon
import com.g1.booklog.ui.theme.ThinNoteIcon
import com.g1.booklog.ui.theme.ThinStatsIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.g1.booklog.ui.screens.*
import com.g1.booklog.ui.viewmodel.AuthState
import com.g1.booklog.ui.viewmodel.AuthViewModel
import com.g1.booklog.ui.viewmodel.BookViewModel
import com.g1.booklog.ui.viewmodel.FriendViewModel

sealed class Screen(val route: String) {
    object Home         : Screen("home")
    object Library      : Screen("library")
    object Calendar     : Screen("calendar")
    object Stats        : Screen("stats")
    object Note         : Screen("note")
    object BookSearch   : Screen("book_search")
    object AddBook      : Screen("add_book")
    object Profile      : Screen("profile")
    object Friends      : Screen("friends")
    object FriendLibrary : Screen("friend_library")
    object EditBook     : Screen("edit_book/{bookId}") {
        fun createRoute(bookId: Long) = "edit_book/$bookId"
    }
    object BookDetail   : Screen("book_detail/{bookId}") {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
    }
}

private val topLevelRoutes = setOf(
    Screen.Home.route,
    Screen.Library.route,
    Screen.Calendar.route,
    Screen.Stats.route,
    Screen.Note.route
)

@Composable
fun BookLogNavGraph(
    navController: NavHostController,
    viewModel: BookViewModel,
    authViewModel: AuthViewModel,
    friendViewModel: FriendViewModel,
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val authState by authViewModel.authState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    when (authState) {
        is AuthState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AuthState.Unauthenticated -> {
            LoginScreen(authViewModel = authViewModel, bookViewModel = viewModel)
        }
        is AuthState.Authenticated -> {
            val isCompact = LocalConfiguration.current.screenWidthDp < 600
            val navigate: (String) -> Unit = { route ->
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                if (!isCompact && currentRoute in topLevelRoutes) {
                    NavigationRailBar(currentRoute = currentRoute, onNavigate = navigate)
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (isCompact && currentRoute in topLevelRoutes) {
                            BottomNavBar(currentRoute = currentRoute, onNavigate = navigate)
                        }
                    }
                ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onBookClick = { navController.navigate(Screen.BookDetail.createRoute(it)) },
                                onAddBook   = { navController.navigate(Screen.BookSearch.route) },
                                darkTheme   = darkTheme,
                                onToggleTheme = onToggleTheme,
                                onProfile   = { navController.navigate(Screen.Profile.route) }
                            )
                        }

                        composable(Screen.Library.route) {
                            LibraryScreen(
                                viewModel = viewModel,
                                onBookClick = { navController.navigate(Screen.BookDetail.createRoute(it)) },
                                onAddBook   = { navController.navigate(Screen.BookSearch.route) }
                            )
                        }

                        composable(Screen.Calendar.route) {
                            CalendarScreen(viewModel = viewModel)
                        }

                        composable(Screen.Stats.route) {
                            StatsScreen(viewModel = viewModel)
                        }

                        composable(Screen.Note.route) {
                            ReadingNoteScreen(
                                viewModel = viewModel,
                                onBookClick = { navController.navigate(Screen.BookDetail.createRoute(it)) }
                            )
                        }

                        composable(Screen.BookSearch.route) {
                            BookSearchScreen(
                                viewModel = viewModel,
                                onBookSelected = { navController.popBackStack() },
                                onNavigateBack = { navController.popBackStack() },
                                onAddManually  = { navController.navigate(Screen.AddBook.route) }
                            )
                        }

                        composable(Screen.AddBook.route) {
                            AddBookScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                authViewModel  = authViewModel,
                                bookViewModel  = viewModel,
                                friendViewModel = friendViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onFriendClick  = { friend ->
                                    friendViewModel.selectFriend(friend)
                                    navController.navigate(Screen.FriendLibrary.route)
                                }
                            )
                        }

                        composable(Screen.Friends.route) {
                            FriendScreen(
                                friendViewModel = friendViewModel,
                                onFriendClick   = { friend ->
                                    friendViewModel.selectFriend(friend)
                                    navController.navigate(Screen.FriendLibrary.route)
                                },
                                onNavigateBack  = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.FriendLibrary.route) {
                            FriendLibraryScreen(
                                friendViewModel = friendViewModel,
                                bookViewModel   = viewModel,
                                onNavigateBack  = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.EditBook.route,
                            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
                            EditBookScreen(
                                bookId = bookId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.BookDetail.route,
                            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
                            BookDetailScreen(
                                bookId = bookId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onEditBook = { navController.navigate(Screen.EditBook.createRoute(it)) }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = { Icon(ThinHomeIcon, null) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Library.route,
            onClick = { onNavigate(Screen.Library.route) },
            icon = { Icon(ThinLibraryIcon, null) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Calendar.route,
            onClick = { onNavigate(Screen.Calendar.route) },
            icon = { Icon(ThinCalendarIcon, null) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Stats.route,
            onClick = { onNavigate(Screen.Stats.route) },
            icon = { Icon(ThinStatsIcon, null) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Note.route,
            onClick = { onNavigate(Screen.Note.route) },
            icon = { Icon(ThinNoteIcon, null) },
            colors = itemColors
        )
    }
}

@Composable
private fun NavigationRailBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        val itemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = { Icon(ThinHomeIcon, null) },
            colors = itemColors
        )
        NavigationRailItem(
            selected = currentRoute == Screen.Library.route,
            onClick = { onNavigate(Screen.Library.route) },
            icon = { Icon(ThinLibraryIcon, null) },
            colors = itemColors
        )
        NavigationRailItem(
            selected = currentRoute == Screen.Calendar.route,
            onClick = { onNavigate(Screen.Calendar.route) },
            icon = { Icon(ThinCalendarIcon, null) },
            colors = itemColors
        )
        NavigationRailItem(
            selected = currentRoute == Screen.Stats.route,
            onClick = { onNavigate(Screen.Stats.route) },
            icon = { Icon(ThinStatsIcon, null) },
            colors = itemColors
        )
        NavigationRailItem(
            selected = currentRoute == Screen.Note.route,
            onClick = { onNavigate(Screen.Note.route) },
            icon = { Icon(ThinNoteIcon, null) },
            colors = itemColors
        )
        Spacer(Modifier.weight(1f))
    }
}
