"""Choose a reviewed repository CSV using the installed ChartsActivity and Android document picker."""
from pathlib import Path
import csv,hashlib,io,os,re,subprocess,time,xml.etree.ElementTree as ET

def import_history(package, query, record, save):
    def adb(*args):
        return subprocess.check_output(['adb',*args],timeout=60)
    assert adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    source=Path('app-source/research_data/ethpk10_draws_20260316_201059.csv')
    original=source.read_bytes();lines=original.decode('utf-8-sig').splitlines(keepends=True)
    rows=[(row[0],line) for line in lines[1:] for row in csv.reader([line]) if len(row)==8 and row[6]=='1']
    chosen=sorted(rows,key=lambda x:x[0])[-5000:]
    assert len({p for p,_ in chosen})==len(chosen)==5000
    fixture=Path(os.environ['RUNNER_TEMP'])/'starcore-review-5000.csv'
    fixture.write_text(lines[0]+''.join(line for _,line in chosen))
    record['input']={'role':'REPOSITORY_HISTORICAL_REPLAY_ONLY','sourceSha256':hashlib.sha256(original).hexdigest(),'selectedSha256':hashlib.sha256(fixture.read_bytes()).hexdigest(),'selectedRows':5000}
    save()
    location='/sdcard/Download/'+fixture.name
    adb('push',str(fixture),location)
    adb('shell','am','broadcast','-a','android.intent.action.MEDIA_SCANNER_SCAN_FILE','-d','file://'+location)
    def tap_text(choices,attempts=5):
        for _ in range(attempts):
            subprocess.run(['adb','shell','uiautomator','dump','/sdcard/r335-research-import.xml'],capture_output=True,timeout=45)
            try: hierarchy=ET.fromstring(adb('shell','cat','/sdcard/r335-research-import.xml'))
            except (ET.ParseError,subprocess.CalledProcessError):
                time.sleep(3);continue
            for node in hierarchy.iter('node'):
                label=' '.join(node.attrib.get(k,'') for k in ('text','content-desc','resource-id'))
                if not any(choice in label for choice in choices):continue
                coord=list(map(int,re.findall(r'\d+',node.attrib['bounds'])))
                if len(coord)==4 and coord[2]>coord[0] and coord[3]>coord[1]:
                    adb('shell','input','tap',str((coord[0]+coord[2])//2),str((coord[1]+coord[3])//2));time.sleep(1.2);return True
            time.sleep(2)
        return False
    # An empty Research schema records a genuine dependency until startup makes the official import owner ready.
    for _ in range(50):
        if query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r31_history_import_run','r31_history_import_row')")=='2':break
        time.sleep(6)
    activity=package+'/com.ethk3.novaonepro.ChartsActivityR83'
    adb('shell','am','start','-W','-n',activity)
    assert tap_text(['R27 工具']),'CHART_TOOLS_UNAVAILABLE'
    assert tap_text(['导入历史CSV（']),'IMPORT_ACTION_UNAVAILABLE'
    if not tap_text([fixture.name],2):
        assert tap_text(['Show roots','Open from','打开抽屉','显示根目录','Navigate up','drawer_toggle','Show navigation drawer']),'DOCUMENT_ROOTS_UNAVAILABLE'
        assert tap_text(['Downloads','下载内容','下载']),'DOWNLOADS_UNAVAILABLE'
        assert tap_text([fixture.name]),'FIXTURE_NOT_FOUND_IN_PICKER'
    record['documentPickerSelected']=True;save()
    for ordinal in range(90):
        try:
            row=int(query("SELECT count(*) FROM r31_history_import_row d JOIN r31_history_import_run r ON r.job_id=d.job_id WHERE r.status='COMPLETE' AND d.status='IMPORTED'"))
            record['publishedReplayRows']=row
            if row>=5000:
                save();return
        except (ValueError,subprocess.SubprocessError):pass
        if ordinal%6==0:save()
        time.sleep(5)
    raise RuntimeError('FULL_INPUT_PUBLICATION_NOT_OBSERVED')
