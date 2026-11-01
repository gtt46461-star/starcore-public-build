package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Immutable typed research tensors. Legacy FrozenMatrix retains its original position-number ABI. */
public final class MatrixTensorR30R1 {
    private MatrixTensorR30R1(){}
    public static final String VERSION="R30_REPAIR_TYPED_MATRIX_1";
    private static final Map<String,Map<String,Object>> MUTABLE=new LinkedHashMap<>();
    public static final Map<String,Map<String,Object>> CONTRACTS=Collections.unmodifiableMap(MUTABLE);
    static {
        contract("R930:EVIDENCE_TEACHER","teacher×target","合法教师冻结概率/ABI/支持","教师选择/解释");
        contract("R930:STUDENT_ERROR","student×error-condition","学生事前预测与标签","失败驱动处方");
        contract("R930:FORWARD_REASONING","rule×premise","可执行规则与PIT前提","AST/推理提案");
        contract("R930:REVERSE_REASONING","target×premise","目标与历史可用前提","新假设生成");
        contract("R930:COUNTERFACTUAL","component×context","精确with/without输出","测得反事实子代");
        contract("R930:FEEDBACK","state×settled-error","事前状态与结算回执","研究更新");
        contract("R930:CONTRADICTION","claim×evidence","同口径主张/证据","拒绝冲突输入");
        contract("R930:TEACHER_DIVERSITY","teacher×teacher","固定probe教师输出","保留互补教师");
        contract("R930:TEMPORAL_PRECEDENCE","source×target-time","availableAt与freezeAt","PIT阻断");
        contract("R930:INVARIANCE","condition×transform","预声明保持语义变换","鲁棒性筛选");
        contract("R930:CAUSAL_SUPPORT","mechanism×assumption","PIT条件与受控消融","候选处方审查");
        contract("R930:COUNTER_EVIDENCE","hypothesis×counterexample","完整反例原件","重复失败记忆");
        contract("R930:RESIDUAL_MECHANISM","condition×residual","同目标残差及机制标签","新机制候选");
        contract("R930:NUMBER_POSITION_COMPATIBILITY","number×position","严格滞后号码/位置特征","跨位/集合AST");
        contract("R930:CROSS_POSITION_FLOW","position×lag","跨位滞后流","IdeaI04候选");
        contract("R930:ACTION_UTILITY","action×outcome","合法冻结动作及真实结算","研究行动规则");
        contract("R930:ALGORITHM_COMPLEMENTARITY","algorithm×algorithm","同期残差与融合输出","新融合资产");
        contract("R930:EVOLUTION_CREDIT","operator×child","proposal/child/CPU/role","研究预算分配");
        contract("R930:RESOURCE_EFFICIENCY","task×resource","CPU/内存/队列/真实commit","公平调度");
    }
    private static void contract(String id,String dimensions,String input,String action){Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("dimensions",dimensions);m.put("inputContract",input);m.put("intendedConsumer",action);m.put("producerVersion",VERSION);MUTABLE.put(id,Collections.unmodifiableMap(m));}
    public static Map<String,Object> contract(String kind){Map<String,Object> c=CONTRACTS.get("R930:"+kind);if(c==null)throw new IllegalArgumentException("UNKNOWN_MATRIX_CONTRACT:"+kind);return c;}
    public static final class Tensor {
        public final String kind,role,datasetHash,inputHash,policyHash,payload,hash,throughIssue,targetIssue,analysisRun;public final long revision,throughOrdinal;public final int cells;
        public Tensor(String kind,String role,String dataset,String policy,String through,String target,long revision,List<String> rowLabels,List<String> columnLabels,double[][] values,long[][] support,Map<String,Object> details){
            contract(kind);if(!Arrays.asList("TRAIN","SELECT","ONLINE_RESEARCH","SYNTHETIC").contains(role))throw new IllegalArgumentException("FORMAL_ROLE_CANNOT_DRIVE_RESEARCH_MATRIX");
            if(!dataset.matches("[0-9a-f]{64}")||!policy.matches("[0-9a-f]{64}")||revision<0||rowLabels.isEmpty()||columnLabels.isEmpty()||rowLabels.size()>64||columnLabels.size()>64||new HashSet<>(rowLabels).size()!=rowLabels.size()||new HashSet<>(columnLabels).size()!=columnLabels.size())throw new IllegalArgumentException("MATRIX_HEADER");
            long source=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(through),next=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(target);if(source>=next)throw new IllegalArgumentException("MATRIX_SOURCE_NOT_PAST");
            if(values.length!=rowLabels.size()||support.length!=values.length)throw new IllegalArgumentException("MATRIX_ROWS");
            List<Object> actual=new ArrayList<>(),counts=new ArrayList<>();for(int r=0;r<values.length;r++){if(values[r].length!=columnLabels.size()||support[r].length!=columnLabels.size())throw new IllegalArgumentException("MATRIX_COLUMNS");List<Object> v=new ArrayList<>(),n=new ArrayList<>();for(int c=0;c<values[r].length;c++){if(!Double.isFinite(values[r][c])||support[r][c]<0)throw new IllegalArgumentException("MATRIX_CELL");v.add(support[r][c]==0?null:values[r][c]);n.add(support[r][c]);}actual.add(v);counts.add(n);}
            analysisRun=details.get("runId") instanceof String?(String)details.get("runId"):null;this.kind=kind;this.role=role;datasetHash=dataset;policyHash=policy;throughIssue=through;targetIssue=target;this.revision=revision;throughOrdinal=source;cells=values.length*columnLabels.size();
            inputHash=StatsR27.sha256(dataset+"|"+policy+"|"+source+"|"+revision+"|"+role);
            Map<String,Object> m=new LinkedHashMap<>(contract(kind));m.put("schema",VERSION);m.put("role",role);m.put("datasetHash",dataset);m.put("policyHash",policy);m.put("inputHash",inputHash);m.put("sourceRevision",revision);m.put("inputThrough",through);m.put("inputThroughOrdinal",source);m.put("effectiveAfterIssue",target);m.put("rows",rowLabels);m.put("columns",columnLabels);m.put("values",actual);m.put("support",counts);m.put("details",details);m.put("qualityEffective",null);m.put("qualityScope","RESEARCH_ASSOCIATION_NOT_CAUSAL_OR_NATURAL_CERTIFICATE");payload=JsonR27.write(m);if(payload.length()>262144)throw new IllegalArgumentException("MATRIX_PAYLOAD_BUDGET");hash=StatsR27.sha256(payload);
        }
    }
    public static void append(ResearchWriteQueueR27.Batch b,Tensor t,long now){
        b.add("INSERT OR IGNORE INTO research_matrix_snapshot_r930(matrix_hash,snapshot_hash,target_issue,max_source_issue,kind,frozen_at,input_stage,graph_hash,input_hash,bundle_hash,model_hash,present_cells,created_at,payload_json,role,source_revision,producer_version,analysis_run_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",t.hash,t.hash,t.targetIssue,t.throughIssue,t.kind,now,"TYPED_RESEARCH",t.policyHash,t.inputHash,t.datasetHash,t.policyHash,t.cells,now,t.payload,t.role,t.revision,VERSION,t.analysisRun);
    }
}
