name:     wf_service
caption:  流程类型
repr:     tcaption

columns:
    - {name : tid, type : id, auto : false, caption : 流程ID}
    - {name : pid, type : numeric, caption : 组织ID, default : 0}
    - {name : tcode,  type : string,caption : 流程标识, nullable : false}
    - {name : tname,  type : string,caption : 流程模型, nullable : false}
    - {name : serTableName,type : string,caption : 数据模型}
    - {name : stepTable,type : string,caption : 步骤数据模型}
    - {name : tcaption,type : string,caption : 流程名称}
     