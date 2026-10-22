from pathlib import Path
import json,re,sys,xml.etree.ElementTree as ET
root=Path('.')
checks={}
def ck(k,v,detail=''):
    checks[k]={'pass':bool(v),'detail':detail}
java=root/'app/src/main/java/com/ethk3/novaonepro'
manifest=(java/'V49BuildManifest.java').read_text('utf-8')
config=(java/'AppConfig.java').read_text('utf-8')
auto=(java/'AutoFetchService.java').read_text('utf-8')
lane=(java/'LanePolicyCompiler.java').read_text('utf-8')
pipe=(java/'V49RealtimePipelineEngine.java').read_text('utf-8')
moe=(java/'MixtureOfExpertsGateEngine.java').read_text('utf-8')
feature=(java/'FeatureContractRegistry.java').read_text('utf-8')
main=(java/'MainActivity.java').read_text('utf-8')
layout=(root/'app/src/main/res/layout/activity_main.xml').read_text('utf-8')
ck('identity_v49','V49.0.49072201' in manifest and 'V49BuildManifest.VERSION_NAME' in config)
ck('db_version_32','DB_VERSION = 32' in config)
ck('realtime_100_to_10','V49_NUMBER_EVENT_COUNT = 100' in config and 'candidates.size() == 10' in pipe)
ck('top3_probability_sum','p1 + p2 + p3' in pipe and 'thirdMargin = p3 - p4' in pipe)
ck('single_probability_owner','Canonical probability owner fields' in pipe and 'MixtureOfExpertsGateEngine' in pipe)
ck('nine_experts','V49_EXPERT_COUNT = 9' in config and moe.count('experts.add(')>=9,str(moe.count('experts.add(')))
ck('no_champion_deadlock_v49','compileV49' in lane and 'MODEL_NOT_PROVEN' not in lane[lane.rfind('private static Result compileV49'):lane.find('private static LaneAttempt evaluateNormal')])
ck('full_information_learning','FullInformationLearningEngine' in auto and 'settleAndLearn' in auto)
ck('predraw_freeze','PreDrawHypothesisLedgerEngine' in pipe and 'V49ContractEngine' in pipe)
ck('feature_contract','smoothFrequency' in feature and 'contractChecksum' in feature)
ck('no_nested_scroll','@+id/scroll_history' in layout and '<ScrollView\n                android:id="@+id/scroll_history"' not in layout and '<ScrollView\n                android:id="@+id/scroll_draw_history"' not in layout)
ck('bounded_expand','Integer.MAX_VALUE' not in main)
ck('details_activities',(java/'DiagnosisActivity.java').exists() and (java/'HistoryActivity.java').exists())
ck('ui_snapshot',(java/'UiSnapshotBuilder.java').exists() and (java/'DashboardRenderer.java').exists())
# basic lexical balance ignoring comments/strings

def strip_java(s):
    out=[];i=0;state='n'
    while i<len(s):
        c=s[i];n=s[i+1] if i+1<len(s) else ''
        if state=='n':
            if c=='"': state='s';out.append(' ')
            elif c=="'": state='c';out.append(' ')
            elif c=='/' and n=='/': state='l';out.extend('  ');i+=1
            elif c=='/' and n=='*': state='b';out.extend('  ');i+=1
            else: out.append(c)
        elif state=='s':
            if c=='\\': out.extend('  ');i+=1
            elif c=='"': state='n';out.append(' ')
            else: out.append('\n' if c=='\n' else ' ')
        elif state=='c':
            if c=='\\': out.extend('  ');i+=1
            elif c=="'": state='n';out.append(' ')
            else: out.append(' ')
        elif state=='l':
            if c=='\n': state='n';out.append('\n')
            else: out.append(' ')
        else:
            if c=='*' and n=='/': state='n';out.extend('  ');i+=1
            else: out.append('\n' if c=='\n' else ' ')
        i+=1
    return ''.join(out),state
bad=[]
for p in java.glob('*.java'):
    st,state=strip_java(p.read_text('utf-8'))
    if state not in ('n','l') or st.count('{')!=st.count('}') or st.count('(')!=st.count(')') or st.count('[')!=st.count(']'):
        bad.append((p.name,state,st.count('{'),st.count('}'),st.count('('),st.count(')')))
ck('java_lexical_balance',not bad,str(bad[:10]))
xmlbad=[]
for p in (root/'app/src/main/res/layout').glob('*.xml'):
    try: ET.parse(str(p))
    except Exception as e: xmlbad.append((p.name,str(e)))
try: ET.parse(str(root/'app/src/main/AndroidManifest.xml'))
except Exception as e: xmlbad.append(('AndroidManifest.xml',str(e)))
ck('xml_parse',not xmlbad,str(xmlbad))
report={'version':'V49.0.49072201','checks':checks,'passed':sum(x['pass'] for x in checks.values()),'total':len(checks)}
Path('reports').mkdir(exist_ok=True)
Path('reports/V49_STATIC_SELFCHECK_49072201.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),'utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
sys.exit(0 if report['passed']==report['total'] else 1)
