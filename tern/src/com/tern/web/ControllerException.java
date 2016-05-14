package com.tern.web;

@SuppressWarnings("serial")
public class ControllerException extends RedirectRequest
{	
	public ControllerException(Controller ctrl, /*Throwable e,*/ String message)
	{		
		super(2, "/error");
		
		ctrl.request.setAttribute("message", message);
	}

	protected ControllerException(int mode,String url)
	{
		super(mode,url);
	}
}
