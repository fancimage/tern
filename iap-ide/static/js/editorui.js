$(function(){
	$('a,input[type="button"],input[type="submit"],input[type="radio"],input[type="checkbox"]').focus(function(){
        this.blur();
    });
    $("a[href='#']").click(function(event){
        event.preventDefault();
    });
    dropDown();
    //switchover();
});
//下拉弹出
function dropDown(){
	var dropDown = $(".drop-down");
	if(dropDown.length>0){
		$(".drop-details").hide();
		dropDown.find(".drop-link").click(function(){
			$(this).closest(".drop-down").find(".drop-details").toggle();
		});
	}
}

//文本编辑
function textEdit(textDefault){
	var textEdit = $(".text-edit");
	textEdit.each(function(){
		var this_ = $(this),
			text = this_.find(".text"),
			input = this_.find(".inputText");
		if($.trim(text.text())==""){
			text.text(textDefault).hide();
			input.val(textDefault).show();
		}else{
			input.hide();
			text.show();
		}
		this_.find(".icon-save").click(function(){
			var textEdit = $(this).closest(".text-edit");
			var text = textEdit.find(".inputText").val();
			if($.trim(text)==""){
				text = textDefault;
			}
			textEdit.find(".inputText").hide();
			textEdit.find(".text").text(text).show();
			$(this).removeClass("icon-save").addClass("icon-edit");
		});
		this_.find(".icon-edit").click(function(){
			var textEdit = $(this).closest(".text-edit");
			var text = textEdit.find(".text").text();
			textEdit.find(".text").hide();
			textEdit.find(".inputText").val(text).show();
			$(this).removeClass("icon-edit").addClass("icon-save");
		});
	});
}
//颜色面板设置
function colorSet(){
	$(".colorsPanel .colors a").click(function(){
		var this_ = $(this),
			colorsPanel = this_.closest(".colorsPanel"),
			colorValue = "#"+this_.attr("color"),
			background = colorValue,
			border = colorValue;
		if(background=="#ffffff"){
			border = "#ccc";
		}
		colorsPanel.find(".colorShow").css({
			"backgroundColor":background,
			"borderColor":border
		});
		colorsPanel.find(".drop-details").hide();

	});
}
// 图形可视化编辑工具栏效果
function editor(){
	var editorWrapper = $(".editor-wrapper"),
	topTool = editorWrapper.find(".editor-tool"),
	canvas = $("#canvas");

    var $container=$('.inner');
	/*调整客户区域的大小*/
	if(window.innerWidth-4 > $container.width()){
	    $container.width(window.innerWidth-4);
	}

	if(window.innerHeight-2 > $container.height()){
	    var height = window.innerHeight -2 - $('.editor-head').height();
    	canvas.parent().height(height);
    	editorWrapper.find('.editor-body-tool').height(height);
    	editorWrapper.find('.editor-body-menu').height(height-20);
    }

	//颜色面板设置
	colorSet();
	//顶部工具栏网格设置
	canvas.parent().addClass("gridBg");
	topTool.find(".grid").click(function(){
		canvas.parent().toggleClass("gridBg");
	});
	//右侧工具栏文本编辑(系列名)
	var checked = $(".seriesList :checked"),
		show = $("#"+checked.attr("show"));
	show.find(">h1 span").text($.trim(checked.next(".text-edit").find(".text").text()));

	textEdit("未命名");
	$(".body-tool-text .text-edit .icon-dell").click(function(){
		$(this).closest(".form-entry").remove();
	});
	$(".seriesList :radio").click(function(){
		var text = $(this).next(".text-edit").find(".text").text();
		text = $.trim(text);
		var show = $(this).attr("show");
		$("#"+show).find(">h1 span").text(text);
	});
	//右侧工具栏时间设置
	$(".editor-body-tool .toggle").each(function(){
		var this_ = $(this),
			arrow = this_.find(".toggle-arrow"),
			toggleContent = this_.next(".toggleContent");
		if(arrow.hasClass("up")){
			toggleContent.show();
		}else{
			toggleContent.hide();
		}
		arrow.click(function(){
			toggleContent.toggle();
			arrow.toggleClass("up");
		});
	});
	//字体样式，段落选中样式
	$(".editor-wrapper .operate").each(function(){
		$(this).click(function(){
			if($(this).hasClass("persist")){
				$(this).toggleClass("active");
			}
			
		})
	})
}

