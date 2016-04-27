(function(){
//$('body').modalmanager('loading');
	
var tern_list = function(){
	var $tern = new Tern();
	
	if(this_url && this_url[this_url.length-1]!='/'){
		this_url+='/';
	}
	
	$('#enTable A').click(function(){
		var name = $(this).attr('href');
		//$(this).parent().parent().find('input[name=cb_record]');
		var $cb = $tern.getId($(this).parent().parent());
        if(name == '#del'){
        	if(confirm('您确实要删除选这条记录吗?')){
        		$tern.onDelete( $cb );                		
        	}                	                
        } else if(name == '#modify'){
        	$tern.onModify($cb.val());
        } else {
        	$tern.onCommand(name,$cb.val(),$cb);
        }
        
        return false;
	});
	
	$('#cb_all').click(function(){
		$('#enTable input[name=cb_record]').prop('checked' , $(this).prop('checked') );        		
	});
	
	$('#btnDel').click(function(){        		
		var $items = $('#enTable input:checked[name=cb_record]');
        if($items.length > 0){
            if(confirm('您确实要删除选中的数据吗?')){
        	    $tern.onDelete($items);	
            }	
        } else {
            alert('请先选择要操作的数据。');
        }
	});        	        	
    
    $('#btnNew').click(function(){
    	$tern.onNew();
    });
    
    var $page=$('.pagination');
    if($page.length == 1){
    	$page.css('margin','0 0').css('text-align','right').css('padding','2px').css('padding-bottom','0px');
    	var $ul=$('<ul></ul>');
    	
    	var current = 1*$page.attr('page-current');
    	var count = 1*$page.attr('page-count');            	
    	var addItem = function(i,content,act){
    		if(!content) content=i;
    		if(!act) act=i;
    		if(i!=current){
    			var $A = $('<a></a>').html(content).attr('href','#page-'+act);
    			$('<li></li>').append($A).appendTo($ul);
    		} else {
    			$('<li></li>').append('<span>'+content+'</span>').appendTo($ul);            		    
    		}            		
    	};
    	
    	addItem(1,'&laquo;','pre');
    	addItem(1);
    	
    	var s = current-2;
    	if(s <= 1) s=2;
    	var e = s+4;
    	if(e>=count) e=count-1;
    	if(s > 2){
    		$('<li></li>').append('<span>...</span>').appendTo($ul);
    	}
    	
    	for(var i=s;i<=e;i++){
    		addItem(i);
    	}
    	
    	if(e < count-1){
    		$('<li></li>').append('<span>...</span>').appendTo($ul);
    	}
    	
    	addItem(count);
    	addItem(count,'&raquo;','next');
    	$page.append($ul);
    	
    	$page.find('A').click(function(){
    		var name = $(this).attr('href');
    		if(name.indexOf('#page-' == 0)){
        	   var act = name.substring(6);
        	   var pageIdx = 0;
         	   var $current = $('input[name=query_page_current]');
        	   if(act == 'pre'){
        		  pageIdx = current-1;
        	   } else if(act == 'next'){
        		  pageIdx = current+1;
        	   } else {
        		  try{pageIdx = act*1;}catch(e){pageIdx=0;}
        	   }
        	
        	   if(pageIdx > 0 && pageIdx<=count && pageIdx != current){
        		  $current.val(pageIdx);
        		  $('#query').submit();
        	   }
           }
    	});
    }
    
    $('#btnSearch').click(function(){
    	$('input[name=query_page_total]').val('-1');
    	$('#query').submit();
    });
    
    return $tern;
};

window.tern_list = tern_list;

var _reg =new RegExp('\n','g');
var frm_alert = function($frm,msg){
	var $alert = $frm.find('.alert');
	if(0 == $alert.length){
		$alert = $('<div></div>').addClass('alert alert-block alert-error fade in');
		$alert.append($('<button></button>').addClass('close')
		     .attr('type','button').attr('data-dismiss','alert').html('&times;'));
		$alert.append('<p></p>');
		
		$alert.appendTo($frm);
	}
	$alert.find('p').html(msg.replace(_reg,"<br>"));
	$alert.alert();
};

var deleteItems = function(items){
	var sels = '';
	items.each(function(){
    	if(sels.length>0) sels+=',';
    	sels += $(this).val();
    });
        		            
    $.post(this_url+'delete',{"items":sels},function(result){
        if(0 == result.code){
        	items.each(function(){
        		$(this).parent().parent().remove();
        		$('input[name=query_page_total]').val('-1');
        	});                		
        } else if(result.message){
        	alert(result.message);
        } else {
        	alert('操作失败，errcode='+result.code);
        }
    },'json').error(function(){
        alert('操作失败！');
    });       
};

var Tern = function(){
	//this.onModify = on_edit;
	//this.onNew = on_edit;
	this.onDelete = deleteItems;
	this.string_update = '更新';
	this.string_new = '新增';
};

Tern.prototype.onModify = function(id){
	if(id) var url=id+"/edit";
    else url="new";
	
	/*有缓存?*/
	var ran = (new Date()).getTime();
	url += '?ran='+ran;
	
	var options = {minWidth:768,minHeight:100};
	options.title = (id?this.string_update:this.string_new)+' <small>'+this.caption+'</small>';
	
	modal.openURL(this_url+url,options);
};

Tern.prototype.getId = function($row,pos){
	if(!$row || $row.length<=0){
		$row = $('#enTable').find('tr:first');
		pos = null;
	}
	
	if(pos == 'prev'){
		$row = $row.prev();
	} else if(pos == 'next'){
		$row = $row.next();
	}
	
	var $id = $row.find('input[name=cb_record]');
	if($id.length <= 0){
		$id = $row.find('input[data-id=true]');
	}
	
	return $id;
}

Tern.prototype.onNew = Tern.prototype.onModify;
Tern.prototype.onCommand = function(){}

})();