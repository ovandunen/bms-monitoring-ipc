plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("maven-publish")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.fleet.shared.bms.ipc"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        aidl = true
    }


    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}

val integrationContract = rootProject.file("../bms-monitoring-app/integration-test.contract.properties")
tasks.withType<Test>().configureEach {
    if (integrationContract.exists()) {
        systemProperty("integration.contract.file", integrationContract.absolutePath)
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.fleet.shared"
            artifactId = "bms-monitoring-ipc"
            version = "1.0.0-SNAPSHOT"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            url = uri("${System.getProperty("user.home")}/.m2/repository")
        }
    }
}
