/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.h2;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.h2.tools.RunScript;


/**
 * Generates a Java class file containing constants representing the tables and columns in an H2 database. This task
 * takes SQL files to populate the database.
 */
public abstract class H2SqlConstantsTask extends ConstantsTaskBase {

    private static final Logger LOGGER = Logging.getLogger(H2SqlConstantsTask.class);

    private final ListProperty<Object> source;
    private final PatternSet patternSet;


    public H2SqlConstantsTask() {
        this.source = getProject().getObjects().listProperty(Object.class);
        this.patternSet = new PatternSet();
    }

    /**
     * Returns the SQL schema files for this task, after the "include" and "exclude" patterns have been applied.
     * Ignores source files which do not exist.
     *
     * <p>The {@link PathSensitivity} for the sources is configured to be {@link PathSensitivity#ABSOLUTE}.
     * If your sources are less strict, please change it accordingly by overriding this method in your subclass.
     *
     * @return The SQL schema files.
     */
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public FileTree getSource() {
        final FileTree src = getProject().files(this.source).getAsFileTree();
        return src.matching(this.patternSet);
    }

    /**
     * Adds SQL schema files. The given source objects will be evaluated as per
     * {@link org.gradle.api.Project#files(Object...)}.
     *
     * @param sources  The SQL schema files to add
     */
    public void sources(final Object... sources) {
        this.source.addAll(sources);
    }

    /**
     * Returns the set of include patterns.
     *
     * @return The "include" patterns or an empty set when there are no include patterns.
     */
    @Input
    @Optional
    public Set<String> getIncludes() {
        return this.patternSet.getIncludes();
    }

    /**
     * Returns the set of exclude patterns.
     *
     * @return The "exclude" patterns or an empty set when there are no exclude patterns.
     */
    @Input
    @Optional
    public Set<String> getExcludes() {
        return this.patternSet.getExcludes();
    }

    /**
     * Set the allowable include patterns.  Note that unlike {@link #include(Iterable)} this replaces any previously
     * defined includes.
     *
     * @param includes  an Iterable providing new include patterns
     * @return This task
     * @see PatternFilterable Pattern Format
     */
    public H2SqlConstantsTask setIncludes(final Iterable<String> includes) {
        this.patternSet.setIncludes(includes);
        return this;
    }

    /**
     * Set the allowable exclude patterns.  Note that unlike {@link #exclude(Iterable)} this replaces any previously
     * defined excludes.
     *
     * @param excludes  an Iterable providing new exclude patterns
     * @return This task
     * @see PatternFilterable Pattern Format
     */
    public H2SqlConstantsTask setExcludes(final Iterable<String> excludes) {
        this.patternSet.setExcludes(excludes);
        return this;
    }

    /**
     * Adds an ANT style include pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * <p>If includes are not provided, then all files in this container will be included. If includes are provided,
     * then a file must match at least one of the include patterns to be processed.
     *
     * @param includes  a vararg list of include patterns
     * @return This task
     * @see PatternFilterable Pattern Format
     */
    public H2SqlConstantsTask include(final String... includes) {
        this.patternSet.include(includes);
        return this;
    }

    /**
     * Adds an ANT style include pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * <p>If includes are not provided, then all files in this container will be included. If includes are provided,
     * then a file must match at least one of the include patterns to be processed.
     *
     * @param includes  a Iterable providing more include patterns
     * @return This task
     * @see PatternFilterable Pattern Format
     */
    public H2SqlConstantsTask include(final Iterable<String> includes) {
        this.patternSet.include(includes);
        return this;
    }

    /**
     * Adds an "include" spec. This method may be called multiple times to append new specs.
     *
     * <p>If includes are not provided, then all files in this container will be included. If includes are provided,
     * then a file must match at least one of the "include" patterns or specs to be included.
     *
     * @param includeSpec  the spec to add
     * @return This task
     * @see PatternFilterable Pattern Format
     */
    public H2SqlConstantsTask include(final Spec<FileTreeElement> includeSpec) {
        this.patternSet.include(includeSpec);
        return this;
    }

    /**
     * Adds an ANT style exclude pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * <p>If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not
     * match any exclude pattern to be processed.
     *
     * @param excludes  a vararg list of exclude patterns
     * @return This task
     * @see PatternFilterable Pattern Format
     */
    public H2SqlConstantsTask exclude(final String... excludes) {
        this.patternSet.exclude(excludes);
        return this;
    }

    /**
     * Adds an ANT style exclude pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * <p>If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not
     * match any exclude pattern to be processed.
     *
     * @param excludes  Iterable providing new exclude patterns
     * @return This task
     * @see PatternFilterable Pattern Format
     */
    public H2SqlConstantsTask exclude(final Iterable<String> excludes) {
        this.patternSet.exclude(excludes);
        return this;
    }

    /**
     * Adds an "exclude" spec. This method may be called multiple times to append new specs.
     *
     * <p>If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not
     * match any exclude pattern to be processed.
     *
     * @param excludeSpec  the spec to add
     * @return This task
     * @see PatternFilterable Pattern Format
     */
    public H2SqlConstantsTask exclude(final Spec<FileTreeElement> excludeSpec) {
        this.patternSet.exclude(excludeSpec);
        return this;
    }

    /**
     * Write the database constants class.
     */
    @TaskAction
    public void taskAction() {
        generateConstants();
    }

    @Override
    protected void loadDatabase(final Connection conn, final DataSource dataSource) throws SQLException, IOException {
        for (final File scriptFile : getSource().getFiles()) {
            LOGGER.info("Loading the database with {}", scriptFile);
            try (Reader reader = Files.newBufferedReader(scriptFile.toPath(), StandardCharsets.UTF_8)) {
                RunScript.execute(conn, reader);
            }
        }
    }
}
