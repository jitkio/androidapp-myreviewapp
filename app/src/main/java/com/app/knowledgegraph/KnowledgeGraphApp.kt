package com.app.knowledgegraph

import android.app.Application
import com.app.knowledgegraph.data.db.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KnowledgeGraphApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        applicationScope.launch {
            DatabaseSeeder.seedIfEmpty(
                container.cardRepository,
                container.graphRepository,
                container.questionBankRepository,
                container.settingsDataStore
            )
        }
    }
}
