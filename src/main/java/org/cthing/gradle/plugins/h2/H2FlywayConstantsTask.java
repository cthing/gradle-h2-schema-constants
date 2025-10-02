/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.h2;

import java.sql.Connection;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;


/**
 * Generates a Java class file containing constants representing the tables and columns in an H2 database. This task
 * performs a Flyway migration to populate the database.
 */
public abstract class H2FlywayConstantsTask extends ConstantsTaskBase {

    @Inject
    public H2FlywayConstantsTask(final ObjectFactory objectFactory, final ProjectLayout layout) {
        super(objectFactory, layout);

        this.excludeTables.add("flyway_schema_history");

        onlyIf(task -> !getLocations().get().isEmpty());
    }

    /**
     * Obtains the directories to recursively scan for Flyway migration files.
     *
     * @return Directories to scan for Flyway migration files.
     */
    @Input
    public abstract ListProperty<String> getLocations();

    /**
     * Obtains the Flyway migration files.
     *
     * @return Flyway migration files.
     */
    @InputFiles
    public FileTree getMigrationFiles() {
        return this.objectFactory.fileCollection().from(getLocations()).getAsFileTree();
    }

    /**
     * Adds directories to recursively scan for Flyway migration files. Relative paths are resolved relative to
     * the project root directory.
     *
     * @param dirs  Directories to scan for Flyway migration files
     */
    public void locations(final String... dirs) {
        getLocations().addAll(dirs);
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
        return getLocations().get().stream()
                             .map(locStr -> this.layout.getProjectDirectory().file(locStr).getAsFile().getAbsolutePath())
                             .map(absPath -> new Location(Location.FILESYSTEM_PREFIX + absPath))
                             .toArray(Location[]::new);
    }
}
