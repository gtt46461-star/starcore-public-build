#!/usr/bin/env python3
"""Build/check actual JVM method + direct-invocation inventory. No runtime coverage claim.
Includes each product Java source, nested/synthetic compiled methods and direct calls.
Virtual target dispatch, reflection/native calls and external libraries are not resolved.
"""
import argparse,struct,hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/research/r975_source_calls.tsv'
class Reader:
 def __init__(self,b):self.b=b;self.p=0
 def n(self,k):v=self.b[self.p:self.p+k];self.p+=k;return v
 def u1(self):return self.n(1)[0]
 def u2(self):return int.from_bytes(self.n(2),'big')
 def u4(self):return int.from_bytes(self.n(4),'big')
def attrs(r,cp):return [(cp[r.u2()],r.n(r.u4())) for _ in range(r.u2())]
def methods(path):
 r=Reader(path.read_bytes());assert r.u4()==0xcafebabe;r.n(4);n=r.u2();cp=[None]*n;i=1
 while i<n:
  t=r.u1()
  if t==1:cp[i]=r.n(r.u2()).decode('utf-8',errors='replace')
  elif t in (3,4):cp[i]=(t,r.n(4))
  elif t in (5,6):cp[i]=(t,r.n(8));i+=1
  elif t in (7,8,16,19,20):cp[i]=(t,r.u2())
  elif t in (9,10,11,12,17,18):cp[i]=(t,r.u2(),r.u2())
  elif t==15:cp[i]=(t,r.u1(),r.u2())
  else:raise ValueError(('constant tag',t))
  i+=1
 r.u2();name=cp[cp[r.u2()][1]];r.u2();r.n(r.u2()*2)
 for _ in range(r.u2()):r.n(6);attrs(r,cp)
 ms=[]
 for _ in range(r.u2()):
  flags=r.u2();nm=cp[r.u2()];desc=cp[r.u2()];aa=attrs(r,cp);calls=[];lines=[]
  for key,bb in aa:
   if key!='Code':continue
   cr=Reader(bb);cr.n(4);code=cr.n(cr.u4());cr.n(cr.u2()*8)
   for an,ab in attrs(cr,cp):
    if an=='LineNumberTable':lr=Reader(ab);lines=[(lr.u2(),lr.u2()) for _ in range(lr.u2())]
   p=0
   while p<len(code):
    op=code[p];size=1
    if op in [0xb6,0xb7,0xb8,0xb9,0xba]:
     ref=cp[int.from_bytes(code[p+1:p+3],'big')];nt=cp[ref[2]]
     owner=cp[cp[ref[1]][1]] if ref[0] in (10,11) else '<invokedynamic:bootstrap-unresolved>'
     line=max((x for x in lines if x[0]<=p),default=(0,0))[1]
     calls.append(f'{line}@{p}:{owner}.{cp[nt[1]]}{cp[nt[2]]}')
    if op in [0x10,0x12,*range(0x15,0x1a),*range(0x36,0x3b),0xa9,0xbc]:size=2
    elif op in [0x11,0x13,0x14,0x84,*range(0x99,0xa9),*range(0xb2,0xb9),0xbb,0xbd,0xc0,0xc1,0xc6,0xc7]:size=3
    elif op==0xc5:size=4
    elif op in (0xb9,0xba,0xc8,0xc9):size=5
    elif op==0xc4:size=6 if code[p+1]==0x84 else 4
    elif op in (0xaa,0xab):
     q=(p+4)&~3
     if op==0xaa:
      low,high=struct.unpack('>ii',code[q+4:q+12]);size=q-p+12+4*(high-low+1)
     else:
      pairs=struct.unpack('>i',code[q+4:q+8])[0];size=q-p+8+8*pairs
    if p+size>len(code):raise ValueError(('bad bytecode length',name,nm,p,size))
    p+=size
  ms.append((nm,desc,flags,min([l for _,l in lines],default=0),max([l for _,l in lines],default=0),calls))
 source=None
 for an,bb in attrs(r,cp):
  if an=='SourceFile':source=cp[int.from_bytes(bb,'big')]
 assert r.p==len(r.b)
 return name,source,ms

def main():
 p=argparse.ArgumentParser();p.add_argument('--classes',required=True,type=Path);p.add_argument('--check',action='store_true');a=p.parse_args()
 src=ROOT/'app/src/main/java';records={str(f.relative_to(ROOT)):[] for f in sorted(src.rglob('*.java'))};classes=0;methodn=0;calln=0
 for f in sorted(a.classes.rglob('*.class')):
  name,source,ms=methods(f)
  if not source:continue
  key='app/src/main/java/'+name.rsplit('/',1)[0]+'/'+source
  if key not in records:continue
  classes+=1
  for nm,desc,flags,lo,hi,calls in ms:
   methodn+=1;calln+=len(calls);records[key].append((name,nm,desc,str(lo),str(hi),'SYNTHETIC' if flags&0x1000 else 'DECLARED', ';'.join(calls)))
 lines=['# STATIC_COMPILED_METHOD_CALLS_V1|NOT_RUNTIME_COVERAGE|virtual/reflection/native-dispatch=UNRESOLVED',
        '# SOURCE_SHA refers to each actual source file. Calls are bytecode instruction targets, not guessed regex edges.',
        'record\tfile\tsource_sha256\tclass\tmethod\tdescriptor\tfirst_executable_line\tlast_executable_line\tmethod_kind\tdirect_calls\truntime_status']
 for key,rows in records.items():
  h=hashlib.sha256((ROOT/key).read_bytes()).hexdigest();lines.append('\t'.join(['FILE',key,h,'','','','','','','','RUNTIME_NOT_INSTRUMENTED_UNLESS_STAGE_RECEIPT_PRESENT']))
  for row in sorted(rows):lines.append('\t'.join(['METHOD',key,h,*row,'RUNTIME_NOT_INSTRUMENTED_UNLESS_STAGE_RECEIPT_PRESENT']))
 text='\n'.join(lines)+'\n'
 if a.check:
  if not OUT.is_file() or OUT.read_text()!=text:raise SystemExit('SOURCE_CALL_INDEX_STALE')
 else:OUT.parent.mkdir(parents=True,exist_ok=True);OUT.write_text(text)
 print(json.dumps(dict(files=len(records),classes=classes,methods=methodn,direct_invocations=calln,sha256=hashlib.sha256(text.encode()).hexdigest(),runtime_coverage='NOT_CLAIMED',check=a.check)))
if __name__=='__main__':main()
