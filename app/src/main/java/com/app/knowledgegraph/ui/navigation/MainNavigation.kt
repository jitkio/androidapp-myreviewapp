package com.app.knowledgegraph.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.app.knowledgegraph.ui.profile.SubjectManageScreen
import com.app.knowledgegraph.ui.scan.BulkDeleteQuestionsScreen
import com.app.knowledgegraph.ui.scan.ImportedQuizScreen
import com.app.knowledgegraph.ui.scan.QuestionDetailScreen
import com.app.knowledgegraph.ui.scan.ScanScreen
import com.app.knowledgegraph.ui.smartscan.SmartScanScreen
import com.app.knowledgegraph.ui.today.TodayScreen
import com.app.knowledgegraph.ui.theme.*

@Composable
private fun PageWrapper(direction: IncomingFrom, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).pageEdgeShadow(direction)) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(container: AppContainer) {
    val appNavState = rememberAppNavState()

    Scaffold(bottomBar = { BottomBar(appNavState) }) { innerPadding ->
        NavHost(
            navController = appNavState.navController,
            startDestination = "tabs",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { appNavState.incomingFrom.forwardEnter() },
            exitTransition = { appNavState.incomingFrom.forwardExit() },
            popEnterTransition = { appNavState.incomingFrom.popEnter() },
            popExitTransition = { appNavState.incomingFrom.popExit() }
        ) {
            composable("tabs") {
                HorizontalPager(
                    state = appNavState.pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        when (page) {
                            0 -> TodayScreen(container = container, appNavState = appNavState)
                            1 -> LibraryScreen(container = container, appNavState = appNavState)
                            2 -> PracticeHubScreen(
                                container = container,
                                onNavigateToMethodTraining = { appNavState.navigate(DetailRoutes.METHOD_TRAINING, IncomingFrom.BOTTOM) },
                                onNavigateToScan = { appNavState.navigate(DetailRoutes.SCAN_SCREEN, IncomingFrom.BOTTOM) },
                                onNavigateToQuiz = { count, sources, types, folderIds ->
                                    appNavState.navigate(DetailRoutes.importedQuiz(count, sources, types, folderIds), IncomingFrom.BOTTOM)
                                },
                                onNavigateToQuestionDetail = { questionId ->
                                    appNavState.navigate(DetailRoutes.questionDetail(questionId), IncomingFrom.BOTTOM)
                                },
                                onNavigateToBulkDelete = { appNavState.navigate(DetailRoutes.BULK_DELETE_QUESTIONS, IncomingFrom.BOTTOM) },
                                onNavigateToQuestionBank = { appNavState.navigate(DetailRoutes.QUESTION_BANK, IncomingFrom.BOTTOM) }
                            )
                            3 -> MeScreen(
                                container = container,
                                onNavigateToSettings = { appNavState.navigate(DetailRoutes.REVIEW_SETTINGS, IncomingFrom.BOTTOM) },
                                onNavigateToApiKeySettings = { appNavState.navigate(DetailRoutes.API_KEY_SETTINGS, IncomingFrom.BOTTOM) },
                                onNavigateToSubjectManage = { appNavState.navigate(DetailRoutes.SUBJECT_MANAGE, IncomingFrom.BOTTOM) }
                            )
                        }
                    }
                }
            }

            composable(DetailRoutes.MAP_SCREEN) {
                PageWrapper(appNavState.incomingFrom) {
                    MapScreen(container = container, appNavState = appNavState, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            // ★ SmartScan — 不再需要 subject 参数
            composable(DetailRoutes.SMART_SCAN) {
                PageWrapper(appNavState.incomingFrom) {
                    SmartScanScreen(container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            // ★ 学科管理
            composable(DetailRoutes.SUBJECT_MANAGE) {
                PageWrapper(appNavState.incomingFrom) {
                    SubjectManageScreen(
                        settingsDataStore = container.settingsDataStore,
                        onNavigateBack = { appNavState.popBackStack() }
                    )
                }
            }

            composable(DetailRoutes.CARD_CREATE) {
                PageWrapper(appNavState.incomingFrom) {
                    CardCreateScreen(container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(DetailRoutes.CARD_DETAIL, arguments = listOf(navArgument("cardId") { type = NavType.LongType })) { entry ->
                val cardId = entry.arguments?.getLong("cardId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    CardDetailScreen(cardId = cardId, container = container,
                        onNavigateBack = { appNavState.popBackStack() },
                        onNavigateToAddEdge = { id -> appNavState.navigate(DetailRoutes.addEdge(id), IncomingFrom.BOTTOM) },
                        onNavigateToCard = { id -> appNavState.navigate(DetailRoutes.cardDetail(id), IncomingFrom.BOTTOM) })
                }
            }

            composable(DetailRoutes.ADD_EDGE, arguments = listOf(navArgument("cardId") { type = NavType.LongType })) { entry ->
                val cardId = entry.arguments?.getLong("cardId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    AddEdgeScreen(sourceCardId = cardId, container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(DetailRoutes.REVIEW_SETTINGS) {
                PageWrapper(appNavState.incomingFrom) { ReviewSettingsScreen(onNavigateBack = { appNavState.popBackStack() }) }
            }

            composable(DetailRoutes.METHOD_TRAINING) {
                PageWrapper(appNavState.incomingFrom) { PracticeScreen(container = container) }
            }

            composable(DetailRoutes.SCAN_SCREEN) {
                PageWrapper(appNavState.incomingFrom) {
                    ScanScreen(container = container, onNavigateBack = { appNavState.popBackStack() },
                        onNavigateToQuiz = { appNavState.navigate(DetailRoutes.importedQuiz(), IncomingFrom.BOTTOM) { popUpTo("tabs") } })
                }
            }

            composable(DetailRoutes.IMPORTED_QUIZ, arguments = listOf(
                navArgument("count") { type = NavType.IntType; defaultValue = 0 },
                navArgument("sources") { type = NavType.StringType; defaultValue = "" },
                navArgument("types") { type = NavType.StringType; defaultValue = "" },
                navArgument("folderIds") { type = NavType.StringType; defaultValue = "" }
            )) { entry ->
                PageWrapper(appNavState.incomingFrom) {
                    ImportedQuizScreen(container = container, onNavigateBack = { appNavState.popBackStack() },
                        presetCount = entry.arguments?.getInt("count") ?: 0,
                        presetSources = entry.arguments?.getString("sources") ?: "",
                        presetTypes = entry.arguments?.getString("types") ?: "",
                        presetFolderIds = entry.arguments?.getString("folderIds") ?: "")
                }
            }

            composable(DetailRoutes.API_KEY_SETTINGS) {
                PageWrapper(appNavState.incomingFrom) {
                    ApiKeySettingsScreen(settingsDataStore = container.settingsDataStore, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(DetailRoutes.QUESTION_DETAIL, arguments = listOf(navArgument("questionId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("questionId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    QuestionDetailScreen(questionId = id, container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(DetailRoutes.BULK_DELETE_CARDS) {
                PageWrapper(appNavState.incomingFrom) {
                    BulkDeleteCardsScreen(container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(DetailRoutes.BULK_DELETE_QUESTIONS) {
                PageWrapper(appNavState.incomingFrom) {
                    BulkDeleteQuestionsScreen(container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }

            composable(DetailRoutes.QUESTION_BANK) {
                PageWrapper(appNavState.incomingFrom) {
                    QuestionBankScreen(container = container, onNavigateBack = { appNavState.popBackStack() },
                        onNavigateToFolder = { folderId -> appNavState.navigate(DetailRoutes.folderDetail(folderId), IncomingFrom.BOTTOM) })
                }
            }

            composable(DetailRoutes.FOLDER_DETAIL, arguments = listOf(navArgument("folderId") { type = NavType.LongType })) { entry ->
                val folderId = entry.arguments?.getLong("folderId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    FolderDetailScreen(folderId = folderId, container = container, onNavigateBack = { appNavState.popBackStack() },
                        onNavigateToAddQuestions = { id -> appNavState.navigate(DetailRoutes.addQuestionsToFolder(id), IncomingFrom.BOTTOM) })
                }
            }

            composable(DetailRoutes.ADD_QUESTIONS_TO_FOLDER, arguments = listOf(navArgument("folderId") { type = NavType.LongType })) { entry ->
                val folderId = entry.arguments?.getLong("folderId") ?: return@composable
                PageWrapper(appNavState.incomingFrom) {
                    AddQuestionsScreen(folderId = folderId, container = container, onNavigateBack = { appNavState.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun BottomBar(appNavState: AppNavState) {
    val navBackStackEntry by appNavState.navController.currentBackStackEntryAsState()
    if (navBackStackEntry?.destination?.route != "tabs") return

    val currentTabIndex = appNavState.pagerState.currentPage

    NavigationBar(containerColor = BgCard, contentColor = TextSecondary) {
        Screen.bottomTabs.forEachIndexed { index, screen ->
            val selected = currentTabIndex == index
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "tabIconScale"
            )
            NavigationBarItem(
                selected = selected,
                onClick = { appNavState.navigateTab(index) },
                icon = { Icon(screen.icon, screen.title, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) },
                label = { Text(screen.title, style = MaterialTheme.typography.bodySmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary, selectedTextColor = Primary,
                    unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary,
                    indicatorColor = Primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
