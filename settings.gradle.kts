rootProject.name = "TdGroupBot"

includeBuild("ktlib") {
    dependencySubstitution {
        fun include(name: String) = substitute(module("io.nekohasekai.ktlib:$name")).with(project(":$name"))
        include("ktlib-td-cli")
        include("ktlib-db")
        include("ktlib-nsfw")
        include("ktlib-ocr")
        include("ktlib-opencc")
    }
}

include(":data_fetcher")