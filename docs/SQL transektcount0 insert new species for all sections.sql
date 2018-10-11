/* Insert a new species (e.g. Cossus cossus, 04151) for all 14 sections in transectcount0 db */

PRAGMA temp_store = 2;
CREATE TEMP TABLE _Variables(sid TEXT PRIMARY KEY, sname TEXT, scode TEXT);

INSERT INTO _Variables (sid) VALUES ('1'); 

/* Exchange new name and code for Cossus cossus and 04151 in the next line */
UPDATE _Variables SET sname = 'Cossus cossus', scode = '04151' WHERE sid = '1';

/* Adapt the number of VALUES lines for the number of your transect sections (14 here, mind last line ends with ';') */
INSERT INTO counts (section_id, name, code, count_f1i, count_f2i, count_f3i, count_pi, count_li, count_ei, 
count_f1e, count_f2e, count_f3e, count_pe, count_le, count_ee, notes)

  VALUES (1, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(2, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(3, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(4, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(5, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(6, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(7, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(8, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(9, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(10, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(11, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(12, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(13, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ""),
	(14, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "");

	DROP TABLE _Variables;

/* Sort the counts table by section_id and code */
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
