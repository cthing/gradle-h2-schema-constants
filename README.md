# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") gradle-h2-schema-constants

[![CI](https://github.com/cthing/gradle-h2-schema-constants/actions/workflows/ci.yml/badge.svg)](https://github.com/cthing/gradle-h2-schema-constants/actions/workflows/ci.yml)

Gradle plugins that generate a Java source file with constants for table and column names in an H2 database.

## Motivation

A common pattern in Java applications is to hardcode the names of database tables and columns when accessing the
database. Unfortunately, this can lead to problems due to misspelling of names, and can make schema naming changes
a challenge because it is a string replacement. To address these issues, the `org.cthing.h2-sql-constants` and
`org.cthing.h2-flyway-constants` plugins extract the table and column names from database schema and generate a
source file containing constants whose values are the names. These constants can then be used in place of the
hardcoded strings to access the database. This avoids misspelling and makes renaming a straightforward refactoring
operation.

## Usage

The plugin is not currently released. If you would like a release, please open an issue to request one.

Overall, a database schema is specified, which is then loaded into an in-memory H2 database. The database is then
queried for the schema metadata, which is then used to generate the Java constants source file. The database schema
can be specified either using SQL files or [Flyway](https://flywaydb.org/) migration files. Select the plugin based
on how the schema will be specified:

| Schema Source | Plugin ID                      |
|---------------|--------------------------------|
| SQL           | org.cthing.h2-sql-constants    |
| Flyway        | org.cthing.h2-flyway-constants |

### Specifying Database Schema

To specify the database schema using SQL files, select the `org.cthing.h2-sql-constants` plugin and specify
the files using the `sources` method of the `generateH2SqlConstants` task. Additional methods are provided
for including and excluding files based on glob patterns.

```kotlin
plugins {
    java
    id("org.cthing.h2-sql-constants")
}

tasks {
    generateH2SqlConstants {
        classname = "org.cthing.test.DBConstants"
        sources(file("schema1.sql"), file("schema2.sql"))
    }
}
```

To specify the database schema using Flyway migration files, select the `org.cthing.h2-flyway-constants`
plugin and specify the directories containing the migration files using the `locations` method of the
`generateH2FlywayConstants` task.

```kotlin
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
```

In both cases, a Java source file will be generated with the specified `classname` and containing the names of
the tables, nested classes for each table and within those the constants for the column names.

```java
//
// DO NOT EDIT - Automatically generated file
//

package org.cthing.test;

/**
 * Constants for database table names, column names, and character field lengths.
 */
@SuppressWarnings("all")
public final class DBConstants {

    public static final String TBL_COMMON_TABLE1 = "COMMON.TABLE1";
    public static final class COMMON_TABLE1 {

        private COMMON_TABLE1() { }

        public static final String SCHEMA = "COMMON";

        public static final String COL_ID = "ID";
        public static final boolean COL_ID_NULLABLE = false;

        public static final String COL_VAL = "VAL";
        public static final boolean COL_VAL_NULLABLE = true;
        public static final int COL_VAL_SIZE = 128;

    }

    public static final String TBL_COMMON_TABLE2 = "COMMON.TABLE2";
    public static final class COMMON_TABLE2 {

        private COMMON_TABLE2() { }

        public static final String SCHEMA = "COMMON";

        public static final String COL_ID = "ID";
        public static final boolean COL_ID_NULLABLE = false;

        public static final String COL_MSG = "MSG";
        public static final boolean COL_MSG_NULLABLE = true;
        public static final int COL_MSG_SIZE = 40;

    }

    ...

    private DBConstants() { }
}
```

By default, all constants are public. All table names are prefixed with the name of the database schema
containing the table. Where applicable, constants are also provided indicating the nullability of the column
and the maximum size of character columns. The task provides properties for changing these defaults.

### Field Sizes Only

The task can be configured to provide only the maximum sizes of character columns.

```kotlin
tasks {
    generateH2SqlConstants {
        sizesOnly = true
        ...
    }
}
```

### Schema Name Prefix

By default, all table names are prefixed with the name of the database schema containing the table. The task
can be configured to omit the prefix.

```kotlin
tasks {
    generateH2SqlConstants {
        prefixWithSchema = false
        ...
    }
}
```

### File Per Schema

By default, the constants for all schema are generated into a single Java source file. The task can be
configured to generate a separate file for each schema.

```kotlin
tasks {
    generateH2SqlConstants {
        filePerSchema = true
        ...
    }
}
```

The files will be named with the specified class name with the schema name appended. For example, if the
specified class name is `DBConstants` and there are two schema, `Common` and `Other`, the resulting source
file names are `DBConstantsCommon.java` and `DBConstantsOther.java`.

### Schema Exclusion

By default, all detected schema will be used to generate constants. The task can be configured to exclude
specific schema.

```kotlin
tasks {
    generateH2SqlConstants {
        excludeSchema = listOf("Foo", "Bar")
        ...
    }
}
```

### Constants Access Modifier

By default, the generated constants and classes are given public access. The task can be configured to
generate the constants and classes with package private access.

```kotlin
import org.cthing.gradle.plugins.properties.SourceAccess

...

tasks {
    generateH2SqlConstants {
        sourceAccess = SourceAccess.PACKAGE
        ...
    }
}
```

### Output Directory

The default location for the generated constants source file is:
```
${project.layout.buildDirectory}/generated-src/h2-constants/${sourceSet.name}`
```
To change the location, configure the `outputDirectory` property on the task.

## Compatibility

The following Gradle and Java versions are supported:

| Plugin Version | Gradle Version | Minimum Java Version |
|----------------|----------------|----------------------|
| 2.x            | 8.3+           | 17                   |

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
