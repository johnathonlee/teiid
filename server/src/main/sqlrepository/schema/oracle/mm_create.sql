--
-- 7.1.7.21
-- BUILD SCRIPT
--                      RDBMS: ORACLE
--                SERVER NAME:
--                     SCHEMA: 4.0
--          DATABASE REVISION:
--   CLIENT SOFTWARE REVISION:
--            SNAPSHOT AUTHOR:
--          PHYSICAL LOCATION:
--              RDBMS VERSION: ORACLE
--                       NOTE:
--

CREATE TABLE AUDITENTRIES
(
  TIMESTAMP  VARCHAR2(50) NOT NULL,
  CONTEXT    VARCHAR2(64) NOT NULL,
  ACTIVITY   VARCHAR2(64) NOT NULL,
  RESOURCES  VARCHAR2(4000) NOT NULL,
  PRINCIPAL  VARCHAR2(255) NOT NULL,
  HOSTNAME   VARCHAR2(64) NOT NULL,
  VMID       VARCHAR2(64) NOT NULL
);

CREATE TABLE AUTHPERMTYPES
(
  PERMTYPEUID       NUMBER(10) NOT NULL CONSTRAINT PK_AUTHPERMYPES UNIQUE,
  DISPLAYNAME       VARCHAR2(250) NOT NULL,
  FACTORYCLASSNAME  VARCHAR2(80) NOT NULL
);

CREATE TABLE AUTHPOLICIES
(
  POLICYUID    NUMBER(10) NOT NULL CONSTRAINT PK_AUTHPOLICIES UNIQUE,
  DESCRIPTION  VARCHAR2(250),
  POLICYNAME   VARCHAR2(250) NOT NULL
);

CREATE TABLE AUTHPRINCIPALS
(
  PRINCIPALTYPE  NUMBER(10) NOT NULL,
  PRINCIPALNAME  VARCHAR2(255) NOT NULL,
  POLICYUID      NUMBER(10) NOT NULL CONSTRAINT FK_ATHPLCY_PLCYUID REFERENCES AUTHPOLICIES (POLICYUID) ,
  GRANTOR        VARCHAR2(255) NOT NULL,
  CONSTRAINT PK_AUTHPOLICYPRINCIPALS UNIQUE (PRINCIPALNAME, POLICYUID)
);

CREATE TABLE AUTHREALMS
(
  REALMUID     NUMBER(10) NOT NULL CONSTRAINT PK_AUTHREALMS UNIQUE,
  REALMNAME    VARCHAR2(250) NOT NULL UNIQUE,
  DESCRIPTION  VARCHAR2(550)
);

CREATE TABLE IDTABLE
(
  IDCONTEXT  VARCHAR2(20) NOT NULL PRIMARY KEY,
  NEXTID     NUMBER
);

CREATE TABLE LOGMESSAGETYPES
(
  MESSAGELEVEL  NUMBER(10) NOT NULL CONSTRAINT PK_LOGMSGTYPS UNIQUE,
  NAME          VARCHAR2(64) NOT NULL,
  DISPLAYNAME   VARCHAR2(64)
);

CREATE TABLE PRINCIPALTYPES
(
  PRINCIPALTYPEUID  NUMBER(10) NOT NULL CONSTRAINT PK_PRNCPLTYPUID UNIQUE,
  PRINCIPALTYPE     VARCHAR2(60) NOT NULL,
  DISPLAYNAME       VARCHAR2(80) NOT NULL,
  LASTCHANGEDBY     VARCHAR2(255) NOT NULL,
  LASTCHANGED       VARCHAR2(50)
);


CREATE TABLE RT_MDLS
(
  MDL_UID           NUMBER(10) NOT NULL CONSTRAINT PK_MDLS UNIQUE,
  MDL_UUID          VARCHAR(64) NOT NULL,
  MDL_NM            VARCHAR2(255) NOT NULL,
  MDL_VERSION       VARCHAR2(50),
  DESCRIPTION       VARCHAR2(255),
  MDL_URI           VARCHAR2(255),
  MDL_TYPE          NUMBER(3),
  IS_PHYSICAL       CHAR(1) NOT NULL,
  MULTI_SOURCED     CHAR(1) DEFAULT ('0') NULL,  
  VISIBILITY      NUMBER(10)
  );

CREATE TABLE RT_MDL_PRP_NMS
(
  PRP_UID  NUMBER(10) NOT NULL CONSTRAINT PK_MDL_PRP_NMS UNIQUE,
  MDL_UID  NUMBER(10) NOT NULL ,
  PRP_NM   VARCHAR2(255) NOT NULL
);

CREATE TABLE RT_MDL_PRP_VLS
(
  PRP_UID  NUMBER(10) NOT NULL ,
  PART_ID  NUMBER(10) NOT NULL,
  PRP_VL   VARCHAR2(255) NOT NULL,
  CONSTRAINT PK_MDL_PRP_VLS UNIQUE (PRP_UID, PART_ID)
);


CREATE TABLE RT_VIRTUAL_DBS
(
  VDB_UID        NUMBER(10) NOT NULL CONSTRAINT PK_VIRT_DB UNIQUE,
  VDB_VERSION    VARCHAR2(50) NOT NULL,
  VDB_NM         VARCHAR2(255) NOT NULL,
  DESCRIPTION    VARCHAR2(255),
  PROJECT_GUID   VARCHAR2(64),
  VDB_STATUS     NUMBER NOT NULL,
  WSDL_DEFINED   CHAR(1) DEFAULT ('0') NULL,     
  VERSION_BY     VARCHAR2(100),
  VERSION_DATE   VARCHAR2(50) NOT NULL,
  CREATED_BY     VARCHAR2(100),
  CREATION_DATE  VARCHAR2(50),
  UPDATED_BY     VARCHAR2(100),
  UPDATED_DATE   VARCHAR2(50),
  VDB_FILE_NM VARCHAR(2048)
);

CREATE INDEX RTMDLS_NM_IX ON RT_MDLS (MDL_NM);

CREATE INDEX RTVIRTUALDBS_NM_IX ON RT_VIRTUAL_DBS (VDB_NM);

CREATE INDEX RTVIRTUALDBS_VRSN_IX ON RT_VIRTUAL_DBS (VDB_VERSION);

CREATE UNIQUE INDEX MDL_PRP_NMS_UIX ON RT_MDL_PRP_NMS (MDL_UID, PRP_NM);

CREATE UNIQUE INDEX PRNCIPALTYP_UIX ON PRINCIPALTYPES (PRINCIPALTYPE);

CREATE UNIQUE INDEX AUTHPOLICIES_NAM_UIX ON AUTHPOLICIES (POLICYNAME);

CREATE TABLE AUTHPERMISSIONS
(
  PERMISSIONUID    NUMBER(10) NOT NULL CONSTRAINT PK_AUTHPERMISSIONS UNIQUE,
  RESOURCENAME     VARCHAR2(250) NOT NULL,
  ACTIONS          NUMBER(10) NOT NULL,
  CONTENTMODIFIER  VARCHAR2(250),
  PERMTYPEUID      NUMBER(10) NOT NULL CONSTRAINT FK_ATHPRMS_ATHPERM REFERENCES AUTHPERMTYPES (PERMTYPEUID) ,
  REALMUID         NUMBER(10) NOT NULL CONSTRAINT FK_ATHPRMS_ATHRLMS REFERENCES AUTHREALMS (REALMUID) ,
  POLICYUID        NUMBER(10) NOT NULL CONSTRAINT FK_ATHPRMS_ATHPLCY REFERENCES AUTHPOLICIES (POLICYUID)
);


CREATE TABLE LOGENTRIES
(
  TIMESTAMP   VARCHAR2(50) NOT NULL,
  CONTEXT     VARCHAR2(64) NOT NULL,
  MSGLEVEL    NUMBER(10) NOT NULL CONSTRAINT FK_LOGENTRIES_MSGTYPES REFERENCES LOGMESSAGETYPES (MESSAGELEVEL) ,
  EXCEPTION   VARCHAR2(4000),
  MESSAGE     VARCHAR2(2000) NOT NULL,
  HOSTNAME    VARCHAR2(64) NOT NULL,
  VMID        VARCHAR2(64) NOT NULL,
  THREADNAME  VARCHAR2(64) NOT NULL,
  VMSEQNUM NUMBER(7) NOT NULL
);

CREATE TABLE RT_VDB_MDLS
(
  VDB_UID         NUMBER(10) NOT NULL ,
  MDL_UID         NUMBER(10) NOT NULL ,
  CNCTR_BNDNG_NM  VARCHAR2(255)
);

CREATE INDEX AWA_SYS_MSGLEVEL_1E6F845E ON LOGENTRIES (MSGLEVEL);

CREATE UNIQUE INDEX AUTHPERM_UIX ON AUTHPERMISSIONS ( POLICYUID, RESOURCENAME);

CREATE TABLE CS_EXT_FILES  (
   FILE_UID             INTEGER                          NOT NULL,
   CHKSUM               NUMBER(20),
   FILE_NAME            VARCHAR(255)		NOT NULL,
   FILE_CONTENTS        BLOB,
   CONFIG_CONTENTS	CLOB,
   SEARCH_POS           INTEGER,
   IS_ENABLED           CHAR(1),
   FILE_DESC            VARCHAR(4000),
   CREATED_BY           VARCHAR(100),
   CREATION_DATE        VARCHAR(50),
   UPDATED_BY           VARCHAR(100),
   UPDATE_DATE          VARCHAR(50),
   FILE_TYPE            VARCHAR(30),
   CONSTRAINT PK_CS_EXT_FILES PRIMARY KEY (FILE_UID)
)
;


COMMENT ON COLUMN CS_EXT_FILES.FILE_UID IS
'UNIQUE INTERNAL IDENTIFIER, NOT EXPOSED'
;


COMMENT ON COLUMN CS_EXT_FILES.FILE_NAME IS
'THE FILE NAME OF THE EXTENSION FILE'
;


COMMENT ON COLUMN CS_EXT_FILES.FILE_CONTENTS IS
'THE ACTUAL FILE BYTE[] ARRAY'
;


COMMENT ON COLUMN CS_EXT_FILES.SEARCH_POS IS
'THE SEARCH POSITION OF THE EXTENSION FILE - INDICATES THE ORDER THE SOURCES ARE SEARCHED'
;


COMMENT ON COLUMN CS_EXT_FILES.IS_ENABLED IS
'INDICATES WHETHER THE EXTENSION FILE IS ENABLED FOR SEARCH OR NOT'
;


COMMENT ON COLUMN CS_EXT_FILES.FILE_DESC IS
'THE DESCRIPTION FOR THE EXTENSION FILE'
;


COMMENT ON COLUMN CS_EXT_FILES.CREATED_BY IS
'NAME PRINCIPAL WHO CREATED THIS ENTRY'
;


COMMENT ON COLUMN CS_EXT_FILES.CREATION_DATE IS
'DATE OF CREATION'
;


COMMENT ON COLUMN CS_EXT_FILES.UPDATED_BY IS
'NAME OF PRINCIPAL WHO LAST UPDATED THIS ENTRY'
;


COMMENT ON COLUMN CS_EXT_FILES.UPDATE_DATE IS
'DATE OF LAST UPDATE'
;


COMMENT ON COLUMN CS_EXT_FILES.FILE_TYPE IS
'TYPE OF EXTENSION FILE (JAR FILE, XML USER-DEFINED FUNCTION METADATA FILE, ETC.)'
;


ALTER TABLE CS_EXT_FILES ADD CONSTRAINT CSEXFILS_FIL_NA_UK UNIQUE (FILE_NAME);

CREATE TABLE TX_MMXCMDLOG
(
	REQUESTID  VARCHAR(255)  NOT NULL,
	TXNUID  VARCHAR(50)  NULL,
	CMDPOINT  NUMERIC(10)  NOT NULL,
	SESSIONUID  VARCHAR(255)  NOT NULL,
	APP_NAME  VARCHAR(255)  NULL,
	PRINCIPAL_NA  VARCHAR(255)  NOT NULL,
	VDBNAME  VARCHAR(255)  NOT NULL,
	VDBVERSION  VARCHAR(50)  NOT NULL,
	CREATED_TS  VARCHAR(50)  NULL,
	ENDED_TS  VARCHAR(50)  NULL,
	CMD_STATUS  NUMERIC(10)  NOT NULL,
	SQL_ID  NUMERIC(10) NULL,
	FINL_ROWCNT  NUMERIC(10) NULL
)
;

CREATE TABLE TX_SRCCMDLOG
(
	REQUESTID  VARCHAR(255)  NOT NULL,
	NODEID  NUMERIC(10)  NOT NULL,
	SUBTXNUID  VARCHAR(50)  NULL,
	CMD_STATUS  NUMERIC(10)  NOT NULL,
	MDL_NM  VARCHAR(255)  NOT NULL,
	CNCTRNAME  VARCHAR(255)  NOT NULL,
	CMDPOINT  NUMERIC(10)  NOT NULL,
	SESSIONUID  VARCHAR(255)  NOT NULL,
	PRINCIPAL_NA  VARCHAR(255)  NOT NULL,
	CREATED_TS  VARCHAR(50)  NULL,
	ENDED_TS  VARCHAR(50)  NULL,
	SQL_ID  NUMERIC(10)  NULL,
	FINL_ROWCNT  NUMERIC(10)  NULL
)
;


COMMENT ON COLUMN TX_MMXCMDLOG.REQUESTID IS 'UNIQUE COMMAND ID';
COMMENT ON COLUMN TX_MMXCMDLOG.TXNUID  IS 'UNIQUE TRANSACTION ID'  ;
COMMENT ON COLUMN TX_MMXCMDLOG.CMDPOINT  IS 'POINT IN COMMAND BEING LOGGED - BEGIN, END';
COMMENT ON COLUMN TX_MMXCMDLOG.SESSIONUID  IS 'SESSION ID';
COMMENT ON COLUMN TX_MMXCMDLOG.APP_NAME  IS 'NAME OF THE CLIENT APPLICATION';
COMMENT ON COLUMN TX_MMXCMDLOG.PRINCIPAL_NA  IS 'USER NAME';
COMMENT ON COLUMN TX_MMXCMDLOG.VDBNAME  IS 'VDB NAME';
COMMENT ON COLUMN TX_MMXCMDLOG.VDBVERSION  IS 'VDB VERSION';
COMMENT ON COLUMN TX_MMXCMDLOG.CREATED_TS  IS 'BEGIN COMMAND TIMESTAMP';
COMMENT ON COLUMN TX_MMXCMDLOG.ENDED_TS  IS 'END COMMAND TIMESTAMP';
COMMENT ON COLUMN TX_MMXCMDLOG.SQL_ID  IS 'SQL ID FOR PORTION OF COMMAND';
COMMENT ON COLUMN TX_MMXCMDLOG.FINL_ROWCNT  IS 'FINAL ROW COUNT';


COMMENT ON COLUMN TX_SRCCMDLOG.REQUESTID IS 'UNIQUE COMMAND ID';
COMMENT ON COLUMN TX_SRCCMDLOG.NODEID IS 'SUBCOMMAND ID';
COMMENT ON COLUMN TX_SRCCMDLOG.SUBTXNUID IS 'ID';
COMMENT ON COLUMN TX_SRCCMDLOG.CMD_STATUS  IS 'TYPE OF REQUEST - NEW, CANCEL';
COMMENT ON COLUMN TX_SRCCMDLOG.MDL_NM  IS 'NAME OF MODEL';
COMMENT ON COLUMN TX_SRCCMDLOG.CNCTRNAME  IS 'CONNECTOR BINDING NAME';
COMMENT ON COLUMN TX_SRCCMDLOG.CMDPOINT  IS 'POINT IN COMMAND BEING LOGGED - BEGIN, END';
COMMENT ON COLUMN TX_SRCCMDLOG.SESSIONUID  IS 'SESSION ID';
COMMENT ON COLUMN TX_SRCCMDLOG.PRINCIPAL_NA  IS 'USER NAME';
COMMENT ON COLUMN TX_SRCCMDLOG.CREATED_TS  IS 'BEGIN COMMAND TIMESTAMP';
COMMENT ON COLUMN TX_SRCCMDLOG.ENDED_TS  IS 'END COMMAND TIMESTAMP';
COMMENT ON COLUMN TX_SRCCMDLOG.SQL_ID  IS 'SQL ID FOR PORTION OF COMMAND';
COMMENT ON COLUMN TX_SRCCMDLOG.FINL_ROWCNT  IS 'FINAL ROW COUNT';


CREATE TABLE TX_SQL ( SQL_ID  NUMERIC(10)    NOT NULL,
    SQL_VL  CLOB )
;
ALTER TABLE TX_SQL 
    ADD CONSTRAINT TX_SQL_PK
PRIMARY KEY (SQL_ID)
;

CREATE INDEX LOGENTRIES_TMSTMP_IX ON LOGENTRIES (TIMESTAMP);

CREATE TABLE MMSCHEMAINFO_CA
(
    SCRIPTNAME        VARCHAR(50),
    SCRIPTEXECUTEDBY  VARCHAR(50),
    SCRIPTREV         VARCHAR(50),
    RELEASEDATE VARCHAR(50),
    DATECREATED       DATE,
    DATEUPDATED       DATE,
    UPDATEID          VARCHAR(50),
    METAMATRIXSERVERURL  VARCHAR(100)
)
;


COMMENT ON TABLE MMSCHEMAINFO_CA IS
'TABLE FOR TRACKING METAMATRIX SCHEMA';

COMMENT ON COLUMN MMSCHEMAINFO_CA.SCRIPTNAME IS 'CORRELATES TO THE NAME OF THE SCRIPT THAT WAS EXECUTED ';

COMMENT ON COLUMN MMSCHEMAINFO_CA.SCRIPTEXECUTEDBY IS 'THE DB USER THAT EXECUTED THE SCRIPT.';

COMMENT ON COLUMN MMSCHEMAINFO_CA.SCRIPTREV IS 'CORRELATES TO RELEASE VERSION ';

COMMENT ON COLUMN MMSCHEMAINFO_CA.RELEASEDATE IS 'CORRELATES TO RELEASE DATE';

COMMENT ON COLUMN MMSCHEMAINFO_CA.DATECREATED IS 'DATE SCRIPT WAS EXECUTED';

COMMENT ON COLUMN MMSCHEMAINFO_CA.DATEUPDATED IS 'DATE ANY DDL UPDATES WERE PERFORMED';

COMMENT ON COLUMN MMSCHEMAINFO_CA.UPDATEID IS 'ID GENERATED BY METAMATRIX THAT A CORRELEATES TO CHANGES NEEDED TO METAMATRIX SCHEMA';
COMMENT ON COLUMN MMSCHEMAINFO_CA.METAMATRIXSERVERURL IS 'URL OF METAMATRIX SERVER USING THIS SCHEMA'
;


INSERT INTO MMSCHEMAINFO_CA (SCRIPTNAME,SCRIPTEXECUTEDBY,SCRIPTREV,
RELEASEDATE, DATECREATED,DATEUPDATED, UPDATEID,METAMATRIXSERVERURL)
SELECT 'MM_CREATE.SQL',USER,'##BUILD_NUMBER##', '##BUILD_DATE##',SYSDATE,SYSDATE,'','' FROM DUAL;

