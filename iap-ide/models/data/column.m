name:     iap_columns
caption:  数据模型
repr:     ccaption

columns:
    - {name : cid, type : id, auto : false, caption : ID}
    - {name : eid, type : numeric, caption : 所属表}
    - {name : cname,  type : string,caption : 字段名, nullable : false, min : 1 , max : 16}
    - {name : ccaption,type : string,caption : 标题, nullable : false,min : 1 , max : 16}
    - {name : ctype,type : string,caption : 类型, min : 1 , max : 12}
    - {name : nullable,type : bool,caption : 是否可空, default: true}
    - {name : auto,type : bool,caption : 自增长？}
    - {name : iskey,type : bool,caption : 是否主键}
    - {name : readonly,type : bool,caption : 只读？}
    - {name : cmax,type : numeric,caption : 最大值}
    - {name : cmin,type : numeric,caption : 最小值}
    - {name : format,type : string,caption : 显示格式,min : 1 , max : 32}
    - {name : clength,type : numeric,caption : 位数}
    - {name : scale,type : numeric,caption : 小数位数}
    - {name : cref,type : string,caption : 引用,min : 1 , max : 32}
    - {name : cdefault,type : string,caption : 默认值,min : 1 , max : 32}
    - {name : csort,type : string,caption : 顺序,min : 1 , max : 32}  
     