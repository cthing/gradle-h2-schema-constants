# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") gradle-h2-schema-constants

[![CI](https://github.com/cthing/gradle-h2-schema-constants/actions/workflows/ci.yml/badge.svg)](https://github.com/cthing/gradle-h2-schema-constants/actions/workflows/ci.yml)

Gradle plugins that generate a Java source file with constants for table and column names in an H2 database.

## Building

The plugin is compiled for Java 17. If a Java 17 toolchain is not available, one will be downloaded.

Gradle is used to build the plugin:
```bash
./gradlew build
```
The Javadoc for the plugin can be generated by running:
```bash
./gradlew javadoc
```
