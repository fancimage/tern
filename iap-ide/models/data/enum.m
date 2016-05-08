name:     tn_enums
caption:  枚举值
repr:     ecaption

columns:
    - {name : eid, type : id, auto : false, caption : ID}
    - {name : etype,  type : string,caption : 类型, nullable : false}
    - {name : ecode,type : string,caption : 编码, min : 1 , max : 16}
    - {name : ecaption,type : string,caption : 标题, min : 1 , max : 32}
    
relations:
    - name: type
      ref:  enumtype
      mode: belong
      map:
        - [etype,etype]
     