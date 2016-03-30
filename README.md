# tern
tern被设计为一个java框架，目前分为tern、tern-ui、tern-iap三个库（它们之间的依赖关系也是按这个顺序），其中前两者已经完善。

##1.tern lirary
整个框架的基础，提供了数据库访问、dao封装、以及一个简易的web resultful的框架。基于该库可以快速构建web应用，或者其他类型的java应用。
* com.tern.util：提供配置文件读取(yaml格式，config.java)、表达式计算(Expression)、日志(Trace)以及基本数据类型处理辅助类等(Convert、DateTimeUtil)。
* com.tern.db：基于对jdbc的封装，简化对数据库的访问。
* com.tern.dao：在com.tern.db基础上提供dao封装，核心是Record、RecordSet类，不使用SQL即可访问数据库。有些类似于ROR或web.py中对db的封装。支持外键、枚举值。
* com.tern.web：支持resultful的web框架。  

##2.tern-ui
基于tern库和freemarker，提供了block、override等freemarker模板指令（标签）。更重要的是基于com.tern.dao提供了form、field等指令，能够根据dao模型自动生成表单等界面。

##3.tern-iap
该库还需继续完善，目标是提供一个模型驱动的应用框架，实现基本界面（表单等）可无需编码自动生成，同时基于"模板"又能随时替换框架的默认生成界面，这样在兼顾封装与降低开发难度、减少开发工作量的同时又不失灵活性。
目前初步集成了osworkflow工作流。
