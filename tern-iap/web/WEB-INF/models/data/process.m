name:     wf_process
caption:  工作流程
repr:     taskName

columns:
    - {name : wfID, type : id, auto : false, caption : 流程ID}
    - {name : tid, type : numeric , caption : 类型, nullable : false}
    - {name : creator,  type : numeric,caption : 创建者, nullable : false}
    - {name : createtime,type : datetime,caption : 发起时间,format: "yyyy-MM-dd HH:mm:ss"}
    - {name : finishtime,type : datetime,caption : 完成时间,format: "yyyy-MM-dd HH:mm:ss"}
    - {name : serid,type : numeric,caption : 数据ID}
    - {name : status,type : numeric,caption : 状态}
    - {name : taskName,type : string,caption : 流程名称}
    - {name : wfName,type : string,caption : 类型名称}
    
relations:
    - name: service
      #ref:  wf_service
      mode: belong
      map:
        - [tid,tid]
     