package com.orca.agent.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiDeviceCoordinator @Inject constructor() {
    private val _deviceId = MutableStateFlow("orca_${java.util.UUID.randomUUID().toString().take(8)}")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()
    private val _isPrimaryDevice = MutableStateFlow(true)
    val isPrimaryDevice: StateFlow<Boolean> = _isPrimaryDevice.asStateFlow()

    fun isOnlyActiveDevice(): Boolean = true
    fun getActiveDeviceCount(): Int = 1
}
