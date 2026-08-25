extension {
    name = "extensions/nicomanga.rve"
}

android {
    namespace = "app.revanced.extension.nicomanga"
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}
