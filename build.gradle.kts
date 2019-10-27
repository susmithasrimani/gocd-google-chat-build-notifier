plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    id("org.jetbrains.kotlin.jvm").version("1.3.50")
    id("org.jetbrains.kotlin.plugin.serialization").version("1.3.50")
    id("org.jlleitschuh.gradle.ktlint") version "8.2.0"
    jacoco
}
repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    mavenCentral()
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

dependencies {
    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // kotlintest test framework
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.0")

    // komock
    testImplementation("ua.com.lavi:komock-core:1.10.0")

    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.12.0")

    // gocd plugin api
    compile("cd.go.plugin:go-plugin-api:19.7.0")

    // lightbend config
    compile("com.typesafe:config:1.3.4")

    // fuel library
    implementation("com.github.kittinunf.fuel:fuel:2.2.0")
}

// task to create a uber/fat jar with plugin code and all it's dependencies
tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    }, file("plugin.xml"))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.73".toBigDecimal()
            }
        }
    }
}
