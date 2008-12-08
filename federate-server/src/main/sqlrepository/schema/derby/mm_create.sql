CREATE TABLE AUDITENTRIES
(
   TIMESTAMP            VARCHAR(50)            NOT NULL,
   CONTEXT              VARCHAR(64)            NOT NULL,
   ACTIVITY             VARCHAR(64)            NOT NULL,
   RESOURCES            VARCHAR(4000)          NOT NULL,
   PRINCIPAL            VARCHAR(255)            NOT NULL,
   HOSTNAME             VARCHAR(64)            NOT NULL,
   VMID                 VARCHAR(64)            NOT NULL
);
CREATE TABLE AUTHPERMISSIONS
(
   PERMISSIONUID        NUMERIC(20)            NOT NULL,
   RESOURCENAME         VARCHAR(250)           NOT NULL,
   ACTIONS              NUMERIC(10)            NOT NULL,
   CONTENTMODIFIER      VARCHAR(250),
   PERMTYPEUID          NUMERIC(10)            NOT NULL,
   REALMUID             NUMERIC(20)            NOT NULL,
   POLICYUID            NUMERIC(20)            NOT NULL,
	CONSTRAINT P_KEY_2a PRIMARY KEY (PERMISSIONUID)
);
CREATE TABLE AUTHPERMTYPES
(
   PERMTYPEUID          NUMERIC(10)            NOT NULL,
   DISPLAYNAME          VARCHAR(250)           NOT NULL,
   FACTORYCLASSNAME     VARCHAR(80)            NOT NULL,
   CONSTRAINT P_KEY_2b PRIMARY KEY (PERMTYPEUID)
);
CREATE TABLE AUTHPOLICIES
(
   POLICYUID            NUMERIC(20)            NOT NULL,
   DESCRIPTION          VARCHAR(250),
   POLICYNAME           VARCHAR(250)           NOT NULL,
   CONSTRAINT P_KEY_2c PRIMARY KEY (POLICYUID)
);
CREATE TABLE AUTHPRINCIPALS
(
   PRINCIPALTYPE        NUMERIC(10)            NOT NULL,
   PRINCIPALNAME        VARCHAR(255)           NOT NULL,
   POLICYUID            NUMERIC(20)            NOT NULL,
   GRANTOR              VARCHAR(255)           NOT NULL
);
CREATE TABLE AUTHREALMS
(
   REALMUID             NUMERIC(20)            NOT NULL,
   REALMNAME            VARCHAR(250)           NOT NULL,
   DESCRIPTION          VARCHAR(550),
   CONSTRAINT P_KEY_3a PRIMARY KEY (REALMUID)
);
CREATE TABLE CFG_STARTUP_STATE
(
   STATE                NUMERIC,
   LASTCHANGED          VARCHAR(50)
);
CREATE TABLE IDTABLE
(
   IDCONTEXT            VARCHAR(20)            NOT NULL,
   NEXTID               NUMERIC(20),
   CONSTRAINT SYS_C0083745 PRIMARY KEY (IDCONTEXT)
);
CREATE TABLE LOGENTRIES
(
   TIMESTAMP            VARCHAR(50)            NOT NULL,
   CONTEXT              VARCHAR(64)            NOT NULL,
   MSGLEVEL             NUMERIC(10)            NOT NULL,
   "EXCEPTION"          VARCHAR(4000),
   MESSAGE              VARCHAR(2000)          NOT NULL,
   HOSTNAME             VARCHAR(64)            NOT NULL,
   VMID                 VARCHAR(64)            NOT NULL,
   THREADNAME           VARCHAR(64)            NOT NULL,
   VMSEQNUM             NUMERIC(7)             NOT NULL
);
CREATE TABLE LOGMESSAGETYPES
(
   MESSAGELEVEL         NUMERIC(10)            NOT NULL,
   "NAME"               VARCHAR(64)            NOT NULL,
   DISPLAYNAME          VARCHAR(64),
   CONSTRAINT P_KEY_2d PRIMARY KEY (MESSAGELEVEL)
);
CREATE TABLE PRINCIPALTYPES
(
   PRINCIPALTYPEUID     NUMERIC(10)            NOT NULL,
   PRINCIPALTYPE        VARCHAR(60)            NOT NULL,
   DISPLAYNAME          VARCHAR(80)            NOT NULL,
   LASTCHANGEDBY        VARCHAR(255)            NOT NULL,
   LASTCHANGED          VARCHAR(50),
   CONSTRAINT P_KEY_2f PRIMARY KEY (PRINCIPALTYPEUID)
);
CREATE TABLE RT_MDLS
(
   MDL_UID              NUMERIC(20)            NOT NULL,
   MDL_UUID             VARCHAR(64)            NOT NULL,
   MDL_NM               VARCHAR(255)           NOT NULL,
   MDL_VERSION          VARCHAR(50),
   DESCRIPTION          VARCHAR(255),
   MDL_URI              VARCHAR(255),
   MDL_TYPE             NUMERIC(3),
   IS_PHYSICAL          CHAR(1)                NOT NULL,
   MULTI_SOURCED        CHAR(1)    WITH DEFAULT '0',  
   VISIBILITY           NUMERIC(3) 
   
);
CREATE TABLE RT_MDL_PRP_NMS
(
   PRP_UID              NUMERIC(20)            NOT NULL,
   MDL_UID              NUMERIC(20)            NOT NULL,
   PRP_NM               VARCHAR(255)           NOT NULL
);
CREATE TABLE RT_MDL_PRP_VLS
(
   PRP_UID              NUMERIC(20)            NOT NULL,
   PART_ID              NUMERIC(20)            NOT NULL,
   PRP_VL               VARCHAR(255)           NOT NULL
);
CREATE TABLE RT_VDB_MDLS
(
   VDB_UID              NUMERIC(20)            NOT NULL,
   MDL_UID              NUMERIC(20)            NOT NULL,
   CNCTR_BNDNG_NM       VARCHAR(255)
);
CREATE TABLE RT_VIRTUAL_DBS
(
   VDB_UID              NUMERIC(20)            NOT NULL,
   VDB_VERSION          VARCHAR(50)            NOT NULL,
   VDB_NM               VARCHAR(255)           NOT NULL,
   DESCRIPTION          VARCHAR(255),
   PROJECT_GUID         VARCHAR(64),
   VDB_STATUS           NUMERIC                NOT NULL,
   WSDL_DEFINED         CHAR(1)  WITH DEFAULT '0',   
   VERSION_BY           VARCHAR(100),
   VERSION_DATE         VARCHAR(50)            NOT NULL,
   CREATED_BY           VARCHAR(100),
   CREATION_DATE        VARCHAR(50),
   UPDATED_BY           VARCHAR(100),
   UPDATED_DATE         VARCHAR(50),
   VDB_FILE_NM VARCHAR(2048)
);

ALTER TABLE AUTHPERMISSIONS
   ADD CONSTRAINT FK_ATHPRMS_ATHPERM FOREIGN KEY (PERMTYPEUID)
      REFERENCES AUTHPERMTYPES (PERMTYPEUID)
;
ALTER TABLE AUTHPERMISSIONS
   ADD CONSTRAINT FK_ATHPRMS_ATHPLCY FOREIGN KEY (POLICYUID)
      REFERENCES AUTHPOLICIES (POLICYUID)
;
ALTER TABLE AUTHPERMISSIONS
   ADD CONSTRAINT FK_ATHPRMS_ATHRLMS FOREIGN KEY (REALMUID)
      REFERENCES AUTHREALMS (REALMUID)
;
ALTER TABLE AUTHPRINCIPALS
   ADD CONSTRAINT FK_ATHPLCY_PLCYUID FOREIGN KEY (POLICYUID)
      REFERENCES AUTHPOLICIES (POLICYUID)
;
CREATE UNIQUE INDEX AUTHPERM_UIX
    ON AUTHPERMISSIONS(POLICYUID, RESOURCENAME)
;
CREATE UNIQUE INDEX AUTHPOLICIES_NAM_U
    ON AUTHPOLICIES(POLICYNAME)
;
CREATE INDEX LOGNTRIS_MSGLVL_IX
    ON LOGENTRIES(MSGLEVEL)
;

CREATE UNIQUE INDEX PRNCIPALTYP_UIX
    ON PRINCIPALTYPES(PRINCIPALTYPE)
;
CREATE UNIQUE INDEX MDL_PRP_NMS_UIX
    ON RT_MDL_PRP_NMS(MDL_UID,PRP_NM)
;
CREATE INDEX RTMDLS_MDLNAME_IX
    ON RT_MDLS(MDL_NM)
;
CREATE INDEX RTVIRTUALDBS_NM_IX
    ON RT_VIRTUAL_DBS(VDB_NM)
;
CREATE INDEX RTVIRTULDBSVRSN_IX
    ON RT_VIRTUAL_DBS(VDB_VERSION)
;
CREATE TABLE CS_EXT_FILES  (
   FILE_UID             DECIMAL(19)                          NOT NULL,
   CHKSUM               DECIMAL(20),
   FILE_NAME            VARCHAR(255)		NOT NULL,
   FILE_CONTENTS        BLOB(1000M),
   CONFIG_CONTENTS	    CLOB(20M),
   SEARCH_POS           DECIMAL(10),
   IS_ENABLED           VARCHAR(1),
   FILE_DESC            VARCHAR(4000),
   CREATED_BY           VARCHAR(100),
   CREATION_DATE        VARCHAR(50),
   UPDATED_BY           VARCHAR(100),
   UPDATE_DATE          VARCHAR(50),
   FILE_TYPE            VARCHAR(30))
;
ALTER TABLE CS_EXT_FILES
       ADD   PRIMARY KEY (FILE_UID);
ALTER TABLE CS_EXT_FILES ADD CONSTRAINT CSEXFILS_FIL_NA_UK UNIQUE (FILE_NAME)
;


CREATE TABLE CS_SYSTEM_PROPS (
	PROPERTY_NAME VARCHAR(255),
	PROPERTY_VALUE VARCHAR(255)
)
;

CREATE UNIQUE INDEX SYSPROPS_KEY ON CS_SYSTEM_PROPS (PROPERTY_NAME);


CREATE FUNCTION SYSDATE () 
RETURNS TIMESTAMP
PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
EXTERNAL NAME 'java.lang.System.currentTimeMillis'
;
-- CREATE FUNCTION SYSDATE ()
-- RETURNS TIMESTAMP
-- LANGUAGE SQL
-- SPECIFIC SYSDATEORACLE
-- NOT DETERMINISTIC
-- CONTAINS SQL
-- NO EXTERNAL ACTION
-- RETURN
-- CURRENT TIMESTAMP
-- ;

CREATE TABLE CFG_LOCK (
  USER_NAME       VARCHAR(50) NOT NULL,
  DATETIME_ACQUIRED VARCHAR(50) NOT NULL,
  DATETIME_EXPIRE VARCHAR(50) NOT NULL,
  HOST       VARCHAR(100),
  LOCK_TYPE NUMERIC (1) )
;

CREATE TABLE TX_MMXCMDLOG (
	REQUESTID  VARCHAR(255)  NOT NULL,
	TXNUID  VARCHAR(50)  ,
	CMDPOINT  NUMERIC(10)  NOT NULL,
	SESSIONUID  VARCHAR(255)  NOT NULL,
	APP_NAME  VARCHAR(255) ,
	PRINCIPAL_NA  VARCHAR(255)  NOT NULL,
	VDBNAME  VARCHAR(255)  NOT NULL,
	VDBVERSION  VARCHAR(50)  NOT NULL,
	CREATED_TS  VARCHAR(50)  ,
	ENDED_TS  VARCHAR(50)  ,
	CMD_STATUS  NUMERIC(10)  NOT NULL,
	SQL_ID  NUMERIC(10) ,
	FINL_ROWCNT NUMERIC(10)
)
;
CREATE TABLE TX_SRCCMDLOG (
	REQUESTID  VARCHAR(255)  NOT NULL,
	NODEID  NUMERIC(10)  NOT NULL,
	SUBTXNUID  VARCHAR(50)  ,
	CMD_STATUS  NUMERIC(10)  NOT NULL,
	MDL_NM  VARCHAR(255)  NOT NULL,
	CNCTRNAME  VARCHAR(255)  NOT NULL,
	CMDPOINT  NUMERIC(10)  NOT NULL,
	SESSIONUID  VARCHAR(255)  NOT NULL,
	PRINCIPAL_NA  VARCHAR(255)  NOT NULL,
	CREATED_TS  VARCHAR(50)  ,
	ENDED_TS  VARCHAR(50)  ,
	SQL_ID  NUMERIC(10)  ,
	FINL_ROWCNT  NUMERIC(10)  
)
;

CREATE TABLE TX_SQL ( SQL_ID  NUMERIC(10)    NOT NULL,
    SQL_VL  CLOB(1000000) )
;
ALTER TABLE TX_SQL 
    ADD CONSTRAINT TX_SQL_PK
PRIMARY KEY (SQL_ID)
;

CREATE INDEX LOGNTRIS_TMSTMP_IX 
    ON LOGENTRIES (TIMESTAMP)
;

CREATE VIEW DUAL(C1) AS VALUES 1
;

CREATE TABLE MMSCHEMAINFO_CA
(
    SCRIPTNAME        VARCHAR(50),
    SCRIPTEXECUTEDBY  VARCHAR(50),
    SCRIPTREV         VARCHAR(50),
    RELEASEDATE       VARCHAR(50),
    DATECREATED       TIMESTAMP,
    DATEUPDATED       TIMESTAMP,
    UPDATEID          VARCHAR(50),
    METAMATRIXSERVERURL  VARCHAR(100)
)
;

INSERT INTO MMSCHEMAINFO_CA (SCRIPTNAME,SCRIPTEXECUTEDBY,SCRIPTREV,
RELEASEDATE, DATECREATED,DATEUPDATED, UPDATEID,METAMATRIXSERVERURL)
SELECT 'MM_CREATE.SQL',USER,'##BUILD_NUMBER##', '##BUILD_DATE##', CURRENT TIMESTAMP,CURRENT TIMESTAMP,'','' 
FROM DUAL
;


