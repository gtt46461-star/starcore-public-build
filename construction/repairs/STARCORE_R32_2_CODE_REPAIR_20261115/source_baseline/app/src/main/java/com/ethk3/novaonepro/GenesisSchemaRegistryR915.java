package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Executable Stage-2 schema foundation imported from 算法创造方案 V4.1.  These 240 L0 atoms and 72
 * navigation families live only in GENESIS_NEW_ASSETS; they neither rename nor mutate legacy
 * assets.  Later stages must persist an AssetGenome/ResearchGenome and earn Future OOS evidence.
 */
public final class GenesisSchemaRegistryR915 {
    public static final String NAMESPACE = "GENESIS_NEW";
    public static final int EXPECTED_ATOMS = 240;
    public static final int EXPECTED_FAMILIES = 72;
    public static final int EXPECTED_PRODUCTION_SCORE_ATOMS = 165;
    public static final int EXPECTED_RESEARCH_PROXY_ATOMS = 40;
    public static final int EXPECTED_AUDIT_IDENTITY_ATOMS = 15;
    public static final int EXPECTED_SETTLEMENT_EVIDENCE_ATOMS = 20;

    public static final String TIER_PRODUCTION_SCORE = "PRODUCTION_SCORE_SEMANTIC";
    public static final String TIER_RESEARCH_PROXY = "RESEARCH_PROXY_NOT_CERTIFIABLE";
    public static final String TIER_AUDIT_IDENTITY = "AUDIT_IDENTITY_NOT_NUMERIC";
    public static final String TIER_SETTLEMENT_EVIDENCE = "SETTLEMENT_ONLY_NOT_PREDRAW";

    public static final class AtomSpec {
        public final String id, group, name, valueType, valueRange, windowSpec;
        public final String leakagePolicy, updateTiming, evidenceStage, costClass;
        public final int maxLag, minSupport, version;
        AtomSpec(String id,String group,String name,String type,int groupIndex){
            this.id=id;this.group=group;this.name=name;valueType=type;
            valueRange=rangeFor(type);windowSpec=windowFor(groupIndex);
            maxLag=maxLagFor(groupIndex);minSupport=minSupportFor(groupIndex);
            evidenceStage=stageFor(groupIndex);leakagePolicy=leakageFor(groupIndex);
            updateTiming=timingFor(groupIndex);costClass=costFor(groupIndex);version=1;
        }
        public boolean numeric(){return "DOUBLE".equals(valueType)||"INTEGER".equals(valueType)||"BOOLEAN".equals(valueType);}
        /** Only these atoms may enter a target issue's immutable pre-draw score program. */
        public boolean predrawComputable(){
            return numeric() && !"SETTLEMENT_EVIDENCE".equals(evidenceStage);
        }
        public boolean settlementEvidence(){return "SETTLEMENT_EVIDENCE".equals(evidenceStage);}
        /**
         * Only fully materialized PIT/action atoms may enter a newly created certifiable asset.
         *
         * <p>A10 runtime-health and A11 lifecycle/meta rows are retained for backward-compatible
         * observation and audit.  Their current shared action context cannot supply the per-process
         * and per-asset counters promised by their names, so treating their zero/proxy values as
         * production signal would be an empty semantic shell.  R922 therefore excludes them from
         * every new or mutated certifiable genome without rewriting historical frozen evidence.</p>
         */
        public boolean productionScoreEligible(){
            return predrawComputable() && !"A10".equals(group) && !"A11".equals(group);
        }
        public String semanticTier(){
            if(settlementEvidence())return TIER_SETTLEMENT_EVIDENCE;
            if(!numeric())return TIER_AUDIT_IDENTITY;
            if("A10".equals(group)||"A11".equals(group))return TIER_RESEARCH_PROXY;
            return TIER_PRODUCTION_SCORE;
        }
        public String canonical(){return id+'|'+group+'|'+name+'|'+valueType+'|'+valueRange
                +'|'+windowSpec+'|'+maxLag+'|'+minSupport+'|'+leakagePolicy+'|'+updateTiming
                +'|'+evidenceStage+'|'+costClass+'|'+version;}
    }
    public static final class FamilySpec {
        public final String id,nameZh;
        FamilySpec(int index,String name){id=String.format(java.util.Locale.US,"AF%02d",index);nameZh=name;}
        public String canonical(){return id+'|'+nameZh+'|'+NAMESPACE;}
    }

    private static final String[][] GROUPS = new String[][] {
        {
            "issue_id","source_issue_id","capture_ts","draw_deadline_ts","trained_until_issue",
            "history_contiguous_len","source_consistency","missing_mask","late_data_flag","data_age_ms",
            "source_latency_ms","schema_version","snapshot_hash","truth_source_id","window_legal_start",
            "duplicate_issue_guard","primary_backup_delta","clock_monotonicity","history_revision_id","data_quality_grade"
        },{
            "position_id","number_id","number_at_position","position_of_number","rank_coordinate",
            "inverse_rank_coordinate","position_distance","number_distance","cyclic_position_distance","relative_order",
            "set_membership","topk_membership","pair_identity","triple_identity","action_identity",
            "position_pair","number_pair","position_triple","number_triple","permutation_identity"
        },{
            "lag","age_since_event","time_between_events","run_length","return_time","first_passage_time",
            "event_count_window","event_rate","event_recency_weight","temporal_order","before_after_relation",
            "short_mid_gap","mid_long_gap","change_duration","stable_duration","recovery_duration",
            "decay_half_life","event_interarrival_cv","recurrence_index","temporal_support"
        },{
            "directed_edge","undirected_edge","edge_weight","edge_age","path_length","two_step_path",
            "three_step_path","cycle_membership","cycle_length","inflow","outflow","flow_balance","collision",
            "swap","mutual_exclusion","co_occurrence","conditional_support","graph_degree","local_cluster","community_membership"
        },{
            "difference","second_difference","ratio","safe_log","logit","sigmoid","clip","normalize",
            "zscore_robust","rank_transform","quantile_transform","ewma","decayed_sum","min_operator",
            "max_operator","product_interaction","signed_interaction","residual","orthogonal_residual","low_rank_projection"
        },{
            "count","frequency","joint_frequency","conditional_frequency","posterior_mean","posterior_variance",
            "entropy","conditional_entropy","cross_entropy","mutual_information","conditional_mutual_information",
            "js_divergence","concentration","effective_sample_size","support_floor","effect_size","effect_direction",
            "permutation_null_p","bootstrap_variance","information_gain"
        },{
            "local_memory_slot","global_memory_slot","cross_position_memory_slot","fast_trace","slow_trace",
            "prototype_slot","centroid_slot","reservoir_slot","regime_slot","error_trace","success_trace",
            "counterfactual_trace","state_age","state_confidence","state_decay","state_reset_gate",
            "state_write_gate","state_read_gate","state_version","checkpoint_pointer"
        },{
            "prediction_entropy","top_margin","support_uncertainty","neighbor_distance","ensemble_disagreement",
            "behavior_disagreement","ood_distance","calibration_residual","brier_component","wilson_lower",
            "wilson_upper","paired_delta_lower","drift_score","novelty_score","instability_score","max_miss_risk",
            "resource_risk","data_quality_risk","regime_confidence","hold_reason_code"
        },{
            "action_position","action_set3","member_score","weakest_member","third_fourth_margin","substitution_cost",
            "set_diversity","set_internal_dependence","joint_failure_risk","action_neighbor","action_neighbor_support",
            "action_rank","action_rank_stability","action_perturbation_sensitivity","action_probability",
            "action_calibrated_probability","action_hold_gate","action_regime_fit","action_behavior_cluster","action_counterfactual_delta"
        },{
            "future_oos_n","future_oos_hit","block_id","regime_id","paired_mainline_outcome","freeze_hash",
            "settlement_hash","behavior_hash","structural_hash","parent_id","generation","mutation_op",
            "creation_reason","prune_reason","certificate_id","certificate_status","evidence_version","tombstone_id",
            "multiple_test_family","audit_event_id"
        },{
            "predict_latency_ms","update_latency_ms","training_slice_ms","memory_peak_bytes","state_size_bytes",
            "db_read_count","db_write_count","artifact_load_ms","checkpoint_age","resume_count","yield_count",
            "oom_risk","queue_depth","battery_budget","foreground_background_state","scheduler_lag","io_contention",
            "cache_hit_rate","gc_pressure","runtime_health_grade"
        },{
            "asset_incremental_info","asset_redundancy","asset_downstream_delta","algorithm_complementarity",
            "paired_error_complementarity","behavior_novelty","niche_id","family_id","credit_assignment",
            "selection_pressure","mutation_success_rate","crossover_success_rate","rollback_count","regime_specialization",
            "coverage_specialization","stability_specialization","resource_efficiency","certificate_survival_age",
            "failure_mode_id","recovery_gain"
        }
    };

    private static final String[] FAMILY_NAMES = new String[] {
        "多尺度方向场","局部-全局残差趋势","拐点/变化点资产","回归/逃逸轨迹","位移惯性/反转资产","轨迹相似原型库",
        "时间尺度冲突面","恢复速度资产","置换环形态","迁移母题图","碰撞/排斥形态","Rank反转母题",
        "窗口分歧母题","边界替换母题","失败形态库","成功形态库","市场状态向量","区制概率面",
        "区制迁移资产","区制寿命资产","局部状态机","全局状态机","跨位耦合状态","OOD环境资产",
        "号码属性画像","位置属性画像","号码-位置兼容矩阵","跨位迁移流矩阵","位置关联图","号码关联图",
        "多位置交叉交集证据面","关联新增信息矩阵","Evidence Teacher Matrix","Student Error Matrix",
        "Forward Reasoning Matrix","Reverse Reasoning Matrix","Counterfactual Matrix","Feedback/反哺矩阵",
        "Contradiction Matrix","Teacher Diversity Matrix","Temporal Precedence Matrix","Invariance Matrix",
        "Mechanism Support Graph","CounterEvidence Graph","Mediation Candidate Map","Confound Risk Map",
        "Residual Mechanism Field","Mechanism Novelty Archive","Probability Surface","Calibrated Probability Surface",
        "Uncertainty Surface","Stability Surface","Risk Veto Surface","Evidence Density Surface","Drift Surface","MaxMiss Hazard",
        "1200动作几何图","K3集合内部结构","第三边界资产","动作扰动敏感度","动作邻域局部后验","选择性/HOLD机会资产",
        "主链残差修复资产","互补专家机会资产","Asset Utility Matrix","Asset Redundancy Graph",
        "Algorithm Complementarity Matrix","Evolution Credit Matrix","Resource Efficiency Matrix","Runtime Health Asset",
        "Evidence Integrity Asset","Rollback Decision Asset"
    };

    private static final List<AtomSpec> ATOMS;
    private static final List<FamilySpec> FAMILIES;
    static {
        ArrayList<AtomSpec> atoms=new ArrayList<AtomSpec>(EXPECTED_ATOMS);
        for(int g=0;g<GROUPS.length;g++)for(int i=0;i<GROUPS[g].length;i++){
            String group="A"+g;
            atoms.add(new AtomSpec(String.format(java.util.Locale.US,"%s-%02d",group,i+1),group,
                    GROUPS[g][i],typeFor(g,GROUPS[g][i]),g));
        }
        ATOMS=Collections.unmodifiableList(atoms);
        ArrayList<FamilySpec> families=new ArrayList<FamilySpec>(EXPECTED_FAMILIES);
        for(int i=0;i<FAMILY_NAMES.length;i++)families.add(new FamilySpec(i+1,FAMILY_NAMES[i]));
        FAMILIES=Collections.unmodifiableList(families);
        assertContract();
    }

    private GenesisSchemaRegistryR915() {}
    public static List<AtomSpec> atoms(){return ATOMS;}
    public static List<FamilySpec> families(){return FAMILIES;}
    public static int atomCount(){return ATOMS.size();}
    public static int familyCount(){return FAMILIES.size();}
    public static int predrawComputableCount(){int n=0;for(AtomSpec a:ATOMS)if(a.predrawComputable())n++;return n;}
    public static int productionScoreEligibleCount(){int n=0;for(AtomSpec a:ATOMS)if(a.productionScoreEligible())n++;return n;}
    public static int researchProxyCount(){int n=0;for(AtomSpec a:ATOMS)if(TIER_RESEARCH_PROXY.equals(a.semanticTier()))n++;return n;}
    public static int auditIdentityCount(){int n=0;for(AtomSpec a:ATOMS)if(TIER_AUDIT_IDENTITY.equals(a.semanticTier()))n++;return n;}
    public static int settlementEvidenceCount(){int n=0;for(AtomSpec a:ATOMS)if(a.settlementEvidence())n++;return n;}
    public static String fingerprint(){
        StringBuilder b=new StringBuilder(NAMESPACE);
        for(AtomSpec a:ATOMS)b.append('|').append(a.canonical());
        for(FamilySpec f:FAMILIES)b.append('|').append(f.canonical());
        return V51ChecksumEngine.sha256(b.toString());
    }
    public static boolean validateNumericComposition(String leftId,String rightId,String operator){
        AtomSpec left=find(leftId),right=find(rightId);
        // Numeric is not sufficient: A9 contains numeric settlement outcomes, hashes and
        // lifecycle counters that exist only after the target draw.  Letting a generic grammar
        // helper accept those atoms would reopen target leakage even though Genome.random()
        // currently filters them.  The boundary therefore belongs in the registry itself so all
        // present and future callers inherit the same pre-draw contract.
        if(left==null||right==null||!left.predrawComputable()||!right.predrawComputable())return false;
        return "ADD".equals(operator)||"SUB".equals(operator)||"MUL".equals(operator)
                ||"SAFE_DIV".equals(operator)||"MIN".equals(operator)||"MAX".equals(operator);
    }
    public static boolean validateNewAtomProposal(String name,String valueType,String leakage,String timing){
        String n=name==null?"":name.trim();
        if(!n.matches("[a-z][a-z0-9_]{2,63}"))return false;
        for(AtomSpec atom:ATOMS)if(atom.name.equals(n))return false;
        boolean type="DOUBLE".equals(valueType)||"INTEGER".equals(valueType)
                ||"BOOLEAN".equals(valueType)||"STRING".equals(valueType);
        return type&&"PREDRAW_ONLY".equals(leakage)&&"FREEZE_BEFORE_TARGET_DRAW".equals(timing);
    }
    public static void assertContract(){
        if(GROUPS.length!=12||ATOMS.size()!=EXPECTED_ATOMS||FAMILIES.size()!=EXPECTED_FAMILIES)
            throw new IllegalStateException("R915_GENESIS_CARDINALITY_INVALID");
        HashSet<String> ids=new HashSet<String>(),names=new HashSet<String>();
        for(AtomSpec a:ATOMS){
            if(!ids.add(a.id)||!names.add(a.name)||a.id.length()==0)
                throw new IllegalStateException("R915_GENESIS_ATOM_DUPLICATE");
            if(a.valueRange.length()==0||a.windowSpec.length()==0||a.leakagePolicy.length()==0
                    ||a.updateTiming.length()==0||a.evidenceStage.length()==0||a.costClass.length()==0)
                throw new IllegalStateException("R919_GENESIS_ATOM_CONTRACT_INCOMPLETE:"+a.id);
            if(a.settlementEvidence()&&a.predrawComputable())
                throw new IllegalStateException("R919_POSTDRAW_ATOM_LEAKAGE:"+a.id);
        }
        if(predrawComputableCount()!=205
                ||productionScoreEligibleCount()!=EXPECTED_PRODUCTION_SCORE_ATOMS
                ||researchProxyCount()!=EXPECTED_RESEARCH_PROXY_ATOMS
                ||auditIdentityCount()!=EXPECTED_AUDIT_IDENTITY_ATOMS
                ||settlementEvidenceCount()!=EXPECTED_SETTLEMENT_EVIDENCE_ATOMS)
            throw new IllegalStateException("R922_GENESIS_ATOM_STAGE_CARDINALITY");
        for(FamilySpec f:FAMILIES)if(!ids.add(f.id)||!names.add(f.nameZh))
            throw new IllegalStateException("R915_GENESIS_FAMILY_DUPLICATE");
    }
    private static AtomSpec find(String id){for(AtomSpec a:ATOMS)if(a.id.equals(id))return a;return null;}
    private static String typeFor(int group,String name){
        if(group==0&&(name.endsWith("_id")||name.endsWith("_hash")||name.endsWith("_version")
                ||name.endsWith("_grade")))return "STRING";
        if(group==1&&(name.endsWith("_identity")||name.endsWith("_pair")||name.endsWith("_triple")))return "STRING";
        if(group==9&&(name.endsWith("_id")||name.endsWith("_hash")||name.endsWith("_op")
                ||name.endsWith("_reason")||name.endsWith("_status")||name.endsWith("_family")))return "STRING";
        if(name.endsWith("_flag")||name.endsWith("_guard"))return "BOOLEAN";
        if(name.endsWith("_count")||name.endsWith("_len")||name.endsWith("_n")
                ||name.endsWith("_rank")||name.endsWith("_generation"))return "INTEGER";
        return "DOUBLE";
    }

    private static String rangeFor(String type){
        if("STRING".equals(type))return "TEXT_ENUM_OR_HASH";
        if("BOOLEAN".equals(type))return "[0,1]_BOOLEAN";
        if("INTEGER".equals(type))return "[0,+INF)_NORMALIZED_WHEN_SCORED";
        return "[0,1]_FINITE";
    }
    private static String windowFor(int group){
        switch(group){
            case 0:return "POINT_IN_TIME_SNAPSHOT";
            case 1:return "CURRENT_ACTION_COORDINATE";
            case 2:return "MULTISCALE_1_12_24_60_120_480";
            case 3:return "GRAPH_WINDOW_MAX_480";
            case 4:return "DERIVED_FROM_FROZEN_INPUTS";
            case 5:return "STAT_WINDOW_24_60_120_480";
            case 6:return "PERSISTENT_STATE_T_PLUS_1";
            case 7:return "PREDRAW_RISK_WINDOW_30_60_120";
            case 8:return "CURRENT_1200_ACTION_SPACE";
            case 9:return "FUTURE_OOS_SETTLEMENT_LEDGER";
            case 10:return "CURRENT_RUNTIME_SLICE";
            default:return "TRAINED_META_WINDOW_24_120_240";
        }
    }
    private static int maxLagFor(int group){
        if(group==0||group==1||group==8||group==10)return 0;
        if(group==6||group==9)return 1;
        return 480;
    }
    private static int minSupportFor(int group){
        if(group==2||group==3||group==5)return 24;
        if(group==7||group==11)return 12;
        return 1;
    }
    private static String stageFor(int group){
        if(group<=5)return "PREDRAW_HISTORY";
        if(group==6)return "TRAINED_STATE_PREDRAW_READ";
        if(group<=8)return "PREDRAW_ACTION";
        if(group==9)return "SETTLEMENT_EVIDENCE";
        if(group==10)return "PREDRAW_RUNTIME";
        return "TRAINED_META_PREDRAW_READ";
    }
    private static String leakageFor(int group){
        if(group==9)return "POSTDRAW_EVIDENCE_FOR_LIFECYCLE_ONLY";
        if(group==6||group==11)return "TRAINED_UNTIL_LT_TARGET";
        return "PREDRAW_ONLY";
    }
    private static String timingFor(int group){
        if(group==9)return "SETTLEMENT_EXACTLY_ONCE_EARLIEST_USE_T_PLUS_1";
        if(group==6||group==11)return "SETTLEMENT_UPDATE_PREDRAW_READ_ONLY";
        return "FREEZE_BEFORE_TARGET_DRAW";
    }
    private static String costFor(int group){
        return group==3||group==5||group==11?"MOBILE_MEDIUM_PRECOMPUTED":"MOBILE_LOW";
    }
}
