package controllers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tern.iap.AppContext;
import com.tern.iap.Application;
import com.tern.web.Controller;
import com.tern.web.ControllerException;
import com.tern.web.Route;
import com.tern.web.routes.ControllerWrapper;
import com.tern.web.routes.Path;
import com.tern.web.routes.RouteSet;

/*
 * 展示应用的所有请求路由
 * */
@Route("/route/$appName/*")
public class RouteController extends Controller
{
	private String appName;
	
	public String index()
	{
		AppContext ctx = AppContext.getAppContext(appName);
		if(ctx == null)
		{
			throw new ControllerException(this,"app:"+appName+"不存在。");
		}
		
		RouteSet rs = ctx.getRouter();
		
		request.setAttribute("appName", appName);
		
		List<PathInfo> items = parseRouter(rs);		
		request.setAttribute("appRoutes", items);
		
		rs = Application.getRouter();
		items = parseRouter(rs);
		request.setAttribute("iapRoutes", items);
		
		return "route";
	}
	
	private static List<PathInfo> parseRouter(RouteSet rs)
	{
		Map<String,List<Path>> paths = rs.getPaths();
		List<PathInfo> items = new ArrayList<PathInfo>();
		
		for(String key:paths.keySet())
		{			
			List<Path> list = paths.get(key);
			for(Path path:list)
			{
				PathInfo info = new PathInfo();
				info.url = path.getUrl();
				
				Object target =  path.getTarget();				
				
				if(target instanceof ControllerWrapper)
				{
					info.value = target.toString();
					info.items = parseRouter(((ControllerWrapper)target).getRouteSet());
				}
				else if(target instanceof Method)
				{
					Method m =(Method)target;
					info.value = m.getDeclaringClass().getName()+"."+m.getName();
				}
				
				items.add(info);
			}
		}
		
		return items;
	}
	
	public static class PathInfo
	{
		String url;
		String value;
		
		List<PathInfo> items;
		
		public String getUrl(){return url;}
		public String getValue(){return value;}
				
		public String toString()
		{
			return url + ":" + value;
		}
		
		public List<PathInfo> getItems()
		{
			return items;
		}
	}
}
