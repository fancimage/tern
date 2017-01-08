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

//var imgpath='static/skins/wfeditor/images/';

var _shapeWidth = 120;
var _shapeHeight = 46;
var _shadowOffset = 6;
var _shadwColor = "#d0d0d0";

var MAX_SHAPE_ID = 0;
var MAX_ACTION_ID = 1;

var _createElement = function(name,parent,added){
    var ret = $(parent.get(0).ownerDocument.createElement(name));

    if(added==null || added===true){
        parent.append(ret);
    }
    return ret;
};

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
    appendConnection: function(line){
        var results = this.xml.find('results');
        if(results.length <= 0){
            results = _createElement('results',this.xml);
        }

        if(line.xml && line.xml.length > 0){
            results.append(line.xml);
            return line.xml;
        }
        return _createElement('unconditional-result',results);
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
            cn =  _createElement('arg',this.xml).attr('name',argName);
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
           node =  _createElement('arg',parent).attr('name','参数'+(list.length+1) );
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
            node =  _createElement('actions',parent.xml,false);
            _insertBefore(node,parent.xml,['post-functions']);
        }
        var n =  _createElement('action',node).attr('name','动作'+(list.length+1));
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

    var node = parent.xml.children(pname);
    var list = new wf.List(function(){
        if(node.length ==0){
            node = _createElement(pname,parent.xml,false);
            if(pname=='post-functions'){
                node.appendTo(parent.xml);
            } else {
                _insertBefore(node,parent.xml,['actions','results','post-functions']);
            }
        }
        var n= _createElement('function',node).attr('name','脚本'+(list.length+1));
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

var _getConditions = function(parent){
    if(parent._attrs && parent._attrs['conditions']){
        return parent._attrs['conditions'];
    }

    var node = parent.xml.find('conditions');
    var list = new wf.List(function(){
        if(node.length ==0){
            node = _createElement('conditions',parent.xml);
        }
        var n= _createElement('condition',node).attr('name','条件'+(list.length+1));
        return new wf.Function(n);
    });

    node.find('condition').each(function(){
        list.push(new wf.Function($(this)));
    });

    if(parent._attrs == null){
        parent._attrs={};
    }
    parent._attrs['conditions'] = list;
    return list;
};

wf.classdef('WorkflowShape',tern.Shape,{
  WorkflowShape: function(xml,json){
    tern.Shape.call(this);
    this.width = _shapeWidth;
    this.height = _shapeHeight;
    if(json){
        this.x = json.x;
        this.y = json.y;
    }

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

    this.xml=$(xml);

    this.id = this.xml.attr('id')*1;
    if(this.id >= MAX_SHAPE_ID){
        MAX_SHAPE_ID = this.id+1;
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

  getPre: function(){
      var list = new wf.List();
      for(var i=0;i<this.connectors.length;i++){
          var ct = this.connectors[i];
          if(ct.attachedConnectors && ct.attachedConnectors.length>0){
              for(var j=0;j<ct.attachedConnectors.length;j++){
                  var ct1 = ct.attachedConnectors[j];
                  var cts = ct1.parent.connectors;
                  if(ct1 === cts[cts.length-1]){
                      var endCT = cts[0].attachTo;
                      if(endCT && endCT.parent){
                          list.push(endCT.parent);
                      }
                  }
              }
          }
      }
      return list;
  },

  getNext: function(){
      var list = new wf.List();
      for(var i=0;i<this.connectors.length;i++){
        var ct = this.connectors[i];
        if(ct.attachedConnectors && ct.attachedConnectors.length>0){
            for(var j=0;j<ct.attachedConnectors.length;j++){
                var ct1 = ct.attachedConnectors[j];
                var cts = ct1.parent.connectors;
                if(ct1 === cts[0]){
                    var endCT = cts[cts.length-1].attachTo;
                    if(endCT && endCT.parent){
                        list.push(endCT.parent);
                    }
                }
            }
        }
      }
      return list;
  },

  setX: function(v){
      if(v != this.x){
          var cmd = new tern.Commands.MoveCommand([this], v - this.x, 0);
          cmd.redo();
          this.getDiagram().undoManager.addCommand(cmd);
      }
  },
  setY: function(v){
      if(v != this.y){
          var cmd = new tern.Commands.MoveCommand([this], 0,v-this.y);
          cmd.redo();
          this.getDiagram().undoManager.addCommand(cmd);
      }
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
          n = _createElement('meta',this.xml,false).attr('name','op.type');
          _insertBefore(n,this.xml,['pre-functions','actions','post-functions']);
      }
      n.text(value);
  },

  getOpName:function(){
      return this.xml.find('meta[name="op.name"]').text();
  },
  setOpName:function(value){
      var n = this.xml.find('meta[name="op.name"]');
      if(n.length<=0){
          n = _createElement('meta',this.xml,false).attr('name','op.name');
          _insertBefore(n,this.xml,['pre-functions','actions','post-functions']);
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

  appendConnection: function(line){
      var list = this.getActions();
      var action = null;
      if(list.length > 0){
          action = list[0];
      } else {
          action = list.newItem();
      }
      return action.appendConnection(line);
  },
  removeConnection: function(con){
      var line = _getConnectionModel(con);
      if(line.xml.parent().parent().parent().parent().get(0) == this.xml.get(0)){
          line.xml.remove();
      }
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
  appendConnection: function(line){
      if(line.xml && line.xml.length > 0){
          this.xml.append(line.xml);
          return line.xml;
      }
      return _createElement('unconditional-result',this.xml);
  },
  removeConnection: function(con){
      var line = _getConnectionModel(con);
      if(line.xml.parent().get(0) == this.xml.get(0)){
          line.xml.remove();
      }
  },

  getConditions: function(){
      return _getConditions(this);
  },
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
  appendConnection: function(line){
      if(line.xml && line.xml.length > 0){
          this.xml.append(line.xml);
          return line.xml;
      }
      return _createElement('unconditional-result',this.xml);
  },
  removeConnection: function(con){
      var line = _getConnectionModel(con);
      if(line.xml.parent().get(0) == this.xml.get(0)){
          line.xml.remove();
      }
  },
});

wf.classdef('StartShape',wf.WorkflowShape,{
  StartShape: function(xml,json){
    wf.WorkflowShape.call(this,xml,json);

    for(var i=0;i<this.connectors.length;i++){
        this.connectors[i].attachable = tern.AttachType.Out;  //只支持引出
    }

    this.label.text='开始';
    this.id=0;

    this.action = this.xml.find('action');
    if(this.action.length <= 0){
        this.action=_createElement('action',this.xml).attr('id',0);
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
      return _getFunctions('pre-functions',this._actionData);
  },

  getPostFunctions: function(){
      return  _getFunctions('post-functions',this._actionData);
  },

  appendConnection: function(line){
      return this._actionData.appendConnection(line);
  },
  removeConnection: function(con){
      var line = _getConnectionModel(con);
      if(line.xml.parent().parent().parent().get(0) == this.xml.get(0)){
          line.xml.remove();
      }
  },

  fromData: function(){
      this.id = 0;
  },
});

wf.classdef('EndShape',wf.WorkflowShape,{
  EndShape: function(xml,json){
    wf.WorkflowShape.call(this,xml,json);
    for(var i=0;i<this.connectors.length;i++){
        this.connectors[i].attachable = tern.AttachType.In;  //只支持引入
    }
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
        if(con._dataXml){
            this.xml = $(con._dataXml);
            con._data = this;
        } else {
            var s = con.getStartShape();
            if(s && s.appendConnection){
                this.xml = s.appendConnection(this);
                con._dataXml = this.xml.get(0);
                con._data = this;
            }
        }
    },

    getMode: function(){
        var s = this.connection.getStartShape();
        if(s instanceof wf.StepShape) return 'Line';
        else return 'Line2';
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
        var p = this.xml.parent();

        if(0 == value){
            if(node.nodeName == 'unconditional-result') return;
            newXml = _createElement('unconditional-result',p,false);
        } else {
            if(node.nodeName == 'result') return;
            newXml = _createElement('result',p,false);
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
            if(node && node.nodeName=='action'){
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
                $results= _createElement('results',pa.xml,false);// $('<results></results>').appendTo(pa.xml);
                _insertBefore($results,pa.xml,['post-functions'])
            }
            $results.append(this.xml);
        }
    },

    getConditions: function(){
        if(this.xml.get(0).nodeName == 'unconditional-result'){
            return null;
        }
        return _getConditions(this);
    },

    getSourceActions: function(){
        var shape = this.connection.getStartShape();
        if(shape && shape.getActions){
            return shape.getActions();
        }
        return null;
    },

    setX: function(v){
        if(!this.connection.draggable()) return;

        var ct = this.connection.connectors[0];
        var point = ct.getPoint();
        if(v != point.x){
            var cmd = new tern.Commands.MoveCommand([this.connection], v - point.x, 0);
            cmd.redo();
            this.connection.getDiagram().undoManager.addCommand(cmd);
        }
    },
    setY: function(v){
        if(!this.connection.draggable()) return;

        var ct = this.connection.connectors[0];
        var point = ct.getPoint();
        if(v != point.y){
            var cmd = new tern.Commands.MoveCommand([this.connection], 0,v - point.y);
            cmd.redo();
            this.connection.getDiagram().undoManager.addCommand(cmd);
        }
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
        this.diagram = diagram;

        if(!this._createDefaultShapes(json)){
            alert('流程定义存在错误！');  //todo...弹窗显示详细的错误信息，并能定位到具体错误的步骤
            return;
        }

        if(json && json.backgroundColor){
            diagram.backgroundColor = json.backgroundColor;
        }
    },

    setBackground: function(color){},

    _createDefaultShapes: function(json){
        /*读取工作流原始定义，按默认排版生成流程图*/
        var $init_action = this._root.find('initial-actions action');
        if($init_action.length <=0){
             alert('流程缺少初始化动作定义.');
             return false;
        }

        var $steps = this._root.find('steps');
        var $joins = this._root.find('joins');
        var $splits = this._root.find('splits');
        var step1 = $steps.find('step[id="1"]');
        var endNode = null;
        if(step1.length ==1){
            if(step1.find('action').length<=0){
                endNode = step1;
            }
        }

        var _shapes = (json==null?{}:json.shapes);
        if(!_shapes){
            _shapes = {};
        }

        var jsonShape = $.extend({x:100,y:20},_shapes[0]);
        var shape = new wf.StartShape(this._root.find('initial-actions'),jsonShape);
        this.diagram.addChild(shape);
        this.shapes[0] = shape;

        var stacks = [];
        var current = {"shape":shape,lines:[],index:0,json:jsonShape};

        if(endNode){
            jsonShape = $.extend({x:320,y:20},_shapes[endNode.attr('id')]);
        } else {
            jsonShape={x:320,y:20};
        }
        shape = new wf.EndShape(endNode,jsonShape);
        this.diagram.addChild(shape);
        this.shapes[shape.id] = shape;

        $init_action.each(function(){
            $(this).find('results').children().each(function(){
                current.lines.push(this);
            });
        });

        stacks.push(current);

        var findLine = function(next,line,cjson){
            if(!cjson || !cjson.connections) return null;

            var aid = null;
            if(line.parentNode){
                var p = line.parentNode.parentNode;
                if(p && p.nodeName=='action'){
                    aid = $(p).attr('id');
                }
            }

            var to = next.getType()+':'+next.id;
            var list = cjson.connections;
            for(var i=0;i<list.length;i++){
                if(list[i].action==aid && list[i].to==to){
                    if(!list[i]._used){
                        list[i]._used = true;
                        return list[i];
                    }
                }
            }
            return null;
        };

        var ret = true;
        while(current != null){
            var i=current.index;
            var item = null;
            for(;i<current.lines.length;i++){
                current.index = i;

                var sid = $(current.lines[i]).attr('step');
                var _shapeType = wf.StepShape;
                if(sid==null || sid==''){
                    sid = $(current.lines[i]).attr('split');
                    _shapeType = wf.SplitShape;
                    if(sid==null || sid==''){
                        sid = $(current.lines[i]).attr('join');
                        _shapeType = wf.JoinShape;
                    }
                }

                var next = null;
                var line = null;

                if(sid != null && sid != ''){  //next is step?
                    /*该目标节点是否已经生成*/
                    next = this.shapes[sid];
                    if(next){
                        /*直接生成两者之间的连线：直线*/
                        var jsonLine = findLine(next,current.lines[i],current.json);

                        var cn = 1,ct_from = 3;
                        if(jsonLine){
                            ct_from = jsonLine.ct_from;
                            if(ct_from==null || ct_from<0 || ct_from>=current.shape.connectors.length) ct_from = 3;
                            cn = jsonLine.ct_to;
                            if(cn==null || cn<0 || cn>=next.connectors.length) cn = 1;
                        }
                        else if(current.shape.y < next.y) cn = 0;

                        line = current.shape.connectors[ct_from].connectTo(next.connectors[cn],jsonLine?jsonLine.points:null);
                    } else {
                       var node = null;

                       if(_shapeType === wf.SplitShape){
                           node = $splits.find('split[id="'+sid+'"]');
                       } else if(_shapeType === wf.JoinShape){
                           node = $joins.find('join[id="'+sid+'"]');
                       } else {
                           node = $steps.find('step[id="'+sid+'"]');
                       }

                       if(node.length > 0){
                           var left = current.shape.x + 220*i;
                           var top = current.shape.y + 100;
                           jsonShape = $.extend({x:left,y:top},_shapes[sid]);
                           next = new _shapeType(node,jsonShape);
                           this.shapes[sid] = next;

                           item = {"shape":next,lines:[],index:0,json:jsonShape};

                           if(_shapeType === wf.StepShape){
                               node.find('action').each(function(){
                                  $(this).find('results').children().each(function(){
                                      item.lines.push(this);
                                  });
                               });
                           } else {
                               node.find('unconditional-result').each(function(){
                                   item.lines.push(this);
                               });
                           }
                       } //node.length > 0
                    }
                }

                if(null == next){
                    ret = false;
                    continue;
                }

                if(null == line){
                    this.diagram.addChild(next);

                    var jsonLine = findLine(next,current.lines[i],current.json);
                    var from=1,to=0;
                    if(jsonLine){
                        from = jsonLine.ct_from;
                        to = jsonLine.ct_to;
                        if(from==null || from<0 || from>=current.shape.connectors.length) from = 1;
                        if(to==null || to<0 || to>=next.connectors.length) to = 0;

                        line = current.shape.connectors[from].connectTo(next.connectors[to],jsonLine.points);
                    } else {
                        if(i==0){
                            line = current.shape.connectors[from].connectTo(next.connectors[to]);
                        } else {
                            line = current.shape.connectors[from].connectTo(next.connectors[to] , 'v25,h');
                        }
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

        /*找出可能存在的孤立步骤*/

        return ret;
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
        var obj = {shapes:{}};

        if(this.diagram.backgroundColor){
            obj.backgroundColor = this.diagram.backgroundColor;
        }

        var items = this.diagram.children;
        for(var i=0;i<items.length;i++){
            var item = items[i];
            if(item instanceof wf.WorkflowShape){
                var s = {type:item.getType(),id:item.id,x:item.x,y:item.y,connections:[]};
                obj.shapes[item.id]=s;
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
                                var ct1 = line.connectors[line.connectors.length-1].attachTo;
                                var n = 0;
                                for(n=0;n<endShape.connectors.length;n++){
                                    if(endShape.connectors[n] == ct1) break;
                                }

                                var wfLine = _getConnectionModel(line);
                                var lData = {to:endShape.getType()+':'+endShape.id,ct_from:j,ct_to:n };

                                if(item instanceof wf.StepShape){
                                    lData.action = wfLine.getAction().id;
                                }

                                var pts = line.getPointsString();
                                if(pts!=null && pts.length>0) lData.points = pts;
                                s.connections.push(lData);
                            }
                        }
                    }
                }
            }
        }
        return obj;
    },

    clear : function(){
        this.diagram.removeAllChildren();
        this.shapes = {};
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
        var old_data = this.item;
        this.item = data;

        var UI = null;//this.ui;
        if(selector){
            if(typeof(selector)==='string'){
                ctrls = this.ui.find(selector);
            } else if(selector instanceof jQuery){
                UI = selector;
            } else {
                UI = $(selector);
            }
        } else {
            UI = this.ui;
        }

        if(UI){
            /*先删除迭代生成的DOM*/
            UI.find('[data-generated="temp"]').remove();
            ctrls = UI.find('[data-property]');
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
            } else if($(this).data('action')=='foreach'){ /*循环迭代*/
                if(v instanceof wf.List){
                    var oldNode = $(this);
                    oldNode.hide();

                    for(var i=0;i<v.length;i++){
                        v.index = i;
                        if(i == 0){
                            var node = oldNode.clone().attr('data-generated','temp').removeAttr('data-property').removeAttr('data-action');
                        } else {
                            var node = oldNode.clone();
                        }

                        oldNode.after(node);
                        node.show();
                        oldNode = node;

                        var itemData = {index:i,item:v[i]};
                        THIS.setModel(itemData,node.get(0));
                    }
                }
            } else {
                $(this).val(v);
            }
        });

        if(selector){
            this.item = old_data;
        }
    },

    _updateListItem: function(listName){
        var target = this.ui.find('[data-list-item="'+listName+'"]');
        if(target.length > 0){
            var that = this;
            target.each(function(){
                that.setModel(that.item,this);
            });
        }
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
       this._hasload = false;

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
           //removeAllChildren
           if(that.document){
               that.document.clear();
           }
           this._load();
       });

       this._$straight = $tools.find('A.straight');
       this._$poly = $tools.find('A.polyline');

       var _convert = function(ele,type){
           if(!(that.item instanceof wf.WFConnection)) return;
           if($(ele).hasClass('active')) return;
           var con = that.item.connection;
           if(con.type === type) return;

           con.convert();

           if(con.type == tern.LineType.RightAngle){
               if(that._$straight.hasClass('active')) that._$straight.removeClass('active');
               if(!that._$poly.hasClass('active')) that._$poly.addClass('active');
           } else {
               if(that._$poly.hasClass('active')) that._$poly.removeClass('active');
               if(!that._$straight.hasClass('active')) that._$straight.addClass('active');
           }
       };
       this._$straight.click(function(){
           _convert(this,tern.LineType.Straight);
       });
       this._$poly.click(function(){
           _convert(this,tern.LineType.RightAngle);
       });

       $('#bgColor').change(function(){
           if(that.diagram){
               that.diagram.backgroundColor = '#'+$(this).val();
           }
       });
    },

    _load: function(){
       /*加载工作流定义*/
       var that = this;
       var xml = null;
       var json = null;
       that._hasload = false;
       $.post(this.url+'/define',{},function(result){
           xml = result;
           if(json != null){
               that.document = new wf.WorkflowDocument();
               that.document.load(xml,json,that.diagram);
               that._hasload = true;
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
              that._hasload = true;
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
       var that = this;
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
               diagram.toolbox(this,null,function(){
                   var xml = null;
                   var context = that.document._root.context;
                   if(shapeType==wf.StepShape){
                       var pn = that.document._root.find('steps');
                       if(pn.length <= 0){
                           pn = $(context.createElement('steps')).appendTo(that.document._root);
                       }
                       xml = $(context.createElement('step')).appendTo(pn);
                   } else if(shapeType==wf.SplitShape){
                       var pn = that.document._root.find('splits');
                       if(pn.length <= 0){
                          pn = $(context.createElement('splits')).appendTo(that.document._root);
                       }
                       xml = $(context.createElement('split')).appendTo(pn);
                   } else if(shapeType==wf.JoinShape){
                        var pn = that.document._root.find('joins');
                        if(pn.length <= 0){
                           pn = $(context.createElement('joins')).appendTo(that.document._root);
                        }
                        xml = $(context.createElement('join')).appendTo(pn);
                   } else if(shapeType==tern.Connection){
                       var con = new tern.Connection([new tern.Point(0,0),new tern.Point(100,100)],tern.LineType.Straight);
                       con._createConnectors();
                       con._dataXml = context.createElement('unconditional-result');
                       return con;
                   }

                   xml.attr('id' , MAX_SHAPE_ID);
                   MAX_SHAPE_ID++;

                   return new shapeType(xml);
               });
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

        this._current = this.ctrls['Workflow'];

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
            if(THIS.item instanceof tern.Shape){
                THIS.x.value = THIS.item.x;
                THIS.y.value = THIS.item.y;
            } else if(THIS.item instanceof wf.WFConnection){
                var p = THIS.item.connection.connectors[0].getPoint();
                THIS.x.value = p.x;
                THIS.y.value = p.y;
            }
        }).bind('onAdded',function(item){
            if(!THIS._hasload) return;
            if(item instanceof tern.Connection){
                if(item._dataXml == null){
                    var context = THIS.document._root.context;
                    item._dataXml = context.createElement('unconditional-result')
                }

                item = _getConnectionModel(item);
            }
            if(item._xmlParent && item.xml){
                item._xmlParent.append(item.xml);
                item._xmlParent=null;
            }
        }).bind('onRemoved',function(item){
            if(!THIS._hasload) return;
            if(item instanceof tern.Connection){
                item = _getConnectionModel(item);
            }
            if(item.xml){
                item._xmlParent = item.xml.parent();
                item.xml.remove();
            }
        }).bind('onTextChange',function(ctrl,text){
            if(ctrl.parent instanceof wf.WorkflowShape){
                ctrl.parent.xml.attr('name',text);
            }
        }).bind('onAttached',function(ctChild,ctParent){
            var con = ctChild.parent;
            var shape = ctParent.parent;
            if(!(con instanceof tern.Connection) || !(shape instanceof wf.WorkflowShape)){
                return;
            }

            var line = _getConnectionModel(con);
            if(shape == con.getStartShape()){
                if(line.xml.parent().length > 0) return;
                shape.appendConnection(line);
            } else if(shape == con.getEndShape()){
                var type = shape.getType().toLowerCase();
                line.xml.attr(type,shape.id);
            }
        }).bind('onDettached',function(ctChild,ctParent){
            var con = ctChild.parent;
            var shape = ctParent.parent;
            if(!(con instanceof tern.Connection) || !(shape instanceof wf.WorkflowShape)){
                return;
            }
            if(null == con.getStartShape()){
                shape.removeConnection(con);
            } else if(null == con.getEndShape()){
                var type = shape.getType().toLowerCase();
                var line = _getConnectionModel(con);
                if(line.xml.attr(type)==shape.id){
                    line.xml.removeAttr(type);
                }
            }
        }).bind('onBeforeRemoved',function(items){
             if(!items || !items.length) return;
             for(var i=0;i<items.length;i++){
                 if( (items[i] instanceof wf.StartShape) || (items[i] instanceof wf.EndShape) ){
                     return false;
                 }
             }
             return true;
        });
    },

    attach: function(items){
        var type = null;
        if(items && items.length == 1){
            this.item = items[0];

            if(this._$straight.hasClass('active')) this._$straight.removeClass('active');
            if(this._$poly.hasClass('active')) this._$poly.removeClass('active');
            if(this.item.getType) {
                type = this.item.getType();
                this.x.value = this.item.x;
                this.y.value = this.item.y;
            } else if(this.item instanceof tern.Connection){
                this.item = _getConnectionModel(items[0]);
                type = this.item.getMode();
                if(tern.LineType.RightAngle===items[0].type){
                    this._$poly.addClass('active');
                } else {
                    this._$straight.addClass('active');
                }

                if(items[0].connectors.length > 0){
                     var point = items[0].connectors[0].getPoint();
                     this.x.value = point.x;
                     this.y.value = point.y;
                }
            } else {
                type='Workflow';
            }
        } else {
            type='Workflow';
            this.item = null;
        }

        if(type){
            var ctrl = this.ctrls[type];
            if(this._current && this._current != ctrl){
                this._current.ui.css('display','none');
                this._current = null;
            }
            if(ctrl){
                /*更新界面上UI的属性信息*/
                if(this.item) ctrl.setModel(this.item);

                this._current = ctrl;
                ctrl.ui.css('display','block');
            }
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
        return obj[ps];
    }
};

})();