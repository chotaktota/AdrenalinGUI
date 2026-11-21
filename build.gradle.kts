plugins {
    kotlin("jvm") version "2.5.0-Beta2"
    `maven-publish`
}
group = "dev.aethereal"
version = providers.gradleProperty("releaseVersion").getOrElse("1.0-SNAPSHOT")
repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.helpch.at/releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("com.github.MilkBowl:VaultAPI:1.7")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}
kotlin { jvmToolchain(21) }
java { withSourcesJar() }
tasks.test { useJUnitPlatform() }
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }
publishing { publications { create<MavenPublication>("library") { from(components["java"]) } } }

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}
