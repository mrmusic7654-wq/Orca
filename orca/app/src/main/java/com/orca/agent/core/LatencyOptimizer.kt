package com.orca.agent.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LatencyOptimizer @Inject constructor() {
    private var baseWaitAfterAction = 500L
    private var waitAfterTap = 400L
    private var waitAfterAppLaunch = 1500L

    fun estimateRemainingTime(chain: TaskChain): Long {
        var estimated = 0L
        for (i in chain.currentNodeIndex until chain.nodes.size) {
            estimated += when (chain.nodes[i].action) {
                is AgentAction.Tap -> waitAfterTap + 500
                is AgentAction.AppAction -> waitAfterAppLaunch + 1000
                else -> baseWaitAfterAction + 500
            }
        }
        return estimated
    }

    fun recordTiming(action: AgentAction, duration: Long) {
        when (action) {
            is AgentAction.Tap -> waitAfterTap = (waitAfterTap * 0.9 + duration * 0.1).toLong()
            else -> baseWaitAfterAction = (baseWaitAfterAction * 0.95 + duration * 0.05).toLong()
        }
    }
}
