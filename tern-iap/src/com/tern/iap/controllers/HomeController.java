/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.controllers;

import com.tern.iap.AppContext;
import com.tern.web.Controller;

public class HomeController extends Controller
{
	public void index()
	{
		String name = AppContext.current().getApplicationName();
		request.setAttribute("message", "Welcome to "+name+"!");
	}
}
