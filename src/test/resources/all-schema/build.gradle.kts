plugins {
    java
    id("org.cthing.h2-sql-constants")
}

tasks {
    generateH2SqlConstants {
        excludeSchema = emptySet()
        classname = "org.cthing.test.DBConstants"
        sources(file("schema1.sql"), file("schema2.sql"))
    }
}
