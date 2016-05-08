package controllers;

import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.iap.util.ActionResult;
import com.tern.util.Trace;
import com.tern.util.html;
import com.tern.web.Route;

@Route("/columns/$appName/%eid/*")
public class ColumnController extends DataResourceController
{
    private long eid;
    
    public ColumnController()
    {
    	this.modelName = "column";
    }
    
    @Override
	public String index()
	{
		model = this.getModel();
		RecordSet records = model.query("eid=?" , eid)
				                 .order("csort");
		
		request.setAttribute("model", model);
		request.setAttribute("records", records);
		
		return "column/index";
	}
    
    public void create()
	{
    	ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		model = this.getModel();
		
		try
		{
			/*得到最大csort*/
			int max = model.getDb().table(model.getName())
			             .select("max(csort)")
			             .where("eid=?",eid)
			             .queryInt();
			max += 1;
			
		    Record record = html.new_record(model, request);
		    record.set("eid", eid);
		    record.set("csort", max);
		    record.save();
		}
		catch(com.tern.dao.ValueException e)
		{
			r.setResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			Trace.write(Trace.Error, t, "create record(enum) failed.");
			r.setResult(3,"服务器异常.");
		}
	}
	
	public void update(int id)
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		model = this.getModel();
		
		try
		{
		    Record record = html.update_record(model, request);
		    record.set("eid", eid);		    
		    record.save();
		}
		catch(com.tern.dao.ValueException e)
		{
			r.setResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			r.setResult(3,"服务器异常.");
		}
	}
	
	public String _new()
	{		
		model = this.getModel();
		
		Record record = model.create(); //new	
		record.set("eid", eid);
		
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "column/edit";
	}
	
	@Route("/resort/%1/%2")
	public void resort(long first,long second) //,HttpMethod.POST
	{
		ActionResult ar = new ActionResult();
		this.setViewObject(ar);
		
		model = this.getModel();
		RecordSet rs = model.find(new long[]{first,second});
		if(rs.size() != 2)
		{
			ar.setResult(1,"请求修改顺序的记录不存在!");
			return;
		}
		
		Record r1 = rs.get(0);
		Record r2 = rs.get(1);
		if(first != r1.getId())
		{
			Record r = r2;
			r2 = r1;
			r1 = r;
		}
		
	    int sort1 = r1.getInt("csort");
	    int sort2 = r2.getInt("csort");
	    
	    if(sort2 <= sort1)
	    {
	    	sort2 = sort1 + 1;
	    }
	    
	    r1.set("csort", sort2);
	    r2.set("csort", sort1);
	    
	    try
	    {
	    	model.getDb().transaction();
	    	
	    	r1.save();
	    	r2.save();
	    	
	    	model.getDb().commit();
	    	
	    	ar.setResult(0,null);
	    }
	    catch(Exception e)
	    {
	    	model.getDb().rollback();
	    	
	    	Trace.write(Trace.Error, e, "entity change sort");
	    	ar.setResult(2,"系统错误!");
	    }
	}
}
