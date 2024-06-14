/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.h2;

/**
 * Generates a Java class file containing constants representing the name of tables and columns in
 * an H2 Flyway schema.
 */
public class H2FlywayConstantsPlugin extends AbstractConstantsPlugin {

    @Override
    protected Class<? extends AbstractConstantsTask> getTaskClass() {
        return H2FlywayConstantsTask.class;
    }

    @Override
    protected String getTaskTarget() {
        return "H2FlywayConstants";
    }
}
