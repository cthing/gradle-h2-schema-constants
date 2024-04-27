plugins {
    java
    id("org.cthing.h2-sql-constants")
}

tasks {
    generateH2SqlConstants {
        classname = "org.cthing.test.DBConstants"
        sizesOnly = true
        sources(file("schema1.sql"), file("schema2.sql"))
    }
}
