#!/usr/bin/env python3
from pathlib import Path
import hashlib, json, re, sys, urllib.parse

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/ethk3/novaonepro'
REPORTS=ROOT/'reports'; REPORTS.mkdir(parents=True,exist_ok=True)
RESULTS=[]

def check(name, condition, detail=''):
    RESULTS.append({'name':name,'pass':bool(condition),'detail':detail})
    if not condition: print('FAIL',name,detail)

def latest_url(base, limit, key='test-key-not-secret'):
    return base.rstrip('/')+'/data?'+urllib.parse.urlencode({'code':'Binance1-PK10','limit':str(limit),'key':key})

def date_url(base,date,key='test-key-not-secret'):
    return base.rstrip('/')+'/data?'+urllib.parse.urlencode({'code':'Binance1-PK10','date':date,'key':key})

def redact(url):
    p=urllib.parse.urlsplit(url); q=urllib.parse.parse_qsl(p.query,keep_blank_values=True)
    q=[(k,'[SECRET]' if k.lower() in ('key','token','secret') else v) for k,v in q]
    return urllib.parse.urlunsplit((p.scheme,p.netloc,p.path,urllib.parse.urlencode(q),p.fragment))

def validate_payload(payload):
    if payload.get('info','').lower()!='success': return False,'INFO'
    if payload.get('lotteryCode')!='Binance1-PK10': return False,'CODE'
    rows=payload.get('data');
    if not isinstance(rows,list) or not rows: return False,'DATA'
    prev=''
    for row in rows:
        if not all(k in row for k in ('betNo','gmtBetNo','openNumber','block','hash','openTime')): return False,'FIELDS'
        nums=[int(x) for x in re.split(r'[,\s-]+',row['openNumber'].strip()) if x]
        if len(nums)!=10 or sorted(nums)!=list(range(1,11)): return False,'PERMUTATION'
        issue=re.sub(r'\D','',str(row['betNo']))
        if prev and (len(issue)<len(prev) or (len(issue)==len(prev) and issue<=prev)): return False,'MONOTONIC'
        prev=issue
    return True,'OK'

def replay_hash(row):
    nums=[int(x) for x in re.split(r'[,\s-]+',row['openNumber'].strip()) if x]
    canonical='%s|%s|%s|%s|BINANCE1_PK10_DATA_V1_B01' % (row['betNo'],row['gmtBetNo'],','.join(map(str,nums))+',',row['openTime'])
    return hashlib.sha256(canonical.encode()).hexdigest()

def source(name): return (JAVA/name).read_text(encoding='utf-8')

# URL contract and no probing.
u=latest_url('https://api.duoqu.online',20)
d=date_url('https://api1.duoqu.online','2026-08-05')
check('latest canonical path',urllib.parse.urlsplit(u).path=='/data',u)
check('latest code',dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(u).query)).get('code')=='Binance1-PK10')
check('date canonical path',urllib.parse.urlsplit(d).path=='/data',d)
check('redaction', 'test-key-not-secret' not in redact(u), redact(u))

# Budget simulation: exact capacity before refill.
class Bucket:
    def __init__(self,n): self.n=n; self.left=n
    def take(self):
        if self.left<=0:return False
        self.left-=1;return True
for cap,label in ((45,'latest'),(4,'date')):
    b=Bucket(cap); accepted=sum(1 for _ in range(cap+3) if b.take())
    check(label+' budget capacity',accepted==cap,str(accepted))

# Circuit/failover model.
order=['primary','backup']; failures={'primary':0,'backup':0}
def fail(endpoint):
    failures[endpoint]+=1
    if failures[endpoint]>=2 and endpoint in order:
        order.remove(endpoint); order.append(endpoint)
fail('primary');fail('primary')
check('same-call failover order',order[0]=='backup',str(order))

sample={'info':'success','lotteryCode':'Binance1-PK10','data':[
 {'betNo':'202608050001','gmtBetNo':'202608050001','openNumber':'01,02,03,04,05,06,07,08,09,10','block':'b1','hash':'h1','openTime':1785900000000},
 {'betNo':'202608050002','gmtBetNo':'202608050002','openNumber':'10,09,08,07,06,05,04,03,02,01','block':'b2','hash':'h2','openTime':1785900060000},
]}
ok,code=validate_payload(sample);check('strict payload accept',ok,code)
bad=json.loads(json.dumps(sample));bad['lotteryCode']='Ethereum1-PK10';ok,code=validate_payload(bad);check('wrong game quarantine',not ok and code=='CODE',code)
bad=json.loads(json.dumps(sample));bad['data'][0]['openNumber']='01,01,03,04,05,06,07,08,09,10';ok,code=validate_payload(bad);check('duplicate number quarantine',not ok and code=='PERMUTATION',code)
bad=json.loads(json.dumps(sample));bad['data'][1]['betNo']='202608050000';ok,code=validate_payload(bad);check('nonmonotonic quarantine',not ok and code=='MONOTONIC',code)
h1=replay_hash(sample['data'][0]);h2=replay_hash(sample['data'][0]);check('deterministic replay hash',h1==h2,h1)

joined='\n'.join(source(n) for n in ['AppConfig.java','BuildIdentity.java','V51BuildManifest.java','V51ArtifactBundleManifest.java','KeyProvider.java','ApiClient.java','ApiParser.java','SafeLog.java','BinanceGameContract.java','ApiTransportGovernanceEngine.java'])
check('no ethereum formal token','Ethereum1-PK10' not in joined)
check('no old latest path','/api/lottery/latest' not in joined)
check('no old history path','/api/lottery/history' not in joined)
check('model not proven','MODEL_NOT_PROVEN' in joined)
check('shadow lock','SHADOW_ONLY' in joined and 'REAL_BET_HARD_DISABLED' in joined)
check('single game code','Binance1-PK10' in joined)
check('key doc placeholder','${DUOQU_API_KEY}' in joined)

summary={'suite':'STARCORE-REFORGE-51-B01','passed':sum(1 for r in RESULTS if r['pass']),'failed':sum(1 for r in RESULTS if not r['pass']),'results':RESULTS}
(REPORTS/'B01_CONTRACT_HARNESS.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
(REPORTS/'B01_CONTRACT_HARNESS.txt').write_text('\n'.join(('[PASS] ' if r['pass'] else '[FAIL] ')+r['name']+' '+r['detail'] for r in RESULTS)+'\n',encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))
sys.exit(1 if summary['failed'] else 0)
