import com.android.build.api.variant.VariantOutputConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
}

abstract class PhoneResourcesTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourcesDirectory: DirectoryProperty

    @get:Input
    abstract val resourcePatterns: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun sync() {
        fileSystemOperations.sync {
            from(resourcesDirectory) { include(resourcePatterns.get()) }
            into(outputDirectory)
        }
    }
}

val phoneResources = tasks.register<PhoneResourcesTask>("phoneResources") {
    resourcesDirectory.set(rootProject.layout.projectDirectory.dir("app/src/main/res"))
    resourcePatterns.addAll(
        "font/google_sans_flex.ttf",
        "mipmap-*/ic_launcher*",
        "drawable/ic_launcher_foreground.xml",
        "values/ic_launcher_background.xml",
        "values*/strings.xml",
        "drawable/widget_play.xml",
        "drawable/widget_pause.xml",
        "drawable/skip_next.xml",
        "drawable/skip_previous.xml",
        "drawable/shuffle.xml",
        "drawable/queue_music_rounded.xml",
        "drawable/music_note.xml",
        "drawable/warning.xml",
        "drawable/folder_rounded.xml"
    )
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(phoneResources, PhoneResourcesTask::outputDirectory)
    }

    onVariants(selector().withBuildType("release")) { variant ->
        val mainOutput = variant.outputs.single { it.outputType == VariantOutputConfiguration.OutputType.SINGLE }

        @Suppress("UnstableApiUsage")
        mainOutput.outputFileName = "Chocola_Wear_${mainOutput.versionName.get()}.apk"
    }
}

android {
    namespace = "com.sosauce.chocola.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sosauce.cutemusic"
        minSdk = 30
        targetSdk = 37
        versionCode = 50015
        versionName = "4.5.0"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            if (keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                println("Wear app won't be signed!")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.startup)
    implementation(libs.androidx.material3)
    implementation(libs.material.kolor)
    implementation(libs.kmpalette.core)
    implementation(libs.squircle.shape)
    implementation(libs.sosauce.nekobites)
    debugImplementation(libs.androidx.wear.compose.ui.tooling)
}
