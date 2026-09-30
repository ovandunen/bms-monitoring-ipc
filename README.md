# bms-monitoring-ipc

Standalone Android library (AAR) that defines the shared domain model, application ports, and AIDL-based IPC contracts between the **BMS monitoring service** (`applicationId` `ch.ecocarsolaire.bms`) and the **EcoCar GUI** client. No UI — only contracts and adapters.

This library is **Family A** (`com.fleet.shared.bms.ipc`). EcoCar also contains a separate Family B AIDL under `com.bms.monitor.aidl` (not this artifact).

## Version and JDK

- **Group:** `com.fleet.shared`
- **Artifact:** `bms-monitoring-ipc`
- **Version:** `1.2.0-SNAPSHOT` (`build.gradle.kts` `version` and Maven publication)

Gradle wrapper **9.3.1**. `jvmToolchain(17)`; `compileOptions` Java **17**. `gradle.properties` sets `org.gradle.java.home` to a Temurin 21 install path (Gradle daemon), not the compile target.

Both apps consume this project as a **composite build**:

- `includeBuild("../bms-monitoring-ipc")` — `bms-monitoring-app/settings.gradle.kts:24`
- `implementation("com.fleet.shared:bms-monitoring-ipc:1.2.0-SNAPSHOT")` — `bms-monitoring-app/app/build.gradle.kts:164`
- `includeBuild("../bms-monitoring-ipc")` — `bms-monitoring-car-gui/settings.gradle.kts:27`
- `implementation("com.fleet.shared:bms-monitoring-ipc:1.2.0-SNAPSHOT")` — `bms-monitoring-car-gui/composeApp/build.gradle.kts:44`

That `1.2.0-SNAPSHOT` string matches this library’s `version`. Gradle substitutes the included project; `mavenLocal()` is still listed in EcoCar `dependencyResolutionManagement.repositories`.

## Publish to Maven Local

The `maven-publish` plugin is applied. Task:

```bash
./gradlew publishToMavenLocal
```

Installed path:

`~/.m2/repository/com/fleet/shared/bms-monitoring-ipc/1.2.0-SNAPSHOT/`

Use this only if you are not using `includeBuild` (for example an out-of-tree consumer).

## Consume from APK projects (composite build, primary)

In the consuming app’s `settings.gradle.kts`:

```kotlin
includeBuild("../bms-monitoring-ipc")
```

Dependency (same version as this library):

```kotlin
implementation("com.fleet.shared:bms-monitoring-ipc:1.2.0-SNAPSHOT")
```

Optional `mavenLocal()` remains valid if you published the AAR and are not using `includeBuild`.

## AIDL contract (stable IPC surface)

Files under `src/main/aidl/com/fleet/shared/bms/ipc/`:

| File | Role |
|------|------|
| `IBmsService.aidl` | Server methods (order below) |
| `IBmsCallback.aidl` | `onStateChanged`, `onConnectionStatusChanged`, `onLocationChanged` |
| `ParcelableBatterySnapshot.aidl` | Parcelable declaration for telemetry DTO |
| `ParcelableBmsCommand.aidl` | Parcelable declaration for commands |
| `ParcelableVehicleLocation.aidl` | Parcelable declaration for GPS DTO |
| `ParcelableTripSession.aidl` | Parcelable declaration for trip rows |

`IBmsService` methods in declaration order:

1. `getCurrentSnapshot`
2. `registerCallback`
3. `unregisterCallback`
4. `sendCommand`
5. `getCurrentLocation`
6. `resetTrip`
7. `getTripSessions`
8. `getIpcVersion`

Kotlin `@Parcelize` implementations live beside the AIDL package (`ParcelableBatterySnapshot` in `infrastructure/`, `ParcelableTripSession` / `ParcelableVehicleLocation` next to AIDL). Extend `ParcelableBatterySnapshot` by **appending** fields with defaults; do not reorder existing fields.

**Service binding (client `AidlBatteryClientAdapter`):** action `ch.ecocarsolaire.bms.action.DASHBOARD_SERVICE`, package `ch.ecocarsolaire.bms`. That matches BMS `BmsDashboardService` in `bms-monitoring-app` (`AndroidManifest.xml`).

## Compatibility rules

- `IpcContract.IPC_VERSION` is **2** (`src/main/kotlin/com/fleet/shared/bms/ipc/IpcContract.kt`). The client compares `remote.ipcVersion` to that constant; on mismatch it sets `ConnectionStatus.Error(IpcContract.VERSION_MISMATCH_REASON)` (`"IPC_VERSION_MISMATCH"`).
- Current `ParcelableBatterySnapshot` / domain `BatterySnapshot` field order: `timestamp`, `stateOfChargePercent`, `totalVoltage`, `current`, `cellVoltageMax`, `cellVoltageMin`, `batteryTempMax`, `batteryTempMin`, `controllerTemp`, `motorTemp`, `motorRpm`, `vehicleSpeed`, `faultCodes`, `estimatedRangeKm`, `tripDistanceKm`, `co2SavingKg`, `batteryTempAvg`, `vehicleStatus`, `batteryDataStale`, `cloudConnected`.
- Bump `IPC_VERSION` when the Parcel layout or AIDL method set changes. Build BMS and EcoCar from the **same** library version (`includeBuild` or the same published AAR).

## Types added after 1.0.0

- `TripSession` / `ParcelableTripSession` / `TripSessionMapper` — trip history over AIDL
- `VehicleStatus` (STANDBY 0, DRIVING 1, CHARGING 2 – reserved, not produced by the BMS app yet) and snapshot `vehicleStatus`
- `batteryDataStale` / `cloudConnected` on the snapshot
- `ParcelableVehicleLocation` + `getCurrentLocation` / `onLocationChanged`
- `getIpcVersion` / `IPC_VERSION`

## DDD layer boundaries

```
domain/           Pure Kotlin — BatterySnapshot, BmsCommand, ConnectionStatus, events
application/ports Driven & driving interfaces (no Android)
infrastructure/   AIDL adapters, parcelables, mappers
```

- **BMS APK (server):** implement `BatteryTelemetryPort` via `AidlBatteryServiceAdapter` inside your `Service` and expose `IBmsService` from `onBind()`.
- **EcoCar APK (client):** use `AidlBatteryClientAdapter` for `BatteryQueryPort`, `StateFlow` telemetry, and `sendCommand()`. Optionally implement `BatteryTelemetryListenerPort` in your presentation layer.

## SOLID notes

Documented in source on adapters and mappers: SRP per class, segregated read/write/command ports, domain free of Android, infrastructure depends inward on application ports.
