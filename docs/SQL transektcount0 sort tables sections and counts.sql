/* 
 * SQL-script to sort TABLE sections of a multi-sections TransektCount.db by _id of sections
 * and Table counts by section_id and species code.
 */
 
/* Step 1: Sort TABLE sections by name */
ALTER TABLE sections RENAME TO sections_old;

CREATE TABLE sections
( _id INTEGER,
  created_at int,
  name text,
  notes text,
  PRIMARY KEY(_id)
);

INSERT INTO sections (created_at, name, notes)
  SELECT created_at, name, notes
  FROM sections_old
  order by _id;

DROP TABLE sections_old;

/* Step 2: Sort TABLE counts by section_id and code */
ALTER TABLE counts RENAME TO counts_old;

CREATE TABLE counts
( _id INTEGER,
  section_id int DEFAULT 1,
  name text,
  code text,
  count_f1i int DEFAULT 0,
  count_f2i int DEFAULT 0,
  count_f3i int DEFAULT 0,
  count_pi int DEFAULT 0,
  count_li int DEFAULT 0,
  count_ei int DEFAULT 0,
  count_f1e int DEFAULT 0,
  count_f2e int DEFAULT 0,
  count_f3e int DEFAULT 0,
  count_pe int DEFAULT 0,
  count_le int DEFAULT 0,
  count_ee int DEFAULT 0,
  notes text DEFAULT "",
  name_g text DEFAULT "",
  PRIMARY KEY(_id)
);

INSERT INTO counts (section_id, name, code, name_g)
  SELECT section_id, name, code, name_g
  FROM counts_old
  order by section_id, code;

DROP TABLE counts_old;  
