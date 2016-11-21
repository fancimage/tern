package controllers;

import com.tern.dao.Column;
import com.tern.dao.DataType;
import com.tern.dao.Model;
import com.tern.dao.Record;
import com.tern.db.Database;
import com.tern.iap.AppContext;
import com.tern.iap.util.ActionResult;
import com.tern.util.Convert;
import com.tern.util.Trace;
import com.tern.web.ControllerException;
import com.tern.web.Route;
import com.tern.web.routes.HttpMethod;

import java.util.Date;

@Route("/entity/$appName/*")
public class EntityController extends DataResourceController
{
    public EntityController()
    {
        this.modelName = "entity";
    }

    public void delete()
    {
        /*需级联删除*/
        ActionResult r = new ActionResult();
        this.setViewObject(r);

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

        model = getModel();

        try {
            model.getDb().transaction();

            model.from("column",model.getDb()).delete("eid in("+ids+")");
            model.delete(arr);

            model.getDb().commit();
        } catch(Throwable t){
            model.getDb().rollback();
            Trace.write(Trace.Error,t,"remove model");
        }
    }

    @Route("/import")
    public String toimport()
    {
        this.request.setAttribute("appName", appName);
        return "entity/import";
    }

    @Route(value="/import/update",method= HttpMethod.POST)
    public void doimport()
    {
        AppContext ctx = AppContext.getAppContext(appName);
        if(ctx == null || ctx.getMetaDB() == null)
        {
            throw new ControllerException(this,"app:"+appName+"不存在或未配置元数据库。");
        }

        ActionResult r = new ActionResult();
        this.setViewObject(r);

        String tableName = Convert.toString(request.getParameter("tname"));
        if(tableName.length()<=0)
        {
            r.setResult(1,"参数错误.");
            return;
        }

        String modelName = Convert.toString(request.getParameter("mname"));
        if(modelName.length()<=0)
        {
            modelName = tableName;
        }

        /*是否已经存在？*/
        Database db = ctx.getMetaDB();
        try {
            int count = db.table("iap_entities").where("ename=? or tablename=?",modelName,tableName).count();
            if(count> 0)
            {
                r.setResult(2,"模型已存在.");
                return;
            }

            Model model = null;

            try {
                model = Model.from(tableName);
            } catch(Exception e) {
                r.setResult(3,"物理表不存在");
                return;
            }


            /*将模型定义存储到数据库中*/
            Model entity =  Model.from("entity",ctx.getMetaDB());
            Record row = entity.create();
            row.set("tablename",tableName);
            row.set("ename",modelName);
            row.set("ecaption",request.getParameter("title"));
            row.set("createtime",new Date());


            ctx.getMetaDB().transaction();
            row.save();

            int eid = row.getInt("eid");

            /*各个字段的定义*/
            Model column = Model.from("column",ctx.getMetaDB());
            Column[] cols = model.getColumnList();
            for(int i=0;i<cols.length;i++)
            {
                Record col = column.create();

                col.set("eid",eid);
                col.set("cname",cols[i].getName());
                col.set("ccaption",cols[i].getCaption());
                col.set("ctype", cols[i].getType().toString().toLowerCase());

                col.set("auto",false);//默认
                col.set("iskey",cols[i].isKey());
                col.set("readonly",cols[i].isReadonly());
                col.set("nullable",cols[i].isNullable());

                col.set("format",cols[i].getFormat());
                col.set("cdefault",cols[i].getDefault());
                col.set("csort",(i+1));

                if(cols[i].getType() == DataType.Numeric)
                {
                    col.set("clength",cols[i].getLength());
                    col.set("scale",cols[i].getScale());
                    col.set("cmax",cols[i].getMax());
                    col.set("cmin",cols[i].getMin());
                }
                else if(cols[i].getType() == DataType.String)
                {
                    col.set("cmax",cols[i].getLength());
                    col.set("cmin",cols[i].getMinLength());
                }

                col.save();
            }

            ctx.getMetaDB().commit();
        }
        catch(com.tern.dao.ValueException e)
        {
            r.setResult(2,e.getMessage());
        }
        catch(Throwable t)
        {
            ctx.getMetaDB().rollback();

            Trace.write(Trace.Error,t,"import model.");
            r.setResult(4,"导入失败!");
        }
    }
}
