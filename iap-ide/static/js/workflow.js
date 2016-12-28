/**
 * TernLight: Javascript Library for draw flow-chart,Based on HTML5 CANVAS API.
 *
 * @author fancimage
 * @Copyright 2013 fancimage@gmail.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

(function(){

var wf = tern.namespace('workflow');
window.wf = wf;

var imgpath='static/skins/wfeditor/images/';

var _shapeWidth = 120;
var _shapeHeight = 46;
var _shadowOffset = 6;
var _shadwColor = "#d0d0d0";

var MAX_SHAPE_ID = 0;
var MAX_ACTION_ID = 1;

var drawRect = function(ctx,x,y,w,h,fill,stroke,shadow){
    fill = typeof(fill) == "undefined" ? true : fill;
    stroke = typeof(stroke) == "undefined" ? true : stroke;
    shadow = typeof(shadow) == "undefined" ? true : shadow;
    if (shadow) {
        var offset = _shadowOffset;
        var oldStyle = ctx.fillStyle;
        ctx.fillStyle = _shadwColor;
        drawRect(ctx,x + offset,y + offset, w, h, true, false, false);
        ctx.fillStyle = oldStyle;
    }

    var r = 5;
    if (w < 2 * r) {
        r = w / 2;
    }
    if (h < 2 * r) {
        r = h / 2;
    }
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
    if (stroke) {
        ctx.stroke();
    }
    if (fill) {
        ctx.fill();
    }
};

var drawEllipse=function(ctx,x,y, w, h, fill, stroke, shadow) {
    fill = typeof(fill) == "undefined" ? true : fill;
    stroke = typeof(stroke) == "undefined" ? true : stroke;
    shadow = typeof(shadow) == "undefined" ? true : shadow;
    if (shadow) {
        var offset = _shadowOffset;
        var oldStyle = ctx.fillStyle;
        ctx.fillStyle = _shadwColor;
        drawEllipse(ctx,x + offset,y + offset, w, h, true, false, false);
        ctx.fillStyle = oldStyle;
    }
    var k = 0.5522848,
        a = w / 2,
        b = h / 2,
        ox = a * k,
        oy = b * k,
        x = x + a, y = y + b;

    ctx.beginPath();
    ctx.moveTo(x - a, y);
    ctx.bezierCurveTo(x - a, y - oy, x - ox, y - b, x, y - b);
    ctx.bezierCurveTo(x + ox, y - b, x + a, y - oy, x + a, y);
    ctx.bezierCurveTo(x + a, y + oy, x + ox, y + b, x, y + b);
    ctx.bezierCurveTo(x - ox, y + b, x - a, y + oy, x - a, y);
    ctx.closePath();
    if (fill) {
        ctx.fill();
    }
    if (stroke) {
        ctx.stroke();
    }
};

var drawDiamond=function(ctx,x,y, w, h, fill, stroke, shadow){
    fill = typeof(fill) == "undefined" ? true : fill;
    stroke = typeof(stroke) == "undefined" ? true : stroke;
    shadow = typeof(shadow) == "undefined" ? true : shadow;
    if (shadow) {
        var offset = _shadowOffset;
        var oldStyle = ctx.fillStyle;
        ctx.fillStyle = _shadwColor;
        drawDiamond(ctx,x + offset,y + offset, w, h, true, false, false);
        ctx.fillStyle = oldStyle;
    }

    ctx.beginPath();

    ctx.moveTo(x + w/2, y);
    ctx.lineTo(x, y+h/2);
    ctx.lineTo(x + w/2, y+h);
    ctx.lineTo(x+w, y+h/2);

    ctx.closePath();
    if (stroke) {
        ctx.stroke();
    }
    if (fill) {
        ctx.fill();
    }
};

wf.classdef('Action',{
    Action:function(node){
        this.xml = node;
        this.id = this.xml.attr('id');
        if(this.id==null || this.id==''){
            this.id = MAX_ACTION_ID;
            MAX_ACTION_ID++;
            this.xml.attr('id',this.id);
        } else {
            this.id=this.id*1;
            if(this.id>=MAX_ACTION_ID){
                MAX_ACTION_ID = this.id+1;
            }
        }
    },

    getName: function(){
        return this.xml.attr('name');
    },
    setName: function(value){
        this.xml.attr('name',value);
    },
    getPreFunctions: function(){
        return _getFunctions('pre-functions',this);
    },
    getPostFunctions: function(){
        return _getFunctions('post-functions',this);
    },
});

wf.classdef('Argument',{
    Argument: function(node){this.xml = node;},
    getName: function(){
         return this.xml.attr('name');
    },
    setName: function(value){
        this.xml.attr('name',value);
    },
    getValue: function(){
         return this.xml.text();
    },
    setValue: function(value){
        this.xml.text(value);
    },
});

wf.classdef('Function',{
    Function: function(node){
        this.xml = node;
    },

    getName: function(){
         var name = this.xml.attr('name');
         if(name==null || name==''){
             name = this.xml.attr('type');
         }
         return name;
    },
    setName: function(value){
        this.xml.attr('name',value);
    },

    getType: function(){
        return this.xml.attr('type');
    },
    setType: function(value){
        if(value == 'class'){
        } else if(value != 'beanshell'){
            value = 'beanshell';
        }
        this.xml.attr('type',value);
    },

    getScript: function(){
        var type = this.xml.attr('type');
        var sn = this.xml.find('arg[name="script"]');
        var cn = this.xml.find('arg[name="class.name"]');
        if('beanshell'==type){
            cn.remove();
            return sn.text();
        } else if('class'==type){
            sn.remove();
            return cn.text();
        }
    },
    setScript: function(value){
        var type = this.xml.attr('type');
        var argName = null;
        if('beanshell'==type){
           argName='script';
        } else if('class'==type){
           argName='class.name';
        } else {
           return;
        }

        var cn = this.xml.find('arg[name="'+argName+'"]');
        if(cn.length <= 0){
            cn = $('<arg></arg>').attr('name',argName).appendTo(this.xml);
        }
        cn.text(value);
    },

    getArguments: function(){
        var type = this.getType();
        if(type!='class') return null;

        if(this._attrs && this._attrs['arguments']){
            return this._attrs['arguments'];
        }

        var parent = this.xml;
        var list = new wf.List(function(){
           node = $('<arg></arg>').appendTo(parent);
           return new wf.Argument(node);
        });

        parent.find('arg').each(function(){
            if($(this).attr('name')=='class.name') return;
            list.push(new wf.Argument($(this)));
        });

        if(this._attrs == null){
            this._attrs={};
        }
        this._attrs['arguments'] = list;

        return list;
    },
});

var _insertBefore=function(node,parent,ns){
    if(ns == null || ns.length <=0){
        parent.append(node);
    } else {
        var i = 0;
        for(i=0;i<ns.length;i++){
            var pre = parent.find(ns[i]);
            if(pre.length > 0){
                $(pre.get(0)).before(node);
                break;
            }
        }

        if(i >= ns.length){
            parent.append(node);
        }
    }
};

var _getActions = function(node,parent){
    if(parent._attrs && parent._attrs['actions']){
        return parent._attrs['actions'];
    }

    var list = new wf.List(function(){
        if(node.length <= 0){
            node = $('<actions></actions>');
            _insertBefore(node,parent.xml,['post-functions']);
        }
        var n=$('<action></action>').appendTo(node);
        return new wf.Action(n);
    });

    node.find('action').each(function(){
        list.push(new wf.Action($(this)));
    });

    if(parent._attrs == null){
        parent._attrs={};
    }
    parent._attrs['actions'] = list;

    return list;
};

var _getFunctions = function(pname,parent){
    if(parent._attrs && parent._attrs[pname]){
        return parent._attrs[pname];
    }

    var node = parent.xml.find(pname);
    var list = new wf.List(function(){
        if(node.length ==0){
            node = $('<'+pname+'></'+pname+'>');
            if(pname=='post-functions'){
                node.appendTo(parent.xml);
            } else {
                _insertBefore(node,parent.xml,['actions','results','post-functions']);
            }
        }
        var n=$('<function></function>').appendTo(node);
        return new wf.Function(n);
    });

    node.find('function').each(function(){
        list.push(new wf.Function($(this)));
    });

    if(parent._attrs == null){
        parent._attrs={};
    }
    parent._attrs[pname] = list;
    return list;
};

wf.classdef('WorkflowShape',tern.Shape,{
  WorkflowShape: function(xml,json){
    tern.Shape.call(this);
    this.width = _shapeWidth;
    this.height = _shapeHeight;

    //this.icon = icon_step;
    this.label = new tern.Text('step',true);

    this.label.height = 32;
    this.label.width = _shapeWidth-12;
    this.label.x = (_shapeWidth-this.label.width)/2;
    this.label.y = (_shapeHeight-this.label.height)/2;
    this.label.align = 'center';
    this.label.valign= 'middle';
    this.label.font = "normal normal normal 12px Arial";

    this.addChild(this.label);

    this.addConnector(new tern.ShapeConnector(_shapeWidth/2,0));
    this.addConnector(new tern.ShapeConnector(_shapeWidth/2,_shapeHeight));
    this.addConnector(new tern.ShapeConnector(0,_shapeHeight/2));
    this.addConnector(new tern.ShapeConnector(_shapeWidth,_shapeHeight/2));

    if(xml==null){
        this.id = MAX_SHAPE_ID;
        this.xml=$('<step></step>').attr('id' , this.id);
        this.json={};
        MAX_SHAPE_ID++;
    } else {
        this.xml=$(xml);
        this.json=(json==null?{}:json);

        this.id = this.xml.attr('id')*1;
        if(this.id >= MAX_SHAPE_ID){
            MAX_SHAPE_ID = this.id+1;
        }
    }

    var text = this.xml.attr('name');
    if(text==null || text==''){
      text = 'step'+this.xml.attr('id');
    }

    this.label.text=text;

    this.fromData();
  },

  getType: function(){return null;},

  getText: function(){return this.label.text;},
  setText: function(value){
      this.label.text = value;
      this.xml.attr('name',value);
  },

  fromData: function(){
      /*从流程定义中读取数据更新本组件*/
  },

  /*paint: function(context){
    context.fillStyle ="#FFFF37";
    context.fillRect(0, 0,this.width,this.height);
    context.strokeRect(0,0 ,this.width,this.height);

    wf.WorkflowShape.superClass.paint.call(this,context);
  },*/
});

wf.classdef('StepShape',wf.WorkflowShape,{
  StepShape: function(xml,json){
    wf.WorkflowShape.call(this,xml,json);
  },

  paint: function(context){
      context.fillStyle ="#FFFF37";
      drawRect(context,0,0,this.width,this.height);
      wf.WorkflowShape.superClass.paint.call(this,context);
  },

  getType: function(){return 'Step';},

  getOpType:function(){
      return this.xml.find('meta[name="op.type"]').text();
  },
  setOpType:function(value){
      var n = this.xml.find('meta[name="op.type"]');
      if(n.length<=0){
          n = $('<meta></meta >').attr('name','op.type').appendTo(this.xml);
      }
      n.text(value);
  },

  getOpName:function(){
      return this.xml.find('meta[name="op.name"]').text();
  },
  setOpName:function(value){
      var n = this.xml.find('meta[name="op.name"]');
      if(n.length<=0){
          n = $('<meta></meta >').attr('name','op.name').appendTo(this.xml);
      }
      n.text(value);
  },

  getActions: function(){
      return _getActions(this.xml.find('actions'),this);
  },

  getPreFunctions: function(){
      return _getFunctions('pre-functions',this);
  },

  getPostFunctions: function(){
      return _getFunctions('post-functions',this);
  },

  fromData: function(){

  },

});

wf.classdef('JoinShape',wf.WorkflowShape,{
  JoinShape: function(xml,json){
    wf.WorkflowShape.call(this,xml,json);
    this.label.text='join';
  },
  paint: function(context){
      context.fillStyle ="#FFFF37";
      drawEllipse(context,0,0,this.width,this.height);
      wf.WorkflowShape.superClass.paint.call(this,context);
  },

  getType: function(){return 'Join';},
});

wf.classdef('SplitShape',wf.WorkflowShape,{
  SplitShape: function(xml,json){
    wf.WorkflowShape.call(this,xml,json);
    this.label.text='split';
  },
  paint: function(context){
      context.fillStyle ="#FFFF37";
      drawDiamond(context,0,0,this.width,this.height);
      wf.WorkflowShape.superClass.paint.call(this,context);
  },

  getType: function(){return 'Split';},
});

wf.classdef('StartShape',wf.WorkflowShape,{
  StartShape: function(xml,json){
    wf.WorkflowShape.call(this,xml,json);
    this.label.text='开始';
    this.id=0;

    this.action = this.xml.find('action');
    if(this.action.length <= 0){
        this.action=$('<action></action>').attr('id',0).appendTo(this.xml);
    } else if(this.action.length > 1){
        this.action = $(this.action[0]);
    }
    this._actionData = new wf.Action(this.action);
  },
  paint: function(context){
      context.fillStyle ="#9AFF02";
      drawEllipse(context,0,0,this.width,this.height);
      wf.WorkflowShape.superClass.paint.call(this,context);
  },

  getAction: function(){
      return this._actionData;
  },

  getType: function(){return 'Start';},

  getPreFunctions: function(){
      return _getFunctions('pre-functions',this);
  },

  getPostFunctions: function(){
      return  _getFunctions('post-functions',this);
  },

  fromData: function(){
      this.id = 0;
  },
});

wf.classdef('EndShape',wf.WorkflowShape,{
  EndShape: function(xml,json){
    wf.WorkflowShape.call(this,xml,json);
    this.label.text='结束';
  },
  paint: function(context){
      context.fillStyle ="#9AFF02";
      drawEllipse(context,0,0,this.width,this.height);
      wf.WorkflowShape.superClass.paint.call(this,context);
  },

  getPreFunctions: function(){
      return _getFunctions('pre-functions',this);
  },

  getType: function(){return 'End';},

});

var _getConnectionModel = function(con){  /*得到连线对应的数据模型*/
    if(con._data){
        return con._data;
    }

    return new wf.WFConnection(con);
};

wf.classdef('WFConnection',{  /*连线的数据*/
    WFConnection: function(con){
        this.connection = con;
        con._data = this;
        this.xml = $(con._dataXml);
    },

    getNext: function(){
        var shape = this.connection.getEndShape();
        if(shape){
            var text = shape.getText();
            text += '['+shape.id+']';
            return text;
        } else return "";
    },

    getType: function(){
        if(this.xml.get(0).nodeName == 'unconditional-result'){
            return 0;
        } else {
            var node = this.xml.find('conditions');
            if(node.attr('type')=='OR') return 2;
            else return 1;
        }
    },
    setType: function(value){
        value = value*1;
        var node = this.xml.get(0);
        var newXml = null;
        if(0 == value){
            if(node.nodeName == 'unconditional-result') return;
            newXml = $('<unconditional-result></unconditional-result>');
        } else {
            if(node.nodeName == 'result') return;
            newXml = $('<result></result>');
        }

        var id = this.xml.attr('step');
        if(id!= null){
            newXml.attr('step' , id);
        } else {
            id = this.xml.attr('split');
            if(id != null) newXml.attr('split' , id);
            else {
                id = this.xml.attr('join');
                if(id != null) newXml.attr('join' , id);
            }
        }

        var p = this.xml.parent();
        this.xml.remove();
        p.append(newXml);
        this.xml = newXml;
    },

    getAction: function(){
        var shape = this.connection.getStartShape();
        if(shape && shape.getActions){
            var node = this.xml.get(0).parentNode;
            if(node==null) return null;
            node = node.parentNode;
            if(node.nodeName=='action'){
                var src = shape.getActions();
                for(var i=0;i<src.length;i++){
                    if(node == src[i].xml.get(0)){
                        return src[i];
                    }
                }
            }
        } else if(shape && shape.getAction){
            return shape.getAction();
        }
        return null;
    },
    setAction: function(value){
        var pa = null,node = null;
        var shape = this.connection.getStartShape();
        if(shape && shape.getActions){
            value = value*1 - 1;
            var src = shape.getActions();
            if(value >=0 && value < src.length){
                node = this.xml.get(0).parentNode;
                if(node == null){
                    pa = src[value];
                } else {
                    pa = src[value];
                    node = node.parentNode;
                    if(node != pa.xml.get(0)){
                        pa = src[value];
                    }
                }
            }
        }

        if(pa != null){
            if(node != null){
                this.xml.remove();
            }

            var $results = pa.xml.find('results');
            if($results.length<=0){
                $results=$('<results></results>').appendTo(pa.xml);
            }
            $results.append(this.xml);
        }
    },

    getConditions: function(){
        if(this.xml.get(0).nodeName == 'unconditional-result'){
            return null;
        }
        if(this._attrs && this._attrs['conditions']){
            return this._attrs['conditions'];
        }

        var node = this.xml.find('conditions');
        var list = new wf.List(function(){
            if(node.length ==0){
                node = $('<conditions></conditions>');
                node.appendTo(this.xml);
            }
            var n=$('<condition></condition>').appendTo(node);
            return new wf.Function(n);
        });

        node.find('condition').each(function(){
            list.push(new wf.Function($(this)));
        });

        if(this._attrs == null){
            this._attrs={};
        }
        this._attrs['conditions'] = list;
        return list;
    },

    getSourceActions: function(){
        var shape = this.connection.getStartShape();
        if(shape && shape.getActions){
            return shape.getActions();
        }
        return null;
    },
});

/*******************定义可与界面UI、document(xml、json)双向绑定的组件模型*****************************************/
/*集合类型*/
wf.classdef('List',Array,{
    List: function(fncreate){
        this._newitem = fncreate;
        this.index = 0;
    },

    newItem: function(){
        var i = this._newitem(this);
        if(i) {
            this.push(i);
        }
        return i;
    },

    getCurrent: function(){
        if(this.index < 0 || this.index >= this.length) return undefined;
        return this[this.index];
    },
});

/*******************文档以及Controller***************************************************************************/
/*定义文档，实现osworkflow xml定义的序列化和反序列化*/
wf.classdef('WorkflowDocument',{
    WorkflowDocument: function(){
        this.diagram = null;
        this.shapes = {}; /*hash map: id-->shape*/
    },

    load: function(xml,json,diagram) {
        var $xml = $(xml);
        var $root = $xml.find('workflow');
        if($root.length <= 0){
            alert('不是合法的流程定义!');
            return;
        }

        this._xmlObj = xml;
        this._root = $root;
        this._shapes = json;
        this.diagram = diagram;

        var hasShape = false;
        if(this._shapes!= null && this._shapes.items
           && this._shapes.items.length > 0){
           hasShape = true;
        }

        if(hasShape){
            this._createShapes();
        } else {
            this._createDefaultShapes();
        }
    },

    _createShapes: function(){
        /*按已定义的排版生成流程图*/
    },

    _createDefaultShapes: function(){
        /*读取工作流原始定义，按默认排版生成流程图*/
        var $init_action = this._root.find('initial-actions action');
        if($init_action.length <=0){
             alert('流程缺少初始化动作定义.');
             return false;
        }

        var $steps = this._root.find('steps');
        var step1 = $steps.find('step[id="1"]');
        var endNode = null;
        if(step1.length ==1){
            if(step1.find('action').length<=0){
                endNode = step1;
            }
        }

        var shape = new wf.StartShape(this._root.find('initial-actions'));
        shape.x = 100;
        shape.y = 20;
        this.diagram.addChild(shape);
        this.shapes[0] = shape;

        var stacks = [];
        var current = {"shape":shape,lines:[],index:0};

        shape = new wf.EndShape(endNode);
        shape.x = 320;
        shape.y = 20;
        this.diagram.addChild(shape);
        this.shapes[shape.id] = shape;

        $init_action.each(function(){
            $(this).find('results').children().each(function(){
                current.lines.push(this);
            });
        });

        stacks.push(current);

        while(current != null){
            var i=current.index;
            var item = null;
            for(;i<current.lines.length;i++){
                current.index = i;

                var sid = $(current.lines[i]).attr('step');
                var next = null;
                var line = null;

                if(sid != null){  //next is step?
                    /*该目标节点是否已经生成*/
                    next = this.shapes[sid];
                    if(next){
                        /*直接生成两者之间的连线：直线*/
                        var cn = 1;
                        if(current.shape.y < next.y) cn = 0;
                        line = current.shape.connectors[3].connectTo(next.connectors[cn]);
                    } else {
                       var node = $steps.find('step[id="'+sid+'"]');
                       if(node.length > 0){
                           next = new wf.StepShape(node);
                           this.shapes[sid] = next;

                           item = {"shape":next,lines:[],index:0};
                           node.find('action').each(function(){
                               $(this).find('results').children().each(function(){
                                   item.lines.push(this);
                               });
                           });
                       }
                    }
                }

                if(null == next){ //next is split?
                }

                if(null == next){
                    return false;
                }

                if(null == line){
                    var left = current.shape.x + 220*i;
                    var top = current.shape.y + 100;
                    next.x = left;
                    next.y = top;
                    this.diagram.addChild(next);

                    if(i==0){
                        line = current.shape.connectors[1].connectTo(next.connectors[0]);
                    } else {
                        line = current.shape.connectors[1].connectTo(next.connectors[0] , 'v40,h');
                    }
                }
                line._dataXml = current.lines[i];
                this.diagram.addChild(line);

                if(item != null){
                    current.index++;
                    break;
                }

            }

            if(item == null){
                stacks.pop();
                current = stacks[stacks.length-1];
            } else {
                stacks.push(item);
                current = item;
            }
        }

        return true;
    },

    xmlString: function() {
        if(this._xmlObj==null) return null;

        if (window.ActiveXObject) {
          return this._xmlObj.xml;
        }else {
          return (new XMLSerializer()).serializeToString(this._xmlObj);
        }
    },

    diagramData: function(){  /*得到画布以及图元的信息*/
        var obj = {shapes:[],connections:[]};
        var items = this.diagram.children;
        for(var i=0;i<items.length;i++){
            var item = items[i];
            if(item instanceof wf.WorkflowShape){
                var s = {type:item.getType(),id:item.id,x:item.x,y:item.y};
                obj.shapes.push(s);
                /*得到其出线*/
                if(!item.connectors || item.connectors.length <= 0) continue;
                for(var j = 0;j<item.connectors.length;j++){
                    var ct = item.connectors[j];//attachedConnectors
                    for(var m = 0;m<ct.attachedConnectors.length;m++){
                        var line = ct.attachedConnectors[m].parent;
                        if(line instanceof tern.Connection){
                            if(item != line.getStartShape()) continue;
                            var endShape = line.getEndShape();
                            if(endShape){
                                var wfLine = _getConnectionModel(line);
                                var lData = {from:item.getType()+':'+item.id,action:wfLine.getAction().id,to:endShape.getType()+':'+endShape.id };
                                var pts = line.getPointsString();
                                if(pts!=null && pts.length>0) lData.points = pts;
                                obj.connections.push(lData);
                            }
                        }
                    }
                }
            }
        }
        return obj;
    },
});

wf.classdef('ViewController',{
    ViewController: function(ui){
        this.ui = ui;       /*要绑定的视图*/
        this.item = null;   /*要绑定的数据*/
        this._inEditing = false;

        this._bindViews();
    },

    setModel: function(data,selector){
        var THIS = this;
        this.item = data;

        var ctrls;
        if(selector){
            ctrls = this.ui.find(selector);//.find('[data-property]');
        } else {
            ctrls = this.ui.find('[data-property]');
        }

        ctrls.each(function(){
            var type = this.nodeName;
            var v = _getProperty(THIS.item,$(this).data('property'));
            var flag = true;
            if('UL' == type && $(this).hasClass('colors')){  //颜色
                $(this).parent().parent().parent().find('.colorSet').css('background-color',v);
            } else if('A' == type){  //布尔类型
                if(typeof(v)=='boolean'){
                    if(v) {
                        if(!$(this).hasClass('active')) $(this).addClass('active');
                    } else {
                        if($(this).hasClass('active')) $(this).removeClass('active');
                    }
                } else {
                    if($(this).hasClass('addSeries')){
                        var ptype = $(this).parent().get(0).nodeName;
                        if(v instanceof wf.List){
                            if(ptype=='H3'|| ptype=='H4'){
                                $(this).parent().show();
                            } else {
                                $(this).show();
                            }
                        } else {
                            if(ptype=='H3'|| ptype=='H4'){
                                $(this).parent().hide();
                            } else {
                                $(this).hide();
                            }
                        }
                    } else {
                        var true_value = $(this).data('value');
                        if(v == true_value){
                            if(!$(this).hasClass('active')) $(this).addClass('active');
                        } else {
                            if($(this).hasClass('active')) $(this).removeClass('active');
                        }
                    }
                }
            } else if('INPUT'== type && this.type=='radio'){
                if(this.value == v){
                    this.checked = true;
                    $(this).trigger('click');
                } else {
                    this.checked = false;
                }
            } else if('INPUT'== type && this.type=='checkbox'){
                if(typeof(v) == 'boolean'){
                    if(v){
                        this.checked = true;
                    } else {
                        this.checked = false;
                    }
                }
            } else if('SPAN'== type || 'LABEL'==type){
                /*相当于静态文本*/
                $(this).text(v);
            } else if('SELECT'== type){
                var source = $(this).data('source');
                if(source != null && source.length > 0){
                    $(this).find('OPTION').each(function(){
                        if(this.value!=''){
                            $(this).remove();
                        }
                    });

                    var srcList = _getProperty(THIS.item,source);
                    if(srcList && srcList.length>0){
                        var sel = null;
                        for(var i=0;i<srcList.length;i++){
                            var text = null;
                            if(srcList[i].getName) text=srcList[i].getName();
                            else text=srcList[i].name;

                            var ele = $('<option></option>').val(i+1).data('value',srcList[i]).text(text);
                            if(v == srcList[i]){
                                sel = ''+(i+1);
                            }
                            ele.appendTo($(this));
                        }

                        $(this).val(sel);
                    }
                } else {
                    $(this).val(v);
                }
            }else if('DIV' == type && $(this).data('type')=='list'){
                /*生成集合列表*/
                /*清除当前子元素*/
                $(this).empty();
                var selctor='[data-list-item="'+$(this).data('property')+'"]';
                if(v instanceof wf.List){
                    for(var i=0;i<v.length;i++){
                        _addListItemUI(v[i],$(this),i);
                    }

                    /*修改当前集合的索引值*/
                    if(v.length > 0){
                         v.index = 0;
                         THIS.ui.find(selctor).show();
                    } else {
                        THIS.ui.find(selctor).hide();
                    }
                } else {
                    THIS.ui.find(selctor).hide();
                }
            } else {
                $(this).val(v);
            }
        });
    },

    _updateListItem: function(listName){
        this.setModel(this.item,'[data-list-item="'+listName+'"] [data-property]');
    },

    _getListByItem: function(i){
        var $p = $(i).parent();
        $p = $p.parent();
        var item = $p.data('item');
        var list = _getProperty(this.item,$p.parent().data('property'));
        if(list instanceof wf.List){
            var index = list.indexOf(item);
            if(index >=0 && index < list.length){
                return list[index];
            }
        }

        return null;
    },

    _bindViews: function(){
        var THIS = this;
        this.ui.on('change',function(e){
            if(!THIS.item) return true;

            var $target = $(e.target);
            var property = $target.data('property');
            if(property && property.length > 0){
                THIS._inEditing = true;

                var value = e.target.value;
                //input[checkbox]
                if(e.target.type == 'checkbox'){
                    if(e.target.checked) value = true;
                    else value = false;
                }

                if(_setProperty(THIS.item,property,value)){
                }
                THIS._inEditing = false;

                var onchange = $target.data('change');
                if(onchange!=null && onchange.length>0){
                    var args = onchange.split(',');
                    if('bind'==args[0]){
                        var selector = null;
                        if(args.length > 1){
                            selector = '[data-property="'+args[1]+'"]';
                            for(var _i=2;_i<args.length;_i++){
                                selector+=','+'[data-property="'+args[_i]+'"]';
                            }
                        }
                        THIS.setModel(THIS.item,selector);
                    }
                }
            }

            return true;
        });

        /*绑定boolean类型控件*/
        this.ui.on('click','A[data-property]',function(){
            if(!THIS.item) return true;
            var ps = $(this).data('property');
            var tv = $(this).data('value');

            THIS._inEditing = true;
            if(tv && tv !=''){
                if($(this).hasClass('active')){
                    if(_setProperty(THIS.item,ps,tv)){}
                }
            } else {
                if(_setProperty(THIS.item,ps,$(this).hasClass('active'))){}
            }

            THIS._inEditing = false;
        });

        /*处理集合的新增数据等事件*/
        this.ui.on('click','[data-action]',function(){
            if(!THIS.item) return;

            var action = $(this).data('action');
            if(action=='new'){    /*新增加一个集合元素*/
                var v = _getProperty(THIS.item,$(this).data('property'));
                if(v instanceof wf.List){
                    var $p = $(THIS.ui).find('[data-type="list"][data-property="'+$(this).data('property')+'"]');
                    if($p.length == 1){
                        var item = v.newItem(); /*显示到List中*/
                        var $ui = _addListItemUI(item,$p,v.length-1);
                        /*修改当前集合的索引值*/
                        v.index = v.length-1;

                        $ui.find('INPUT[type="radio"]').get(0).checked=true;
                        var selctor='[data-list-item="'+$(this).data('property')+'"]';
                        THIS.ui.find(selctor).show();
                        THIS._updateListItem($(this).data('property'));
                    }
                }
            } else if('delete' == action){
                 var $p = $(this).parent().parent();
                 var item = $p.data('item');
                 var ps = $p.parent().data('property');
                 var list = _getProperty(THIS.item,ps);
                 if(list instanceof wf.List){
                     var index = list.indexOf(item);
                     if(index >=0 && index < list.length){
                        list.splice(index,1);

                        var $pp = $p.parent();
                        $p.remove();

                        if(list.length > 0){
                            $($pp.find('.form-entry').get(0)).find('INPUT[type="radio"]').get(0).checked = true;
                            list.index = 0;
                            THIS._updateListItem(ps);
                        } else {
                            var selctor='[data-list-item="'+ps+'"]';
                            THIS.ui.find(selctor).hide();
                        }
                     }
                 }
            } else if('edit' == action){
                /*first,find custom handler*/
                var $p = $(this).parent();
                var $parent = $p.parent().parent();
                var expr = $parent.data('onedit');
                if(expr != null && expr.length>0){
                    eval(expr);
                    return;
                }

                var listItem = THIS._getListByItem(this);
                if(listItem==null) return;

                var $label = $p.find('.text');
                var $input = $p.find('.inputText');
                if(this.className=='icon-edit'){
                    $label.hide();
                    $input.val($label.text()).show();
                    this.className = 'icon-save';
                } else {
                    if($input.val()==''){
                        alert('该项名称不能为空!');
                        return;
                    }

                    $input.hide();
                    $label.text($input.val()).show();
                    this.className = 'icon-edit';

                    /*修改模型中名称*/
                    THIS._inEditing = true;
                    THIS._inEditing = false;

                    $p = $p.parent();//listItem
                    listItem.setName($input.val());
                    THIS._updateListItem($p.parent().data('property'));
                }
            } else if('indexChange' == action){ /*集合索引改变*/
                var $p = $(this).parent();
                var item = $p.data('item');
                var list = _getProperty(THIS.item,$p.parent().data('property'));
                if(list instanceof wf.List){
                    var index = list.indexOf(item);
                    if(index >=0 && index < list.length && index != list.index){
                        list.index = index;
                        THIS._updateListItem($p.parent().data('property'));
                    }
                }
            }
        });
    },
});

wf.classdef('WorkflowController',{
    WorkflowController: function(url){
       this.url = url;
       this.document = null;

       editor();  //main ui init--from editorui.js
       this._init();

       /*初始化编辑器View*/
       this._initEditor();

       this._load();

       var $tools = $('.editor-wrapper .editor-tool');
       var that = this;
       $tools.find('A.save').click(function(){
           if(null == that.document){
               alert('流程未加载！');
               return;
           }

           var bodyStr = that.document.xmlString();
           /*获取shape json定义*/
           var json = that.document.diagramData();
           $.post(url+'/define/update.json',bodyStr+'\n===boundary===\n'+JSON.stringify(json),function(result){
               if(0 == result.result){
                   alert('保存成功！');
               } else if(result.message){
                   alert(result.message);
               } else {
                   alert('保存失败！');
               }
           },'json').error(function(){
              alert('保存失败！');
              return;
           });
       });
       $tools.find('A.refresh').click(function(){
           this._load();
       });
    },

    _load: function(){
       /*加载工作流定义*/
       var that = this;
       var xml = null;
       var json = null;
       $.post(this.url+'/define',{},function(result){
           xml = result;
           if(json != null){
               that.document = new wf.WorkflowDocument();
               that.document.load(xml,json,that.diagram);
           }
       },'xml').error(function(){
           //this.diagram.setReadonly(true);
           alert('获取流程定义失败！');
           return;
       });

       $.post(this.url+'/shape',{},function(result){
          json = result;
          if(xml != null){
              that.document = new wf.WorkflowDocument();
              that.document.load(xml,json,that.diagram);
          }
       },'json').error(function(){
          //this.diagram.setReadonly(true);
          alert('获取流程定义失败！');
          return;
       });
    },

    _init: function(){
       var diagram = new tern.Diagram('mycanvas');
       this.diagram = diagram;
       //diagram.drawBackGround = false;

       //set diagram size
       var $container = $('#canvas');
       var $canvas = $('#mycanvas');
       diagram.resize($container.find('.grid-top').width(),$container.find('.grid-left').height());

       //init toolboxes
       $('.editor-body-menu A').each(function(){
           var type=$(this).data('type');
           if(type ==null) return;

           var arr = type.split('.');
           var shapeType=null;
           for(var s in arr){
               if(shapeType) shapeType=shapeType[arr[s]];
               else shapeType=window[arr[s]];
               if(!shapeType) break;
           }
           if(shapeType){
               diagram.toolbox(this,null,shapeType);
           }
       });
    },

    _initEditor: function(){
        var $root = $('.editor-wrapper');
        var $tools = $root.find('.editor-tool');
        var $inputs = $tools.find('.tool-axes INPUT');

        this.x = $inputs.get(0);
        this.y = $inputs.get(1);

        this.item = null;
        this._inEditing = false;
        this._current = null;

        /*初始化各个UIElement的编辑器*/
        this.ctrls = {};
        var arr = $root.find('.editor-body-tool').get(0).childNodes;
        for(var i=0;i<arr.length;i++){
            if(arr[i].nodeName == 'DIV' && arr[i].className.indexOf('body-tool-')==0){
                var type =  arr[i].className.substring(10);
                this.ctrls[type] = new wf.ViewController($(arr[i]));
            }
        }

        /*处理编辑器中的INPUT、SELECT、TextArea等元素*/
        var THIS = this;
        $root.find('.editor-tool').on('change',function(e){
            if(!THIS.item) return true;

            var $target = $(e.target);
            var property = $target.data('property');
            if(property && property.length > 0){
                THIS._inEditing = true;

                var value = e.target.value;
                //input[checkbox]
                if(e.target.type == 'checkbox'){
                    if(e.target.checked) value = true;
                    else value = false;
                }

                if(_setProperty(THIS.item,property,value)){
                }
                THIS._inEditing = false;
            } else {
            }

            return true;
        });

        this._funcCtrl = new wf.ViewController($('#functionEditor').find('FORM'));

        this.diagram.selectedChange(function(items){
            THIS.attach(items);
        }).bind('onMove',function(items){
            if(THIS.item!=null){
                THIS.x.value = THIS.item.x;
                THIS.y.value = THIS.item.y;
            }
        }).bind('onTextChange',function(ctrl,text){
            if(ctrl.parent instanceof wf.WorkflowShape){
                ctrl.parent.xml.attr('name',text);
            }
        });
    },

    attach: function(items){
        if(items && items.length == 1){
            this.item = items[0];

            var type = null;
            if(this.item.getType) {
                type = this.item.getType();
                this.x.value = this.item.x;
                this.y.value = this.item.y;
            } else if(this.item instanceof tern.Connection){
                type = 'Line';
                this.item = _getConnectionModel(items[0]);
            }
            var ctrl = this.ctrls[type];
            if(this._current && this._current != ctrl){
                this._current.ui.css('display','none');
                this._current = null;
            }
            if(ctrl){
                /*更新界面上UI的属性信息*/
                ctrl.setModel(this.item);

                this._current = ctrl;
                ctrl.ui.css('display','block');
            }
        } else {
            this.item = null;
        }
    },

    editScript:function(item){
        var listItem = this._current._getListByItem(item);
        if(listItem != null){
            var element = $('#functionEditor');
            element.find('H3').text('脚本编辑');

             this._funcCtrl.setModel(listItem);

             element.on('hidden.bs.modal',function(){
                 var $p = $(item).parent();
                 $p.find('.inputText').val(listItem.getName());
                 $p.find('SPAN').text(listItem.getName());
             });
             element.modal('show');
        }
    },
});

/****************************UI与数据绑定**********************************************************************************/

var _addListItemUI = function(item,ele,index){
    var caption = item;
    if(item['name']) caption = item.name;
    else if(item.getName) caption = item.getName();

    var ui = $('<div class="form-entry"><input type="radio" data-action="indexChange"/><span class="text-edit"><span class="text">元素1</span>'
           +'<input type="text" class="inputText" /><i class="icon-edit" data-action="edit"></i><i class="icon-dell" data-action="delete"></i></span></div>');
    ui.data('item',item).find('.text').text(caption);
    ui.find('.inputText').hide();

    var radio = ui.find('INPUT[type="radio"]').get(0);
    radio.name = 'rlist_'+ele.data('property');
    if(0 == index){
        radio.checked = true;
    }

    ui.appendTo(ele);

    return ui;
};

var _setProperty = function(obj,property,v){
    if(!property) return false;
    var arr = property.split('.');
    if(!arr || arr.length <= 0 || arr[0]=='') return false;

    for(var i = 0;i<arr.length-1;i++){
        if(!obj) return false;

        var ps = 'get'+arr[i];
        if(typeof(obj[ps]) == 'function'){
            obj = obj[ps]();
        } else if(obj[arr[i]]){
            obj = obj[arr[i]];
        } else {
            return false;
        }
    }

    if(!obj) return false;

    var ps = 'set'+arr[arr.length-1];
    if(typeof(obj[ps]) == 'function'){
        obj[ps](v);
        return true;
    } else {
        ps = arr[arr.length-1];
        obj[ps] = v;
        return true;
    }
};

var _getProperty = function(obj,property){
    if(!property) return undefined;
    var arr = property.split('.');
    if(!arr || arr.length <= 0 || arr[0]=='') return undefined;

    for(var i = 0;i<arr.length-1;i++){
        if(!obj) return undefined;

        var ps = 'get'+arr[i];
        if(typeof(obj[ps]) == 'function'){
            obj = obj[ps]();
        } else if(obj[arr[i]]){
            obj = obj[arr[i]];
        } else {
            return undefined;
        }
    }

    if(!obj) return undefined;

    var ps = 'get'+arr[arr.length-1];
    if(typeof(obj[ps]) == 'function'){
        return obj[ps]();
    } else {
        ps = arr[arr.length-1];
        if(obj[ps]) return obj[ps];
        else return undefined;
    }
};

})();