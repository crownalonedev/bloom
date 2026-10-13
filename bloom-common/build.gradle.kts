plugins {
    id("bloom.java8-runtime-tests")
    id("bloom.publishing")
}

description = "Bloom common: internal platform-neutral implementations shared by Bloom modules. Not a public API."

dependencies {
    api(project(":bloom-api"))
}
