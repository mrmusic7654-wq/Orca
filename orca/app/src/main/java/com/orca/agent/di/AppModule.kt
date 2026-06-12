package com.orca.agent.di
import android.content.Context
import com.orca.agent.core.*
import com.orca.agent.data.database.OrcaDatabase
import com.orca.agent.data.network.GeminiApi
import com.orca.agent.execution.*
import com.orca.agent.brain.*
import com.orca.agent.memory.*
import com.orca.agent.agent.*
import com.orca.agent.security.SecurityManager
import com.orca.agent.voice.OrcaVoiceEngine
import com.orca.agent.data.datastore.PreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun provideContext(@ApplicationContext c: Context): Context = c
    @Provides @Singleton fun provideDb(@ApplicationContext c: Context) = OrcaDatabase.getInstance(c)
    @Provides @Singleton fun provideGemini() = GeminiApi()
    @Provides @Singleton fun provideThreatDetector() = ThreatDetector()
    @Provides @Singleton fun provideProceduralGraph() = ProceduralGraph()
    @Provides @Singleton fun provideSkillForge(pg: ProceduralGraph) = SkillForge(pg)
    @Provides @Singleton fun provideDeepSave(api: GeminiApi, db: OrcaDatabase) = DeepSave(api, db)
    @Provides @Singleton fun provideSemanticLake(api: GeminiApi) = SemanticLake(api)
    @Provides @Singleton fun provideEpisodicJournal(db: OrcaDatabase) = EpisodicJournal(db)
    @Provides @Singleton fun provideMemoryStream(db: OrcaDatabase, ds: DeepSave) = MemoryStream(db, ds)
    @Provides @Singleton fun provideMemoryCortex(ms: MemoryStream, sl: SemanticLake, pg: ProceduralGraph, ej: EpisodicJournal, db: OrcaDatabase) = MemoryCortex(ms, sl, pg, ej, db)
    @Provides @Singleton fun provideIntentPredictor(mc: MemoryCortex) = IntentPredictor(mc)
    @Provides @Singleton fun provideConsciousMind(api: GeminiApi, mc: MemoryCortex) = ConsciousMind(api, mc)
    @Provides @Singleton fun provideSubconscious(ip: IntentPredictor, td: ThreatDetector, mc: MemoryCortex) = SubconsciousEngine(ip, td, mc)
    @Provides @Singleton fun provideTouchController() = TouchController()
    @Provides @Singleton fun provideGestureEngine(tc: TouchController) = GestureEngine(tc)
    @Provides @Singleton fun provideScreenParser(api: GeminiApi) = ScreenParser(api)
    @Provides @Singleton fun provideAppNavigator(@ApplicationContext c: Context, ge: GestureEngine, sp: ScreenParser) = AppNavigator(c, ge, sp)
    @Provides @Singleton fun provideTaskExecutor(ge: GestureEngine, an: AppNavigator, sp: ScreenParser, cm: ConsciousMind, mc: MemoryCortex) = TaskExecutor(ge, an, sp, cm, mc)
    @Provides @Singleton fun provideSilentTask(@ApplicationContext c: Context, oc: OrcaCore) = SilentTask(c, oc)
    @Provides @Singleton fun provideAutoReply(oc: OrcaCore, ip: IntentPredictor) = AutoReply(oc, ip)
    @Provides @Singleton fun provideCrossAppWorkflow(an: AppNavigator, ge: GestureEngine, oc: OrcaCore) = CrossAppWorkflow(an, ge, oc)
    @Provides @Singleton fun provideDigitalTwin(mc: MemoryCortex) = DigitalTwin(mc)
    @Provides @Singleton fun provideSecurity(@ApplicationContext c: Context) = SecurityManager(c)
    @Provides @Singleton fun provideVoice(@ApplicationContext c: Context) = OrcaVoiceEngine(c)
    @Provides @Singleton fun providePrefs(@ApplicationContext c: Context) = PreferencesDataStore(c)
    @Provides @Singleton fun provideOrcaCore(cm: ConsciousMind, se: SubconsciousEngine, mc: MemoryCortex, te: TaskExecutor, dt: DigitalTwin, db: OrcaDatabase) = OrcaCore(cm, se, mc, te, dt, db)
}
