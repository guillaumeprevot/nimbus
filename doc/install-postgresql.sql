CREATE ROLE postgres WITH
	LOGIN
	NOSUPERUSER
	NOCREATEDB
	NOCREATEROLE
	INHERIT
	NOREPLICATION
	CONNECTION LIMIT -1
	PASSWORD 'postgres';

CREATE DATABASE nimbus
	WITH 
	OWNER = postgres
	ENCODING = 'UTF8'
	LC_COLLATE = 'French_France.1252'
	LC_CTYPE = 'French_France.1252'
	TABLESPACE = pg_default
	CONNECTION LIMIT = -1;

