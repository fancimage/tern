(function(){

var Modal = function(){
    /*是否是被其他窗口打开的？*/
	this.top = window;
    this.stack = [];  /*modal栈*/
};

Modal.prototype.popContent = function(content,options){
    if(!options) options = {};
    options = $.extend({title:'提示'},options);
    var tmpl = [
        // tabindex is required for focus
        '<div class="modal fade" tabindex="-1"><div class="modal-dialog"><div class="modal-content">',
           '<div class="modal-header">',
            '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>',
            '<h3>'+options.title+'</h3>', 
           '</div>',
           '<div class="modal-body">',             
           '</div>',           
        '</div></div></div>'
    ].join('');
	
    var jq = this.top.$;
	var element = jq(tmpl).appendTo(jq(this.top.document.body) );
    
    if(content instanceof jQuery){
        element.find('.modal-body').append(content);
    } else {
        element.find('.modal-body').html(content);
    }
    
    if(options.footer){
	    var footer = $('<div class="modal-footer"></div>');
		if(options.footer instanceof jQuery){
		    footer.append(options.footer);
		} else {
		    footer.html(options.footer);
		}
		element.append(footer);
	}
    
    var eleContent = element.find('.modal-content');
    if(options.minWidth && eleContent.width() < options.minWidth+30)
    {
    	//eleContent.width(options.minWidth+30);
    	eleContent.css('min-width',(options.minWidth+30)+'px');
    }
    
    var extraHeight = 140;
    if(options.minHeight && eleContent.height() < options.minHeight+extraHeight)
    {
    	//eleContent.height(options.minHeight);
    	eleContent.css('min-height',(options.minHeight+extraHeight)+'px');
    }
    
    var stack = this.stack;
	stack.push(element);
	element.on('hidden.bs.modal',function(){
	    stack.pop();
	    element.remove();
	});
	
	if(options.onshown){
		element.on('shown.bs.modal',function(){
			options.onshown();
		});
	}
	
	element.modal('show');
	return element;
};

Modal.prototype.openURL = function(url,options,win){ //打开一个新的窗口  
	if(!options) options = {};
	options = $.extend({autoSize:true},options);
	
	var jq = this.top.$;
	var iframe = jq('<iframe></iframe>').prop('frameborder','0').prop('src',url);
	if(options.width || options.height){
	    if(options.width) iframe.css('width',width+'px');
	    if(options.height) iframe.css('height',height+'px');	    
	} else if(options.autoSize) {
	    /*自适应*/
		iframe.bind('load',function(){
			if(iframe.prop('src')=='') return;
			iframe.unbind('load');
		    var win = iframe[0].contentWindow;

		    win.$(win).load(function(){
            	alert($(this).height());
            });

			var w = win.document.body.scrollWidth;
			var h = win.document.body.scrollHeight;
			if(w < iframe.width()) w = iframe.width();
			if(h < iframe.height()) h = iframe.height();
			if(w > win.screen.width-100) w = win.screen.width-100;
			if(h > win.screen.height-50) h = win.screen.height-50;
			
			if(options.minWidth && w<options.minWidth) w = options.minWidth;
			//if(options.minHeight && h<options.minHeight) h = options.minHeight;
			iframe.width(w).height(h);
			
			iframe[0].contentWindow.callback=options.callback;
		});
		
		var bak = options.onshown;
		options.onshown = function(){
			if(options.minWidth && iframe.width()<options.minWidth)
			{
				iframe.width(options.minWidth);
			}
			
			if(iframe.width() < iframe.parent().width()-10){
				iframe.width(iframe.parent().width()-10);
			}
			if(iframe.height() < iframe.parent().height()-10){
				iframe.height(iframe.parent().height()-10);
			}
			
			if(bak) bak();
		}
	}
	
	var element = this.popContent(iframe , options);	
	if(win) element.data('parentWin',element);	
		
	return element;
};

Modal.prototype.lastWindow = function(){  //找到当前窗口的前一个窗口
    var len = this.stack.length;
	if(len > 0){	     
		 var ret = this.stack[len-1].data('parentWin');
		 if(ret) return ret;
	}
	
	if(len > 1){
	    var iframe = this.stack[len-2].find('iframe');
        if(iframe.length <= 0) return this.top;
	    return iframe[0].contentWindow;
	} else {
	    return this.top;
	}
};

Modal.prototype.current = function(){
    var len = this.stack.length;
	if(len > 0) return this.stack[len-1];
	else return null;
};

Modal.prototype.close = function(){  //关闭当前窗口
    var len = this.stack.length;
    if(len > 0){
	    this.stack[len-1].modal('hide');
	}
};

/*保持一个实例*/
if(window.top != window){
    var topModal = window.top.modal;
    if(!topModal){
	    /*逐级向上查找*/
		var current = window.parent;
		var last = null;
		while(current){
		    if(current.modal) last = current;
			current = window.parent;
		}
		
		if(last) window.modal = last.modal;
	    else window.modal = new Modal();
	} else {
	    window.modal = topModal;
	}
} else {
    window.modal = new Modal();
}

})();