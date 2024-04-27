CREATE SCHEMA other;
SET SCHEMA other;

CREATE TABLE Table1 (
    id     INT NOT NULL,
    val    VARCHAR(128),
    PRIMARY KEY (id)
);

CREATE TABLE Table2 (
    id     LONG NOT NULL,
    msg    VARCHAR(40),
    PRIMARY KEY(id)
);
