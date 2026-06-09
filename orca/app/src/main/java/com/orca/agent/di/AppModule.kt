// app/src/main/java/com/orca/agent/di/AppModule.kt - REPLACE WITH REAL CONTENT
package com.orca.agent.di

import android.content.Context
import com.orca.agent.OrcaApplication
import com.orca.agent.core.OrcaCore
import com.orca.agent.data.database.OrcaDatabase
import com.orca.agent.data.network.GeminiApi
import com.orca.agent.execution.*
import com.orca.agent.brain.*
import com.orca.agent.memory.*
import com.orca.agent.agent.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OrcaScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideApplication(): OrcaApplication = OrcaApplication.instance
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
    
    @Provides
    @Singleton
    fun provideOrcaDatabase(@ApplicationContext context: Context): OrcaDatabase {
        return OrcaDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideGeminiApi(): GeminiApi = GeminiApi()
    
    @Provides
    @Singleton
    @OrcaScope
    fun provideOrcaCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    
    @Provides
    @Singleton
    @MainScope
    fun provideMainCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
    
    // Brain Module
    @Provides
    @Singleton
    fun provideConsciousMind(geminiApi: GeminiApi, memoryCortex: MemoryCortex): ConsciousMind {
        return ConsciousMind(geminiApi, memoryCortex)
    }
    
    @Provides
    @Singleton
    fun provideSubconsciousEngine(
        intentPredictor: IntentPredictor,
        threatDetector: ThreatDetector,
        memoryCortex: MemoryCortex
    ): SubconsciousEngine {
        return SubconsciousEngine(intentPredictor, threatDetector, memoryCortex)
    }
    
    @Provides
    @Singleton
    fun provideIntentPredictor(memoryCortex: MemoryCortex): IntentPredictor {
        return IntentPredictor(memoryCortex)
    }
    
    @Provides
    @Singleton
    fun provideThreatDetector(): ThreatDetector = ThreatDetector()
    
    @Provides
    @Singleton
    fun provideSkillForge(proceduralGraph: ProceduralGraph): SkillForge {
        return SkillForge(proceduralGraph)
    }
    
    // Memory Module
    @Provides
    @Singleton
    fun provideMemoryCortex(
        database: OrcaDatabase,
        semanticLake: SemanticLake,
        proceduralGraph: ProceduralGraph,
        episodicJournal: EpisodicJournal
    ): MemoryCortex {
        val memoryStream = MemoryStream(database, DeepSave(database, provideGeminiApi()))
        return MemoryCortex(memoryStream, semanticLake, proceduralGraph, episodicJournal, database)
    }
    
    @Provides
    @Singleton
    fun provideMemoryStream(database: OrcaDatabase, deepSave: DeepSave): MemoryStream {
        return MemoryStream(database, deepSave)
    }
    
    @Provides
    @Singleton
    fun provideSemanticLake(geminiApi: GeminiApi): SemanticLake {
        return SemanticLake(geminiApi)
    }
    
    @Provides
    @Singleton
    fun provideProceduralGraph(): ProceduralGraph = ProceduralGraph()
    
    @Provides
    @Singleton
    fun provideEpisodicJournal(database: OrcaDatabase): EpisodicJournal {
        return EpisodicJournal(database)
    }
    
    @Provides
    @Singleton
    fun provideDeepSave(geminiApi: GeminiApi, database: OrcaDatabase): DeepSave {
        return DeepSave(geminiApi, database)
    }
    
    // Execution Module
    @Provides
    @Singleton
    fun provideTouchController(): TouchController = TouchController()
    
    @Provides
    @Singleton
    fun provideGestureEngine(touchController: TouchController): GestureEngine {
        return GestureEngine(touchController)
    }
    
    @Provides
    @Singleton
    fun provideScreenParser(geminiApi: GeminiApi): ScreenParser {
        return ScreenParser(geminiApi)
    }
    
    @Provides
    @Singleton
    fun provideAppNavigator(
        @ApplicationContext context: Context,
        gestureEngine: GestureEngine,
        screenParser: ScreenParser
    ): AppNavigator {
        return AppNavigator(context, gestureEngine, screenParser)
    }
    
    // Agent Module
    @Provides
    @Singleton
    fun provideTaskExecutor(
        gestureEngine: GestureEngine,
        appNavigator: AppNavigator,
        screenParser: ScreenParser,
        consciousMind: ConsciousMind,
        memoryCortex: MemoryCortex
    ): TaskExecutor {
        return TaskExecutor(gestureEngine, appNavigator, screenParser, consciousMind, memoryCortex)
    }
    
    @Provides
    @Singleton
    fun provideSilentTask(
        @ApplicationContext context: Context,
        orcaCore: OrcaCore
    ): SilentTask {
        return SilentTask(context, orcaCore)
    }
    
    @Provides
    @Singleton
    fun provideAutoReply(
        orcaCore: OrcaCore,
        intentPredictor: IntentPredictor
    ): AutoReply {
        return AutoReply(orcaCore, intentPredictor)
    }
    
    @Provides
    @Singleton
    fun provideCrossAppWorkflow(
        appNavigator: AppNavigator,
        gestureEngine: GestureEngine,
        orcaCore: OrcaCore
    ): CrossAppWorkflow {
        return CrossAppWorkflow(appNavigator, gestureEngine, orcaCore)
    }
    
    @Provides
    @Singleton
    fun provideDigitalTwin(memoryCortex: MemoryCortex): DigitalTwin {
        return DigitalTwin(memoryCortex)
    }
    
    // Core
    @Provides
    @Singleton
    fun provideOrcaCore(
        consciousMind: ConsciousMind,
        subconsciousEngine: SubconsciousEngine,
        memoryCortex: MemoryCortex,
        taskExecutor: TaskExecutor,
        digitalTwin: DigitalTwin,
        database: OrcaDatabase
    ): OrcaCore {
        return OrcaCore(
            consciousMind,
            subconsciousEngine,
            memoryCortex,
            taskExecutor,
            digitalTwin,
            database
        )
    }
}
