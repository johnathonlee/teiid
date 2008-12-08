CREATE TABLE AUDITENTRIES
(
  TIMESTAMP  VARCHAR(50)	NOT NULL,
  CONTEXT    VARCHAR(64)	NOT NULL,
  ACTIVITY   VARCHAR(64)	NOT NULL,
  RESOURCES  VARCHAR(4000)	NOT NULL,
  PRINCIPAL  VARCHAR(64)	NOT NULL,
  HOSTNAME   VARCHAR(64)	NOT NULL,
  VMID       VARCHAR(64)	NOT NULL
)
go

CREATE TABLE AUTHPERMTYPES
(
  PERMTYPEUID       NUMERIC(10) NOT NULL CONSTRAINT PK_AUTHPERMYPES UNIQUE,
  DISPLAYNAME       VARCHAR(250) NOT NULL,
  FACTORYCLASSNAME  VARCHAR(80) NOT NULL
)
go

CREATE TABLE AUTHPOLICIES
(
  POLICYUID    NUMERIC(10)	NOT NULL CONSTRAINT PK_AUTHPOLICIES UNIQUE,
  DESCRIPTION  VARCHAR(250)	NULL,
  POLICYNAME   VARCHAR(250)	NOT NULL
)
go

CREATE TABLE AUTHPRINCIPALS
(
  PRINCIPALTYPE  NUMERIC(10) NOT NULL,
  PRINCIPALNAME  VARCHAR(255) NOT NULL,
  POLICYUID      NUMERIC(10) NOT NULL CONSTRAINT FK_ATHPLCY_PLCYUID REFERENCES AUTHPOLICIES (POLICYUID),
  GRANTOR        VARCHAR(255) NOT NULL,
  CONSTRAINT PK_AUTHPOLICYPRINCIPALS UNIQUE (PRINCIPALNAME, POLICYUID)
)
go

CREATE TABLE AUTHREALMS
(
  REALMUID     NUMERIC(10) NOT NULL CONSTRAINT PK_AUTHREALMS UNIQUE,
  REALMNAME    VARCHAR(250) NOT NULL UNIQUE,
  DESCRIPTION  VARCHAR(550) NULL
)
go

CREATE TABLE CFG_STARTUP_STATE
(STATE INTEGER DEFAULT (0) NULL,
LASTCHANGED VARCHAR(50) NULL)
go

CREATE TABLE IDTABLE
(
  IDCONTEXT  VARCHAR(20) NOT NULL PRIMARY KEY,
  NEXTID     NUMERIC
)
go

CREATE TABLE LOGMESSAGETYPES
(
  MESSAGELEVEL  NUMERIC(10) NOT NULL CONSTRAINT PK_LOGMSGTYPS UNIQUE,
  NAME          VARCHAR(64) NOT NULL,
  DISPLAYNAME   VARCHAR(64) NULL
)
go

CREATE TABLE PRINCIPALTYPES
(
  PRINCIPALTYPEUID  NUMERIC(10) NOT NULL CONSTRAINT PK_PRNCPLTYPUID UNIQUE,
  PRINCIPALTYPE     VARCHAR(60) NOT NULL,
  DISPLAYNAME       VARCHAR(80) NOT NULL,
  LASTCHANGEDBY     VARCHAR(255) NOT NULL,
  LASTCHANGED       VARCHAR(50) NULL
)
go


CREATE TABLE RT_MDLS
(
  MDL_UID           NUMERIC(10) NOT NULL CONSTRAINT PK_MDLS UNIQUE,
  MDL_UUID          VARCHAR(64) NOT NULL,
  MDL_NM            VARCHAR(255) NOT NULL,
  MDL_VERSION       VARCHAR(50) NULL,
  DESCRIPTION       VARCHAR(255) NULL,
  MDL_URI           VARCHAR(255) NULL,
  MDL_TYPE          NUMERIC(3) NULL,
  IS_PHYSICAL       CHAR(1) NOT NULL,
  MULTI_SOURCED     CHAR(1) DEFAULT ('0') NULL,  
  VISIBILITY      NUMERIC(10) NULL
)
go

CREATE TABLE RT_MDL_PRP_NMS
(
  PRP_UID  NUMERIC(10) NOT NULL CONSTRAINT PK_MDL_PRP_NMS UNIQUE,
  MDL_UID  NUMERIC(10) NOT NULL,
  PRP_NM   VARCHAR(255) NOT NULL
)
go

CREATE TABLE RT_MDL_PRP_VLS
(
  PRP_UID  NUMERIC(10) NOT NULL,
  PART_ID  NUMERIC(10) NOT NULL,
  PRP_VL   VARCHAR(255) NOT NULL,
  CONSTRAINT PK_MDL_PRP_VLS UNIQUE (PRP_UID, PART_ID)
)
go


CREATE TABLE RT_VIRTUAL_DBS
(
  VDB_UID        NUMERIC(10) NOT NULL CONSTRAINT PK_VIRT_DB UNIQUE,
  VDB_VERSION    VARCHAR(50) NOT NULL,
  VDB_NM         VARCHAR(255) NOT NULL,
  DESCRIPTION    VARCHAR(255) NULL,
  PROJECT_GUID   VARCHAR(64) NULL,
  VDB_STATUS     NUMERIC NOT NULL,
  WSDL_DEFINED   CHAR(1) DEFAULT ('0') NULL,     
  VERSION_BY     VARCHAR(100) NULL,
  VERSION_DATE   VARCHAR(50) NOT NULL,
  CREATED_BY     VARCHAR(100) NULL,
  CREATION_DATE  VARCHAR(50) NULL,
  UPDATED_BY     VARCHAR(100) NULL,
  UPDATED_DATE   VARCHAR(50) NULL,
  VDB_FILE_NM VARCHAR(2048) NULL
)
go

CREATE INDEX RTMDLS_NM_IX ON RT_MDLS (MDL_NM)
go

CREATE INDEX RTVIRTUALDBS_NM_IX ON RT_VIRTUAL_DBS (VDB_NM)
go

CREATE INDEX RTVIRTUALDBS_VRSN_IX ON RT_VIRTUAL_DBS (VDB_VERSION)
go

CREATE UNIQUE INDEX MDL_PRP_NMS_UIX ON RT_MDL_PRP_NMS (MDL_UID, PRP_NM)
go

CREATE UNIQUE INDEX PRNCIPALTYP_UIX ON PRINCIPALTYPES (PRINCIPALTYPE)
go

CREATE UNIQUE INDEX AUTHPOLICIES_NAM_UIX ON AUTHPOLICIES (POLICYNAME)
go

CREATE TABLE AUTHPERMISSIONS
(
  PERMISSIONUID    NUMERIC(10) NOT NULL CONSTRAINT PK_AUTHPERMISSIONS UNIQUE,
  RESOURCENAME     VARCHAR(250) NOT NULL,
  ACTIONS          NUMERIC(10) NOT NULL,
  CONTENTMODIFIER  VARCHAR(250) NULL,
  PERMTYPEUID      NUMERIC(10) NOT NULL CONSTRAINT FK_ATHPRMS_ATHPERM REFERENCES AUTHPERMTYPES (PERMTYPEUID),
  REALMUID         NUMERIC(10) NOT NULL CONSTRAINT FK_ATHPRMS_ATHRLMS REFERENCES AUTHREALMS (REALMUID),
  POLICYUID        NUMERIC(10) NOT NULL CONSTRAINT FK_ATHPRMS_ATHPLCY REFERENCES AUTHPOLICIES (POLICYUID)
)
go


CREATE TABLE LOGENTRIES
(
  TIMESTAMP   VARCHAR(50) NOT NULL,
  CONTEXT     VARCHAR(64) NOT NULL,
  MSGLEVEL    NUMERIC(10) NOT NULL CONSTRAINT FK_LOGENTRIES_MSGTYPES REFERENCES LOGMESSAGETYPES (MESSAGELEVEL),
  EXCEPTION   VARCHAR(4000) NULL,
  MESSAGE     VARCHAR(2000) NOT NULL,
  HOSTNAME    VARCHAR(64) NOT NULL,
  VMID        VARCHAR(64) NOT NULL,
  THREADNAME  VARCHAR(64) NOT NULL,
  VMSEQNUM NUMERIC(7) NOT NULL
)
go

CREATE TABLE RT_VDB_MDLS
(
  VDB_UID         NUMERIC(10) NOT NULL,
  MDL_UID         NUMERIC(10) NOT NULL,
  CNCTR_BNDNG_NM  VARCHAR(255) NULL
)
go

CREATE INDEX AWA_SYS_MSGLEVEL_1E6F845E ON LogEntries (MSGLEVEL)
go

CREATE UNIQUE INDEX AUTHPERM_UIX ON AUTHPERMISSIONS ( POLICYUID, RESOURCENAME)
go


-- Had to add the null specifically for this field ..dw
CREATE TABLE CS_EXT_FILES  (
   FILE_UID             INTEGER			NOT NULL,
   CHKSUM               NUMERIC(20) NULL,
   FILE_NAME            VARCHAR(255)		NOT NULL,
   FILE_CONTENTS        IMAGE			NULL,
   CONFIG_CONTENTS      TEXT NULL,
   SEARCH_POS           INTEGER NULL,
   IS_ENABLED           CHAR(1) NULL,
   FILE_DESC            VARCHAR(4000) NULL,
   CREATED_BY           VARCHAR(100) NULL,
   CREATION_DATE        VARCHAR(50) NULL,
   UPDATED_BY           VARCHAR(100) NULL,
   UPDATE_DATE          VARCHAR(50) NULL,
   FILE_TYPE            VARCHAR(30) NULL,
   CONSTRAINT PK_CS_EXT_FILES PRIMARY KEY (FILE_UID)
)
go


ALTER TABLE CS_EXT_FILES ADD CONSTRAINT CSEXFILS_FIL_NA_UK UNIQUE (FILE_NAME)
go

CREATE TABLE CS_SYSTEM_PROPS (
	PROPERTY_NAME VARCHAR(255) NULL,
-- Case change ..dw
	Property_VALUE VARCHAR(255) NULL
)
go

CREATE UNIQUE INDEX SYSPROPS_KEY ON CS_SYSTEM_PROPS (PROPERTY_NAME)
go

CREATE TABLE CFG_LOCK (
  USER_NAME       VARCHAR(50) NOT NULL,
  DATETIME_ACQUIRED VARCHAR(50) NOT NULL,
  DATETIME_EXPIRE VARCHAR(50) NOT NULL,
  HOST       VARCHAR(100) NULL,
  LOCK_TYPE NUMERIC (1) NULL
)
go


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
go

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
go


CREATE TABLE TX_SQL ( SQL_ID  NUMERIC(10)    NOT NULL,
    SQL_VL  TEXT NULL
)
go

ALTER TABLE TX_SQL 
    ADD CONSTRAINT TX_SQL_PK
PRIMARY KEY (SQL_ID)
go

CREATE INDEX LOGENTRIES_TMSTMP_IX ON LogEntries (TIMESTAMP)
go

CREATE TABLE MMSCHEMAINFO_CA
(
    SCRIPTNAME        VARCHAR(50) NULL,
    SCRIPTEXECUTEDBY  VARCHAR(50) NULL,
    SCRIPTREV         VARCHAR(50) NULL,
    RELEASEDATE VARCHAR(50) NULL,
    DATECREATED       DATETIME,
    DATEUPDATED       DATETIME,
    UPDATEID          VARCHAR(50) NULL,
    METAMATRIXSERVERURL  VARCHAR(100) NULL
)
go


INSERT INTO MMSCHEMAINFO_CA (SCRIPTNAME,SCRIPTEXECUTEDBY,SCRIPTREV,
RELEASEDATE, DATECREATED,DATEUPDATED, UPDATEID,METAMATRIXSERVERURL)
SELECT 'MM_CREATE.SQL',USER,'##BUILD_NUMERIC##', '##BUILD_DATE##',GETDATE(),GETDATE(),'',''
go

