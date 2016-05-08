name:     t_permgroup
caption:  权限组
repr:     pgcaption

columns:
    - {name : pgid, type : id, auto : false, caption : 权限组ID}
    - {name : pgcaption, type : string,caption : 组名称, min : 1, max : 16}
    - {name : memo, type : string , caption : 描述, max : 128, min : 2}
    

     