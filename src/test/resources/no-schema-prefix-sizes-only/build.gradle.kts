plugins {
    java
    id("org.cthing.h2-sql-constants")
}

tasks {
    generateH2SqlConstants {
        classname = "org.cthing.test.DBConstants"
        prefixWithSchema = false
        sizesOnly = true
        sources(file("schema1.sql"), file("schema2.sql"))
    }
}
