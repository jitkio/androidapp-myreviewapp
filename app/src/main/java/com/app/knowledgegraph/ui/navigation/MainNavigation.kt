package com.app.knowledgegraph.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.app.knowledgegraph.AppContainer
import com.app.knowledgegraph.ui.library.AddEdgeScreen
import com.app.knowledgegraph.ui.library.BulkDeleteCardsScreen
import com.app.knowledgegraph.ui.library.CardCreateScreen
import com.app.knowledgegraph.ui.library.CardDetailScreen
import com.app.knowledgegraph.ui.library.LibraryScreen
import com.app.knowledgegraph.ui.map.MapScreen
import com.app.knowledgegraph.ui.practice.PracticeHubScreen
import com.app.knowledgegraph.ui.practice.PracticeScreen
import com.app.knowledgegraph.ui.practice.QuestionBankScreen
import com.app.knowledgegraph.ui.practice.FolderDetailScreen
import com.app.knowledgegraph.ui.practice.AddQuestionsScreen
import com.app.knowledgegraph.ui.profile.ApiKeySettingsScreen
import com.app.knowledgegraph.ui.profile.MeScreen
import com.app.knowledgegraph.ui.profile.ReviewSettingsScreen
import com.app.knowledgegraph.ui.scan.BulkDeleteQuestionsScreen
import com.app.knowledgegraph.ui.scan.ImportedQuizScreen
import com.app.knowledgegraph.ui.scan.QuestionDetailScreen
import com.app.knowledgegraph.ui.scan.ScanScreen
import com.app.knowledgegraph.ui.smartscan.SmartScanScreen
import com.app.knowledgegraph.ui.today.TodayScreen
import com.app.knowledgegraph.ui.theme.*

@Composable
private fun PageWrapper(
    direction: IncomingFrom,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).pageEdgeShadow(direction)) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(container: AppContainer) {
    val appNavState = rememberAppNavState()

    Scaffold(
        bottomBar = { BottomBar(appNavState) }
    ) { innerPadding ->
        NavHost(
            navController = appNavState.navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { appNavState.incomingFrom.forwardEnter() },
            exitTransition = { appNavState.incomingFrom.forwardExit() },
            popEnterTransition = { appNavState.incomingFrom.popEnter() },
            popExitTransition = { appNavState.incomingFrom.popExit() }
        ) {
            composable(Screen.Today.route) {
                PageWrapper(appNavState.incomingFrom) {
                    TodayScreen(container = container, appNavState = appNavState)
                }
            }
            composable(Screen.Library.route) {
                PageWrapper(appNavState.incomingFrom) {
                    LibraryScreen(container = container, appNavState = appNavState)
                }
            }
            composable(Screen.Map.route) {
                PageWrapper(appNavState.incomingFrom) {
                    MapScreen(container = container, appNavState = appNavState)
                }
            }
            composable(Screen.Practice.route) {
                PageWrapper(appNavState.incomingFrom) {
                    PracticeHubScreen(
                        container = container,
                        onNavigateToMethodTraining = { appNavState.navigate(DetailRoutes.METHOD_TRAINING, IncomingFrom.BOTTOM) },
                        onNavigateToScan = { appNavState.navigate(DetailRoutes.SCAN_SCREEN, IncomingFrom.BOTTOM) },
                        onNavigateToQuiz = { count, sources, types, folderIds ->
                            appNavState.navigate(DetailRoutes.importedQuiz(count, sources, types, folderIds), IncomingFrom.BOTTOM)
                        },
                        onNavigateToQuestionDetail = { questionId ->
                            appNavState.navigate(DetailRoutes.questionDetail(questionId), IncomingFrom.BOTTOM)
                        },
                        onNavigateToBulkDelete = {
                            appNavState.navigate(DetailRoutes.BULK_DELETE_QUESTIONS, IncomingFrom.BOTTOM)
                        },
                        onNavigateToQuestionBank = {
                            appNavState.navigate(DetailRoutes.QUESTION_BANK, IncomingFrom.BOTTOM)
                        }
                    )
                }
            }
            composable(Screen.Me.route) {
                PageWrapper(appNavState.incomingFrom) {
                    MeScreen(
                        container = container,
                        onNavigateToSettings = { appNavState.navigate(DetailRoutes.REVIEW_SETTINGS, IncomingFrom.BOTTOM) },
                        onNavigateToApiKeySettings = { appNavState.navigate(DetailRoutes.API_KEY_SETTINGS, IncomingFrom.BOTTOM) }
                    )
                }
            }

            composable(DetailRoutes.CARD_CREATE) {
                PageWrapper(appNavState.incomingFrom) {
                    CardCreateScreen(container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(
                route = DetailRoutes.CARD_DETAIL,
                arguments = listOf(navArgument("cardId") { type = NavType.LongType })
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    CardDetailScreen(
                        cardId = cardId,
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() },
                        onNavigateToAddEdge = { id -> appNavState.navigate(DetailRoutes.addEdge(id), IncomingFrom.BOTTOM) },
                        onNavigateToCard = { id -> appNavState.navigate(DetailRoutes.cardDetail(id), IncomingFrom.BOTTOM) }
                    )
                }
            }

            composable(
                route = DetailRoutes.ADD_EDGE,
                arguments = listOf(navArgument("cardId") { type = NavType.LongType })
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    AddEdgeScreen(sourceCardId = cardId, container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(DetailRoutes.REVIEW_SETTINGS) {
                PageWrapper(appNavState.incomingFrom) {
                    ReviewSettingsScreen(onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(DetailRoutes.METHOD_TRAINING) {
                PageWrapper(appNavState.incomingFrom) {
                    PracticeScreen(container = container)
                }
            }

            composable(DetailRoutes.SCAN_SCREEN) {
                PageWrapper(appNavState.incomingFrom) {
                    ScanScreen(
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() },
                        onNavigateToQuiz = {
                            appNavState.navigate(DetailRoutes.importedQuiz(), IncomingFrom.BOTTOM) {
                                popUpTo(Screen.Practice.route)
                            }
                        }
                    )
                }
            }

            composable(
                route = DetailRoutes.IMPORTED_QUIZ,
                arguments = listOf(
                    navArgument("count") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("sources") { type = NavType.StringType; defaultValue = "" },
                    navArgument("types") { type = NavType.StringType; defaultValue = "" },
                    navArgument("folderIds") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val presetCount = backStackEntry.arguments?.getInt("count") ?: 0
                val presetSources = backStackEntry.arguments?.getString("sources") ?: ""
                val presetTypes = backStackEntry.arguments?.getString("types") ?: ""
                val presetFolderIds = backStackEntry.arguments?.getString("folderIds") ?: ""
                PageWrapper(appNavState.incomingFrom) {
                    ImportedQuizScreen(
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() },
                        presetCount = presetCount,
                        presetSources = presetSources,
                        presetTypes = presetTypes,
                        presetFolderIds = presetFolderIds
                    )
                }
            }

            composable(DetailRoutes.API_KEY_SETTINGS) {
                PageWrapper(appNavState.incomingFrom) {
                    ApiKeySettingsScreen(
                        settingsDataStore = container.settingsDataStore,
                        onNavigateBack = { appNavState.popBackStack() }
                    )
                }
            }

            composable(
                route = DetailRoutes.SMART_SCAN,
                arguments = listOf(navArgument("subject") { type = NavType.StringType })
            ) { backStackEntry ->
                val subject = backStackEntry.arguments?.getString("subject") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    SmartScanScreen(
                        container = container,
                        subject = subject,
                        onNavigateBack = { appNavState.popBackStack() }
                    )
                }
            }

            composable(
                route = DetailRoutes.QUESTION_DETAIL,
                arguments = listOf(navArgument("questionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val questionId = backStackEntry.arguments?.getLong("questionId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    QuestionDetailScreen(
                        questionId = questionId,
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() }
                    )
                }
            }

            composable(DetailRoutes.BULK_DELETE_CARDS) {
                PageWrapper(appNavState.incomingFrom) {
                    BulkDeleteCardsScreen(
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() }
                    )
                }
            }

            composable(DetailRoutes.BULK_DELETE_QUESTIONS) {
                PageWrapper(appNavState.incomingFrom) {
                    BulkDeleteQuestionsScreen(
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() }
                    )
                }
            }

            composable(DetailRoutes.QUESTION_BANK) {
                PageWrapper(appNavState.incomingFrom) {
                    QuestionBankScreen(
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() },
                        onNavigateToFolder = { folderId ->
                            appNavState.navigate(DetailRoutes.folderDetail(folderId), IncomingFrom.BOTTOM)
                        }
                    )
                }
            }

            composable(
                route = DetailRoutes.FOLDER_DETAIL,
                arguments = listOf(navArgument("folderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    FolderDetailScreen(
                        folderId = folderId,
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() },
                        onNavigateToAddQuestions = { id ->
                            appNavState.navigate(DetailRoutes.addQuestionsToFolder(id), IncomingFrom.BOTTOM)
                        }
                    )
                }
            }

            composable(
                route = DetailRoutes.ADD_QUESTIONS_TO_FOLDER,
                arguments = listOf(navArgument("folderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    AddQuestionsScreen(
                        folderId = folderId,
                        container = container,
                        onNavigateBack = { appNavState.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomBar(appNavState: AppNavState) {
    val navBackStackEntry by appNavState.navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = Screen.bottomTabs.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    if (showBottomBar) {
        val currentIndex = Screen.bottomTabs.indexOfFirst { screen ->
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        }

        NavigationBar(
            containerColor = BgCard,
            contentColor = TextSecondary
        ) {
            Screen.bottomTabs.forEachIndexed { index, screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                // Tab切换动画 - 图标弹跳效果
                val scale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (selected) 1.15f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                    ),
                    label = "tabIconScale"
                )

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        appNavState.navigateTab(screen.route, currentIndex, index)
                    },
                    icon = {
                        Icon(
                            screen.icon,
                            screen.title,
                            modifier = androidx.compose.ui.Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    },
                    label = { Text(screen.title, style = MaterialTheme.typography.bodySmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = Primary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}
