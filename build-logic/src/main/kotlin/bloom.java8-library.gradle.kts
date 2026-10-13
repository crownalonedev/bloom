// Java 8 bytecode module. Required for anything that can run on a legacy (1.7.10 / 1.8.x) server.

plugins {
    id("bloom.java-base")
}

tasks.named<JavaCompile>("compileJava") {
    options.release = 8
}

// Tests may use a newer language level unless the module opts into Java 8 runtime tests
// (see bloom.java8-runtime-tests), which recompile tests for Java 8.
tasks.named<JavaCompile>("compileTestJava") {
    options.release = 21
}
