// Java 21 bytecode module (modern Paper 1.21.x+). Must never be loaded on a legacy server JVM;
// bloom-bukkit only loads these classes after selecting the modern adapter.

plugins {
    id("bloom.java-base")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

