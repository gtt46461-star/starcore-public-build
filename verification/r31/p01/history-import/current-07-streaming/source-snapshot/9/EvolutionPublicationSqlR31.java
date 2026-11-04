package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import java.util.regex.*;

/** Compiles the owner's INSERT/point-UPDATE dialect before acquiring the writer. */
final class EvolutionPublicationSqlR31 {
    private EvolutionPublicationSqlR31() {}
    static final class Shape {
        final String sql,table,verb,where;final List<String> columns,values,assignments,primaryKeys;
        final List<Integer> keyArguments;final List<String> keyLiterals;final int whereArgument;final Set<String> foreignTables=new HashSet<>();
        boolean firstUpdate;long chunkedArguments;
        Shape(SQLiteDatabase read,String sql){
            this.sql=sql;Matcher insert=Pattern.compile("(?is)^(INSERT(?: OR (?:IGNORE|REPLACE))?) INTO ([a-zA-Z0-9_]+)(?:\\(([^)]*)\\))? VALUES\\((.*)\\)$").matcher(sql);
            List<String> schema=new ArrayList<>(),pk=new ArrayList<>();Map<Integer,String> ordered=new TreeMap<>();
            Matcher update=Pattern.compile("(?is)^UPDATE ([a-zA-Z0-9_]+) SET (.+?) WHERE (.+)$").matcher(sql);
            boolean isInsert=insert.matches();if(!isInsert&&!update.matches())throw unsupported(sql);
            table=isInsert?insert.group(2):update.group(1);identifier(table);
            try(Cursor c=read.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext()){schema.add(c.getString(1));if(c.getInt(5)>0)ordered.put(c.getInt(5),c.getString(1));}}
            pk.addAll(ordered.values());if(schema.isEmpty()||pk.isEmpty())throw unsupported("TABLE_OR_PRIMARY_KEY:"+table);primaryKeys=Collections.unmodifiableList(pk);
            try(Cursor c=read.rawQuery("PRAGMA foreign_key_list("+table+")",null)){while(c.moveToNext())foreignTables.add(c.getString(2));}
            // Grouping must not reorder application-defined cross-row triggers. A DELETE-only
            // retention trigger is inactive here because no DELETE is accepted and REPLACE's
            // implicit DELETE triggers require recursive_triggers=ON (checked by the caller).
            try(Cursor c=read.rawQuery("SELECT sql FROM sqlite_master WHERE type='trigger' AND tbl_name=?",new String[]{table})){
                while(c.moveToNext()){String ddl=c.getString(0).toUpperCase(Locale.ROOT);boolean relevant=ddl.matches("(?s).*\\b(?:BEFORE|AFTER)\\s+"+(isInsert?"INSERT":"UPDATE")+"\\b.*");if(relevant&&!ddl.contains("BEGIN SELECT RAISE"))throw unsupported("CANONICAL_TRIGGER_REQUIRES_EXPLICIT_OWNER_PLAN:"+table);}
            }
            keyArguments=new ArrayList<>();keyLiterals=new ArrayList<>();
            if(isInsert){verb=insert.group(1);where=null;assignments=Collections.emptyList();columns=insert.group(3)==null?schema:split(insert.group(3));values=split(insert.group(4));whereArgument=0;
                if(columns.size()!=values.size())throw unsupported("INSERT_SHAPE:"+table);
                for(String column:columns)identifier(column);
                for(String key:pk){int position=columns.indexOf(key);if(position<0)throw unsupported("MISSING_INSERT_KEY:"+key);String value=values.get(position);int argument=0;for(int i=0;i<position;i++)argument+=questionCount(values.get(i));
                    if(value.equals("?")){keyArguments.add(argument);keyLiterals.add(null);}else{keyArguments.add(-1);keyLiterals.add(literal(value));}}
                for(String value:values)if(!value.equals("?"))literal(value);
            }else{verb="UPDATE";where=update.group(3);assignments=split(update.group(2));columns=Collections.emptyList();values=Collections.emptyList();int arguments=0;
                Set<String> changed=new HashSet<>();for(String assignment:assignments){String[] pair=assignment.split("=",2);if(pair.length!=2)throw unsupported("SET:"+sql);identifier(pair[0].trim());changed.add(pair[0].trim());String value=pair[1].trim();if(!value.equals("?"))literal(value);arguments+=questionCount(value);}whereArgument=arguments;
                for(String key:pk){Matcher match=Pattern.compile("(?i)(?<![a-zA-Z0-9_])"+key+"\\s*=\\s*(\\?|'(?:''|[^'])*'|-?[0-9]+)(?![a-zA-Z0-9_])").matcher(where);
                    if(!match.find())throw unsupported("NON_POINT_UPDATE:"+key+":"+sql);String value=match.group(1);int argument=whereArgument+questionCount(where.substring(0,match.start(1)));
                    if(value.equals("?")){keyArguments.add(argument);keyLiterals.add(null);}else{keyArguments.add(-1);keyLiterals.add(literal(value));}}
                Set<String> predicateChanges=new HashSet<>();for(String name:changed)if(!pk.contains(name)&&Pattern.compile("(?i)(?<![a-zA-Z0-9_])"+name+"(?![a-zA-Z0-9_])").matcher(where).find())predicateChanges.add(name);
                if(!predicateChanges.isEmpty()){
                    if(table.equals("r27_policy")&&predicateChanges.equals(Collections.singleton("state"))&&where.contains("state NOT IN ('LIVE')"))firstUpdate=false;
                    else if(table.equals("r30r1_proposal")&&predicateChanges.equals(Collections.singleton("state"))&&where.contains("state='READY'"))firstUpdate=true;
                    else if(table.equals("r27_live_shadow")&&predicateChanges.equals(Collections.singleton("status"))&&where.contains("status IN ('ACTIVE','ACTIVE_REPAIR1')"))firstUpdate=true;
                    else throw unsupported("STATEFUL_UPDATE_REQUIRES_EXPLICIT_OWNER_PLAN:"+sql);
                }
            }
        }
        String key(ResearchWriteQueueR27.Op op){List<String> key=new ArrayList<>();for(int i=0;i<keyArguments.size();i++){int argument=keyArguments.get(i);Object value=argument<0?keyLiterals.get(i):op.args[argument];if(value==null||value instanceof byte[])throw unsupported("NULL_OR_BLOB_KEY");key.add(String.valueOf(value));}return table+'|'+JsonR27.write(key);}
        Object[] lookup(ResearchWriteQueueR27.Op op){Object[] keys=new Object[4];if(verb.equals("UPDATE")){if(primaryKeys.size()>4)throw unsupported("OWNER_POINT_KEY_ARITY");for(int i=0;i<primaryKeys.size();i++)keys[i]=keyArguments.get(i)<0?keyLiterals.get(i):op.args[keyArguments.get(i)];}return keys;}
        void validate(ResearchWriteQueueR27.Op op){
            if(op.args==null||op.args.length!=questionCount(sql)||op.args.length>20)throw unsupported("BIND_SHAPE:"+sql);
            if(table.equals("r27_policy")&&verb.equals("UPDATE")&&"LIVE".equals(op.args[0]))throw unsupported("LIVE_TRANSITION_NOT_OWNER_EVOLUTION");
            if(firstUpdate&&table.equals("r30r1_proposal")&&"READY".equals(op.args[0]))throw unsupported("NON_TERMINAL_PROPOSAL_UPDATE");
        }
        private String expansion(String text,int[] index){return replace(text,index,chunkedArguments);}
        String compile(String publication,int kind){
            String selected="r31_evolution_op WHERE publication_id='"+publication+"' AND kind="+kind;
            if(!verb.equals("UPDATE")){int[] index={0};List<String> projected=new ArrayList<>();for(String value:values)projected.add(expansion(value,index));return verb+" INTO "+table+"("+String.join(",",columns)+") SELECT "+String.join(",",projected)+" FROM "+selected+" ORDER BY op_seq";}
            int[] index={whereArgument};String predicate=expansion(where,index);for(int i=0;i<primaryKeys.size();i++)selected+=" AND key"+i+"="+table+"."+primaryKeys.get(i);List<String> sets=new ArrayList<>();index[0]=0;
            for(String assignment:assignments){String[] pair=assignment.split("=",2);String expression=expansion(pair[1].trim(),index);sets.add(pair[0].trim()+"=(SELECT "+expression+" FROM "+selected+" AND ("+predicate+") ORDER BY op_seq "+(firstUpdate?"ASC":"DESC")+" LIMIT 1)");}
            return "UPDATE "+table+" SET "+String.join(",",sets)+" WHERE EXISTS(SELECT 1 FROM "+selected+" AND ("+predicate+"))";
        }
    }
    static final class Plan {
        final List<Shape> shapes;final List<Integer> kinds;final List<Integer> ordered;
        Plan(SQLiteDatabase read,List<ResearchWriteQueueR27.Op> ops){
            try(Cursor c=read.rawQuery("PRAGMA recursive_triggers",null)){if(!c.moveToFirst()||c.getInt(0)!=0)throw unsupported("RECURSIVE_TRIGGERS_REQUIRE_EXPLICIT_OWNER_PLAN");}
            shapes=new ArrayList<>();kinds=new ArrayList<>();Map<String,Integer> bySql=new LinkedHashMap<>(),last=new HashMap<>();List<Set<Integer>> edges=new ArrayList<>();
            for(ResearchWriteQueueR27.Op op:ops){Integer kind=bySql.get(op.sql);if(kind==null){kind=shapes.size();bySql.put(op.sql,kind);shapes.add(new Shape(read,op.sql));edges.add(new LinkedHashSet<>());}Shape shape=shapes.get(kind);shape.validate(op);String key=shape.key(op);Integer prior=last.put(key,kind);if(prior!=null&&!prior.equals(kind))edges.get(prior).add(kind);kinds.add(kind);}
            for(int child=0;child<shapes.size();child++)for(int parent=0;parent<shapes.size();parent++)if(child!=parent&&!shapes.get(child).verb.equals("UPDATE")&&!shapes.get(parent).verb.equals("UPDATE")&&shapes.get(child).foreignTables.contains(shapes.get(parent).table))edges.get(parent).add(child);
            int[] degree=new int[shapes.size()];for(Set<Integer> group:edges)for(int target:group)degree[target]++;ordered=new ArrayList<>();Set<Integer> used=new HashSet<>();
            while(ordered.size()<shapes.size()){Integer next=null;for(int i=0;i<shapes.size();i++)if(!used.contains(i)&&degree[i]==0){next=i;break;}if(next==null)throw unsupported("NONCOMMUTING_OWNER_STATEMENT_CYCLE");used.add(next);ordered.add(next);for(int target:edges.get(next))degree[target]--;}
        }
    }
    static String argument(int index){String column="a"+index;long bit=1L<<index;
        String chunks="(SELECT group_concat(payload,'') FROM (SELECT payload FROM r31_evolution_value v WHERE v.publication_id=r31_evolution_op.publication_id AND v.op_seq=r31_evolution_op.op_seq AND v.arg_index="+index+" ORDER BY chunk_seq))";
        return "CASE WHEN (chunk_mask & "+bit+")=0 THEN "+column+" WHEN (blob_mask & "+bit+")<>0 THEN CAST("+chunks+" AS BLOB) ELSE CAST("+chunks+" AS TEXT) END";
    }
    static String replace(String text,int[] index){return replace(text,index,-1L);}
    static String replace(String text,int[] index,long chunks){StringBuilder out=new StringBuilder();boolean quoted=false;for(int i=0;i<text.length();i++){char ch=text.charAt(i);if(ch=='\''){out.append(ch);if(quoted&&i+1<text.length()&&text.charAt(i+1)=='\''){out.append(text.charAt(++i));continue;}quoted=!quoted;}else if(ch=='?'&&!quoted){int argument=index[0]++;out.append((chunks&(1L<<argument))!=0?argument(argument):"a"+argument);}else out.append(ch);}if(quoted)throw unsupported("UNBALANCED_SQL");return out.toString();}
    static int questionCount(String text){int count=0;boolean quote=false;for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='\''){if(quote&&i+1<text.length()&&text.charAt(i+1)=='\''){i++;continue;}quote=!quote;}else if(c=='?'&&!quote)count++;}if(quote)throw unsupported("UNBALANCED_SQL");return count;}
    static List<String> split(String text){List<String> out=new ArrayList<>();boolean quote=false;int depth=0,start=0;for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='\''){if(quote&&i+1<text.length()&&text.charAt(i+1)=='\''){i++;continue;}quote=!quote;}else if(!quote){if(c=='(')depth++;else if(c==')')depth--;else if(c==','&&depth==0){out.add(text.substring(start,i).trim());start=i+1;}}}if(quote||depth!=0)throw unsupported("SQL_LIST");out.add(text.substring(start).trim());return out;}
    static String literal(String value){value=value.trim();if(value.matches("-?[0-9]+(?:\\.[0-9]+)?")||value.equalsIgnoreCase("NULL"))return value;if(value.length()>=2&&value.charAt(0)=='\''&&value.charAt(value.length()-1)=='\'')return value.substring(1,value.length()-1).replace("''", "'");throw unsupported("NON_LITERAL_OWNER_SQL:"+value);}
    static void identifier(String name){if(!name.matches("[a-zA-Z_][a-zA-Z0-9_]*"))throw unsupported("IDENTIFIER");}
    static IllegalStateException unsupported(String reason){return new IllegalStateException("R31_EVOLUTION_OWNER_PLAN_REQUIRED:"+reason);}
}
