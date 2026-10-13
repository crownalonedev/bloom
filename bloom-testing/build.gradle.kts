plugins {
    id("bloom.java8-runtime-tests")
    id("bloom.publishing")
}

description = "Bloom Testing: server-free test doubles for plugin unit tests (manual scheduler, controllable clock)."

dependencies {
    api(project(":bloom-api"))
    implementation(project(":bloom-common"))
}
