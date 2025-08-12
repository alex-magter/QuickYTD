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
            jvmTarget.set(JvmTarget.JVM_21) // o JVM_11, o incluso JVM_21 si está disponible como enum
        }
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
            //implementation(libs.androidx.material3)
            //implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
            implementation(libs.navigation.compose)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)

        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            /*implementation(libs.kotlinx.coroutines.swing.v190)*/
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
    implementation(libs.androidx.compose.bom)
    /*implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)*/



    //debugImplementation(compose.uiTooling)
}

tasks.named("desktopProcessResources", org.gradle.language.jvm.tasks.ProcessResources::class.java) {
    // La tarea por defecto ya tiene como fuente src/desktopMain/resources.
    // Solo necesitamos ajustar qué de esa fuente se incluye en la salida.

    val os = OperatingSystem.current()

    // Definimos los patrones de exclusión para las carpetas de otros SOs.
    // Estos patrones son relativos a la raíz de src/desktopMain/resources.
    val excludes = mutableListOf<String>()
    if (!os.isWindows) {
        excludes.add("bin/windows/**")
    }
    if (!os.isLinux) {
        excludes.add("bin/linux/**")
    }
    if (!os.isMacOsX) {
        excludes.add("bin/macos/**")
    }

    // Aplicamos las exclusiones.
    // Esto asegura que solo se copien los archivos comunes de src/desktopMain/resources
    // y la carpeta específica del SO actual desde src/desktopMain/resources.
    exclude(excludes)

    // No necesitamos 'include' explícitos aquí si solo estamos excluyendo
    // las carpetas de los otros SOs. Todo lo demás de src/desktopMain/resources
    // (archivos en su raíz + la carpeta del SO actual + otras carpetas comunes) se incluirá.

    // Por ejemplo, si estás en Windows:
    // - Se excluye "linux/**" y "macos/**".
    // - src/desktopMain/resources/commonFile.txt se incluye.
    // - src/desktopMain/resources/windows/** se incluye.
    // - src/desktopMain/resources/anotherDesktopCommonFolder/** se incluye.
}

compose.desktop {
    application {
        mainClass = "org.alexmagter.QuickYTD.MainKt"


        nativeDistributions {

            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.AppImage)
            packageName = "QuickYTD"
            packageVersion = "1.2.0"
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
