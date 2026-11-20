plugins { kotlin("jvm") version "2.5.0-Beta2" }
repositories { mavenLocal(); mavenCentral(); maven("https://repo.papermc.io/repository/maven-public/") }
val assets = providers.gradleProperty("benchmarkAssets").get()
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(files("../../build/libs/AdrenalinGUI-1.0-SNAPSHOT.jar"))
    compileOnly(files(listOf("BMenu-2.7.jar", "AbstractMenus-1.18.0.jar", "BLibV2-1.7.9.jar", "PlaceholderAPI.jar")
        .map { file("$assets/$it") }))
}
kotlin { jvmToolchain(21) }
