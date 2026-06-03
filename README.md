# bms-monitoring-ipc

Standalone Android library (AAR) that defines the shared domain model, application ports, and AIDL-based IPC contracts between the **BMS monitoring service** (`com.fleet.bms`) and the **EcoCar GUI** client. No UI — only contracts and adapters.

## Publish to Maven Local

Requires Android SDK. Use Gradle **8.14.4+** (included in the wrapper) so the build runs on JDK 25; compile target remains Java 17.

```bash
./gradlew publishToMavenLocal
```

Artifact coordinates:

- **Group:** `com.fleet.shared`
- **Artifact:** `bms-monitoring-ipc`
- **Version:** `1.0.0-SNAPSHOT`

Installed path:

`~/.m2/repository/com/fleet/shared/bms-monitoring-ipc/1.0.0-SNAPSHOT/`

## Consume from APK projects

In the consuming app’s `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
repositories {
    mavenLocal()
    google()
    mavenCentral()
}
```

Dependency:

```kotlin
implementation("com.fleet.shared:bms-monitoring-ipc:1.0.0-SNAPSHOT")
```

## AIDL contract (stable IPC surface)

These files are the cross-process API; keep them backward compatible when possible:

| File | Role |
|------|------|
| `IBmsService.aidl` | Server: snapshot query, callback registration, command ingress |
| `IBmsCallback.aidl` | Client callbacks: state + connection status |
| `ParcelableBatterySnapshot.aidl` | Parcelable declaration for telemetry DTO |
| `ParcelableBmsCommand.aidl` | Parcelable declaration for commands |

Kotlin `@Parcelize` implementations live beside the AIDL package (`com.fleet.shared.bms.ipc`). Extend `ParcelableBatterySnapshot` fields with defaults before changing AIDL when adding telemetry.

**Service binding (client):** action `com.fleet.bms.action.MONITOR_SERVICE`, package `com.fleet.bms` — see `AidlBatteryClientAdapter`.

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
