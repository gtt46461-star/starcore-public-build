package android.database.sqlite;
/** Android SQL construction transport adapter; executes the resulting original product query in SQLite. */
public class SQLiteQueryBuilder {
 public static String buildQueryString(boolean distinct,String tables,String[] columns,String where,String group,String having,String order,String limit) {
  if(having!=null&&!having.isEmpty()&&(group==null||group.isEmpty()))throw new IllegalArgumentException("HAVING without GROUP BY");
  StringBuilder s=new StringBuilder("SELECT ");if(distinct)s.append("DISTINCT ");
  s.append(columns==null||columns.length==0?"*":String.join(",",columns)).append(" FROM ").append(tables);
  if(where!=null&&!where.isEmpty())s.append(" WHERE ").append(where);if(group!=null&&!group.isEmpty())s.append(" GROUP BY ").append(group);
  if(having!=null&&!having.isEmpty())s.append(" HAVING ").append(having);if(order!=null&&!order.isEmpty())s.append(" ORDER BY ").append(order);
  if(limit!=null&&!limit.isEmpty())s.append(" LIMIT ").append(limit);return s.toString();
 }
}
