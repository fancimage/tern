package com.tern.iap;

public class Menu 
{
	protected int id;
    protected String code;
    protected String caption;
    protected String url;
    protected String target;
    protected String icon;
    
    protected Menu[] child;
    
    public Menu(int id,String code,String caption,String url,String target,String icon)
    {
    	this.id = id;
    	this.code = code;
    	this.caption = caption;
    	this.url = url;
    	this.target = target;
    	this.icon = icon;
    }
    
    public int getId(){return id;}
    public String getCode(){return code;}
    public String getCaption(){return caption;}
    public String getUrl(){return url;}
    public String getTarget(){return target;}
    public String getIcon(){return icon;}
    public Menu[] getChildItem(){return child;}
    
    /*@Override
    public String toString()
    {
    	return String.format("{\"code\":\"%s\",\"caption\":\"%s\",\"url\":\"%s\",\"target\":\"%s\"}",
    			code,caption,url,target);
    }*/
}
