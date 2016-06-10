package controllers;

import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.iap.util.ActionResult;
import com.tern.util.Trace;
import com.tern.util.html;
import com.tern.web.ControllerException;
import com.tern.web.Route;

@Route("/permission/$appName/%pgid/*")
public class PermissionController extends DataResourceController
{
	private int pgid;
	
    public PermissionController()
    {
    	this.modelName = "permission";
    }
    
    @Override
	public String index()
	{
		model = this.getModel();
		RecordSet records = model.query("A.pgid=?" , pgid).joinAll();
		
		request.setAttribute("model", model);
		request.setAttribute("records", records);
		
		return "permission/index";
	}
    
    public void create()
	{
		ActionResult ar = new ActionResult();
		this.setViewObject(ar);
		
		model = this.getModel();
		
		try
		{
		    Record record = html.new_record(model, request);
		    record.set("pgid", pgid);
		    record.save();		    		   
		}
		catch(com.tern.dao.ValueException e)
		{
			ar.setResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			Trace.write(Trace.Error, t, "create record(enum) failed.");
			ar.setResult(3,"该权限已经添加.");
		}
	}
	
	public void update(int id)
	{
		ActionResult ar = new ActionResult();
		this.setViewObject(ar);
		
		model = this.getModel();
		
		try
		{
		    Record record = html.update_record(model, request);
		    record.set("pgid", pgid);
		    record.save();
		}
		catch(com.tern.dao.ValueException e)
		{
			ar.setResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			ar.setResult(3,"该权限已经添加.");
			Trace.write(Trace.Error, t, "update record(enum) failed.");
		}
	}
	
	public String _new()
	{		
		model = this.getModel();
		
		Record record = model.create(); //new	
		record.set("pgid", pgid);
		
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "permission/edit";
	}
	
	public String edit(int id)
	{
		model = getModel();
		
		RecordSet rs = model.query("pgid=? and mid=?" , pgid,id);
		if(rs.size() == 1)
		{
			request.setAttribute("model", model);
			request.setAttribute("record", rs.get(0));
		}
		else
		{
			throw new ControllerException(this,"获取数据失败!");
		}
		
		return modelName+"/edit";
	}
	
	public void delete()
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		model = getModel();
		
		String ids = request.getParameter("items");
		if(ids==null || ids.length()<=0)
		{
			r.setResult(1);
			return;
		}
		
		String[] arr = ids.split(",");
	    if(arr.length<=0)
	    {
	    	r.setResult(1);
	    	return;
	    }
	    
	    model.delete("pgid=? and mid in ("+ids+")", pgid);
	}
}
