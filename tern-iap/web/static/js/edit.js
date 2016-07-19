$(function(){
	$('#btnSave').click(function(){
    	var $frm = $('#editFrm');
    	if($frm.length<=0) $frm = $('#datafrm');
    	
    	var url = $frm.prop('action');
    	if(url==null || url == '')
    	{
    		var i = this_url.lastIndexOf('/');
    		var baseUrl = this_url.substr(0,i);
    		var tmp = this_url.substr(i+1);
    		if(tmp == 'new')
    		{
    			url = baseUrl+'/create';
    		}
    		else if(tmp == 'edit')
    		{
    			url = baseUrl+'/update';
    		}
    	}
    	
    	$.post(url+".json",$frm.serialize(),function(result){
    		var win = window;
    		if(modal.current()){
    			win = window.top;
    		}
    		
    		if(0 == result.result){
    			if(win != window) modal.close();
    			win.location.reload();
    		} else if(result.message){
    			frm_alert($frm.parent(),result.message);
        	} else {
        		frm_alert($frm.parent(),'操作失败，errcode='+result.result);
        	}
    	},'json').error(function(){
    		alert('操作失败！');
    	});            	
    });	
	
	$('#btnClose').click(function(){
		if(modal.current()){
			modal.close();	
		} else {
		    window.close();	
		}		
	});
		
});

function initDate(){
	$('.date').each(function(){
		var formatStr = $(this).find('input[type=text]').data('format');
		if(formatStr==null || formatStr=='') formatStr='YYYY-MM-DD HH:mm:ss';
		
		formatStr=formatStr.replace(new RegExp("y","g"), "Y");
		formatStr=formatStr.replace(new RegExp("d","g"), "D");

		var options = {locale:'zh_cn',format:formatStr};
		$(this).datetimepicker(options);
	});
}

function chooseEnum(src){
	var val_input = $(src).prev();
	var type = val_input.data('type');
	if(type==null || type == '') modal.popContent('无法选择枚举值!');
	
	modal.openURL('enum/'+type+'?value='+val_input.val(),{title:'枚举值选择',callback:function(eid,ename){
		val_input.val(eid);
		val_input.prev().val(ename);
	}});
};

var _reg =new RegExp('\n','g');
var frm_alert = function($frm,msg){
	var $alert = $frm.find('.alert');
	if(0 == $alert.length){
		$alert = $('<div></div>').addClass('alert alert-danger fade in');
		$alert.append($('<button></button>').addClass('close')
		     .attr('type','button').attr('data-dismiss','alert').html('&times;'));
		$alert.append('<p></p>');
		
		$alert.appendTo($frm);
	}
	$alert.find('p').html(msg.replace(_reg,"<br>"));
	$alert.alert();
};
