create sequence orderblock_id_seq;

create table order_block (
	id long not null default nextval('orderblock_id_seq'),
	blockDate timestamp, 
	sellOrBuy varchar2(1), 
	security varchar2(10), 
	quantity decimal, 
	currency varchar2(10), 
	price integer,
  highPrice decimal, 
  lowPrice decimal, 
  closePrice decimal 
);

grant select, insert, update, delete on order_block to bvapp;
