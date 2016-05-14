package com.tern.iap;

import com.tern.web.ControllerException;

public class SessionExpireException extends ControllerException
{
	private static String url;
	
	static
	{
		url = com.tern.util.config.getString("application.login");
		if(url == null || url.length() <=0 )
		{
			url = "/static/login.html";
		}
	}
	
    public SessionExpireException()
    {
    	super(1,url);
    }
    
    public final String getUrl(){return url;}
}
