name:     iap_entities
caption:  数据模型
repr:     ecaption

columns:
    - {name : eid, type : id, auto : false, caption : ID}
    - {name : ename,  type : string,caption : 名称, nullable : false, min : 1 , max : 16}
    - {name : tablename,type : string,caption : 表名称, nullable : false,min : 1 , max : 32}
    - {name : ecaption,type : string,caption : 标题, min : 1 , max : 32}
    - {name : repr,type : string,caption : 标题字段, min : 1 , max : 16}
    - {name : createtime,type : datetime,caption : 创建日期,format: "yyyy-MM-dd HH:mm:ss"}
     