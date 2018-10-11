/* SQL-script to sort the species list of the TransektCount Basic DB by section ID and species code */
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
  PRIMARY KEY(_id)
);

INSERT INTO counts (section_id, name, code)
  SELECT section_id, name, code
  FROM counts_old
  order by section_id, code;

DROP TABLE counts_old;  

  