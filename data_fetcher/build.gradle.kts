import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
}

repositories {
    mavenCentral()
    jcenter()
    google()
}

application {
    applicationName = "data_fetcher"
    mainClass.set("io.nekohasekai.group.data_fetcher.DataFetcher")
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
            ) it.replace(project.name, "main{") else it
        }
    }
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    jvmTarget = "11"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")

    val vKtLib = "1.0-SNAPSHOT"
    implementation("io.nekohasekai.ktlib:ktlib-td-cli:$vKtLib")
    implementation("io.nekohasekai.ktlib:ktlib-opencc:$vKtLib")
    implementation("com.hankcs:hanlp:portable-1.8.1")
    implementation("org.slf4j:slf4j-nop:2.0.0-alpha1")

}