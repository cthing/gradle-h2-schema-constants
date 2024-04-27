SET SCHEMA common;
CREATE DOMAIN t_title AS VARCHAR(60);

CREATE TABLE Table3 (
    id     INT NOT NULL,
    str    VARCHAR(20),
    PRIMARY KEY (id)
);

CREATE TABLE Table4 (
    id     LONG NOT NULL,
    title  t_title,
    PRIMARY KEY(id)
);
