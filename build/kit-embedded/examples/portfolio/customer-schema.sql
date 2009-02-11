CONNECT 'jdbc:derby://localhost:1527/teiid/accounts;create=true;';

DROP TABLE CUSTOMER;
CREATE TABLE CUSTOMER
(
   SSN char(10),
   FIRSTNAME varchar(64),
   LASTNAME varchar(64),
   ST_ADDRESS varchar(256),
   APT_NUMBER varchar(32),
   CITY varchar(64),
   STATE varchar(32),
   ZIPCODE varchar(10),
   PHONE varchar(15)
);

DROP TABLE ACCOUNT;
CREATE TABLE ACCOUNT
(
   ACCOUNT_ID integer,
   SSN char(10),
   STATUS char(10),
   TYPE char(10),
   DATEOPENED timestamp,
   DATECLOSED timestamp
);

DROP TABLE PRODUCT;
CREATE TABLE PRODUCT (
   ID integer,
   SYMBOL varchar(16),
   COMPANY_NAME varchar(256)
);

DROP TABLE HOLDINGS;
CREATE TABLE HOLDINGS
(
   TRANSACTION_ID integer GENERATED ALWAYS AS IDENTITY (start with 2000, increment by 1),
   ACCOUNT_ID integer,
   PRODUCT_ID integer,
   PURCHASE_DATE timestamp,
   SHARES_COUNT integer
);

INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01002','Joseph','Smith','1234 Main Street','Apartment 56','New York','New York','10174','(646)555-1776');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01003','Nicholas','Ferguson','202 Palomino Drive',null,'Pittsburgh','Pennsylvania','15071','(412)555-4327');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01004','Jane','Aire','15 State Street',null,'Philadelphia','Pennsylvania','19154','(814)555-6789');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01005','Charles','Jones','1819 Maple Street','Apartment 17F','Stratford','Connecticut','06614','(203)555-3947');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01006','Virginia','Jefferson','1710 South 51st Street','Apartment 3245','New York','New York','10175','(718)555-2693');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01007','Ralph','Bacon','57 Barn Swallow Avenue',null,'Charlotte','North Carolina','28205','(704)555-4576');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01008','Bonnie','Dragon','88 Cinderella Lane',null,'Jacksonville','Florida','32225','(904)555-6514');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01009','Herbert','Smith','12225 Waterfall Way','Building 100, Suite 9','Portland','Oregon','97220','(971)555-7803');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01015','Jack','Corby','1 Lone Star Way',null,'Dallas','Texas','75231','(469)555-8023');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01019','Robin','Evers','1814 Falcon Avenue',null,'Atlanta','Georgia','30355','(470)555-4390');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01020','Lloyd','Abercrombie','1954 Hughes Parkway',null,'Los Angeles','California','90099','(213)555-2312');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01021','Scott','Watters','24 Mariner Way',null,'Seattle','Washington','98124','(206)555-6790');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01022','Sandra','King','96 Lakefront Parkway',null,'Minneapolis','Minnesota','55426','(651)555-9017');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01027','Maryanne','Peters','35 Grand View Circle','Apartment 5F','Cincinnati','Ohio','45232','(513)555-9067');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01034','Corey','Snyder','1760 Boston Commons Avenue','Suite 543','Boston','Massachusetts','02136 ','(617)555-3546');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01035','Henry','Thomas','345 Hilltop Parkway',null,'San Francisco','California','94129','(415)555-2093');
INSERT INTO CUSTOMER (SSN,FIRSTNAME,LASTNAME,ST_ADDRESS,APT_NUMBER,CITY,STATE,ZIPCODE,PHONE) VALUES ('CST01036','James','Drew','876 Lakefront Lane',null,'Cleveland','Ohio','44107','(216)555-6523');


INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (19980002,'CST01002','Personal  ','Active    ',{ts '1998-02-01 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (19980003,'CST01003','Personal  ','Active    ',{ts '1998-03-06 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (19980004,'CST01004','Personal  ','Active    ',{ts '1998-03-07 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (19980005,'CST01005','Personal  ','Active    ',{ts '1998-06-15 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (19980006,'CST01006','Personal  ','Active    ',{ts '1998-09-15 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (19990007,'CST01007','Personal  ','Active    ',{ts '1999-01-20 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (19990008,'CST01008','Personal  ','Active    ',{ts '1999-04-16 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (19990009,'CST01009','Business  ','Active    ',{ts '1999-06-25 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20000015,'CST01015','Personal  ','Closed    ',{ts '2000-04-20 00:00:00.000'},{ts '2001-06-22 00:00:00.000'});
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20000019,'CST01019','Personal  ','Active    ',{ts '2000-10-08 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20000020,'CST01020','Personal  ','Active    ',{ts '2000-10-20 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20000021,'CST01021','Personal  ','Active    ',{ts '2000-12-05 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20010022,'CST01022','Personal  ','Active    ',{ts '2001-01-05 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20010027,'CST01027','Personal  ','Active    ',{ts '2001-08-22 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20020034,'CST01034','Business  ','Active    ',{ts '2002-01-22 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20020035,'CST01035','Personal  ','Active    ',{ts '2002-02-12 00:00:00.000'},null);
INSERT INTO ACCOUNT (ACCOUNT_ID,SSN,STATUS,TYPE,DATEOPENED,DATECLOSED) VALUES (20020036,'CST01036','Personal  ','Active    ',{ts '2002-03-22 00:00:00.000'},null);

INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1002,'BA','The Boeing Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1003,'MON','Monsanto Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1004,'ORCL','Oracle Corporation');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1005,'SY','Sybase Incorporated');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1006,'MSFT','Microsoft Corporation');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1007,'IBM','International Business Machines Corporation');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1008,'DELL','Dell Computer Corporation');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1010,'HPQ','Hewlett-Packard Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1011,'GTW','Gateway, Incorporated');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1012,'GE','General Electric Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1013,'MRK','Merck and Company Incorporated');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1014,'DIS','Walt Disney Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1015,'MCD','McDonalds Corporation');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1016,'DOW','Dow Chemical Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1018,'GM','General Motors Corporation');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1022,'JAVA','Sun Microsystems Incorporated');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1024,'SBGI','Sinclair Broadcast Group Incorporated');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1025,'COLM','Columbia Sportsware Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1026,'COLB','Columbia Banking System Incorporated');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1028,'BSY','British Sky Broadcasting Group PLC');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1029,'CSVFX','Columbia Strategic Value Fund');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1030,'CMTFX','Columbia Technology Fund');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1031,'F','Ford Motor Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1033,'FCZ','Ford Motor Credit Company');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1034,'SAP','SAP AG');
INSERT INTO PRODUCT (ID,SYMBOL,COMPANY_NAME) VALUES(1036,'TM','Toyota Motor Corporation');

INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980002,1008,{ts '1998-02-01 00:00:00.000'},50);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980002,1036,{ts '1998-02-01 00:00:00.000'},25);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980003,1002,{ts '1998-03-06 00:00:00.000'},100);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980003,1029,{ts '1998-03-06 00:00:00.000'},25);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980003,1016,{ts '1998-03-06 00:00:00.000'},51);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980004,1011,{ts '1998-03-07 00:00:00.000'},30);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980005,1024,{ts '1998-06-15 00:00:00.000'},18);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980006,1033,{ts '1998-09-15 00:00:00.000'},200);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19990007,1031,{ts '1999-01-20 00:00:00.000'},65);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19990008,1012,{ts '1999-04-16 00:00:00.000'},102);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19990007,1008,{ts '1999-05-11 00:00:00.000'},85);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19990008,1005,{ts '1999-05-21 00:00:00.000'},105);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19990009,1004,{ts '1999-06-25 00:00:00.000'},120);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980003,1020,{ts '1999-07-22 00:00:00.000'},150);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000015,1018,{ts '2000-04-20 00:00:00.000'},135);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980006,1030,{ts '2000-06-12 00:00:00.000'},91);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000019,1029,{ts '2000-10-08 00:00:00.000'},351);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000020,1030,{ts '2000-10-20 00:00:00.000'},127);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000020,1018,{ts '2000-11-14 00:00:00.000'},100);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000019,1031,{ts '2000-11-15 00:00:00.000'},125);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000021,1028,{ts '2000-12-05 00:00:00.000'},400);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20010022,1006,{ts '2001-01-05 00:00:00.000'},237);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19990008,1015,{ts '2001-01-23 00:00:00.000'},180);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980005,1025,{ts '2001-03-23 00:00:00.000'},125);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20010027,1020,{ts '2001-08-22 00:00:00.000'},70);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000020,1006,{ts '2001-11-14 00:00:00.000'},125);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980003,1029,{ts '2001-11-15 00:00:00.000'},100);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000021,1011,{ts '2001-12-18 00:00:00.000'},44);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20010027,1022,{ts '2001-12-19 00:00:00.000'},115);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20020034,1024,{ts '2002-01-22 00:00:00.000'},189);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19990009,1022,{ts '2002-01-24 00:00:00.000'},30);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20020035,1013,{ts '2002-02-12 00:00:00.000'},110);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20020035,1034,{ts '2002-02-13 00:00:00.000'},70);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20020034,1003,{ts '2002-02-22 00:00:00.000'},25);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000019,1013,{ts '2002-02-26 00:00:00.000'},195);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980004,1007,{ts '2002-03-05 00:00:00.000'},250);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20000021,1014,{ts '2002-03-12 00:00:00.000'},300);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20010027,1024,{ts '2002-03-14 00:00:00.000'},136);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20020036,1012,{ts '2002-03-22 00:00:00.000'},54);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (20020036,1010,{ts '2002-03-26 00:00:00.000'},189);
INSERT INTO HOLDINGS (ACCOUNT_ID,PRODUCT_ID,PURCHASE_DATE,SHARES_COUNT) VALUES (19980005,1010,{ts '2002-04-01 00:00:00.000'},26);


COMMIT;

DISCONNECT CURRENT;

