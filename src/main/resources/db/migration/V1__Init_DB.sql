alter table if exists payments_tariffs drop constraint if exists FKfu5658f1l4mtgyx0dcg99axvf
alter table if exists payments_tariffs drop constraint if exists FKaohjgoth0s3c0bxddtws9hdn8
alter table if exists tokens drop constraint if exists FK2dylsfo39lgjyqml2tbe0b0ss
alter table if exists user_roles drop constraint if exists FKh8ciramu9cc9q3qcqiv4ue8a6
alter table if exists user_roles drop constraint if exists FKhfh9dx7w3ubf1co1vdev94g3f
alter table if exists user_balances drop constraint if exists FKfw60yn584dehknpjjufbl72uo
drop table if exists payments cascade
drop table if exists payments_tariffs cascade
drop table if exists roles cascade
drop table if exists tariffs cascade
drop table if exists tokens cascade
drop table if exists user_roles cascade
drop table if exists user_balances cascade
drop table if exists users cascade
create table payments (id int8 generated by default as identity, electricity_sum varchar(255), electricity_value_current_month float8 not null, home_mates int4 not null, internet_sum varchar(255), internet_value_current_month float8 not null, payment_date timestamp, rent_rate_sum varchar(255), rent_sum varchar(255), water_cold_sum varchar(255), water_cold_value_current_month float8 not null, water_out_sum varchar(255), water_out_value_current_month float8 not null, water_warm_sum varchar(255), water_warm_value_current_month float8 not null, primary key (id))
create table payments_tariffs (payment_id int8 not null, tariff_id int8 not null, primary key (payment_id, tariff_id))
create table roles (id int4 generated by default as identity, role_name varchar(20), primary key (id))
create table tariffs (id int8 generated by default as identity, date_rate_change timestamp, electricity_rate varchar(255), internet_rate varchar(255), rent_rate varchar(255), water_cold_rate varchar(255), water_hot_rate varchar(255), primary key (id))
create table tokens (id int8 generated by default as identity, active boolean, creation_date timestamp, token varchar(500) not null, user_id int8, primary key (id))
create table user_roles (user_id int8 not null, role_id int4 not null, primary key (user_id, role_id))
create table user_balances (id int8 generated by default as identity, balance_date timestamp, balance_sum_of_balance varchar(255), user_id int8, primary key (id))
create table users (id int8 generated by default as identity, creation_date timestamp, password varchar(255), user_email varchar(255), user_name varchar(255), primary key (id))
alter table if exists tokens add constraint UK_na3v9f8s7ucnj16tylrs822qj unique (token)
alter table if exists payments_tariffs add constraint FKfu5658f1l4mtgyx0dcg99axvf foreign key (tariff_id) references tariffs
alter table if exists payments_tariffs add constraint FKaohjgoth0s3c0bxddtws9hdn8 foreign key (payment_id) references payments
alter table if exists tokens add constraint FK2dylsfo39lgjyqml2tbe0b0ss foreign key (user_id) references users
alter table if exists user_roles add constraint FKh8ciramu9cc9q3qcqiv4ue8a6 foreign key (role_id) references roles
alter table if exists user_roles add constraint FKhfh9dx7w3ubf1co1vdev94g3f foreign key (user_id) references users
alter table if exists user_balances add constraint FKfw60yn584dehknpjjufbl72uo foreign key (user_id) references users