import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.4.30"
    id("com.github.ben-manes.versions") version "0.36.0"
}

repositories {
    mavenCentral()
    jcenter()
    google()
}

group = "io.nekohasekai"
version = "1.0-SNAPSHOT"

application {
    applicationName = project.name
    mainClass.set("io.nekohasekai.group.TdGroupBot")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

distributions {
    main {
        distributionBaseName.set("main")
        contents.rename {
            if (it == project.name ||
                it.startsWith(project.name) && it.endsWith("bat")
            ) it.replace(project.name, "main") else it
        }
    }
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    jvmTarget = "11"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    val vKtLib = "1.0-SNAPSHOT"
    implementation("io.nekohasekai.ktlib:ktlib-td-cli:$vKtLib")
    implementation("io.nekohasekai.ktlib:ktlib-db:$vKtLib")
    implementation("io.nekohasekai.ktlib:ktlib-nsfw:$vKtLib")
    implementation("io.nekohasekai.ktlib:ktlib-opencc:$vKtLib")
    implementation("io.nekohasekai.ktlib:ktlib-ocr:$vKtLib")

    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("com.google.zxing:javase:3.4.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.hankcs:hanlp:portable-1.7.8")

    implementation("org.slf4j:slf4j-nop:2.0.0-alpha1")

}