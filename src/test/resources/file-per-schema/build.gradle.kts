plugins {
    java
    id("org.cthing.h2-sql-constants")
}

tasks {
    generateH2SqlConstants {
        classname = "org.cthing.test.DBConstants"
        filePerSchema = true
        sources(file("schema1.sql"), file("schema2.sql"), file("schema4.sql"))
    }
}
