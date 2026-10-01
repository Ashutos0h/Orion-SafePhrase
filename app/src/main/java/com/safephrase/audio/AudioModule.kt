package com.safephrase.audio

import com.safephrase.audio.KeywordDetector
import com.safephrase.audio.HeuristicKeywordDetector
import com.safephrase.audio.RiskVerifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds @Singleton
    abstract fun bindKeywordDetector(impl: HeuristicKeywordDetector): KeywordDetector
}
