# Установка

Публичная сборка поставляется отдельным серверным плагином. Ваш плагин подключает её API и объявляет зависимость AdrenalinGUI.

## Требования

| Компонент | Требование |
| --- | --- |
| Java | 21 |
| Сервер | Paper 1.21.11 или совместимый форк |
| Проверенное ядро | Paper 1.21.11, build 132 |
| Компилятор проекта | Kotlin 2.5.0-Beta2 |
| Дополнительные плагины | По используемым интеграциям |

Для обычных меню достаточно AdrenalinGUI. Плейсхолдеры требуют PlaceholderAPI и нужных расширений. Экономика требует Vault и экономического провайдера. Группы и изменение прав требуют LuckPerms.

## Серверный JAR

Скачайте AdrenalinGUI-2.0.1.jar из GitHub Releases, положите его в plugins и перезапустите сервер.

[Скачать релиз](https://github.com/chotaktota/AdrenalinGUI/releases/tag/v2.0.1)

## Зависимость в своём плагине

Положите тот же JAR в папку libs своего проекта.

~~~kotlin build.gradle.kts
plugins {
    kotlin("jvm") version "2.5.0-Beta2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(files("libs/AdrenalinGUI-2.0.1.jar"))
}

kotlin {
    jvmToolchain(21)
}
~~~

~~~yaml plugin.yml
name: MyPlugin
version: '1.0'
main: example.MyPlugin
api-version: '1.21.11'
depend: [AdrenalinGUI]
commands:
  menu:
    description: Открыть меню
~~~

## paper-plugin.yml

Если ваш плагин использует формат Paper, объявите серверную зависимость и доступ к classpath.

~~~yaml paper-plugin.yml
dependencies:
  server:
    AdrenalinGUI:
      load: BEFORE
      required: true
      join-classpath: true
~~~

## Сборка из исходников

~~~bash
git clone https://github.com/chotaktota/AdrenalinGUI.git
cd AdrenalinGUI
git checkout v2.0.1
./gradlew clean build publishToMavenLocal "-PreleaseVersion=2.0.1"
~~~

В Windows используйте .\gradlew.bat. Публикация в mavenLocal позволяет подключить compileOnly("dev.aethereal:AdrenalinGUI:2.0.1") из локального Maven-репозитория.
