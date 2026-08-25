group = "io.github.roflsunriz"

configurations.named("compileClasspath") {
    resolutionStrategy.activateDependencyLocking()
    resolutionStrategy.force("org.apache.commons:commons-lang3:3.18.0")
}

configurations.named("runtimeClasspath") {
    resolutionStrategy.activateDependencyLocking()
    resolutionStrategy.force("org.apache.commons:commons-lang3:3.18.0")
}

dependencyLocking {
    lockMode.set(org.gradle.api.artifacts.dsl.LockMode.STRICT)
}

patches {
    about {
        name = "Nicomanga ReVanced"
        description = "ReVanced patches for Nicomanga"
        source = "https://github.com/roflsunriz/nicomanga-revanced"
        author = "roflsunriz"
        contact = "https://github.com/roflsunriz/nicomanga-revanced/issues"
        website = "https://github.com/roflsunriz/nicomanga-revanced"
        license = "GNU General Public License v3.0"
    }
}
