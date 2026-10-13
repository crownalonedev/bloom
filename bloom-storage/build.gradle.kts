plugins {
    id("bloom.java8-runtime-tests")
    id("bloom.publishing")
}

description = "Bloom Storage: asynchronous JDBC (SQLite, MySQL, MariaDB) with pooling, transactions, migrations and repositories."

dependencies {
    api(project(":bloom-api"))
    // HikariCP 4.0.3 is the last Java 8 line; its API is compatible with newer versions, which modern-only
    // plugins may substitute. Brings slf4j-api transitively (shade + relocate both).
    implementation(libs.hikari)

    testImplementation(libs.sqlite.jdbc)
    testRuntimeOnly(libs.mariadb.jdbc)
    testRuntimeOnly(libs.mysql.jdbc)
}

// Opt-in tests against real MySQL/MariaDB servers (see scripts/test-network-databases.sh). Excluded from `test`.
tasks.named<Test>("test") {
    exclude("**/NetworkDatabaseIT*")
}
tasks.named<Test>("testJava8") {
    exclude("**/NetworkDatabaseIT*")
}
tasks.register<Test>("networkDatabaseTest") {
    description = "Runs storage tests against MySQL/MariaDB given by BLOOM_TEST_MYSQL_PORT / BLOOM_TEST_MARIADB_PORT."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/NetworkDatabaseIT*")
    environment("BLOOM_TEST_MYSQL_PORT", providers.environmentVariable("BLOOM_TEST_MYSQL_PORT").getOrElse(""))
    environment("BLOOM_TEST_MARIADB_PORT", providers.environmentVariable("BLOOM_TEST_MARIADB_PORT").getOrElse(""))
}
