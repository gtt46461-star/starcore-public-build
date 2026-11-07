#!/usr/bin/env python3
"""Generate the R31.4 (11 merged construction batches) batch ledgers and batch pages (single source of the batch layout).

Original 56 cards and their acceptance text are never edited here; only placement fields and derived
ledgers/pages are (re)written. Idempotent.
"""
from pathlib import Path
import hashlib
import json

P = Path(__file__).resolve().parents[1]
VERSION = 'R31.4-MERGED-11-20261107'
PREVIOUS_VERSION = 'R31.3-CONSTRUCTION-20261105'
RESUME_COMMIT = '293bb23015e9ff694b4344aaf688cb4c1f6e356d'
SOURCE_BRANCH = 'construction/r31-final-20261104'
LAST_FULL_GREEN_BATCH = 'K11'
RELEASE_BATCH = 'K11'

LEVELS = {
    'LOCAL': '本地/CI确定性执行：JVM单元、HOST协议、静态检查、编译、lint。本批关门需绑定通过的测试（或按结转规则带红测试结转）。',
    'EMU': 'Android模拟器/Android SQLite仪器测试。只在K11集中执行；其余批次不阻塞，不得记PASS。',
    'DEVICE': '真机/时长/设备时延/截图（V2353A、24h、72h、设备P95）。施工期DEFERRED，不阻塞，不得记PASS。',
    'NATURAL': '自然未来质量。永远不是施工门，只能NOT_PROVEN。',
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

# ---------------------------------------------------------------- repair cards for code already merged
ENG = [
    {'id': 'R31-ENG-01', 'batch': 'K03', 'title': '修复启动Certificate关键域指纹失配（随B02-C02同批）',
     'issue': 'CI015 API29/35各10项协议8项失败于SCHEMA_PENDING:R31_CERTIFICATE_REVOCATION:UNTRUSTED_CRITICAL:FINGERPRINT_MISMATCH；共享启动增量认证改动(493080ad)后，Certificate关键域指纹的登记方与校验方不一致。SchemaDomainCoordinatorR926由B02-C02负责，故同批修。',
     'files': ['app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java',
               'app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java'],
     'steps': ['用HOST条目run_certificate_startup_host（host.sh已跑全部启动顺序与两个反例）复现；若HOST全过而仅Android失败，补一个HOST/JVM用例复现Android的初始化顺序。',
               '对比指纹计算输入（规范化DDL、版本号、登记顺序）在登记方与启动校验方的差异，改为唯一来源计算。',
               '旧库已有合法指纹按迁移规则重新认证；篡改仍拒绝；不加信任旁路，不改断言。'],
     'acceptance': ['新库启动Certificate域READY（HOST/JVM用例）。',
                    'R30旧库升级后READY，既有撤销记录逐行不变。',
                    '篡改关键DDL必然FINGERPRINT_MISMATCH拒绝。'],
     'origin': 'B02-C02在制品回归'},
    {'id': 'R31-ENG-02', 'batch': 'K02', 'title': '内存准入在制品与既有研究测试相容（不加堆、不改阈值）',
     'issue': 'MemoryBudgetR31（B05-C11在制品）合入后，11项既有研究测试在原512MiB下报MEMORY_DECLARED_CAPACITY/MEMORY_HEAP_PRESSURE。这些测试守护ResearchPool等K02–K07要改的文件，所以尽早修复。',
     'files': ['app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java',
               'app/src/main/java/com/ethk3/novaonepro/strategyv2/BoundedResearchExecutorR31.java',
               'app/src/main/java/com/ethk3/novaonepro/strategyv2/SharedFeatureStoreR31.java'],
     'steps': ['对失败类分别单独运行与全量运行：单独过、全量挂=前序泄漏；单独也挂=容量估算或真实压力（554d2427已有诊断记录，先读它）。',
               '泄漏：修生产代码lease的close/finally与owner生命周期、有界缓存回收；测试间复位只能调用生产API。',
               '估算偏大：按实测上界修正估算函数；真实压力：同输入共享实例。'],
     'acceptance': ['原512MiB、原阈值下，原11项CapacityWait失败测试通过。',
                    '全量运行中每个测试结束outstanding lease=0（守卫断言）。',
                    '超额请求仍被拒绝、异常释放、重复close幂等。'],
     'origin': 'B05-C11在制品回归'},
]

# ---------------------------------------------------------------- batches (order = construction order)
BATCHES = [
    ('K01', '基线身份与故障观测；登记已知失败基线', ['R31-B00-C01', 'R31-B00-C02'],
     'B00两卡源码已完成，S00的绑定可直接沿用。本批首次全量运行，把当时所有失败登记为“已知失败”，并给每条指定负责修复的批次（只能往后、最迟K11）。已于R31.3关门，R31.4不改本批。'),
    ('K02', '内存准入相容 + 任务故障分类与owner/fence', ['R31-ENG-02', 'R31-B01-C01', 'R31-B01-C02'],
     'R31.4不改本批（与R31.3的K02完全相同）。'),
    ('K03', '有限隔离恢复与可中断关闭 + 写优先级、短事务、提交结果 + Certificate指纹',
     ['R31-B01-C03', 'R31-B01-C04', 'R31-B02-C01', 'R31-B02-C02', 'R31-B02-C03', 'R31-ENG-01'],
     '合并自R31.3的K03+K04（同属原P01）。按下列卡片顺序施工：先B01-C03/C04，再B02三卡与ENG-01。本批结束时，B01/B02相关HOST联合协议（write_protocol、task_runtime、writer_hold_fault、runtime_ddl、certificate_startup）不得在已知失败中由本批负责而仍失败。'),
    ('K04', '抓取、唯一冻结与结算解耦 + 研究健康与后台能力 + 存储普查、写前准入与报告清理 + 内存与缓存队列有界治理',
     ['R31-B03-C01', 'R31-B03-C02', 'R31-B03-C03', 'R31-B04-C01', 'R31-B04-C02',
      'R31-B05-C01', 'R31-B05-C02', 'R31-B05-C03', 'R31-B05-C11'],
     '合并自R31.3的K05+K06+K07+K08（同属原P02）。按B03三卡、B04两卡、B05-C01..C03、B05-C11顺序施工；B04-C02多为真机项，本批只做本地可验部分。'),
    ('K05', '历史回放与模型状态共同提交', ['R31-B06-C01', 'R31-B06-C02', 'R31-B06-C03'],
     '原R31.3的K09（原P03单独成批：P04的卡必须在P03关门后才能开工）。'),
    ('K06', '家族保全、typed AST与创造预算 + 父子比较、工厂检出力与能力升级',
     ['R31-B07-C01', 'R31-B07-C02', 'R31-B07-C03', 'R31-B07-C04', 'R31-B07-C05', 'R31-B07-C06'],
     '合并自R31.3的K10+K11（同属原P04）。先B07-C01..C03，再B07-C04..C06。'),
    ('K07', '研究矩阵依赖、typed来源与融合研究 + 研究产物数值合同与16逻辑表',
     ['R31-B08-C01', 'R31-B08-C02', 'R31-B08-C03', 'R31-B08-C04', 'R31-B08-C05'],
     '合并自R31.3的K12+K13（同属原P04）。先B08-C01..C03，再B08-C04/C05。'),
    ('K08', '主链typed权威、资格合同与撤销闭环', ['R31-B10-C01', 'R31-B10-C02', 'R31-B10-C03'],
     '原R31.3的K14（原P05单独成批）：主链权威迁移关门后，K09才允许任何破坏性清理。'),
    ('K09', '保护迁移、引用闭包GC、CAS与事件有界 + 图表缓存迁移清理、WAL与旧库重建 + 47面板稳定ID、慢图隔离与缓存限额 + 证据口径统一与47图/16表回归',
     ['R31-B05-C04', 'R31-B05-C05', 'R31-B05-C06', 'R31-B05-C07', 'R31-B05-C08', 'R31-B05-C09', 'R31-B05-C10',
      'R31-B09-C01', 'R31-B09-C02', 'R31-B09-C03', 'R31-B09-C04', 'R31-B09-C05'],
     '合并自R31.3的K15+K16+K17+K18（同属原P06）。按B05-C04..C07、B05-C08..C10、B09-C01..C03、B09-C04/C05顺序施工。'),
    ('K10', '44指标/96检查完整报告', ['R31-B11-C01', 'R31-B11-C02', 'R31-B11-C03', 'R31-B11-C04'],
     '原R31.3的K19（原P07单独成批）。'),
    ('K11', '迁移恢复、统一CI故障注入、受控退役 + 全绿 + Android集中回归 + 发布冻结与成品交付',
     ['R31-B12-C01', 'R31-B12-C02', 'R31-B12-C04', 'R31-B12-C05', 'R31-B12-C03'],
     '合并自R31.3的K20+K21（同属原P08）。顺序强制：先完成B12-C01/C02/C04，已知失败清零、所有结转兑现，EMU项跑一次模拟器矩阵（环境不可用写NOT_RUN，不写PASS）；之后才做B12-C05的正式身份冻结、r982调用清单、签名和APK/ZIP。B12-C03全是真机时长项，登记DEFERRED，发布收据如实写NOT_RUN。'),
]

# R31.3 batch id -> R31.4 batch id. Receipts written under R31.3 (K01) name owner batches in the old
# numbering; r31_gate.py translates them through this map. Receipts written under R31.4 are not translated.
LEGACY_BATCH_MAP = {PREVIOUS_VERSION: {
    'K01': 'K01', 'K02': 'K02', 'K03': 'K03', 'K04': 'K03', 'K05': 'K04', 'K06': 'K04', 'K07': 'K04', 'K08': 'K04',
    'K09': 'K05', 'K10': 'K06', 'K11': 'K06', 'K12': 'K07', 'K13': 'K07', 'K14': 'K08', 'K15': 'K09', 'K16': 'K09',
    'K17': 'K09', 'K18': 'K09', 'K19': 'K10', 'K20': 'K11', 'K21': 'K11'}}



def sha(b):
    return hashlib.sha256(b).hexdigest()


def main():
    cards = json.loads((P / 'ledgers/cards.json').read_text())
    by_id = {c['id']: c for c in cards}
    eng_by_id = {e['id']: e for e in ENG}
    order = {}
    for i, (bid, _n, cids, _note) in enumerate(BATCHES):
        for cid in cids:
            if cid in order:
                raise SystemExit('duplicate placement ' + cid)
            order[cid] = (bid, i)
    missing = [c['id'] for c in cards if c['id'] not in order] + [e['id'] for e in ENG if e['id'] not in order]
    if missing:
        raise SystemExit('unplaced cards: ' + ', '.join(missing))
    for e in ENG:
        if order[e['id']][0] != e['batch']:
            raise SystemExit('engineering card batch mismatch ' + e['id'])

    items, used = [], set()

    def add_items(cid, acc, kind):
        bid = order[cid][0]
        for n, text in enumerate(acc, 1):
            key = f'{cid}#A{n}'
            h = sha(text.encode())
            if key in SPLIT:
                used.add(key)
                for suf, lvl, t in SPLIT[key]:
                    items.append({'item': key + suf, 'card': cid, 'batch': bid, 'level': lvl, 'blocking': lvl in BLOCKING,
                                  'text': t, 'original_item': key, 'original_text_sha256': h, 'kind': kind})
            else:
                lvl = WHOLE.get(key, 'LOCAL')
                if key in WHOLE:
                    used.add(key)
                items.append({'item': key, 'card': cid, 'batch': bid, 'level': lvl, 'blocking': lvl in BLOCKING,
                              'text': text, 'original_item': key, 'original_text_sha256': h, 'kind': kind})

    for bid, _n, cids, _note in BATCHES:
        for cid in cids:
            if cid in by_id:
                add_items(cid, by_id[cid]['acceptance'], 'ORIGINAL_CARD')
            else:
                add_items(cid, eng_by_id[cid]['acceptance'], 'REPAIR_CARD')
    stale = (set(SPLIT) | set(WHOLE)) - used
    if stale:
        raise SystemExit('classification keys do not resolve: ' + ', '.join(sorted(stale)))

    for c in cards:
        for k in ('r312_batch', 'r312_order', 'r312_batch_doc'):
            c.pop(k, None)
        bid, idx = order[c['id']]
        c['construction_batch'] = bid
        c['construction_order'] = idx + 1
        c['construction_batch_doc'] = f'batches/{bid}.md'
    (P / 'ledgers/cards.json').write_text(json.dumps(cards, ensure_ascii=False, indent=2) + '\n')

    file_batches = {}
    for c in cards:
        for f in c['files']:
            file_batches.setdefault(f['path'], set()).add(order[c['id']][0])
    for e in ENG:
        for f in e['files']:
            file_batches.setdefault(f, set()).add(e['batch'])

    batches = []
    for i, (bid, name, cids, note) in enumerate(BATCHES):
        files = sorted({f['path'] for cid in cids if cid in by_id for f in by_id[cid]['files']} |
                       {f for cid in cids if cid in eng_by_id for f in eng_by_id[cid]['files']})
        bitems = [x for x in items if x['batch'] == bid]
        batches.append({
            'batch': bid, 'order': i + 1, 'name': name, 'cards': cids,
            'requires_closed': [BATCHES[i - 1][0]] if i else [],
            'original_phases': sorted({by_id[c]['execution_phase'] for c in cids if c in by_id}),
            'scope_files': files,
            'later_batches_touching_same_files': sorted({b for f in files for b in file_batches.get(f, ()) if b > bid}),
            'records_known_failure_baseline': bid == 'K01',
            'requires_zero_known_failures': bid in (LAST_FULL_GREEN_BATCH, RELEASE_BATCH),
            'emu_items_executed_here': bid == LAST_FULL_GREEN_BATCH,
            'release_identity_freeze': bid == RELEASE_BATCH,
            'max_carried_items': 0 if bid in (LAST_FULL_GREEN_BATCH, RELEASE_BATCH) else 2,
            'local_items': [x['item'] for x in bitems if x['level'] == 'LOCAL'],
            'deferred_items': [x['item'] for x in bitems if x['level'] != 'LOCAL'],
            'note': note, 'doc': f'batches/{bid}.md'})
    for b in batches:
        if not b['local_items']:
            raise SystemExit('batch without LOCAL item: ' + b['batch'])

    ledger = {
        'version': VERSION,
        'scope': 'CONSTRUCTION_BATCHES_NOT_APPLICATION_IMPLEMENTATION',
        'source_branch': SOURCE_BRANCH, 'resume_commit': RESUME_COMMIT,
        'rule': '批次序号=施工顺序；上一批RECEIPT提交后才开下一批。关门用棘轮：不得新增失败、本批负责的已知失败必须修好、LOCAL项绑定通过的测试（最多2项带红测试结转）；K11清零。R31.4把R31.3的21批相邻合并为11批，卡片、验收原文、阈值不变。',
        'levels': LEVELS, 'blocking_levels': sorted(BLOCKING),
        'last_batch_for_known_failures_and_carries': LAST_FULL_GREEN_BATCH,
        'legacy_batch_map': LEGACY_BATCH_MAP, 'previous_version': PREVIOUS_VERSION,
        'batch_count': len(batches), 'original_card_count': len(cards), 'repair_card_count': len(ENG),
        'batches': batches}
    (P / 'ledgers/batches.json').write_text(json.dumps(ledger, ensure_ascii=False, indent=1) + '\n')
    (P / 'ledgers/acceptance_items.json').write_text(json.dumps(items, ensure_ascii=False, indent=1) + '\n')
    (P / 'ledgers/repair_cards.json').write_text(json.dumps(ENG, ensure_ascii=False, indent=1) + '\n')
    deferred = [x for x in items if x['level'] != 'LOCAL']
    (P / 'ledgers/deferred_items.json').write_text(json.dumps({
        'version': VERSION, 'rule': '施工期不阻塞、不得记PASS。EMU在K11集中执行；DEVICE待真机；NATURAL不作施工门。',
        'counts': {lvl: sum(1 for x in deferred if x['level'] == lvl) for lvl in ('EMU', 'DEVICE', 'NATURAL')},
        'items': deferred}, ensure_ascii=False, indent=1) + '\n')

    write_execution_ledgers(batches, by_id)
    tag_card_docs(cards, items, order)
    write_repair_docs()
    write_batch_docs(ledger, items, by_id, eng_by_id)
    print(json.dumps({'batches': len(batches), 'items': len(items),
                      'levels': {l: sum(1 for x in items if x['level'] == l) for l in LEVELS}}, ensure_ascii=False))


def write_execution_ledgers(batches, by_id):
    phases = []
    for b in batches:
        phases.append({'phase': b['batch'], 'order': b['order'], 'name': b['name'],
                       'batches': sorted({by_id[c]['batch'] if c in by_id else 'ENG' for c in b['cards']}),
                       'requires': b['requires_closed'], 'cards': b['cards'], 'original_phases': b['original_phases'],
                       'doc': b['doc'], 'execution_policy': 'ledgers/execution_policy.json',
                       'closure': 'tools/r31_gate.py close (ratchet; LOCAL items bound or carried)',
                       'physical_device_is_source_prerequisite': False})
    (P / 'ledgers/execution_phases.json').write_text(json.dumps(phases, ensure_ascii=False, indent=1) + '\n')
    pol_path = P / 'ledgers/execution_policy.json'
    pol = json.loads(pol_path.read_text())
    if pol['version'] != VERSION:
        pol['supersedes'] = [pol['version']] + [v for v in pol.get('supersedes', []) if v != pol['version']]
    pol['version'] = VERSION
    pol['r31_4_merge'] = {'reason': 'User-directed: 21 construction batches merged into 11 adjacent batches. Card text, acceptance text, '
                                    'thresholds, original phase order and ratchet rules unchanged; K01 (closed) and K02 (in progress) unchanged.',
                          'legacy_batch_map': LEGACY_BATCH_MAP[PREVIOUS_VERSION]}
    pol['sequence']['batches'] = [b['batch'] for b in batches]
    pol['closure']['ratchet']['zero_known_failures_from'] = LAST_FULL_GREEN_BATCH
    pol['closure']['carry']['latest_target'] = LAST_FULL_GREEN_BATCH
    pol['closure']['emu_executed_in'] = LAST_FULL_GREEN_BATCH
    pol['closure']['release_only_batch'] = RELEASE_BATCH
    pol['preservation']['construction_batches'] = len(batches)
    pol_path.write_text(json.dumps(pol, ensure_ascii=False, indent=1) + '\n')
    sum_path = P / 'ledgers/package_summary.json'
    summ = json.loads(sum_path.read_text())
    summ['plan_version'] = VERSION
    summ['construction_batches'] = len(batches)
    sum_path.write_text(json.dumps(summ, ensure_ascii=False, indent=1) + '\n')


def tag_card_docs(cards, items, order):
    for c in cards:
        bid = order[c['id']][0]
        lv = [x for x in items if x['card'] == c['id']]
        tag = (f"> **施工批次：[{bid}](../../batches/{bid}.md)**。验收分层：" +
               '；'.join(f"{x['item'].split('#')[1]}={x['level']}" for x in lv) + '。LOCAL项是关门条件，其余延后且不得记PASS。')
        f = P / 'cards' / c['batch'] / (c['id'] + '.md')
        lines = [l for l in f.read_text().split('\n') if not l.startswith('> **R31.2施工批次') and not l.startswith('> **施工批次')]
        k = 1
        while k < len(lines) and lines[k].strip() == '':
            k += 1
        f.write_text('\n'.join(lines[:1] + ['', tag, ''] + lines[k:]))


def write_repair_docs():
    d = P / 'cards' / 'ENG'
    d.mkdir(parents=True, exist_ok=True)
    for e in ENG:
        L = [f"# {e['id']} {e['title']}", '', f"来源：{e['origin']}。施工批次：[{e['batch']}](../../batches/{e['batch']}.md)。", '',
             '## 问题', '', e['issue'], '', '## 文件', ''] + [f'- `{f}`' for f in e['files']] + \
            ['', '## 步骤', ''] + [f'{i}. {s}' for i, s in enumerate(e['steps'], 1)] + \
            ['', '## 验收（LOCAL）', ''] + [f'- A{i}：{a}' for i, a in enumerate(e['acceptance'], 1)] + \
            ['', '## 禁止', '', '不加堆、不改阈值、不降断言、不跳过测试、不加信任旁路。', '']
        (d / (e['id'] + '.md')).write_text('\n'.join(L))


def cell(t):
    return str(t).replace('|', '/').replace('\n', ' ')


def write_batch_docs(ledger, items, by_id, eng_by_id):
    d = P / 'batches'
    d.mkdir(exist_ok=True)
    for old in d.glob('*.md'):
        old.unlink()
    idx = ['# R31.4 施工批次总表（11批）', '', f"版本：{ledger['version']}。按序号逐批施工；每批一条命令关门：`r31_gate.py close --batch Kxx`。", '',
           '| 序号 | 批次 | 卡 | LOCAL项 | 延后项 |', '|---|---|---|---|---|']
    for b in ledger['batches']:
        idx.append(f"| {b['order']:02d} | [{b['batch']} {b['name']}]({b['batch']}.md) | {len(b['cards'])} | {len(b['local_items'])} | {len(b['deferred_items'])} |")
    (d / 'README.md').write_text('\n'.join(idx) + '\n')
    for b in ledger['batches']:
        bid = b['batch']
        prev = b['requires_closed'][0] if b['requires_closed'] else None
        L = [f"# {bid} {b['name']}", '', f"第{b['order']:02d}批（共{ledger['batch_count']}批）。原阶段：{', '.join(b['original_phases']) or '-'}。", '',
             '## 进入条件', '',
             (f"`construction/r31/batches/{prev}/RECEIPT.json`已提交（`r31_gate.py status`显示当前批为{bid}）。" if prev else
              f"施工分支`{ledger['source_branch']}`含提交`{ledger['resume_commit'][:12]}`。"), '']
        if b['note']:
            L += ['## 本批要点', '', b['note'], '']
        L += ['## 施工内容（先改代码，这是本批主体）', '']
        for cid in b['cards']:
            if cid in by_id:
                c = by_id[cid]
                L += [f"### {cid} {c['title']}", '',
                      f"卡片原文：[cards/{c['batch']}/{cid}.md](../cards/{c['batch']}/{cid}.md)。树内历史状态：{WIP.get(cid, 'NOT_STARTED')}（只说明已有代码在哪里，不等于已验收）。", '',
                      f"问题：{c['issue']}", '', '步骤：', ''] + [f'{i}. {s}' for i, s in enumerate(c['steps'], 1)] + \
                     ['', '| 文件 | 方法 | 操作 | 改成 |', '|---|---|---|---|'] + \
                     [f"| `{f['path']}` | {cell(', '.join(f['methods']))} | {f['operation']} | {cell(f['after'])} |" for f in c['files']] + ['']
            else:
                e = eng_by_id[cid]
                L += [f"### {cid} {e['title']}", '', f"卡片：[cards/ENG/{cid}.md](../cards/ENG/{cid}.md)。", '', f"问题：{e['issue']}", '', '步骤：', ''] + \
                     [f'{i}. {s}' for i, s in enumerate(e['steps'], 1)] + ['', '文件：' + '、'.join(f'`{f}`' for f in e['files']), '']
        if b['later_batches_touching_same_files']:
            L += [f"后续{', '.join(b['later_batches_touching_same_files'])}还会改其中部分文件：本批只做本批卡，不预做后续卡。", '']
        L += ['## 验收（LOCAL，每项在BINDINGS.json绑定至少一个测试）', '', '| 项 | 内容 |', '|---|---|']
        L += [f"| {x['item']} | {cell(x['text'])} |" for x in items if x['batch'] == bid and x['level'] == 'LOCAL']
        L += ['', f"本批最多结转{b['max_carried_items']}项（必须已有提交的红测试，指定后续批次且不晚于{LAST_FULL_GREEN_BATCH}）。" if b['max_carried_items'] else
              '本批不得结转，已知失败必须清零。', '']
        dl = [x for x in items if x['batch'] == bid and x['level'] != 'LOCAL']
        if dl:
            L += ['## 延后项（不阻塞，RECEIPT中只能是NOT_RUN/NOT_PROVEN）', '', '| 项 | 层级 | 内容 |', '|---|---|---|']
            L += [f"| {x['item']} | {x['level']} | {cell(x['text'])} |" for x in dl]
            L.append('')
        L += ['## 关门', '', '```bash', f'python3 construction/STARCORE_R31_FINAL_20261104/tools/r31_gate.py quick --batch {bid}   # 改代码过程中，可多次',
              f'python3 construction/STARCORE_R31_FINAL_20261104/tools/r31_gate.py close --batch {bid}   # 改完后一次：全量构建+判定+写RECEIPT', '```', '',
              '`close`不通过就按列出的问题在本批修，再`close`；通过后提交`construction/r31/batches/' + bid + '/`与`verification/r31/batches/' + bid + '/`并推送，进入下一批。', '']
        (d / f'{bid}.md').write_text('\n'.join(L))


WIP_FILE = P / 'ledgers/wip_snapshot.json'
WIP = json.loads(WIP_FILE.read_text())['cards'] if WIP_FILE.is_file() else {}

if __name__ == '__main__':
    main()
