name:     t_role
caption:  角色
repr:     rname

columns:
    - {name : rid, type : id, auto : false, caption : 角色ID}
    - {name : rname, type : string,caption : 角色名称, min : 1, max : 16}
    - {name : memo, type : string , caption : 描述, max : 128, min : 2}
    

     