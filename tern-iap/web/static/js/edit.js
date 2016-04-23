$(function(){
	$('#btnSave').click(function(){
    	var $frm = $('#editFrm');
    	if($frm.length<=0) $frm = $('#datafrm');
    	
    	var url = $frm.attr('action');
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
    	
    	$.post(url,$frm.serialize(),function(result){
    		var win = window;
    		if(modal.current()){
    			win = window.top;
    		}
    		
    		if(0 == result.code){
    			if(win != window) modal.close();
    			win.location.reload();
    		} else if(result.message){
    			frm_alert($frm.parent(),result.message);
        	} else {
        		frm_alert($frm.parent(),'操作失败，errcode='+result.code);
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
