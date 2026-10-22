import sys,sqlite3,base64
D=sqlite3.connect(sys.argv[1] if len(sys.argv)>1 else ':memory:');D.isolation_level=None
D.execute("PRAGMA query_only=ON")
enc=lambda s:base64.b64encode(str(s).encode()).decode()
def val(v):
 if v=='N':return None
 if v[0]=='I':return int(v[1:])
 if v[0]=='F':return float(v[1:])
 if v[0]=='B':return base64.b64decode(v[1:])
 return base64.b64decode(v[1:]).decode()
for line in sys.stdin:
 try:
  parts=line.rstrip('\n').split('\t');sql=base64.b64decode(parts[1]).decode();args=[val(v) for v in parts[2:]]
  c=D.execute(sql,args);cols=[d[0] for d in c.description] if c.description else [];rows=c.fetchall() if cols else []
  print('\t'.join(['OK',str(len(rows)),str(len(cols)),*[enc(c) for c in cols]]))
  for row in rows:
   print('\t'.join('N' if v is None else ('B'+base64.b64encode(v).decode()) if isinstance(v,bytes) else 'I'+str(v) if isinstance(v,int) else 'F'+str(v) if isinstance(v,float) else 'S'+enc(v) for v in row))
  sys.stdout.flush()
 except Exception as e:print('ERROR\t'+enc(str(e)),flush=True)
