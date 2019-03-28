/* Insert a new species (e.g. Cossus cossus, Weidenbohrer, 04151) for all (e.g. 14) sections in transectcount0 db */
/* Read the --> Attention! lines and modify the script appropriately */

PRAGMA temp_store = 2;
CREATE TEMP TABLE _Variables(sid TEXT PRIMARY KEY, sname TEXT, scode TEXT, sname_g TEXT);

INSERT INTO _Variables (sid) VALUES ('1'); 

/* --> Attention! Substitute your species name, local name and code for Cossus cossus, Weidenbohrer and 04151 in the next line */
UPDATE _Variables SET sname = 'Cossus cossus', sname_g = 'Weidenbohrer', scode = '04151' WHERE sid = '1';

/* --> Attention! Adapt the number of VALUES lines for the number of your transect sections (14 here, mind last line ends with ';') */
INSERT INTO counts (section_id, name, code, count_f1i, count_f2i, count_f3i, count_pi, count_li, count_ei, 
count_f1e, count_f2e, count_f3e, count_pe, count_le, count_ee, notes, name_g)

  VALUES 
	(1, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(2, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(3, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(4, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(5, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(6, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(7, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(8, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(9, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(10, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(11, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(12, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(13, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1')),
	(14, (SELECT sname FROM _Variables WHERE sid = '1'), (SELECT scode FROM _Variables WHERE sid = '1'), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", (SELECT sname_g FROM _Variables WHERE sid = '1'));

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
  name_g text DEFAULT "",
  PRIMARY KEY(_id)
);

INSERT INTO counts (section_id, name, code, name_g)
  SELECT section_id, name, code, name_g
  FROM counts_old
  order by section_id, code;

DROP TABLE counts_old;
