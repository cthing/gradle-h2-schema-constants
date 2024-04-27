plugins {
    java
    id("org.cthing.h2-flyway-constants")
}

tasks {
    generateH2FlywayConstants {
        classname = "org.cthing.test.DBConstants"
        locations("db/migrations")
    }
}
