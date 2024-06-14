/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.h2;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.cthing.annotations.AccessForTesting;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskExecutionException;
import org.h2.jdbcx.JdbcDataSource;


/**
 * Base class for all H2 constants tasks.
 */
public abstract class AbstractConstantsTask extends DefaultTask {

    private static final String[] TABLE_TYPES = { "TABLE", "VIEW" };
    private static final int TABLE_RS_SCHEMA_NAME = 2;
    private static final int TABLE_RS_TABLE_NAME = 3;
    private static final int COLUMN_RS_NAME = 4;
    private static final int COLUMN_RS_TYPE = 5;
    private static final int COLUMN_RS_SIZE = 7;
    private static final int COLUMN_RS_NULLABLE = 11;
    private static final Pattern WORD_REGEX = Pattern.compile("[\\W_\\-]+|(?<=\\p{Ll})(?=\\p{Lu})");
    private static final Logger LOGGER = Logging.getLogger(AbstractConstantsTask.class);

    protected final Set<String> excludeTables = new HashSet<>();

    private final Property<Boolean> sizesOnly;
    private final Property<Boolean> prefixWithSchema;
    private final Property<Boolean> filePerSchema;
    private final SetProperty<String> excludeSchema;
    private final Property<String> classname;
    private final DirectoryProperty outputDirectory;
    private final Property<SourceAccess> sourceAccess;

    protected AbstractConstantsTask() {
        setGroup("Generate Constants");

        final ObjectFactory objects = getProject().getObjects();
        this.sizesOnly = objects.property(Boolean.class).convention(Boolean.FALSE);
        this.prefixWithSchema = objects.property(Boolean.class).convention(Boolean.TRUE);
        this.filePerSchema = objects.property(Boolean.class).convention(Boolean.FALSE);
        this.excludeSchema = objects.setProperty(String.class).convention(Set.of("INFORMATION_SCHEMA"));
        this.classname = objects.property(String.class);
        this.outputDirectory = objects.directoryProperty();
        this.sourceAccess = objects.property(SourceAccess.class).convention(SourceAccess.PUBLIC);

        onlyIf(task -> !StringUtils.isBlank(this.classname.get()));
    }

    /**
     * Obtains the fully qualified name for the generated class (e.g. com.cthing.myapp.PropertyConstants).
     *
     * @return Fully qualified class name.
     */
    @Input
    public Property<String> getClassname() {
        return this.classname;
    }

    /**
     * Obtains the root location on the filesystem for the generated class.
     *
     * @return Location of the generated class.
     */
    @OutputDirectory
    public DirectoryProperty getOutputDirectory() {
        return this.outputDirectory;
    }

    /**
     * Indicates if constants should be generated only for the size of varchar fields.
     *
     * @return {@code true} if only size constants are generated. Default is {@code false}.
     */
    @Input
    public Property<Boolean> getSizesOnly() {
        return this.sizesOnly;
    }

    /**
     * Indicates if table names are prefixed with the name of the schema.
     *
     * @return {@code true} if table names are prefixed with the schema name. Default is {@code false}.
     */
    @Input
    public Property<Boolean> getPrefixWithSchema() {
        return this.prefixWithSchema;
    }

    /**
     * Indicates is a separate constants file is created for each schema in the database.
     *
     * @return {@code true} if a file is generated per schema. Default is {@code false}.
     */
    @Input
    public Property<Boolean> getFilePerSchema() {
        return this.filePerSchema;
    }

    /**
     * Names of schema to exclude from the output.
     *
     * @return Names of schema to exclude from the output.
     */
    @Input
    public SetProperty<String> getExcludeSchema() {
        return this.excludeSchema;
    }

    /**
     * Generates the file object representing a schema output file.
     *
     * @param schemaSuffix  Suffix to apply to the output filename to differentiate schemas.
     * @return Output file object
     */
    public Provider<RegularFile> outputFile(final String schemaSuffix) {
        final Provider<String> pathname = this.classname.map(cname -> cname.replace('.', '/') + schemaSuffix + ".java");
        return this.outputDirectory.file(pathname);
    }

    /**
     * Generates the file object representing a schema output file.
     *
     * @return Output file object
     */
    public Provider<RegularFile> outputFile() {
        return outputFile("");
    }

    /**
     * Obtains the access modifier for the generated constants. The default is {@link SourceAccess#PUBLIC}.
     *
     * @return Access modifier for the generated constants.
     */
    @Input
    public Property<SourceAccess> getSourceAccess() {
        return this.sourceAccess;
    }

    /**
     * Creates a temporary database, loads it with the schema and writes the constants.
     */
    protected void generateConstants() {
        // Create the temporary database into which the schema will be loaded.
        final String tempUrl = "jdbc:h2:mem:constants-" + UUID.randomUUID().toString().toLowerCase();

        // Connect to the database.
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl(tempUrl);
        dataSource.setUser("SA");
        dataSource.setPassword("");

        try (Connection conn = dataSource.getConnection()) {
            loadDatabase(conn, dataSource);
            writeConstants(conn);
        } catch (final IOException | SQLException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    /**
     * Load the database schema.
     *
     * @param conn  Open database connection to the database
     * @param dataSource  Data source for the database
     * @throws SQLException if there is an error processing the SQL
     * @throws IOException if there is an error writing the constants file
     */
    protected abstract void loadDatabase(Connection conn, DataSource dataSource) throws SQLException, IOException;

    /**
     * Writes the constants class file.
     *
     * @param conn  Database connection
     * @throws SQLException if there is an error processing the SQL
     * @throws IOException if there is an error writing the constants file
     */
    private void writeConstants(final Connection conn) throws SQLException, IOException {
        final DatabaseMetaData meta = conn.getMetaData();
        final String packageName = StringUtils.substringBeforeLast(this.classname.get(), ".");
        final String classBaseName = StringUtils.substringAfterLast(this.classname.get(), ".");
        final String modifier = this.sourceAccess.get() == SourceAccess.PUBLIC ? "public " : "";

        final File parentFile = outputFile().get().getAsFile().getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new GradleException("could not create directories " + parentFile);
            }
        }

        final Map<String, PrintWriter> writerMap = new HashMap<>();

        try {
            final ResultSet tableRS = meta.getTables(null, null, null, TABLE_TYPES);

            while (tableRS.next()) {
                final String tableName = tableRS.getString(TABLE_RS_TABLE_NAME);
                if (this.excludeTables.contains(tableName)) {
                    continue;
                }

                final String schemaName = tableRS.getString(TABLE_RS_SCHEMA_NAME);
                if (this.excludeSchema.get().contains(schemaName.toUpperCase())) {
                    continue;
                }
                final String schemaSuffix = this.filePerSchema.get() ? toCamelCase(schemaName) : "";

                PrintWriter writer = writerMap.get(schemaSuffix);
                if (writer == null) {
                    final File pathname = outputFile(schemaSuffix).get().getAsFile();
                    LOGGER.info("Writing database constants file: {}", pathname);
                    writer = new PrintWriter(pathname, StandardCharsets.UTF_8);
                    writerMap.put(schemaSuffix, writer);
                    writeFileStart(writer, packageName, classBaseName + schemaSuffix, modifier);
                }

                writeTableStart(writer, schemaName, tableName, modifier);

                final ResultSet columnRS = meta.getColumns(null, schemaName, tableName, "%");
                while (columnRS.next()) {
                    final String colName = columnRS.getString(COLUMN_RS_NAME);
                    final int colType = columnRS.getInt(COLUMN_RS_TYPE);
                    final int colSize = columnRS.getInt(COLUMN_RS_SIZE);
                    final boolean nullable = (columnRS.getInt(COLUMN_RS_NULLABLE) == DatabaseMetaData.columnNullable);
                    if (colSize > 0) {
                        writeColumn(writer, colName, colType, colSize, nullable, modifier);
                    }
                }

                writeTableEnd(writer);
            }
        } finally {
            writerMap.forEach((suffix, writer) -> {
                writeFileEnd(writer, classBaseName + suffix);
                writer.close();
            });
        }
    }

    private static void writeFileStart(final PrintWriter writer, final String packageName, final String className,
                                       final String modifier) {
        writer.print("""
                     //
                     // DO NOT EDIT - Automatically generated file
                     //

                     package %s;

                     /**
                      * Constants for database table names, column names, and character field lengths.
                      */
                     @SuppressWarnings("all")
                     %sfinal class %s {

                     """.formatted(packageName, modifier, className));
    }

    private static void writeFileEnd(final PrintWriter writer, final String className) {
        writer.print("""

                         private %s() { }
                     }
                     """.formatted(className));
    }

    private void writeTableStart(final PrintWriter writer, final String schemaName, final String tableName,
                                 final String modifier) {
        final String tableConstant = this.prefixWithSchema.get()
                                     ? schemaName.toUpperCase() + "_" + tableName.toUpperCase()
                                     : tableName.toUpperCase();
        final String qualifiedTableName = this.prefixWithSchema.get() ? schemaName + "." + tableName : tableName;

        if (!this.sizesOnly.get()) {
            writer.println(String.format("    %sstatic final String TBL_%s = \"%s\";", modifier, tableConstant,
                                         qualifiedTableName));
        }
        writer.println(String.format("    %sstatic final class %s {", modifier, tableConstant));
        writer.println();
        writer.println(String.format("        private %s() { }", tableConstant));
        writer.println();
        writer.println(String.format("        %sstatic final String SCHEMA = \"%s\";", modifier, schemaName));
        writer.println();
    }

    private static void writeTableEnd(final PrintWriter writer) {
        writer.print("""
                         }

                     """);
    }

    private void writeColumn(final PrintWriter writer, final String colName, final int colType, final int colSize,
                             final boolean nullable, final String modifier) {
        final String col = "COL_" + colName.toUpperCase();

        if (!this.sizesOnly.get()) {
            writer.println(String.format("        %sstatic final String %s = \"%s\";", modifier, col, colName));
            writer.println(String.format("        %sstatic final boolean %s_NULLABLE = %s;", modifier, col, nullable));
        }
        if (colType == Types.CHAR || colType == Types.VARCHAR) {
            writer.println(String.format("        %sstatic final int %s_SIZE = %d;", modifier, col, colSize));
        }
        if (!this.sizesOnly.get()) {
            writer.println();
        }
    }

    /**
     * Converts the specified string to CamelCase.
     * <pre>
     * h -> H
     * hello -> Hello
     * Hello -> Hello
     * HELLO -> Hello
     * hello_world -> HelloWorld
     * Hello_World -> HelloWorld
     * hello-world -> HelloWorld
     * hello.world -> HelloWorld
     * hello__world -> HelloWorld
     * HELLO_WORLD -> HelloWorld
     * HelloWorld -> HelloWorld
     * "" -> ""
     * - -> ""
     * _ -> ""
     * . -> ""
     * </pre>
     *
     * @param str  String to convert to camel case. Underscores are considered word separators.
     * @return Specified string converted to camel case.
     */
    @AccessForTesting
    @SuppressWarnings("Convert2streamapi")
    static String toCamelCase(final String str) {
        if (str.isEmpty()) {
            return str;
        }

        final String[] words = WORD_REGEX.split(str);
        if (words.length == 1) {
            final String word = words[0];
            return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
        }

        final StringBuilder builder = new StringBuilder();
        for (final String word : words) {
            builder.append(Character.toUpperCase(word.charAt(0)))
                   .append(word.substring(1).toLowerCase(Locale.ENGLISH));
        }
        return builder.toString();
    }
}
