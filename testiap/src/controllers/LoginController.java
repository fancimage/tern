package controllers;

import com.tern.util.Convert;
import com.tern.util.config;
import com.tern.web.Controller;
import com.tern.web.HttpStream;

public class LoginController extends Controller
{
	public void index()
    {
		String name = request.getParameter("username");
		String pwd = request.getParameter("password");
    	
		if(name==null || pwd == null
			|| name.trim().length()<=0 || pwd.trim().length()<=0)
		{
			writeResult(1,"参数错误!");
			return;
		}
		
		if(name.equals("admin") && pwd.equals("1"))
		{
			Object obj = session.getAttribute("user");
			if(obj!=null && obj.equals(name))
			{
				writeResult(0,"之前已经登陆成功了的!");
			}
			else
			{
			    writeResult(0,"登陆成功!");
			}
			session.setAttribute("user", name);
		}
		else
		{
			writeResult(2,"用户名或密码错误!");
		}
    }
	
	protected void writeResult(int result,String err)
    {
        this.setContentType("application/javascript");
        this.response.setCharacterEncoding(config.getEncoding());
        
        HttpStream out = this.getStream();
        out.append("{\"result\":").append(String.valueOf(result));
        
        if( err != null )
        {
        	if(err.indexOf('\n') >= 0)
        	{
        		err = Convert.replaceAll(err, "\n", "\\n");
        	}
            out.append(",\"message\":\"").append(err).append("\"");
        }
        
        out.append("}");
    }
}
