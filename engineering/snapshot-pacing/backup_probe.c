#define CP4_HOST
#define main cp4_auxiliary_cli
#include "report_native_cp4.c"
#undef main

struct probe {
 int checks, admissions, chunks;
 int cancel_after, pin_handshake;
};

static int check(void *data) {
 struct probe *p=data;
 ++p->checks;
 return !p->cancel_after || p->chunks<p->cancel_after;
}

static int admit(void *data,int64_t bytes) {
 struct probe *p=data;
 ++p->admissions;
 return bytes>0;
}

static void progress(void *data,int total,int remaining,int rc) {
 struct probe *p=data;
 if (rc!=SQLITE_OK && rc!=SQLITE_DONE) return;
 if (++p->chunks==1 && p->pin_handshake) {
  if(total<=remaining) exit(2);
  puts("PINNED");
  fflush(stdout);
  if(getchar()!='\n') exit(2);
 }
}

int main(int argc,char **argv) {
 if(argc!=4)return 2;
 struct probe p={0};
 p.cancel_after=strcmp(argv[1],"cancel")==0?20:0;
 p.pin_handshake=strcmp(argv[1],"wal")==0;
 int rc=cp4_backup_impl(argv[2],argv[3],4*1024*1024,120000,check,&p,progress,admit);
 printf("BACKUP_RESULT %d %d %d %d\n",rc,p.checks,p.admissions,p.chunks);
 if(p.checks<p.chunks || p.admissions<p.chunks+1) return 1;
 return rc==(p.cancel_after?CP4_CANCEL:SQLITE_OK)?0:1;
}
