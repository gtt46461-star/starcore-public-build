#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>
int cp4_backup(const char *,const char *,int64_t,int64_t,int (*)(void *),void *);
static int64_t ms(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return (int64_t)t.tv_sec*1000+t.tv_nsec/1000000;}
static int keep(void *p){return ms()<*(int64_t*)p;}
int main(int argc,char **argv){if(argc!=4)return 2;int64_t started=ms(),end=started+atoll(argv[3]);int rc=cp4_backup(argv[1],argv[2],32*1024*1024,0,keep,&end);printf("{\"nativeCode\":%d,\"elapsedMs\":%lld}\n",rc,(long long)(ms()-started));return 0;}
