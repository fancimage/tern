name:     t_menu
caption:  应用菜单
repr:     mcaption

columns:
    - {name : mid, type : id, auto : false, caption : 功能ID}
    - {name : mcode, type : string,caption : 功能编码, min : 1, max : 16}
    - {name : mcaption, type : string , caption : 功能名称, nullable : false,max : 32, min : 2}
    - {name : murl, type : string , caption : url地址, nullable : false,max : 64, min : 1}
    - {name : micon, type : string , caption : 图标, max : 16, min : 0}
    - {name : mtarget, type : string , caption : 目标窗口, max : 16, min : 0}
    - {name : mismenu, type : bool, caption : 是否为菜单}

     