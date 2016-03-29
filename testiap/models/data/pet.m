name:     t_pet
caption:  宠物信息
repr:     name

columns:
    - {name : petID, type : id, auto : false, caption : 宠物ID}
    - {name : name, type : string , caption : 宠物名称, nullable : false, max : 8, min : 2}
    - {name : age,  type : numeric,caption : 年龄 , max : 100,min : 1}
    - {name : price,type : numeric,caption : 价格 , scale : 2}
    - {name : clsID,type : numeric,caption : 种类}
    - {name : birthday,type : datetime,caption : 出生日期,format: "yyyy-MM-dd"}
    
relations:
    - name: petclass
      #ref:  t_petclass
      mode: belong
      map:
        - [clsID,clsID]
     