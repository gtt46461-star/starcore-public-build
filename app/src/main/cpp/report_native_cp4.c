/* SQLite/report JNI is diagnostics-only. Application process may use CAS directory walker only. */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <dirent.h>
#include <limits.h>
#include "vendor/sqlite3.h"
#define ZSTD_STATIC_LINKING_ONLY
#include "vendor/zstd/lib/zstd.h"
#ifndef CP4_HOST
#include <jni.h>
#endif
#define CP4_CANCEL -1001
#define CP4_IO -1002
#define CP4_CODEC -1003
#define CP4_EXISTS -1004
#define CP4_VERIFY -1005
#define CP4_MEMORY -1006
typedef int (*check_fn)(void *);
typedef void (*backup_progress_fn)(void *,int,int,int);
typedef int (*write_fn)(void *,const void *,size_t);
static int64_t now_ms(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return (int64_t)t.tv_sec*1000+t.tv_nsec/1000000;}
static int allowed(check_fn fn,void *p,int64_t end){return now_ms()<end && (!fn||fn(p));}
static void nap_ms(long ms){struct timespec t={ms/1000,(ms%1000)*1000000};nanosleep(&t,0);}
static int configure(sqlite3 *db){
 sqlite3_busy_timeout(db,50);
 return sqlite3_exec(db,"PRAGMA mmap_size=0; PRAGMA cache_size=-512; PRAGMA temp_store=FILE;",0,0,0);
}
/* Page backup does not need parsed table/index/trigger ASTs. cache_size and a
 * sqlite_schema SELECT force that unbounded schema allocation before copying.
 * Keep the hard limit, release clean cache pages under pressure, and use only
 * pragmas that do not require schema loading on these backup connections. */
static int configure_backup(sqlite3 *db){
 sqlite3_busy_timeout(db,50);
 int rc=sqlite3_db_config(db,SQLITE_DBCONFIG_LOOKASIDE,0,0,0);
 if(rc!=SQLITE_OK)return rc;
 return sqlite3_exec(db,"PRAGMA mmap_size=0; PRAGMA cache_spill=ON; PRAGMA temp_store=FILE;",0,0,0);
}
/* dest is private/exclusively created; source is opened READONLY, never CREATE/immutable. */
static int cp4_backup_impl(const char *source,const char *dest,int64_t memory,int64_t budget,check_fn check,void *opaque,backup_progress_fn progress){
 if(memory<2*1024*1024 || budget<0) return CP4_MEMORY;
 int fd=open(dest,O_CREAT|O_EXCL|O_RDWR,0600);if(fd<0)return errno==EEXIST?CP4_EXISTS:CP4_IO;close(fd);
 sqlite3 *src=0,*dst=0;sqlite3_backup *backup=0;int rc=SQLITE_OK,done=0,read_tx=0;int64_t busy_since=0,end=budget==0?INT64_MAX:now_ms()+budget;
 sqlite3_hard_heap_limit64(memory); /* this vendored library serves no other clients in :diagnostics */
 sqlite3_soft_heap_limit64(memory-512*1024);
 if((rc=sqlite3_open_v2(source,&src,SQLITE_OPEN_READONLY|SQLITE_OPEN_FULLMUTEX,0))!=SQLITE_OK)goto cleanup;
 if((rc=configure_backup(src))!=SQLITE_OK)goto cleanup;
 /* Pin one real read snapshot. Without this transaction every live writer can restart
  * sqlite3_backup_step from page one indefinitely. WAL writers continue normally;
  * the retained WAL is released when this connection closes, including cancellation. */
 if((rc=sqlite3_exec(src,"PRAGMA query_only=ON;BEGIN;PRAGMA schema_version;",0,0,0))!=SQLITE_OK)goto cleanup;
 read_tx=1;
 if(sqlite3_txn_state(src,"main")!=SQLITE_TXN_READ){rc=CP4_VERIFY;goto cleanup;}
 if((rc=sqlite3_open_v2(dest,&dst,SQLITE_OPEN_READWRITE|SQLITE_OPEN_FULLMUTEX,0))!=SQLITE_OK)goto cleanup;
 if((rc=configure_backup(dst))!=SQLITE_OK)goto cleanup;
 if((rc=sqlite3_exec(dst,"PRAGMA journal_mode=DELETE; PRAGMA synchronous=FULL;",0,0,0))!=SQLITE_OK)goto cleanup;
 backup=sqlite3_backup_init(dst,"main",src,"main");if(!backup){rc=sqlite3_errcode(dst);goto cleanup;}
 for(;;){
  if(!allowed(check,opaque,end)){rc=CP4_CANCEL;break;}
  rc=sqlite3_backup_step(backup,100);
  if(progress)progress(opaque,sqlite3_backup_pagecount(backup),sqlite3_backup_remaining(backup),rc);
  if(rc==SQLITE_DONE){done=1;rc=SQLITE_OK;break;}
  if(rc==SQLITE_BUSY||rc==SQLITE_LOCKED){
   if(!busy_since)busy_since=now_ms();
   /* A lock that never makes progress is a failure, not an unlimited size/time cap. */
   if(now_ms()-busy_since>=30000)break;
   nap_ms(25);continue;
  }
  if(rc!=SQLITE_OK)break;
  busy_since=0;nap_ms(1); /* Yield CPU while preserving the fixed WAL snapshot. */
 }
 {int finish=sqlite3_backup_finish(backup);backup=0;if(rc==SQLITE_OK)rc=finish;}
 if(!done && rc==SQLITE_OK)rc=CP4_VERIFY;
 /* Copying is complete. Release both page caches and the source snapshot
  * before journal-mode normalization parses the destination schema. Otherwise
  * a schema that fits alone can exhaust the same 4MiB limit while both copy
  * connections are still alive. The completed private copy is not published. */
 if(rc==SQLITE_OK){rc=sqlite3_exec(src,"ROLLBACK;",0,0,0);read_tx=0;}
 if(rc==SQLITE_OK){rc=sqlite3_close(src);if(rc==SQLITE_OK)src=0;}
 if(rc==SQLITE_OK){rc=sqlite3_close(dst);if(rc==SQLITE_OK)dst=0;}
 if(rc==SQLITE_OK&&!allowed(check,opaque,end))rc=CP4_CANCEL;
 /* Convert only the isolated destination to rollback-journal form for API21 read-only reopen. */
 if(rc==SQLITE_OK)rc=sqlite3_open_v2(dest,&dst,SQLITE_OPEN_READWRITE|SQLITE_OPEN_FULLMUTEX|SQLITE_OPEN_NOFOLLOW,0);
 if(rc==SQLITE_OK)rc=configure_backup(dst);
 if(rc==SQLITE_OK)rc=sqlite3_exec(dst,"PRAGMA journal_mode=DELETE;",0,0,0);
 if(rc==SQLITE_OK&&!allowed(check,opaque,end))rc=CP4_CANCEL;

cleanup:
 if(backup)sqlite3_backup_finish(backup);
 if(src&&read_tx){int c=sqlite3_exec(src,"ROLLBACK;",0,0,0);if(rc==SQLITE_OK)rc=c;}
 if(dst){int c=sqlite3_close(dst);if(rc==SQLITE_OK)rc=c;}
 if(src){int c=sqlite3_close(src);if(rc==SQLITE_OK)rc=c;}
 if(rc==SQLITE_OK){fd=open(dest,O_RDONLY);if(fd<0||fsync(fd)!=0)rc=CP4_IO;if(fd>=0)close(fd);}
 if(rc!=SQLITE_OK){unlink(dest);char *j=malloc(strlen(dest)+9);if(j){sprintf(j,"%s-journal",dest);unlink(j);free(j);}}
 return rc;
}
int cp4_backup(const char *source,const char *dest,int64_t memory,int64_t budget,check_fn check,void *opaque){return cp4_backup_impl(source,dest,memory,budget,check,opaque,0);}
/* R76 retention pages copy only requested rows, not every page of a multi-GB DB.
 * origin is a URI mode=ro attachment. One read transaction pins schema, cells and
 * generations together; no live table, trigger, journal mode or revision is changed. */
typedef struct {check_fn check;void *opaque;int cancelled;} r76_control;
static int r76_progress(void *opaque){r76_control *c=opaque;if(c->check&&!c->check(c->opaque)){c->cancelled=1;return 1;}return 0;}
static char *r76_readonly_uri(const char *path){
 size_t n=strlen(path);if(n>(SIZE_MAX-32)/3)return 0;char *u=malloc(n*3+32);if(!u)return 0;
 char *p=u;memcpy(p,"file:",5);p+=5;static const char hex[]="0123456789ABCDEF";
 for(size_t i=0;i<n;i++){unsigned char c=(unsigned char)path[i];
  if((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='/'||c=='_'||c=='-'||c=='.'){*p++=(char)c;}
  else{*p++='%';*p++=hex[c>>4];*p++=hex[c&15];}}
 memcpy(p,"?mode=ro",9);return u;
}
int cp76_retention_page(const char *source,const char *dest,const char *table,const int64_t *rows,int count,
                       int64_t memory,int64_t max_bytes,check_fn check,void *opaque){
 if(!source||!dest||!table||!rows||count<1||count>128||strlen(table)>256||memory<2*1024*1024||max_bytes<65536)return SQLITE_MISUSE;
 for(int i=1;i<count;i++)if(rows[i]<=rows[i-1])return SQLITE_MISUSE;
 if(check&&!check(opaque))return CP4_CANCEL;
 int fd=open(dest,O_CREAT|O_EXCL|O_RDWR,0600);if(fd<0)return errno==EEXIST?CP4_EXISTS:CP4_IO;close(fd);
 sqlite3 *db=0,*encoding_db=0;sqlite3_stmt *st=0,*insert=0,*revision=0;char *uri=0,*ddl=0,*sql=0,*columns=0;
 int rc=SQLITE_OK,tx=0;r76_control control={check,opaque,0};
 sqlite3_hard_heap_limit64(memory);sqlite3_soft_heap_limit64(memory-512*1024);
 /* SQLite attachments must have the main DB's encoding. Encoding is immutable
  * for an existing DB; read it before ATTACH, then pin all row data below. */
 if((rc=sqlite3_open_v2(source,&encoding_db,SQLITE_OPEN_READONLY|SQLITE_OPEN_FULLMUTEX,0))!=SQLITE_OK)goto end;
 if((rc=sqlite3_prepare_v2(encoding_db,"PRAGMA encoding",-1,&st,0))!=SQLITE_OK)goto end;
 rc=sqlite3_step(st);if(rc!=SQLITE_ROW)goto end;
 sql=sqlite3_mprintf("PRAGMA main.encoding=%Q",sqlite3_column_text(st,0));if(!sql){rc=SQLITE_NOMEM;goto end;}
 sqlite3_finalize(st);st=0;rc=sqlite3_close(encoding_db);if(rc!=SQLITE_OK)goto end;encoding_db=0;
 if((rc=sqlite3_open_v2(dest,&db,SQLITE_OPEN_READWRITE|SQLITE_OPEN_FULLMUTEX|SQLITE_OPEN_URI|SQLITE_OPEN_NOFOLLOW,0))!=SQLITE_OK)goto end;
 rc=sqlite3_exec(db,sql,0,0,0);sqlite3_free(sql);sql=0;if(rc!=SQLITE_OK)goto end;
 if((rc=configure(db))!=SQLITE_OK)goto end;
 sqlite3_progress_handler(db,1000,r76_progress,&control);
 if((rc=sqlite3_exec(db,"PRAGMA page_size=4096;PRAGMA journal_mode=DELETE;PRAGMA synchronous=FULL;PRAGMA foreign_keys=OFF;",0,0,0))!=SQLITE_OK)goto end;
 sql=sqlite3_mprintf("PRAGMA max_page_count=%lld;",(long long)(max_bytes/4096));if(!sql){rc=SQLITE_NOMEM;goto end;}
 rc=sqlite3_exec(db,sql,0,0,0);sqlite3_free(sql);sql=0;if(rc!=SQLITE_OK)goto end;
 uri=r76_readonly_uri(source);if(!uri){rc=SQLITE_NOMEM;goto end;}
 if((rc=sqlite3_prepare_v2(db,"ATTACH DATABASE ? AS origin",-1,&st,0))!=SQLITE_OK)goto end;
 if((rc=sqlite3_bind_text(st,1,uri,-1,SQLITE_STATIC))!=SQLITE_OK)goto end;
 rc=sqlite3_step(st);if(rc!=SQLITE_DONE)goto end;sqlite3_finalize(st);st=0;
 if(sqlite3_db_readonly(db,"origin")!=1){rc=CP4_VERIFY;goto end;}
 if((rc=sqlite3_exec(db,"BEGIN",0,0,0))!=SQLITE_OK)goto end;tx=1;
 if((rc=sqlite3_prepare_v2(db,"SELECT sql FROM origin.sqlite_master WHERE type='table' AND name=?",-1,&st,0))!=SQLITE_OK)goto end;
 if((rc=sqlite3_bind_text(st,1,table,-1,SQLITE_STATIC))!=SQLITE_OK)goto end;
 rc=sqlite3_step(st);if(rc!=SQLITE_ROW){if(rc==SQLITE_DONE)rc=SQLITE_NOTFOUND;goto end;}
 if(sqlite3_column_bytes(st,0)<1||sqlite3_column_bytes(st,0)>65536){rc=CP4_VERIFY;goto end;}
 ddl=sqlite3_mprintf("%s",sqlite3_column_text(st,0));if(!ddl){rc=SQLITE_NOMEM;goto end;}
 sqlite3_finalize(st);st=0;
 if(sqlite3_txn_state(db,"origin")!=SQLITE_TXN_READ){rc=CP4_VERIFY;goto end;}
 if((rc=sqlite3_prepare_v2(db,"PRAGMA origin.encoding",-1,&st,0))!=SQLITE_OK)goto end;
 rc=sqlite3_step(st);if(rc!=SQLITE_ROW)goto end;
 sql=sqlite3_mprintf("PRAGMA main.encoding=%Q",sqlite3_column_text(st,0));if(!sql){rc=SQLITE_NOMEM;goto end;}
 sqlite3_finalize(st);st=0;rc=sqlite3_exec(db,sql,0,0,0);sqlite3_free(sql);sql=0;if(rc!=SQLITE_OK)goto end;
 if((rc=sqlite3_exec(db,ddl,0,0,0))!=SQLITE_OK)goto end;
 /* Original table DDL (including generated expressions/constraints) is unchanged.
  * table_info lists only insertable columns. Explicit lists avoid generated columns. */
 sql=sqlite3_mprintf("PRAGMA origin.table_info(\"%w\")",table);if(!sql){rc=SQLITE_NOMEM;goto end;}
 rc=sqlite3_prepare_v2(db,sql,-1,&st,0);sqlite3_free(sql);sql=0;if(rc!=SQLITE_OK)goto end;
 sqlite3_str *list=sqlite3_str_new(db);int n=0,shadow=0;
 while((rc=sqlite3_step(st))==SQLITE_ROW){const char *name=(const char*)sqlite3_column_text(st,1);
  if(!name||++n>1024){rc=CP4_VERIFY;break;}
  if(!sqlite3_stricmp(name,"rowid"))shadow=1;
  sqlite3_str_appendf(list,",\"%w\"",name);
 }
 columns=sqlite3_str_finish(list);sqlite3_finalize(st);st=0;
 if(rc!=SQLITE_DONE)goto end;if(!columns){rc=SQLITE_NOMEM;goto end;}if(!n||shadow){rc=CP4_VERIFY;goto end;}
 sql=sqlite3_mprintf("INSERT INTO main.\"%w\"(rowid%s) SELECT rowid%s FROM origin.\"%w\" WHERE rowid=?",table,columns,columns,table);
 if(!sql){rc=SQLITE_NOMEM;goto end;}rc=sqlite3_prepare_v2(db,sql,-1,&insert,0);sqlite3_free(sql);sql=0;if(rc!=SQLITE_OK)goto end;
 if((rc=sqlite3_exec(db,"CREATE TABLE research_row_revision_v2(source_table TEXT NOT NULL,source_rowid INTEGER NOT NULL,generation INTEGER NOT NULL,PRIMARY KEY(source_table,source_rowid))",0,0,0))!=SQLITE_OK)goto end;
 if((rc=sqlite3_prepare_v2(db,"INSERT INTO main.research_row_revision_v2 SELECT source_table,source_rowid,generation FROM origin.research_row_revision_v2 WHERE source_table=? AND source_rowid=?",-1,&revision,0))!=SQLITE_OK)goto end;
 if((rc=sqlite3_bind_text(revision,1,table,-1,SQLITE_STATIC))!=SQLITE_OK)goto end;
 for(int i=0;i<count;i++){
  if(r76_progress(&control)){rc=CP4_CANCEL;goto end;}
  if((rc=sqlite3_bind_int64(insert,1,rows[i]))!=SQLITE_OK)goto end;
  rc=sqlite3_step(insert);if(rc!=SQLITE_DONE)goto end;
  if(sqlite3_changes(db)!=1){rc=SQLITE_NOTFOUND;goto end;}
  if((rc=sqlite3_reset(insert))!=SQLITE_OK)goto end;
  if((rc=sqlite3_bind_int64(revision,2,rows[i]))!=SQLITE_OK)goto end;
  rc=sqlite3_step(revision);if(rc!=SQLITE_DONE)goto end;
  if((rc=sqlite3_reset(revision))!=SQLITE_OK)goto end;
 }
 if(r76_progress(&control)){rc=CP4_CANCEL;goto end;}
 rc=sqlite3_exec(db,"COMMIT",0,0,0);if(rc==SQLITE_OK)tx=0;
end:
 if(control.cancelled)rc=CP4_CANCEL;
 sqlite3_finalize(st);sqlite3_finalize(insert);sqlite3_finalize(revision);
 sqlite3_free(sql);sqlite3_free(columns);sqlite3_free(ddl);free(uri);
 if(encoding_db)sqlite3_close(encoding_db);
 if(db){sqlite3_progress_handler(db,0,0,0);if(tx)sqlite3_exec(db,"ROLLBACK",0,0,0);int close=sqlite3_close(db);if(rc==SQLITE_OK)rc=close;}
 if(rc==SQLITE_OK){fd=open(dest,O_RDONLY);if(fd<0||fsync(fd)!=0)rc=CP4_IO;if(fd>=0)close(fd);}
 if(rc!=SQLITE_OK){unlink(dest);char *j=malloc(strlen(dest)+9);if(j){sprintf(j,"%s-journal",dest);unlink(j);free(j);}}
 return rc;
}

/* CP7 fixed-snapshot row reader. No SELECT of an unbounded TEXT/BLOB value. */
typedef struct {sqlite3 *db;sqlite3_blob *blob;} cp7_reader;
static int cp7_blob_close(cp7_reader *r){if(!r)return SQLITE_MISUSE;int rc=SQLITE_OK;if(r->blob){rc=sqlite3_blob_close(r->blob);r->blob=0;}return rc;}
static int cp7_reader_close(cp7_reader *r){if(!r)return SQLITE_OK;int rc=cp7_blob_close(r);if(r->db){int c=sqlite3_close(r->db);if(rc==SQLITE_OK)rc=c;}free(r);return rc;}
static int cp7_reader_open(const char *path,int64_t memory,cp7_reader **out){
 *out=0;if(memory<2*1024*1024)return CP4_MEMORY;
 sqlite3_memory_highwater(1);
 cp7_reader *r=calloc(1,sizeof(*r));if(!r)return CP4_MEMORY;
 sqlite3_hard_heap_limit64(memory);
 int rc=sqlite3_open_v2(path,&r->db,SQLITE_OPEN_READONLY|SQLITE_OPEN_FULLMUTEX,0);
 if(rc==SQLITE_OK)rc=configure(r->db);
 if(rc==SQLITE_OK)rc=sqlite3_exec(r->db,"PRAGMA query_only=ON;BEGIN;",0,0,0);
 if(rc!=SQLITE_OK){cp7_reader_close(r);return rc;}*out=r;return SQLITE_OK;
}
static int cp7_identifier(const char *s){return s&&*s&&strlen(s)<=256;}
static int cp7_next(cp7_reader *r,const char *table,const char *alias,int first,int64_t prior,int64_t *row,int *found){
 *found=0;if(!r||!cp7_identifier(table)||!cp7_identifier(alias))return SQLITE_MISUSE;
 int rc=cp7_blob_close(r);if(rc)return rc;
 char *sql=sqlite3_mprintf(first?"SELECT \"%w\" FROM \"%w\" ORDER BY \"%w\" LIMIT 1":"SELECT \"%w\" FROM \"%w\" WHERE \"%w\">? ORDER BY \"%w\" LIMIT 1",alias,table,alias,alias);
 if(!sql)return CP4_MEMORY;sqlite3_stmt *st=0;rc=sqlite3_prepare_v2(r->db,sql,-1,&st,0);sqlite3_free(sql);
 if(rc==SQLITE_OK&&!first)rc=sqlite3_bind_int64(st,1,prior);
 if(rc==SQLITE_OK){rc=sqlite3_step(st);if(rc==SQLITE_ROW){if(sqlite3_column_type(st,0)!=SQLITE_INTEGER)rc=SQLITE_MISMATCH;else{*row=sqlite3_column_int64(st,0);*found=1;rc=SQLITE_OK;}}else if(rc==SQLITE_DONE)rc=SQLITE_OK;}
 int c=sqlite3_finalize(st);return rc==SQLITE_OK?c:rc;
}
static int cp7_cell(cp7_reader *r,const char *table,const char *column,const char *alias,int64_t row,int64_t *kind,int64_t *bits,int64_t *length){
 if(!r||!cp7_identifier(table)||!cp7_identifier(column)||!cp7_identifier(alias))return SQLITE_MISUSE;
 int rc=cp7_blob_close(r);if(rc)return rc;
 char *sql=sqlite3_mprintf("SELECT typeof(\"%w\"),CASE WHEN typeof(\"%w\") IN ('integer','real') THEN \"%w\" ELSE NULL END FROM \"%w\" WHERE \"%w\"=?",column,column,column,table,alias);
 if(!sql)return CP4_MEMORY;sqlite3_stmt *st=0;rc=sqlite3_prepare_v2(r->db,sql,-1,&st,0);sqlite3_free(sql);
 if(rc==SQLITE_OK)rc=sqlite3_bind_int64(st,1,row);
 if(rc==SQLITE_OK){rc=sqlite3_step(st);if(rc==SQLITE_ROW){
  const char *type=(const char*)sqlite3_column_text(st,0);*bits=0;*length=0;
  if(!type)rc=SQLITE_NOMEM;
  else if(!strcmp(type,"null")){*kind=0;rc=SQLITE_OK;}
  else if(!strcmp(type,"integer")){*kind=1;*bits=sqlite3_column_int64(st,1);*length=8;rc=SQLITE_OK;}
  else if(!strcmp(type,"real")){double d=sqlite3_column_double(st,1);*kind=2;memcpy(bits,&d,8);*length=8;rc=SQLITE_OK;}
  else if(!strcmp(type,"text")){*kind=3;rc=SQLITE_OK;}
  else if(!strcmp(type,"blob")){*kind=4;rc=SQLITE_OK;}else rc=SQLITE_MISMATCH;
 }else if(rc==SQLITE_DONE)rc=SQLITE_NOTFOUND;}
 int c=sqlite3_finalize(st);if(rc==SQLITE_OK)rc=c;
 if(rc==SQLITE_OK&&(*kind==3||*kind==4)){
  rc=sqlite3_blob_open(r->db,"main",table,column,row,0,&r->blob);
  if(rc==SQLITE_OK)*length=sqlite3_blob_bytes(r->blob);
 }
 return rc;
}
static int cp7_read(cp7_reader *r,int64_t offset,unsigned char *buffer,int n){
 if(!r||!r->blob||offset<0||offset>INT32_MAX||n<1||n>8192||offset>(int64_t)sqlite3_blob_bytes(r->blob)-n)return SQLITE_RANGE;
 return sqlite3_blob_read(r->blob,buffer,n,(int)offset);
}

typedef struct {size_t used,limit,peak;} budget_t;
typedef union {size_t size;max_align_t alignment;} allocation_t;
static void *bounded_alloc(void *opaque,size_t size){
 budget_t *b=(budget_t*)opaque;if(size>SIZE_MAX-sizeof(allocation_t))return 0;
 size_t bytes=size+sizeof(allocation_t);if(bytes>b->limit-b->used)return 0;
 allocation_t *a=malloc(bytes);if(!a)return 0;a->size=bytes;b->used+=bytes;if(b->used>b->peak)b->peak=b->used;return a+1;
}
static void bounded_free(void *opaque,void *ptr){if(ptr){budget_t *b=opaque;allocation_t *a=((allocation_t*)ptr)-1;b->used-=a->size;free(a);}}
int cp4_compress(const char *path,size_t memory,int64_t budget,write_fn write,void *sink,check_fn check,void *control,size_t *peak){
 budget_t b={0,memory,0};ZSTD_customMem allocator={bounded_alloc,bounded_free,&b};
 ZSTD_CCtx *ctx=ZSTD_createCCtx_advanced(allocator);if(!ctx)return CP4_MEMORY;
 FILE *in=fopen(path,"rb");if(!in){ZSTD_freeCCtx(ctx);return CP4_IO;}
 unsigned char src[32768],dst[32768];int rc=0;int64_t end=budget==0?INT64_MAX:now_ms()+budget;
 #define SET_PARAM(k,v) if(ZSTD_isError(ZSTD_CCtx_setParameter(ctx,k,v))){rc=CP4_CODEC;goto finish;}
 SET_PARAM(ZSTD_c_compressionLevel,3);SET_PARAM(ZSTD_c_windowLog,19);SET_PARAM(ZSTD_c_checksumFlag,1);SET_PARAM(ZSTD_c_nbWorkers,0);
 for(;;){
  if(!allowed(check,control,end)){rc=CP4_CANCEL;break;}
  size_t n=fread(src,1,sizeof(src),in);if(ferror(in)){rc=CP4_IO;break;}
  int last=feof(in);ZSTD_inBuffer ib={src,n,0};size_t remaining;
  do{
   if(!allowed(check,control,end)){rc=CP4_CANCEL;goto finish;}
   ZSTD_outBuffer ob={dst,sizeof(dst),0};remaining=ZSTD_compressStream2(ctx,&ob,&ib,last?ZSTD_e_end:ZSTD_e_continue);
   if(ZSTD_isError(remaining)){rc=CP4_CODEC;goto finish;}
   if(ob.pos && write(sink,dst,ob.pos)!=0){rc=CP4_IO;goto finish;}
  }while(ib.pos<ib.size || (last&&remaining!=0));
  if(last)break;
 }
finish:
 fclose(in);ZSTD_freeCCtx(ctx);if(peak)*peak=b.peak;return rc;
}
/* CP12 streaming verification: one frame exactly, a hard output size, and bounded native allocation. */
typedef int (*read_fn)(void *,void *,size_t,size_t *);
int cp12_decompress(read_fn read,void *source,int64_t expected,size_t memory,int64_t budget,
                     write_fn write,void *sink,check_fn check,void *control,size_t *peak){
 if(!read||!write||expected<0||memory<1024*1024||budget<0)return CP4_MEMORY;
 budget_t b={0,memory,0};ZSTD_customMem allocator={bounded_alloc,bounded_free,&b};
 ZSTD_DCtx *ctx=ZSTD_createDCtx_advanced(allocator);if(!ctx)return CP4_MEMORY;
 unsigned char src[32768],dst[32768];int rc=0,frameDone=0;int64_t total=0,end=budget==0?INT64_MAX:now_ms()+budget;
 if(ZSTD_isError(ZSTD_DCtx_setParameter(ctx,ZSTD_d_windowLogMax,19))){rc=CP4_CODEC;goto done;}
 for(;;){
  if(!allowed(check,control,end)){rc=CP4_CANCEL;break;}
  size_t n=0;if(read(source,src,sizeof(src),&n)!=0||n>sizeof(src)){rc=CP4_IO;break;}
  if(!n){if(!frameDone||total!=expected)rc=CP4_VERIFY;break;}
  if(frameDone){rc=CP4_VERIFY;break;} /* reject trailing bytes, including a second valid frame */
  ZSTD_inBuffer ib={src,n,0};
  while(ib.pos<ib.size){
   if(!allowed(check,control,end)){rc=CP4_CANCEL;goto done;}
   ZSTD_outBuffer ob={dst,sizeof(dst),0};size_t before=ib.pos,remaining=ZSTD_decompressStream(ctx,&ob,&ib);
   if(ZSTD_isError(remaining)){rc=CP4_CODEC;goto done;}
   if(ob.pos>(uint64_t)(expected-total)){rc=CP4_VERIFY;goto done;}
   if(ob.pos&&write(sink,dst,ob.pos)!=0){rc=CP4_IO;goto done;}total+=(int64_t)ob.pos;
   if(remaining==0){frameDone=1;if(ib.pos!=ib.size){rc=CP4_VERIFY;goto done;}break;}
   if(ib.pos==before&&ob.pos==0){rc=CP4_CODEC;goto done;}
  }
 }
done:ZSTD_freeDCtx(ctx);if(peak)*peak=b.peak;return rc;
}
int cp12_verify_directory(const char *directory,int parts,check_fn check,void *control){
 if(!directory||parts<1||parts>999999)return CP4_VERIFY;
 DIR *dir=opendir(directory);if(!dir)return CP4_IO;int rc=0,seen=0,manifest=0;struct dirent *entry;
 errno=0;
 for(;;){
  errno=0;entry=readdir(dir);if(!entry){if(errno)rc=CP4_IO;break;}
  if(check&&!check(control)){rc=CP4_CANCEL;break;}
  const char *n=entry->d_name;if(!strcmp(n,".")||!strcmp(n,".."))continue;
  if(!strcmp(n,"manifest.jsonl")){
   struct stat manifest_st;
   if(fstatat(dirfd(dir),n,&manifest_st,AT_SYMLINK_NOFOLLOW)!=0||!S_ISREG(manifest_st.st_mode)){rc=CP4_VERIFY;break;}
   manifest++;continue;
  }
  if(strlen(n)!=11||n[0]!='P'||strcmp(n+7,".bin")){rc=CP4_VERIFY;break;}
  int number=0;for(int i=1;i<7;i++){if(n[i]<'0'||n[i]>'9'){rc=CP4_VERIFY;break;}number=number*10+n[i]-'0';}
  if(rc||number<1||number>parts){rc=CP4_VERIFY;break;}
  int rootfd=dirfd(dir);struct stat st;if(fstatat(rootfd,n,&st,AT_SYMLINK_NOFOLLOW)!=0||!S_ISREG(st.st_mode)){rc=CP4_VERIFY;break;}
  seen++;
 }
 closedir(dir);return rc?rc:(seen==parts&&manifest==1?0:CP4_VERIFY);
}

static int cp12_lower_hex(const char *s,size_t n){for(size_t i=0;i<n;i++)if(!((s[i]>='0'&&s[i]<='9')||(s[i]>='a'&&s[i]<='f')))return 0;return 1;}
static int cp12_cas_name(const char *s){size_t n=strlen(s);return (n==70&&!strcmp(s+64,".scdev")||n==71&&!strcmp(s+64,".scbind"))&&cp12_lower_hex(s,64);}
static int cp12_partial_name(const char *s){if(strlen(s)!=52||strncmp(s,"pending-",8)||strcmp(s+44,".partial"))return 0;for(int i=8;i<44;i++){int hyphen=i==16||i==21||i==26||i==31;if(hyphen?s[i]!='-':!cp12_lower_hex(s+i,1))return 0;}return 1;}
/* Streaming readdir/lstat: constant native memory, no File.listFiles/DirectoryStream array. */
static int cp12_cas_bytes(const char *path,int64_t wall,int clean,int64_t *bytes){
 if(!path||wall<0||!bytes)return CP4_VERIFY;
 int root=open(path,O_RDONLY|O_DIRECTORY|O_NOFOLLOW|O_CLOEXEC);if(root<0)return CP4_IO;
 int copy=dup(root);if(copy<0){close(root);return CP4_IO;}DIR *dir=fdopendir(copy);if(!dir){close(copy);close(root);return CP4_IO;}
 int rc=0,changed=0,count=0;int64_t total=0;
 for(;;){errno=0;struct dirent *entry=readdir(dir);if(!entry){if(errno)rc=CP4_IO;break;}const char *name=entry->d_name;if(!strcmp(name,".")||!strcmp(name,".."))continue;
  if(++count>65536){rc=CP4_VERIFY;break;}struct stat st;if(fstatat(root,name,&st,AT_SYMLINK_NOFOLLOW)||!S_ISREG(st.st_mode)||st.st_nlink!=1||st.st_size<0){rc=CP4_VERIFY;break;}
  int control=!strcmp(name,"publication.lock")||!strcmp(name,"admission.lock"),partial=cp12_partial_name(name);
  if(!control&&!partial&&!cp12_cas_name(name)){rc=CP4_VERIFY;break;}
  if((control&&st.st_size!=0)||(!control&&(st.st_size>8LL*1024*1024||(!partial&&st.st_size==0)))){rc=CP4_VERIFY;break;}
  int64_t modified_seconds=(int64_t)st.st_mtime;
  if(partial&&clean&&modified_seconds>=0&&modified_seconds<=INT64_MAX/1000&&wall>=modified_seconds*1000&&wall-modified_seconds*1000>=86400000){if(unlinkat(root,name,0)){rc=CP4_IO;break;}changed=1;continue;}
  if((int64_t)st.st_size>INT64_MAX-total){rc=CP4_VERIFY;break;}total+=(int64_t)st.st_size;
 }
 if(changed&&fsync(root))rc=CP4_IO;closedir(dir);close(root);if(rc)return rc;*bytes=total;return 0;
}

#ifdef CP4_HOST
static int write_file(void *p,const void *b,size_t n){return fwrite(b,1,n,(FILE*)p)==n?0:-1;}
int main(int argc,char **argv){
 if(argc<4)return 2;
 if(!strcmp(argv[1],"backup")){int r=cp4_backup(argv[2],argv[3],4*1024*1024,120000,0,0);printf("backup_rc=%d\n",r);return r==0?0:1;}
 if(!strcmp(argv[1],"compress")){FILE *f=fopen(argv[3],"wb");if(!f)return 2;size_t peak=0;int r=cp4_compress(argv[2],2*1024*1024,120000,write_file,f,0,0,&peak);fflush(f);fsync(fileno(f));fclose(f);printf("compress_rc=%d peak_native_codec_bytes=%zu\n",r,peak);return r==0?0:1;}
 return 2;
}
#else
typedef struct {JNIEnv *env;jobject control;jmethodID method;jmethodID progress;} jcontrol_t;
static int jcheck(void *p){jcontrol_t *c=p;jboolean r=(*c->env)->CallBooleanMethod(c->env,c->control,c->method);return !(*c->env)->ExceptionCheck(c->env)&&r;}
static void jbackup_progress(void *p,int total,int remaining,int rc){jcontrol_t *c=p;if(c->progress&&!(*c->env)->ExceptionCheck(c->env))(*c->env)->CallVoidMethod(c->env,c->control,c->progress,(jlong)total,(jlong)remaining,(jint)rc);}
typedef struct {JNIEnv *env;jobject sink;jmethodID write;jbyteArray buffer;} jsink_t;
static int jwrite(void *p,const void *bytes,size_t size){jsink_t *s=p;(*s->env)->SetByteArrayRegion(s->env,s->buffer,0,(jsize)size,bytes);if((*s->env)->ExceptionCheck(s->env))return -1;(*s->env)->CallVoidMethod(s->env,s->sink,s->write,s->buffer,0,(jint)size);return (*s->env)->ExceptionCheck(s->env)?-1:0;}

JNIEXPORT jlong JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_trainingCasBytes0(JNIEnv *env,jclass type,jstring path,jlong wall,jboolean clean){
 (void)type;if(!path){jclass ex=(*env)->FindClass(env,"java/lang/IllegalArgumentException");if(ex){(*env)->ThrowNew(env,ex,"CAS_WALK_ARGUMENT");(*env)->DeleteLocalRef(env,ex);}return 0;}
 const char *p=(*env)->GetStringUTFChars(env,path,0);if(!p)return 0;int64_t total=0;int rc=cp12_cas_bytes(p,wall,clean?1:0,&total);(*env)->ReleaseStringUTFChars(env,path,p);
 if(rc){jclass ex=(*env)->FindClass(env,"java/lang/IllegalStateException");if(ex){char detail[64];snprintf(detail,sizeof(detail),"CAS_DIRECTORY_INVALID|native=%d",rc);(*env)->ThrowNew(env,ex,detail);(*env)->DeleteLocalRef(env,ex);}return 0;}return (jlong)total;
}

JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_backup0(JNIEnv *e,jclass c,jstring source,jstring dest,jlong memory,jlong millis,jobject control){
 (void)c;const char *s=(*e)->GetStringUTFChars(e,source,0);if(!s)return CP4_MEMORY;const char *d=(*e)->GetStringUTFChars(e,dest,0);if(!d){(*e)->ReleaseStringUTFChars(e,source,s);return CP4_MEMORY;}
 jclass cc=(*e)->GetObjectClass(e,control);jmethodID mid=(*e)->GetMethodID(e,cc,"shouldContinue","()Z");int rc=CP4_CANCEL;
 if(mid){jmethodID progress=(*e)->GetMethodID(e,cc,"onBackupProgress","(JJI)V");if(progress){jcontrol_t jc={e,control,mid,progress};rc=cp4_backup_impl(s,d,memory,millis,jcheck,&jc,jbackup_progress);}}
 (*e)->DeleteLocalRef(e,cc);(*e)->ReleaseStringUTFChars(e,source,s);(*e)->ReleaseStringUTFChars(e,dest,d);return rc;
}
JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_retentionPage0(JNIEnv *e,jclass c,jstring source,jstring dest,jstring table,jlongArray ids,jlong memory,jlong maxbytes,jobject control){
 (void)c;int rc=SQLITE_MISUSE;const char *s=0,*d=0,*t=0;jclass cc=0;int64_t rows[128];
 jsize n=(*e)->GetArrayLength(e,ids);if(n<1||n>128)return rc;
 jlong values[128];(*e)->GetLongArrayRegion(e,ids,0,n,values);if((*e)->ExceptionCheck(e))return rc;
 for(int i=0;i<n;i++)rows[i]=(int64_t)values[i];
 s=(*e)->GetStringUTFChars(e,source,0);if(!s)goto done;
 d=(*e)->GetStringUTFChars(e,dest,0);if(!d)goto done;
 t=(*e)->GetStringUTFChars(e,table,0);if(!t)goto done;
 cc=(*e)->GetObjectClass(e,control);jmethodID mid=(*e)->GetMethodID(e,cc,"shouldContinue","()Z");
 if(mid){jcontrol_t jc={e,control,mid,0};rc=cp76_retention_page(s,d,t,rows,n,memory,maxbytes,jcheck,&jc);}
 done:if(cc)(*e)->DeleteLocalRef(e,cc);if(t)(*e)->ReleaseStringUTFChars(e,table,t);if(d)(*e)->ReleaseStringUTFChars(e,dest,d);if(s)(*e)->ReleaseStringUTFChars(e,source,s);return rc;
}
JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_compress0(JNIEnv *e,jclass c,jstring source,jlong memory,jlong millis,jobject out,jobject control){
 (void)c;const char *s=(*e)->GetStringUTFChars(e,source,0);if(!s)return CP4_MEMORY;
 jclass oc=(*e)->GetObjectClass(e,out),cc=(*e)->GetObjectClass(e,control);jmethodID wm=(*e)->GetMethodID(e,oc,"write","([BII)V"),cm=(*e)->GetMethodID(e,cc,"shouldContinue","()Z");jbyteArray buf=(*e)->NewByteArray(e,32768);int rc=CP4_MEMORY;
 if(wm&&cm&&buf){jsink_t sink={e,out,wm,buf};jcontrol_t jc={e,control,cm};rc=cp4_compress(s,memory,millis,jwrite,&sink,jcheck,&jc,0);}
 if(buf)(*e)->DeleteLocalRef(e,buf);(*e)->DeleteLocalRef(e,oc);(*e)->DeleteLocalRef(e,cc);(*e)->ReleaseStringUTFChars(e,source,s);return rc;
}

typedef struct {JNIEnv *env;jobject source;jmethodID read;jbyteArray buffer;} jsource_t;
static int jread(void *p,void *bytes,size_t maximum,size_t *size){
 jsource_t *s=p;jint n=(*s->env)->CallIntMethod(s->env,s->source,s->read,s->buffer,0,(jint)maximum);
 if((*s->env)->ExceptionCheck(s->env)||n==0||n>(jint)maximum||n < -1)return -1;
 if(n<0){*size=0;return 0;}(*s->env)->GetByteArrayRegion(s->env,s->buffer,0,n,bytes);
 if((*s->env)->ExceptionCheck(s->env))return -1;*size=(size_t)n;return 0;
}
JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_decompress0(JNIEnv *e,jclass c,jobject input,jobject out,jlong expected,jlong memory,jlong millis,jobject control){
 (void)c;jclass ic=(*e)->GetObjectClass(e,input),oc=(*e)->GetObjectClass(e,out),cc=(*e)->GetObjectClass(e,control);
 jmethodID rm=(*e)->GetMethodID(e,ic,"read","([BII)I"),wm=(*e)->GetMethodID(e,oc,"write","([BII)V"),cm=(*e)->GetMethodID(e,cc,"shouldContinue","()Z");
 jbyteArray ib=(*e)->NewByteArray(e,32768),ob=(*e)->NewByteArray(e,32768);int rc=CP4_MEMORY;
 if(rm&&wm&&cm&&ib&&ob){jsource_t source={e,input,rm,ib};jsink_t sink={e,out,wm,ob};jcontrol_t jc={e,control,cm};
  rc=cp12_decompress(jread,&source,expected,(size_t)memory,millis,jwrite,&sink,jcheck,&jc,0);}
 if(ib)(*e)->DeleteLocalRef(e,ib);if(ob)(*e)->DeleteLocalRef(e,ob);(*e)->DeleteLocalRef(e,ic);(*e)->DeleteLocalRef(e,oc);(*e)->DeleteLocalRef(e,cc);return rc;
}
JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_verifyArchiveDirectory0(JNIEnv *e,jclass c,jstring path,jint parts,jobject control){
 (void)c;const char *p=(*e)->GetStringUTFChars(e,path,0);if(!p)return CP4_MEMORY;jclass cc=(*e)->GetObjectClass(e,control);
 jmethodID cm=(*e)->GetMethodID(e,cc,"shouldContinue","()Z");int rc=CP4_CANCEL;
 if(cm){jcontrol_t jc={e,control,cm};rc=cp12_verify_directory(p,parts,jcheck,&jc);}
 (*e)->DeleteLocalRef(e,cc);(*e)->ReleaseStringUTFChars(e,path,p);return rc;
}

JNIEXPORT jlong JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_readerOpen0(JNIEnv *e,jclass c,jstring path,jlong memory){
 (void)c;const char *p=(*e)->GetStringUTFChars(e,path,0);if(!p)return 0;cp7_reader *r=0;int rc=cp7_reader_open(p,memory,&r);(*e)->ReleaseStringUTFChars(e,path,p);
 if(rc!=SQLITE_OK){jclass ex=(*e)->FindClass(e,"java/io/IOException");char msg[64];snprintf(msg,sizeof(msg),"REPORT_ROW_READER_OPEN|native=%d",rc);if(ex){(*e)->ThrowNew(e,ex,msg);(*e)->DeleteLocalRef(e,ex);}return 0;}return (jlong)(intptr_t)r;
}
JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_readerClose0(JNIEnv *e,jclass c,jlong h){(void)e;(void)c;return cp7_reader_close((cp7_reader*)(intptr_t)h);}
JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_readerNext0(JNIEnv *e,jclass c,jlong h,jstring table,jstring alias,jboolean first,jlong prior,jlongArray result){
 (void)c;if((*e)->GetArrayLength(e,result)!=2)return SQLITE_MISUSE;
 const char *t=(*e)->GetStringUTFChars(e,table,0);if(!t)return CP4_MEMORY;const char *a=(*e)->GetStringUTFChars(e,alias,0);if(!a){(*e)->ReleaseStringUTFChars(e,table,t);return CP4_MEMORY;}
 int64_t row=0;int found=0;int rc=cp7_next((cp7_reader*)(intptr_t)h,t,a,first,prior,&row,&found);
 if(rc==SQLITE_OK){jlong x[2]={found,row};(*e)->SetLongArrayRegion(e,result,0,2,x);}
 (*e)->ReleaseStringUTFChars(e,table,t);(*e)->ReleaseStringUTFChars(e,alias,a);return rc;
}
JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_readerCell0(JNIEnv *e,jclass c,jlong h,jstring table,jstring column,jstring alias,jlong row,jlongArray result){
 (void)c;if((*e)->GetArrayLength(e,result)!=3)return SQLITE_MISUSE;
 const char *t=(*e)->GetStringUTFChars(e,table,0);if(!t)return CP4_MEMORY;const char *col=(*e)->GetStringUTFChars(e,column,0);if(!col){(*e)->ReleaseStringUTFChars(e,table,t);return CP4_MEMORY;}
 const char *a=(*e)->GetStringUTFChars(e,alias,0);if(!a){(*e)->ReleaseStringUTFChars(e,column,col);(*e)->ReleaseStringUTFChars(e,table,t);return CP4_MEMORY;}
 int64_t kind=0,bits=0,length=0;int rc=cp7_cell((cp7_reader*)(intptr_t)h,t,col,a,row,&kind,&bits,&length);
 if(rc==SQLITE_OK){jlong x[3]={kind,bits,length};(*e)->SetLongArrayRegion(e,result,0,3,x);}
 (*e)->ReleaseStringUTFChars(e,table,t);(*e)->ReleaseStringUTFChars(e,column,col);(*e)->ReleaseStringUTFChars(e,alias,a);return rc;
}
JNIEXPORT jint JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_readerRead0(JNIEnv *e,jclass c,jlong h,jlong offset,jbyteArray output,jint n){
 (void)c;if(n<1||n>8192||(*e)->GetArrayLength(e,output)<n)return SQLITE_RANGE;unsigned char b[8192];int rc=cp7_read((cp7_reader*)(intptr_t)h,offset,b,n);if(rc==SQLITE_OK)(*e)->SetByteArrayRegion(e,output,0,n,(jbyte*)b);return rc;
}
JNIEXPORT jlong JNICALL Java_com_ethk3_novaonepro_ReportNativeCP4_readerPeak0(JNIEnv *e,jclass c,jlong h){(void)e;(void)c;return h?sqlite3_memory_highwater(0):0;}
#endif

/* Independent explicit offline-maintenance entry points; existing report JNI unchanged. */
#include "storage_maintenance_r82.inc"
