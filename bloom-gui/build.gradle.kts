plugins {
    id("bloom.java8-library")
    id("bloom.publishing")
}

description = "Bloom GUI: inventory menus with buttons, layouts, pagination, navigation and duplication-safe event handling."

dependencies {
    api(project(":bloom-items"))
    implementation(project(":bloom-common"))
    compileOnly(libs.spigot.api.legacy)

    testImplementation(libs.spigot.api.legacy)
    testImplementation(platform(libs.junit6.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
