import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget



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

    }

    jvmToolchain { // Configura la toolchain para todos los targets JVM de Kotlin
        languageVersion.set(JavaLanguageVersion.of(21)) // O 17, 21
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
            implementation(libs.androidx.material3)
            implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
            implementation(libs.navigation.compose)
            implementation(compose.materialIconsExtended)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.java.v301)
            implementation(libs.kotlinx.coroutines.swing.v190)
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
        versionCode = 1
        versionName = "1.0"
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
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.compose.bom)
    implementation(libs.androidx.animation.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)



    //debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.alexmagter.QuickYTD.MainKt"

        javaHome = "C:\\Program Files\\Java\\jdk-21"

        nativeDistributions {

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "QuickYTD"
            packageVersion = "1.2.0"
            vendor = "Alex_magter"

            jvmArgs += listOf("--module-path", "app/libs") // Ajusta si tus JARs están en otra subcarpeta dentro del bundle
            // La siguiente línea es para pasar opciones directamente a jpackage,

            args += listOf("--add-modules", "java.base,java.desktop,java.logging,jdk.crypto.ec,jdk.unsupported")

            modules("java.compiler", "java.instrument" , "java.sql", "jdk.unsupported")



            windows {
                // Esta es la propiedad válida para Windows

                includeAllModules = true
                menuGroup = "AlexMagter"
                // Opcional: define nombre del acceso directo
                shortcut = true

                // iconFile.set(...)
            }

            macOS {
                bundleID = "com.miempresa.miapp"
                // iconFile.set(...)
            }

            linux {
                // iconFile.set(...)
            }
        }
    }
}
