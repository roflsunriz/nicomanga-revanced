extension {
    name = "extensions/nicomanga.rve"
}

configurations.configureEach {
    if (name == "releaseCompileClasspath" || name == "releaseRuntimeClasspath") {
        resolutionStrategy.activateDependencyLocking()
    }
}

dependencyLocking {
    lockMode.set(org.gradle.api.artifacts.dsl.LockMode.STRICT)
}

android {
    namespace = "app.revanced.extension.nicomanga"
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}
