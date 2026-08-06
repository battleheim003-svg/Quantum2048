pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/google") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/gradle-plugin") }
        google(); mavenCentral(); gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/google") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        google(); mavenCentral()
    }
}
rootProject.name = "Quantum2048"
include(":app")
