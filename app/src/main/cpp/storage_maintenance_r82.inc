/* R82 offline full-copy rebuild. Included after sqlite3.c and the existing JNI.
 * The Java owner closes its pool and holds the interprocess exclusive lease.
 * No original database byte is modified by this function. */
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int sm82_exec(sqlite3 *d, const char *q, char **error) {
    return sqlite3_exec(d,q,0,0,error)==SQLITE_OK;
}
static void sm82_fail(char **e, sqlite3 *d, const char *step) {
    if (!*e) *e=sqlite3_mprintf("%s: %s",step,d?sqlite3_errmsg(d):"invalid input");
}
static int sm82_prepare(sqlite3 *d,const char *q,sqlite3_stmt **s,char **e) {
    if(sqlite3_prepare_v2(d,q,-1,s,0)!=SQLITE_OK){sm82_fail(e,d,q);return 0;}return 1;
}
static int sm82_integer(sqlite3 *d,const char *q,sqlite3_int64 *v,char **e) {
    sqlite3_stmt *s=0;int ok=sm82_prepare(d,q,&s,e);
    if(ok){int r=sqlite3_step(s);ok=r==SQLITE_ROW&&sqlite3_column_type(s,0)==SQLITE_INTEGER;if(ok)*v=sqlite3_column_int64(s,0);else sm82_fail(e,d,q);}
    sqlite3_finalize(s);return ok;
}
static int sm82_equal_value(sqlite3_stmt *a,sqlite3_stmt *b,int c) {
    int t=sqlite3_column_type(a,c);if(t!=sqlite3_column_type(b,c))return 0;
    if(t==SQLITE_NULL)return 1;
    if(t==SQLITE_INTEGER)return sqlite3_column_int64(a,c)==sqlite3_column_int64(b,c);
    if(t==SQLITE_FLOAT){double x=sqlite3_column_double(a,c),y=sqlite3_column_double(b,c);return memcmp(&x,&y,sizeof x)==0;}
    int n=sqlite3_column_bytes(a,c);if(n!=sqlite3_column_bytes(b,c))return 0;
    const void *x=t==SQLITE_BLOB?sqlite3_column_blob(a,c):sqlite3_column_text(a,c);
    const void *y=t==SQLITE_BLOB?sqlite3_column_blob(b,c):sqlite3_column_text(b,c);
    return n==0||memcmp(x,y,(size_t)n)==0;
}
static int sm82_compare(sqlite3 *a,sqlite3 *b,const char *q,sqlite3_int64 *rows,char **e) {
    sqlite3_stmt *x=0,*y=0;int ok=sm82_prepare(a,q,&x,e)&&sm82_prepare(b,q,&y,e);
    if(ok&&sqlite3_column_count(x)!=sqlite3_column_count(y)){*e=sqlite3_mprintf("column count mismatch: %s",q);ok=0;}
    while(ok){int rx=sqlite3_step(x),ry=sqlite3_step(y);if(rx!=ry){*e=sqlite3_mprintf("row count mismatch: %s",q);ok=0;break;}
        if(rx==SQLITE_DONE)break;if(rx!=SQLITE_ROW){sm82_fail(e,a,q);ok=0;break;}
        for(int c=0;c<sqlite3_column_count(x);c++)if(!sm82_equal_value(x,y,c)){*e=sqlite3_mprintf("typed value mismatch at column %d: %s",c,q);ok=0;break;}
        if(ok&&rows)(*rows)++;
    }sqlite3_finalize(x);sqlite3_finalize(y);return ok;
}
/* Genuine rowid preservation is mandatory. If every intrinsic spelling is
 * shadowed by a user column the database is rejected, never silently reordered. */
static int sm82_copy_table(sqlite3 *src,sqlite3 *dst,const char *name,sqlite3_int64 *rows,char **e) {
    char *pragma=sqlite3_mprintf("PRAGMA table_xinfo(\"%w\")",name);sqlite3_stmt *meta=0;
    int ok=sm82_prepare(src,pragma,&meta,e);sqlite3_free(pragma);
    sqlite3_str *columns=sqlite3_str_new(src),*order=sqlite3_str_new(src);
    int n=0,shadow[3]={0,0,0};const char *aliases[3]={"rowid","_rowid_","oid"};
    while(ok){int r=sqlite3_step(meta);if(r==SQLITE_DONE)break;if(r!=SQLITE_ROW){sm82_fail(e,src,"table_xinfo");ok=0;break;}
        const char *col=(const char*)sqlite3_column_text(meta,1);int hidden=sqlite3_column_int(meta,6);
        if(!col){*e=sqlite3_mprintf("NULL column name");ok=0;break;}
        for(int i=0;i<3;i++)if(sqlite3_stricmp(col,aliases[i])==0)shadow[i]=1;
        if(hidden==1){*e=sqlite3_mprintf("unsupported hidden column: %s",name);ok=0;break;}
        if(hidden==0){if(n++)sqlite3_str_appendall(columns,",");sqlite3_str_appendf(columns,"\"%w\"",col);}
        int pk=sqlite3_column_int(meta,5);if(pk>0){if(sqlite3_str_length(order))sqlite3_str_appendall(order,",");sqlite3_str_appendf(order,"\"%w\"",col);}
    }sqlite3_finalize(meta);
    char *cols=sqlite3_str_finish(columns),*pkOrder=sqlite3_str_finish(order);const char *rowAlias=0;
    if(ok){for(int i=0;i<3;i++)if(!shadow[i]){char *probe=sqlite3_mprintf("SELECT %s FROM \"%w\" LIMIT 0",aliases[i],name);sqlite3_stmt *p=0;int rc=sqlite3_prepare_v2(src,probe,-1,&p,0);sqlite3_free(probe);sqlite3_finalize(p);if(rc==SQLITE_OK){rowAlias=aliases[i];break;}}
        if(!rowAlias){sqlite3_stmt *wr=0;char *q=sqlite3_mprintf("SELECT wr FROM pragma_table_list WHERE schema='main' AND name=%Q",name);
            ok=sm82_prepare(src,q,&wr,e);sqlite3_free(q);int isWithout=ok&&sqlite3_step(wr)==SQLITE_ROW&&sqlite3_column_int(wr,0)==1;sqlite3_finalize(wr);
            if(!isWithout){if(!*e)*e=sqlite3_mprintf("intrinsic rowid unavailable: %s",name);ok=0;}
        }
    }
    char *select=0,*insert=0;
    if(ok){select=rowAlias?sqlite3_mprintf("SELECT %s,%s FROM \"%w\" ORDER BY %s",rowAlias,cols,name,rowAlias):sqlite3_mprintf("SELECT %s FROM \"%w\" ORDER BY %s",cols,name,pkOrder);
        insert=rowAlias?sqlite3_mprintf("INSERT INTO \"%w\"(%s,%s) VALUES(?1",name,rowAlias,cols):sqlite3_mprintf("INSERT INTO \"%w\"(%s) VALUES(?1",name,cols);
        sqlite3_str *is=sqlite3_str_new(dst);sqlite3_str_appendall(is,insert);sqlite3_free(insert);for(int i=2;i<=n+(rowAlias?1:0);i++)sqlite3_str_appendf(is,",?%d",i);sqlite3_str_appendall(is,")");insert=sqlite3_str_finish(is);
        sqlite3_stmt *read=0,*write=0;ok=sm82_prepare(src,select,&read,e)&&sm82_prepare(dst,insert,&write,e);
        while(ok){int r=sqlite3_step(read);if(r==SQLITE_DONE)break;if(r!=SQLITE_ROW){sm82_fail(e,src,"copy read");ok=0;break;}
            for(int c=0;c<sqlite3_column_count(read);c++){if(sqlite3_bind_value(write,c+1,sqlite3_column_value(read,c))!=SQLITE_OK){sm82_fail(e,dst,"bind copy");ok=0;break;}}
            if(ok&&sqlite3_step(write)!=SQLITE_DONE){sm82_fail(e,dst,"copy insert");ok=0;}
            sqlite3_reset(write);sqlite3_clear_bindings(write);
        }sqlite3_finalize(read);sqlite3_finalize(write);
        if(ok){char *verify=rowAlias?sqlite3_mprintf("SELECT %s,* FROM \"%w\" ORDER BY %s",rowAlias,name,rowAlias):sqlite3_mprintf("SELECT * FROM \"%w\" ORDER BY %s",name,pkOrder);ok=sm82_compare(src,dst,verify,rows,e);sqlite3_free(verify);}
    }sqlite3_free(select);sqlite3_free(insert);sqlite3_free(cols);sqlite3_free(pkOrder);return ok;
}
static int sm82_clean_check(sqlite3 *db,const char *sql,int wantsOk,char **e) {
    sqlite3_stmt *s=0;int ok=sm82_prepare(db,sql,&s,e),count=0;
    while(ok){int r=sqlite3_step(s);if(r==SQLITE_DONE)break;if(r!=SQLITE_ROW){sm82_fail(e,db,sql);ok=0;break;}count++;
        const char *v=(const char*)sqlite3_column_text(s,0);if(!wantsOk||!v||strcmp(v,"ok")!=0||count!=1){*e=sqlite3_mprintf("validation failed: %s",sql);ok=0;break;}}
    if(ok&&wantsOk&&count!=1){*e=sqlite3_mprintf("missing integrity result: %s",sql);ok=0;}sqlite3_finalize(s);return ok;
}
static char *sm82_rebuild(const char *source,const char *target) {
    sqlite3 *src=0,*dst=0;sqlite3_stmt *schema=0;char *err=0;sqlite3_int64 rows=0,tables=0,user=0,app=0,page=0;int ok=1,created=0;
    if(!source||!target||!strcmp(source,target))return sqlite3_mprintf("ERROR: invalid paths");
    int fd=open(target,O_WRONLY|O_CREAT|O_EXCL,0600);if(fd<0)return sqlite3_mprintf("ERROR: exclusive destination create: %s",strerror(errno));close(fd);created=1;
    if(sqlite3_open_v2(source,&src,SQLITE_OPEN_READONLY|SQLITE_OPEN_NOMUTEX,0)!=SQLITE_OK){sm82_fail(&err,src,"open source");ok=0;}
    if(ok&&sqlite3_open_v2(target,&dst,SQLITE_OPEN_READWRITE|SQLITE_OPEN_NOMUTEX,0)!=SQLITE_OK){sm82_fail(&err,dst,"open destination");ok=0;}
    if(ok)ok=sm82_exec(src,"BEGIN",&err)&&sm82_clean_check(src,"PRAGMA quick_check",1,&err)&&sm82_clean_check(src,"PRAGMA foreign_key_check",0,&err);
    if(ok)ok=sm82_integer(src,"PRAGMA user_version",&user,&err)&&sm82_integer(src,"PRAGMA application_id",&app,&err)&&sm82_integer(src,"PRAGMA page_size",&page,&err);
    if(ok){char *q=sqlite3_mprintf("PRAGMA page_size=%lld;PRAGMA auto_vacuum=INCREMENTAL;PRAGMA foreign_keys=OFF;PRAGMA journal_mode=DELETE;PRAGMA synchronous=FULL;PRAGMA user_version=%lld;PRAGMA application_id=%lld;BEGIN IMMEDIATE",page,user,app);ok=sm82_exec(dst,q,&err);sqlite3_free(q);}
    if(ok){sqlite3_stmt *encoding=0;ok=sm82_prepare(src,"PRAGMA encoding",&encoding,&err);if(ok){int r=sqlite3_step(encoding);const char *enc=r==SQLITE_ROW?(const char*)sqlite3_column_text(encoding,0):0;if(!enc||(strcmp(enc,"UTF-8")&&strcmp(enc,"UTF-16le")&&strcmp(enc,"UTF-16be"))){err=sqlite3_mprintf("unsupported database encoding");ok=0;}else{char *q=sqlite3_mprintf("PRAGMA encoding=%Q",enc);ok=sm82_exec(dst,q,&err);sqlite3_free(q);}}sqlite3_finalize(encoding);}
    if(ok)ok=sm82_prepare(src,"SELECT name,sql FROM sqlite_schema WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",&schema,&err);
    while(ok){int r=sqlite3_step(schema);if(r==SQLITE_DONE)break;if(r!=SQLITE_ROW){sm82_fail(&err,src,"table schema");ok=0;break;}
        const char *name=(const char*)sqlite3_column_text(schema,0),*sql=(const char*)sqlite3_column_text(schema,1);
        if(!sql||sqlite3_strnicmp(sql,"CREATE VIRTUAL",14)==0){err=sqlite3_mprintf("unsupported virtual/opaque table: %s",name);ok=0;break;}
        ok=sm82_exec(dst,sql,&err)&&sm82_copy_table(src,dst,name,&rows,&err);if(ok)tables++;
    }sqlite3_finalize(schema);schema=0;
    /* sqlite_sequence must retain even entries for empty/deleted AUTOINCREMENT tables. */
    sqlite3_int64 sequence=0;
    if(ok)ok=sm82_integer(src,"SELECT count(*) FROM sqlite_schema WHERE type='table' AND name='sqlite_sequence'",&sequence,&err);
    if(ok&&sequence){ok=sm82_exec(dst,"DELETE FROM sqlite_sequence",&err)&&sm82_copy_table(src,dst,"sqlite_sequence",&rows,&err);}
    if(ok)ok=sm82_prepare(src,"SELECT sql FROM sqlite_schema WHERE type IN('index','view','trigger') AND sql IS NOT NULL ORDER BY CASE type WHEN 'index' THEN 0 WHEN 'view' THEN 1 ELSE 2 END,name",&schema,&err);
    while(ok){int r=sqlite3_step(schema);if(r==SQLITE_DONE)break;if(r!=SQLITE_ROW){sm82_fail(&err,src,"secondary schema");ok=0;break;}ok=sm82_exec(dst,(const char*)sqlite3_column_text(schema,0),&err);}sqlite3_finalize(schema);schema=0;
    /* Preserve planner statistics too. ANALYZE only creates the engine-owned
     * schemas; its generated values are discarded and replaced by the source. */
    sqlite3_int64 stats=0;
    if(ok)ok=sm82_integer(src,"SELECT count(*) FROM sqlite_schema WHERE type='table' AND name IN('sqlite_stat1','sqlite_stat4')",&stats,&err);
    if(ok&&stats)ok=sm82_exec(dst,"ANALYZE sqlite_schema",&err);
    if(ok&&stats)ok=sm82_prepare(src,"SELECT name FROM sqlite_schema WHERE type='table' AND name IN('sqlite_stat1','sqlite_stat4') ORDER BY name",&schema,&err);
    while(ok&&stats){int r=sqlite3_step(schema);if(r==SQLITE_DONE)break;if(r!=SQLITE_ROW){sm82_fail(&err,src,"statistics schema");ok=0;break;}const char *name=(const char*)sqlite3_column_text(schema,0);char *q=sqlite3_mprintf("DELETE FROM \"%w\"",name);ok=sm82_exec(dst,q,&err);sqlite3_free(q);if(ok)ok=sm82_copy_table(src,dst,name,&rows,&err);}sqlite3_finalize(schema);schema=0;
    if(ok)ok=sm82_compare(src,dst,"SELECT type,name,tbl_name,sql FROM sqlite_schema WHERE name NOT LIKE 'sqlite_autoindex_%' ORDER BY type,name",0,&err);
    if(ok)ok=sm82_clean_check(dst,"PRAGMA foreign_key_check",0,&err)&&sm82_exec(dst,"COMMIT",&err)&&sm82_clean_check(dst,"PRAGMA quick_check",1,&err);
    sqlite3_int64 vacuum=0;if(ok)ok=sm82_integer(dst,"PRAGMA auto_vacuum",&vacuum,&err)&&vacuum==2;
    if(ok)ok=sm82_exec(src,"COMMIT",&err);
    if(dst){if(!ok)sqlite3_exec(dst,"ROLLBACK",0,0,0);if(sqlite3_close(dst)!=SQLITE_OK&&ok){ok=0;err=sqlite3_mprintf("destination close failed");}}
    if(src){if(!ok)sqlite3_exec(src,"ROLLBACK",0,0,0);sqlite3_close(src);}
    if(ok){fd=open(target,O_RDONLY);if(fd<0||fsync(fd)!=0){ok=0;err=sqlite3_mprintf("destination fsync failed");}if(fd>=0)close(fd);}
    char *result=ok?sqlite3_mprintf("OK rows=%lld tables=%lld auto_vacuum=2 user_version=%lld application_id=%lld",rows,tables,user,app):sqlite3_mprintf("ERROR: %s",err?err:"validation failed");
    sqlite3_free(err);if(!ok&&created)unlink(target);return result;
}
#if !defined(STORAGE_MAINTENANCE_R82_HOST) && !defined(CP4_HOST)
JNIEXPORT jstring JNICALL Java_com_ethk3_novaonepro_StorageMaintenanceFileR82_nativeRebuild(JNIEnv *env,jclass cls,jstring from,jstring to) {
    (void)cls;
    if(!from||!to){jclass ex=(*env)->FindClass(env,"java/io/IOException");(*env)->ThrowNew(env,ex,"maintenance path missing");return 0;}
    const char *a=(*env)->GetStringUTFChars(env,from,0);if(!a)return 0;
    const char *b=(*env)->GetStringUTFChars(env,to,0);if(!b){(*env)->ReleaseStringUTFChars(env,from,a);return 0;}
    char *result=sm82_rebuild(a,b);jstring out=(*env)->NewStringUTF(env,result?result:"ERROR: out of memory");sqlite3_free(result);
    if(a)(*env)->ReleaseStringUTFChars(env,from,a);if(b)(*env)->ReleaseStringUTFChars(env,to,b);return out;
}
JNIEXPORT void JNICALL Java_com_ethk3_novaonepro_StorageMaintenanceFileR82_nativeFsyncDirectory(JNIEnv *env,jclass cls,jstring path) {
    (void)cls;const char *p=(*env)->GetStringUTFChars(env,path,0);int fd=p?open(p,O_RDONLY|O_DIRECTORY):-1;int rc=fd>=0?fsync(fd):-1;if(fd>=0)close(fd);if(p)(*env)->ReleaseStringUTFChars(env,path,p);
    if(rc!=0){jclass ex=(*env)->FindClass(env,"java/io/IOException");(*env)->ThrowNew(env,ex,"directory fsync failed");}
}
#endif
