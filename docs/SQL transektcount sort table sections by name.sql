/* SQL-script to sort the sections table of a TransektCount0.db without track data by name.
 */
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
  order by name;

DROP TABLE sections_old;  

  
