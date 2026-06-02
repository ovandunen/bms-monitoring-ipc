package com.fleet.shared.bms.ipc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * IPC parcelable DTO for [com.fleet.shared.bms.ipc.domain.BmsCommand].
 */
@Parcelize
data class ParcelableBmsCommand(
    val commandId: String,
    val typeName: String,
    val payload: Map<String, String> = emptyMap(),
) : Parcelable
