import com.aliucord.gradle.AliucordExtension
import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        // Aliucords Maven repo which contains our tools and dependencies
        maven("https://maven.aliucord.com/snapshots")
        // Shitpack which still contains some Aliucord dependencies for now. TODO: Remove
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        // D8 bundled with AGP 7.0 crashes on classes emitted by current javac.
        classpath("com.android.tools:r8:8.3.37")
        // Aliucord gradle plugin which makes everything work and builds plugins
        classpath("com.aliucord:gradle:main-SNAPSHOT")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliucord.com/snapshots")
    }
}

fun Project.aliucord(configuration: AliucordExtension.() -> Unit) = extensions.getByName<AliucordExtension>("aliucord").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "com.aliucord.gradle")

    // Fill out with your info
    aliucord {
        author("ThatWolf", 347096063569559553)
        updateUrl.set("https://raw.githubusercontent.com/WolfPlugs/hot-plugins/builds/updater.json")
        buildUrl.set("https://raw.githubusercontent.com/WolfPlugs/hot-plugins/builds/%s.zip")
    }

    android {
        compileSdkVersion(31)

        defaultConfig {
            minSdk = 24
            targetSdk = 31
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    // Read the current Java 21 Aliucord API while emitting Java 11 plugin classes.
    tasks.withType<JavaCompile>().configureEach {
        javaCompiler.set(project.extensions.getByType<org.gradle.jvm.toolchain.JavaToolchainService>().compilerFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        })
    }

    dependencies {
        val discord by configurations
        val implementation by configurations

        // Stubs for all Discord classes
        discord("com.discord:discord:aliucord-SNAPSHOT")
        implementation("com.aliucord:Aliucord:main-SNAPSHOT")

        implementation("androidx.appcompat:appcompat:1.4.1")
        implementation("com.google.android.material:material:1.5.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}