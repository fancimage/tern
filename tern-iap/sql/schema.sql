CREATE TABLE wf_service(
	tid int NOT NULL,
	pid int NOT NULL,
	tname varchar(16) NULL,
	serTableName varchar(32) NULL,
	stepTable    varchar(32) NULL,
	tcaption varchar(64) NULL,
	primary key(tid)
 );

 CREATE TABLE wf_process(
	wfID int NOT NULL primary key,
	tid int NULL,
	pid int NOT NULL,
	creator int NULL,
	createtime datetime NULL,
	finishtime datetime NULL,
	serid int NOT NULL,
	status int NULL
	#taskName varchar(256) NULL
	#tname   varchar(16)
);

---正在流转的流程，当前需要什么人来处理(待办?)
CREATE TABLE wf_operator(
    wfID         int NOT NULL,
    operatorID   int NOT NULL,
    primary key(wfID,operatorID)
);

CREATE TABLE wf_stepinfo(
	wfID int NOT NULL,
	stepID int NOT NULL,
	wfstep int NULL,
	actionid    int,
	preStep int NULL,
	stepName varchar(64) NULL,
	actionType int NULL,	
	owner int NULL,	
	--ownertype int NULL,	
	hDate datetime NULL,
	sDate datetime NULL,
	due_date datetime,	
	sstate int NULL,
	ownername varchar(64) NULL,
	caller    varchar(16) null,
	astatus   varchar(16) null,
	hDescription varchar(512) NULL,
	primary key(wfID,stepID)
);

ALTER TABLE wf_stepinfo ADD  CONSTRAINT FK_WF_STEPI_REFERENCE_WF_TASK FOREIGN KEY(wfID)
REFERENCES wf_process (wfID);	

--create table wf_prestep(
--    wfID int NOT NULL,
--	stepID int NOT NULL,
--	preStep int not NULL,
--	primary key(wfID,stepID,preStep)
--);

CREATE TABLE OS_PROPERTYENTRY
(
  GLOBAL_KEY varchar(255),
  ITEM_KEY varchar(255),
  ITEM_TYPE smallint,
  STRING_VALUE varchar(255),
  DATE_VALUE datetime,
  DATA_VALUE varbinary(2000),
  FLOAT_VALUE float,
  NUMBER_VALUE numeric,
  primary key (GLOBAL_KEY, ITEM_KEY)
);

----------------------------------------------------------------------------------------------------------------
----组织信息表(应用可以自行扩展该表)
CREATE TABLE t_organization(
    oid  int not null primary key,
    oname varchar(32) not null
);

CREATE TABLE t_role(
	roleID int NOT NULL primary key,
	roleName varchar(32),
	--roleType int default 0,
	--roleCode varchar(50) NULL,
	remark varchar(256) NULL
);

create TABLE t_group(
    groupID int NOT NULL PRIMARY KEY,
    groupName  varchar(32),
    --groupType  int default 0,
    --level      varchar(60)   --max 20 levels
    remark varchar(256) NULL
);

--菜单的定义
CREATE TABLE t_menu(
    mid int not null primary key,
    mcode varchar(16) not null,
    mname varchar(32) not null,
    pid int,
    url varchar(128),
    target varchar(16)
);

CREATE TABLE t_operator(
	operatorID int NOT NULL primary key,
	loginName varchar(16) NOT NULL,
	operatorName varchar(32) NULL,
	loginPwd varchar(256) NULL,
	isloked int NULL,
	asex int NULL,
	aphone varchar(25) NULL,
	theemail varchar(50) NULL,
	remark varchar(256) NULL
	--signs BLOB NULL
);

CREATE TABLE t_operator_roles(
    operatorID int not null,
    oid        int not null,
    roleID     int not null,
    primary key(operatorID,oid,roleID)
);

--CREATE TABLE t_group_roles(
--    groupID    int not null,
--    roleID     int not null,
--    primary key(groupID,roleID)
--);

CREATE TABLE t_operator_groups(
    operatorID  int not null,
    oid         int not null,
    groupID     int not null,
    primary key(operatorID,oid,groupID)
);

------------------------------test----------------------------------------------------------------------------------
create table leaveApply
(
    reqid       int primary key,
    opid        int not null,
    start_date  datetime,
    end_date    datetime,
    reason      varchar(256),
    reqtype     int     
);

