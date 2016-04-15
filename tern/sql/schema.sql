create table tn_enumtypes
(
    etype     varchar(16) primary key, 
    ecaption  varchar(64)
);

create table tn_enums
(
    eid      int primary key,
    etype    varchar(16),
    ecode    varchar(16),
    ecaption varchar(64)
);

insert into tn_enumtypes values('gender','性别');
insert into tn_enumtypes values('nation','民族');

insert into tn_enums(eid,etype,ecode,ecaption) values(1,'gender','01','男');
insert into tn_enums(eid,etype,ecode,ecaption) values(2,'gender','02','女');

insert into tn_enums values(5,'nation','001','汉族');
insert into tn_enums values(6,'nation','002','回族');
insert into tn_enums values(7,'nation','003','蒙族');
insert into tn_enums values(8,'nation','004','藏族');
insert into tn_enums values(9,'nation','005','满族');
insert into tn_enums values(10,'nation','006','壮族');
insert into tn_enums values(11,'nation','007','苗族');
insert into tn_enums values(12,'nation','008','彝族');

----描述Model的表结构(oracle)------------
create table iap_entities
(
    eid int primary key,
    ename varchar2(32) not null,
    ecaption varchar2(64) not null,
    repr  varchar2(16),
    creator number(26),
    createtime date,
    inherit int,
    estatus int default 0  --状态:0-设计中  1-使用中   (2--已经存在数据,暂时不用)
);

create table iap_columns
(
    cid int primary key,
    eid int not null,
    cname  varchar2(32) not null,
    ccaption varchar2(32) not null,
    ctype  varchar2(12) not null,
    nullable number(1) default 1,
    auto     number(1) default 0,
    iskey    number(1) default 0,
    readonly number(1) default 0,
    cmax     int default 0,
    cmin     int default 0,
    format   varchar2(32),
    clength  int default 0,
    scale    int default 0,
    cref     varchar2(32),
    cdefault varchar2(64),
    csort    int default 0,   --显示顺序
    cstatus  int default 0,   --状态:0-设计中  1-使用中  2--已经存在数据
    foreign KEY(eid) REFERENCES iap_entities(eid)
);

create unique index iap_columns_name on iap_columns(eid,cname);

create table iap_relations
(
    rid   int primary key,
    eid   int not null,
    rcaption varchar2(32),
    rref  int not null,
    rmode varchar2(8) default 'have',
    foreign KEY(eid) REFERENCES iap_entities(eid),
    foreign KEY(rref) REFERENCES iap_entities(eid)
);

create table iap_relation_map
(
    rid int not null,
    rsrc  int,
    rdst  int
);

insert into TMENU (menuid, tme_menuid, menuname, href, priority, menutype, menulevel, menutips, statecode, remark, displayable, restriction, highlight, target)
values (282885574245017, 32652401172565915, '表单定义', 'do/model/iapModel/getPreview?dataAccess=1', 7, 'W', 3, '自定义表单设计', '1', null, '1', null, '0', '0');

/*
 自定义表单系统：

1). 建立一个新库，用来存储自定义的表单；
            表单定义好后，系统为其创建物理表；
2). 表单的模式定义存储在主库中（相比存成文件较为安全，可以统一备份）
3). 需要控制表单的访问权限
    a. 创建/修改表单的权限
    b. 录入表单数据的权限
    c. 查看表单数据的权限
4). 表单定义页面的开发
5). 表单数据WEB界面的开发
    a. 新增/修改
    b. 查看
    c. 搜索(查询)
6). 表单数据ANDROID界面的开发

每一个自定义表，都需要一个taskid字段

权限
1. 对任务是否有权限  --数据权限
   /iap/form/${eid}/${taskid}/   --列表页面
   /iap/form/${eid}/${taskid}/new   --新建页面
   /iap/form/${eid}/${taskid}/${id}   --某条数据的详情页面
   /iap/form/${eid}/${taskid}/${id}/edit --某条数据的编辑界面
   
    /iap/form/${eid}/${taskid}/create   --新增一条数据
   /iap/form/${eid}/${taskid}/${id}/update --更新一条数据
   /iap/form/${eid}/${taskid}/${id}/delete  --删除一条数据
 
2. 是否能新建表单     --菜单权限

   --/iap/design/form/${name}
   
   /do/model/iapModel
             getlist
             entity -- 增删改查
             
任务以及任务与表单的关联 -- 不做             
    
*/