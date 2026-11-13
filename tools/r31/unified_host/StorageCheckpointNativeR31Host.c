#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#define CP4_HOST 1
#define main cp4_original_host_main
#include "../../../app/src/main/cpp/report_native_cp4.c"
#undef main
static int permit(void *value){return *(int*)value;}
int main(int argc,char **argv){if(argc!=4)return 2;int64_t out[8];int allowed=atoi(argv[3]);int rc=r31_storage_checkpoint(argv[1],atoi(argv[2]),40,permit,&allowed,out);for(int i=0;i<8;i++)printf("%s%lld",i?",":"",(long long)out[i]);puts("");return rc==out[0]?0:3;}
