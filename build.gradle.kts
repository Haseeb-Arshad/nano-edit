// Root build script (Kotlin DSL). Module dependencies belong in app/build.gradle.kts.

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
