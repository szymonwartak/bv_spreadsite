CREATE SEQUENCE users_id_seq;

create table users (
	id long not null default nextval('users_id_seq'),
	firstname varchar2(32),
	lastname varchar2(32),
	email varchar2(48),
	locale varchar2(16),
	timezone varchar2(32),
	password_pw varchar2(48),
	password_slt varchar2(20),
	textarea varchar2(2048),
	superuser boolean,
	validated boolean,
	uniqueid varchar2(32)
);

grant select, insert, update, delete on users to bvapp;
