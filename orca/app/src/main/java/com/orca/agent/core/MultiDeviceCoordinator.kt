package com.orca.agent.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiDeviceCoordinator @Inject constructor() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _deviceId = MutableStateFlow(generateDeviceId())
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()
    
    private val _isPrimaryDevice = MutableStateFlow(true)
    val isPrimaryDevice: StateFlow<Boolean> = _isPrimaryDevice.asStateFlow()
    
    private val _coordinatorState = MutableStateFlow(CoordinatorState())
    val coordinatorState: StateFlow<CoordinatorState> = _coordinatorState.asStateFlow()
    
    private val _conflictLog = MutableSharedFlow<DeviceConflict>(replay = 20)
    val conflictLog: SharedFlow<DeviceConflict> = _conflictLog.asSharedFlow()
    
    data class CoordinatorState(
        val knownDevices: List<DeviceInfo> = emptyList(),
        val activeLocks: Map<String, String> = emptyMap(), // resource -> deviceId
        val pendingActions: List<CoordinatedAction> = emptyList(),
        val lastSyncTimestamp: Long = 0
    )
    
    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val isActive: Boolean,
        val lastSeen: Long,
        val capabilities: List<String>
    )

    // ============================================================
    // DISTRIBUTED LOCKING
    // ============================================================
    
    suspend fun acquireLock(resource: String): Boolean {
        val currentLocks = _coordinatorState.value.activeLocks
        
        // Check if another device holds the lock
        val lockHolder = currentLocks[resource]
        if (lockHolder != null && lockHolder != _deviceId.value) {
            // Another device has the lock
            _conflictLog.emit(
                DeviceConflict(
                    resource = resource,
                    holdingDevice = lockHolder,
                    requestingDevice = _deviceId.value,
                    timestamp = System.currentTimeMillis(),
                    resolution = "LOCK_DENIED"
                )
            )
            return false
        }
        
        // Acquire lock
        val newLocks = currentLocks.toMutableMap()
        newLocks[resource] = _deviceId.value
        
        _coordinatorState.value = _coordinatorState.value.copy(
            activeLocks = newLocks
        )
        
        return true
    }
    
    suspend fun releaseLock(resource: String) {
        val currentLocks = _coordinatorState.value.activeLocks.toMutableMap()
        if (currentLocks[resource] == _deviceId.value) {
            currentLocks.remove(resource)
            _coordinatorState.value = _coordinatorState.value.copy(
                activeLocks = currentLocks
            )
        }
    }

    // ============================================================
    // CONFLICT PREVENTION
    // ============================================================
    
    fun shouldExecuteAction(action: String): Boolean {
        // Check if this action would conflict with another device
        val resourceMap = mapOf(
            "book_flight" to "flight_booking",
            "send_message" to "messaging",
            "order_food" to "food_ordering",
            "make_call" to "phone_call"
        )
        
        val resource = resourceMap.entries
            .find { action.contains(it.key, ignoreCase = true) }
            ?.value ?: return true // No conflict potential
        
        // Try to acquire lock for this resource
        return runBlocking { acquireLock(resource) }
    }
    
    fun detectDuplicateActions(
        myAction: String,
        otherDeviceActions: List<String>
    ): Boolean {
        return otherDeviceActions.any { otherAction ->
            calculateActionSimilarity(myAction, otherAction) > 0.8f
        }
    }
    
    private fun calculateActionSimilarity(action1: String, action2: String): Float {
        val words1 = action1.lowercase().split(" ").toSet()
        val words2 = action2.lowercase().split(" ").toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0f
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toFloat() / union.toFloat()
    }

    // ============================================================
    // DEVICE DISCOVERY
    // ============================================================
    
    fun registerDevice(deviceInfo: DeviceInfo) {
        val devices = _coordinatorState.value.knownDevices.toMutableList()
        devices.removeAll { it.deviceId == deviceInfo.deviceId }
        devices.add(deviceInfo)
        
        _coordinatorState.value = _coordinatorState.value.copy(
            knownDevices = devices
        )
    }
    
    fun getActiveDeviceCount(): Int {
        return _coordinatorState.value.knownDevices.count { it.isActive }
    }
    
    fun isOnlyActiveDevice(): Boolean {
        return getActiveDeviceCount() <= 1
    }

    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun generateDeviceId(): String {
        return "orca_${java.util.UUID.randomUUID().toString().take(8)}"
    }
}

data class DeviceConflict(
    val resource: String,
    val holdingDevice: String,
    val requestingDevice: String,
    val timestamp: Long,
    val resolution: String
)

data class CoordinatedAction(
    val actionId: String,
    val deviceId: String,
    val action: String,
    val status: String,
    val timestamp: Long
)
