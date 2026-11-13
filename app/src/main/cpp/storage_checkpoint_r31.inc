/* Narrow online maintenance API: no caller-supplied SQL, CREATE, journal-mode
 * change, VACUUM, swap or recovery. SQLite BUSY never waits. NOOP observes the
 * actual WAL counters before bounded work; platform-created WAL is unchanged. */
#define R31_CP_CANCEL (-1101)
#define R31_CP_BUDGET (-1102)
#define R31_CP_UNSAFE (-1103)
#define R31_CP_DEFER (-1104)
#define R31_CP_MAX_COPY (4*1024*1024)
typedef struct {check_fn check;void *opaque;int64_t end;} r31_cp_control;
static int r31_cp_progress(void *opaque){r31_cp_control *p=opaque;return !allowed(p->check,p->opaque,p->end);}
int r31_storage_checkpoint(const char *path,int mode,int64_t budget,check_fn check,void *opaque,int64_t out[8]){
 if(!out)return SQLITE_MISUSE;for(int i=0;i<8;i++)out[i]=0;if(!path||mode<0||mode>2||budget<1||budget>50){out[0]=SQLITE_MISUSE;return SQLITE_MISUSE;}
 int64_t began=now_ms(),end=began+budget;for(int i=0;i<8;i++)out[i]=0;out[2]=out[3]=-1;out[6]=SQLITE_VERSION_NUMBER;
 if(!allowed(check,opaque,end)){out[0]=R31_CP_CANCEL;return R31_CP_CANCEL;}
 struct stat st;int fd=-1,rc=SQLITE_OK;sqlite3 *db=0;sqlite3_stmt *statement=0;unsigned char header[100];int log=-1,done=-1,page=0;
 char *journal=sqlite3_mprintf("%s-journal",path),*wal=sqlite3_mprintf("%s-wal",path),*shm=sqlite3_mprintf("%s-shm",path);
 if(!journal||!wal||!shm){rc=SQLITE_NOMEM;goto finish;}
 if(lstat(path,&st)||!S_ISREG(st.st_mode)||st.st_nlink!=1){rc=R31_CP_UNSAFE;goto finish;}
 if(lstat(journal,&st)==0&&st.st_size>0){rc=R31_CP_UNSAFE;goto finish;}
 fd=open(path,O_RDONLY|O_NOFOLLOW);if(fd<0||read(fd,header,sizeof(header))!=(ssize_t)sizeof(header)){rc=R31_CP_UNSAFE;goto finish;}close(fd);fd=-1;
 if(memcmp(header,"SQLite format 3\000",16)||header[18]!=2||header[19]!=2){rc=R31_CP_UNSAFE;goto finish;}
 page=(header[16]<<8)|header[17];if(page==1)page=65536;if(page<512||page>65536||(page&(page-1))){rc=R31_CP_UNSAFE;goto finish;}
 /* Do not open a disconnected or legacy DB and implicitly reconstruct WAL
  * state. The already-open production owner must have its regular WAL+SHM. */
 if(lstat(wal,&st)){if(errno==ENOENT){rc=SQLITE_OK;goto finish;}rc=R31_CP_UNSAFE;goto finish;}
 if(!S_ISREG(st.st_mode)||st.st_nlink!=1){rc=R31_CP_UNSAFE;goto finish;}
 if(st.st_size==0&&mode!=2){rc=SQLITE_OK;goto finish;}
 if(lstat(shm,&st)||!S_ISREG(st.st_mode)||st.st_size<32768){rc=R31_CP_UNSAFE;goto finish;}
 if(!allowed(check,opaque,end)){rc=R31_CP_CANCEL;goto finish;}
 rc=sqlite3_open_v2(path,&db,SQLITE_OPEN_READWRITE|SQLITE_OPEN_NOMUTEX|SQLITE_OPEN_NOFOLLOW,0);if(rc!=SQLITE_OK)goto finish;
 sqlite3_busy_timeout(db,0);sqlite3_db_config(db,SQLITE_DBCONFIG_LOOKASIDE,0,0,0);r31_cp_control control={check,opaque,end};sqlite3_progress_handler(db,100,r31_cp_progress,&control);
 rc=sqlite3_exec(db,"PRAGMA schema_version",0,0,0);if(rc!=SQLITE_OK)goto finish;
 rc=sqlite3_wal_checkpoint_v2(db,"main",SQLITE_CHECKPOINT_NOOP,&log,&done);out[2]=log;out[3]=done;if(rc!=SQLITE_OK)goto finish;
 if(mode!=2&&(log<0||done<0))goto finish;
 out[5]=(int64_t)(log-done)*page;
 if(!allowed(check,opaque,end)){rc=R31_CP_CANCEL;goto finish;}
 if(mode==1){
  /* TRUNCATE must start with zero unbackfilled frames. The Java caller holds
   * the original writer and a nonblocking exclusive source-read lease. */
  if(log!=done){rc=R31_CP_DEFER;goto finish;}
 }else if(mode==0&&out[5]>R31_CP_MAX_COPY){rc=R31_CP_BUDGET;goto finish;}
 if(mode==2){
  int vacuum=-1;rc=sqlite3_prepare_v2(db,"PRAGMA auto_vacuum",-1,&statement,0);if(rc!=SQLITE_OK)goto finish;
  if(sqlite3_step(statement)==SQLITE_ROW)vacuum=sqlite3_column_int(statement,0);sqlite3_finalize(statement);statement=0;
  if(vacuum!=2){rc=R31_CP_DEFER;goto finish;}
  if(!allowed(check,opaque,end)){rc=R31_CP_CANCEL;goto finish;}
  out[7]=1;rc=sqlite3_exec(db,"PRAGMA incremental_vacuum(16)",0,0,0);
 }else{
  out[7]=1;rc=sqlite3_wal_checkpoint_v2(db,"main",mode==1?SQLITE_CHECKPOINT_TRUNCATE:SQLITE_CHECKPOINT_PASSIVE,&log,&done);out[2]=log;out[3]=done;
 }
finish:
 if(fd>=0)close(fd);if(statement)sqlite3_finalize(statement);if(db){sqlite3_progress_handler(db,0,0,0);int closed=sqlite3_close(db);if(rc==SQLITE_OK)rc=closed;}
 sqlite3_free(journal);sqlite3_free(wal);sqlite3_free(shm);
 out[4]=now_ms()-began;out[0]=rc;out[1]=(rc==SQLITE_BUSY||rc==SQLITE_LOCKED||rc==R31_CP_DEFER||rc==R31_CP_BUDGET)?1:0;
 return rc;
}
