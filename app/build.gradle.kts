plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
}

tasks.register("printConfigs"){
    doLast {
        println("--- configurations for project ':app' ---")
        configurations.forEach { println(it.name) }
        println("--- end ---")
    }
}

android {
    namespace = "org.wahid.attendancehub"
    compileSdk {
        version = release(36)
    }


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
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") { isMinifyEnabled = false }
        getByName("release") { isMinifyEnabled = true }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
//        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }


    flavorDimensions("user_type")
    productFlavors {
        create("teacher") {
            isDefault = true
            dimension = "user_type"
            applicationId = "com.attendancehub.teacher"
            buildConfigField("boolean", "IS_TEACHER", "true")
        }

        create("student") {
            dimension = "user_type"
            applicationId = "com.attendancehub.student"
            buildConfigField("boolean", "IS_TEACHER", "false")
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
//    implementation(libs.viewmodel)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Teacher-specific dependencies
    add("teacherImplementation", dependencyNotation =  "com.journeyapps:zxing-android-embedded:4.3.0")

    // Student-specific dependencies
    add("studentImplementation", project(":student"))
    add("studentImplementation", libs.barcode.scanning)
    add("studentImplementation", libs.play.services.code.scanner)

    implementation(libs.barcode.scanning)
    implementation(libs.play.services.code.scanner)
}
