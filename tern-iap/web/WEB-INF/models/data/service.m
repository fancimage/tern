name:     wf_service
caption:  流程类型
repr:     tcaption

columns:
    - {name : tid, type : id, auto : false, caption : 流程ID}
    - {name : tname,  type : string,caption : 流程标识, nullable : false}
    - {name : serTableName,type : string,caption : 数据模型}
    - {name : tcaption,type : string,caption : 流程名称}
    - {name : appname,type : string,caption : 所属应用}
     