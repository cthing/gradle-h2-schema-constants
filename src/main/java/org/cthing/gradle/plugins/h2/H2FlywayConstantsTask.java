/*
 * Copyright 2024 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cthing.gradle.plugins.h2;

import java.sql.Connection;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;


/**
 * Generates a Java class file containing constants representing the tables and columns in an H2 database. This task
 * performs a Flyway migration to populate the database.
 */
public class H2FlywayConstantsTask extends AbstractConstantsTask {

    private final ListProperty<String> locations;


    public H2FlywayConstantsTask() {
        this.excludeTables.add("flyway_schema_history");
        this.locations = getProject().getObjects().listProperty(String.class);

        onlyIf(task -> !this.locations.get().isEmpty());
    }

    /**
     * Obtains the directories to recursively scan for Flyway migration files.
     *
     * @return Directories to scan for Flyway migration files.
     */
    @Input
    public ListProperty<String> getLocations() {
        return this.locations;
    }

    /**
     * Obtains the Flyway migration files.
     *
     * @return Flyway migration files.
     */
    @InputFiles
    public FileTree getMigrationFiles() {
        return getProject().files(this.locations).getAsFileTree();
    }

    /**
     * Adds directories to recursively scan for Flyway migration files. Relative paths are resolved relative to
     * the project root directory.
     *
     * @param dirs  Directories to scan for Flyway migration files
     */
    public void locations(final String... dirs) {
        this.locations.addAll(dirs);
    }

    /**
     * Write the database constants class.
     */
    @TaskAction
    public void taskAction() {
        generateConstants();
    }

    @Override
    protected void loadDatabase(final Connection conn, final DataSource dataSource) {
        final Flyway flyway = Flyway.configure().dataSource(dataSource).locations(processLocations()).load();
        flyway.migrate();
    }

    private Location[] processLocations() {
        return this.locations.get().stream()
                             .map(locStr -> new Location(Location.FILESYSTEM_PREFIX + getProject().file(locStr)
                                                                                                  .getAbsolutePath()))
                             .toArray(Location[]::new);
    }
}
