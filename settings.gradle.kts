rootProject.name = "TdGroupBot"

includeBuild("ktlib") {
    dependencySubstitution {
        fun include(name: String) = substitute(module("io.nekohasekai.ktlib:$name")).with(project(":$name"))
        include("ktlib-td-cli")
        include("ktlib-db")
    }
}