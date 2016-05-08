package controllers;

import com.tern.iap.AppContext;
import com.tern.web.Controller;

public class HomeController extends Controller
{
	public void index()
	{
		String appname = request.getParameter("appname");
		if(appname == null || appname.length() <= 0
			|| AppContext.getAppContext(appname) == null)
		{
			appname = AppContext.getDefault().getApplicationName();
		}		
				
		request.setAttribute("appname", appname);
	}
}
