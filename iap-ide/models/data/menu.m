name:     t_menu
caption:  应用菜单
repr:     mcaption

columns:
    - {name : mid, type : id, auto : false, caption : 菜单ID}
    - {name : mcode, type : string,caption : 菜单编码, min : 1, max : 16}
    - {name : mcaption, type : string , caption : 菜单名称, nullable : false,max : 32, min : 2}
    - {name : murl, type : string , caption : url地址, nullable : false,max : 64, min : 1}
    - {name : mtarget, type : string , caption : 目标窗口, max : 16, min : 0}
    

     