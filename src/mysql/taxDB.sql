##database to store a mirror of NCBI taxonomy database

DROP TABLE IF EXISTS deleted_nodes;
DROP TABLE IF EXISTS merged_nodes;
DROP TABLE IF EXISTS names;
DROP TABLE IF EXISTS nodes;
DROP TABLE IF EXISTS divisions;



CREATE TABLE divisions (
  division_id smallint NOT NULL,
  division_code varchar(3) NULL,
  division_name varchar(25) NULL,
  comments varchar(75) default NULL,
  PRIMARY KEY  (division_id)
);


CREATE TABLE deleted_nodes (
  tax_id int NOT NULL,
  PRIMARY KEY  (tax_id)
);


CREATE TABLE nodes (
  tax_id int NOT NULL,
  parent_tax_id int NOT NULL,
  rank varchar(100) NOT NULL default "no rank",
 -- embl_code varchar(2) default NULL, 
  division_code varchar(5) default NULL,
  inherited_div boolean default NULL,
  genetic_code_id smallint default NULL,
 -- inherited_gc boolean default NULL,
 -- mitochondrial_gc smallint default NULL,
 -- inherited_mgc boolean default NULL,
 -- genbank_hidden boolean default NULL,
 -- hidden_subtree_root boolean default NULL,
 -- comments text default NULL,
 -- scientific_name varchar(200) default NULL,
  PRIMARY KEY  (tax_id)
 --  FOREIGN KEY (parent_tax_id) REFERENCES nodes(tax_id),
 -- FOREIGN KEY (division_code) REFERENCES divisions(division_code)
);

#-- --rank--rank of this node(superkingdom, kingdom etc.)
#-- --embl_code--locus-name prefix
#-- --division_id--see division.dmp
#-- --inherited_div--0 or 1; 1 if node inherits division from parent
#-- --genetic_code_id--see gencode.dmp
#-- --inherited_gc--inherited genetic code; 0 or 1; 1 if node inherits genetic code from parent
#-- --mitochondrial_gc--mitochondrial genetic code; see gencode.dmp
#-- --inherited_mgc--inherited mitochondrial genetic code; 0 or 1; 1 if node inherits mitochondrial genetic code from parent
#-- --genbank_hidden--1 if name is suppressed in GenBank entry lineage
#-- --hidden_subtree_root--1 if this subtree has no sequence data yet
#-- --comments--free-text comments and citations
#-- --scientific_name-- !!!this is not-normal!!! but chosen to include for reasons of query speed
#-- 
#-- --name--from name.dmp the node name
#-- --unique_name--from name.dmp the unique variant of the name
#-- --name_class--from name.dmp synonym, common name, ...

CREATE TABLE names (
  name_id int NOT NULL auto_increment,
  tax_id int NOT NULL,
  name varchar(250) NULL,
  unique_name varchar(250) NULL,
  name_class enum ('acronym', 'anamorph', 'authority', 'blast name', 'common name', 'equivalent name', 'genbank acronym', 'genbank anamorph', 'genbank common name', 'genbank synonym', 'in-part', 'includes', 'misnomer', 'misspelling', 'scientific name', 'synonym', 'teleomorph', 'type material'),
  PRIMARY KEY  (name_id),
  FOREIGN KEY (tax_id) REFERENCES nodes(tax_id)
);



CREATE TABLE merged_nodes (
  merge_id int NOT NULL auto_increment,
  old_tax_id int NOT NULL,
  new_tax_id int NOT NULL,
  PRIMARY KEY  (merge_id)
);

#--old_tax_id-- id of node which has been merged
#--new_tax_id-- id of node which is the result of the merge



DROP PROCEDURE IF EXISTS lineage;
	DELIMITER //

	CREATE PROCEDURE lineage( IN curr_tax_id INT )
	BEGIN
		DECLARE n_tax_id INT;
		DECLARE n_parent_tax_id INT;
		DECLARE n_rank varchar(100);
--		DECLARE n_division_id smallint;
--		DECLARE n_inherited_div boolean;
		DECLARE n_scientific_name varchar(200);
		
		DROP TEMPORARY TABLE IF EXISTS node_lineage;
		
		CREATE TEMPORARY TABLE IF NOT EXISTS node_lineage(
			`tax_id` int, `parent_tax_id` int, `rank` varchar(100), `scientific_name` varchar(200)
		);
		
		WHILE curr_tax_id != 1 DO
			
			SELECT nodes.tax_id, nodes.parent_tax_id, nodes.rank, names.name
 			INTO n_tax_id, n_parent_tax_id, n_rank, n_scientific_name
			FROM nodes 
            LEFT JOIN names ON nodes.tax_id=names.tax_id 
            WHERE nodes.tax_id = curr_tax_id AND names.name_class = "scientific name";
			
			INSERT INTO node_lineage (tax_id, parent_tax_id, rank, scientific_name) VALUES (n_tax_id, n_parent_tax_id, n_rank, n_scientific_name);
			
			SELECT n_parent_tax_id INTO curr_tax_id;
		END WHILE;


		SELECT nodes.tax_id, nodes.parent_tax_id, nodes.rank, names.name
		INTO n_tax_id, n_parent_tax_id, n_rank, n_scientific_name
		FROM nodes 
		LEFT JOIN names ON nodes.tax_id=names.tax_id 
		WHERE nodes.tax_id = curr_tax_id AND names.name_class = "scientific name";
		
		INSERT INTO node_lineage (tax_id, parent_tax_id, rank, scientific_name) VALUES (n_tax_id, n_parent_tax_id, n_rank, n_scientific_name);

		SELECT * FROM node_lineage;
	END //
	DELIMITER ;
	

/*
DROP PROCEDURE IF EXISTS gi_lineage;
DELIMITER //

CREATE PROCEDURE gi_lineage( IN q_gi_number INT )
BEGIN
	DECLARE n_tax_id INT;
	DECLARE n_parent_tax_id INT;
	DECLARE n_rank varchar(100);
	DECLARE n_division_id smallint;
	DECLARE n_inherited_div boolean;
	DECLARE n_scientific_name varchar(200);
	DECLARE curr_tax_id INT;
	
	DROP TEMPORARY TABLE IF EXISTS node_lineage;
	
	CREATE TEMPORARY TABLE IF NOT EXISTS node_lineage(
		`tax_id` int, `parent_tax_id` int, `rank` varchar(100), `division_id` smallint, `inherited_div` boolean, `scientific_name` varchar(200)
	);
	
	SELECT tax_id INTO curr_tax_id FROM gi_numbers WHERE gi_number = q_gi_number;
	
	WHILE curr_tax_id != 1 DO
		
		SELECT tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name
		INTO n_tax_id, n_parent_tax_id, n_rank, n_division_id, n_inherited_div, n_scientific_name
		FROM nodes WHERE tax_id = curr_tax_id;
		
		INSERT INTO node_lineage (tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name) VALUES (n_tax_id, n_parent_tax_id, n_rank, n_division_id, n_inherited_div, n_scientific_name);
		
		SELECT parent_tax_id INTO curr_tax_id FROM nodes WHERE tax_id = curr_tax_id;
	END WHILE;
	
	SELECT tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name
	INTO n_tax_id, n_parent_tax_id, n_rank, n_division_id, n_inherited_div, n_scientific_name
	FROM nodes WHERE tax_id = curr_tax_id;
	
	INSERT INTO node_lineage (tax_id, parent_tax_id, rank, division_id, inherited_div, scientific_name) VALUES (n_tax_id, n_parent_tax_id, n_rank, n_division_id, n_inherited_div, n_scientific_name);
	
	SELECT * FROM node_lineage;
END //
DELIMITER ;
	
*/
