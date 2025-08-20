import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.internal.os.OperatingSystem



plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvmToolchain { // Configura la toolchain para todos los targets JVM de Kotlin
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(project(":pythonrunner"))
            implementation(libs.androidx.media3.transformer)
            implementation(libs.androidx.media3.effect)
            implementation(libs.androidx.media3.common)
            implementation(libs.androidx.media3.exoplayer)

            implementation(libs.androidx.material3.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.animation.android)





        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)

        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

android {
    namespace = "org.alexmagter.QuickYTD"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.alexmagter.QuickYTD"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        
        versionCode = 3
        versionName = "2.0.0"

    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main"){
            jniLibs.srcDir("src/androidMain/jniLibs")
        }
    }

}

dependencies {
    implementation(libs.androidx.compose.bom)
}

tasks.named("desktopProcessResources", org.gradle.language.jvm.tasks.ProcessResources::class.java) {

    val os = OperatingSystem.current()

    val excludes = mutableListOf<String>()
    if (!os.isWindows) {
        excludes.add("bin/win/**")
    }
    if (!os.isLinux) {
        excludes.add("bin/linux/**")
    }
    if (!os.isMacOsX) {
        excludes.add("bin/macos/**")
    }
}

compose.desktop {
    application {
        mainClass = "org.alexmagter.QuickYTD.MainKt"


        nativeDistributions {

            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.AppImage)
            packageName = "QuickYTD"
            packageVersion = "2.0.0"
            vendor = "Alex_magter"

            val iconBaseDir = project.projectDir.resolve("src/desktopMain/resources/icons")

            windows {

                menuGroup = "AlexMagter"
                shortcut = true

                iconFile.set(iconBaseDir.resolve("logo.ico"))
            }

            macOS {
                bundleID = "com.miempresa.miapp"
                iconFile.set(iconBaseDir.resolve("logo.icns"))
            }

            linux {
                iconFile.set(iconBaseDir.resolve("logo.png"))
            }
        }
    }
}
