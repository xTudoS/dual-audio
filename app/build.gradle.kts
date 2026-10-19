plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.byxnd.dualaudio"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.byxnd.dualaudio"
        minSdk = 34
        targetSdk = 37
        versionCode = 3
        // The release workflow passes the git tag (v1.2.3 -> 1.2.3)
        versionName = providers.environmentVariable("DUALAUDIO_VERSION_NAME").orElse("1.0").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing comes from the environment (GitHub Secrets in CI); never commit the keystore.
    val releaseKeystore = providers.environmentVariable("DUALAUDIO_KEYSTORE_FILE").orNull
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("DUALAUDIO_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("DUALAUDIO_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("DUALAUDIO_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        aidl = true
        buildConfig = true
    }
}

/** Copies the repository's license files into the APK assets (assets/legal/), shown in the UI. */
abstract class CopyLegalNoticesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val notices: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val legal = outputDir.get().asFile.resolve("legal")
        legal.deleteRecursively()
        legal.mkdirs()
        notices.files.forEach { it.copyTo(legal.resolve(it.name), overwrite = true) }
    }
}

val copyLegalNotices = tasks.register<CopyLegalNoticesTask>("copyLegalNotices") {
    notices.from(rootProject.file("LICENSE"), rootProject.file("THIRD_PARTY_NOTICES.md"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyLegalNotices, CopyLegalNoticesTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}