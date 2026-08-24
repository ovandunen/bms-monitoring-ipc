package com.fleet.shared.bms.ipc.infrastructure

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import androidx.core.content.ContextCompat
import com.fleet.shared.bms.ipc.IBmsCallback
import com.fleet.shared.bms.ipc.IBmsService
import com.fleet.shared.bms.ipc.ParcelableBmsCommand
import com.fleet.shared.bms.ipc.application.ports.BatteryQueryPort
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.BmsCommand
import com.fleet.shared.bms.ipc.domain.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

/**
   * Client-side IPC adapter.
   *
   *  - Single Responsibility: Android [ServiceConnection] lifecycle, bind/reconnect, and callback bridge.
   *  - Liskov Substitution: usable wherever [BatteryQueryPort] is required; flows expose reactive state.
   *  - Dependency Inversion: maps IPC DTOs to domain via [BatterySnapshotMapper] before exposing to callers.
   */
class AidlBatteryClientAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
) : BatteryQueryPort {

    // ... rest of code...
}
