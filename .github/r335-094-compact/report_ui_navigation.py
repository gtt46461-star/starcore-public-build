"""Navigate the report menu using the installed app's visible controls on the QA guest."""
import re
import time

def install(n, record, package):
    base = n.ui.hierarchy
    def tap_node(node):
        coordinates = list(map(int, re.findall(r'\d+', node.get('bounds',''))))
        if len(coordinates) != 4:return False
        x,y,right,bottom = coordinates
        if x>=right or y>=bottom:return False
        n.ui.adb('shell','input','tap',str((x+right)//2),str((y+bottom)//2),timeout=40)
        return True
    def hierarchy():
        for _ in range(8):
            root=base();nodes=list(root.iter('node'))
            action=next((node for node in nodes if node.get('resource-id')=='android:id/aerr_wait' and node.get('text') in ('等待','Wait')),None)
            if any(node.get('text')=='修改数据接口密钥（可选）' and node.get('package')==package for node in nodes):
                action=next((node for node in nodes if node.get('resource-id')=='android:id/button2' and node.get('text')=='取消'),action)
            if action is None:return root
            tap_node(action);record['dismissedBlockingGuestDialogs']=1+record.get('dismissedBlockingGuestDialogs',0)
            time.sleep(1)
        return base()
    n.ui.hierarchy=hierarchy
    def menu(label):
        launcher='导出完整报告（含数据库和训练附件）'
        retrieve='取回上次报告或继续生成'
        width,height=map(int,re.findall(r'(\d+)x(\d+)',n.ui.adb('shell','wm','size'))[-1])
        start=time.monotonic();direction=1;last=None;static=0;empty=0;swipes=0
        for i in range(90):
            if time.monotonic()-start>480:break
            tree=hierarchy()
            nodes=[node for node in tree.iter('node') if node.get('enabled')=='true' and node.get('package')==package]
            record['appNodesSeen']=max(record.get('appNodesSeen',0),len(nodes))
            if any(node.get('text')==label for node in nodes):return
            controls=[node for node in nodes if node.get('text') in (retrieve,launcher)]
            if controls:
                if tap_node(controls[0]):
                    record['actualReportMenuTaps']=1+record.get('actualReportMenuTaps',0)
                    time.sleep(1)
                    continue
            if not nodes:
                empty+=1
                if empty in (3,12):
                    n.ui.adb('shell','am','start','-W','-n',n.field.ACTIVITY,timeout=90)
                time.sleep(1)
                continue
            view=tuple((node.get('resource-id'),node.get('class'),node.get('bounds')) for node in nodes if node.get('text'))
            static=static+1 if view==last else 0;last=view
            if static>=2 or swipes>=20:
                direction=-direction;static=0;swipes=0
            swipes+=1
            record['reportMenuSwipes']=1+record.get('reportMenuSwipes',0)
            y1,y2=(height*8//10,height*3//10) if direction>0 else (height*3//10,height*8//10)
            n.ui.adb('shell','input','swipe',str(width//2),str(y1),str(width//2),str(y2),'310',timeout=40)
        raise RuntimeError('VISIBLE_REPORT_CONTROL_UNAVAILABLE')
    n.report_dialog=menu
