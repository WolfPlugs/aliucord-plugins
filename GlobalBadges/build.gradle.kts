version = "1.1.0"
description = "Show global profile badges from other Discord clients and BadgeVault"

// Current Aliucord API artifacts use Java 21 bytecode. Emit Java 11 plugin classes.
tasks.withType<JavaCompile>().configureEach {
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

// This module is Java-only; avoid Kotlin scanning newer Aliucord metadata.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    enabled = false
}

// The older packaging plugin otherwise also scans a missing Kotlin output folder.
tasks.named<com.aliucord.gradle.task.CompileDexTask>("compileDex").configure {
    input.setFrom(tasks.named<JavaCompile>("compileDebugJavaWithJavac").map { it.destinationDirectory })
}
