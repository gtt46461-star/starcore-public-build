#!/usr/bin/env python3
"""Generate the R31.2 sequential-batch ledgers and batch documents.

Single source for R31.2 batch layout and acceptance-level classification.
Original 56 cards and their acceptance text are never edited; this script only
adds R31.2 placement fields and derived ledgers. Re-running is idempotent.
"""
from pathlib import Path
import hashlib
import json

P = Path(__file__).resolve().parents[1]
VERSION = 'R31.2-SEQUENTIAL-20261105'
RESUME_COMMIT = '2c5eca70641aa2ca4f9d9f0c9ff23623e48e0bac'
SOURCE_BRANCH = 'construction/r31-final-20261104'

# ---------------------------------------------------------------- levels
LEVELS = {
    'LOCAL': '本地/CI确定性执行：JVM单元、HOST协议、静态检查、编译、lint。批次关门必过。',
    'EMU': 'Android模拟器或Android SQLite仪器测试。只在S20集中执行；其余批次不阻塞、不得记PASS。',
    'DEVICE': '真机/时长/实测时延/截图（V2353A、24h、72h、设备P95）。施工期一律DEFERRED，不阻塞，不得记PASS。',
    'NATURAL': '自然未来质量。永远不是施工门，只能NOT_PROVEN或另行独立证明。',
}
BLOCKING = {'LOCAL'}

# Item classification. Default: whole item LOCAL.
# Split items: list of (suffix, level, text). The original text stays in cards.json untouched.
SPLIT = {
    'R31-B01-C03#A3': [('a', 'LOCAL', '虚拟时钟连续推进24h调度：pending>0且依赖满足的任务均有提交进展或精确阻塞证据，无永久无声停摆。'),
                       ('b', 'DEVICE', '真机连续24小时无永久无声停摆。')],
    'R31-B01-C04#A1': [('a', 'LOCAL', '永不poll任务：无第二个同任务实例；P0继续；stop请求在10秒(虚拟时钟)内返回STOP_INCOMPLETE而非已停止。'),
                       ('b', 'EMU', 'UI上停止按钮可用且显示“停止未完成”。')],
    'R31-B01-C04#A3': [('a', 'LOCAL', '压力循环(≥10000次领取/取消)中线程总数、待执行队列、prepared对象数有上限；取消后计数归零。'),
                       ('b', 'DEVICE', '设备长时间运行下线程/队列/prepared上限。')],
    'R31-B02-C01#A2': [('a', 'LOCAL', '虚拟时钟压缩60分钟竞争：1Hz轮询+历史回补+研究+报告+保留，关键写无饿死，研究pending有界。'),
                       ('b', 'DEVICE', '真机实时60分钟竞争测试。')],
    'R31-B02-C02#A3': [('a', 'EMU', 'Android实际SQLite：受保护触发器、WAL、checkpoint并发。'),
                       ('b', 'DEVICE', '设备事务时延（host sqlite结果不能代替）。')],
    'R31-B03-C01#A2': [('a', 'LOCAL', '模拟1000次抓取测量输出全分布、分母与最差20次原因；不只保留最近96次。'),
                       ('b', 'DEVICE', '真机真实网络1000次测量分布。')],
    'R31-B03-C02#A2': [('a', 'LOCAL', '100%已知实时机会有终端状态（模拟时序全覆盖）。'),
                       ('b', 'DEVICE', '设备/网络/正常资源条件下可预测覆盖≥99%。')],
    'R31-B04-C02#A2': [('a', 'LOCAL', '协调器层：用户stop后不再提交新抓取/研究且不自行复活；普通前后台切换保持会话状态。'),
                       ('b', 'DEVICE', '真机后台切换与stop行为。')],
    'R31-B05-C02#A2': [('a', 'LOCAL', '模拟24h增长负载：新建产物数量/bytes有上界，保护对象无误删。'),
                       ('b', 'DEVICE', '真机24h产物数量/bytes上界。')],
    'R31-B05-C11#A3': [('a', 'LOCAL', '100轮会话打开/取消后，旧session/epoch对象经弱引用+GC断言可回收（JVM）。'),
                       ('b', 'DEVICE', '设备heap dominator无Activity/旧session滞留。')],
    'R31-B05-C11#A5': [('a', 'LOCAL', 'DB reader/游标/文件句柄在正常与取消路径后计数回到基线（HOST计数）。'),
                       ('b', 'DEVICE', 'native/FD实测回到基线包络。')],
    'R31-B05-C07#A3': [('a', 'LOCAL', '模拟24h事件写入：日志总bytes与保留条数有上界。'),
                       ('b', 'DEVICE', '真机24h日志上界。')],
    'R31-B05-C09#A2': [('a', 'LOCAL', '长reader+GC压力模拟下冻结/结算路径不被阻塞（虚拟时钟预算断言）。'),
                       ('b', 'DEVICE', '真机压力下实时冻结/结算延迟门。')],
    'R31-B06-C02#A3': [('a', 'LOCAL', '性能收据缺失或未达门时家族预算不得提高（门控逻辑测试）；吞吐/峰值/延迟记录字段齐全。'),
                       ('b', 'DEVICE', '真机CPU吞吐、峰值内存、实时延迟达到性能门。')],
    'R31-B06-C02#A4': [('a', 'LOCAL', '退休候选释放后缓存引用与磁盘引用下降；模型cache命中率与字节记账可核对。'),
                       ('b', 'DEVICE', '设备堆/native实测下降。')],
    'R31-B07-C03#A3': [('a', 'LOCAL', '置入高成本低增益候选后总预算不突破，模拟实时SLO预算不劣化，研究逐步继续。'),
                       ('b', 'DEVICE', '真机实时SLO不劣化。')],
    'R31-B09-C02#A1': [('a', 'LOCAL', '面板加载器隔离：一个面板SQL阻塞/格式错误，其余面板在预算内产出数据帧（JVM）。'),
                       ('b', 'EMU', 'UI中其余面板实际可见。')],
    'R31-B09-C02#A2': [('a', 'LOCAL', '加载epoch守卫：100次切换/取消无旧epoch覆盖，连接/线程计数回到基线（JVM）。'),
                       ('b', 'EMU', '真实Activity切tab/后台/旋转100次无BadToken与泄漏。')],
    'R31-B09-C05#A1': [('a', 'LOCAL', '47个ID各有有数据fixture及空/错误状态帧；16视图分页查询通过。'),
                       ('b', 'EMU', '47图渲染图证。')],
    'R31-B12-C01#A1': [('a', 'LOCAL', 'HOST迁移每个切点杀进程再打开：不丢开奖/模型/证书根，不重复迁移。'),
                       ('b', 'EMU', 'Android覆盖安装后各切点杀进程恢复。')],
    'R31-B12-C02#A3': [('a', 'LOCAL', 'HOST全链故障注入：无重复owner提交、重复结算、越界预测、受保护资产删除。'),
                       ('b', 'EMU', 'Android全链故障注入矩阵（API29/35）。')],
    'R31-B12-C05#A1': [('a', 'LOCAL', 'ZIP完整性、成员摘要、APK签名指纹与发布收据一致。'),
                       ('b', 'DEVICE', '设备安装身份与收据一致。')],
}
WHOLE = {
    'R31-B04-C02#A1': 'DEVICE', 'R31-B04-C02#A3': 'DEVICE',
    'R31-B05-C11#A6': 'DEVICE',
    'R31-B07-C06#A5': 'NATURAL',
    'R31-B09-C02#A3': 'DEVICE',
    'R31-B09-C05#A2': 'DEVICE',
    'R31-B12-C01#A2': 'EMU',
    'R31-B12-C03#A1': 'DEVICE', 'R31-B12-C03#A2': 'DEVICE', 'R31-B12-C03#A3': 'DEVICE',
}

# ---------------------------------------------------------------- engineering cards
ENG = [
    {'id': 'R312-S00-C01', 'batch': 'S00', 'title': '安装施工闸门工具、HOST注册表与CI兜底构建',
     'issue': 'R31.1无统一关门命令：每次会话手工挑选测试、手工写账本，导致同一阶段反复重审。',
     'files': ['construction/r31/r312/HOST_SUITE.json', '.github/workflows/r312-gate.yml'],
     'steps': ['从方案包reference_code/r312复制HOST_SUITE模板与r312-gate.yml到施工分支对应路径。',
               '逐个运行tools/r31下现有HOST/verify脚本，把每个能运行的命令写入HOST_SUITE.json（含所需参数、owner卡）；需参数的补齐，确实已废弃的写retired及替代测试。',
               '在HOST_SUITE.json的pre_build中写入开发构建所需的身份刷新命令（若构建需要），保证gate build一条命令即可完成。',
               '运行r312_gate.py status与build --batch S00，确认能产出BUILD_SUMMARY.json。'],
     'acceptance': ['HOST_SUITE.json无command为空的条目，check-plan与status通过。',
                    'gate build在当前HEAD实际执行gradle与全部HOST条目，BUILD_SUMMARY逐项记录结果（失败如实保留）。',
                    '本地无Android SDK时，r312-gate.yml在施工分支推送后能产出同格式BUILD_SUMMARY工件。'],
     'origin': 'R31.2新增工程卡'},
    {'id': 'R312-S00-C02', 'batch': 'S00', 'title': '在制品盘点与唯一候选源码',
     'issue': '21卡已开工、7个批次被触及，进度只记“IMPLEMENTING/BUILD_PASS”，看不出哪些代码属于哪张卡、哪些已在树内。',
     'files': ['construction/r31/r312/WIP_INVENTORY.json'],
     'steps': ['确认施工分支HEAD包含全部已推送工作（集成分支/本地未推送提交已并入或明确放弃并记录）。',
               '以SOURCE_CHANGE_OWNERS.json定点查询，生成WIP_INVENTORY.json：每张已开工卡→已改文件、对应提交、当时状态；不复制大JSON全文。',
               '把S01需要修复的全量失败清单（测试类#方法、错误信息）写入WIP_INVENTORY.failures。'],
     'acceptance': ['WIP_INVENTORY覆盖EXECUTION_LEDGER中全部非NOT_STARTED卡，文件路径均存在于HEAD。',
                    '唯一候选：resume提交是HEAD祖先，且无其它分支需要回并的应用源码。',
                    '全量失败清单与S00的BUILD_SUMMARY逐条一致。'],
     'origin': 'R31.2新增工程卡'},
    {'id': 'R312-S01-C01', 'batch': 'S01', 'title': '修复启动Certificate关键域指纹失配',
     'issue': 'CI015 API29/35各10项协议8项失败于SCHEMA_PENDING:R31_CERTIFICATE_REVOCATION:UNTRUSTED_CRITICAL:FINGERPRINT_MISMATCH；共享启动增量认证改动(493080ad)后Certificate关键域指纹生产者与校验者不一致。原属B02-C02在制品回归。',
     'files': ['app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java',
               'app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java',
               'tools/r31/certificate_startup_host/'],
     'steps': ['用certificate_startup_host在HOST复现：全新库启动、R30旧库升级两条路径，得到同一失败码。',
               '对比指纹计算输入（规范化DDL文本、版本号、登记顺序）在生产者登记与启动校验两侧的差异，定位唯一真值来源。',
               '修正为单一来源计算；旧库合法指纹按迁移规则重新认证，篡改仍拒绝；不得增加信任旁路或改断言。',
               '补三条回归：新库READY、R30旧库升级READY且撤销记录不变、篡改关键DDL被FINGERPRINT_MISMATCH拒绝。'],
     'acceptance': ['HOST新库启动Certificate域READY。',
                    'R30旧库升级后READY，既有撤销记录逐行不变。',
                    '篡改关键DDL必然FINGERPRINT_MISMATCH；源码中不存在无条件信任新指纹的路径（静态检查）。'],
     'origin': 'R31-B02-C02在制品回归'},
    {'id': 'R312-S01-C02', 'batch': 'S01', 'title': '内存准入在制品与原有测试相容（不加堆、不改阈值）',
     'issue': 'MemoryBudgetR31（B05-C11在制品）合入后，11项既有研究测试在原512MiB下报MEMORY_DECLARED_CAPACITY/MEMORY_HEAP_PRESSURE。',
     'files': ['app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java',
               'app/src/main/java/com/ethk3/novaonepro/strategyv2/BoundedResearchExecutorR31.java',
               'app/src/main/java/com/ethk3/novaonepro/strategyv2/SharedFeatureStoreR31.java',
               'app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java'],
     'steps': ['对每个失败类在原JVM限制下单独运行与全量运行各一次：单独通过而全量失败=前序泄漏；单独也失败=声明容量或真实压力。',
               '泄漏：修生产代码的lease close/finally与owner生命周期、有界缓存的回收；提供生产API用于测试间复位，不在测试里清静态字段掩盖。',
               '声明容量估算偏大：按实测上界修正估算函数并留测量依据；真实压力：修数据结构共享（同输入共享实例）。',
               '加泄漏守卫：每个测试结束时outstanding lease=0、声明容量回到0。'],
     'acceptance': ['原512MiB限制、原阈值下，原11项失败测试全部通过。',
                    '每个测试结束outstanding lease=0（全量运行断言）。',
                    '超额请求仍被拒绝、异常路径释放、重复close幂等（ST-M01子集仍成立）。'],
     'origin': 'R31-B05-C11在制品回归'},
    {'id': 'R312-S01-C03', 'batch': 'S01', 'title': '全量基线绿化：单元/HOST/lint/编译同一HEAD全过',
     'issue': '最新全量485项13失败、CI015为8失败、HOST脚本状态分散；任何后续批次都无法证明“未回归”。',
     'files': ['app/src/test/', 'tools/r31/', 'construction/r31/r312/HOST_SUITE.json'],
     'steps': ['以S00的失败清单为唯一输入，逐条修复剩余失败（含H15 StackOverflow类），每条记录根因一句话。',
               '修复过程中只改与失败直接相关的文件；涉及其它在制品卡的文件按scope_extension声明。',
               '同一HEAD运行gate build：单元、HOST注册表、lintDebug、androidTest编译全部通过。'],
     'acceptance': ['gate build：单元失败=0、错误=0、跳过数不增加。',
                    'HOST_SUITE全部条目PASS。',
                    'lintDebug与compileDebugAndroidTestJavaWithJavac通过。'],
     'origin': 'R31.2新增工程卡'},
]

# ---------------------------------------------------------------- batches
BATCHES = [
    ('S00', '施工通道与在制品盘点', ['R31-B00-C01', 'R31-B00-C02', 'R312-S00-C01', 'R312-S00-C02'],
     '建立唯一关门命令和唯一候选源码；B00两卡已有源码合同，本批绑定其测试。全量测试允许失败但必须逐条记录，作为S01输入。', False),
    ('S01', '基线绿化（修复已合入在制品造成的全量失败）', ['R312-S01-C01', 'R312-S01-C02', 'R312-S01-C03'],
     '从此批起，每个批次关门时全量单元/HOST/lint必须为0失败；这是不再兜圈子的前提。', True),
    ('S02', '任务故障分类与owner/fence', ['R31-B01-C01', 'R31-B01-C02'], '', True),
    ('S03', '有限隔离恢复与可中断关闭', ['R31-B01-C03', 'R31-B01-C04'], '', True),
    ('S04', '写优先级、短事务与提交结果（含B01/B02联合协议）', ['R31-B02-C01', 'R31-B02-C02', 'R31-B02-C03'],
     '关门额外要求：B01与B02生产者/消费者HOST联合协议条目全部PASS。', True),
    ('S05', '抓取、唯一冻结与结算解耦', ['R31-B03-C01', 'R31-B03-C02', 'R31-B03-C03'], '', True),
    ('S06', '研究健康与后台能力', ['R31-B04-C01', 'R31-B04-C02'], 'B04-C02大部分为真机项，本批只关本地可验部分。', True),
    ('S07', '存储普查、写前准入与报告清理', ['R31-B05-C01', 'R31-B05-C02', 'R31-B05-C03'], '', True),
    ('S08', '内存与缓存队列有界治理', ['R31-B05-C11'], 'S01只修了相容性；本批完成B05-C11其余本地验收。', True),
    ('S09', '历史回放与模型状态共同提交', ['R31-B06-C01', 'R31-B06-C02', 'R31-B06-C03'], '', True),
    ('S10', '家族保全、typed AST与创造预算', ['R31-B07-C01', 'R31-B07-C02', 'R31-B07-C03'], '', True),
    ('S11', '父子比较、工厂检出力与能力升级', ['R31-B07-C04', 'R31-B07-C05', 'R31-B07-C06'], '', True),
    ('S12', '研究矩阵依赖、typed来源与融合研究', ['R31-B08-C01', 'R31-B08-C02', 'R31-B08-C03'], '', True),
    ('S13', '研究产物数值合同与16逻辑表', ['R31-B08-C04', 'R31-B08-C05'], '', True),
    ('S14', '主链typed权威、资格合同与撤销闭环', ['R31-B10-C01', 'R31-B10-C02', 'R31-B10-C03'],
     '先完成权威迁移，后续S15/S16才允许任何破坏性清理。', True),
    ('S15', '保护迁移、引用闭包GC、CAS与事件有界', ['R31-B05-C04', 'R31-B05-C05', 'R31-B05-C06', 'R31-B05-C07'], '', True),
    ('S16', '图表缓存迁移清理、WAL与旧库重建', ['R31-B05-C08', 'R31-B05-C09', 'R31-B05-C10'], '', True),
    ('S17', '47面板稳定ID、慢图隔离与缓存限额', ['R31-B09-C01', 'R31-B09-C02', 'R31-B09-C03'], '', True),
    ('S18', '证据口径统一与47图/16表回归', ['R31-B09-C04', 'R31-B09-C05'], '', True),
    ('S19', '44指标/96检查完整报告', ['R31-B11-C01', 'R31-B11-C02', 'R31-B11-C03', 'R31-B11-C04'], '', True),
    ('S20', '迁移恢复、统一CI故障注入、受控退役与Android集中回归', ['R31-B12-C01', 'R31-B12-C02', 'R31-B12-C04'],
     '唯一执行EMU项的批次：全部批次累计的EMU项在此一次性跑模拟器矩阵；环境无法运行时如实记NOT_RUN，不记PASS。', True),
    ('S21', '发布冻结与成品交付', ['R31-B12-C05', 'R31-B12-C03'],
     '此批才做正式身份冻结、r982调用清单、签名和APK/ZIP；B12-C03全部是真机时长项，登记到DEFERRED，发布收据如实写NOT_RUN。', True),
]


def sha(b):
    return hashlib.sha256(b).hexdigest()


def main():
    cards = json.loads((P / 'ledgers/cards.json').read_text())
    by_id = {c['id']: c for c in cards}
    eng_by_id = {e['id']: e for e in ENG}
    order = {}
    for i, (sid, *_rest) in enumerate(BATCHES):
        for cid in _rest[1]:
            if cid in order:
                raise SystemExit('duplicate placement ' + cid)
            order[cid] = (sid, i)
    missing = [c['id'] for c in cards if c['id'] not in order]
    if missing:
        raise SystemExit('unplaced cards: ' + ', '.join(missing))

    # acceptance items
    items = []
    used_split, used_whole = set(), set()

    def add_items(cid, acc, origin_kind):
        sid, idx = order[cid]
        for n, text in enumerate(acc, 1):
            key = f'{cid}#A{n}'
            if key in SPLIT:
                used_split.add(key)
                for suf, lvl, t in SPLIT[key]:
                    items.append({'item': key + suf, 'card': cid, 'batch': sid, 'level': lvl,
                                  'blocking': lvl in BLOCKING, 'text': t, 'original_item': key,
                                  'original_text_sha256': sha(text.encode()), 'kind': origin_kind})
            else:
                lvl = WHOLE.get(key, 'LOCAL')
                if key in WHOLE:
                    used_whole.add(key)
                items.append({'item': key, 'card': cid, 'batch': sid, 'level': lvl,
                              'blocking': lvl in BLOCKING, 'text': text, 'original_item': key,
                              'original_text_sha256': sha(text.encode()), 'kind': origin_kind})

    for sid, name, cids, note, full in BATCHES:
        for cid in cids:
            if cid in by_id:
                add_items(cid, by_id[cid]['acceptance'], 'ORIGINAL_CARD')
            else:
                add_items(cid, eng_by_id[cid]['acceptance'], 'R312_ENGINEERING_CARD')
    stale = (set(SPLIT) - used_split) | (set(WHOLE) - used_whole)
    if stale:
        raise SystemExit('classification keys do not resolve: ' + ', '.join(sorted(stale)))

    # cards.json: add placement only; acceptance text untouched
    for c in cards:
        sid, idx = order[c['id']]
        c['r312_batch'] = sid
        c['r312_order'] = idx
        c['r312_batch_doc'] = f'batches/{sid}.md'
    (P / 'ledgers/cards.json').write_text(json.dumps(cards, ensure_ascii=False, indent=2) + '\n')

    # shared files inside/after batches
    file_batches = {}
    for c in cards:
        for f in c['files']:
            file_batches.setdefault(f['path'], []).append(order[c['id']][0])
    for e in ENG:
        for f in e['files']:
            file_batches.setdefault(f, []).append(e['batch'])

    batches = []
    for i, (sid, name, cids, note, full) in enumerate(BATCHES):
        files = []
        for cid in cids:
            if cid in by_id:
                files += [f['path'] for f in by_id[cid]['files']]
            else:
                files += eng_by_id[cid]['files']
        files = sorted(set(files))
        bitems = [x for x in items if x['batch'] == sid]
        orig_phases = sorted({by_id[c]['execution_phase'] for c in cids if c in by_id})
        batches.append({
            'batch': sid, 'order': i, 'name': name, 'cards': cids,
            'requires_closed': [BATCHES[i - 1][0]] if i else [],
            'original_phases': orig_phases,
            'scope_files': files,
            'later_batches_touching_same_files': sorted({b for f in files for b in file_batches.get(f, []) if b > sid}),
            'full_suite_must_pass': full,
            'emu_items_executed_here': sid == 'S20',
            'release_identity_freeze': sid == 'S21',
            'local_items': [x['item'] for x in bitems if x['level'] == 'LOCAL'],
            'deferred_items': [x['item'] for x in bitems if x['level'] != 'LOCAL'],
            'note': note,
            'doc': f'batches/{sid}.md',
        })
    for b in batches:
        if not b['local_items']:
            raise SystemExit('batch without blocking local item: ' + b['batch'])

    ledger = {
        'version': VERSION,
        'scope': 'SEQUENTIAL_CONSTRUCTION_BATCHES_NOT_APPLICATION_IMPLEMENTATION',
        'source_branch': SOURCE_BRANCH,
        'resume_commit': RESUME_COMMIT,
        'rule': '批次序号=施工顺序。上一批RECEIPT存在才可开下一批；同时只有一批OPEN；关门只看LOCAL项。',
        'levels': LEVELS,
        'blocking_levels': sorted(BLOCKING),
        'batch_count': len(batches),
        'original_card_count': len(cards),
        'engineering_card_count': len(ENG),
        'batches': batches,
    }
    (P / 'ledgers/r312_batches.json').write_text(json.dumps(ledger, ensure_ascii=False, indent=1) + '\n')
    (P / 'ledgers/r312_acceptance_items.json').write_text(json.dumps(items, ensure_ascii=False, indent=1) + '\n')
    (P / 'ledgers/r312_engineering_cards.json').write_text(json.dumps(ENG, ensure_ascii=False, indent=1) + '\n')
    deferred = [x for x in items if x['level'] != 'LOCAL']
    (P / 'ledgers/r312_deferred_items.json').write_text(json.dumps({
        'version': VERSION,
        'rule': '施工期不阻塞、不得记PASS。EMU在S20集中执行；DEVICE待真机；NATURAL不作施工门。',
        'counts': {lvl: sum(1 for x in deferred if x['level'] == lvl) for lvl in ('EMU', 'DEVICE', 'NATURAL')},
        'items': deferred}, ensure_ascii=False, indent=1) + '\n')

    # documents
    for c in cards:
        sid = order[c['id']][0]
        lv = [x for x in items if x['card'] == c['id']]
        tag = (f"> **R31.2施工批次：[{sid}](../../batches/{sid}.md)**。本卡验收分层：" +
               '；'.join(f"{x['item'].split('#')[1]}={x['level']}" for x in lv) +
               '。只有LOCAL项是本批关门条件；其余延后且不得记PASS。')
        f = P / 'cards' / c['batch'] / (c['id'] + '.md')
        lines = f.read_text().split('\n')
        lines = [l for l in lines if not l.startswith('> **R31.2施工批次')]
        k = 1
        while k < len(lines) and lines[k].strip() == '':
            k += 1
        lines = lines[:1] + ['', tag] + ([''] if lines[k:k+1] else []) + lines[k:]
        f.write_text('\n'.join(lines))
    write_card_docs()
    write_batch_docs(ledger, items, by_id, eng_by_id)
    counts = {lvl: sum(1 for x in items if x['level'] == lvl) for lvl in LEVELS}
    print(json.dumps({'batches': len(batches), 'items': len(items), 'levels': counts}, ensure_ascii=False))


def write_card_docs():
    for e in ENG:
        d = P / 'cards' / e['batch']
        d.mkdir(parents=True, exist_ok=True)
        lines = [f"# {e['id']} {e['title']}", '', f"来源：{e['origin']}。施工批次：[{e['batch']}](../../batches/{e['batch']}.md)。", '',
                 '## 问题', '', e['issue'], '', '## 文件', ''] + [f'- `{f}`' for f in e['files']] + \
                ['', '## 步骤', ''] + [f'{i}. {s}' for i, s in enumerate(e['steps'], 1)] + \
                ['', '## 验收（全部LOCAL，关门必过）', ''] + [f'- A{i}：{a}' for i, a in enumerate(e['acceptance'], 1)] + \
                ['', '## 禁止', '', '- 不加堆、不改阈值、不降断言、不跳过测试、不增加信任旁路、不把失败改成SKIP。', '']
        (d / (e['id'] + '.md')).write_text('\n'.join(lines))


WIP_FILE = P / 'ledgers/r312_wip_snapshot.json'
WIP = json.loads(WIP_FILE.read_text())['cards'] if WIP_FILE.is_file() else {}


def write_batch_docs(ledger, items, by_id, eng_by_id):
    d = P / 'batches'
    d.mkdir(exist_ok=True)
    idx_lines = ['# R31.2 顺序施工批次总表', '', f"版本：{ledger['version']}。按序号逐批施工，上一批RECEIPT提交后才开下一批。", '',
                 '| 序号 | 批次 | 卡 | 本地必过项 | 延后项 |', '|---|---|---|---|---|']
    for b in ledger['batches']:
        idx_lines.append(f"| {b['order']:02d} | [{b['batch']} {b['name']}]({b['batch']}.md) | {len(b['cards'])} | {len(b['local_items'])} | {len(b['deferred_items'])} |")
    (d / 'README.md').write_text('\n'.join(idx_lines) + '\n')

    for b in ledger['batches']:
        sid = b['batch']
        prev = b['requires_closed'][0] if b['requires_closed'] else None
        L = [f"# {sid} {b['name']}", '',
             f"R31.2第{b['order']:02d}批。原阶段：{', '.join(b['original_phases']) or '新增工程批'}。",
             '', '## 进入条件', '',
             (f"- `construction/r31/r312/{prev}/RECEIPT.json`已提交且`r312_gate.py status`显示当前批为{sid}。" if prev else
              f"- 施工分支`{ledger['source_branch']}`，HEAD含提交`{ledger['resume_commit'][:12]}`。"),
             '- 工作区干净或只含本批改动；不得开启下一批的任何新改动。',
             '- 树内历史状态只说明哪里已有代码；BUILD_PASS/IMPLEMENTING都不等于本批已验收，必须按下表重新绑定测试。', '']
        if b['note']:
            L += ['## 本批要点', '', b['note'], '']
        L += ['## 卡片（含接续起点）', '']
        for cid in b['cards']:
            if cid in by_id:
                c = by_id[cid]
                st = WIP.get(cid, 'NOT_STARTED')
                L.append(f"- [{cid}](../cards/{c['batch']}/{cid}.md) {c['title']}（树内历史状态：{st}）")
            else:
                L.append(f"- [{cid}](../cards/{sid}/{cid}.md) {eng_by_id[cid]['title']}")
        L += ['', '## 允许修改的主文件', '', '超出下列范围的主源码改动须在BINDINGS.json的scope_extensions逐条写原因；测试、tools/r31、tools/r312及本批证据目录不受此限。', '']
        L += [f'- `{f}`' for f in b['scope_files']]
        if b['later_batches_touching_same_files']:
            L += ['', f"后续批次{', '.join(b['later_batches_touching_same_files'])}还会改其中部分文件：本批只完成本批卡的合同，不预做后续卡。"]
        L += ['', '## 施工步骤（固定顺序）', '',
              '1. `python3 construction/STARCORE_R31_FINAL_20261104/tools/r312_gate.py status`，确认当前批。',
              '2. 只读本页与本批卡片；对照WIP_INVENTORY确认本批卡已有的树内代码，接续而非重写。',
              '3. 为下表每个LOCAL项先写或定位测试（JUnit类#方法，或HOST_SUITE条目），在BINDINGS.json登记。',
              '4. 修改源码直到这些测试通过；只改本批范围文件。',
              '5. 提交代码与BINDINGS.json后运行`r312_gate.py build --batch ' + sid + '`（本地无SDK时推送触发r312-gate.yml，再`--import`其BUILD_SUMMARY）。',
              '6. 失败就在本批内修，重复4–5；不得把失败挪到后面的批次或改成延后项。',
              '7. `r312_gate.py close --batch ' + sid + '`生成RECEIPT.json，连同BUILD_SUMMARY提交推送；本批结束。', '']
        L += ['## 关门必过项（LOCAL）', '', '| 项 | 内容 |', '|---|---|']
        for x in items:
            if x['batch'] == sid and x['level'] == 'LOCAL':
                L.append(f"| {x['item']} | {x['text'].replace('|', '/')} |")
        L += ['', '关门还要求：' + ('全量单元失败=0、HOST_SUITE全PASS、lintDebug与androidTest编译通过。' if b['full_suite_must_pass'] else
                                  'gradle与HOST全部实际执行并逐条记录；全量失败允许存在但须进入WIP_INVENTORY.failures，作为S01输入。'), '']
        dl = [x for x in items if x['batch'] == sid and x['level'] != 'LOCAL']
        if dl:
            L += ['## 不阻塞本批的延后项', '', '这些项登记在`ledgers/r312_deferred_items.json`；本批RECEIPT中状态写NOT_RUN，禁止写PASS。', '',
                  '| 项 | 层级 | 内容 |', '|---|---|---|']
            for x in dl:
                L.append(f"| {x['item']} | {x['level']} | {x['text'].replace('|', '/')} |")
            L.append('')
        L += ['## 禁止', '',
              '- 降低断言、改阈值、加堆、跳过/排除测试、捕获异常后直接PASS、把LOCAL项改成延后。',
              '- 重新分析整包、重做历史复算、从5237重建、重写已关批次的RECEIPT。',
              '- 在本批内做发布身份冻结、签名或APK发布（S21除外）。', '']
        (d / f'{sid}.md').write_text('\n'.join(L))


if __name__ == '__main__':
    main()
