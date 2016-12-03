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
var icon_step = new Image();
icon_step.src=imgpath+"step.png";
//icon_step.onload = function(){};

var icon_split = new Image();
icon_split.src=imgpath+"split.png";

var icon_join = new Image();
icon_join.src=imgpath+"join.png";

var _shapeWidth = 56;
var _shapeHeight = 60;

var MAX_SHAPE_ID = 1;
var MAX_ACTION_ID = 1;

wf.classdef('WorkflowShape',tern.Shape,{
  WorkflowShape: function(){
    tern.Shape.call(this);
    this.width = _shapeWidth;
    this.height = _shapeHeight;

    this.icon = icon_step;
    this.label = new tern.Text('step',true);

    this.label.height = 20;
    this.label.x = 0;
    this.label.y = _shapeHeight-this.label.height;
    this.label.width = _shapeWidth;
    this.label.align = 'center';
    this.label.valign= 'middle';

    this.addChild(this.label);

    var labelHeight = this.label.height;
    this.addConnector(new tern.ShapeConnector(_shapeWidth/2,0));
    this.addConnector(new tern.ShapeConnector(_shapeWidth/2,_shapeHeight-labelHeight));
    this.addConnector(new tern.ShapeConnector(0,(_shapeHeight-labelHeight)/2));
    this.addConnector(new tern.ShapeConnector(_shapeWidth,(_shapeHeight-labelHeight)/2));
  },

  getText: function(){return this.label.text;},
  setText: function(value){
      this.label.text = value;
  },

  fromData: function(xml,json){
      /*从流程定义中读取数据更新本组件*/
  },

  paint: function(context){
     var labelHeight = this.label.height;

    //drawimage(32*32)
    var left = (_shapeWidth - 32)/2;
    var top = (_shapeHeight - labelHeight - 32)/2;
    context.drawImage(this.icon,left,top,32,32);

    wf.WorkflowShape.superClass.paint.call(this,context);
  },
});

wf.classdef('StepShape',wf.WorkflowShape,{
  StepShape: function(){
    wf.WorkflowShape.call(this);
  },

  fromData: function(xml,json){
      var $xml=$(xml);
      var text = $xml.prop('name');
      if(text==null || text==''){
          text = 'step'+$xml.prop('id');
      }
      this.label.text=text;
  },

});

wf.classdef('JoinShape',wf.WorkflowShape,{
  JoinShape: function(){
    wf.WorkflowShape.call(this);
    this.icon = icon_join;
    this.label.text='join';
  },
});

wf.classdef('SplitShape',wf.WorkflowShape,{
  SplitShape: function(){
    wf.WorkflowShape.call(this);
    this.icon = icon_split;
    this.label.text='split';
  },
});

var icon_start = new Image();
icon_start.src=imgpath+"start.png";
wf.classdef('StartShape',wf.WorkflowShape,{
  StartShape: function(){
    wf.WorkflowShape.call(this);
    this.icon = icon_start;
    this.label.text='开始';
  },
});

var icon_end = new Image();
icon_end.src=imgpath+"end.png";
wf.classdef('EndShape',wf.WorkflowShape,{
  EndShape: function(){
    wf.WorkflowShape.call(this);
    this.icon = icon_end;
    this.label.text='结束';
  },
});

/*******************定义可与界面UI、document(xml、json)双向绑定的组件模型*****************************************/
/*集合类型*/
wf.classdef('List',Array,{
    List: function(element,fncreate){
        this.element = element;
        this._newitem = fncreate;
        this.index = 0;
    },

    newItem: function(){
        var i = this._newitem(this);
        if(i) {
            this.push(i);
            this.element.updateChart();
        }
        return i;
    },

    getCurrent: function(){
        if(this.index < 0 || this.index >= this.length) return undefined;
        return this[this.index];
    },
});

/*组件类基类*/
wf.classdef('JComponent',{
    JComponent: function(element){
        this.element=element;
    }
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
        var shape = new wf.EndShape();
        shape.x = 320;
        shape.y = 20;
        this.diagram.addChild(shape);

        var step1 = $steps.find('step[id="1"]');
        if(step1.length ==1){
            if(step1.find('action').length<=0){
                shape.fromData(step1);
                this.shapes[1] = shape;
            }
        }

        shape = new wf.StartShape();
        shape.fromData(this._root.find('initial-actions'));
        shape.x = 100;
        shape.y = 20;
        this.diagram.addChild(shape);
        this.shapes[0] = shape;

        var stacks = [];
        var current = {"shape":shape,lines:[],index:0};

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
                        line = current.shape.connectors[3].connectTo(next.connectors[0]);
                    } else {
                       var node = $steps.find('step[id="'+sid+'"]');
                       if(node.length > 0){
                           next = new wf.StepShape();
                           next.fromData(node);
                           this.shapes[sid] = shape;

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
                this.diagram.addChild(line);

                if(item != null){
                    current.index++;
                    break;
                }

            }

            if(item == null){
                current = stacks.pop();
            } else {
                stacks.push(item);
                current = item;
            }
        }

        return true;
    },

});

wf.classdef('WorkflowController',{
    WorkflowController: function(url){
       editor();  //main ui init--from editorui.js
       this._init();

       /*加载工作流定义*/
       var that = this;
       var xml = null;
       var json = null;
       $.post(url+'/define',{},function(result){
           xml = result;
           if(json != null){
               var doc = new wf.WorkflowDocument();
               doc.load(xml,json,that.diagram);
           }
       },'xml').error(function(){
           //this.diagram.setReadonly(true);
           alert('获取流程定义失败！');
           return;
       });

       $.post(url+'/shape',{},function(result){
          json = result;
          if(xml != null){
              var doc = new wf.WorkflowDocument();
              doc.load(xml,json,that.diagram);
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
});

})();