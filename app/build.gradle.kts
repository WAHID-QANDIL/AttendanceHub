plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "org.wahid.attendancehub"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.wahid.attendancehub"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    flavorDimensions += "userType"
    productFlavors {
        create("teacher") {
            dimension = "userType"
            applicationIdSuffix = ".teacher"
            versionNameSuffix = "-teacher"
            buildConfigField("boolean", "IS_TEACHER", "true")
        }

        create("student") {
            dimension = "userType"
            applicationIdSuffix = ".student"
            versionNameSuffix = "-student"
            buildConfigField("boolean", "IS_TEACHER", "false")
        }
    }

    // Explicit source set configuration for IDE recognition
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
        }
        getByName("teacher") {
            java.srcDirs("src/teacher/java")
            res.srcDirs("src/teacher/res")
        }
        getByName("student") {
            java.srcDirs("src/student/java")
            res.srcDirs("src/student/res")
        }
    }
}

dependencies {

    implementation(project(":core"))

    // Shared dependencies
    "teacherImplementation"(libs.zxing.android.embedded)
    implementation(libs.kotlinx.serialization.json)

    // Student flavor specific dependencies
    "studentImplementation"(libs.androidx.camera.view)
    "studentImplementation"(libs.androidx.camera.camera2)
    "studentImplementation"(libs.androidx.camera.core)
    "studentImplementation"(libs.androidx.camera.lifecycle)
    "studentImplementation"(libs.barcode.scanning)
    "studentImplementation"(libs.play.services.code.scanner)

    // Testing dependencies (each module needs its own)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug dependencies (each module needs its own)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.material)
}
