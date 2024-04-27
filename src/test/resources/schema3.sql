SET SCHEMA common;

CREATE TABLE Table5 (
    id     INT NOT NULL,
    str    VARCHAR(20),
    PRIMARY KEY (id)
);

GRANT ALL ON Table5 TO TestUser;
