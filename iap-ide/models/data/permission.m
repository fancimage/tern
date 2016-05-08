name:     t_permission
caption:  权限
repr:     mid

columns:
    - {name : pgid, type : numeric, caption : 权限组, key : true}
    - {name : mid, type : numeric, caption :  菜单, key : true}
    
relations:
    - name: permgroup
      #ref:  t_petclass
      mode: belong
      map:
        - [pgid,pgid]    
    
    - name: menu
      mode: belong
      map:
        - [mid,mid]

     