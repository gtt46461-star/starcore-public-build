-- adaptive_judge_log
CREATE TABLE adaptive_judge_log (_id INTEGER PRIMARY KEY AUTOINCREMENT,judge_key TEXT UNIQUE,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,base_issue TEXT,target_issue TEXT,position INTEGER DEFAULT 0,n1 INTEGER DEFAULT 0,n2 INTEGER DEFAULT 0,n3 INTEGER DEFAULT 0,k3_mask INTEGER DEFAULT 0,final_p REAL DEFAULT 0,dynamic_gate REAL DEFAULT 0,edge REAL DEFAULT 0,teacher_status TEXT,v20_status TEXT,window_state_key TEXT,judge_status TEXT,judge_score REAL DEFAULT 0,judge_risk REAL DEFAULT 0,pass INTEGER DEFAULT 0,downgrade_to_shadow INTEGER DEFAULT 0,freeze INTEGER DEFAULT 0,promote_shadow_candidate INTEGER DEFAULT 0,freeze_key TEXT,freeze_level TEXT,reason_text TEXT,module_advice TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT, record_key TEXT, source_type TEXT, module_name TEXT, status TEXT);

-- adaptive_judge_sample
CREATE TABLE adaptive_judge_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, judge_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, candidate_count INTEGER DEFAULT 0, cluster_id TEXT, cluster_status TEXT, status TEXT, action TEXT, domain TEXT, cluster_hit_rate REAL DEFAULT 0, cluster_recent_hit_rate REAL DEFAULT 0, cluster_sample INTEGER DEFAULT 0, cluster_confidence REAL DEFAULT 0, cause_score REAL DEFAULT 0, counter_score REAL DEFAULT 0, promote_score REAL DEFAULT 0, freeze_score REAL DEFAULT 0, reason TEXT, version TEXT, snapshot_key TEXT, base_issue TEXT, candidate_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT);

-- ai_judge_log
CREATE TABLE ai_judge_log (_id INTEGER PRIMARY KEY AUTOINCREMENT, judge_id TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, best_evidence_key TEXT, second_evidence_key TEXT, champion_gap REAL DEFAULT 0, judge_score REAL DEFAULT 0, judge_confidence REAL DEFAULT 0, conflict_level REAL DEFAULT 0, bet_advice TEXT, hold_reason TEXT, summary_text TEXT, created_at INTEGER DEFAULT 0);

-- android_metadata
CREATE TABLE android_metadata (locale TEXT);

-- app_state
CREATE TABLE app_state (state_key TEXT PRIMARY KEY, state_value TEXT, updated_at INTEGER DEFAULT 0);

-- atomic_tool_context_r943
CREATE TABLE atomic_tool_context_r943(target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,features BLOB NOT NULL,probabilities BLOB NOT NULL,network_probability REAL NOT NULL,ablation BLOB NOT NULL,network_hash TEXT NOT NULL,trained_until TEXT NOT NULL,record_hash TEXT NOT NULL,hit INTEGER NOT NULL DEFAULT -1,settled_at INTEGER NOT NULL DEFAULT 0);

-- atomic_tool_network_r943
CREATE TABLE atomic_tool_network_r943(owner TEXT PRIMARY KEY,artifact BLOB NOT NULL,artifact_hash TEXT NOT NULL,trained_until TEXT NOT NULL,updates INTEGER NOT NULL,loss REAL NOT NULL,gain_sum REAL NOT NULL,ablation_gain BLOB NOT NULL);

-- atomic_tool_quality_r943
CREATE TABLE atomic_tool_quality_r943(atom_index INTEGER PRIMARY KEY,atom_id TEXT NOT NULL,n INTEGER NOT NULL,missing INTEGER NOT NULL,counts BLOB NOT NULL,gain_sum REAL NOT NULL,gain_sq REAL NOT NULL,block_n INTEGER NOT NULL,block_gain REAL NOT NULL,positive_blocks INTEGER NOT NULL,blocks INTEGER NOT NULL,state TEXT NOT NULL,last_issue TEXT NOT NULL);

-- auto_learn_cursor
CREATE TABLE auto_learn_cursor (_id INTEGER PRIMARY KEY AUTOINCREMENT, task_key TEXT NOT NULL UNIQUE, task_type TEXT, window_size INTEGER DEFAULT 0, segment_start_index INTEGER DEFAULT 0, segment_end_index INTEGER DEFAULT 0, next_target_index INTEGER DEFAULT 0, last_start_index INTEGER DEFAULT 0, random_seed INTEGER DEFAULT 0, last_issue TEXT, progress TEXT, status TEXT, used_segments TEXT, updated_at INTEGER DEFAULT 0);

-- b03_action_event
CREATE TABLE b03_action_event(_id INTEGER PRIMARY KEY AUTOINCREMENT,bundle_id TEXT NOT NULL,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT NULL DEFAULT 0,raw_action_p REAL NOT NULL,rank_score REAL NOT NULL,rank_no INTEGER NOT NULL,action_hash TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,UNIQUE(bundle_id,position),FOREIGN KEY(bundle_id) REFERENCES b03_prediction_bundle(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'),CHECK(position BETWEEN 1 AND 10));

-- b03_authorization_event
CREATE TABLE b03_authorization_event(decision_id TEXT PRIMARY KEY,bundle_id TEXT NOT NULL,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,artifact_bundle_hash TEXT NOT NULL,cohort_id TEXT NOT NULL,authorization_hash TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,FOREIGN KEY(bundle_id) REFERENCES b03_prediction_bundle(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'));

-- b03_draw_quarantine
CREATE TABLE b03_draw_quarantine(_id INTEGER PRIMARY KEY AUTOINCREMENT,source_database TEXT NOT NULL,source_game_code TEXT NOT NULL,source_table TEXT NOT NULL,source_issue TEXT NOT NULL,source_numbers TEXT NOT NULL,normalized_numbers TEXT NOT NULL DEFAULT '',draw_time INTEGER NOT NULL DEFAULT 0,quarantine_reason TEXT NOT NULL,review_status TEXT NOT NULL,row_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL);

-- b03_integrity_snapshot
CREATE TABLE b03_integrity_snapshot(_id INTEGER PRIMARY KEY AUTOINCREMENT,snapshot_hash TEXT NOT NULL UNIQUE,schema_hash TEXT NOT NULL,replay_hash TEXT NOT NULL,valid INTEGER NOT NULL,detail TEXT NOT NULL,created_at INTEGER NOT NULL);

-- b03_migration_journal
CREATE TABLE b03_migration_journal(migration_id TEXT PRIMARY KEY,source_database TEXT NOT NULL,source_game_code TEXT NOT NULL,policy TEXT NOT NULL,cursor_issue TEXT NOT NULL DEFAULT '',scanned_count INTEGER NOT NULL DEFAULT 0,imported_count INTEGER NOT NULL DEFAULT 0,replay_hash TEXT NOT NULL,status TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- b03_number_event
CREATE TABLE b03_number_event(_id INTEGER PRIMARY KEY AUTOINCREMENT,bundle_id TEXT NOT NULL,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,position INTEGER NOT NULL,number INTEGER NOT NULL,raw_score REAL NOT NULL,number_p REAL NOT NULL,number_rank INTEGER NOT NULL,state_id TEXT NOT NULL,domain_id TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,UNIQUE(bundle_id,position,number),FOREIGN KEY(bundle_id) REFERENCES b03_prediction_bundle(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'),CHECK(position BETWEEN 1 AND 10),CHECK(number BETWEEN 1 AND 10));

-- b03_permutation_event
CREATE TABLE b03_permutation_event(bundle_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,matrix_blob TEXT NOT NULL,row_error REAL NOT NULL,column_error REAL NOT NULL,iterations INTEGER NOT NULL,converged INTEGER NOT NULL,used_fallback INTEGER NOT NULL,map_blob TEXT NOT NULL,matrix_hash TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,FOREIGN KEY(bundle_id) REFERENCES b03_prediction_bundle(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'));

-- b03_prediction_bundle
CREATE TABLE b03_prediction_bundle(bundle_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,base_issue TEXT NOT NULL,history_hash TEXT NOT NULL,feature_hash TEXT NOT NULL,state_hash TEXT NOT NULL,expert_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,contract_hash TEXT NOT NULL,artifact_bundle_hash TEXT NOT NULL,cohort_id TEXT NOT NULL,status TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,UNIQUE(game_code,target_issue),CHECK(game_code='Binance1-PK10'));

-- b03_replay_audit
CREATE TABLE b03_replay_audit(_id INTEGER PRIMARY KEY AUTOINCREMENT,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,replay_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,contract_hash TEXT NOT NULL,status TEXT NOT NULL,detail TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(game_code,target_issue,replay_hash),CHECK(game_code='Binance1-PK10'));

-- b03_schema_meta
CREATE TABLE b03_schema_meta(meta_key TEXT PRIMARY KEY,meta_value TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- b03_selected_event
CREATE TABLE b03_selected_event(bundle_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,decision_id TEXT NOT NULL UNIQUE,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_hit_p REAL NOT NULL,predictability REAL NOT NULL,lane TEXT NOT NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,authorization_hash TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,FOREIGN KEY(bundle_id) REFERENCES b03_prediction_bundle(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'),CHECK(decision_action IN ('BET','SHADOW','HOLD')));

-- b03_settlement_event
CREATE TABLE b03_settlement_event(_id INTEGER PRIMARY KEY AUTOINCREMENT,bundle_id TEXT NOT NULL,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,actual_permutation TEXT NOT NULL,result TEXT NOT NULL,settlement_hash TEXT NOT NULL UNIQUE,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,UNIQUE(game_code,target_issue),FOREIGN KEY(bundle_id) REFERENCES b03_prediction_bundle(bundle_id) ON DELETE RESTRICT,CHECK(game_code='Binance1-PK10'));

-- b03_transaction_audit
CREATE TABLE b03_transaction_audit(_id INTEGER PRIMARY KEY AUTOINCREMENT,idempotency_key TEXT NOT NULL UNIQUE,operation TEXT NOT NULL,game_code TEXT NOT NULL,target_issue TEXT NOT NULL DEFAULT '',transaction_hash TEXT NOT NULL,status TEXT NOT NULL,error_detail TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,CHECK(game_code='Binance1-PK10'));

-- backtest_report
CREATE TABLE backtest_report (_id INTEGER PRIMARY KEY AUTOINCREMENT, ok INTEGER DEFAULT 0, mode TEXT, type TEXT, window_no INTEGER DEFAULT 0, window INTEGER DEFAULT 0, range_start TEXT, range_end TEXT, start_issue TEXT, end_issue TEXT, train_count INTEGER DEFAULT 0, test_count INTEGER DEFAULT 0, total_count INTEGER DEFAULT 0, bet_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hold_count INTEGER DEFAULT 0, shadow_count INTEGER DEFAULT 0, candidate_count INTEGER DEFAULT 0, enrich_count INTEGER DEFAULT 0, deep_count INTEGER DEFAULT 0, learning_case_count INTEGER DEFAULT 0, teacher_matrix_sample_count INTEGER DEFAULT 0, teacher_matrix_profile_count INTEGER DEFAULT 0, teacher_matrix_positive_count INTEGER DEFAULT 0, teacher_matrix_negative_count INTEGER DEFAULT 0, reason_sample_count INTEGER DEFAULT 0, reason_profile_count INTEGER DEFAULT 0, elite45_pass_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, win_rate REAL DEFAULT 0, frequency REAL DEFAULT 0, pnl REAL DEFAULT 0, max_drawdown REAL DEFAULT 0, longest_lose INTEGER DEFAULT 0, avg_raw_p REAL DEFAULT 0, avg_final_p REAL DEFAULT 0, avg_gate REAL DEFAULT 0, avg_edge REAL DEFAULT 0, calibration_error REAL DEFAULT 0, manual_triggered INTEGER DEFAULT 0, profile_count INTEGER DEFAULT 0, attribution_count INTEGER DEFAULT 0, promoted_count INTEGER DEFAULT 0, rejected_count INTEGER DEFAULT 0, a_plus_count INTEGER DEFAULT 0, progress INTEGER DEFAULT 0, a_plus_hit_rate REAL DEFAULT 0, bias_score_avg REAL DEFAULT 0, brier_score REAL DEFAULT 0, ai_judge_avg_score REAL DEFAULT 0, deep_score_avg REAL DEFAULT 0, teacher_matrix_avg_hit_rate REAL DEFAULT 0, teacher_matrix_avg_confidence REAL DEFAULT 0, reason_avg_edge REAL DEFAULT 0, reason_avg_hit_similarity REAL DEFAULT 0, reason_avg_miss_similarity REAL DEFAULT 0, bias_summary TEXT, audit_summary TEXT, promotion_summary TEXT, a_plus_summary TEXT, learning_impact_summary TEXT, teacher_matrix_summary TEXT, reason_summary TEXT, elite45_summary TEXT, top_profiles TEXT, top_miss_reasons TEXT, top_hit_reasons TEXT, status TEXT, summary TEXT, started_at INTEGER DEFAULT 0, finished_at INTEGER DEFAULT 0, cost_ms INTEGER DEFAULT 0, created_at INTEGER DEFAULT 0, v20_s_count INTEGER DEFAULT 0, v20_a_count INTEGER DEFAULT 0, v20_b_count INTEGER DEFAULT 0, v20_c_count INTEGER DEFAULT 0, v20_b_promoted_count INTEGER DEFAULT 0, v20_false_strong_block_count INTEGER DEFAULT 0, v20_shadow_health_pass_count INTEGER DEFAULT 0, v20_max_same_class_lose INTEGER DEFAULT 0, v20_default_mode_hit_rate REAL DEFAULT 0, v20_expansion_mode_hit_rate REAL DEFAULT 0, v20_reason_summary TEXT);

-- bias_evidence
CREATE TABLE bias_evidence (_id INTEGER PRIMARY KEY AUTOINCREMENT, evidence_id TEXT NOT NULL UNIQUE, issue TEXT, base_issue TEXT, target_issue TEXT, bias_score REAL DEFAULT 0, position_bias_score REAL DEFAULT 0, number_bias_score REAL DEFAULT 0, transition_bias_score REAL DEFAULT 0, cycle_bias_score REAL DEFAULT 0, combo_bias_score REAL DEFAULT 0, state_bias_score REAL DEFAULT 0, sample_count INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, confidence REAL DEFAULT 0, status TEXT, summary_text TEXT, created_at INTEGER DEFAULT 0);

-- break_memory
CREATE TABLE break_memory (_id INTEGER PRIMARY KEY AUTOINCREMENT, break_key TEXT NOT NULL, position INTEGER DEFAULT 0, break_depth_bucket INTEGER DEFAULT 0, drift_bucket INTEGER DEFAULT 0, markov_bucket INTEGER DEFAULT 0, state_mode_before TEXT, break_type TEXT, trend_life_bucket INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, recover1_count INTEGER DEFAULT 0, recover2_count INTEGER DEFAULT 0, recover3_count INTEGER DEFAULT 0, fail_count INTEGER DEFAULT 0, reverse_count INTEGER DEFAULT 0, recover_rate REAL DEFAULT 0, avg_recover_steps REAL DEFAULT 0, last240_recover_rate REAL DEFAULT 0, last1000_recover_rate REAL DEFAULT 0, updated_at INTEGER DEFAULT 0, UNIQUE(break_key, position, break_depth_bucket, drift_bucket, markov_bucket));

-- calibration_bucket
CREATE TABLE calibration_bucket (_id INTEGER PRIMARY KEY AUTOINCREMENT, bucket_key TEXT NOT NULL UNIQUE, raw_min REAL DEFAULT 0, raw_max REAL DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, real_hit_rate REAL DEFAULT 0, last240_sample INTEGER DEFAULT 0, last240_hit INTEGER DEFAULT 0, last240_hit_rate REAL DEFAULT 0, last1000_sample INTEGER DEFAULT 0, last1000_hit INTEGER DEFAULT 0, last1000_hit_rate REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- candidate_audit
CREATE TABLE candidate_audit (_id INTEGER PRIMARY KEY AUTOINCREMENT, audit_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, candidate_key TEXT, rank_no INTEGER DEFAULT 0, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, grade TEXT, action TEXT, hit INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, actual_rank INTEGER DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, final_cause_score REAL DEFAULT 0, why_hit TEXT, why_miss TEXT, why_hold TEXT, dynamic_cluster_id TEXT, dynamic_cluster_status TEXT, dynamic_cluster_prototype_key TEXT, dynamic_cluster_feature_key TEXT, dynamic_cluster_sample INTEGER DEFAULT 0, dynamic_cluster_hit_count INTEGER DEFAULT 0, dynamic_cluster_miss_count INTEGER DEFAULT 0, dynamic_cluster_recent_sample INTEGER DEFAULT 0, dynamic_cluster_recent_hit_count INTEGER DEFAULT 0, dynamic_cluster_hit_rate REAL DEFAULT 0, dynamic_cluster_recent_hit_rate REAL DEFAULT 0, dynamic_cluster_confidence REAL DEFAULT 0, dynamic_cluster_distance REAL DEFAULT 0, dynamic_cluster_volatility REAL DEFAULT 0, dynamic_cluster_max_lose INTEGER DEFAULT 0, dynamic_cluster_counter_score REAL DEFAULT 0, dynamic_cluster_version TEXT, dynamic_cluster_reason TEXT, position_need_key TEXT, position_need_summary TEXT, position_need_strength REAL DEFAULT 0, position_need_chaos_risk REAL DEFAULT 0, position_need_repair_score REAL DEFAULT 0, position_need_relay_score REAL DEFAULT 0, number_supply_key TEXT, number_supply_summary TEXT, number_supply_strength REAL DEFAULT 0, number_supply_risk REAL DEFAULT 0, need_supply_match_key TEXT, need_supply_match_score REAL DEFAULT 0, need_supply_third_quality REAL DEFAULT 0, need_supply_role_diversity REAL DEFAULT 0, need_supply_risk REAL DEFAULT 0, need_supply_summary TEXT, k3_role_main TEXT, k3_role_assist TEXT, k3_role_defense TEXT, k3_role_main_number INTEGER DEFAULT 0, k3_role_assist_number INTEGER DEFAULT 0, k3_role_defense_number INTEGER DEFAULT 0, k3_role_structure TEXT, counterfactual_reason_key TEXT, counterfactual_rank INTEGER DEFAULT 0, counterfactual_rank_score REAL DEFAULT 0, counterfactual_why_not_risk REAL DEFAULT 0, counterfactual_why_not TEXT, counterfactual_summary TEXT, cause_ablation_key TEXT, cause_ablation_score REAL DEFAULT 0, cause_ablation_false_cause_count INTEGER DEFAULT 0, cause_ablation_summary TEXT, miss_reason_type TEXT, miss_reason_matrix_key TEXT, miss_reason_detail TEXT, adaptive_judge_candidate_count INTEGER DEFAULT 0, adaptive_judge_status TEXT, adaptive_judge_action TEXT, adaptive_judge_domain TEXT, adaptive_cluster_hit_rate REAL DEFAULT 0, adaptive_cluster_recent_hit_rate REAL DEFAULT 0, adaptive_cluster_sample INTEGER DEFAULT 0, adaptive_cluster_confidence REAL DEFAULT 0, adaptive_cause_score REAL DEFAULT 0, adaptive_counter_score REAL DEFAULT 0, adaptive_promote_score REAL DEFAULT 0, adaptive_freeze_score REAL DEFAULT 0, adaptive_judge_reason TEXT, adaptive_judge_version TEXT, promotion_cluster_status TEXT, promotion_cluster_hit_rate REAL DEFAULT 0, promotion_cluster_recent_hit_rate REAL DEFAULT 0, promotion_cluster_sample INTEGER DEFAULT 0, promotion_cluster_reason TEXT, why_bet TEXT, why_no_bet TEXT, dynamic_causal_summary TEXT, snapshot_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- candidate_evidence
CREATE TABLE candidate_evidence (_id INTEGER PRIMARY KEY AUTOINCREMENT, evidence_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, position_evidence REAL DEFAULT 0, number_evidence REAL DEFAULT 0, cycle_evidence REAL DEFAULT 0, transition_evidence REAL DEFAULT 0, pattern_evidence REAL DEFAULT 0, combo_evidence REAL DEFAULT 0, profile_evidence REAL DEFAULT 0, runtime_profile_evidence REAL DEFAULT 0, state_number_evidence REAL DEFAULT 0, teacher_matrix_evidence REAL DEFAULT 0, reason_evidence REAL DEFAULT 0, audit_evidence REAL DEFAULT 0, risk_score REAL DEFAULT 0, evidence_score REAL DEFAULT 0, confidence REAL DEFAULT 0, support_text TEXT, risk_text TEXT, summary_text TEXT, created_at INTEGER DEFAULT 0);

-- causal_factor_matrix
CREATE TABLE causal_factor_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, causal_key TEXT NOT NULL UNIQUE, state_key TEXT, reason_atom TEXT, reason_combo_key TEXT, sample_count INTEGER DEFAULT 0, with_cause_sample INTEGER DEFAULT 0, with_cause_hit INTEGER DEFAULT 0, without_cause_sample INTEGER DEFAULT 0, without_cause_hit INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, contrast_hit_rate REAL DEFAULT 0, causal_edge REAL DEFAULT 0, lcb REAL DEFAULT 0, recent30_hit_rate REAL DEFAULT 0, recent60_hit_rate REAL DEFAULT 0, decay_risk REAL DEFAULT 0, valid_level TEXT, updated_at INTEGER DEFAULT 0, summary_text TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- causal_research_feedback_r947
CREATE TABLE causal_research_feedback_r947(lane TEXT PRIMARY KEY,module TEXT NOT NULL,support INTEGER NOT NULL,score REAL NOT NULL,through_issue TEXT NOT NULL,evidence_hash TEXT NOT NULL,reason TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- cause_ablation_sample
CREATE TABLE cause_ablation_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, ablation_key TEXT NOT NULL UNIQUE, cluster_id TEXT, created_at INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, omission_drop REAL DEFAULT 0, tail_drop REAL DEFAULT 0, neighbor_drop REAL DEFAULT 0, cross_position_drop REAL DEFAULT 0, need_supply_drop REAL DEFAULT 0, third_quality_drop REAL DEFAULT 0, total_ablation_score REAL DEFAULT 0, false_cause_count INTEGER DEFAULT 0, false_cause_summary TEXT, summary TEXT);

-- combo_memory
CREATE TABLE combo_memory (_id INTEGER PRIMARY KEY AUTOINCREMENT, shape_key TEXT NOT NULL, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, last240_sample INTEGER DEFAULT 0, last240_hit INTEGER DEFAULT 0, last240_hit_rate REAL DEFAULT 0, last1000_sample INTEGER DEFAULT 0, last1000_hit INTEGER DEFAULT 0, last1000_hit_rate REAL DEFAULT 0, avg_combo_gap REAL DEFAULT 0, avg_trend_life REAL DEFAULT 0, active_flag INTEGER DEFAULT 1, updated_at INTEGER DEFAULT 0, UNIQUE(shape_key, position, k3_mask));

-- combo_role_profile
CREATE TABLE combo_role_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, combo_role_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, main_number INTEGER DEFAULT 0, support_number INTEGER DEFAULT 0, defense_number INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, main_hit_count INTEGER DEFAULT 0, support_hit_count INTEGER DEFAULT 0, defense_hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, main_hit_rate REAL DEFAULT 0, support_hit_rate REAL DEFAULT 0, defense_hit_rate REAL DEFAULT 0, role_diversity REAL DEFAULT 0, combo_conflict REAL DEFAULT 0, role_score REAL DEFAULT 0, confidence REAL DEFAULT 0, status TEXT, summary_text TEXT, updated_at INTEGER DEFAULT 0);

-- counterfactual_rank
CREATE TABLE counterfactual_rank (_id INTEGER PRIMARY KEY AUTOINCREMENT, rank_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, state_key TEXT, rank_no INTEGER DEFAULT 0, rank_group TEXT, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, rank_counter_edge REAL DEFAULT 0, false_strong_risk REAL DEFAULT 0, rank4to6_overtake_risk REAL DEFAULT 0, rank_pass INTEGER DEFAULT 0, truth_mode TEXT, summary_text TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT, updated_at INTEGER DEFAULT 0);

-- counterfactual_reason_sample
CREATE TABLE counterfactual_reason_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, reason_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, cluster_id TEXT, position_rank INTEGER DEFAULT 0, k3_rank INTEGER DEFAULT 0, rank_score REAL DEFAULT 0, why_not_risk REAL DEFAULT 0, mirror_miss_similarity REAL DEFAULT 0, cause_score REAL DEFAULT 0, counter_score REAL DEFAULT 0, would_hit INTEGER DEFAULT 0, why_this_position TEXT, why_these_numbers TEXT, why_not_other_position TEXT, why_not_other_numbers TEXT, summary TEXT, state_key TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, sample_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT, updated_at INTEGER DEFAULT 0);

-- cp12_action_teacher_witness_no_delete
CREATE TRIGGER cp12_action_teacher_witness_no_delete BEFORE DELETE ON research_action_teacher_witness_cp12 BEGIN SELECT RAISE(ABORT,'ACTION_TEACHER_WITNESS_PERMANENT'); END;

-- cp12_action_teacher_witness_no_replace
CREATE TRIGGER cp12_action_teacher_witness_no_replace BEFORE INSERT ON research_action_teacher_witness_cp12 WHEN EXISTS(SELECT 1 FROM research_action_teacher_witness_cp12 WHERE action_id=NEW.action_id AND teacher_freeze_hash=NEW.teacher_freeze_hash) BEGIN SELECT RAISE(ABORT,'ACTION_TEACHER_WITNESS_REPLACE_FORBIDDEN'); END;

-- cp12_action_teacher_witness_no_update
CREATE TRIGGER cp12_action_teacher_witness_no_update BEFORE UPDATE ON research_action_teacher_witness_cp12 BEGIN SELECT RAISE(ABORT,'ACTION_TEACHER_WITNESS_IMMUTABLE'); END;

-- cp12_action_teachers_no_delete
CREATE TRIGGER cp12_action_teachers_no_delete BEFORE DELETE ON research_action_teacher_binding_cp12 BEGIN SELECT RAISE(ABORT,'ACTION_TEACHERS_PERMANENT'); END;

-- cp12_action_teachers_no_replace
CREATE TRIGGER cp12_action_teachers_no_replace BEFORE INSERT ON research_action_teacher_binding_cp12 WHEN EXISTS(SELECT 1 FROM research_action_teacher_binding_cp12 WHERE action_id=NEW.action_id) BEGIN SELECT RAISE(ABORT,'ACTION_TEACHERS_REPLACE_FORBIDDEN'); END;

-- cp12_action_teachers_no_update
CREATE TRIGGER cp12_action_teachers_no_update BEFORE UPDATE ON research_action_teacher_binding_cp12 BEGIN SELECT RAISE(ABORT,'ACTION_TEACHERS_IMMUTABLE'); END;

-- cp12_archive_guard_adaptive_judge_log
CREATE TRIGGER cp12_archive_guard_adaptive_judge_log BEFORE DELETE ON "adaptive_judge_log" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='adaptive_judge_log' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='adaptive_judge_log' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_diagnosis_case_log
CREATE TRIGGER cp12_archive_guard_diagnosis_case_log BEFORE DELETE ON "diagnosis_case_log" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='diagnosis_case_log' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='diagnosis_case_log' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_diagnosis_fix_plan
CREATE TRIGGER cp12_archive_guard_diagnosis_fix_plan BEFORE DELETE ON "diagnosis_fix_plan" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='diagnosis_fix_plan' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='diagnosis_fix_plan' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_dynamic_freeze_matrix
CREATE TRIGGER cp12_archive_guard_dynamic_freeze_matrix BEFORE DELETE ON "dynamic_freeze_matrix" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='dynamic_freeze_matrix' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='dynamic_freeze_matrix' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_module_effect_cache
CREATE TRIGGER cp12_archive_guard_module_effect_cache BEFORE DELETE ON "module_effect_cache" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='module_effect_cache' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='module_effect_cache' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_research_formal_observation_cp12
CREATE TRIGGER cp12_archive_guard_research_formal_observation_cp12 BEFORE DELETE ON "research_formal_observation_cp12" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_formal_observation_cp12' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_formal_observation_cp12' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_research_matrix_input_cp12
CREATE TRIGGER cp12_archive_guard_research_matrix_input_cp12 BEFORE DELETE ON "research_matrix_input_cp12" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_matrix_input_cp12' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_matrix_input_cp12' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_research_matrix_regime5_cp12
CREATE TRIGGER cp12_archive_guard_research_matrix_regime5_cp12 BEFORE DELETE ON "research_matrix_regime5_cp12" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_matrix_regime5_cp12' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_matrix_regime5_cp12' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_research_training_feature_cursor_cp12
CREATE TRIGGER cp12_archive_guard_research_training_feature_cursor_cp12 BEFORE DELETE ON "research_training_feature_cursor_cp12" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_training_feature_cursor_cp12' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_training_feature_cursor_cp12' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_research_training_feature_row_cp11
CREATE TRIGGER cp12_archive_guard_research_training_feature_row_cp11 BEFORE DELETE ON "research_training_feature_row_cp11" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_training_feature_row_cp11' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_training_feature_row_cp11' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_research_training_registration_row_cp12
CREATE TRIGGER cp12_archive_guard_research_training_registration_row_cp12 BEFORE DELETE ON "research_training_registration_row_cp12" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_training_registration_row_cp12' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_training_registration_row_cp12' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_archive_guard_shadow_promotion_cache
CREATE TRIGGER cp12_archive_guard_shadow_promotion_cache BEFORE DELETE ON "shadow_promotion_cache" WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='shadow_promotion_cache' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='shadow_promotion_cache' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- cp12_artifact_identity_immutable
CREATE TRIGGER cp12_artifact_identity_immutable BEFORE UPDATE ON research_artifact_identity_v2 BEGIN SELECT RAISE(ABORT,'CP12_ARTIFACT_IDENTITY_IMMUTABLE'); END;

-- cp12_artifact_identity_no_delete
CREATE TRIGGER cp12_artifact_identity_no_delete BEFORE DELETE ON research_artifact_identity_v2 BEGIN SELECT RAISE(ABORT,'CP12_ARTIFACT_IDENTITY_PERMANENT'); END;

-- cp12_artifact_identity_no_replace
CREATE TRIGGER cp12_artifact_identity_no_replace BEFORE INSERT ON research_artifact_identity_v2 WHEN EXISTS(SELECT 1 FROM research_artifact_identity_v2 WHERE artifact_hash=NEW.artifact_hash OR delivery_hash=NEW.delivery_hash) BEGIN SELECT RAISE(ABORT,'CP12_ARTIFACT_IDENTITY_DUPLICATE'); END;

-- cp12_atom_catalog_identity
CREATE TRIGGER cp12_atom_catalog_identity BEFORE UPDATE ON research_atom_catalog_import_cp12
 WHEN NEW.asset_sha256<>OLD.asset_sha256 OR NEW.asset_name<>OLD.asset_name OR NEW.expected_rows<>OLD.expected_rows OR NEW.expected_bytes<>OLD.expected_bytes
 BEGIN SELECT RAISE(ABORT,'ATOM_CATALOG_IDENTITY_IMMUTABLE'); END;

-- cp12_attachment_active_delete
CREATE TRIGGER cp12_attachment_active_delete BEFORE DELETE ON research_training_attachment_cp12
 WHEN EXISTS(SELECT 1 FROM research_training_protocol_attachment_cp12 p JOIN research_training_protocol_v2 t ON t.protocol_hash=p.protocol_hash JOIN v57_research_task_r967 q ON q.candidate_id=t.candidate_id WHERE p.attachment_hash=OLD.attachment_hash AND q.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))
 BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_ATTACHMENT_PROTECTED'); END;

-- cp12_attachment_no_replace
CREATE TRIGGER cp12_attachment_no_replace BEFORE INSERT ON research_training_attachment_cp12
 WHEN EXISTS(SELECT 1 FROM research_training_attachment_cp12 WHERE attachment_hash=NEW.attachment_hash OR relative_path=NEW.relative_path)
 BEGIN SELECT RAISE(ABORT,'CP12_ATTACHMENT_REPLACE_FORBIDDEN'); END;

-- cp12_attachment_no_update
CREATE TRIGGER cp12_attachment_no_update BEFORE UPDATE ON research_training_attachment_cp12
 BEGIN SELECT RAISE(ABORT,'CP12_ATTACHMENT_IMMUTABLE'); END;

-- cp12_cal_quality_immutable
CREATE TRIGGER cp12_cal_quality_immutable BEFORE UPDATE ON research_calibration_quality_cp12 BEGIN SELECT RAISE(ABORT,'CALIBRATION_QUALITY_IMMUTABLE'); END;

-- cp12_cal_quality_no_delete
CREATE TRIGGER cp12_cal_quality_no_delete BEFORE DELETE ON research_calibration_quality_cp12 BEGIN SELECT RAISE(ABORT,'CALIBRATION_QUALITY_PERMANENT'); END;

-- cp12_cal_quality_no_replace
CREATE TRIGGER cp12_cal_quality_no_replace BEFORE INSERT ON research_calibration_quality_cp12 WHEN EXISTS(SELECT 1 FROM research_calibration_quality_cp12 q WHERE q.study_hash=NEW.study_hash) BEGIN SELECT RAISE(ABORT,'CALIBRATION_QUALITY_REPLACE_FORBIDDEN'); END;

-- cp12_cal_quality_state
CREATE INDEX cp12_cal_quality_state ON research_calibration_quality_cp12(state,study_hash);

-- cp12_candidate_semantic_identity
CREATE UNIQUE INDEX cp12_candidate_semantic_identity ON r952_candidate(semantic_hash) WHERE semantic_hash<>'';

-- cp12_candidate_semantic_immutable
CREATE TRIGGER cp12_candidate_semantic_immutable BEFORE UPDATE OF semantic_hash ON r952_candidate BEGIN SELECT RAISE(ABORT,'SEMANTIC_GENOME_IMMUTABLE'); END;

-- cp12_canonical_reason_immutable
CREATE TRIGGER cp12_canonical_reason_immutable BEFORE UPDATE OF canonical_reason_json,canonical_reason_hash ON v57_shadow_observation_r35 BEGIN SELECT RAISE(ABORT,'CANONICAL_REASON_IMMUTABLE'); END;

-- cp12_child_teachers_no_delete
CREATE TRIGGER cp12_child_teachers_no_delete BEFORE DELETE ON research_child_teacher_lineage_cp12 BEGIN SELECT RAISE(ABORT,'CHILD_TEACHERS_PERMANENT'); END;

-- cp12_child_teachers_no_replace
CREATE TRIGGER cp12_child_teachers_no_replace BEFORE INSERT ON research_child_teacher_lineage_cp12 WHEN EXISTS(SELECT 1 FROM research_child_teacher_lineage_cp12 WHERE child_id=NEW.child_id AND teacher_freeze_hash=NEW.teacher_freeze_hash) BEGIN SELECT RAISE(ABORT,'CHILD_TEACHERS_REPLACE_FORBIDDEN'); END;

-- cp12_child_teachers_no_update
CREATE TRIGGER cp12_child_teachers_no_update BEFORE UPDATE ON research_child_teacher_lineage_cp12 BEGIN SELECT RAISE(ABORT,'CHILD_TEACHERS_IMMUTABLE'); END;

-- cp12_domain_cap_break_memory
CREATE TRIGGER cp12_domain_cap_break_memory BEFORE INSERT ON "break_memory" WHEN NEW."break_key" IS NOT NULL AND NEW."position" IS NOT NULL AND NEW."break_depth_bucket" IS NOT NULL AND NEW."drift_bucket" IS NOT NULL AND NEW."markov_bucket" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "break_memory" old WHERE old."break_key"=NEW."break_key" AND old."position"=NEW."position" AND old."break_depth_bucket"=NEW."break_depth_bucket" AND old."drift_bucket"=NEW."drift_bucket" AND old."markov_bucket"=NEW."markov_bucket") AND (SELECT COUNT(*) FROM "break_memory")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_calibration_bucket
CREATE TRIGGER cp12_domain_cap_calibration_bucket BEFORE INSERT ON "calibration_bucket" WHEN NEW."bucket_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "calibration_bucket" old WHERE old."bucket_key"=NEW."bucket_key") AND (SELECT COUNT(*) FROM "calibration_bucket")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_causal_factor_matrix
CREATE TRIGGER cp12_domain_cap_causal_factor_matrix BEFORE INSERT ON "causal_factor_matrix" WHEN NEW."causal_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "causal_factor_matrix" old WHERE old."causal_key"=NEW."causal_key") AND (SELECT COUNT(*) FROM "causal_factor_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_combo_memory
CREATE TRIGGER cp12_domain_cap_combo_memory BEFORE INSERT ON "combo_memory" WHEN NEW."shape_key" IS NOT NULL AND NEW."position" IS NOT NULL AND NEW."k3_mask" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "combo_memory" old WHERE old."shape_key"=NEW."shape_key" AND old."position"=NEW."position" AND old."k3_mask"=NEW."k3_mask") AND (SELECT COUNT(*) FROM "combo_memory")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_combo_role_profile
CREATE TRIGGER cp12_domain_cap_combo_role_profile BEFORE INSERT ON "combo_role_profile" WHEN NEW."combo_role_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "combo_role_profile" old WHERE old."combo_role_key"=NEW."combo_role_key") AND (SELECT COUNT(*) FROM "combo_role_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_deep_cycle
CREATE TRIGGER cp12_domain_cap_deep_cycle BEFORE INSERT ON "deep_cycle" WHEN NEW."cycle_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "deep_cycle" old WHERE old."cycle_key"=NEW."cycle_key") AND (SELECT COUNT(*) FROM "deep_cycle")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_deep_profile
CREATE TRIGGER cp12_domain_cap_deep_profile BEFORE INSERT ON "deep_profile" WHEN NEW."profile_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "deep_profile" old WHERE old."profile_key"=NEW."profile_key") AND (SELECT COUNT(*) FROM "deep_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_deep_transition
CREATE TRIGGER cp12_domain_cap_deep_transition BEFORE INSERT ON "deep_transition" WHEN NEW."transition_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "deep_transition" old WHERE old."transition_key"=NEW."transition_key") AND (SELECT COUNT(*) FROM "deep_transition")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_diagnosis_fix_plan
CREATE TRIGGER cp12_domain_cap_diagnosis_fix_plan BEFORE INSERT ON "diagnosis_fix_plan" WHEN NEW."plan_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "diagnosis_fix_plan" old WHERE old."plan_key"=NEW."plan_key") AND (SELECT COUNT(*) FROM "diagnosis_fix_plan")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_dynamic_domain_stat_v20
CREATE TRIGGER cp12_domain_cap_dynamic_domain_stat_v20 BEFORE INSERT ON "dynamic_domain_stat_v20" WHEN NEW."domain_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "dynamic_domain_stat_v20" old WHERE old."domain_key"=NEW."domain_key") AND (SELECT COUNT(*) FROM "dynamic_domain_stat_v20")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_dynamic_freeze_matrix
CREATE TRIGGER cp12_domain_cap_dynamic_freeze_matrix BEFORE INSERT ON "dynamic_freeze_matrix" WHEN NEW."freeze_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "dynamic_freeze_matrix" old WHERE old."freeze_key"=NEW."freeze_key") AND (SELECT COUNT(*) FROM "dynamic_freeze_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_false_strong_profile
CREATE TRIGGER cp12_domain_cap_false_strong_profile BEFORE INSERT ON "false_strong_profile" WHEN NEW."false_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "false_strong_profile" old WHERE old."false_key"=NEW."false_key") AND (SELECT COUNT(*) FROM "false_strong_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_forward_validation_stat_v20
CREATE TRIGGER cp12_domain_cap_forward_validation_stat_v20 BEFORE INSERT ON "forward_validation_stat_v20" WHEN NEW."domain_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "forward_validation_stat_v20" old WHERE old."domain_key"=NEW."domain_key") AND (SELECT COUNT(*) FROM "forward_validation_stat_v20")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_hierarchical_posterior_v28
CREATE TRIGGER cp12_domain_cap_hierarchical_posterior_v28 BEFORE INSERT ON "hierarchical_posterior_v28" WHEN NEW."family_level" IS NOT NULL AND NEW."family_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "hierarchical_posterior_v28" old WHERE old."family_level"=NEW."family_level" AND old."family_key"=NEW."family_key") AND (SELECT COUNT(*) FROM "hierarchical_posterior_v28")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_model_weight
CREATE TRIGGER cp12_domain_cap_model_weight BEFORE INSERT ON "model_weight" WHEN NEW."weight_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "model_weight" old WHERE old."weight_key"=NEW."weight_key") AND (SELECT COUNT(*) FROM "model_weight")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_module_effect_cache
CREATE TRIGGER cp12_domain_cap_module_effect_cache BEFORE INSERT ON "module_effect_cache" WHEN NEW."cache_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "module_effect_cache" old WHERE old."cache_key"=NEW."cache_key") AND (SELECT COUNT(*) FROM "module_effect_cache")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_monotonic_calibration_v28
CREATE TRIGGER cp12_domain_cap_monotonic_calibration_v28 BEFORE INSERT ON "monotonic_calibration_v28" WHEN NEW."calibration_version" IS NOT NULL AND NEW."bucket_no" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "monotonic_calibration_v28" old WHERE old."calibration_version"=NEW."calibration_version" AND old."bucket_no"=NEW."bucket_no") AND (SELECT COUNT(*) FROM "monotonic_calibration_v28")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_position_cause_matrix
CREATE TRIGGER cp12_domain_cap_position_cause_matrix BEFORE INSERT ON "position_cause_matrix" WHEN NEW."position_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "position_cause_matrix" old WHERE old."position_key"=NEW."position_key") AND (SELECT COUNT(*) FROM "position_cause_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_promotion_domain_stat_v20
CREATE TRIGGER cp12_domain_cap_promotion_domain_stat_v20 BEFORE INSERT ON "promotion_domain_stat_v20" WHEN NEW."domain_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "promotion_domain_stat_v20" old WHERE old."domain_key"=NEW."domain_key") AND (SELECT COUNT(*) FROM "promotion_domain_stat_v20")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_reason_causal_profile
CREATE TRIGGER cp12_domain_cap_reason_causal_profile BEFORE INSERT ON "reason_causal_profile" WHEN NEW."causal_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "reason_causal_profile" old WHERE old."causal_key"=NEW."causal_key") AND (SELECT COUNT(*) FROM "reason_causal_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_reason_contrast_profile
CREATE TRIGGER cp12_domain_cap_reason_contrast_profile BEFORE INSERT ON "reason_contrast_profile" WHEN NEW."profile_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "reason_contrast_profile" old WHERE old."profile_key"=NEW."profile_key") AND (SELECT COUNT(*) FROM "reason_contrast_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_rule_health_matrix
CREATE TRIGGER cp12_domain_cap_rule_health_matrix BEFORE INSERT ON "rule_health_matrix" WHEN NEW."rule_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "rule_health_matrix" old WHERE old."rule_key"=NEW."rule_key") AND (SELECT COUNT(*) FROM "rule_health_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_runtime_profile_cache
CREATE TRIGGER cp12_domain_cap_runtime_profile_cache BEFORE INSERT ON "runtime_profile_cache" WHEN NEW."profile_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "runtime_profile_cache" old WHERE old."profile_key"=NEW."profile_key") AND (SELECT COUNT(*) FROM "runtime_profile_cache")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_shadow_health_gate
CREATE TRIGGER cp12_domain_cap_shadow_health_gate BEFORE INSERT ON "shadow_health_gate" WHEN NEW."shadow_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "shadow_health_gate" old WHERE old."shadow_key"=NEW."shadow_key") AND (SELECT COUNT(*) FROM "shadow_health_gate")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_shadow_promotion_cache
CREATE TRIGGER cp12_domain_cap_shadow_promotion_cache BEFORE INSERT ON "shadow_promotion_cache" WHEN NEW."promotion_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "shadow_promotion_cache" old WHERE old."promotion_key"=NEW."promotion_key") AND (SELECT COUNT(*) FROM "shadow_promotion_cache")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_shape_memory
CREATE TRIGGER cp12_domain_cap_shape_memory BEFORE INSERT ON "shape_memory" WHEN NEW."shape_key" IS NOT NULL AND NEW."position" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "shape_memory" old WHERE old."shape_key"=NEW."shape_key" AND old."position"=NEW."position") AND (SELECT COUNT(*) FROM "shape_memory")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_source_health_matrix
CREATE TRIGGER cp12_domain_cap_source_health_matrix BEFORE INSERT ON "source_health_matrix" WHEN NEW."source_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "source_health_matrix" old WHERE old."source_key"=NEW."source_key") AND (SELECT COUNT(*) FROM "source_health_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_state_health_matrix
CREATE TRIGGER cp12_domain_cap_state_health_matrix BEFORE INSERT ON "state_health_matrix" WHEN NEW."state_health_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "state_health_matrix" old WHERE old."state_health_key"=NEW."state_health_key") AND (SELECT COUNT(*) FROM "state_health_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_state_number_profile
CREATE TRIGGER cp12_domain_cap_state_number_profile BEFORE INSERT ON "state_number_profile" WHEN NEW."profile_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "state_number_profile" old WHERE old."profile_key"=NEW."profile_key") AND (SELECT COUNT(*) FROM "state_number_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_teacher_assist_profile
CREATE TRIGGER cp12_domain_cap_teacher_assist_profile BEFORE INSERT ON "teacher_assist_profile" WHEN NEW."assist_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "teacher_assist_profile" old WHERE old."assist_key"=NEW."assist_key") AND (SELECT COUNT(*) FROM "teacher_assist_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_teacher_domain_stat_v20
CREATE TRIGGER cp12_domain_cap_teacher_domain_stat_v20 BEFORE INSERT ON "teacher_domain_stat_v20" WHEN NEW."domain_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "teacher_domain_stat_v20" old WHERE old."domain_key"=NEW."domain_key") AND (SELECT COUNT(*) FROM "teacher_domain_stat_v20")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_teacher_matrix_combo_profile
CREATE TRIGGER cp12_domain_cap_teacher_matrix_combo_profile BEFORE INSERT ON "teacher_matrix_combo_profile" WHEN NEW."combo_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "teacher_matrix_combo_profile" old WHERE old."combo_key"=NEW."combo_key") AND (SELECT COUNT(*) FROM "teacher_matrix_combo_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_teacher_matrix_profile
CREATE TRIGGER cp12_domain_cap_teacher_matrix_profile BEFORE INSERT ON "teacher_matrix_profile" WHEN NEW."profile_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "teacher_matrix_profile" old WHERE old."profile_key"=NEW."profile_key") AND (SELECT COUNT(*) FROM "teacher_matrix_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_teacher_matrix_state_profile
CREATE TRIGGER cp12_domain_cap_teacher_matrix_state_profile BEFORE INSERT ON "teacher_matrix_state_profile" WHEN NEW."state_profile_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "teacher_matrix_state_profile" old WHERE old."state_profile_key"=NEW."state_profile_key") AND (SELECT COUNT(*) FROM "teacher_matrix_state_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_teacher_number_matrix
CREATE TRIGGER cp12_domain_cap_teacher_number_matrix BEFORE INSERT ON "teacher_number_matrix" WHEN NEW."teacher_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "teacher_number_matrix" old WHERE old."teacher_key"=NEW."teacher_key") AND (SELECT COUNT(*) FROM "teacher_number_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_third_guard_matrix
CREATE TRIGGER cp12_domain_cap_third_guard_matrix BEFORE INSERT ON "third_guard_matrix" WHEN NEW."guard_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "third_guard_matrix" old WHERE old."guard_key"=NEW."guard_key") AND (SELECT COUNT(*) FROM "third_guard_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_v20_action_matrix
CREATE TRIGGER cp12_domain_cap_v20_action_matrix BEFORE INSERT ON "v20_action_matrix" WHEN NEW."action_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "v20_action_matrix" old WHERE old."action_key"=NEW."action_key") AND (SELECT COUNT(*) FROM "v20_action_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_v20_cause_matrix
CREATE TRIGGER cp12_domain_cap_v20_cause_matrix BEFORE INSERT ON "v20_cause_matrix" WHEN NEW."cause_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "v20_cause_matrix" old WHERE old."cause_key"=NEW."cause_key") AND (SELECT COUNT(*) FROM "v20_cause_matrix")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_v49_teacher_posterior
CREATE TRIGGER cp12_domain_cap_v49_teacher_posterior BEFORE INSERT ON "v49_teacher_posterior" WHEN NEW."teacher_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "v49_teacher_posterior" old WHERE old."teacher_key"=NEW."teacher_key") AND (SELECT COUNT(*) FROM "v49_teacher_posterior")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_v49_ui_cache
CREATE TRIGGER cp12_domain_cap_v49_ui_cache BEFORE INSERT ON "v49_ui_cache" WHEN NEW."cache_key" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "v49_ui_cache" old WHERE old."cache_key"=NEW."cache_key") AND (SELECT COUNT(*) FROM "v49_ui_cache")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_cap_v50_teacher_profile
CREATE TRIGGER cp12_domain_cap_v50_teacher_profile BEFORE INSERT ON "v50_teacher_profile" WHEN NEW."evidence_level" IS NOT NULL AND NEW."state_domain" IS NOT NULL AND NEW."lifecycle" IS NOT NULL AND NEW."position" IS NOT NULL AND NEW."number" IS NOT NULL AND NOT EXISTS(SELECT 1 FROM "v50_teacher_profile" old WHERE old."evidence_level"=NEW."evidence_level" AND old."state_domain"=NEW."state_domain" AND old."lifecycle"=NEW."lifecycle" AND old."position"=NEW."position" AND old."number"=NEW."number") AND (SELECT COUNT(*) FROM "v50_teacher_profile")>=4096 BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_CAPACITY'); END;

-- cp12_domain_null_break_memory
CREATE TRIGGER cp12_domain_null_break_memory BEFORE INSERT ON "break_memory" WHEN NEW."break_key" IS NULL OR NEW."position" IS NULL OR NEW."break_depth_bucket" IS NULL OR NEW."drift_bucket" IS NULL OR NEW."markov_bucket" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_calibration_bucket
CREATE TRIGGER cp12_domain_null_calibration_bucket BEFORE INSERT ON "calibration_bucket" WHEN NEW."bucket_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_causal_factor_matrix
CREATE TRIGGER cp12_domain_null_causal_factor_matrix BEFORE INSERT ON "causal_factor_matrix" WHEN NEW."causal_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_combo_memory
CREATE TRIGGER cp12_domain_null_combo_memory BEFORE INSERT ON "combo_memory" WHEN NEW."shape_key" IS NULL OR NEW."position" IS NULL OR NEW."k3_mask" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_combo_role_profile
CREATE TRIGGER cp12_domain_null_combo_role_profile BEFORE INSERT ON "combo_role_profile" WHEN NEW."combo_role_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_deep_cycle
CREATE TRIGGER cp12_domain_null_deep_cycle BEFORE INSERT ON "deep_cycle" WHEN NEW."cycle_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_deep_profile
CREATE TRIGGER cp12_domain_null_deep_profile BEFORE INSERT ON "deep_profile" WHEN NEW."profile_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_deep_transition
CREATE TRIGGER cp12_domain_null_deep_transition BEFORE INSERT ON "deep_transition" WHEN NEW."transition_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_diagnosis_fix_plan
CREATE TRIGGER cp12_domain_null_diagnosis_fix_plan BEFORE INSERT ON "diagnosis_fix_plan" WHEN NEW."plan_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_dynamic_domain_stat_v20
CREATE TRIGGER cp12_domain_null_dynamic_domain_stat_v20 BEFORE INSERT ON "dynamic_domain_stat_v20" WHEN NEW."domain_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_dynamic_freeze_matrix
CREATE TRIGGER cp12_domain_null_dynamic_freeze_matrix BEFORE INSERT ON "dynamic_freeze_matrix" WHEN NEW."freeze_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_false_strong_profile
CREATE TRIGGER cp12_domain_null_false_strong_profile BEFORE INSERT ON "false_strong_profile" WHEN NEW."false_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_forward_validation_stat_v20
CREATE TRIGGER cp12_domain_null_forward_validation_stat_v20 BEFORE INSERT ON "forward_validation_stat_v20" WHEN NEW."domain_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_hierarchical_posterior_v28
CREATE TRIGGER cp12_domain_null_hierarchical_posterior_v28 BEFORE INSERT ON "hierarchical_posterior_v28" WHEN NEW."family_level" IS NULL OR NEW."family_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_model_weight
CREATE TRIGGER cp12_domain_null_model_weight BEFORE INSERT ON "model_weight" WHEN NEW."weight_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_module_effect_cache
CREATE TRIGGER cp12_domain_null_module_effect_cache BEFORE INSERT ON "module_effect_cache" WHEN NEW."cache_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_monotonic_calibration_v28
CREATE TRIGGER cp12_domain_null_monotonic_calibration_v28 BEFORE INSERT ON "monotonic_calibration_v28" WHEN NEW."calibration_version" IS NULL OR NEW."bucket_no" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_position_cause_matrix
CREATE TRIGGER cp12_domain_null_position_cause_matrix BEFORE INSERT ON "position_cause_matrix" WHEN NEW."position_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_promotion_domain_stat_v20
CREATE TRIGGER cp12_domain_null_promotion_domain_stat_v20 BEFORE INSERT ON "promotion_domain_stat_v20" WHEN NEW."domain_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_reason_causal_profile
CREATE TRIGGER cp12_domain_null_reason_causal_profile BEFORE INSERT ON "reason_causal_profile" WHEN NEW."causal_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_reason_contrast_profile
CREATE TRIGGER cp12_domain_null_reason_contrast_profile BEFORE INSERT ON "reason_contrast_profile" WHEN NEW."profile_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_rule_health_matrix
CREATE TRIGGER cp12_domain_null_rule_health_matrix BEFORE INSERT ON "rule_health_matrix" WHEN NEW."rule_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_runtime_profile_cache
CREATE TRIGGER cp12_domain_null_runtime_profile_cache BEFORE INSERT ON "runtime_profile_cache" WHEN NEW."profile_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_shadow_health_gate
CREATE TRIGGER cp12_domain_null_shadow_health_gate BEFORE INSERT ON "shadow_health_gate" WHEN NEW."shadow_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_shadow_promotion_cache
CREATE TRIGGER cp12_domain_null_shadow_promotion_cache BEFORE INSERT ON "shadow_promotion_cache" WHEN NEW."promotion_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_shape_memory
CREATE TRIGGER cp12_domain_null_shape_memory BEFORE INSERT ON "shape_memory" WHEN NEW."shape_key" IS NULL OR NEW."position" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_source_health_matrix
CREATE TRIGGER cp12_domain_null_source_health_matrix BEFORE INSERT ON "source_health_matrix" WHEN NEW."source_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_state_health_matrix
CREATE TRIGGER cp12_domain_null_state_health_matrix BEFORE INSERT ON "state_health_matrix" WHEN NEW."state_health_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_state_number_profile
CREATE TRIGGER cp12_domain_null_state_number_profile BEFORE INSERT ON "state_number_profile" WHEN NEW."profile_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_teacher_assist_profile
CREATE TRIGGER cp12_domain_null_teacher_assist_profile BEFORE INSERT ON "teacher_assist_profile" WHEN NEW."assist_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_teacher_domain_stat_v20
CREATE TRIGGER cp12_domain_null_teacher_domain_stat_v20 BEFORE INSERT ON "teacher_domain_stat_v20" WHEN NEW."domain_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_teacher_matrix_combo_profile
CREATE TRIGGER cp12_domain_null_teacher_matrix_combo_profile BEFORE INSERT ON "teacher_matrix_combo_profile" WHEN NEW."combo_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_teacher_matrix_profile
CREATE TRIGGER cp12_domain_null_teacher_matrix_profile BEFORE INSERT ON "teacher_matrix_profile" WHEN NEW."profile_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_teacher_matrix_state_profile
CREATE TRIGGER cp12_domain_null_teacher_matrix_state_profile BEFORE INSERT ON "teacher_matrix_state_profile" WHEN NEW."state_profile_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_teacher_number_matrix
CREATE TRIGGER cp12_domain_null_teacher_number_matrix BEFORE INSERT ON "teacher_number_matrix" WHEN NEW."teacher_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_third_guard_matrix
CREATE TRIGGER cp12_domain_null_third_guard_matrix BEFORE INSERT ON "third_guard_matrix" WHEN NEW."guard_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_v20_action_matrix
CREATE TRIGGER cp12_domain_null_v20_action_matrix BEFORE INSERT ON "v20_action_matrix" WHEN NEW."action_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_v20_cause_matrix
CREATE TRIGGER cp12_domain_null_v20_cause_matrix BEFORE INSERT ON "v20_cause_matrix" WHEN NEW."cause_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_v49_teacher_posterior
CREATE TRIGGER cp12_domain_null_v49_teacher_posterior BEFORE INSERT ON "v49_teacher_posterior" WHEN NEW."teacher_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_v49_ui_cache
CREATE TRIGGER cp12_domain_null_v49_ui_cache BEFORE INSERT ON "v49_ui_cache" WHEN NEW."cache_key" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_domain_null_v50_teacher_profile
CREATE TRIGGER cp12_domain_null_v50_teacher_profile BEFORE INSERT ON "v50_teacher_profile" WHEN NEW."evidence_level" IS NULL OR NEW."state_domain" IS NULL OR NEW."lifecycle" IS NULL OR NEW."position" IS NULL OR NEW."number" IS NULL BEGIN SELECT RAISE(ABORT,'RETENTION_AGGREGATE_INVALID_KEY'); END;

-- cp12_draw_dependency_delete_guard
CREATE TRIGGER cp12_draw_dependency_delete_guard BEFORE DELETE ON draw_dependency_input_cp12 WHEN NOT EXISTS(SELECT 1 FROM app_state WHERE state_key='cp12_draw_dependency_release_issue' AND state_value=OLD.issue) BEGIN SELECT RAISE(ABORT,'CP12_DRAW_DEPENDENCY_IN_USE'); END;

-- cp12_draw_dependency_immutable
CREATE TRIGGER cp12_draw_dependency_immutable BEFORE UPDATE ON draw_dependency_input_cp12 BEGIN SELECT RAISE(ABORT,'CP12_DRAW_DEPENDENCY_IMMUTABLE'); END;

-- cp12_family_creation_identity
CREATE TRIGGER cp12_family_creation_identity BEFORE UPDATE ON research_family_creation_cp12 WHEN OLD.family<>NEW.family OR OLD.source_hash<>NEW.source_hash OR OLD.variant<>NEW.variant OR OLD.ast_hash<>NEW.ast_hash OR OLD.ast<>NEW.ast OR OLD.action_id<>NEW.action_id OR OLD.candidate_id<>NEW.candidate_id OR OLD.source_issue<>NEW.source_issue BEGIN SELECT RAISE(ABORT,'FAMILY_CREATION_IMMUTABLE'); END;

-- cp12_family_creation_no_delete
CREATE TRIGGER cp12_family_creation_no_delete BEFORE DELETE ON research_family_creation_cp12 BEGIN SELECT RAISE(ABORT,'FAMILY_CREATION_PERMANENT'); END;

-- cp12_family_creation_no_replace
CREATE TRIGGER cp12_family_creation_no_replace BEFORE INSERT ON research_family_creation_cp12 WHEN EXISTS(SELECT 1 FROM research_family_creation_cp12 c WHERE c.family=NEW.family AND(c.source_hash=NEW.source_hash OR c.variant=NEW.variant)) BEGIN SELECT RAISE(ABORT,'FAMILY_CREATION_REPLACE_FORBIDDEN'); END;

-- cp12_family_state
CREATE INDEX cp12_family_state ON research_family_creation_cp12(family,state,variant);

-- cp12_feature_active_delete
CREATE TRIGGER cp12_feature_active_delete BEFORE DELETE ON research_training_feature_row_cp11
 WHEN EXISTS(SELECT 1 FROM v57_research_task_r967 WHERE task_id=OLD.task_id AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))
 BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_FEATURE_PROTECTED'); END;

-- cp12_feature_cursor_active_delete
CREATE TRIGGER cp12_feature_cursor_active_delete BEFORE DELETE ON research_training_feature_cursor_cp12
 WHEN EXISTS(SELECT 1 FROM v57_research_task_r967 WHERE task_id=OLD.task_id AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))
 BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_FEATURE_CURSOR_PROTECTED'); END;

-- cp12_feature_cursor_identity
CREATE TRIGGER cp12_feature_cursor_identity BEFORE UPDATE OF task_id,protocol_hash,input_hash ON research_training_feature_cursor_cp12
 BEGIN SELECT RAISE(ABORT,'CP12_FEATURE_CURSOR_IDENTITY'); END;

-- cp12_feature_no_replace
CREATE TRIGGER cp12_feature_no_replace BEFORE INSERT ON research_training_feature_row_cp11
 WHEN EXISTS(SELECT 1 FROM research_training_feature_row_cp11 WHERE task_id=NEW.task_id AND (row_index=NEW.row_index OR target_issue=NEW.target_issue))
 BEGIN SELECT RAISE(ABORT,'CP12_FEATURE_REPLACE_FORBIDDEN'); END;

-- cp12_feature_no_update
CREATE TRIGGER cp12_feature_no_update BEFORE UPDATE ON research_training_feature_row_cp11
 BEGIN SELECT RAISE(ABORT,'CP12_FEATURE_IMMUTABLE'); END;

-- cp12_matrix_terminal_immutable
CREATE TRIGGER cp12_matrix_terminal_immutable BEFORE UPDATE ON research_matrix_terminal_cp12 BEGIN
 SELECT RAISE(ABORT,'CP12_MATRIX_TERMINAL_IMMUTABLE'); END;

-- cp12_model_counter_consistency
CREATE TRIGGER cp12_model_counter_consistency BEFORE UPDATE OF ref_count ON research_durable_model_identity_cp12 WHEN NEW.ref_count<>(SELECT COUNT(*) FROM research_durable_model_ref_cp12 r WHERE r.model_key=NEW.model_key) BEGIN SELECT RAISE(ABORT,'MODEL_STORAGE_COUNTER_CONFLICT'); END;

-- cp12_model_identity_guard
CREATE TRIGGER cp12_model_identity_guard BEFORE UPDATE OF model_key,byte_count ON research_durable_model_identity_cp12 WHEN OLD.ref_count>0 BEGIN SELECT RAISE(ABORT,'MODEL_STORAGE_IDENTITY_IMMUTABLE'); END;

-- cp12_model_quota_r952_artifact_insert
CREATE TRIGGER cp12_model_quota_r952_artifact_insert BEFORE INSERT ON "r952_artifact" WHEN length(CAST(NEW."model_payload" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."model_payload" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."payload_hash" AND m.byte_count<>length(CAST(NEW."model_payload" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."payload_hash" AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."model_payload" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."model_payload" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='r952_artifact' AND source_key=NEW."artifact_hash"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_r952_artifact_update
CREATE TRIGGER cp12_model_quota_r952_artifact_update BEFORE UPDATE OF "model_payload","payload_hash","artifact_hash" ON "r952_artifact" WHEN length(CAST(NEW."model_payload" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."model_payload" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."payload_hash" AND m.byte_count<>length(CAST(NEW."model_payload" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."payload_hash" AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."model_payload" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."model_payload" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='r952_artifact' AND source_key=NEW."artifact_hash"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_r958_frozen_candidate_insert
CREATE TRIGGER cp12_model_quota_r958_frozen_candidate_insert BEFORE INSERT ON "r958_frozen_candidate" WHEN length(CAST(NEW."model_payload" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."model_payload" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.byte_count<>length(CAST(NEW."model_payload" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."model_payload" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."model_payload" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='r958_frozen_candidate' AND source_key=NEW."candidate_id"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_r958_frozen_candidate_update
CREATE TRIGGER cp12_model_quota_r958_frozen_candidate_update BEFORE UPDATE OF "model_payload","model_hash","candidate_id" ON "r958_frozen_candidate" WHEN length(CAST(NEW."model_payload" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."model_payload" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.byte_count<>length(CAST(NEW."model_payload" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."model_payload" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."model_payload" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='r958_frozen_candidate' AND source_key=NEW."candidate_id"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_r967_frozen_evaluation_v2_insert
CREATE TRIGGER cp12_model_quota_r967_frozen_evaluation_v2_insert BEFORE INSERT ON "r967_frozen_evaluation_v2" WHEN length(CAST(NEW."model_payload" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."model_payload" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.byte_count<>length(CAST(NEW."model_payload" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."model_payload" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."model_payload" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='r967_frozen_evaluation_v2' AND source_key=NEW."candidate_id"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_r967_frozen_evaluation_v2_update
CREATE TRIGGER cp12_model_quota_r967_frozen_evaluation_v2_update BEFORE UPDATE OF "model_payload","model_hash","candidate_id" ON "r967_frozen_evaluation_v2" WHEN length(CAST(NEW."model_payload" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."model_payload" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.byte_count<>length(CAST(NEW."model_payload" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."model_payload" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."model_payload" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='r967_frozen_evaluation_v2' AND source_key=NEW."candidate_id"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_r967_numeric_training_run_insert
CREATE TRIGGER cp12_model_quota_r967_numeric_training_run_insert BEFORE INSERT ON "r967_numeric_training_run" WHEN length(CAST(NEW."initial_payload" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."initial_payload" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='INITIAL/'||NEW.run_hash AND m.byte_count<>length(CAST(NEW."initial_payload" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='INITIAL/'||NEW.run_hash AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."initial_payload" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."initial_payload" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='r967_numeric_training_run' AND source_key=NEW."run_hash"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_r967_numeric_training_run_update
CREATE TRIGGER cp12_model_quota_r967_numeric_training_run_update BEFORE UPDATE OF "initial_payload","run_hash" ON "r967_numeric_training_run" WHEN length(CAST(NEW."initial_payload" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."initial_payload" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='INITIAL/'||NEW.run_hash AND m.byte_count<>length(CAST(NEW."initial_payload" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='INITIAL/'||NEW.run_hash AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."initial_payload" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."initial_payload" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='r967_numeric_training_run' AND source_key=NEW."run_hash"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_research_candidate_cal_model_r932_insert
CREATE TRIGGER cp12_model_quota_research_candidate_cal_model_r932_insert BEFORE INSERT ON "research_candidate_cal_model_r932" WHEN length(CAST(NEW."model_json" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."model_json" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.byte_count<>length(CAST(NEW."model_json" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."model_json" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."model_json" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='research_candidate_cal_model_r932' AND source_key=NEW."study_hash"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_quota_research_candidate_cal_model_r932_update
CREATE TRIGGER cp12_model_quota_research_candidate_cal_model_r932_update BEFORE UPDATE OF "model_json","model_hash","study_hash" ON "research_candidate_cal_model_r932" WHEN length(CAST(NEW."model_json" AS BLOB))>0 BEGIN
SELECT CASE WHEN length(CAST(NEW."model_json" AS BLOB))>262144 THEN RAISE(ABORT,'MODEL_STORAGE_SINGLE_PAYLOAD_BOUND') END;
SELECT CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.byte_count<>length(CAST(NEW."model_json" AS BLOB)) AND m.ref_count>0) THEN RAISE(ABORT,'MODEL_STORAGE_HASH_SIZE_CONFLICT') END;
SELECT CASE WHEN COALESCE((SELECT SUM(byte_count) FROM research_durable_model_identity_cp12 WHERE ref_count>0),0)+CASE WHEN EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key='MODEL/'||NEW."model_hash" AND m.ref_count>0) THEN 0 ELSE length(CAST(NEW."model_json" AS BLOB)) END>67108864 OR COALESCE((SELECT SUM(byte_count) FROM research_durable_model_ref_cp12),0)+length(CAST(NEW."model_json" AS BLOB))-COALESCE((SELECT byte_count FROM research_durable_model_ref_cp12 WHERE source_table='research_candidate_cal_model_r932' AND source_key=NEW."study_hash"),0)>134217728 THEN RAISE(ABORT,'MODEL_STORAGE_CAPACITY') END; END;

-- cp12_model_ref_add
CREATE TRIGGER cp12_model_ref_add AFTER INSERT ON research_durable_model_ref_cp12 BEGIN UPDATE research_durable_model_identity_cp12 SET ref_count=ref_count+1 WHERE model_key=NEW.model_key; END;

-- cp12_model_ref_identity
CREATE TRIGGER cp12_model_ref_identity BEFORE INSERT ON research_durable_model_ref_cp12 WHEN NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 m WHERE m.model_key=NEW.model_key AND m.byte_count=NEW.byte_count) BEGIN SELECT RAISE(ABORT,'MODEL_STORAGE_REF_IDENTITY'); END;

-- cp12_model_ref_immutable
CREATE TRIGGER cp12_model_ref_immutable BEFORE UPDATE ON research_durable_model_ref_cp12 BEGIN SELECT RAISE(ABORT,'MODEL_STORAGE_REF_IMMUTABLE'); END;

-- cp12_model_ref_r952_artifact_delete
CREATE TRIGGER cp12_model_ref_r952_artifact_delete AFTER DELETE ON "r952_artifact" BEGIN DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r952_artifact' AND source_key=OLD."artifact_hash"; END;

-- cp12_model_ref_r952_artifact_insert
CREATE TRIGGER cp12_model_ref_r952_artifact_insert AFTER INSERT ON "r952_artifact" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r952_artifact' AND source_key=NEW."artifact_hash";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'MODEL/'||NEW."payload_hash",length(CAST(NEW."model_payload" AS BLOB)),0 WHERE length(CAST(NEW."model_payload" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='MODEL/'||NEW."payload_hash");
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."model_payload" AS BLOB)) WHERE model_key='MODEL/'||NEW."payload_hash" AND ref_count=0 AND byte_count<>length(CAST(NEW."model_payload" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'r952_artifact',NEW."artifact_hash",'MODEL/'||NEW."payload_hash",length(CAST(NEW."model_payload" AS BLOB)) WHERE length(CAST(NEW."model_payload" AS BLOB))>0; END;

-- cp12_model_ref_r952_artifact_update
CREATE TRIGGER cp12_model_ref_r952_artifact_update AFTER UPDATE OF "model_payload","payload_hash","artifact_hash" ON "r952_artifact" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r952_artifact' AND source_key=NEW."artifact_hash";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'MODEL/'||NEW."payload_hash",length(CAST(NEW."model_payload" AS BLOB)),0 WHERE length(CAST(NEW."model_payload" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='MODEL/'||NEW."payload_hash");
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."model_payload" AS BLOB)) WHERE model_key='MODEL/'||NEW."payload_hash" AND ref_count=0 AND byte_count<>length(CAST(NEW."model_payload" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'r952_artifact',NEW."artifact_hash",'MODEL/'||NEW."payload_hash",length(CAST(NEW."model_payload" AS BLOB)) WHERE length(CAST(NEW."model_payload" AS BLOB))>0; END;

-- cp12_model_ref_r958_frozen_candidate_delete
CREATE TRIGGER cp12_model_ref_r958_frozen_candidate_delete AFTER DELETE ON "r958_frozen_candidate" BEGIN DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r958_frozen_candidate' AND source_key=OLD."candidate_id"; END;

-- cp12_model_ref_r958_frozen_candidate_insert
CREATE TRIGGER cp12_model_ref_r958_frozen_candidate_insert AFTER INSERT ON "r958_frozen_candidate" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r958_frozen_candidate' AND source_key=NEW."candidate_id";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'MODEL/'||NEW."model_hash",length(CAST(NEW."model_payload" AS BLOB)),0 WHERE length(CAST(NEW."model_payload" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='MODEL/'||NEW."model_hash");
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."model_payload" AS BLOB)) WHERE model_key='MODEL/'||NEW."model_hash" AND ref_count=0 AND byte_count<>length(CAST(NEW."model_payload" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'r958_frozen_candidate',NEW."candidate_id",'MODEL/'||NEW."model_hash",length(CAST(NEW."model_payload" AS BLOB)) WHERE length(CAST(NEW."model_payload" AS BLOB))>0; END;

-- cp12_model_ref_r958_frozen_candidate_update
CREATE TRIGGER cp12_model_ref_r958_frozen_candidate_update AFTER UPDATE OF "model_payload","model_hash","candidate_id" ON "r958_frozen_candidate" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r958_frozen_candidate' AND source_key=NEW."candidate_id";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'MODEL/'||NEW."model_hash",length(CAST(NEW."model_payload" AS BLOB)),0 WHERE length(CAST(NEW."model_payload" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='MODEL/'||NEW."model_hash");
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."model_payload" AS BLOB)) WHERE model_key='MODEL/'||NEW."model_hash" AND ref_count=0 AND byte_count<>length(CAST(NEW."model_payload" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'r958_frozen_candidate',NEW."candidate_id",'MODEL/'||NEW."model_hash",length(CAST(NEW."model_payload" AS BLOB)) WHERE length(CAST(NEW."model_payload" AS BLOB))>0; END;

-- cp12_model_ref_r967_frozen_evaluation_v2_delete
CREATE TRIGGER cp12_model_ref_r967_frozen_evaluation_v2_delete AFTER DELETE ON "r967_frozen_evaluation_v2" BEGIN DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r967_frozen_evaluation_v2' AND source_key=OLD."candidate_id"; END;

-- cp12_model_ref_r967_frozen_evaluation_v2_insert
CREATE TRIGGER cp12_model_ref_r967_frozen_evaluation_v2_insert AFTER INSERT ON "r967_frozen_evaluation_v2" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r967_frozen_evaluation_v2' AND source_key=NEW."candidate_id";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'MODEL/'||NEW."model_hash",length(CAST(NEW."model_payload" AS BLOB)),0 WHERE length(CAST(NEW."model_payload" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='MODEL/'||NEW."model_hash");
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."model_payload" AS BLOB)) WHERE model_key='MODEL/'||NEW."model_hash" AND ref_count=0 AND byte_count<>length(CAST(NEW."model_payload" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'r967_frozen_evaluation_v2',NEW."candidate_id",'MODEL/'||NEW."model_hash",length(CAST(NEW."model_payload" AS BLOB)) WHERE length(CAST(NEW."model_payload" AS BLOB))>0; END;

-- cp12_model_ref_r967_frozen_evaluation_v2_update
CREATE TRIGGER cp12_model_ref_r967_frozen_evaluation_v2_update AFTER UPDATE OF "model_payload","model_hash","candidate_id" ON "r967_frozen_evaluation_v2" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r967_frozen_evaluation_v2' AND source_key=NEW."candidate_id";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'MODEL/'||NEW."model_hash",length(CAST(NEW."model_payload" AS BLOB)),0 WHERE length(CAST(NEW."model_payload" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='MODEL/'||NEW."model_hash");
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."model_payload" AS BLOB)) WHERE model_key='MODEL/'||NEW."model_hash" AND ref_count=0 AND byte_count<>length(CAST(NEW."model_payload" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'r967_frozen_evaluation_v2',NEW."candidate_id",'MODEL/'||NEW."model_hash",length(CAST(NEW."model_payload" AS BLOB)) WHERE length(CAST(NEW."model_payload" AS BLOB))>0; END;

-- cp12_model_ref_r967_numeric_training_run_delete
CREATE TRIGGER cp12_model_ref_r967_numeric_training_run_delete AFTER DELETE ON "r967_numeric_training_run" BEGIN DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r967_numeric_training_run' AND source_key=OLD."run_hash"; END;

-- cp12_model_ref_r967_numeric_training_run_insert
CREATE TRIGGER cp12_model_ref_r967_numeric_training_run_insert AFTER INSERT ON "r967_numeric_training_run" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r967_numeric_training_run' AND source_key=NEW."run_hash";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'INITIAL/'||NEW.run_hash,length(CAST(NEW."initial_payload" AS BLOB)),0 WHERE length(CAST(NEW."initial_payload" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='INITIAL/'||NEW.run_hash);
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."initial_payload" AS BLOB)) WHERE model_key='INITIAL/'||NEW.run_hash AND ref_count=0 AND byte_count<>length(CAST(NEW."initial_payload" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'r967_numeric_training_run',NEW."run_hash",'INITIAL/'||NEW.run_hash,length(CAST(NEW."initial_payload" AS BLOB)) WHERE length(CAST(NEW."initial_payload" AS BLOB))>0; END;

-- cp12_model_ref_r967_numeric_training_run_update
CREATE TRIGGER cp12_model_ref_r967_numeric_training_run_update AFTER UPDATE OF "initial_payload","run_hash" ON "r967_numeric_training_run" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='r967_numeric_training_run' AND source_key=NEW."run_hash";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'INITIAL/'||NEW.run_hash,length(CAST(NEW."initial_payload" AS BLOB)),0 WHERE length(CAST(NEW."initial_payload" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='INITIAL/'||NEW.run_hash);
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."initial_payload" AS BLOB)) WHERE model_key='INITIAL/'||NEW.run_hash AND ref_count=0 AND byte_count<>length(CAST(NEW."initial_payload" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'r967_numeric_training_run',NEW."run_hash",'INITIAL/'||NEW.run_hash,length(CAST(NEW."initial_payload" AS BLOB)) WHERE length(CAST(NEW."initial_payload" AS BLOB))>0; END;

-- cp12_model_ref_remove
CREATE TRIGGER cp12_model_ref_remove AFTER DELETE ON research_durable_model_ref_cp12 BEGIN UPDATE research_durable_model_identity_cp12 SET ref_count=ref_count-1 WHERE model_key=OLD.model_key; DELETE FROM research_durable_model_identity_cp12 WHERE model_key=OLD.model_key AND ref_count=0; END;

-- cp12_model_ref_research_candidate_cal_model_r932_delete
CREATE TRIGGER cp12_model_ref_research_candidate_cal_model_r932_delete AFTER DELETE ON "research_candidate_cal_model_r932" BEGIN DELETE FROM research_durable_model_ref_cp12 WHERE source_table='research_candidate_cal_model_r932' AND source_key=OLD."study_hash"; END;

-- cp12_model_ref_research_candidate_cal_model_r932_insert
CREATE TRIGGER cp12_model_ref_research_candidate_cal_model_r932_insert AFTER INSERT ON "research_candidate_cal_model_r932" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='research_candidate_cal_model_r932' AND source_key=NEW."study_hash";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'MODEL/'||NEW."model_hash",length(CAST(NEW."model_json" AS BLOB)),0 WHERE length(CAST(NEW."model_json" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='MODEL/'||NEW."model_hash");
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."model_json" AS BLOB)) WHERE model_key='MODEL/'||NEW."model_hash" AND ref_count=0 AND byte_count<>length(CAST(NEW."model_json" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'research_candidate_cal_model_r932',NEW."study_hash",'MODEL/'||NEW."model_hash",length(CAST(NEW."model_json" AS BLOB)) WHERE length(CAST(NEW."model_json" AS BLOB))>0; END;

-- cp12_model_ref_research_candidate_cal_model_r932_update
CREATE TRIGGER cp12_model_ref_research_candidate_cal_model_r932_update AFTER UPDATE OF "model_json","model_hash","study_hash" ON "research_candidate_cal_model_r932" BEGIN
 DELETE FROM research_durable_model_ref_cp12 WHERE source_table='research_candidate_cal_model_r932' AND source_key=NEW."study_hash";
 INSERT INTO research_durable_model_identity_cp12(model_key,byte_count,ref_count) SELECT 'MODEL/'||NEW."model_hash",length(CAST(NEW."model_json" AS BLOB)),0 WHERE length(CAST(NEW."model_json" AS BLOB))>0 AND NOT EXISTS(SELECT 1 FROM research_durable_model_identity_cp12 WHERE model_key='MODEL/'||NEW."model_hash");
 UPDATE research_durable_model_identity_cp12 SET byte_count=length(CAST(NEW."model_json" AS BLOB)) WHERE model_key='MODEL/'||NEW."model_hash" AND ref_count=0 AND byte_count<>length(CAST(NEW."model_json" AS BLOB));
 INSERT INTO research_durable_model_ref_cp12(source_table,source_key,model_key,byte_count) SELECT 'research_candidate_cal_model_r932',NEW."study_hash",'MODEL/'||NEW."model_hash",length(CAST(NEW."model_json" AS BLOB)) WHERE length(CAST(NEW."model_json" AS BLOB))>0; END;

-- cp12_molecule_checkpoint_identity
CREATE TRIGGER cp12_molecule_checkpoint_identity BEFORE UPDATE ON research_molecule_import_checkpoint_cp12 WHEN OLD.source_hash<>NEW.source_hash OR OLD.asset_name<>NEW.asset_name OR OLD.expected_rows<>NEW.expected_rows OR OLD.expected_bytes<>NEW.expected_bytes OR OLD.legacy_target_row<>NEW.legacy_target_row BEGIN SELECT RAISE(ABORT,'SEED_CHECKPOINT_IDENTITY_IMMUTABLE'); END;

-- cp12_molecule_checkpoint_no_delete
CREATE TRIGGER cp12_molecule_checkpoint_no_delete BEFORE DELETE ON research_molecule_import_checkpoint_cp12 BEGIN SELECT RAISE(ABORT,'SEED_CHECKPOINT_COMPACT_PERMANENT'); END;

-- cp12_molecule_checkpoint_no_replace
CREATE TRIGGER cp12_molecule_checkpoint_no_replace BEFORE INSERT ON research_molecule_import_checkpoint_cp12 WHEN EXISTS(SELECT 1 FROM research_molecule_import_checkpoint_cp12 WHERE source_hash=NEW.source_hash) BEGIN SELECT RAISE(ABORT,'SEED_CHECKPOINT_REPLACE_FORBIDDEN'); END;

-- cp12_protocol_attachment_no_update
CREATE TRIGGER cp12_protocol_attachment_no_update BEFORE UPDATE ON research_training_protocol_attachment_cp12
 BEGIN SELECT RAISE(ABORT,'CP12_PROTOCOL_ATTACHMENT_IMMUTABLE'); END;

-- cp12_regime5_no_update
CREATE TRIGGER cp12_regime5_no_update BEFORE UPDATE ON research_matrix_regime5_cp12 BEGIN
 SELECT RAISE(ABORT,'CP12_REGIME5_IMMUTABLE'); END;

-- cp12_registration_active_delete
CREATE TRIGGER cp12_registration_active_delete BEFORE DELETE ON research_training_registration_row_cp12
 WHEN EXISTS(SELECT 1 FROM research_action_v2 WHERE action_id=OLD.action_id AND state='PROPOSED')
 BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_REGISTRATION_PROTECTED'); END;

-- cp12_registration_no_replace
CREATE TRIGGER cp12_registration_no_replace BEFORE INSERT ON research_training_registration_row_cp12
 WHEN EXISTS(SELECT 1 FROM research_training_registration_row_cp12 WHERE action_id=NEW.action_id AND selection_hash=NEW.selection_hash AND row_index=NEW.row_index)
 BEGIN SELECT RAISE(ABORT,'CP12_REGISTRATION_REPLACE_FORBIDDEN'); END;

-- cp12_registration_no_update
CREATE TRIGGER cp12_registration_no_update BEFORE UPDATE ON research_training_registration_row_cp12
 BEGIN SELECT RAISE(ABORT,'CP12_REGISTRATION_RECEIPT_IMMUTABLE'); END;

-- cp12_revision_adaptive_judge_log_delete
CREATE TRIGGER cp12_revision_adaptive_judge_log_delete AFTER DELETE ON "adaptive_judge_log" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='adaptive_judge_log' AND source_rowid=OLD.rowid; END;

-- cp12_revision_adaptive_judge_log_insert
CREATE TRIGGER cp12_revision_adaptive_judge_log_insert AFTER INSERT ON "adaptive_judge_log" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('adaptive_judge_log',NEW.rowid); END;

-- cp12_revision_adaptive_judge_log_update
CREATE TRIGGER cp12_revision_adaptive_judge_log_update AFTER UPDATE ON "adaptive_judge_log" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='adaptive_judge_log' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('adaptive_judge_log',NEW.rowid); END;

-- cp12_revision_atom_catalog_delete
CREATE TRIGGER cp12_revision_atom_catalog_delete AFTER DELETE ON research_atom_catalog_import_cp12 BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_catalog_import_cp12' AND source_rowid=OLD.rowid; END;

-- cp12_revision_atom_catalog_insert
CREATE TRIGGER cp12_revision_atom_catalog_insert AFTER INSERT ON research_atom_catalog_import_cp12 BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_catalog_import_cp12',NEW.rowid); END;

-- cp12_revision_atom_catalog_update
CREATE TRIGGER cp12_revision_atom_catalog_update AFTER UPDATE ON research_atom_catalog_import_cp12 BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_catalog_import_cp12' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_catalog_import_cp12',NEW.rowid); END;

-- cp12_revision_diagnosis_case_log_delete
CREATE TRIGGER cp12_revision_diagnosis_case_log_delete AFTER DELETE ON "diagnosis_case_log" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='diagnosis_case_log' AND source_rowid=OLD.rowid; END;

-- cp12_revision_diagnosis_case_log_insert
CREATE TRIGGER cp12_revision_diagnosis_case_log_insert AFTER INSERT ON "diagnosis_case_log" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnosis_case_log',NEW.rowid); END;

-- cp12_revision_diagnosis_case_log_update
CREATE TRIGGER cp12_revision_diagnosis_case_log_update AFTER UPDATE ON "diagnosis_case_log" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='diagnosis_case_log' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnosis_case_log',NEW.rowid); END;

-- cp12_revision_diagnosis_fix_plan_delete
CREATE TRIGGER cp12_revision_diagnosis_fix_plan_delete AFTER DELETE ON "diagnosis_fix_plan" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='diagnosis_fix_plan' AND source_rowid=OLD.rowid; END;

-- cp12_revision_diagnosis_fix_plan_insert
CREATE TRIGGER cp12_revision_diagnosis_fix_plan_insert AFTER INSERT ON "diagnosis_fix_plan" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnosis_fix_plan',NEW.rowid); END;

-- cp12_revision_diagnosis_fix_plan_update
CREATE TRIGGER cp12_revision_diagnosis_fix_plan_update AFTER UPDATE ON "diagnosis_fix_plan" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='diagnosis_fix_plan' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnosis_fix_plan',NEW.rowid); END;

-- cp12_revision_dynamic_freeze_matrix_delete
CREATE TRIGGER cp12_revision_dynamic_freeze_matrix_delete AFTER DELETE ON "dynamic_freeze_matrix" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='dynamic_freeze_matrix' AND source_rowid=OLD.rowid; END;

-- cp12_revision_dynamic_freeze_matrix_insert
CREATE TRIGGER cp12_revision_dynamic_freeze_matrix_insert AFTER INSERT ON "dynamic_freeze_matrix" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('dynamic_freeze_matrix',NEW.rowid); END;

-- cp12_revision_dynamic_freeze_matrix_update
CREATE TRIGGER cp12_revision_dynamic_freeze_matrix_update AFTER UPDATE ON "dynamic_freeze_matrix" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='dynamic_freeze_matrix' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('dynamic_freeze_matrix',NEW.rowid); END;

-- cp12_revision_matrix_input_delete
CREATE TRIGGER cp12_revision_matrix_input_delete AFTER DELETE ON research_matrix_input_cp12 BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_matrix_input_cp12' AND source_rowid=OLD.rowid; END;

-- cp12_revision_matrix_input_insert
CREATE TRIGGER cp12_revision_matrix_input_insert AFTER INSERT ON research_matrix_input_cp12 BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_matrix_input_cp12',NEW.rowid); END;

-- cp12_revision_matrix_input_update
CREATE TRIGGER cp12_revision_matrix_input_update AFTER UPDATE ON research_matrix_input_cp12 BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_matrix_input_cp12' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_matrix_input_cp12',NEW.rowid); END;

-- cp12_revision_matrix_spec_delete
CREATE TRIGGER cp12_revision_matrix_spec_delete AFTER DELETE ON research_matrix_spec_cp12 BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_matrix_spec_cp12' AND source_rowid=OLD.rowid; END;

-- cp12_revision_matrix_spec_insert
CREATE TRIGGER cp12_revision_matrix_spec_insert AFTER INSERT ON research_matrix_spec_cp12 BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_matrix_spec_cp12',NEW.rowid); END;

-- cp12_revision_matrix_spec_update
CREATE TRIGGER cp12_revision_matrix_spec_update AFTER UPDATE ON research_matrix_spec_cp12 BEGIN
 SELECT RAISE(ABORT,'CP12_MATRIX_SPEC_IMMUTABLE'); END;

-- cp12_revision_matrix_terminal_delete
CREATE TRIGGER cp12_revision_matrix_terminal_delete AFTER DELETE ON research_matrix_terminal_cp12 BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_matrix_terminal_cp12' AND source_rowid=OLD.rowid; END;

-- cp12_revision_matrix_terminal_insert
CREATE TRIGGER cp12_revision_matrix_terminal_insert AFTER INSERT ON research_matrix_terminal_cp12 BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_matrix_terminal_cp12',NEW.rowid); END;

-- cp12_revision_module_effect_cache_delete
CREATE TRIGGER cp12_revision_module_effect_cache_delete AFTER DELETE ON "module_effect_cache" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='module_effect_cache' AND source_rowid=OLD.rowid; END;

-- cp12_revision_module_effect_cache_insert
CREATE TRIGGER cp12_revision_module_effect_cache_insert AFTER INSERT ON "module_effect_cache" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('module_effect_cache',NEW.rowid); END;

-- cp12_revision_module_effect_cache_update
CREATE TRIGGER cp12_revision_module_effect_cache_update AFTER UPDATE ON "module_effect_cache" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='module_effect_cache' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('module_effect_cache',NEW.rowid); END;

-- cp12_revision_regime5_delete
CREATE TRIGGER cp12_revision_regime5_delete AFTER DELETE ON research_matrix_regime5_cp12 BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_matrix_regime5_cp12' AND source_rowid=OLD.rowid; END;

-- cp12_revision_regime5_insert
CREATE TRIGGER cp12_revision_regime5_insert AFTER INSERT ON research_matrix_regime5_cp12 BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_matrix_regime5_cp12',NEW.rowid); END;

-- cp12_revision_research_formal_observation_cp12_delete
CREATE TRIGGER cp12_revision_research_formal_observation_cp12_delete AFTER DELETE ON "research_formal_observation_cp12" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_formal_observation_cp12' AND source_rowid=OLD.rowid; END;

-- cp12_revision_research_formal_observation_cp12_insert
CREATE TRIGGER cp12_revision_research_formal_observation_cp12_insert AFTER INSERT ON "research_formal_observation_cp12" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_formal_observation_cp12',NEW.rowid); END;

-- cp12_revision_research_formal_observation_cp12_update
CREATE TRIGGER cp12_revision_research_formal_observation_cp12_update AFTER UPDATE ON "research_formal_observation_cp12" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_formal_observation_cp12' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_formal_observation_cp12',NEW.rowid); END;

-- cp12_revision_research_training_feature_cursor_cp12_delete
CREATE TRIGGER cp12_revision_research_training_feature_cursor_cp12_delete AFTER DELETE ON "research_training_feature_cursor_cp12" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_training_feature_cursor_cp12' AND source_rowid=OLD.rowid; END;

-- cp12_revision_research_training_feature_cursor_cp12_insert
CREATE TRIGGER cp12_revision_research_training_feature_cursor_cp12_insert AFTER INSERT ON "research_training_feature_cursor_cp12" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_training_feature_cursor_cp12',NEW.rowid); END;

-- cp12_revision_research_training_feature_cursor_cp12_update
CREATE TRIGGER cp12_revision_research_training_feature_cursor_cp12_update AFTER UPDATE ON "research_training_feature_cursor_cp12" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_training_feature_cursor_cp12' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_training_feature_cursor_cp12',NEW.rowid); END;

-- cp12_revision_research_training_feature_row_cp11_delete
CREATE TRIGGER cp12_revision_research_training_feature_row_cp11_delete AFTER DELETE ON "research_training_feature_row_cp11" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_training_feature_row_cp11' AND source_rowid=OLD.rowid; END;

-- cp12_revision_research_training_feature_row_cp11_insert
CREATE TRIGGER cp12_revision_research_training_feature_row_cp11_insert AFTER INSERT ON "research_training_feature_row_cp11" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_training_feature_row_cp11',NEW.rowid); END;

-- cp12_revision_research_training_feature_row_cp11_update
CREATE TRIGGER cp12_revision_research_training_feature_row_cp11_update AFTER UPDATE ON "research_training_feature_row_cp11" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_training_feature_row_cp11' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_training_feature_row_cp11',NEW.rowid); END;

-- cp12_revision_research_training_registration_row_cp12_delete
CREATE TRIGGER cp12_revision_research_training_registration_row_cp12_delete AFTER DELETE ON "research_training_registration_row_cp12" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_training_registration_row_cp12' AND source_rowid=OLD.rowid; END;

-- cp12_revision_research_training_registration_row_cp12_insert
CREATE TRIGGER cp12_revision_research_training_registration_row_cp12_insert AFTER INSERT ON "research_training_registration_row_cp12" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_training_registration_row_cp12',NEW.rowid); END;

-- cp12_revision_research_training_registration_row_cp12_update
CREATE TRIGGER cp12_revision_research_training_registration_row_cp12_update AFTER UPDATE ON "research_training_registration_row_cp12" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_training_registration_row_cp12' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_training_registration_row_cp12',NEW.rowid); END;

-- cp12_revision_shadow_promotion_cache_delete
CREATE TRIGGER cp12_revision_shadow_promotion_cache_delete AFTER DELETE ON "shadow_promotion_cache" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='shadow_promotion_cache' AND source_rowid=OLD.rowid; END;

-- cp12_revision_shadow_promotion_cache_insert
CREATE TRIGGER cp12_revision_shadow_promotion_cache_insert AFTER INSERT ON "shadow_promotion_cache" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shadow_promotion_cache',NEW.rowid); END;

-- cp12_revision_shadow_promotion_cache_update
CREATE TRIGGER cp12_revision_shadow_promotion_cache_update AFTER UPDATE ON "shadow_promotion_cache" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='shadow_promotion_cache' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shadow_promotion_cache',NEW.rowid); END;

-- cp12_seed_asset_identity
CREATE TRIGGER cp12_seed_asset_identity BEFORE UPDATE ON research_molecule_import_cp12 WHEN OLD.source_hash<>NEW.source_hash OR OLD.asset_name<>NEW.asset_name OR OLD.expected_rows<>NEW.expected_rows BEGIN SELECT RAISE(ABORT,'SEED_ASSET_IDENTITY_IMMUTABLE'); END;

-- cp12_seed_use_delete
CREATE TRIGGER cp12_seed_use_delete BEFORE DELETE ON research_molecule_use_cp12 BEGIN SELECT RAISE(ABORT,'SEED_USE_COMPACT_PERMANENT'); END;

-- cp12_seed_use_identity
CREATE TRIGGER cp12_seed_use_identity BEFORE UPDATE ON research_molecule_use_cp12 WHEN OLD.seed_hash<>NEW.seed_hash OR OLD.ast_hash<>NEW.ast_hash OR OLD.action_id<>NEW.action_id OR OLD.candidate_id<>NEW.candidate_id BEGIN SELECT RAISE(ABORT,'SEED_USE_IDENTITY_IMMUTABLE'); END;

-- cp12_task_expiry_no_delete
CREATE TRIGGER cp12_task_expiry_no_delete BEFORE DELETE ON research_task_retention_expiry_cp12 BEGIN SELECT RAISE(ABORT,'TASK_EXPIRY_PERMANENT'); END;

-- cp12_task_expiry_no_replace
CREATE TRIGGER cp12_task_expiry_no_replace BEFORE INSERT ON research_task_retention_expiry_cp12 WHEN EXISTS(SELECT 1 FROM research_task_retention_expiry_cp12 WHERE task_id=NEW.task_id OR terminal_hash=NEW.terminal_hash) BEGIN SELECT RAISE(ABORT,'TASK_EXPIRY_REPLACE_FORBIDDEN'); END;

-- cp12_task_expiry_no_update
CREATE TRIGGER cp12_task_expiry_no_update BEFORE UPDATE ON research_task_retention_expiry_cp12 BEGIN SELECT RAISE(ABORT,'TASK_EXPIRY_IMMUTABLE'); END;

-- cp12_training_draw_generation_delete
CREATE TRIGGER cp12_training_draw_generation_delete AFTER DELETE ON draws
 BEGIN UPDATE app_state SET state_value=CAST(state_value AS INTEGER)+1,updated_at=0 WHERE state_key='cp12_training_history_generation'; END;

-- cp12_training_draw_generation_insert
CREATE TRIGGER cp12_training_draw_generation_insert AFTER INSERT ON draws
 BEGIN UPDATE app_state SET state_value=CAST(state_value AS INTEGER)+1,updated_at=0 WHERE state_key='cp12_training_history_generation'; END;

-- cp12_training_draw_generation_update
CREATE TRIGGER cp12_training_draw_generation_update AFTER UPDATE ON draws
 BEGIN UPDATE app_state SET state_value=CAST(state_value AS INTEGER)+1,updated_at=0 WHERE state_key='cp12_training_history_generation'; END;

-- cp12_working_cap_research_matrix_checkpoint_v2_insert
CREATE TRIGGER cp12_working_cap_research_matrix_checkpoint_v2_insert BEFORE INSERT ON "research_matrix_checkpoint_v2" BEGIN
 SELECT CASE WHEN COALESCE(length(CAST(NEW."payload" AS BLOB)),0)>262144 THEN RAISE(ABORT,'MATRIX_CHECKPOINT_SINGLE_PAYLOAD_BOUND') END;
 SELECT CASE WHEN COALESCE((SELECT byte_count FROM research_working_storage_cp12 WHERE bucket='MATRIX_CHECKPOINT'),0)+COALESCE(length(CAST(NEW."payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_matrix_checkpoint_v2' AND source_key=NEW.matrix_id),0)>33554432 THEN RAISE(ABORT,'MATRIX_CHECKPOINT_STORAGE_CAPACITY') END; END;

-- cp12_working_cap_research_matrix_checkpoint_v2_update
CREATE TRIGGER cp12_working_cap_research_matrix_checkpoint_v2_update BEFORE UPDATE OF "payload" ON "research_matrix_checkpoint_v2" BEGIN
 SELECT CASE WHEN COALESCE(length(CAST(NEW."payload" AS BLOB)),0)>262144 THEN RAISE(ABORT,'MATRIX_CHECKPOINT_SINGLE_PAYLOAD_BOUND') END;
 SELECT CASE WHEN COALESCE((SELECT byte_count FROM research_working_storage_cp12 WHERE bucket='MATRIX_CHECKPOINT'),0)+COALESCE(length(CAST(NEW."payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_matrix_checkpoint_v2' AND source_key=NEW.matrix_id),0)>33554432 THEN RAISE(ABORT,'MATRIX_CHECKPOINT_STORAGE_CAPACITY') END; END;

-- cp12_working_cap_research_task_checkpoint_v2_insert
CREATE TRIGGER cp12_working_cap_research_task_checkpoint_v2_insert BEFORE INSERT ON "research_task_checkpoint_v2" BEGIN
 SELECT CASE WHEN COALESCE(length(CAST(NEW."partial_payload" AS BLOB)),0)>262144 THEN RAISE(ABORT,'TRAINING_WORKING_SINGLE_PAYLOAD_BOUND') END;
 SELECT CASE WHEN COALESCE((SELECT byte_count FROM research_working_storage_cp12 WHERE bucket='CHECKPOINT'),0)+COALESCE(length(CAST(NEW."partial_payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_task_checkpoint_v2' AND source_key=NEW.task_id),0)>16777216 THEN RAISE(ABORT,'TRAINING_WORKING_STORAGE_CAPACITY') END; END;

-- cp12_working_cap_research_task_checkpoint_v2_update
CREATE TRIGGER cp12_working_cap_research_task_checkpoint_v2_update BEFORE UPDATE OF "partial_payload" ON "research_task_checkpoint_v2" BEGIN
 SELECT CASE WHEN COALESCE(length(CAST(NEW."partial_payload" AS BLOB)),0)>262144 THEN RAISE(ABORT,'TRAINING_WORKING_SINGLE_PAYLOAD_BOUND') END;
 SELECT CASE WHEN COALESCE((SELECT byte_count FROM research_working_storage_cp12 WHERE bucket='CHECKPOINT'),0)+COALESCE(length(CAST(NEW."partial_payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_task_checkpoint_v2' AND source_key=NEW.task_id),0)>16777216 THEN RAISE(ABORT,'TRAINING_WORKING_STORAGE_CAPACITY') END; END;

-- cp12_working_cap_research_training_feature_row_cp11_insert
CREATE TRIGGER cp12_working_cap_research_training_feature_row_cp11_insert BEFORE INSERT ON "research_training_feature_row_cp11" BEGIN
 SELECT CASE WHEN COALESCE(length(CAST(NEW."feature_payload" AS BLOB)),0)>8192 THEN RAISE(ABORT,'TRAINING_WORKING_SINGLE_PAYLOAD_BOUND') END;
 SELECT CASE WHEN COALESCE((SELECT byte_count FROM research_working_storage_cp12 WHERE bucket='FEATURE_REGISTRATION'),0)+COALESCE(length(CAST(NEW."feature_payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_feature_row_cp11' AND source_key=NEW.task_id||':'||NEW.row_index),0)>67108864 THEN RAISE(ABORT,'TRAINING_WORKING_STORAGE_CAPACITY') END; END;

-- cp12_working_cap_research_training_feature_row_cp11_update
CREATE TRIGGER cp12_working_cap_research_training_feature_row_cp11_update BEFORE UPDATE OF "feature_payload" ON "research_training_feature_row_cp11" BEGIN
 SELECT CASE WHEN COALESCE(length(CAST(NEW."feature_payload" AS BLOB)),0)>8192 THEN RAISE(ABORT,'TRAINING_WORKING_SINGLE_PAYLOAD_BOUND') END;
 SELECT CASE WHEN COALESCE((SELECT byte_count FROM research_working_storage_cp12 WHERE bucket='FEATURE_REGISTRATION'),0)+COALESCE(length(CAST(NEW."feature_payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_feature_row_cp11' AND source_key=NEW.task_id||':'||NEW.row_index),0)>67108864 THEN RAISE(ABORT,'TRAINING_WORKING_STORAGE_CAPACITY') END; END;

-- cp12_working_cap_research_training_registration_row_cp12_insert
CREATE TRIGGER cp12_working_cap_research_training_registration_row_cp12_insert BEFORE INSERT ON "research_training_registration_row_cp12" BEGIN
 SELECT CASE WHEN COALESCE(length(CAST(NEW."payload" AS BLOB)),0)>65535 THEN RAISE(ABORT,'TRAINING_WORKING_SINGLE_PAYLOAD_BOUND') END;
 SELECT CASE WHEN COALESCE((SELECT byte_count FROM research_working_storage_cp12 WHERE bucket='FEATURE_REGISTRATION'),0)+COALESCE(length(CAST(NEW."payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_registration_row_cp12' AND source_key=NEW.action_id||':'||NEW.selection_hash||':'||NEW.row_index),0)>67108864 THEN RAISE(ABORT,'TRAINING_WORKING_STORAGE_CAPACITY') END; END;

-- cp12_working_cap_research_training_registration_row_cp12_update
CREATE TRIGGER cp12_working_cap_research_training_registration_row_cp12_update BEFORE UPDATE OF "payload" ON "research_training_registration_row_cp12" BEGIN
 SELECT CASE WHEN COALESCE(length(CAST(NEW."payload" AS BLOB)),0)>65535 THEN RAISE(ABORT,'TRAINING_WORKING_SINGLE_PAYLOAD_BOUND') END;
 SELECT CASE WHEN COALESCE((SELECT byte_count FROM research_working_storage_cp12 WHERE bucket='FEATURE_REGISTRATION'),0)+COALESCE(length(CAST(NEW."payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_registration_row_cp12' AND source_key=NEW.action_id||':'||NEW.selection_hash||':'||NEW.row_index),0)>67108864 THEN RAISE(ABORT,'TRAINING_WORKING_STORAGE_CAPACITY') END; END;

-- cp12_working_checkpoint_identity
CREATE TRIGGER cp12_working_checkpoint_identity BEFORE UPDATE OF task_id ON research_task_checkpoint_v2 WHEN NEW.task_id<>OLD.task_id BEGIN SELECT RAISE(ABORT,'TRAINING_WORKING_IMMUTABLE_KEY'); END;

-- cp12_working_matrix_identity
CREATE TRIGGER cp12_working_matrix_identity BEFORE UPDATE OF matrix_id ON research_matrix_checkpoint_v2 WHEN NEW.matrix_id<>OLD.matrix_id BEGIN SELECT RAISE(ABORT,'MATRIX_CHECKPOINT_IMMUTABLE_KEY'); END;

-- cp12_working_ref_research_matrix_checkpoint_v2_delete
CREATE TRIGGER cp12_working_ref_research_matrix_checkpoint_v2_delete AFTER DELETE ON "research_matrix_checkpoint_v2" BEGIN UPDATE research_working_storage_cp12 SET byte_count=byte_count-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_matrix_checkpoint_v2' AND source_key=OLD.matrix_id),0) WHERE bucket='MATRIX_CHECKPOINT'; DELETE FROM research_working_storage_ref_cp12 WHERE source_table='research_matrix_checkpoint_v2' AND source_key=OLD.matrix_id; END;

-- cp12_working_ref_research_matrix_checkpoint_v2_insert
CREATE TRIGGER cp12_working_ref_research_matrix_checkpoint_v2_insert AFTER INSERT ON "research_matrix_checkpoint_v2" BEGIN
 UPDATE research_working_storage_cp12 SET byte_count=byte_count+COALESCE(length(CAST(NEW."payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_matrix_checkpoint_v2' AND source_key=NEW.matrix_id),0) WHERE bucket='MATRIX_CHECKPOINT';
 INSERT OR REPLACE INTO research_working_storage_ref_cp12(source_table,source_key,bucket,byte_count) VALUES('research_matrix_checkpoint_v2',NEW.matrix_id,'MATRIX_CHECKPOINT',COALESCE(length(CAST(NEW."payload" AS BLOB)),0)); END;

-- cp12_working_ref_research_matrix_checkpoint_v2_update
CREATE TRIGGER cp12_working_ref_research_matrix_checkpoint_v2_update AFTER UPDATE OF "payload" ON "research_matrix_checkpoint_v2" BEGIN
 UPDATE research_working_storage_cp12 SET byte_count=byte_count+COALESCE(length(CAST(NEW."payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_matrix_checkpoint_v2' AND source_key=NEW.matrix_id),0) WHERE bucket='MATRIX_CHECKPOINT';
 INSERT OR REPLACE INTO research_working_storage_ref_cp12(source_table,source_key,bucket,byte_count) VALUES('research_matrix_checkpoint_v2',NEW.matrix_id,'MATRIX_CHECKPOINT',COALESCE(length(CAST(NEW."payload" AS BLOB)),0)); END;

-- cp12_working_ref_research_task_checkpoint_v2_delete
CREATE TRIGGER cp12_working_ref_research_task_checkpoint_v2_delete AFTER DELETE ON "research_task_checkpoint_v2" BEGIN UPDATE research_working_storage_cp12 SET byte_count=byte_count-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_task_checkpoint_v2' AND source_key=OLD.task_id),0) WHERE bucket='CHECKPOINT'; DELETE FROM research_working_storage_ref_cp12 WHERE source_table='research_task_checkpoint_v2' AND source_key=OLD.task_id; END;

-- cp12_working_ref_research_task_checkpoint_v2_insert
CREATE TRIGGER cp12_working_ref_research_task_checkpoint_v2_insert AFTER INSERT ON "research_task_checkpoint_v2" BEGIN
 UPDATE research_working_storage_cp12 SET byte_count=byte_count+COALESCE(length(CAST(NEW."partial_payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_task_checkpoint_v2' AND source_key=NEW.task_id),0) WHERE bucket='CHECKPOINT';
 INSERT OR REPLACE INTO research_working_storage_ref_cp12(source_table,source_key,bucket,byte_count) VALUES('research_task_checkpoint_v2',NEW.task_id,'CHECKPOINT',COALESCE(length(CAST(NEW."partial_payload" AS BLOB)),0)); END;

-- cp12_working_ref_research_task_checkpoint_v2_update
CREATE TRIGGER cp12_working_ref_research_task_checkpoint_v2_update AFTER UPDATE OF "partial_payload" ON "research_task_checkpoint_v2" BEGIN
 UPDATE research_working_storage_cp12 SET byte_count=byte_count+COALESCE(length(CAST(NEW."partial_payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_task_checkpoint_v2' AND source_key=NEW.task_id),0) WHERE bucket='CHECKPOINT';
 INSERT OR REPLACE INTO research_working_storage_ref_cp12(source_table,source_key,bucket,byte_count) VALUES('research_task_checkpoint_v2',NEW.task_id,'CHECKPOINT',COALESCE(length(CAST(NEW."partial_payload" AS BLOB)),0)); END;

-- cp12_working_ref_research_training_feature_row_cp11_delete
CREATE TRIGGER cp12_working_ref_research_training_feature_row_cp11_delete AFTER DELETE ON "research_training_feature_row_cp11" BEGIN UPDATE research_working_storage_cp12 SET byte_count=byte_count-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_feature_row_cp11' AND source_key=OLD.task_id||':'||OLD.row_index),0) WHERE bucket='FEATURE_REGISTRATION'; DELETE FROM research_working_storage_ref_cp12 WHERE source_table='research_training_feature_row_cp11' AND source_key=OLD.task_id||':'||OLD.row_index; END;

-- cp12_working_ref_research_training_feature_row_cp11_insert
CREATE TRIGGER cp12_working_ref_research_training_feature_row_cp11_insert AFTER INSERT ON "research_training_feature_row_cp11" BEGIN
 UPDATE research_working_storage_cp12 SET byte_count=byte_count+COALESCE(length(CAST(NEW."feature_payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_feature_row_cp11' AND source_key=NEW.task_id||':'||NEW.row_index),0) WHERE bucket='FEATURE_REGISTRATION';
 INSERT OR REPLACE INTO research_working_storage_ref_cp12(source_table,source_key,bucket,byte_count) VALUES('research_training_feature_row_cp11',NEW.task_id||':'||NEW.row_index,'FEATURE_REGISTRATION',COALESCE(length(CAST(NEW."feature_payload" AS BLOB)),0)); END;

-- cp12_working_ref_research_training_feature_row_cp11_update
CREATE TRIGGER cp12_working_ref_research_training_feature_row_cp11_update AFTER UPDATE OF "feature_payload" ON "research_training_feature_row_cp11" BEGIN
 UPDATE research_working_storage_cp12 SET byte_count=byte_count+COALESCE(length(CAST(NEW."feature_payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_feature_row_cp11' AND source_key=NEW.task_id||':'||NEW.row_index),0) WHERE bucket='FEATURE_REGISTRATION';
 INSERT OR REPLACE INTO research_working_storage_ref_cp12(source_table,source_key,bucket,byte_count) VALUES('research_training_feature_row_cp11',NEW.task_id||':'||NEW.row_index,'FEATURE_REGISTRATION',COALESCE(length(CAST(NEW."feature_payload" AS BLOB)),0)); END;

-- cp12_working_ref_research_training_registration_row_cp12_delete
CREATE TRIGGER cp12_working_ref_research_training_registration_row_cp12_delete AFTER DELETE ON "research_training_registration_row_cp12" BEGIN UPDATE research_working_storage_cp12 SET byte_count=byte_count-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_registration_row_cp12' AND source_key=OLD.action_id||':'||OLD.selection_hash||':'||OLD.row_index),0) WHERE bucket='FEATURE_REGISTRATION'; DELETE FROM research_working_storage_ref_cp12 WHERE source_table='research_training_registration_row_cp12' AND source_key=OLD.action_id||':'||OLD.selection_hash||':'||OLD.row_index; END;

-- cp12_working_ref_research_training_registration_row_cp12_insert
CREATE TRIGGER cp12_working_ref_research_training_registration_row_cp12_insert AFTER INSERT ON "research_training_registration_row_cp12" BEGIN
 UPDATE research_working_storage_cp12 SET byte_count=byte_count+COALESCE(length(CAST(NEW."payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_registration_row_cp12' AND source_key=NEW.action_id||':'||NEW.selection_hash||':'||NEW.row_index),0) WHERE bucket='FEATURE_REGISTRATION';
 INSERT OR REPLACE INTO research_working_storage_ref_cp12(source_table,source_key,bucket,byte_count) VALUES('research_training_registration_row_cp12',NEW.action_id||':'||NEW.selection_hash||':'||NEW.row_index,'FEATURE_REGISTRATION',COALESCE(length(CAST(NEW."payload" AS BLOB)),0)); END;

-- cp12_working_ref_research_training_registration_row_cp12_update
CREATE TRIGGER cp12_working_ref_research_training_registration_row_cp12_update AFTER UPDATE OF "payload" ON "research_training_registration_row_cp12" BEGIN
 UPDATE research_working_storage_cp12 SET byte_count=byte_count+COALESCE(length(CAST(NEW."payload" AS BLOB)),0)-COALESCE((SELECT byte_count FROM research_working_storage_ref_cp12 WHERE source_table='research_training_registration_row_cp12' AND source_key=NEW.action_id||':'||NEW.selection_hash||':'||NEW.row_index),0) WHERE bucket='FEATURE_REGISTRATION';
 INSERT OR REPLACE INTO research_working_storage_ref_cp12(source_table,source_key,bucket,byte_count) VALUES('research_training_registration_row_cp12',NEW.action_id||':'||NEW.selection_hash||':'||NEW.row_index,'FEATURE_REGISTRATION',COALESCE(length(CAST(NEW."payload" AS BLOB)),0)); END;

-- cp4_binding_no_replace
CREATE TRIGGER cp4_binding_no_replace BEFORE INSERT ON research_cp4_window_binding WHEN EXISTS(SELECT 1 FROM research_cp4_window_binding WHERE window_id=NEW.window_id OR (lifetime_id=NEW.lifetime_id AND stage=NEW.stage) OR plan_hash=NEW.plan_hash) BEGIN SELECT RAISE(ABORT,'CP4_BINDING_REPLACE_FORBIDDEN'); END;

-- cp4_candidate_resource_conflict
CREATE TRIGGER cp4_candidate_resource_conflict BEFORE INSERT ON research_cp4_candidate_resource WHEN EXISTS(SELECT 1 FROM research_cp4_candidate_resource WHERE candidate_key=NEW.candidate_key AND target_issue=NEW.target_issue AND record_hash<>NEW.record_hash) BEGIN SELECT RAISE(ABORT,'CP4_RESOURCE_CONFLICT'); END;

-- cp4_lifetime_no_replace
CREATE TRIGGER cp4_lifetime_no_replace BEFORE INSERT ON research_cp4_frozen_lifetime WHEN EXISTS(SELECT 1 FROM research_cp4_frozen_lifetime WHERE lifetime_id=NEW.lifetime_id OR (candidate_key=NEW.candidate_key AND generation_id=NEW.generation_id) OR hypothesis_id=NEW.hypothesis_id) BEGIN SELECT RAISE(ABORT,'CP4_LIFETIME_REPLACE_FORBIDDEN'); END;

-- cp5_action_identity_immutable
CREATE TRIGGER cp5_action_identity_immutable
 BEFORE UPDATE OF action_id,finding_id,action_type,operator,proposal_json,evidence_hash,seed,parent_hashes_json,protocol_hash,planned_endpoint,alpha_identity
 ON research_action_v2
 BEGIN SELECT RAISE(ABORT,'CP5_RESEARCH_ACTION_IDENTITY_IMMUTABLE'); END;

-- cp5_action_no_replace
CREATE TRIGGER cp5_action_no_replace
 BEFORE INSERT ON research_action_v2
 WHEN EXISTS(SELECT 1 FROM research_action_v2 WHERE action_id=NEW.action_id
 OR (finding_id=NEW.finding_id AND protocol_hash=NEW.protocol_hash AND seed=NEW.seed))
 BEGIN SELECT RAISE(ABORT,'CP5_RESEARCH_ACTION_REPLACE_FORBIDDEN'); END;

-- cp5_input_binding_active_delete
CREATE TRIGGER cp5_input_binding_active_delete
 BEFORE DELETE ON research_task_input_binding_v2
 WHEN EXISTS(SELECT 1 FROM v57_research_task_r967 t
 WHERE t.task_id=OLD.task_id AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))
 BEGIN SELECT RAISE(ABORT,'CP5_ACTIVE_INPUT_BINDING_PROTECTED'); END;

-- cp5_input_binding_immutable
CREATE TRIGGER cp5_input_binding_immutable
 BEFORE UPDATE ON research_task_input_binding_v2
 BEGIN SELECT RAISE(ABORT,'CP5_INPUT_BINDING_IMMUTABLE'); END;

-- cp5_input_binding_no_replace
CREATE TRIGGER cp5_input_binding_no_replace
 BEFORE INSERT ON research_task_input_binding_v2
 WHEN EXISTS(SELECT 1 FROM research_task_input_binding_v2
 WHERE task_id=NEW.task_id AND source_table=NEW.source_table AND source_issue=NEW.source_issue
 AND source_key_hash=NEW.source_key_hash)
 BEGIN SELECT RAISE(ABORT,'CP5_INPUT_BINDING_REPLACE_FORBIDDEN'); END;

-- cp5_result_immutable
CREATE TRIGGER cp5_result_immutable
 BEFORE UPDATE ON research_result_v2
 BEGIN SELECT RAISE(ABORT,'CP5_RESEARCH_RESULT_IMMUTABLE'); END;

-- cp5_result_no_replace
CREATE TRIGGER cp5_result_no_replace
 BEFORE INSERT ON research_result_v2
 WHEN EXISTS(SELECT 1 FROM research_result_v2 WHERE action_id=NEW.action_id AND endpoint_n=NEW.endpoint_n)
 BEGIN SELECT RAISE(ABORT,'CP5_RESEARCH_RESULT_REPLACE_FORBIDDEN'); END;

-- cp5_training_input_immutable
CREATE TRIGGER cp5_training_input_immutable
 BEFORE UPDATE ON research_training_input_v2
 BEGIN SELECT RAISE(ABORT,'CP5_TRAIN_INPUT_IMMUTABLE'); END;

-- cp5_training_input_no_replace
CREATE TRIGGER cp5_training_input_no_replace
 BEFORE INSERT ON research_training_input_v2
 WHEN EXISTS(SELECT 1 FROM research_training_input_v2 WHERE input_hash=NEW.input_hash OR relative_path=NEW.relative_path)
 BEGIN SELECT RAISE(ABORT,'CP5_TRAIN_INPUT_REPLACE_FORBIDDEN'); END;

-- cp5_training_protocol_immutable
CREATE TRIGGER cp5_training_protocol_immutable
 BEFORE UPDATE ON research_training_protocol_v2
 BEGIN SELECT RAISE(ABORT,'CP5_TRAIN_PROTOCOL_IMMUTABLE'); END;

-- cp5_training_protocol_no_delete
CREATE TRIGGER cp5_training_protocol_no_delete
 BEFORE DELETE ON research_training_protocol_v2
 BEGIN SELECT RAISE(ABORT,'CP5_TRAIN_PROTOCOL_PERMANENT'); END;

-- cp5_training_protocol_no_replace
CREATE TRIGGER cp5_training_protocol_no_replace
 BEFORE INSERT ON research_training_protocol_v2
 WHEN EXISTS(SELECT 1 FROM research_training_protocol_v2 WHERE protocol_hash=NEW.protocol_hash OR candidate_id=NEW.candidate_id)
 BEGIN SELECT RAISE(ABORT,'CP5_TRAIN_PROTOCOL_REPLACE_FORBIDDEN'); END;

-- cp8_development_receipt_identity
CREATE TRIGGER cp8_development_receipt_identity
 BEFORE INSERT ON research_development_receipt_v2
 WHEN NOT EXISTS(SELECT 1 FROM research_artifact_identity_v2 a
 JOIN research_training_protocol_v2 p ON p.candidate_id=a.candidate_id
 WHERE a.artifact_hash=NEW.artifact_hash AND p.protocol_hash=NEW.protocol_hash
 AND a.training_kind='DEVELOPMENT_AST3' AND a.validation_pass=1
 AND a.validation_sample_n=p.validation_n AND a.development_sample_n=p.train_n
 AND a.development_receipt_hash=NEW.receipt_hash AND a.validation_hash=NEW.validation_hash)
 BEGIN SELECT RAISE(ABORT,'CP8_DEVELOPMENT_RECEIPT_IDENTITY'); END;

-- cp8_development_receipt_immutable
CREATE TRIGGER cp8_development_receipt_immutable BEFORE UPDATE ON research_development_receipt_v2 BEGIN SELECT RAISE(ABORT,'CP8_DEVELOPMENT_RECEIPT_IMMUTABLE'); END;

-- cp8_development_receipt_no_replace
CREATE TRIGGER cp8_development_receipt_no_replace
 BEFORE INSERT ON research_development_receipt_v2
 WHEN EXISTS(SELECT 1 FROM research_development_receipt_v2
 WHERE receipt_hash=NEW.receipt_hash OR artifact_hash=NEW.artifact_hash OR protocol_hash=NEW.protocol_hash)
 BEGIN SELECT RAISE(ABORT,'CP8_DEVELOPMENT_RECEIPT_DUPLICATE'); END;

-- cp8_development_receipt_permanent
CREATE TRIGGER cp8_development_receipt_permanent BEFORE DELETE ON research_development_receipt_v2 BEGIN SELECT RAISE(ABORT,'CP8_DEVELOPMENT_RECEIPT_PERMANENT'); END;

-- cp8_permit_generation_guard
CREATE TRIGGER cp8_permit_generation_guard
 BEFORE INSERT ON research_retention_permit_v2
 WHEN NOT EXISTS(SELECT 1 FROM research_evidence_archive_member_v2 m
 JOIN research_evidence_archive_v2 a ON a.archive_id=m.archive_id
 WHERE m.archive_id=NEW.archive_id AND m.source_table=NEW.source_table
 AND m.source_rowid=NEW.source_rowid AND m.row_hash=NEW.row_hash
 AND m.source_generation=NEW.source_generation AND a.source_table=NEW.source_table
 AND a.state IN('VERIFIED','REFERENCED','PRUNED')
 AND NEW.source_generation=COALESCE((SELECT generation FROM research_row_revision_v2
 WHERE source_table=NEW.source_table AND source_rowid=NEW.source_rowid),0))
 BEGIN SELECT RAISE(ABORT,'CP8_ARCHIVE_GENERATION_CAS'); END;

-- cp8_predraw_context_immutable
CREATE TRIGGER cp8_predraw_context_immutable
 BEFORE UPDATE OF frozen_context,context_hash ON r958_frozen_predraw
 BEGIN SELECT RAISE(ABORT,'CP8_FROZEN_CONTEXT_IMMUTABLE'); END;

-- cp8_predraw_context_shape
CREATE TRIGGER cp8_predraw_context_shape
 BEFORE INSERT ON r958_frozen_predraw
 WHEN (NEW.context_hash<>'' AND (length(NEW.context_hash)<>64
       OR length(CAST(NEW.frozen_context AS BLOB))>262144 OR NEW.frozen_context=''))
 OR (NEW.context_hash='' AND NEW.frozen_context<>'')
 OR (EXISTS(SELECT 1 FROM r967_frozen_evaluation_v2 f
     WHERE f.candidate_id=NEW.candidate_id AND f.ast LIKE 'AST3:%')
     AND NEW.context_hash='')
 BEGIN SELECT RAISE(ABORT,'CP8_FROZEN_CONTEXT_REQUIRED'); END;

-- cp8_revision_adaptive_judge_sample_delete
CREATE TRIGGER cp8_revision_adaptive_judge_sample_delete
 AFTER DELETE ON "adaptive_judge_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='adaptive_judge_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_adaptive_judge_sample_insert
CREATE TRIGGER cp8_revision_adaptive_judge_sample_insert
 AFTER INSERT ON "adaptive_judge_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('adaptive_judge_sample',NEW.rowid);
 END;

-- cp8_revision_adaptive_judge_sample_update
CREATE TRIGGER cp8_revision_adaptive_judge_sample_update
 AFTER UPDATE ON "adaptive_judge_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='adaptive_judge_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('adaptive_judge_sample',NEW.rowid);
 END;

-- cp8_revision_ai_judge_log_delete
CREATE TRIGGER cp8_revision_ai_judge_log_delete
 AFTER DELETE ON "ai_judge_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='ai_judge_log' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_ai_judge_log_insert
CREATE TRIGGER cp8_revision_ai_judge_log_insert
 AFTER INSERT ON "ai_judge_log" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('ai_judge_log',NEW.rowid);
 END;

-- cp8_revision_ai_judge_log_update
CREATE TRIGGER cp8_revision_ai_judge_log_update
 AFTER UPDATE ON "ai_judge_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='ai_judge_log' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('ai_judge_log',NEW.rowid);
 END;

-- cp8_revision_atomic_tool_context_r943_delete
CREATE TRIGGER cp8_revision_atomic_tool_context_r943_delete
 AFTER DELETE ON "atomic_tool_context_r943" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='atomic_tool_context_r943' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_atomic_tool_context_r943_insert
CREATE TRIGGER cp8_revision_atomic_tool_context_r943_insert
 AFTER INSERT ON "atomic_tool_context_r943" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('atomic_tool_context_r943',NEW.rowid);
 END;

-- cp8_revision_atomic_tool_context_r943_update
CREATE TRIGGER cp8_revision_atomic_tool_context_r943_update
 AFTER UPDATE ON "atomic_tool_context_r943" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='atomic_tool_context_r943' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('atomic_tool_context_r943',NEW.rowid);
 END;

-- cp8_revision_atomic_tool_network_r943_delete
CREATE TRIGGER cp8_revision_atomic_tool_network_r943_delete
 AFTER DELETE ON "atomic_tool_network_r943" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='atomic_tool_network_r943' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_atomic_tool_network_r943_insert
CREATE TRIGGER cp8_revision_atomic_tool_network_r943_insert
 AFTER INSERT ON "atomic_tool_network_r943" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('atomic_tool_network_r943',NEW.rowid);
 END;

-- cp8_revision_atomic_tool_network_r943_update
CREATE TRIGGER cp8_revision_atomic_tool_network_r943_update
 AFTER UPDATE ON "atomic_tool_network_r943" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='atomic_tool_network_r943' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('atomic_tool_network_r943',NEW.rowid);
 END;

-- cp8_revision_atomic_tool_quality_r943_delete
CREATE TRIGGER cp8_revision_atomic_tool_quality_r943_delete
 AFTER DELETE ON "atomic_tool_quality_r943" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='atomic_tool_quality_r943' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_atomic_tool_quality_r943_insert
CREATE TRIGGER cp8_revision_atomic_tool_quality_r943_insert
 AFTER INSERT ON "atomic_tool_quality_r943" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('atomic_tool_quality_r943',NEW.rowid);
 END;

-- cp8_revision_atomic_tool_quality_r943_update
CREATE TRIGGER cp8_revision_atomic_tool_quality_r943_update
 AFTER UPDATE ON "atomic_tool_quality_r943" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='atomic_tool_quality_r943' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('atomic_tool_quality_r943',NEW.rowid);
 END;

-- cp8_revision_auto_learn_cursor_delete
CREATE TRIGGER cp8_revision_auto_learn_cursor_delete
 AFTER DELETE ON "auto_learn_cursor" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='auto_learn_cursor' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_auto_learn_cursor_insert
CREATE TRIGGER cp8_revision_auto_learn_cursor_insert
 AFTER INSERT ON "auto_learn_cursor" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('auto_learn_cursor',NEW.rowid);
 END;

-- cp8_revision_auto_learn_cursor_update
CREATE TRIGGER cp8_revision_auto_learn_cursor_update
 AFTER UPDATE ON "auto_learn_cursor" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='auto_learn_cursor' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('auto_learn_cursor',NEW.rowid);
 END;

-- cp8_revision_b03_action_event_delete
CREATE TRIGGER cp8_revision_b03_action_event_delete
 AFTER DELETE ON "b03_action_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_action_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_action_event_insert
CREATE TRIGGER cp8_revision_b03_action_event_insert
 AFTER INSERT ON "b03_action_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_action_event',NEW.rowid);
 END;

-- cp8_revision_b03_action_event_update
CREATE TRIGGER cp8_revision_b03_action_event_update
 AFTER UPDATE ON "b03_action_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_action_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_action_event',NEW.rowid);
 END;

-- cp8_revision_b03_authorization_event_delete
CREATE TRIGGER cp8_revision_b03_authorization_event_delete
 AFTER DELETE ON "b03_authorization_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_authorization_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_authorization_event_insert
CREATE TRIGGER cp8_revision_b03_authorization_event_insert
 AFTER INSERT ON "b03_authorization_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_authorization_event',NEW.rowid);
 END;

-- cp8_revision_b03_authorization_event_update
CREATE TRIGGER cp8_revision_b03_authorization_event_update
 AFTER UPDATE ON "b03_authorization_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_authorization_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_authorization_event',NEW.rowid);
 END;

-- cp8_revision_b03_draw_quarantine_delete
CREATE TRIGGER cp8_revision_b03_draw_quarantine_delete
 AFTER DELETE ON "b03_draw_quarantine" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_draw_quarantine' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_draw_quarantine_insert
CREATE TRIGGER cp8_revision_b03_draw_quarantine_insert
 AFTER INSERT ON "b03_draw_quarantine" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_draw_quarantine',NEW.rowid);
 END;

-- cp8_revision_b03_draw_quarantine_update
CREATE TRIGGER cp8_revision_b03_draw_quarantine_update
 AFTER UPDATE ON "b03_draw_quarantine" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_draw_quarantine' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_draw_quarantine',NEW.rowid);
 END;

-- cp8_revision_b03_integrity_snapshot_delete
CREATE TRIGGER cp8_revision_b03_integrity_snapshot_delete
 AFTER DELETE ON "b03_integrity_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_integrity_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_integrity_snapshot_insert
CREATE TRIGGER cp8_revision_b03_integrity_snapshot_insert
 AFTER INSERT ON "b03_integrity_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_integrity_snapshot',NEW.rowid);
 END;

-- cp8_revision_b03_integrity_snapshot_update
CREATE TRIGGER cp8_revision_b03_integrity_snapshot_update
 AFTER UPDATE ON "b03_integrity_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_integrity_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_integrity_snapshot',NEW.rowid);
 END;

-- cp8_revision_b03_number_event_delete
CREATE TRIGGER cp8_revision_b03_number_event_delete
 AFTER DELETE ON "b03_number_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_number_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_number_event_insert
CREATE TRIGGER cp8_revision_b03_number_event_insert
 AFTER INSERT ON "b03_number_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_number_event',NEW.rowid);
 END;

-- cp8_revision_b03_number_event_update
CREATE TRIGGER cp8_revision_b03_number_event_update
 AFTER UPDATE ON "b03_number_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_number_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_number_event',NEW.rowid);
 END;

-- cp8_revision_b03_permutation_event_delete
CREATE TRIGGER cp8_revision_b03_permutation_event_delete
 AFTER DELETE ON "b03_permutation_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_permutation_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_permutation_event_insert
CREATE TRIGGER cp8_revision_b03_permutation_event_insert
 AFTER INSERT ON "b03_permutation_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_permutation_event',NEW.rowid);
 END;

-- cp8_revision_b03_permutation_event_update
CREATE TRIGGER cp8_revision_b03_permutation_event_update
 AFTER UPDATE ON "b03_permutation_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_permutation_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_permutation_event',NEW.rowid);
 END;

-- cp8_revision_b03_prediction_bundle_delete
CREATE TRIGGER cp8_revision_b03_prediction_bundle_delete
 AFTER DELETE ON "b03_prediction_bundle" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_prediction_bundle' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_prediction_bundle_insert
CREATE TRIGGER cp8_revision_b03_prediction_bundle_insert
 AFTER INSERT ON "b03_prediction_bundle" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_prediction_bundle',NEW.rowid);
 END;

-- cp8_revision_b03_prediction_bundle_update
CREATE TRIGGER cp8_revision_b03_prediction_bundle_update
 AFTER UPDATE ON "b03_prediction_bundle" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_prediction_bundle' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_prediction_bundle',NEW.rowid);
 END;

-- cp8_revision_b03_replay_audit_delete
CREATE TRIGGER cp8_revision_b03_replay_audit_delete
 AFTER DELETE ON "b03_replay_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_replay_audit' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_replay_audit_insert
CREATE TRIGGER cp8_revision_b03_replay_audit_insert
 AFTER INSERT ON "b03_replay_audit" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_replay_audit',NEW.rowid);
 END;

-- cp8_revision_b03_replay_audit_update
CREATE TRIGGER cp8_revision_b03_replay_audit_update
 AFTER UPDATE ON "b03_replay_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_replay_audit' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_replay_audit',NEW.rowid);
 END;

-- cp8_revision_b03_selected_event_delete
CREATE TRIGGER cp8_revision_b03_selected_event_delete
 AFTER DELETE ON "b03_selected_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_selected_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_selected_event_insert
CREATE TRIGGER cp8_revision_b03_selected_event_insert
 AFTER INSERT ON "b03_selected_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_selected_event',NEW.rowid);
 END;

-- cp8_revision_b03_selected_event_update
CREATE TRIGGER cp8_revision_b03_selected_event_update
 AFTER UPDATE ON "b03_selected_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_selected_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_selected_event',NEW.rowid);
 END;

-- cp8_revision_b03_settlement_event_delete
CREATE TRIGGER cp8_revision_b03_settlement_event_delete
 AFTER DELETE ON "b03_settlement_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_settlement_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_settlement_event_insert
CREATE TRIGGER cp8_revision_b03_settlement_event_insert
 AFTER INSERT ON "b03_settlement_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_settlement_event',NEW.rowid);
 END;

-- cp8_revision_b03_settlement_event_update
CREATE TRIGGER cp8_revision_b03_settlement_event_update
 AFTER UPDATE ON "b03_settlement_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_settlement_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_settlement_event',NEW.rowid);
 END;

-- cp8_revision_b03_transaction_audit_delete
CREATE TRIGGER cp8_revision_b03_transaction_audit_delete
 AFTER DELETE ON "b03_transaction_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_transaction_audit' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_b03_transaction_audit_insert
CREATE TRIGGER cp8_revision_b03_transaction_audit_insert
 AFTER INSERT ON "b03_transaction_audit" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_transaction_audit',NEW.rowid);
 END;

-- cp8_revision_b03_transaction_audit_update
CREATE TRIGGER cp8_revision_b03_transaction_audit_update
 AFTER UPDATE ON "b03_transaction_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='b03_transaction_audit' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('b03_transaction_audit',NEW.rowid);
 END;

-- cp8_revision_backtest_report_delete
CREATE TRIGGER cp8_revision_backtest_report_delete
 AFTER DELETE ON "backtest_report" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='backtest_report' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_backtest_report_insert
CREATE TRIGGER cp8_revision_backtest_report_insert
 AFTER INSERT ON "backtest_report" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('backtest_report',NEW.rowid);
 END;

-- cp8_revision_backtest_report_update
CREATE TRIGGER cp8_revision_backtest_report_update
 AFTER UPDATE ON "backtest_report" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='backtest_report' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('backtest_report',NEW.rowid);
 END;

-- cp8_revision_bias_evidence_delete
CREATE TRIGGER cp8_revision_bias_evidence_delete
 AFTER DELETE ON "bias_evidence" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='bias_evidence' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_bias_evidence_insert
CREATE TRIGGER cp8_revision_bias_evidence_insert
 AFTER INSERT ON "bias_evidence" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('bias_evidence',NEW.rowid);
 END;

-- cp8_revision_bias_evidence_update
CREATE TRIGGER cp8_revision_bias_evidence_update
 AFTER UPDATE ON "bias_evidence" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='bias_evidence' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('bias_evidence',NEW.rowid);
 END;

-- cp8_revision_break_memory_delete
CREATE TRIGGER cp8_revision_break_memory_delete
 AFTER DELETE ON "break_memory" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='break_memory' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_break_memory_insert
CREATE TRIGGER cp8_revision_break_memory_insert
 AFTER INSERT ON "break_memory" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('break_memory',NEW.rowid);
 END;

-- cp8_revision_break_memory_update
CREATE TRIGGER cp8_revision_break_memory_update
 AFTER UPDATE ON "break_memory" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='break_memory' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('break_memory',NEW.rowid);
 END;

-- cp8_revision_calibration_bucket_delete
CREATE TRIGGER cp8_revision_calibration_bucket_delete
 AFTER DELETE ON "calibration_bucket" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='calibration_bucket' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_calibration_bucket_insert
CREATE TRIGGER cp8_revision_calibration_bucket_insert
 AFTER INSERT ON "calibration_bucket" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('calibration_bucket',NEW.rowid);
 END;

-- cp8_revision_calibration_bucket_update
CREATE TRIGGER cp8_revision_calibration_bucket_update
 AFTER UPDATE ON "calibration_bucket" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='calibration_bucket' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('calibration_bucket',NEW.rowid);
 END;

-- cp8_revision_candidate_audit_delete
CREATE TRIGGER cp8_revision_candidate_audit_delete
 AFTER DELETE ON "candidate_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='candidate_audit' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_candidate_audit_insert
CREATE TRIGGER cp8_revision_candidate_audit_insert
 AFTER INSERT ON "candidate_audit" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('candidate_audit',NEW.rowid);
 END;

-- cp8_revision_candidate_audit_update
CREATE TRIGGER cp8_revision_candidate_audit_update
 AFTER UPDATE ON "candidate_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='candidate_audit' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('candidate_audit',NEW.rowid);
 END;

-- cp8_revision_candidate_evidence_delete
CREATE TRIGGER cp8_revision_candidate_evidence_delete
 AFTER DELETE ON "candidate_evidence" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='candidate_evidence' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_candidate_evidence_insert
CREATE TRIGGER cp8_revision_candidate_evidence_insert
 AFTER INSERT ON "candidate_evidence" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('candidate_evidence',NEW.rowid);
 END;

-- cp8_revision_candidate_evidence_update
CREATE TRIGGER cp8_revision_candidate_evidence_update
 AFTER UPDATE ON "candidate_evidence" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='candidate_evidence' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('candidate_evidence',NEW.rowid);
 END;

-- cp8_revision_causal_factor_matrix_delete
CREATE TRIGGER cp8_revision_causal_factor_matrix_delete
 AFTER DELETE ON "causal_factor_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='causal_factor_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_causal_factor_matrix_insert
CREATE TRIGGER cp8_revision_causal_factor_matrix_insert
 AFTER INSERT ON "causal_factor_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('causal_factor_matrix',NEW.rowid);
 END;

-- cp8_revision_causal_factor_matrix_update
CREATE TRIGGER cp8_revision_causal_factor_matrix_update
 AFTER UPDATE ON "causal_factor_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='causal_factor_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('causal_factor_matrix',NEW.rowid);
 END;

-- cp8_revision_causal_research_feedback_r947_delete
CREATE TRIGGER cp8_revision_causal_research_feedback_r947_delete
 AFTER DELETE ON "causal_research_feedback_r947" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='causal_research_feedback_r947' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_causal_research_feedback_r947_insert
CREATE TRIGGER cp8_revision_causal_research_feedback_r947_insert
 AFTER INSERT ON "causal_research_feedback_r947" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('causal_research_feedback_r947',NEW.rowid);
 END;

-- cp8_revision_causal_research_feedback_r947_update
CREATE TRIGGER cp8_revision_causal_research_feedback_r947_update
 AFTER UPDATE ON "causal_research_feedback_r947" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='causal_research_feedback_r947' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('causal_research_feedback_r947',NEW.rowid);
 END;

-- cp8_revision_cause_ablation_sample_delete
CREATE TRIGGER cp8_revision_cause_ablation_sample_delete
 AFTER DELETE ON "cause_ablation_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='cause_ablation_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_cause_ablation_sample_insert
CREATE TRIGGER cp8_revision_cause_ablation_sample_insert
 AFTER INSERT ON "cause_ablation_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('cause_ablation_sample',NEW.rowid);
 END;

-- cp8_revision_cause_ablation_sample_update
CREATE TRIGGER cp8_revision_cause_ablation_sample_update
 AFTER UPDATE ON "cause_ablation_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='cause_ablation_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('cause_ablation_sample',NEW.rowid);
 END;

-- cp8_revision_combo_memory_delete
CREATE TRIGGER cp8_revision_combo_memory_delete
 AFTER DELETE ON "combo_memory" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='combo_memory' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_combo_memory_insert
CREATE TRIGGER cp8_revision_combo_memory_insert
 AFTER INSERT ON "combo_memory" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('combo_memory',NEW.rowid);
 END;

-- cp8_revision_combo_memory_update
CREATE TRIGGER cp8_revision_combo_memory_update
 AFTER UPDATE ON "combo_memory" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='combo_memory' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('combo_memory',NEW.rowid);
 END;

-- cp8_revision_combo_role_profile_delete
CREATE TRIGGER cp8_revision_combo_role_profile_delete
 AFTER DELETE ON "combo_role_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='combo_role_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_combo_role_profile_insert
CREATE TRIGGER cp8_revision_combo_role_profile_insert
 AFTER INSERT ON "combo_role_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('combo_role_profile',NEW.rowid);
 END;

-- cp8_revision_combo_role_profile_update
CREATE TRIGGER cp8_revision_combo_role_profile_update
 AFTER UPDATE ON "combo_role_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='combo_role_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('combo_role_profile',NEW.rowid);
 END;

-- cp8_revision_counterfactual_rank_delete
CREATE TRIGGER cp8_revision_counterfactual_rank_delete
 AFTER DELETE ON "counterfactual_rank" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='counterfactual_rank' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_counterfactual_rank_insert
CREATE TRIGGER cp8_revision_counterfactual_rank_insert
 AFTER INSERT ON "counterfactual_rank" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('counterfactual_rank',NEW.rowid);
 END;

-- cp8_revision_counterfactual_rank_update
CREATE TRIGGER cp8_revision_counterfactual_rank_update
 AFTER UPDATE ON "counterfactual_rank" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='counterfactual_rank' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('counterfactual_rank',NEW.rowid);
 END;

-- cp8_revision_counterfactual_reason_sample_delete
CREATE TRIGGER cp8_revision_counterfactual_reason_sample_delete
 AFTER DELETE ON "counterfactual_reason_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='counterfactual_reason_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_counterfactual_reason_sample_insert
CREATE TRIGGER cp8_revision_counterfactual_reason_sample_insert
 AFTER INSERT ON "counterfactual_reason_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('counterfactual_reason_sample',NEW.rowid);
 END;

-- cp8_revision_counterfactual_reason_sample_update
CREATE TRIGGER cp8_revision_counterfactual_reason_sample_update
 AFTER UPDATE ON "counterfactual_reason_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='counterfactual_reason_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('counterfactual_reason_sample',NEW.rowid);
 END;

-- cp8_revision_decision_audit_delete
CREATE TRIGGER cp8_revision_decision_audit_delete
 AFTER DELETE ON "decision_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='decision_audit' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_decision_audit_insert
CREATE TRIGGER cp8_revision_decision_audit_insert
 AFTER INSERT ON "decision_audit" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('decision_audit',NEW.rowid);
 END;

-- cp8_revision_decision_audit_update
CREATE TRIGGER cp8_revision_decision_audit_update
 AFTER UPDATE ON "decision_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='decision_audit' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('decision_audit',NEW.rowid);
 END;

-- cp8_revision_decision_authorization_v28_delete
CREATE TRIGGER cp8_revision_decision_authorization_v28_delete
 AFTER DELETE ON "decision_authorization_v28" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='decision_authorization_v28' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_decision_authorization_v28_insert
CREATE TRIGGER cp8_revision_decision_authorization_v28_insert
 AFTER INSERT ON "decision_authorization_v28" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('decision_authorization_v28',NEW.rowid);
 END;

-- cp8_revision_decision_authorization_v28_update
CREATE TRIGGER cp8_revision_decision_authorization_v28_update
 AFTER UPDATE ON "decision_authorization_v28" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='decision_authorization_v28' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('decision_authorization_v28',NEW.rowid);
 END;

-- cp8_revision_decisions_delete
CREATE TRIGGER cp8_revision_decisions_delete
 AFTER DELETE ON "decisions" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='decisions' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_decisions_insert
CREATE TRIGGER cp8_revision_decisions_insert
 AFTER INSERT ON "decisions" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('decisions',NEW.rowid);
 END;

-- cp8_revision_decisions_update
CREATE TRIGGER cp8_revision_decisions_update
 AFTER UPDATE ON "decisions" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='decisions' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('decisions',NEW.rowid);
 END;

-- cp8_revision_deep_attribution_delete
CREATE TRIGGER cp8_revision_deep_attribution_delete
 AFTER DELETE ON "deep_attribution" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='deep_attribution' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_deep_attribution_insert
CREATE TRIGGER cp8_revision_deep_attribution_insert
 AFTER INSERT ON "deep_attribution" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('deep_attribution',NEW.rowid);
 END;

-- cp8_revision_deep_attribution_update
CREATE TRIGGER cp8_revision_deep_attribution_update
 AFTER UPDATE ON "deep_attribution" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='deep_attribution' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('deep_attribution',NEW.rowid);
 END;

-- cp8_revision_deep_cycle_delete
CREATE TRIGGER cp8_revision_deep_cycle_delete
 AFTER DELETE ON "deep_cycle" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='deep_cycle' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_deep_cycle_insert
CREATE TRIGGER cp8_revision_deep_cycle_insert
 AFTER INSERT ON "deep_cycle" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('deep_cycle',NEW.rowid);
 END;

-- cp8_revision_deep_cycle_update
CREATE TRIGGER cp8_revision_deep_cycle_update
 AFTER UPDATE ON "deep_cycle" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='deep_cycle' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('deep_cycle',NEW.rowid);
 END;

-- cp8_revision_deep_profile_delete
CREATE TRIGGER cp8_revision_deep_profile_delete
 AFTER DELETE ON "deep_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='deep_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_deep_profile_insert
CREATE TRIGGER cp8_revision_deep_profile_insert
 AFTER INSERT ON "deep_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('deep_profile',NEW.rowid);
 END;

-- cp8_revision_deep_profile_update
CREATE TRIGGER cp8_revision_deep_profile_update
 AFTER UPDATE ON "deep_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='deep_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('deep_profile',NEW.rowid);
 END;

-- cp8_revision_deep_transition_delete
CREATE TRIGGER cp8_revision_deep_transition_delete
 AFTER DELETE ON "deep_transition" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='deep_transition' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_deep_transition_insert
CREATE TRIGGER cp8_revision_deep_transition_insert
 AFTER INSERT ON "deep_transition" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('deep_transition',NEW.rowid);
 END;

-- cp8_revision_deep_transition_update
CREATE TRIGGER cp8_revision_deep_transition_update
 AFTER UPDATE ON "deep_transition" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='deep_transition' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('deep_transition',NEW.rowid);
 END;

-- cp8_revision_diagnostic_log_delete
CREATE TRIGGER cp8_revision_diagnostic_log_delete
 AFTER DELETE ON "diagnostic_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_diagnostic_log_insert
CREATE TRIGGER cp8_revision_diagnostic_log_insert
 AFTER INSERT ON "diagnostic_log" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnostic_log',NEW.rowid);
 END;

-- cp8_revision_diagnostic_log_update
CREATE TRIGGER cp8_revision_diagnostic_log_update
 AFTER UPDATE ON "diagnostic_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnostic_log',NEW.rowid);
 END;

-- cp8_revision_draws_delete
CREATE TRIGGER cp8_revision_draws_delete
 AFTER DELETE ON "draws" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='draws' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_draws_insert
CREATE TRIGGER cp8_revision_draws_insert
 AFTER INSERT ON "draws" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('draws',NEW.rowid);
 END;

-- cp8_revision_draws_update
CREATE TRIGGER cp8_revision_draws_update
 AFTER UPDATE ON "draws" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='draws' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('draws',NEW.rowid);
 END;

-- cp8_revision_dynamic_cause_chain_delete
CREATE TRIGGER cp8_revision_dynamic_cause_chain_delete
 AFTER DELETE ON "dynamic_cause_chain" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='dynamic_cause_chain' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_dynamic_cause_chain_insert
CREATE TRIGGER cp8_revision_dynamic_cause_chain_insert
 AFTER INSERT ON "dynamic_cause_chain" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('dynamic_cause_chain',NEW.rowid);
 END;

-- cp8_revision_dynamic_cause_chain_update
CREATE TRIGGER cp8_revision_dynamic_cause_chain_update
 AFTER UPDATE ON "dynamic_cause_chain" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='dynamic_cause_chain' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('dynamic_cause_chain',NEW.rowid);
 END;

-- cp8_revision_dynamic_domain_stat_v20_delete
CREATE TRIGGER cp8_revision_dynamic_domain_stat_v20_delete
 AFTER DELETE ON "dynamic_domain_stat_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='dynamic_domain_stat_v20' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_dynamic_domain_stat_v20_insert
CREATE TRIGGER cp8_revision_dynamic_domain_stat_v20_insert
 AFTER INSERT ON "dynamic_domain_stat_v20" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('dynamic_domain_stat_v20',NEW.rowid);
 END;

-- cp8_revision_dynamic_domain_stat_v20_update
CREATE TRIGGER cp8_revision_dynamic_domain_stat_v20_update
 AFTER UPDATE ON "dynamic_domain_stat_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='dynamic_domain_stat_v20' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('dynamic_domain_stat_v20',NEW.rowid);
 END;

-- cp8_revision_dynamic_window_cluster_delete
CREATE TRIGGER cp8_revision_dynamic_window_cluster_delete
 AFTER DELETE ON "dynamic_window_cluster" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='dynamic_window_cluster' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_dynamic_window_cluster_insert
CREATE TRIGGER cp8_revision_dynamic_window_cluster_insert
 AFTER INSERT ON "dynamic_window_cluster" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('dynamic_window_cluster',NEW.rowid);
 END;

-- cp8_revision_dynamic_window_cluster_update
CREATE TRIGGER cp8_revision_dynamic_window_cluster_update
 AFTER UPDATE ON "dynamic_window_cluster" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='dynamic_window_cluster' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('dynamic_window_cluster',NEW.rowid);
 END;

-- cp8_revision_elite45_audit_log_delete
CREATE TRIGGER cp8_revision_elite45_audit_log_delete
 AFTER DELETE ON "elite45_audit_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='elite45_audit_log' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_elite45_audit_log_insert
CREATE TRIGGER cp8_revision_elite45_audit_log_insert
 AFTER INSERT ON "elite45_audit_log" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('elite45_audit_log',NEW.rowid);
 END;

-- cp8_revision_elite45_audit_log_update
CREATE TRIGGER cp8_revision_elite45_audit_log_update
 AFTER UPDATE ON "elite45_audit_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='elite45_audit_log' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('elite45_audit_log',NEW.rowid);
 END;

-- cp8_revision_false_strong_freeze_delete
CREATE TRIGGER cp8_revision_false_strong_freeze_delete
 AFTER DELETE ON "false_strong_freeze" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='false_strong_freeze' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_false_strong_freeze_insert
CREATE TRIGGER cp8_revision_false_strong_freeze_insert
 AFTER INSERT ON "false_strong_freeze" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('false_strong_freeze',NEW.rowid);
 END;

-- cp8_revision_false_strong_freeze_update
CREATE TRIGGER cp8_revision_false_strong_freeze_update
 AFTER UPDATE ON "false_strong_freeze" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='false_strong_freeze' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('false_strong_freeze',NEW.rowid);
 END;

-- cp8_revision_false_strong_profile_delete
CREATE TRIGGER cp8_revision_false_strong_profile_delete
 AFTER DELETE ON "false_strong_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='false_strong_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_false_strong_profile_insert
CREATE TRIGGER cp8_revision_false_strong_profile_insert
 AFTER INSERT ON "false_strong_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('false_strong_profile',NEW.rowid);
 END;

-- cp8_revision_false_strong_profile_update
CREATE TRIGGER cp8_revision_false_strong_profile_update
 AFTER UPDATE ON "false_strong_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='false_strong_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('false_strong_profile',NEW.rowid);
 END;

-- cp8_revision_forward_validation_stat_v20_delete
CREATE TRIGGER cp8_revision_forward_validation_stat_v20_delete
 AFTER DELETE ON "forward_validation_stat_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='forward_validation_stat_v20' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_forward_validation_stat_v20_insert
CREATE TRIGGER cp8_revision_forward_validation_stat_v20_insert
 AFTER INSERT ON "forward_validation_stat_v20" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('forward_validation_stat_v20',NEW.rowid);
 END;

-- cp8_revision_forward_validation_stat_v20_update
CREATE TRIGGER cp8_revision_forward_validation_stat_v20_update
 AFTER UPDATE ON "forward_validation_stat_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='forward_validation_stat_v20' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('forward_validation_stat_v20',NEW.rowid);
 END;

-- cp8_revision_full_candidate_scan_delete
CREATE TRIGGER cp8_revision_full_candidate_scan_delete
 AFTER DELETE ON "full_candidate_scan" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='full_candidate_scan' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_full_candidate_scan_insert
CREATE TRIGGER cp8_revision_full_candidate_scan_insert
 AFTER INSERT ON "full_candidate_scan" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('full_candidate_scan',NEW.rowid);
 END;

-- cp8_revision_full_candidate_scan_update
CREATE TRIGGER cp8_revision_full_candidate_scan_update
 AFTER UPDATE ON "full_candidate_scan" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='full_candidate_scan' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('full_candidate_scan',NEW.rowid);
 END;

-- cp8_revision_hierarchical_posterior_v28_delete
CREATE TRIGGER cp8_revision_hierarchical_posterior_v28_delete
 AFTER DELETE ON "hierarchical_posterior_v28" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='hierarchical_posterior_v28' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_hierarchical_posterior_v28_insert
CREATE TRIGGER cp8_revision_hierarchical_posterior_v28_insert
 AFTER INSERT ON "hierarchical_posterior_v28" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('hierarchical_posterior_v28',NEW.rowid);
 END;

-- cp8_revision_hierarchical_posterior_v28_update
CREATE TRIGGER cp8_revision_hierarchical_posterior_v28_update
 AFTER UPDATE ON "hierarchical_posterior_v28" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='hierarchical_posterior_v28' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('hierarchical_posterior_v28',NEW.rowid);
 END;

-- cp8_revision_learning_case_log_delete
CREATE TRIGGER cp8_revision_learning_case_log_delete
 AFTER DELETE ON "learning_case_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='learning_case_log' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_learning_case_log_insert
CREATE TRIGGER cp8_revision_learning_case_log_insert
 AFTER INSERT ON "learning_case_log" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('learning_case_log',NEW.rowid);
 END;

-- cp8_revision_learning_case_log_update
CREATE TRIGGER cp8_revision_learning_case_log_update
 AFTER UPDATE ON "learning_case_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='learning_case_log' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('learning_case_log',NEW.rowid);
 END;

-- cp8_revision_learning_snapshot_delete
CREATE TRIGGER cp8_revision_learning_snapshot_delete
 AFTER DELETE ON "learning_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='learning_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_learning_snapshot_insert
CREATE TRIGGER cp8_revision_learning_snapshot_insert
 AFTER INSERT ON "learning_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('learning_snapshot',NEW.rowid);
 END;

-- cp8_revision_learning_snapshot_update
CREATE TRIGGER cp8_revision_learning_snapshot_update
 AFTER UPDATE ON "learning_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='learning_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('learning_snapshot',NEW.rowid);
 END;

-- cp8_revision_main_chain_candidate_snapshot_delete
CREATE TRIGGER cp8_revision_main_chain_candidate_snapshot_delete
 AFTER DELETE ON "main_chain_candidate_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='main_chain_candidate_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_main_chain_candidate_snapshot_insert
CREATE TRIGGER cp8_revision_main_chain_candidate_snapshot_insert
 AFTER INSERT ON "main_chain_candidate_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('main_chain_candidate_snapshot',NEW.rowid);
 END;

-- cp8_revision_main_chain_candidate_snapshot_update
CREATE TRIGGER cp8_revision_main_chain_candidate_snapshot_update
 AFTER UPDATE ON "main_chain_candidate_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='main_chain_candidate_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('main_chain_candidate_snapshot',NEW.rowid);
 END;

-- cp8_revision_main_chain_reject_bucket_delete
CREATE TRIGGER cp8_revision_main_chain_reject_bucket_delete
 AFTER DELETE ON "main_chain_reject_bucket" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='main_chain_reject_bucket' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_main_chain_reject_bucket_insert
CREATE TRIGGER cp8_revision_main_chain_reject_bucket_insert
 AFTER INSERT ON "main_chain_reject_bucket" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('main_chain_reject_bucket',NEW.rowid);
 END;

-- cp8_revision_main_chain_reject_bucket_update
CREATE TRIGGER cp8_revision_main_chain_reject_bucket_update
 AFTER UPDATE ON "main_chain_reject_bucket" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='main_chain_reject_bucket' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('main_chain_reject_bucket',NEW.rowid);
 END;

-- cp8_revision_manual_backtest_task_delete
CREATE TRIGGER cp8_revision_manual_backtest_task_delete
 AFTER DELETE ON "manual_backtest_task" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='manual_backtest_task' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_manual_backtest_task_insert
CREATE TRIGGER cp8_revision_manual_backtest_task_insert
 AFTER INSERT ON "manual_backtest_task" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('manual_backtest_task',NEW.rowid);
 END;

-- cp8_revision_manual_backtest_task_update
CREATE TRIGGER cp8_revision_manual_backtest_task_update
 AFTER UPDATE ON "manual_backtest_task" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='manual_backtest_task' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('manual_backtest_task',NEW.rowid);
 END;

-- cp8_revision_miss_reason_sample_delete
CREATE TRIGGER cp8_revision_miss_reason_sample_delete
 AFTER DELETE ON "miss_reason_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='miss_reason_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_miss_reason_sample_insert
CREATE TRIGGER cp8_revision_miss_reason_sample_insert
 AFTER INSERT ON "miss_reason_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('miss_reason_sample',NEW.rowid);
 END;

-- cp8_revision_miss_reason_sample_update
CREATE TRIGGER cp8_revision_miss_reason_sample_update
 AFTER UPDATE ON "miss_reason_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='miss_reason_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('miss_reason_sample',NEW.rowid);
 END;

-- cp8_revision_model_weight_delete
CREATE TRIGGER cp8_revision_model_weight_delete
 AFTER DELETE ON "model_weight" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='model_weight' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_model_weight_insert
CREATE TRIGGER cp8_revision_model_weight_insert
 AFTER INSERT ON "model_weight" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('model_weight',NEW.rowid);
 END;

-- cp8_revision_model_weight_update
CREATE TRIGGER cp8_revision_model_weight_update
 AFTER UPDATE ON "model_weight" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='model_weight' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('model_weight',NEW.rowid);
 END;

-- cp8_revision_molecular_tool_proposal_r947_delete
CREATE TRIGGER cp8_revision_molecular_tool_proposal_r947_delete
 AFTER DELETE ON "molecular_tool_proposal_r947" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='molecular_tool_proposal_r947' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_molecular_tool_proposal_r947_insert
CREATE TRIGGER cp8_revision_molecular_tool_proposal_r947_insert
 AFTER INSERT ON "molecular_tool_proposal_r947" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('molecular_tool_proposal_r947',NEW.rowid);
 END;

-- cp8_revision_molecular_tool_proposal_r947_update
CREATE TRIGGER cp8_revision_molecular_tool_proposal_r947_update
 AFTER UPDATE ON "molecular_tool_proposal_r947" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='molecular_tool_proposal_r947' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('molecular_tool_proposal_r947',NEW.rowid);
 END;

-- cp8_revision_monotonic_calibration_v28_delete
CREATE TRIGGER cp8_revision_monotonic_calibration_v28_delete
 AFTER DELETE ON "monotonic_calibration_v28" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='monotonic_calibration_v28' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_monotonic_calibration_v28_insert
CREATE TRIGGER cp8_revision_monotonic_calibration_v28_insert
 AFTER INSERT ON "monotonic_calibration_v28" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('monotonic_calibration_v28',NEW.rowid);
 END;

-- cp8_revision_monotonic_calibration_v28_update
CREATE TRIGGER cp8_revision_monotonic_calibration_v28_update
 AFTER UPDATE ON "monotonic_calibration_v28" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='monotonic_calibration_v28' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('monotonic_calibration_v28',NEW.rowid);
 END;

-- cp8_revision_need_supply_match_sample_delete
CREATE TRIGGER cp8_revision_need_supply_match_sample_delete
 AFTER DELETE ON "need_supply_match_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='need_supply_match_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_need_supply_match_sample_insert
CREATE TRIGGER cp8_revision_need_supply_match_sample_insert
 AFTER INSERT ON "need_supply_match_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('need_supply_match_sample',NEW.rowid);
 END;

-- cp8_revision_need_supply_match_sample_update
CREATE TRIGGER cp8_revision_need_supply_match_sample_update
 AFTER UPDATE ON "need_supply_match_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='need_supply_match_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('need_supply_match_sample',NEW.rowid);
 END;

-- cp8_revision_number_supply_sample_delete
CREATE TRIGGER cp8_revision_number_supply_sample_delete
 AFTER DELETE ON "number_supply_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='number_supply_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_number_supply_sample_insert
CREATE TRIGGER cp8_revision_number_supply_sample_insert
 AFTER INSERT ON "number_supply_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('number_supply_sample',NEW.rowid);
 END;

-- cp8_revision_number_supply_sample_update
CREATE TRIGGER cp8_revision_number_supply_sample_update
 AFTER UPDATE ON "number_supply_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='number_supply_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('number_supply_sample',NEW.rowid);
 END;

-- cp8_revision_oos_action_log_v28_delete
CREATE TRIGGER cp8_revision_oos_action_log_v28_delete
 AFTER DELETE ON "oos_action_log_v28" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='oos_action_log_v28' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_oos_action_log_v28_insert
CREATE TRIGGER cp8_revision_oos_action_log_v28_insert
 AFTER INSERT ON "oos_action_log_v28" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('oos_action_log_v28',NEW.rowid);
 END;

-- cp8_revision_oos_action_log_v28_update
CREATE TRIGGER cp8_revision_oos_action_log_v28_update
 AFTER UPDATE ON "oos_action_log_v28" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='oos_action_log_v28' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('oos_action_log_v28',NEW.rowid);
 END;

-- cp8_revision_point_in_time_snapshot_delete
CREATE TRIGGER cp8_revision_point_in_time_snapshot_delete
 AFTER DELETE ON "point_in_time_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='point_in_time_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_point_in_time_snapshot_insert
CREATE TRIGGER cp8_revision_point_in_time_snapshot_insert
 AFTER INSERT ON "point_in_time_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('point_in_time_snapshot',NEW.rowid);
 END;

-- cp8_revision_point_in_time_snapshot_update
CREATE TRIGGER cp8_revision_point_in_time_snapshot_update
 AFTER UPDATE ON "point_in_time_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='point_in_time_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('point_in_time_snapshot',NEW.rowid);
 END;

-- cp8_revision_position_cause_matrix_delete
CREATE TRIGGER cp8_revision_position_cause_matrix_delete
 AFTER DELETE ON "position_cause_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='position_cause_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_position_cause_matrix_insert
CREATE TRIGGER cp8_revision_position_cause_matrix_insert
 AFTER INSERT ON "position_cause_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('position_cause_matrix',NEW.rowid);
 END;

-- cp8_revision_position_cause_matrix_update
CREATE TRIGGER cp8_revision_position_cause_matrix_update
 AFTER UPDATE ON "position_cause_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='position_cause_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('position_cause_matrix',NEW.rowid);
 END;

-- cp8_revision_position_need_sample_delete
CREATE TRIGGER cp8_revision_position_need_sample_delete
 AFTER DELETE ON "position_need_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='position_need_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_position_need_sample_insert
CREATE TRIGGER cp8_revision_position_need_sample_insert
 AFTER INSERT ON "position_need_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('position_need_sample',NEW.rowid);
 END;

-- cp8_revision_position_need_sample_update
CREATE TRIGGER cp8_revision_position_need_sample_update
 AFTER UPDATE ON "position_need_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='position_need_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('position_need_sample',NEW.rowid);
 END;

-- cp8_revision_pre_draw_feature_delete
CREATE TRIGGER cp8_revision_pre_draw_feature_delete
 AFTER DELETE ON "pre_draw_feature" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='pre_draw_feature' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_pre_draw_feature_insert
CREATE TRIGGER cp8_revision_pre_draw_feature_insert
 AFTER INSERT ON "pre_draw_feature" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('pre_draw_feature',NEW.rowid);
 END;

-- cp8_revision_pre_draw_feature_update
CREATE TRIGGER cp8_revision_pre_draw_feature_update
 AFTER UPDATE ON "pre_draw_feature" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='pre_draw_feature' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('pre_draw_feature',NEW.rowid);
 END;

-- cp8_revision_pre_draw_number_sample_delete
CREATE TRIGGER cp8_revision_pre_draw_number_sample_delete
 AFTER DELETE ON "pre_draw_number_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='pre_draw_number_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_pre_draw_number_sample_insert
CREATE TRIGGER cp8_revision_pre_draw_number_sample_insert
 AFTER INSERT ON "pre_draw_number_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('pre_draw_number_sample',NEW.rowid);
 END;

-- cp8_revision_pre_draw_number_sample_update
CREATE TRIGGER cp8_revision_pre_draw_number_sample_update
 AFTER UPDATE ON "pre_draw_number_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='pre_draw_number_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('pre_draw_number_sample',NEW.rowid);
 END;

-- cp8_revision_pre_draw_reason_sample_delete
CREATE TRIGGER cp8_revision_pre_draw_reason_sample_delete
 AFTER DELETE ON "pre_draw_reason_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='pre_draw_reason_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_pre_draw_reason_sample_insert
CREATE TRIGGER cp8_revision_pre_draw_reason_sample_insert
 AFTER INSERT ON "pre_draw_reason_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('pre_draw_reason_sample',NEW.rowid);
 END;

-- cp8_revision_pre_draw_reason_sample_update
CREATE TRIGGER cp8_revision_pre_draw_reason_sample_update
 AFTER UPDATE ON "pre_draw_reason_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='pre_draw_reason_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('pre_draw_reason_sample',NEW.rowid);
 END;

-- cp8_revision_probability_audit_bucket_delete
CREATE TRIGGER cp8_revision_probability_audit_bucket_delete
 AFTER DELETE ON "probability_audit_bucket" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='probability_audit_bucket' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_probability_audit_bucket_insert
CREATE TRIGGER cp8_revision_probability_audit_bucket_insert
 AFTER INSERT ON "probability_audit_bucket" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('probability_audit_bucket',NEW.rowid);
 END;

-- cp8_revision_probability_audit_bucket_update
CREATE TRIGGER cp8_revision_probability_audit_bucket_update
 AFTER UPDATE ON "probability_audit_bucket" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='probability_audit_bucket' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('probability_audit_bucket',NEW.rowid);
 END;

-- cp8_revision_promotion_candidate_delete
CREATE TRIGGER cp8_revision_promotion_candidate_delete
 AFTER DELETE ON "promotion_candidate" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='promotion_candidate' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_promotion_candidate_insert
CREATE TRIGGER cp8_revision_promotion_candidate_insert
 AFTER INSERT ON "promotion_candidate" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('promotion_candidate',NEW.rowid);
 END;

-- cp8_revision_promotion_candidate_update
CREATE TRIGGER cp8_revision_promotion_candidate_update
 AFTER UPDATE ON "promotion_candidate" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='promotion_candidate' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('promotion_candidate',NEW.rowid);
 END;

-- cp8_revision_promotion_domain_snapshot_delete
CREATE TRIGGER cp8_revision_promotion_domain_snapshot_delete
 AFTER DELETE ON "promotion_domain_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='promotion_domain_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_promotion_domain_snapshot_insert
CREATE TRIGGER cp8_revision_promotion_domain_snapshot_insert
 AFTER INSERT ON "promotion_domain_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('promotion_domain_snapshot',NEW.rowid);
 END;

-- cp8_revision_promotion_domain_snapshot_update
CREATE TRIGGER cp8_revision_promotion_domain_snapshot_update
 AFTER UPDATE ON "promotion_domain_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='promotion_domain_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('promotion_domain_snapshot',NEW.rowid);
 END;

-- cp8_revision_promotion_domain_stat_v20_delete
CREATE TRIGGER cp8_revision_promotion_domain_stat_v20_delete
 AFTER DELETE ON "promotion_domain_stat_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='promotion_domain_stat_v20' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_promotion_domain_stat_v20_insert
CREATE TRIGGER cp8_revision_promotion_domain_stat_v20_insert
 AFTER INSERT ON "promotion_domain_stat_v20" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('promotion_domain_stat_v20',NEW.rowid);
 END;

-- cp8_revision_promotion_domain_stat_v20_update
CREATE TRIGGER cp8_revision_promotion_domain_stat_v20_update
 AFTER UPDATE ON "promotion_domain_stat_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='promotion_domain_stat_v20' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('promotion_domain_stat_v20',NEW.rowid);
 END;

-- cp8_revision_r952_artifact_delete
CREATE TRIGGER cp8_revision_r952_artifact_delete
 AFTER DELETE ON "r952_artifact" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_artifact' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_artifact_insert
CREATE TRIGGER cp8_revision_r952_artifact_insert
 AFTER INSERT ON "r952_artifact" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_artifact',NEW.rowid);
 END;

-- cp8_revision_r952_artifact_update
CREATE TRIGGER cp8_revision_r952_artifact_update
 AFTER UPDATE ON "r952_artifact" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_artifact' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_artifact',NEW.rowid);
 END;

-- cp8_revision_r952_candidate_funnel_delete
CREATE TRIGGER cp8_revision_r952_candidate_funnel_delete
 AFTER DELETE ON "r952_candidate_funnel" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_candidate_funnel' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_candidate_funnel_insert
CREATE TRIGGER cp8_revision_r952_candidate_funnel_insert
 AFTER INSERT ON "r952_candidate_funnel" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_candidate_funnel',NEW.rowid);
 END;

-- cp8_revision_r952_candidate_funnel_update
CREATE TRIGGER cp8_revision_r952_candidate_funnel_update
 AFTER UPDATE ON "r952_candidate_funnel" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_candidate_funnel' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_candidate_funnel',NEW.rowid);
 END;

-- cp8_revision_r952_candidate_predraw_delete
CREATE TRIGGER cp8_revision_r952_candidate_predraw_delete
 AFTER DELETE ON "r952_candidate_predraw" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_candidate_predraw' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_candidate_predraw_insert
CREATE TRIGGER cp8_revision_r952_candidate_predraw_insert
 AFTER INSERT ON "r952_candidate_predraw" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_candidate_predraw',NEW.rowid);
 END;

-- cp8_revision_r952_candidate_predraw_update
CREATE TRIGGER cp8_revision_r952_candidate_predraw_update
 AFTER UPDATE ON "r952_candidate_predraw" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_candidate_predraw' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_candidate_predraw',NEW.rowid);
 END;

-- cp8_revision_r952_intent_delete
CREATE TRIGGER cp8_revision_r952_intent_delete
 AFTER DELETE ON "r952_intent" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_intent' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_intent_insert
CREATE TRIGGER cp8_revision_r952_intent_insert
 AFTER INSERT ON "r952_intent" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_intent',NEW.rowid);
 END;

-- cp8_revision_r952_intent_update
CREATE TRIGGER cp8_revision_r952_intent_update
 AFTER UPDATE ON "r952_intent" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_intent' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_intent',NEW.rowid);
 END;

-- cp8_revision_r952_knowledge_delete
CREATE TRIGGER cp8_revision_r952_knowledge_delete
 AFTER DELETE ON "r952_knowledge" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_knowledge' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_knowledge_insert
CREATE TRIGGER cp8_revision_r952_knowledge_insert
 AFTER INSERT ON "r952_knowledge" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_knowledge',NEW.rowid);
 END;

-- cp8_revision_r952_knowledge_update
CREATE TRIGGER cp8_revision_r952_knowledge_update
 AFTER UPDATE ON "r952_knowledge" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_knowledge' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_knowledge',NEW.rowid);
 END;

-- cp8_revision_r952_matrix_feedback_delete
CREATE TRIGGER cp8_revision_r952_matrix_feedback_delete
 AFTER DELETE ON "r952_matrix_feedback" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_matrix_feedback' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_matrix_feedback_insert
CREATE TRIGGER cp8_revision_r952_matrix_feedback_insert
 AFTER INSERT ON "r952_matrix_feedback" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_matrix_feedback',NEW.rowid);
 END;

-- cp8_revision_r952_matrix_feedback_update
CREATE TRIGGER cp8_revision_r952_matrix_feedback_update
 AFTER UPDATE ON "r952_matrix_feedback" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_matrix_feedback' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_matrix_feedback',NEW.rowid);
 END;

-- cp8_revision_r952_matrix_snapshot_delete
CREATE TRIGGER cp8_revision_r952_matrix_snapshot_delete
 AFTER DELETE ON "r952_matrix_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_matrix_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_matrix_snapshot_insert
CREATE TRIGGER cp8_revision_r952_matrix_snapshot_insert
 AFTER INSERT ON "r952_matrix_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_matrix_snapshot',NEW.rowid);
 END;

-- cp8_revision_r952_matrix_snapshot_update
CREATE TRIGGER cp8_revision_r952_matrix_snapshot_update
 AFTER UPDATE ON "r952_matrix_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_matrix_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_matrix_snapshot',NEW.rowid);
 END;

-- cp8_revision_r952_pareto_delete
CREATE TRIGGER cp8_revision_r952_pareto_delete
 AFTER DELETE ON "r952_pareto" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_pareto' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_pareto_insert
CREATE TRIGGER cp8_revision_r952_pareto_insert
 AFTER INSERT ON "r952_pareto" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_pareto',NEW.rowid);
 END;

-- cp8_revision_r952_pareto_update
CREATE TRIGGER cp8_revision_r952_pareto_update
 AFTER UPDATE ON "r952_pareto" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_pareto' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_pareto',NEW.rowid);
 END;

-- cp8_revision_r952_recovery_journal_delete
CREATE TRIGGER cp8_revision_r952_recovery_journal_delete
 AFTER DELETE ON "r952_recovery_journal" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_recovery_journal' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_recovery_journal_insert
CREATE TRIGGER cp8_revision_r952_recovery_journal_insert
 AFTER INSERT ON "r952_recovery_journal" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_recovery_journal',NEW.rowid);
 END;

-- cp8_revision_r952_recovery_journal_update
CREATE TRIGGER cp8_revision_r952_recovery_journal_update
 AFTER UPDATE ON "r952_recovery_journal" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_recovery_journal' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_recovery_journal',NEW.rowid);
 END;

-- cp8_revision_r952_research_outbox_delete
CREATE TRIGGER cp8_revision_r952_research_outbox_delete
 AFTER DELETE ON "r952_research_outbox" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_research_outbox' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_research_outbox_insert
CREATE TRIGGER cp8_revision_r952_research_outbox_insert
 AFTER INSERT ON "r952_research_outbox" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_research_outbox',NEW.rowid);
 END;

-- cp8_revision_r952_research_outbox_update
CREATE TRIGGER cp8_revision_r952_research_outbox_update
 AFTER UPDATE ON "r952_research_outbox" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_research_outbox' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_research_outbox',NEW.rowid);
 END;

-- cp8_revision_r952_runtime_event_delete
CREATE TRIGGER cp8_revision_r952_runtime_event_delete
 AFTER DELETE ON "r952_runtime_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_runtime_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r952_runtime_event_insert
CREATE TRIGGER cp8_revision_r952_runtime_event_insert
 AFTER INSERT ON "r952_runtime_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_runtime_event',NEW.rowid);
 END;

-- cp8_revision_r952_runtime_event_update
CREATE TRIGGER cp8_revision_r952_runtime_event_update
 AFTER UPDATE ON "r952_runtime_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r952_runtime_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r952_runtime_event',NEW.rowid);
 END;

-- cp8_revision_r958_frozen_predraw_delete
CREATE TRIGGER cp8_revision_r958_frozen_predraw_delete
 AFTER DELETE ON "r958_frozen_predraw" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r958_frozen_predraw' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r958_frozen_predraw_insert
CREATE TRIGGER cp8_revision_r958_frozen_predraw_insert
 AFTER INSERT ON "r958_frozen_predraw" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r958_frozen_predraw',NEW.rowid);
 END;

-- cp8_revision_r958_frozen_predraw_update
CREATE TRIGGER cp8_revision_r958_frozen_predraw_update
 AFTER UPDATE ON "r958_frozen_predraw" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r958_frozen_predraw' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r958_frozen_predraw',NEW.rowid);
 END;

-- cp8_revision_r967_numeric_training_input_delete
CREATE TRIGGER cp8_revision_r967_numeric_training_input_delete
 AFTER DELETE ON "r967_numeric_training_input" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r967_numeric_training_input' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r967_numeric_training_input_insert
CREATE TRIGGER cp8_revision_r967_numeric_training_input_insert
 AFTER INSERT ON "r967_numeric_training_input" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r967_numeric_training_input',NEW.rowid);
 END;

-- cp8_revision_r967_numeric_training_input_update
CREATE TRIGGER cp8_revision_r967_numeric_training_input_update
 AFTER UPDATE ON "r967_numeric_training_input" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r967_numeric_training_input' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r967_numeric_training_input',NEW.rowid);
 END;

-- cp8_revision_r974_computation_receipt_delete
CREATE TRIGGER cp8_revision_r974_computation_receipt_delete
 AFTER DELETE ON "r974_computation_receipt" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r974_computation_receipt' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r974_computation_receipt_insert
CREATE TRIGGER cp8_revision_r974_computation_receipt_insert
 AFTER INSERT ON "r974_computation_receipt" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r974_computation_receipt',NEW.rowid);
 END;

-- cp8_revision_r974_computation_receipt_update
CREATE TRIGGER cp8_revision_r974_computation_receipt_update
 AFTER UPDATE ON "r974_computation_receipt" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r974_computation_receipt' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r974_computation_receipt',NEW.rowid);
 END;

-- cp8_revision_r974_exact_real_receipt_delete
CREATE TRIGGER cp8_revision_r974_exact_real_receipt_delete
 AFTER DELETE ON "r974_exact_real_receipt" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r974_exact_real_receipt' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r974_exact_real_receipt_insert
CREATE TRIGGER cp8_revision_r974_exact_real_receipt_insert
 AFTER INSERT ON "r974_exact_real_receipt" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r974_exact_real_receipt',NEW.rowid);
 END;

-- cp8_revision_r974_exact_real_receipt_update
CREATE TRIGGER cp8_revision_r974_exact_real_receipt_update
 AFTER UPDATE ON "r974_exact_real_receipt" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r974_exact_real_receipt' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r974_exact_real_receipt',NEW.rowid);
 END;

-- cp8_revision_r974_prediction_rejection_delete
CREATE TRIGGER cp8_revision_r974_prediction_rejection_delete
 AFTER DELETE ON "r974_prediction_rejection" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r974_prediction_rejection' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r974_prediction_rejection_insert
CREATE TRIGGER cp8_revision_r974_prediction_rejection_insert
 AFTER INSERT ON "r974_prediction_rejection" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r974_prediction_rejection',NEW.rowid);
 END;

-- cp8_revision_r974_prediction_rejection_update
CREATE TRIGGER cp8_revision_r974_prediction_rejection_update
 AFTER UPDATE ON "r974_prediction_rejection" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r974_prediction_rejection' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r974_prediction_rejection',NEW.rowid);
 END;

-- cp8_revision_r975_calibration_failure_delete
CREATE TRIGGER cp8_revision_r975_calibration_failure_delete
 AFTER DELETE ON "r975_calibration_failure" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r975_calibration_failure' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r975_calibration_failure_insert
CREATE TRIGGER cp8_revision_r975_calibration_failure_insert
 AFTER INSERT ON "r975_calibration_failure" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r975_calibration_failure',NEW.rowid);
 END;

-- cp8_revision_r975_calibration_failure_update
CREATE TRIGGER cp8_revision_r975_calibration_failure_update
 AFTER UPDATE ON "r975_calibration_failure" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r975_calibration_failure' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r975_calibration_failure',NEW.rowid);
 END;

-- cp8_revision_r975_evolution_review_delete
CREATE TRIGGER cp8_revision_r975_evolution_review_delete
 AFTER DELETE ON "r975_evolution_review" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r975_evolution_review' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r975_evolution_review_insert
CREATE TRIGGER cp8_revision_r975_evolution_review_insert
 AFTER INSERT ON "r975_evolution_review" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r975_evolution_review',NEW.rowid);
 END;

-- cp8_revision_r975_evolution_review_update
CREATE TRIGGER cp8_revision_r975_evolution_review_update
 AFTER UPDATE ON "r975_evolution_review" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r975_evolution_review' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r975_evolution_review',NEW.rowid);
 END;

-- cp8_revision_r979_calibration_restart_delete
CREATE TRIGGER cp8_revision_r979_calibration_restart_delete
 AFTER DELETE ON "r979_calibration_restart" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r979_calibration_restart' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_r979_calibration_restart_insert
CREATE TRIGGER cp8_revision_r979_calibration_restart_insert
 AFTER INSERT ON "r979_calibration_restart" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r979_calibration_restart',NEW.rowid);
 END;

-- cp8_revision_r979_calibration_restart_update
CREATE TRIGGER cp8_revision_r979_calibration_restart_update
 AFTER UPDATE ON "r979_calibration_restart" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='r979_calibration_restart' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r979_calibration_restart',NEW.rowid);
 END;

-- cp8_revision_real_trade_ledger_delete
CREATE TRIGGER cp8_revision_real_trade_ledger_delete
 AFTER DELETE ON "real_trade_ledger" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='real_trade_ledger' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_real_trade_ledger_insert
CREATE TRIGGER cp8_revision_real_trade_ledger_insert
 AFTER INSERT ON "real_trade_ledger" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('real_trade_ledger',NEW.rowid);
 END;

-- cp8_revision_real_trade_ledger_update
CREATE TRIGGER cp8_revision_real_trade_ledger_update
 AFTER UPDATE ON "real_trade_ledger" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='real_trade_ledger' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('real_trade_ledger',NEW.rowid);
 END;

-- cp8_revision_realtime_validate_delete
CREATE TRIGGER cp8_revision_realtime_validate_delete
 AFTER DELETE ON "realtime_validate" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='realtime_validate' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_realtime_validate_insert
CREATE TRIGGER cp8_revision_realtime_validate_insert
 AFTER INSERT ON "realtime_validate" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('realtime_validate',NEW.rowid);
 END;

-- cp8_revision_realtime_validate_update
CREATE TRIGGER cp8_revision_realtime_validate_update
 AFTER UPDATE ON "realtime_validate" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='realtime_validate' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('realtime_validate',NEW.rowid);
 END;

-- cp8_revision_reason_causal_profile_delete
CREATE TRIGGER cp8_revision_reason_causal_profile_delete
 AFTER DELETE ON "reason_causal_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='reason_causal_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_reason_causal_profile_insert
CREATE TRIGGER cp8_revision_reason_causal_profile_insert
 AFTER INSERT ON "reason_causal_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('reason_causal_profile',NEW.rowid);
 END;

-- cp8_revision_reason_causal_profile_update
CREATE TRIGGER cp8_revision_reason_causal_profile_update
 AFTER UPDATE ON "reason_causal_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='reason_causal_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('reason_causal_profile',NEW.rowid);
 END;

-- cp8_revision_reason_contrast_profile_delete
CREATE TRIGGER cp8_revision_reason_contrast_profile_delete
 AFTER DELETE ON "reason_contrast_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='reason_contrast_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_reason_contrast_profile_insert
CREATE TRIGGER cp8_revision_reason_contrast_profile_insert
 AFTER INSERT ON "reason_contrast_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('reason_contrast_profile',NEW.rowid);
 END;

-- cp8_revision_reason_contrast_profile_update
CREATE TRIGGER cp8_revision_reason_contrast_profile_update
 AFTER UPDATE ON "reason_contrast_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='reason_contrast_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('reason_contrast_profile',NEW.rowid);
 END;

-- cp8_revision_research_action_v2_delete
CREATE TRIGGER cp8_revision_research_action_v2_delete
 AFTER DELETE ON "research_action_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_action_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_action_v2_insert
CREATE TRIGGER cp8_revision_research_action_v2_insert
 AFTER INSERT ON "research_action_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_action_v2',NEW.rowid);
 END;

-- cp8_revision_research_action_v2_update
CREATE TRIGGER cp8_revision_research_action_v2_update
 AFTER UPDATE ON "research_action_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_action_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_action_v2',NEW.rowid);
 END;

-- cp8_revision_research_algorithm_head_archive_r948_delete
CREATE TRIGGER cp8_revision_research_algorithm_head_archive_r948_delete
 AFTER DELETE ON "research_algorithm_head_archive_r948" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_algorithm_head_archive_r948' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_algorithm_head_archive_r948_insert
CREATE TRIGGER cp8_revision_research_algorithm_head_archive_r948_insert
 AFTER INSERT ON "research_algorithm_head_archive_r948" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_algorithm_head_archive_r948',NEW.rowid);
 END;

-- cp8_revision_research_algorithm_head_archive_r948_update
CREATE TRIGGER cp8_revision_research_algorithm_head_archive_r948_update
 AFTER UPDATE ON "research_algorithm_head_archive_r948" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_algorithm_head_archive_r948' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_algorithm_head_archive_r948',NEW.rowid);
 END;

-- cp8_revision_research_algorithm_head_integrity_r948_delete
CREATE TRIGGER cp8_revision_research_algorithm_head_integrity_r948_delete
 AFTER DELETE ON "research_algorithm_head_integrity_r948" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_algorithm_head_integrity_r948' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_algorithm_head_integrity_r948_insert
CREATE TRIGGER cp8_revision_research_algorithm_head_integrity_r948_insert
 AFTER INSERT ON "research_algorithm_head_integrity_r948" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_algorithm_head_integrity_r948',NEW.rowid);
 END;

-- cp8_revision_research_algorithm_head_integrity_r948_update
CREATE TRIGGER cp8_revision_research_algorithm_head_integrity_r948_update
 AFTER UPDATE ON "research_algorithm_head_integrity_r948" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_algorithm_head_integrity_r948' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_algorithm_head_integrity_r948',NEW.rowid);
 END;

-- cp8_revision_research_algorithm_proof_quarantine_r948_delete
CREATE TRIGGER cp8_revision_research_algorithm_proof_quarantine_r948_delete
 AFTER DELETE ON "research_algorithm_proof_quarantine_r948" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_algorithm_proof_quarantine_r948' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_algorithm_proof_quarantine_r948_insert
CREATE TRIGGER cp8_revision_research_algorithm_proof_quarantine_r948_insert
 AFTER INSERT ON "research_algorithm_proof_quarantine_r948" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_algorithm_proof_quarantine_r948',NEW.rowid);
 END;

-- cp8_revision_research_algorithm_proof_quarantine_r948_update
CREATE TRIGGER cp8_revision_research_algorithm_proof_quarantine_r948_update
 AFTER UPDATE ON "research_algorithm_proof_quarantine_r948" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_algorithm_proof_quarantine_r948' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_algorithm_proof_quarantine_r948',NEW.rowid);
 END;

-- cp8_revision_research_atom_audit_r932_delete
CREATE TRIGGER cp8_revision_research_atom_audit_r932_delete
 AFTER DELETE ON "research_atom_audit_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_audit_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_atom_audit_r932_insert
CREATE TRIGGER cp8_revision_research_atom_audit_r932_insert
 AFTER INSERT ON "research_atom_audit_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_audit_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_audit_r932_update
CREATE TRIGGER cp8_revision_research_atom_audit_r932_update
 AFTER UPDATE ON "research_atom_audit_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_audit_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_audit_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_context_r932_delete
CREATE TRIGGER cp8_revision_research_atom_context_r932_delete
 AFTER DELETE ON "research_atom_context_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_context_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_atom_context_r932_insert
CREATE TRIGGER cp8_revision_research_atom_context_r932_insert
 AFTER INSERT ON "research_atom_context_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_context_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_context_r932_update
CREATE TRIGGER cp8_revision_research_atom_context_r932_update
 AFTER UPDATE ON "research_atom_context_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_context_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_context_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_quality_archive_r932_delete
CREATE TRIGGER cp8_revision_research_atom_quality_archive_r932_delete
 AFTER DELETE ON "research_atom_quality_archive_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_quality_archive_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_atom_quality_archive_r932_insert
CREATE TRIGGER cp8_revision_research_atom_quality_archive_r932_insert
 AFTER INSERT ON "research_atom_quality_archive_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_quality_archive_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_quality_archive_r932_update
CREATE TRIGGER cp8_revision_research_atom_quality_archive_r932_update
 AFTER UPDATE ON "research_atom_quality_archive_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_quality_archive_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_quality_archive_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_quality_r932_delete
CREATE TRIGGER cp8_revision_research_atom_quality_r932_delete
 AFTER DELETE ON "research_atom_quality_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_quality_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_atom_quality_r932_insert
CREATE TRIGGER cp8_revision_research_atom_quality_r932_insert
 AFTER INSERT ON "research_atom_quality_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_quality_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_quality_r932_update
CREATE TRIGGER cp8_revision_research_atom_quality_r932_update
 AFTER UPDATE ON "research_atom_quality_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_quality_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_quality_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_recovery_r932_delete
CREATE TRIGGER cp8_revision_research_atom_recovery_r932_delete
 AFTER DELETE ON "research_atom_recovery_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_recovery_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_atom_recovery_r932_insert
CREATE TRIGGER cp8_revision_research_atom_recovery_r932_insert
 AFTER INSERT ON "research_atom_recovery_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_recovery_r932',NEW.rowid);
 END;

-- cp8_revision_research_atom_recovery_r932_update
CREATE TRIGGER cp8_revision_research_atom_recovery_r932_update
 AFTER UPDATE ON "research_atom_recovery_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_atom_recovery_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_atom_recovery_r932',NEW.rowid);
 END;

-- cp8_revision_research_candidate_cal_observation_r932_delete
CREATE TRIGGER cp8_revision_research_candidate_cal_observation_r932_delete
 AFTER DELETE ON "research_candidate_cal_observation_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_candidate_cal_observation_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_candidate_cal_observation_r932_insert
CREATE TRIGGER cp8_revision_research_candidate_cal_observation_r932_insert
 AFTER INSERT ON "research_candidate_cal_observation_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_candidate_cal_observation_r932',NEW.rowid);
 END;

-- cp8_revision_research_candidate_cal_observation_r932_update
CREATE TRIGGER cp8_revision_research_candidate_cal_observation_r932_update
 AFTER UPDATE ON "research_candidate_cal_observation_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_candidate_cal_observation_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_candidate_cal_observation_r932',NEW.rowid);
 END;

-- cp8_revision_research_candidate_cal_study_r932_delete
CREATE TRIGGER cp8_revision_research_candidate_cal_study_r932_delete
 AFTER DELETE ON "research_candidate_cal_study_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_candidate_cal_study_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_candidate_cal_study_r932_insert
CREATE TRIGGER cp8_revision_research_candidate_cal_study_r932_insert
 AFTER INSERT ON "research_candidate_cal_study_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_candidate_cal_study_r932',NEW.rowid);
 END;

-- cp8_revision_research_candidate_cal_study_r932_update
CREATE TRIGGER cp8_revision_research_candidate_cal_study_r932_update
 AFTER UPDATE ON "research_candidate_cal_study_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_candidate_cal_study_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_candidate_cal_study_r932',NEW.rowid);
 END;

-- cp8_revision_research_candidate_execution_r932_delete
CREATE TRIGGER cp8_revision_research_candidate_execution_r932_delete
 AFTER DELETE ON "research_candidate_execution_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_candidate_execution_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_candidate_execution_r932_insert
CREATE TRIGGER cp8_revision_research_candidate_execution_r932_insert
 AFTER INSERT ON "research_candidate_execution_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_candidate_execution_r932',NEW.rowid);
 END;

-- cp8_revision_research_candidate_execution_r932_update
CREATE TRIGGER cp8_revision_research_candidate_execution_r932_update
 AFTER UPDATE ON "research_candidate_execution_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_candidate_execution_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_candidate_execution_r932',NEW.rowid);
 END;

-- cp8_revision_research_capture_event_r932_delete
CREATE TRIGGER cp8_revision_research_capture_event_r932_delete
 AFTER DELETE ON "research_capture_event_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_capture_event_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_capture_event_r932_insert
CREATE TRIGGER cp8_revision_research_capture_event_r932_insert
 AFTER INSERT ON "research_capture_event_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_capture_event_r932',NEW.rowid);
 END;

-- cp8_revision_research_capture_event_r932_update
CREATE TRIGGER cp8_revision_research_capture_event_r932_update
 AFTER UPDATE ON "research_capture_event_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_capture_event_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_capture_event_r932',NEW.rowid);
 END;

-- cp8_revision_research_capture_gap_v2_delete
CREATE TRIGGER cp8_revision_research_capture_gap_v2_delete
 AFTER DELETE ON "research_capture_gap_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_capture_gap_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_capture_gap_v2_insert
CREATE TRIGGER cp8_revision_research_capture_gap_v2_insert
 AFTER INSERT ON "research_capture_gap_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_capture_gap_v2',NEW.rowid);
 END;

-- cp8_revision_research_capture_gap_v2_update
CREATE TRIGGER cp8_revision_research_capture_gap_v2_update
 AFTER UPDATE ON "research_capture_gap_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_capture_gap_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_capture_gap_v2',NEW.rowid);
 END;

-- cp8_revision_research_capture_opportunity_r932_delete
CREATE TRIGGER cp8_revision_research_capture_opportunity_r932_delete
 AFTER DELETE ON "research_capture_opportunity_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_capture_opportunity_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_capture_opportunity_r932_insert
CREATE TRIGGER cp8_revision_research_capture_opportunity_r932_insert
 AFTER INSERT ON "research_capture_opportunity_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_capture_opportunity_r932',NEW.rowid);
 END;

-- cp8_revision_research_capture_opportunity_r932_update
CREATE TRIGGER cp8_revision_research_capture_opportunity_r932_update
 AFTER UPDATE ON "research_capture_opportunity_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_capture_opportunity_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_capture_opportunity_r932',NEW.rowid);
 END;

-- cp8_revision_research_capture_reservation_v2_delete
CREATE TRIGGER cp8_revision_research_capture_reservation_v2_delete
 AFTER DELETE ON "research_capture_reservation_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_capture_reservation_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_capture_reservation_v2_insert
CREATE TRIGGER cp8_revision_research_capture_reservation_v2_insert
 AFTER INSERT ON "research_capture_reservation_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_capture_reservation_v2',NEW.rowid);
 END;

-- cp8_revision_research_capture_reservation_v2_update
CREATE TRIGGER cp8_revision_research_capture_reservation_v2_update
 AFTER UPDATE ON "research_capture_reservation_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_capture_reservation_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_capture_reservation_v2',NEW.rowid);
 END;

-- cp8_revision_research_challenge_prediction_r932_delete
CREATE TRIGGER cp8_revision_research_challenge_prediction_r932_delete
 AFTER DELETE ON "research_challenge_prediction_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_challenge_prediction_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_challenge_prediction_r932_insert
CREATE TRIGGER cp8_revision_research_challenge_prediction_r932_insert
 AFTER INSERT ON "research_challenge_prediction_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_challenge_prediction_r932',NEW.rowid);
 END;

-- cp8_revision_research_challenge_prediction_r932_update
CREATE TRIGGER cp8_revision_research_challenge_prediction_r932_update
 AFTER UPDATE ON "research_challenge_prediction_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_challenge_prediction_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_challenge_prediction_r932',NEW.rowid);
 END;

-- cp8_revision_research_consumer_choice_r932_delete
CREATE TRIGGER cp8_revision_research_consumer_choice_r932_delete
 AFTER DELETE ON "research_consumer_choice_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_consumer_choice_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_consumer_choice_r932_insert
CREATE TRIGGER cp8_revision_research_consumer_choice_r932_insert
 AFTER INSERT ON "research_consumer_choice_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_consumer_choice_r932',NEW.rowid);
 END;

-- cp8_revision_research_consumer_choice_r932_update
CREATE TRIGGER cp8_revision_research_consumer_choice_r932_update
 AFTER UPDATE ON "research_consumer_choice_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_consumer_choice_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_consumer_choice_r932',NEW.rowid);
 END;

-- cp8_revision_research_cp4_candidate_resource_delete
CREATE TRIGGER cp8_revision_research_cp4_candidate_resource_delete
 AFTER DELETE ON "research_cp4_candidate_resource" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_cp4_candidate_resource' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_cp4_candidate_resource_insert
CREATE TRIGGER cp8_revision_research_cp4_candidate_resource_insert
 AFTER INSERT ON "research_cp4_candidate_resource" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_cp4_candidate_resource',NEW.rowid);
 END;

-- cp8_revision_research_cp4_candidate_resource_update
CREATE TRIGGER cp8_revision_research_cp4_candidate_resource_update
 AFTER UPDATE ON "research_cp4_candidate_resource" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_cp4_candidate_resource' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_cp4_candidate_resource',NEW.rowid);
 END;

-- cp8_revision_research_creation_attempt_r932_delete
CREATE TRIGGER cp8_revision_research_creation_attempt_r932_delete
 AFTER DELETE ON "research_creation_attempt_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_creation_attempt_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_creation_attempt_r932_insert
CREATE TRIGGER cp8_revision_research_creation_attempt_r932_insert
 AFTER INSERT ON "research_creation_attempt_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_creation_attempt_r932',NEW.rowid);
 END;

-- cp8_revision_research_creation_attempt_r932_update
CREATE TRIGGER cp8_revision_research_creation_attempt_r932_update
 AFTER UPDATE ON "research_creation_attempt_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_creation_attempt_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_creation_attempt_r932',NEW.rowid);
 END;

-- cp8_revision_research_evolution_event_v41_delete
CREATE TRIGGER cp8_revision_research_evolution_event_v41_delete
 AFTER DELETE ON "research_evolution_event_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_evolution_event_v41' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_evolution_event_v41_insert
CREATE TRIGGER cp8_revision_research_evolution_event_v41_insert
 AFTER INSERT ON "research_evolution_event_v41" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_evolution_event_v41',NEW.rowid);
 END;

-- cp8_revision_research_evolution_event_v41_update
CREATE TRIGGER cp8_revision_research_evolution_event_v41_update
 AFTER UPDATE ON "research_evolution_event_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_evolution_event_v41' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_evolution_event_v41',NEW.rowid);
 END;

-- cp8_revision_research_finding_evidence_v2_delete
CREATE TRIGGER cp8_revision_research_finding_evidence_v2_delete
 AFTER DELETE ON "research_finding_evidence_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_finding_evidence_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_finding_evidence_v2_insert
CREATE TRIGGER cp8_revision_research_finding_evidence_v2_insert
 AFTER INSERT ON "research_finding_evidence_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_finding_evidence_v2',NEW.rowid);
 END;

-- cp8_revision_research_finding_evidence_v2_update
CREATE TRIGGER cp8_revision_research_finding_evidence_v2_update
 AFTER UPDATE ON "research_finding_evidence_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_finding_evidence_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_finding_evidence_v2',NEW.rowid);
 END;

-- cp8_revision_research_finding_v2_delete
CREATE TRIGGER cp8_revision_research_finding_v2_delete
 AFTER DELETE ON "research_finding_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_finding_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_finding_v2_insert
CREATE TRIGGER cp8_revision_research_finding_v2_insert
 AFTER INSERT ON "research_finding_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_finding_v2',NEW.rowid);
 END;

-- cp8_revision_research_finding_v2_update
CREATE TRIGGER cp8_revision_research_finding_v2_update
 AFTER UPDATE ON "research_finding_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_finding_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_finding_v2',NEW.rowid);
 END;

-- cp8_revision_research_fixed_champion_r932_delete
CREATE TRIGGER cp8_revision_research_fixed_champion_r932_delete
 AFTER DELETE ON "research_fixed_champion_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_fixed_champion_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_fixed_champion_r932_insert
CREATE TRIGGER cp8_revision_research_fixed_champion_r932_insert
 AFTER INSERT ON "research_fixed_champion_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_fixed_champion_r932',NEW.rowid);
 END;

-- cp8_revision_research_fixed_champion_r932_update
CREATE TRIGGER cp8_revision_research_fixed_champion_r932_update
 AFTER UPDATE ON "research_fixed_champion_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_fixed_champion_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_fixed_champion_r932',NEW.rowid);
 END;

-- cp8_revision_research_fixed_pair_r932_delete
CREATE TRIGGER cp8_revision_research_fixed_pair_r932_delete
 AFTER DELETE ON "research_fixed_pair_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_fixed_pair_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_fixed_pair_r932_insert
CREATE TRIGGER cp8_revision_research_fixed_pair_r932_insert
 AFTER INSERT ON "research_fixed_pair_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_fixed_pair_r932',NEW.rowid);
 END;

-- cp8_revision_research_fixed_pair_r932_update
CREATE TRIGGER cp8_revision_research_fixed_pair_r932_update
 AFTER UPDATE ON "research_fixed_pair_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_fixed_pair_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_fixed_pair_r932',NEW.rowid);
 END;

-- cp8_revision_research_fixed_window_r932_delete
CREATE TRIGGER cp8_revision_research_fixed_window_r932_delete
 AFTER DELETE ON "research_fixed_window_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_fixed_window_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_fixed_window_r932_insert
CREATE TRIGGER cp8_revision_research_fixed_window_r932_insert
 AFTER INSERT ON "research_fixed_window_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_fixed_window_r932',NEW.rowid);
 END;

-- cp8_revision_research_fixed_window_r932_update
CREATE TRIGGER cp8_revision_research_fixed_window_r932_update
 AFTER UPDATE ON "research_fixed_window_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_fixed_window_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_fixed_window_r932',NEW.rowid);
 END;

-- cp8_revision_research_fixed_window_statistics_r932_delete
CREATE TRIGGER cp8_revision_research_fixed_window_statistics_r932_delete
 AFTER DELETE ON "research_fixed_window_statistics_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_fixed_window_statistics_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_fixed_window_statistics_r932_insert
CREATE TRIGGER cp8_revision_research_fixed_window_statistics_r932_insert
 AFTER INSERT ON "research_fixed_window_statistics_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_fixed_window_statistics_r932',NEW.rowid);
 END;

-- cp8_revision_research_fixed_window_statistics_r932_update
CREATE TRIGGER cp8_revision_research_fixed_window_statistics_r932_update
 AFTER UPDATE ON "research_fixed_window_statistics_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_fixed_window_statistics_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_fixed_window_statistics_r932',NEW.rowid);
 END;

-- cp8_revision_research_legacy_fixed_registry_v41_delete
CREATE TRIGGER cp8_revision_research_legacy_fixed_registry_v41_delete
 AFTER DELETE ON "research_legacy_fixed_registry_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_legacy_fixed_registry_v41' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_legacy_fixed_registry_v41_insert
CREATE TRIGGER cp8_revision_research_legacy_fixed_registry_v41_insert
 AFTER INSERT ON "research_legacy_fixed_registry_v41" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_legacy_fixed_registry_v41',NEW.rowid);
 END;

-- cp8_revision_research_legacy_fixed_registry_v41_update
CREATE TRIGGER cp8_revision_research_legacy_fixed_registry_v41_update
 AFTER UPDATE ON "research_legacy_fixed_registry_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_legacy_fixed_registry_v41' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_legacy_fixed_registry_v41',NEW.rowid);
 END;

-- cp8_revision_research_lifecycle_event_r932_delete
CREATE TRIGGER cp8_revision_research_lifecycle_event_r932_delete
 AFTER DELETE ON "research_lifecycle_event_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_lifecycle_event_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_lifecycle_event_r932_insert
CREATE TRIGGER cp8_revision_research_lifecycle_event_r932_insert
 AFTER INSERT ON "research_lifecycle_event_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_lifecycle_event_r932',NEW.rowid);
 END;

-- cp8_revision_research_lifecycle_event_r932_update
CREATE TRIGGER cp8_revision_research_lifecycle_event_r932_update
 AFTER UPDATE ON "research_lifecycle_event_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_lifecycle_event_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_lifecycle_event_r932',NEW.rowid);
 END;

-- cp8_revision_research_mainline_contribution_r929_delete
CREATE TRIGGER cp8_revision_research_mainline_contribution_r929_delete
 AFTER DELETE ON "research_mainline_contribution_r929" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_mainline_contribution_r929' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_mainline_contribution_r929_insert
CREATE TRIGGER cp8_revision_research_mainline_contribution_r929_insert
 AFTER INSERT ON "research_mainline_contribution_r929" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_mainline_contribution_r929',NEW.rowid);
 END;

-- cp8_revision_research_mainline_contribution_r929_update
CREATE TRIGGER cp8_revision_research_mainline_contribution_r929_update
 AFTER UPDATE ON "research_mainline_contribution_r929" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_mainline_contribution_r929' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_mainline_contribution_r929',NEW.rowid);
 END;

-- cp8_revision_research_matrix_checkpoint_v2_delete
CREATE TRIGGER cp8_revision_research_matrix_checkpoint_v2_delete
 AFTER DELETE ON "research_matrix_checkpoint_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_matrix_checkpoint_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_matrix_checkpoint_v2_insert
CREATE TRIGGER cp8_revision_research_matrix_checkpoint_v2_insert
 AFTER INSERT ON "research_matrix_checkpoint_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_matrix_checkpoint_v2',NEW.rowid);
 END;

-- cp8_revision_research_matrix_checkpoint_v2_update
CREATE TRIGGER cp8_revision_research_matrix_checkpoint_v2_update
 AFTER UPDATE ON "research_matrix_checkpoint_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_matrix_checkpoint_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_matrix_checkpoint_v2',NEW.rowid);
 END;

-- cp8_revision_research_model_bundle_r929_delete
CREATE TRIGGER cp8_revision_research_model_bundle_r929_delete
 AFTER DELETE ON "research_model_bundle_r929" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_model_bundle_r929' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_model_bundle_r929_insert
CREATE TRIGGER cp8_revision_research_model_bundle_r929_insert
 AFTER INSERT ON "research_model_bundle_r929" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_model_bundle_r929',NEW.rowid);
 END;

-- cp8_revision_research_model_bundle_r929_update
CREATE TRIGGER cp8_revision_research_model_bundle_r929_update
 AFTER UPDATE ON "research_model_bundle_r929" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_model_bundle_r929' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_model_bundle_r929',NEW.rowid);
 END;

-- cp8_revision_research_module_ablation_r932_delete
CREATE TRIGGER cp8_revision_research_module_ablation_r932_delete
 AFTER DELETE ON "research_module_ablation_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_module_ablation_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_module_ablation_r932_insert
CREATE TRIGGER cp8_revision_research_module_ablation_r932_insert
 AFTER INSERT ON "research_module_ablation_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_module_ablation_r932',NEW.rowid);
 END;

-- cp8_revision_research_module_ablation_r932_update
CREATE TRIGGER cp8_revision_research_module_ablation_r932_update
 AFTER UPDATE ON "research_module_ablation_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_module_ablation_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_module_ablation_r932',NEW.rowid);
 END;

-- cp8_revision_research_module_cal_observation_r932_delete
CREATE TRIGGER cp8_revision_research_module_cal_observation_r932_delete
 AFTER DELETE ON "research_module_cal_observation_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_module_cal_observation_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_module_cal_observation_r932_insert
CREATE TRIGGER cp8_revision_research_module_cal_observation_r932_insert
 AFTER INSERT ON "research_module_cal_observation_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_module_cal_observation_r932',NEW.rowid);
 END;

-- cp8_revision_research_module_cal_observation_r932_update
CREATE TRIGGER cp8_revision_research_module_cal_observation_r932_update
 AFTER UPDATE ON "research_module_cal_observation_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_module_cal_observation_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_module_cal_observation_r932',NEW.rowid);
 END;

-- cp8_revision_research_new_asset_freeze_v41_delete
CREATE TRIGGER cp8_revision_research_new_asset_freeze_v41_delete
 AFTER DELETE ON "research_new_asset_freeze_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_new_asset_freeze_v41' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_new_asset_freeze_v41_insert
CREATE TRIGGER cp8_revision_research_new_asset_freeze_v41_insert
 AFTER INSERT ON "research_new_asset_freeze_v41" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_new_asset_freeze_v41',NEW.rowid);
 END;

-- cp8_revision_research_new_asset_freeze_v41_update
CREATE TRIGGER cp8_revision_research_new_asset_freeze_v41_update
 AFTER UPDATE ON "research_new_asset_freeze_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_new_asset_freeze_v41' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_new_asset_freeze_v41',NEW.rowid);
 END;

-- cp8_revision_research_new_atom_proposal_v41_delete
CREATE TRIGGER cp8_revision_research_new_atom_proposal_v41_delete
 AFTER DELETE ON "research_new_atom_proposal_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_new_atom_proposal_v41' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_new_atom_proposal_v41_insert
CREATE TRIGGER cp8_revision_research_new_atom_proposal_v41_insert
 AFTER INSERT ON "research_new_atom_proposal_v41" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_new_atom_proposal_v41',NEW.rowid);
 END;

-- cp8_revision_research_new_atom_proposal_v41_update
CREATE TRIGGER cp8_revision_research_new_atom_proposal_v41_update
 AFTER UPDATE ON "research_new_atom_proposal_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_new_atom_proposal_v41' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_new_atom_proposal_v41',NEW.rowid);
 END;

-- cp8_revision_research_new_contribution_v41_delete
CREATE TRIGGER cp8_revision_research_new_contribution_v41_delete
 AFTER DELETE ON "research_new_contribution_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_new_contribution_v41' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_new_contribution_v41_insert
CREATE TRIGGER cp8_revision_research_new_contribution_v41_insert
 AFTER INSERT ON "research_new_contribution_v41" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_new_contribution_v41',NEW.rowid);
 END;

-- cp8_revision_research_new_contribution_v41_update
CREATE TRIGGER cp8_revision_research_new_contribution_v41_update
 AFTER UPDATE ON "research_new_contribution_v41" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_new_contribution_v41' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_new_contribution_v41',NEW.rowid);
 END;

-- cp8_revision_research_observation_restart_r932_delete
CREATE TRIGGER cp8_revision_research_observation_restart_r932_delete
 AFTER DELETE ON "research_observation_restart_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_observation_restart_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_observation_restart_r932_insert
CREATE TRIGGER cp8_revision_research_observation_restart_r932_insert
 AFTER INSERT ON "research_observation_restart_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_observation_restart_r932',NEW.rowid);
 END;

-- cp8_revision_research_observation_restart_r932_update
CREATE TRIGGER cp8_revision_research_observation_restart_r932_update
 AFTER UPDATE ON "research_observation_restart_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_observation_restart_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_observation_restart_r932',NEW.rowid);
 END;

-- cp8_revision_research_paired_challenge_r932_delete
CREATE TRIGGER cp8_revision_research_paired_challenge_r932_delete
 AFTER DELETE ON "research_paired_challenge_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_paired_challenge_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_paired_challenge_r932_insert
CREATE TRIGGER cp8_revision_research_paired_challenge_r932_insert
 AFTER INSERT ON "research_paired_challenge_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_paired_challenge_r932',NEW.rowid);
 END;

-- cp8_revision_research_paired_challenge_r932_update
CREATE TRIGGER cp8_revision_research_paired_challenge_r932_update
 AFTER UPDATE ON "research_paired_challenge_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_paired_challenge_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_paired_challenge_r932',NEW.rowid);
 END;

-- cp8_revision_research_platform_event_r929_delete
CREATE TRIGGER cp8_revision_research_platform_event_r929_delete
 AFTER DELETE ON "research_platform_event_r929" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_platform_event_r929' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_platform_event_r929_insert
CREATE TRIGGER cp8_revision_research_platform_event_r929_insert
 AFTER INSERT ON "research_platform_event_r929" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_platform_event_r929',NEW.rowid);
 END;

-- cp8_revision_research_platform_event_r929_update
CREATE TRIGGER cp8_revision_research_platform_event_r929_update
 AFTER UPDATE ON "research_platform_event_r929" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_platform_event_r929' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_platform_event_r929',NEW.rowid);
 END;

-- cp8_revision_research_projection_ack_v2_delete
CREATE TRIGGER cp8_revision_research_projection_ack_v2_delete
 AFTER DELETE ON "research_projection_ack_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_projection_ack_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_projection_ack_v2_insert
CREATE TRIGGER cp8_revision_research_projection_ack_v2_insert
 AFTER INSERT ON "research_projection_ack_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_projection_ack_v2',NEW.rowid);
 END;

-- cp8_revision_research_projection_ack_v2_update
CREATE TRIGGER cp8_revision_research_projection_ack_v2_update
 AFTER UPDATE ON "research_projection_ack_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_projection_ack_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_projection_ack_v2',NEW.rowid);
 END;

-- cp8_revision_research_proxy_capture_r932_delete
CREATE TRIGGER cp8_revision_research_proxy_capture_r932_delete
 AFTER DELETE ON "research_proxy_capture_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_proxy_capture_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_proxy_capture_r932_insert
CREATE TRIGGER cp8_revision_research_proxy_capture_r932_insert
 AFTER INSERT ON "research_proxy_capture_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_proxy_capture_r932',NEW.rowid);
 END;

-- cp8_revision_research_proxy_capture_r932_update
CREATE TRIGGER cp8_revision_research_proxy_capture_r932_update
 AFTER UPDATE ON "research_proxy_capture_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_proxy_capture_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_proxy_capture_r932',NEW.rowid);
 END;

-- cp8_revision_research_repair_group_r932_delete
CREATE TRIGGER cp8_revision_research_repair_group_r932_delete
 AFTER DELETE ON "research_repair_group_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_repair_group_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_repair_group_r932_insert
CREATE TRIGGER cp8_revision_research_repair_group_r932_insert
 AFTER INSERT ON "research_repair_group_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_repair_group_r932',NEW.rowid);
 END;

-- cp8_revision_research_repair_group_r932_update
CREATE TRIGGER cp8_revision_research_repair_group_r932_update
 AFTER UPDATE ON "research_repair_group_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_repair_group_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_repair_group_r932',NEW.rowid);
 END;

-- cp8_revision_research_result_v2_delete
CREATE TRIGGER cp8_revision_research_result_v2_delete
 AFTER DELETE ON "research_result_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_result_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_result_v2_insert
CREATE TRIGGER cp8_revision_research_result_v2_insert
 AFTER INSERT ON "research_result_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_result_v2',NEW.rowid);
 END;

-- cp8_revision_research_result_v2_update
CREATE TRIGGER cp8_revision_research_result_v2_update
 AFTER UPDATE ON "research_result_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_result_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_result_v2',NEW.rowid);
 END;

-- cp8_revision_research_roster_manifest_r948_delete
CREATE TRIGGER cp8_revision_research_roster_manifest_r948_delete
 AFTER DELETE ON "research_roster_manifest_r948" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_roster_manifest_r948' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_roster_manifest_r948_insert
CREATE TRIGGER cp8_revision_research_roster_manifest_r948_insert
 AFTER INSERT ON "research_roster_manifest_r948" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_roster_manifest_r948',NEW.rowid);
 END;

-- cp8_revision_research_roster_manifest_r948_update
CREATE TRIGGER cp8_revision_research_roster_manifest_r948_update
 AFTER UPDATE ON "research_roster_manifest_r948" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_roster_manifest_r948' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_roster_manifest_r948',NEW.rowid);
 END;

-- cp8_revision_research_roster_prepare_r943_delete
CREATE TRIGGER cp8_revision_research_roster_prepare_r943_delete
 AFTER DELETE ON "research_roster_prepare_r943" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_roster_prepare_r943' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_roster_prepare_r943_insert
CREATE TRIGGER cp8_revision_research_roster_prepare_r943_insert
 AFTER INSERT ON "research_roster_prepare_r943" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_roster_prepare_r943',NEW.rowid);
 END;

-- cp8_revision_research_roster_prepare_r943_update
CREATE TRIGGER cp8_revision_research_roster_prepare_r943_update
 AFTER UPDATE ON "research_roster_prepare_r943" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_roster_prepare_r943' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_roster_prepare_r943',NEW.rowid);
 END;

-- cp8_revision_research_schedule_plan_r932_delete
CREATE TRIGGER cp8_revision_research_schedule_plan_r932_delete
 AFTER DELETE ON "research_schedule_plan_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_schedule_plan_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_schedule_plan_r932_insert
CREATE TRIGGER cp8_revision_research_schedule_plan_r932_insert
 AFTER INSERT ON "research_schedule_plan_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_schedule_plan_r932',NEW.rowid);
 END;

-- cp8_revision_research_schedule_plan_r932_update
CREATE TRIGGER cp8_revision_research_schedule_plan_r932_update
 AFTER UPDATE ON "research_schedule_plan_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_schedule_plan_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_schedule_plan_r932',NEW.rowid);
 END;

-- cp8_revision_research_schedule_task_r932_delete
CREATE TRIGGER cp8_revision_research_schedule_task_r932_delete
 AFTER DELETE ON "research_schedule_task_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_schedule_task_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_schedule_task_r932_insert
CREATE TRIGGER cp8_revision_research_schedule_task_r932_insert
 AFTER INSERT ON "research_schedule_task_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_schedule_task_r932',NEW.rowid);
 END;

-- cp8_revision_research_schedule_task_r932_update
CREATE TRIGGER cp8_revision_research_schedule_task_r932_update
 AFTER UPDATE ON "research_schedule_task_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_schedule_task_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_schedule_task_r932',NEW.rowid);
 END;

-- cp8_revision_research_similarity_r932_delete
CREATE TRIGGER cp8_revision_research_similarity_r932_delete
 AFTER DELETE ON "research_similarity_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_similarity_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_similarity_r932_insert
CREATE TRIGGER cp8_revision_research_similarity_r932_insert
 AFTER INSERT ON "research_similarity_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_similarity_r932',NEW.rowid);
 END;

-- cp8_revision_research_similarity_r932_update
CREATE TRIGGER cp8_revision_research_similarity_r932_update
 AFTER UPDATE ON "research_similarity_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_similarity_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_similarity_r932',NEW.rowid);
 END;

-- cp8_revision_research_task_checkpoint_v2_delete
CREATE TRIGGER cp8_revision_research_task_checkpoint_v2_delete
 AFTER DELETE ON "research_task_checkpoint_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_task_checkpoint_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_task_checkpoint_v2_insert
CREATE TRIGGER cp8_revision_research_task_checkpoint_v2_insert
 AFTER INSERT ON "research_task_checkpoint_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_task_checkpoint_v2',NEW.rowid);
 END;

-- cp8_revision_research_task_checkpoint_v2_update
CREATE TRIGGER cp8_revision_research_task_checkpoint_v2_update
 AFTER UPDATE ON "research_task_checkpoint_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_task_checkpoint_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_task_checkpoint_v2',NEW.rowid);
 END;

-- cp8_revision_research_task_input_binding_v2_delete
CREATE TRIGGER cp8_revision_research_task_input_binding_v2_delete
 AFTER DELETE ON "research_task_input_binding_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_task_input_binding_v2' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_task_input_binding_v2_insert
CREATE TRIGGER cp8_revision_research_task_input_binding_v2_insert
 AFTER INSERT ON "research_task_input_binding_v2" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_task_input_binding_v2',NEW.rowid);
 END;

-- cp8_revision_research_task_input_binding_v2_update
CREATE TRIGGER cp8_revision_research_task_input_binding_v2_update
 AFTER UPDATE ON "research_task_input_binding_v2" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_task_input_binding_v2' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_task_input_binding_v2',NEW.rowid);
 END;

-- cp8_revision_research_trial_registry_r929_delete
CREATE TRIGGER cp8_revision_research_trial_registry_r929_delete
 AFTER DELETE ON "research_trial_registry_r929" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_trial_registry_r929' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_trial_registry_r929_insert
CREATE TRIGGER cp8_revision_research_trial_registry_r929_insert
 AFTER INSERT ON "research_trial_registry_r929" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_trial_registry_r929',NEW.rowid);
 END;

-- cp8_revision_research_trial_registry_r929_update
CREATE TRIGGER cp8_revision_research_trial_registry_r929_update
 AFTER UPDATE ON "research_trial_registry_r929" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_trial_registry_r929' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_trial_registry_r929',NEW.rowid);
 END;

-- cp8_revision_research_window_publication_r932_delete
CREATE TRIGGER cp8_revision_research_window_publication_r932_delete
 AFTER DELETE ON "research_window_publication_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_window_publication_r932' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_research_window_publication_r932_insert
CREATE TRIGGER cp8_revision_research_window_publication_r932_insert
 AFTER INSERT ON "research_window_publication_r932" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_window_publication_r932',NEW.rowid);
 END;

-- cp8_revision_research_window_publication_r932_update
CREATE TRIGGER cp8_revision_research_window_publication_r932_update
 AFTER UPDATE ON "research_window_publication_r932" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='research_window_publication_r932' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_window_publication_r932',NEW.rowid);
 END;

-- cp8_revision_rule_health_matrix_delete
CREATE TRIGGER cp8_revision_rule_health_matrix_delete
 AFTER DELETE ON "rule_health_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='rule_health_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_rule_health_matrix_insert
CREATE TRIGGER cp8_revision_rule_health_matrix_insert
 AFTER INSERT ON "rule_health_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('rule_health_matrix',NEW.rowid);
 END;

-- cp8_revision_rule_health_matrix_update
CREATE TRIGGER cp8_revision_rule_health_matrix_update
 AFTER UPDATE ON "rule_health_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='rule_health_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('rule_health_matrix',NEW.rowid);
 END;

-- cp8_revision_runtime_profile_cache_delete
CREATE TRIGGER cp8_revision_runtime_profile_cache_delete
 AFTER DELETE ON "runtime_profile_cache" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='runtime_profile_cache' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_runtime_profile_cache_insert
CREATE TRIGGER cp8_revision_runtime_profile_cache_insert
 AFTER INSERT ON "runtime_profile_cache" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('runtime_profile_cache',NEW.rowid);
 END;

-- cp8_revision_runtime_profile_cache_update
CREATE TRIGGER cp8_revision_runtime_profile_cache_update
 AFTER UPDATE ON "runtime_profile_cache" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='runtime_profile_cache' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('runtime_profile_cache',NEW.rowid);
 END;

-- cp8_revision_seed_cause_expansion_delete
CREATE TRIGGER cp8_revision_seed_cause_expansion_delete
 AFTER DELETE ON "seed_cause_expansion" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='seed_cause_expansion' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_seed_cause_expansion_insert
CREATE TRIGGER cp8_revision_seed_cause_expansion_insert
 AFTER INSERT ON "seed_cause_expansion" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('seed_cause_expansion',NEW.rowid);
 END;

-- cp8_revision_seed_cause_expansion_update
CREATE TRIGGER cp8_revision_seed_cause_expansion_update
 AFTER UPDATE ON "seed_cause_expansion" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='seed_cause_expansion' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('seed_cause_expansion',NEW.rowid);
 END;

-- cp8_revision_self_diagnosis_log_delete
CREATE TRIGGER cp8_revision_self_diagnosis_log_delete
 AFTER DELETE ON "self_diagnosis_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='self_diagnosis_log' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_self_diagnosis_log_insert
CREATE TRIGGER cp8_revision_self_diagnosis_log_insert
 AFTER INSERT ON "self_diagnosis_log" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('self_diagnosis_log',NEW.rowid);
 END;

-- cp8_revision_self_diagnosis_log_update
CREATE TRIGGER cp8_revision_self_diagnosis_log_update
 AFTER UPDATE ON "self_diagnosis_log" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='self_diagnosis_log' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('self_diagnosis_log',NEW.rowid);
 END;

-- cp8_revision_service_recovery_event_v20_delete
CREATE TRIGGER cp8_revision_service_recovery_event_v20_delete
 AFTER DELETE ON "service_recovery_event_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='service_recovery_event_v20' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_service_recovery_event_v20_insert
CREATE TRIGGER cp8_revision_service_recovery_event_v20_insert
 AFTER INSERT ON "service_recovery_event_v20" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('service_recovery_event_v20',NEW.rowid);
 END;

-- cp8_revision_service_recovery_event_v20_update
CREATE TRIGGER cp8_revision_service_recovery_event_v20_update
 AFTER UPDATE ON "service_recovery_event_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='service_recovery_event_v20' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('service_recovery_event_v20',NEW.rowid);
 END;

-- cp8_revision_shadow_candidates_delete
CREATE TRIGGER cp8_revision_shadow_candidates_delete
 AFTER DELETE ON "shadow_candidates" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='shadow_candidates' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_shadow_candidates_insert
CREATE TRIGGER cp8_revision_shadow_candidates_insert
 AFTER INSERT ON "shadow_candidates" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shadow_candidates',NEW.rowid);
 END;

-- cp8_revision_shadow_candidates_update
CREATE TRIGGER cp8_revision_shadow_candidates_update
 AFTER UPDATE ON "shadow_candidates" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='shadow_candidates' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shadow_candidates',NEW.rowid);
 END;

-- cp8_revision_shadow_health_gate_delete
CREATE TRIGGER cp8_revision_shadow_health_gate_delete
 AFTER DELETE ON "shadow_health_gate" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='shadow_health_gate' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_shadow_health_gate_insert
CREATE TRIGGER cp8_revision_shadow_health_gate_insert
 AFTER INSERT ON "shadow_health_gate" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shadow_health_gate',NEW.rowid);
 END;

-- cp8_revision_shadow_health_gate_update
CREATE TRIGGER cp8_revision_shadow_health_gate_update
 AFTER UPDATE ON "shadow_health_gate" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='shadow_health_gate' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shadow_health_gate',NEW.rowid);
 END;

-- cp8_revision_shape_memory_delete
CREATE TRIGGER cp8_revision_shape_memory_delete
 AFTER DELETE ON "shape_memory" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='shape_memory' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_shape_memory_insert
CREATE TRIGGER cp8_revision_shape_memory_insert
 AFTER INSERT ON "shape_memory" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shape_memory',NEW.rowid);
 END;

-- cp8_revision_shape_memory_update
CREATE TRIGGER cp8_revision_shape_memory_update
 AFTER UPDATE ON "shape_memory" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='shape_memory' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shape_memory',NEW.rowid);
 END;

-- cp8_revision_shape_vector_delete
CREATE TRIGGER cp8_revision_shape_vector_delete
 AFTER DELETE ON "shape_vector" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='shape_vector' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_shape_vector_insert
CREATE TRIGGER cp8_revision_shape_vector_insert
 AFTER INSERT ON "shape_vector" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shape_vector',NEW.rowid);
 END;

-- cp8_revision_shape_vector_update
CREATE TRIGGER cp8_revision_shape_vector_update
 AFTER UPDATE ON "shape_vector" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='shape_vector' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('shape_vector',NEW.rowid);
 END;

-- cp8_revision_source_health_matrix_delete
CREATE TRIGGER cp8_revision_source_health_matrix_delete
 AFTER DELETE ON "source_health_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='source_health_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_source_health_matrix_insert
CREATE TRIGGER cp8_revision_source_health_matrix_insert
 AFTER INSERT ON "source_health_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('source_health_matrix',NEW.rowid);
 END;

-- cp8_revision_source_health_matrix_update
CREATE TRIGGER cp8_revision_source_health_matrix_update
 AFTER UPDATE ON "source_health_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='source_health_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('source_health_matrix',NEW.rowid);
 END;

-- cp8_revision_state_health_matrix_delete
CREATE TRIGGER cp8_revision_state_health_matrix_delete
 AFTER DELETE ON "state_health_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='state_health_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_state_health_matrix_insert
CREATE TRIGGER cp8_revision_state_health_matrix_insert
 AFTER INSERT ON "state_health_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('state_health_matrix',NEW.rowid);
 END;

-- cp8_revision_state_health_matrix_update
CREATE TRIGGER cp8_revision_state_health_matrix_update
 AFTER UPDATE ON "state_health_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='state_health_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('state_health_matrix',NEW.rowid);
 END;

-- cp8_revision_state_number_profile_delete
CREATE TRIGGER cp8_revision_state_number_profile_delete
 AFTER DELETE ON "state_number_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='state_number_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_state_number_profile_insert
CREATE TRIGGER cp8_revision_state_number_profile_insert
 AFTER INSERT ON "state_number_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('state_number_profile',NEW.rowid);
 END;

-- cp8_revision_state_number_profile_update
CREATE TRIGGER cp8_revision_state_number_profile_update
 AFTER UPDATE ON "state_number_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='state_number_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('state_number_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_assist_profile_delete
CREATE TRIGGER cp8_revision_teacher_assist_profile_delete
 AFTER DELETE ON "teacher_assist_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_assist_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_assist_profile_insert
CREATE TRIGGER cp8_revision_teacher_assist_profile_insert
 AFTER INSERT ON "teacher_assist_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_assist_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_assist_profile_update
CREATE TRIGGER cp8_revision_teacher_assist_profile_update
 AFTER UPDATE ON "teacher_assist_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_assist_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_assist_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_domain_stat_v20_delete
CREATE TRIGGER cp8_revision_teacher_domain_stat_v20_delete
 AFTER DELETE ON "teacher_domain_stat_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_domain_stat_v20' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_domain_stat_v20_insert
CREATE TRIGGER cp8_revision_teacher_domain_stat_v20_insert
 AFTER INSERT ON "teacher_domain_stat_v20" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_domain_stat_v20',NEW.rowid);
 END;

-- cp8_revision_teacher_domain_stat_v20_update
CREATE TRIGGER cp8_revision_teacher_domain_stat_v20_update
 AFTER UPDATE ON "teacher_domain_stat_v20" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_domain_stat_v20' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_domain_stat_v20',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_combo_profile_delete
CREATE TRIGGER cp8_revision_teacher_matrix_combo_profile_delete
 AFTER DELETE ON "teacher_matrix_combo_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_combo_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_matrix_combo_profile_insert
CREATE TRIGGER cp8_revision_teacher_matrix_combo_profile_insert
 AFTER INSERT ON "teacher_matrix_combo_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_combo_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_combo_profile_update
CREATE TRIGGER cp8_revision_teacher_matrix_combo_profile_update
 AFTER UPDATE ON "teacher_matrix_combo_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_combo_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_combo_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_profile_delete
CREATE TRIGGER cp8_revision_teacher_matrix_profile_delete
 AFTER DELETE ON "teacher_matrix_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_matrix_profile_insert
CREATE TRIGGER cp8_revision_teacher_matrix_profile_insert
 AFTER INSERT ON "teacher_matrix_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_profile_update
CREATE TRIGGER cp8_revision_teacher_matrix_profile_update
 AFTER UPDATE ON "teacher_matrix_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_report_delete
CREATE TRIGGER cp8_revision_teacher_matrix_report_delete
 AFTER DELETE ON "teacher_matrix_report" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_report' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_matrix_report_insert
CREATE TRIGGER cp8_revision_teacher_matrix_report_insert
 AFTER INSERT ON "teacher_matrix_report" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_report',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_report_update
CREATE TRIGGER cp8_revision_teacher_matrix_report_update
 AFTER UPDATE ON "teacher_matrix_report" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_report' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_report',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_sample_delete
CREATE TRIGGER cp8_revision_teacher_matrix_sample_delete
 AFTER DELETE ON "teacher_matrix_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_matrix_sample_insert
CREATE TRIGGER cp8_revision_teacher_matrix_sample_insert
 AFTER INSERT ON "teacher_matrix_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_sample',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_sample_update
CREATE TRIGGER cp8_revision_teacher_matrix_sample_update
 AFTER UPDATE ON "teacher_matrix_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_sample',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_state_profile_delete
CREATE TRIGGER cp8_revision_teacher_matrix_state_profile_delete
 AFTER DELETE ON "teacher_matrix_state_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_state_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_matrix_state_profile_insert
CREATE TRIGGER cp8_revision_teacher_matrix_state_profile_insert
 AFTER INSERT ON "teacher_matrix_state_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_state_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_matrix_state_profile_update
CREATE TRIGGER cp8_revision_teacher_matrix_state_profile_update
 AFTER UPDATE ON "teacher_matrix_state_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_matrix_state_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_matrix_state_profile',NEW.rowid);
 END;

-- cp8_revision_teacher_number_matrix_delete
CREATE TRIGGER cp8_revision_teacher_number_matrix_delete
 AFTER DELETE ON "teacher_number_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_number_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_number_matrix_insert
CREATE TRIGGER cp8_revision_teacher_number_matrix_insert
 AFTER INSERT ON "teacher_number_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_number_matrix',NEW.rowid);
 END;

-- cp8_revision_teacher_number_matrix_update
CREATE TRIGGER cp8_revision_teacher_number_matrix_update
 AFTER UPDATE ON "teacher_number_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_number_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_number_matrix',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_case_delete
CREATE TRIGGER cp8_revision_teacher_replay_case_delete
 AFTER DELETE ON "teacher_replay_case" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_case' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_replay_case_insert
CREATE TRIGGER cp8_revision_teacher_replay_case_insert
 AFTER INSERT ON "teacher_replay_case" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_case',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_case_update
CREATE TRIGGER cp8_revision_teacher_replay_case_update
 AFTER UPDATE ON "teacher_replay_case" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_case' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_case',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_counterfactual_delete
CREATE TRIGGER cp8_revision_teacher_replay_counterfactual_delete
 AFTER DELETE ON "teacher_replay_counterfactual" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_counterfactual' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_replay_counterfactual_insert
CREATE TRIGGER cp8_revision_teacher_replay_counterfactual_insert
 AFTER INSERT ON "teacher_replay_counterfactual" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_counterfactual',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_counterfactual_update
CREATE TRIGGER cp8_revision_teacher_replay_counterfactual_update
 AFTER UPDATE ON "teacher_replay_counterfactual" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_counterfactual' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_counterfactual',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_cursor_delete
CREATE TRIGGER cp8_revision_teacher_replay_cursor_delete
 AFTER DELETE ON "teacher_replay_cursor" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_cursor' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_replay_cursor_insert
CREATE TRIGGER cp8_revision_teacher_replay_cursor_insert
 AFTER INSERT ON "teacher_replay_cursor" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_cursor',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_cursor_update
CREATE TRIGGER cp8_revision_teacher_replay_cursor_update
 AFTER UPDATE ON "teacher_replay_cursor" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_cursor' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_cursor',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_report_delete
CREATE TRIGGER cp8_revision_teacher_replay_report_delete
 AFTER DELETE ON "teacher_replay_report" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_report' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_replay_report_insert
CREATE TRIGGER cp8_revision_teacher_replay_report_insert
 AFTER INSERT ON "teacher_replay_report" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_report',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_report_update
CREATE TRIGGER cp8_revision_teacher_replay_report_update
 AFTER UPDATE ON "teacher_replay_report" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_report' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_report',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_stability_delete
CREATE TRIGGER cp8_revision_teacher_replay_stability_delete
 AFTER DELETE ON "teacher_replay_stability" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_stability' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_replay_stability_insert
CREATE TRIGGER cp8_revision_teacher_replay_stability_insert
 AFTER INSERT ON "teacher_replay_stability" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_stability',NEW.rowid);
 END;

-- cp8_revision_teacher_replay_stability_update
CREATE TRIGGER cp8_revision_teacher_replay_stability_update
 AFTER UPDATE ON "teacher_replay_stability" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_replay_stability' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_replay_stability',NEW.rowid);
 END;

-- cp8_revision_teacher_snapshot_delete
CREATE TRIGGER cp8_revision_teacher_snapshot_delete
 AFTER DELETE ON "teacher_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_teacher_snapshot_insert
CREATE TRIGGER cp8_revision_teacher_snapshot_insert
 AFTER INSERT ON "teacher_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_snapshot',NEW.rowid);
 END;

-- cp8_revision_teacher_snapshot_update
CREATE TRIGGER cp8_revision_teacher_snapshot_update
 AFTER UPDATE ON "teacher_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='teacher_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('teacher_snapshot',NEW.rowid);
 END;

-- cp8_revision_third_guard_matrix_delete
CREATE TRIGGER cp8_revision_third_guard_matrix_delete
 AFTER DELETE ON "third_guard_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='third_guard_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_third_guard_matrix_insert
CREATE TRIGGER cp8_revision_third_guard_matrix_insert
 AFTER INSERT ON "third_guard_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('third_guard_matrix',NEW.rowid);
 END;

-- cp8_revision_third_guard_matrix_update
CREATE TRIGGER cp8_revision_third_guard_matrix_update
 AFTER UPDATE ON "third_guard_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='third_guard_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('third_guard_matrix',NEW.rowid);
 END;

-- cp8_revision_trades_delete
CREATE TRIGGER cp8_revision_trades_delete
 AFTER DELETE ON "trades" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='trades' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_trades_insert
CREATE TRIGGER cp8_revision_trades_insert
 AFTER INSERT ON "trades" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('trades',NEW.rowid);
 END;

-- cp8_revision_trades_update
CREATE TRIGGER cp8_revision_trades_update
 AFTER UPDATE ON "trades" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='trades' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('trades',NEW.rowid);
 END;

-- cp8_revision_v20_action_matrix_delete
CREATE TRIGGER cp8_revision_v20_action_matrix_delete
 AFTER DELETE ON "v20_action_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_action_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_action_matrix_insert
CREATE TRIGGER cp8_revision_v20_action_matrix_insert
 AFTER INSERT ON "v20_action_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_action_matrix',NEW.rowid);
 END;

-- cp8_revision_v20_action_matrix_update
CREATE TRIGGER cp8_revision_v20_action_matrix_update
 AFTER UPDATE ON "v20_action_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_action_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_action_matrix',NEW.rowid);
 END;

-- cp8_revision_v20_background_cursor_delete
CREATE TRIGGER cp8_revision_v20_background_cursor_delete
 AFTER DELETE ON "v20_background_cursor" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_background_cursor' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_background_cursor_insert
CREATE TRIGGER cp8_revision_v20_background_cursor_insert
 AFTER INSERT ON "v20_background_cursor" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_background_cursor',NEW.rowid);
 END;

-- cp8_revision_v20_background_cursor_update
CREATE TRIGGER cp8_revision_v20_background_cursor_update
 AFTER UPDATE ON "v20_background_cursor" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_background_cursor' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_background_cursor',NEW.rowid);
 END;

-- cp8_revision_v20_cause_matrix_delete
CREATE TRIGGER cp8_revision_v20_cause_matrix_delete
 AFTER DELETE ON "v20_cause_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_cause_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_cause_matrix_insert
CREATE TRIGGER cp8_revision_v20_cause_matrix_insert
 AFTER INSERT ON "v20_cause_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_cause_matrix',NEW.rowid);
 END;

-- cp8_revision_v20_cause_matrix_update
CREATE TRIGGER cp8_revision_v20_cause_matrix_update
 AFTER UPDATE ON "v20_cause_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_cause_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_cause_matrix',NEW.rowid);
 END;

-- cp8_revision_v20_closed_loop_snapshot_delete
CREATE TRIGGER cp8_revision_v20_closed_loop_snapshot_delete
 AFTER DELETE ON "v20_closed_loop_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_closed_loop_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_closed_loop_snapshot_insert
CREATE TRIGGER cp8_revision_v20_closed_loop_snapshot_insert
 AFTER INSERT ON "v20_closed_loop_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_closed_loop_snapshot',NEW.rowid);
 END;

-- cp8_revision_v20_closed_loop_snapshot_update
CREATE TRIGGER cp8_revision_v20_closed_loop_snapshot_update
 AFTER UPDATE ON "v20_closed_loop_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_closed_loop_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_closed_loop_snapshot',NEW.rowid);
 END;

-- cp8_revision_v20_false_strong_delete
CREATE TRIGGER cp8_revision_v20_false_strong_delete
 AFTER DELETE ON "v20_false_strong" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_false_strong' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_false_strong_insert
CREATE TRIGGER cp8_revision_v20_false_strong_insert
 AFTER INSERT ON "v20_false_strong" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_false_strong',NEW.rowid);
 END;

-- cp8_revision_v20_false_strong_update
CREATE TRIGGER cp8_revision_v20_false_strong_update
 AFTER UPDATE ON "v20_false_strong" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_false_strong' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_false_strong',NEW.rowid);
 END;

-- cp8_revision_v20_frequency_state_delete
CREATE TRIGGER cp8_revision_v20_frequency_state_delete
 AFTER DELETE ON "v20_frequency_state" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_frequency_state' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_frequency_state_insert
CREATE TRIGGER cp8_revision_v20_frequency_state_insert
 AFTER INSERT ON "v20_frequency_state" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_frequency_state',NEW.rowid);
 END;

-- cp8_revision_v20_frequency_state_update
CREATE TRIGGER cp8_revision_v20_frequency_state_update
 AFTER UPDATE ON "v20_frequency_state" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_frequency_state' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_frequency_state',NEW.rowid);
 END;

-- cp8_revision_v20_layer_snapshot_delete
CREATE TRIGGER cp8_revision_v20_layer_snapshot_delete
 AFTER DELETE ON "v20_layer_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_layer_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_layer_snapshot_insert
CREATE TRIGGER cp8_revision_v20_layer_snapshot_insert
 AFTER INSERT ON "v20_layer_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_layer_snapshot',NEW.rowid);
 END;

-- cp8_revision_v20_layer_snapshot_update
CREATE TRIGGER cp8_revision_v20_layer_snapshot_update
 AFTER UPDATE ON "v20_layer_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_layer_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_layer_snapshot',NEW.rowid);
 END;

-- cp8_revision_v20_link_candidate_snapshot_delete
CREATE TRIGGER cp8_revision_v20_link_candidate_snapshot_delete
 AFTER DELETE ON "v20_link_candidate_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_link_candidate_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_link_candidate_snapshot_insert
CREATE TRIGGER cp8_revision_v20_link_candidate_snapshot_insert
 AFTER INSERT ON "v20_link_candidate_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_link_candidate_snapshot',NEW.rowid);
 END;

-- cp8_revision_v20_link_candidate_snapshot_update
CREATE TRIGGER cp8_revision_v20_link_candidate_snapshot_update
 AFTER UPDATE ON "v20_link_candidate_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_link_candidate_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_link_candidate_snapshot',NEW.rowid);
 END;

-- cp8_revision_v20_reverse_reason_sample_delete
CREATE TRIGGER cp8_revision_v20_reverse_reason_sample_delete
 AFTER DELETE ON "v20_reverse_reason_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_reverse_reason_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_reverse_reason_sample_insert
CREATE TRIGGER cp8_revision_v20_reverse_reason_sample_insert
 AFTER INSERT ON "v20_reverse_reason_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_reverse_reason_sample',NEW.rowid);
 END;

-- cp8_revision_v20_reverse_reason_sample_update
CREATE TRIGGER cp8_revision_v20_reverse_reason_sample_update
 AFTER UPDATE ON "v20_reverse_reason_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_reverse_reason_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_reverse_reason_sample',NEW.rowid);
 END;

-- cp8_revision_v20_rule_health_delete
CREATE TRIGGER cp8_revision_v20_rule_health_delete
 AFTER DELETE ON "v20_rule_health" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_rule_health' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_rule_health_insert
CREATE TRIGGER cp8_revision_v20_rule_health_insert
 AFTER INSERT ON "v20_rule_health" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_rule_health',NEW.rowid);
 END;

-- cp8_revision_v20_rule_health_update
CREATE TRIGGER cp8_revision_v20_rule_health_update
 AFTER UPDATE ON "v20_rule_health" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_rule_health' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_rule_health',NEW.rowid);
 END;

-- cp8_revision_v20_shadow_domain_stat_delete
CREATE TRIGGER cp8_revision_v20_shadow_domain_stat_delete
 AFTER DELETE ON "v20_shadow_domain_stat" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_shadow_domain_stat' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_shadow_domain_stat_insert
CREATE TRIGGER cp8_revision_v20_shadow_domain_stat_insert
 AFTER INSERT ON "v20_shadow_domain_stat" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_shadow_domain_stat',NEW.rowid);
 END;

-- cp8_revision_v20_shadow_domain_stat_update
CREATE TRIGGER cp8_revision_v20_shadow_domain_stat_update
 AFTER UPDATE ON "v20_shadow_domain_stat" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_shadow_domain_stat' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_shadow_domain_stat',NEW.rowid);
 END;

-- cp8_revision_v20_shadow_health_delete
CREATE TRIGGER cp8_revision_v20_shadow_health_delete
 AFTER DELETE ON "v20_shadow_health" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_shadow_health' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_shadow_health_insert
CREATE TRIGGER cp8_revision_v20_shadow_health_insert
 AFTER INSERT ON "v20_shadow_health" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_shadow_health',NEW.rowid);
 END;

-- cp8_revision_v20_shadow_health_update
CREATE TRIGGER cp8_revision_v20_shadow_health_update
 AFTER UPDATE ON "v20_shadow_health" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_shadow_health' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_shadow_health',NEW.rowid);
 END;

-- cp8_revision_v20_similar_window_delete
CREATE TRIGGER cp8_revision_v20_similar_window_delete
 AFTER DELETE ON "v20_similar_window" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_similar_window' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_similar_window_insert
CREATE TRIGGER cp8_revision_v20_similar_window_insert
 AFTER INSERT ON "v20_similar_window" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_similar_window',NEW.rowid);
 END;

-- cp8_revision_v20_similar_window_update
CREATE TRIGGER cp8_revision_v20_similar_window_update
 AFTER UPDATE ON "v20_similar_window" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_similar_window' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_similar_window',NEW.rowid);
 END;

-- cp8_revision_v20_teacher_number_delete
CREATE TRIGGER cp8_revision_v20_teacher_number_delete
 AFTER DELETE ON "v20_teacher_number" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_teacher_number' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_teacher_number_insert
CREATE TRIGGER cp8_revision_v20_teacher_number_insert
 AFTER INSERT ON "v20_teacher_number" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_teacher_number',NEW.rowid);
 END;

-- cp8_revision_v20_teacher_number_update
CREATE TRIGGER cp8_revision_v20_teacher_number_update
 AFTER UPDATE ON "v20_teacher_number" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_teacher_number' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_teacher_number',NEW.rowid);
 END;

-- cp8_revision_v20_window_state_delete
CREATE TRIGGER cp8_revision_v20_window_state_delete
 AFTER DELETE ON "v20_window_state" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_window_state' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v20_window_state_insert
CREATE TRIGGER cp8_revision_v20_window_state_insert
 AFTER INSERT ON "v20_window_state" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_window_state',NEW.rowid);
 END;

-- cp8_revision_v20_window_state_update
CREATE TRIGGER cp8_revision_v20_window_state_update
 AFTER UPDATE ON "v20_window_state" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v20_window_state' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v20_window_state',NEW.rowid);
 END;

-- cp8_revision_v48_diagnostic_observation_delete
CREATE TRIGGER cp8_revision_v48_diagnostic_observation_delete
 AFTER DELETE ON "v48_diagnostic_observation" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v48_diagnostic_observation' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v48_diagnostic_observation_insert
CREATE TRIGGER cp8_revision_v48_diagnostic_observation_insert
 AFTER INSERT ON "v48_diagnostic_observation" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v48_diagnostic_observation',NEW.rowid);
 END;

-- cp8_revision_v48_diagnostic_observation_update
CREATE TRIGGER cp8_revision_v48_diagnostic_observation_update
 AFTER UPDATE ON "v48_diagnostic_observation" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v48_diagnostic_observation' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v48_diagnostic_observation',NEW.rowid);
 END;

-- cp8_revision_v48_model_artifact_delete
CREATE TRIGGER cp8_revision_v48_model_artifact_delete
 AFTER DELETE ON "v48_model_artifact" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v48_model_artifact' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v48_model_artifact_insert
CREATE TRIGGER cp8_revision_v48_model_artifact_insert
 AFTER INSERT ON "v48_model_artifact" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v48_model_artifact',NEW.rowid);
 END;

-- cp8_revision_v48_model_artifact_update
CREATE TRIGGER cp8_revision_v48_model_artifact_update
 AFTER UPDATE ON "v48_model_artifact" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v48_model_artifact' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v48_model_artifact',NEW.rowid);
 END;

-- cp8_revision_v49_best_opportunity_delete
CREATE TRIGGER cp8_revision_v49_best_opportunity_delete
 AFTER DELETE ON "v49_best_opportunity" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_best_opportunity' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v49_best_opportunity_insert
CREATE TRIGGER cp8_revision_v49_best_opportunity_insert
 AFTER INSERT ON "v49_best_opportunity" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_best_opportunity',NEW.rowid);
 END;

-- cp8_revision_v49_best_opportunity_update
CREATE TRIGGER cp8_revision_v49_best_opportunity_update
 AFTER UPDATE ON "v49_best_opportunity" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_best_opportunity' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_best_opportunity',NEW.rowid);
 END;

-- cp8_revision_v49_cause_attribution_delete
CREATE TRIGGER cp8_revision_v49_cause_attribution_delete
 AFTER DELETE ON "v49_cause_attribution" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_cause_attribution' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v49_cause_attribution_insert
CREATE TRIGGER cp8_revision_v49_cause_attribution_insert
 AFTER INSERT ON "v49_cause_attribution" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_cause_attribution',NEW.rowid);
 END;

-- cp8_revision_v49_cause_attribution_update
CREATE TRIGGER cp8_revision_v49_cause_attribution_update
 AFTER UPDATE ON "v49_cause_attribution" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_cause_attribution' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_cause_attribution',NEW.rowid);
 END;

-- cp8_revision_v49_dirichlet_calibration_delete
CREATE TRIGGER cp8_revision_v49_dirichlet_calibration_delete
 AFTER DELETE ON "v49_dirichlet_calibration" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_dirichlet_calibration' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v49_dirichlet_calibration_insert
CREATE TRIGGER cp8_revision_v49_dirichlet_calibration_insert
 AFTER INSERT ON "v49_dirichlet_calibration" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_dirichlet_calibration',NEW.rowid);
 END;

-- cp8_revision_v49_dirichlet_calibration_update
CREATE TRIGGER cp8_revision_v49_dirichlet_calibration_update
 AFTER UPDATE ON "v49_dirichlet_calibration" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_dirichlet_calibration' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_dirichlet_calibration',NEW.rowid);
 END;

-- cp8_revision_v49_expert_stats_delete
CREATE TRIGGER cp8_revision_v49_expert_stats_delete
 AFTER DELETE ON "v49_expert_stats" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_expert_stats' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v49_expert_stats_insert
CREATE TRIGGER cp8_revision_v49_expert_stats_insert
 AFTER INSERT ON "v49_expert_stats" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_expert_stats',NEW.rowid);
 END;

-- cp8_revision_v49_expert_stats_update
CREATE TRIGGER cp8_revision_v49_expert_stats_update
 AFTER UPDATE ON "v49_expert_stats" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_expert_stats' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_expert_stats',NEW.rowid);
 END;

-- cp8_revision_v49_position_action_delete
CREATE TRIGGER cp8_revision_v49_position_action_delete
 AFTER DELETE ON "v49_position_action" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_position_action' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v49_position_action_insert
CREATE TRIGGER cp8_revision_v49_position_action_insert
 AFTER INSERT ON "v49_position_action" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_position_action',NEW.rowid);
 END;

-- cp8_revision_v49_position_action_update
CREATE TRIGGER cp8_revision_v49_position_action_update
 AFTER UPDATE ON "v49_position_action" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_position_action' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_position_action',NEW.rowid);
 END;

-- cp8_revision_v49_predraw_snapshot_delete
CREATE TRIGGER cp8_revision_v49_predraw_snapshot_delete
 AFTER DELETE ON "v49_predraw_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_predraw_snapshot' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v49_predraw_snapshot_insert
CREATE TRIGGER cp8_revision_v49_predraw_snapshot_insert
 AFTER INSERT ON "v49_predraw_snapshot" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_predraw_snapshot',NEW.rowid);
 END;

-- cp8_revision_v49_predraw_snapshot_update
CREATE TRIGGER cp8_revision_v49_predraw_snapshot_update
 AFTER UPDATE ON "v49_predraw_snapshot" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_predraw_snapshot' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_predraw_snapshot',NEW.rowid);
 END;

-- cp8_revision_v49_teacher_posterior_delete
CREATE TRIGGER cp8_revision_v49_teacher_posterior_delete
 AFTER DELETE ON "v49_teacher_posterior" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_teacher_posterior' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v49_teacher_posterior_insert
CREATE TRIGGER cp8_revision_v49_teacher_posterior_insert
 AFTER INSERT ON "v49_teacher_posterior" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_teacher_posterior',NEW.rowid);
 END;

-- cp8_revision_v49_teacher_posterior_update
CREATE TRIGGER cp8_revision_v49_teacher_posterior_update
 AFTER UPDATE ON "v49_teacher_posterior" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_teacher_posterior' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_teacher_posterior',NEW.rowid);
 END;

-- cp8_revision_v49_ui_cache_delete
CREATE TRIGGER cp8_revision_v49_ui_cache_delete
 AFTER DELETE ON "v49_ui_cache" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_ui_cache' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v49_ui_cache_insert
CREATE TRIGGER cp8_revision_v49_ui_cache_insert
 AFTER INSERT ON "v49_ui_cache" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_ui_cache',NEW.rowid);
 END;

-- cp8_revision_v49_ui_cache_update
CREATE TRIGGER cp8_revision_v49_ui_cache_update
 AFTER UPDATE ON "v49_ui_cache" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v49_ui_cache' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v49_ui_cache',NEW.rowid);
 END;

-- cp8_revision_v50_authorization_audit_delete
CREATE TRIGGER cp8_revision_v50_authorization_audit_delete
 AFTER DELETE ON "v50_authorization_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_authorization_audit' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_authorization_audit_insert
CREATE TRIGGER cp8_revision_v50_authorization_audit_insert
 AFTER INSERT ON "v50_authorization_audit" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_authorization_audit',NEW.rowid);
 END;

-- cp8_revision_v50_authorization_audit_update
CREATE TRIGGER cp8_revision_v50_authorization_audit_update
 AFTER UPDATE ON "v50_authorization_audit" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_authorization_audit' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_authorization_audit',NEW.rowid);
 END;

-- cp8_revision_v50_cause_attribution_delete
CREATE TRIGGER cp8_revision_v50_cause_attribution_delete
 AFTER DELETE ON "v50_cause_attribution" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_cause_attribution' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_cause_attribution_insert
CREATE TRIGGER cp8_revision_v50_cause_attribution_insert
 AFTER INSERT ON "v50_cause_attribution" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_cause_attribution',NEW.rowid);
 END;

-- cp8_revision_v50_cause_attribution_update
CREATE TRIGGER cp8_revision_v50_cause_attribution_update
 AFTER UPDATE ON "v50_cause_attribution" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_cause_attribution' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_cause_attribution',NEW.rowid);
 END;

-- cp8_revision_v50_expert_regret_delete
CREATE TRIGGER cp8_revision_v50_expert_regret_delete
 AFTER DELETE ON "v50_expert_regret" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_expert_regret' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_expert_regret_insert
CREATE TRIGGER cp8_revision_v50_expert_regret_insert
 AFTER INSERT ON "v50_expert_regret" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_expert_regret',NEW.rowid);
 END;

-- cp8_revision_v50_expert_regret_update
CREATE TRIGGER cp8_revision_v50_expert_regret_update
 AFTER UPDATE ON "v50_expert_regret" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_expert_regret' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_expert_regret',NEW.rowid);
 END;

-- cp8_revision_v50_issue_prediction_delete
CREATE TRIGGER cp8_revision_v50_issue_prediction_delete
 AFTER DELETE ON "v50_issue_prediction" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_issue_prediction' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_issue_prediction_insert
CREATE TRIGGER cp8_revision_v50_issue_prediction_insert
 AFTER INSERT ON "v50_issue_prediction" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_issue_prediction',NEW.rowid);
 END;

-- cp8_revision_v50_issue_prediction_update
CREATE TRIGGER cp8_revision_v50_issue_prediction_update
 AFTER UPDATE ON "v50_issue_prediction" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_issue_prediction' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_issue_prediction',NEW.rowid);
 END;

-- cp8_revision_v50_ledger_error_delete
CREATE TRIGGER cp8_revision_v50_ledger_error_delete
 AFTER DELETE ON "v50_ledger_error" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_ledger_error' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_ledger_error_insert
CREATE TRIGGER cp8_revision_v50_ledger_error_insert
 AFTER INSERT ON "v50_ledger_error" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_ledger_error',NEW.rowid);
 END;

-- cp8_revision_v50_ledger_error_update
CREATE TRIGGER cp8_revision_v50_ledger_error_update
 AFTER UPDATE ON "v50_ledger_error" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_ledger_error' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_ledger_error',NEW.rowid);
 END;

-- cp8_revision_v50_number_event_delete
CREATE TRIGGER cp8_revision_v50_number_event_delete
 AFTER DELETE ON "v50_number_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_number_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_number_event_insert
CREATE TRIGGER cp8_revision_v50_number_event_insert
 AFTER INSERT ON "v50_number_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_number_event',NEW.rowid);
 END;

-- cp8_revision_v50_number_event_update
CREATE TRIGGER cp8_revision_v50_number_event_update
 AFTER UPDATE ON "v50_number_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_number_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_number_event',NEW.rowid);
 END;

-- cp8_revision_v50_online_residual_delete
CREATE TRIGGER cp8_revision_v50_online_residual_delete
 AFTER DELETE ON "v50_online_residual" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_online_residual' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_online_residual_insert
CREATE TRIGGER cp8_revision_v50_online_residual_insert
 AFTER INSERT ON "v50_online_residual" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_online_residual',NEW.rowid);
 END;

-- cp8_revision_v50_online_residual_update
CREATE TRIGGER cp8_revision_v50_online_residual_update
 AFTER UPDATE ON "v50_online_residual" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_online_residual' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_online_residual',NEW.rowid);
 END;

-- cp8_revision_v50_performance_span_delete
CREATE TRIGGER cp8_revision_v50_performance_span_delete
 AFTER DELETE ON "v50_performance_span" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_performance_span' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_performance_span_insert
CREATE TRIGGER cp8_revision_v50_performance_span_insert
 AFTER INSERT ON "v50_performance_span" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_performance_span',NEW.rowid);
 END;

-- cp8_revision_v50_performance_span_update
CREATE TRIGGER cp8_revision_v50_performance_span_update
 AFTER UPDATE ON "v50_performance_span" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_performance_span' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_performance_span',NEW.rowid);
 END;

-- cp8_revision_v50_position_action_delete
CREATE TRIGGER cp8_revision_v50_position_action_delete
 AFTER DELETE ON "v50_position_action" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_position_action' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_position_action_insert
CREATE TRIGGER cp8_revision_v50_position_action_insert
 AFTER INSERT ON "v50_position_action" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_position_action',NEW.rowid);
 END;

-- cp8_revision_v50_position_action_update
CREATE TRIGGER cp8_revision_v50_position_action_update
 AFTER UPDATE ON "v50_position_action" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_position_action' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_position_action',NEW.rowid);
 END;

-- cp8_revision_v50_selected_decision_delete
CREATE TRIGGER cp8_revision_v50_selected_decision_delete
 AFTER DELETE ON "v50_selected_decision" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_selected_decision' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_selected_decision_insert
CREATE TRIGGER cp8_revision_v50_selected_decision_insert
 AFTER INSERT ON "v50_selected_decision" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_selected_decision',NEW.rowid);
 END;

-- cp8_revision_v50_selected_decision_update
CREATE TRIGGER cp8_revision_v50_selected_decision_update
 AFTER UPDATE ON "v50_selected_decision" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_selected_decision' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_selected_decision',NEW.rowid);
 END;

-- cp8_revision_v50_state_history_delete
CREATE TRIGGER cp8_revision_v50_state_history_delete
 AFTER DELETE ON "v50_state_history" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_state_history' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_state_history_insert
CREATE TRIGGER cp8_revision_v50_state_history_insert
 AFTER INSERT ON "v50_state_history" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_state_history',NEW.rowid);
 END;

-- cp8_revision_v50_state_history_update
CREATE TRIGGER cp8_revision_v50_state_history_update
 AFTER UPDATE ON "v50_state_history" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_state_history' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_state_history',NEW.rowid);
 END;

-- cp8_revision_v50_teacher_profile_delete
CREATE TRIGGER cp8_revision_v50_teacher_profile_delete
 AFTER DELETE ON "v50_teacher_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_teacher_profile' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v50_teacher_profile_insert
CREATE TRIGGER cp8_revision_v50_teacher_profile_insert
 AFTER INSERT ON "v50_teacher_profile" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_teacher_profile',NEW.rowid);
 END;

-- cp8_revision_v50_teacher_profile_update
CREATE TRIGGER cp8_revision_v50_teacher_profile_update
 AFTER UPDATE ON "v50_teacher_profile" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v50_teacher_profile' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v50_teacher_profile',NEW.rowid);
 END;

-- cp8_revision_v51_decision_authorization_delete
CREATE TRIGGER cp8_revision_v51_decision_authorization_delete
 AFTER DELETE ON "v51_decision_authorization" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_decision_authorization' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v51_decision_authorization_insert
CREATE TRIGGER cp8_revision_v51_decision_authorization_insert
 AFTER INSERT ON "v51_decision_authorization" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_decision_authorization',NEW.rowid);
 END;

-- cp8_revision_v51_decision_authorization_update
CREATE TRIGGER cp8_revision_v51_decision_authorization_update
 AFTER UPDATE ON "v51_decision_authorization" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_decision_authorization' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_decision_authorization',NEW.rowid);
 END;

-- cp8_revision_v51_issue_prediction_delete
CREATE TRIGGER cp8_revision_v51_issue_prediction_delete
 AFTER DELETE ON "v51_issue_prediction" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_issue_prediction' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v51_issue_prediction_insert
CREATE TRIGGER cp8_revision_v51_issue_prediction_insert
 AFTER INSERT ON "v51_issue_prediction" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_issue_prediction',NEW.rowid);
 END;

-- cp8_revision_v51_issue_prediction_update
CREATE TRIGGER cp8_revision_v51_issue_prediction_update
 AFTER UPDATE ON "v51_issue_prediction" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_issue_prediction' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_issue_prediction',NEW.rowid);
 END;

-- cp8_revision_v51_number_event_delete
CREATE TRIGGER cp8_revision_v51_number_event_delete
 AFTER DELETE ON "v51_number_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_number_event' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v51_number_event_insert
CREATE TRIGGER cp8_revision_v51_number_event_insert
 AFTER INSERT ON "v51_number_event" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_number_event',NEW.rowid);
 END;

-- cp8_revision_v51_number_event_update
CREATE TRIGGER cp8_revision_v51_number_event_update
 AFTER UPDATE ON "v51_number_event" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_number_event' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_number_event',NEW.rowid);
 END;

-- cp8_revision_v51_performance_span_delete
CREATE TRIGGER cp8_revision_v51_performance_span_delete
 AFTER DELETE ON "v51_performance_span" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_performance_span' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v51_performance_span_insert
CREATE TRIGGER cp8_revision_v51_performance_span_insert
 AFTER INSERT ON "v51_performance_span" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_performance_span',NEW.rowid);
 END;

-- cp8_revision_v51_performance_span_update
CREATE TRIGGER cp8_revision_v51_performance_span_update
 AFTER UPDATE ON "v51_performance_span" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_performance_span' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_performance_span',NEW.rowid);
 END;

-- cp8_revision_v51_permutation_matrix_delete
CREATE TRIGGER cp8_revision_v51_permutation_matrix_delete
 AFTER DELETE ON "v51_permutation_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_permutation_matrix' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v51_permutation_matrix_insert
CREATE TRIGGER cp8_revision_v51_permutation_matrix_insert
 AFTER INSERT ON "v51_permutation_matrix" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_permutation_matrix',NEW.rowid);
 END;

-- cp8_revision_v51_permutation_matrix_update
CREATE TRIGGER cp8_revision_v51_permutation_matrix_update
 AFTER UPDATE ON "v51_permutation_matrix" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_permutation_matrix' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_permutation_matrix',NEW.rowid);
 END;

-- cp8_revision_v51_position_action_delete
CREATE TRIGGER cp8_revision_v51_position_action_delete
 AFTER DELETE ON "v51_position_action" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_position_action' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v51_position_action_insert
CREATE TRIGGER cp8_revision_v51_position_action_insert
 AFTER INSERT ON "v51_position_action" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_position_action',NEW.rowid);
 END;

-- cp8_revision_v51_position_action_update
CREATE TRIGGER cp8_revision_v51_position_action_update
 AFTER UPDATE ON "v51_position_action" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_position_action' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_position_action',NEW.rowid);
 END;

-- cp8_revision_v51_selected_action_delete
CREATE TRIGGER cp8_revision_v51_selected_action_delete
 AFTER DELETE ON "v51_selected_action" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_selected_action' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v51_selected_action_insert
CREATE TRIGGER cp8_revision_v51_selected_action_insert
 AFTER INSERT ON "v51_selected_action" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_selected_action',NEW.rowid);
 END;

-- cp8_revision_v51_selected_action_update
CREATE TRIGGER cp8_revision_v51_selected_action_update
 AFTER UPDATE ON "v51_selected_action" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v51_selected_action' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v51_selected_action',NEW.rowid);
 END;

-- cp8_revision_v57_algorithm_predraw_r908_delete
CREATE TRIGGER cp8_revision_v57_algorithm_predraw_r908_delete
 AFTER DELETE ON "v57_algorithm_predraw_r908" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_algorithm_predraw_r908' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_algorithm_predraw_r908_insert
CREATE TRIGGER cp8_revision_v57_algorithm_predraw_r908_insert
 AFTER INSERT ON "v57_algorithm_predraw_r908" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_algorithm_predraw_r908',NEW.rowid);
 END;

-- cp8_revision_v57_algorithm_predraw_r908_update
CREATE TRIGGER cp8_revision_v57_algorithm_predraw_r908_update
 AFTER UPDATE ON "v57_algorithm_predraw_r908" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_algorithm_predraw_r908' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_algorithm_predraw_r908',NEW.rowid);
 END;

-- cp8_revision_v57_analysis_request_r36_delete
CREATE TRIGGER cp8_revision_v57_analysis_request_r36_delete
 AFTER DELETE ON "v57_analysis_request_r36" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_analysis_request_r36' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_analysis_request_r36_insert
CREATE TRIGGER cp8_revision_v57_analysis_request_r36_insert
 AFTER INSERT ON "v57_analysis_request_r36" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_analysis_request_r36',NEW.rowid);
 END;

-- cp8_revision_v57_analysis_request_r36_update
CREATE TRIGGER cp8_revision_v57_analysis_request_r36_update
 AFTER UPDATE ON "v57_analysis_request_r36" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_analysis_request_r36' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_analysis_request_r36',NEW.rowid);
 END;

-- cp8_revision_v57_atom_edge_r925_delete
CREATE TRIGGER cp8_revision_v57_atom_edge_r925_delete
 AFTER DELETE ON "v57_atom_edge_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_atom_edge_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_atom_edge_r925_insert
CREATE TRIGGER cp8_revision_v57_atom_edge_r925_insert
 AFTER INSERT ON "v57_atom_edge_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_atom_edge_r925',NEW.rowid);
 END;

-- cp8_revision_v57_atom_edge_r925_update
CREATE TRIGGER cp8_revision_v57_atom_edge_r925_update
 AFTER UPDATE ON "v57_atom_edge_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_atom_edge_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_atom_edge_r925',NEW.rowid);
 END;

-- cp8_revision_v57_atom_quality_r925_delete
CREATE TRIGGER cp8_revision_v57_atom_quality_r925_delete
 AFTER DELETE ON "v57_atom_quality_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_atom_quality_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_atom_quality_r925_insert
CREATE TRIGGER cp8_revision_v57_atom_quality_r925_insert
 AFTER INSERT ON "v57_atom_quality_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_atom_quality_r925',NEW.rowid);
 END;

-- cp8_revision_v57_atom_quality_r925_update
CREATE TRIGGER cp8_revision_v57_atom_quality_r925_update
 AFTER UPDATE ON "v57_atom_quality_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_atom_quality_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_atom_quality_r925',NEW.rowid);
 END;

-- cp8_revision_v57_attempt_resource_r927_delete
CREATE TRIGGER cp8_revision_v57_attempt_resource_r927_delete
 AFTER DELETE ON "v57_attempt_resource_r927" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_attempt_resource_r927' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_attempt_resource_r927_insert
CREATE TRIGGER cp8_revision_v57_attempt_resource_r927_insert
 AFTER INSERT ON "v57_attempt_resource_r927" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_attempt_resource_r927',NEW.rowid);
 END;

-- cp8_revision_v57_attempt_resource_r927_update
CREATE TRIGGER cp8_revision_v57_attempt_resource_r927_update
 AFTER UPDATE ON "v57_attempt_resource_r927" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_attempt_resource_r927' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_attempt_resource_r927',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_ablation_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_ablation_r930_delete
 AFTER DELETE ON "v57_batch34_ablation_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_ablation_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_ablation_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_ablation_r930_insert
 AFTER INSERT ON "v57_batch34_ablation_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_ablation_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_ablation_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_ablation_r930_update
 AFTER UPDATE ON "v57_batch34_ablation_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_ablation_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_ablation_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_atom_timing_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_atom_timing_r930_delete
 AFTER DELETE ON "v57_batch34_atom_timing_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_atom_timing_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_atom_timing_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_atom_timing_r930_insert
 AFTER INSERT ON "v57_batch34_atom_timing_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_atom_timing_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_atom_timing_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_atom_timing_r930_update
 AFTER UPDATE ON "v57_batch34_atom_timing_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_atom_timing_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_atom_timing_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_atom_value_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_atom_value_r930_delete
 AFTER DELETE ON "v57_batch34_atom_value_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_atom_value_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_atom_value_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_atom_value_r930_insert
 AFTER INSERT ON "v57_batch34_atom_value_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_atom_value_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_atom_value_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_atom_value_r930_update
 AFTER UPDATE ON "v57_batch34_atom_value_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_atom_value_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_atom_value_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_bundle_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_bundle_r930_delete
 AFTER DELETE ON "v57_batch34_bundle_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_bundle_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_bundle_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_bundle_r930_insert
 AFTER INSERT ON "v57_batch34_bundle_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_bundle_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_bundle_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_bundle_r930_update
 AFTER UPDATE ON "v57_batch34_bundle_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_bundle_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_bundle_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_capture_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_capture_r930_delete
 AFTER DELETE ON "v57_batch34_capture_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_capture_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_capture_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_capture_r930_insert
 AFTER INSERT ON "v57_batch34_capture_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_capture_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_capture_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_capture_r930_update
 AFTER UPDATE ON "v57_batch34_capture_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_capture_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_capture_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_capture_transition_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_capture_transition_r930_delete
 AFTER DELETE ON "v57_batch34_capture_transition_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_capture_transition_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_capture_transition_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_capture_transition_r930_insert
 AFTER INSERT ON "v57_batch34_capture_transition_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_capture_transition_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_capture_transition_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_capture_transition_r930_update
 AFTER UPDATE ON "v57_batch34_capture_transition_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_capture_transition_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_capture_transition_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_evidence_link_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_evidence_link_r930_delete
 AFTER DELETE ON "v57_batch34_evidence_link_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_evidence_link_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_evidence_link_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_evidence_link_r930_insert
 AFTER INSERT ON "v57_batch34_evidence_link_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_evidence_link_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_evidence_link_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_evidence_link_r930_update
 AFTER UPDATE ON "v57_batch34_evidence_link_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_evidence_link_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_evidence_link_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_failure_case_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_failure_case_r930_delete
 AFTER DELETE ON "v57_batch34_failure_case_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_failure_case_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_failure_case_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_failure_case_r930_insert
 AFTER INSERT ON "v57_batch34_failure_case_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_failure_case_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_failure_case_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_failure_case_r930_update
 AFTER UPDATE ON "v57_batch34_failure_case_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_failure_case_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_failure_case_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_hypothesis_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_hypothesis_r930_delete
 AFTER DELETE ON "v57_batch34_hypothesis_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_hypothesis_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_hypothesis_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_hypothesis_r930_insert
 AFTER INSERT ON "v57_batch34_hypothesis_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_hypothesis_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_hypothesis_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_hypothesis_r930_update
 AFTER UPDATE ON "v57_batch34_hypothesis_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_hypothesis_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_hypothesis_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_lifecycle_event_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_lifecycle_event_r930_delete
 AFTER DELETE ON "v57_batch34_lifecycle_event_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_lifecycle_event_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_lifecycle_event_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_lifecycle_event_r930_insert
 AFTER INSERT ON "v57_batch34_lifecycle_event_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_lifecycle_event_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_lifecycle_event_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_lifecycle_event_r930_update
 AFTER UPDATE ON "v57_batch34_lifecycle_event_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_lifecycle_event_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_lifecycle_event_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_module_event_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_module_event_r930_delete
 AFTER DELETE ON "v57_batch34_module_event_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_module_event_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_module_event_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_module_event_r930_insert
 AFTER INSERT ON "v57_batch34_module_event_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_module_event_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_module_event_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_module_event_r930_update
 AFTER UPDATE ON "v57_batch34_module_event_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_module_event_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_module_event_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_paired_observation_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_paired_observation_r930_delete
 AFTER DELETE ON "v57_batch34_paired_observation_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_paired_observation_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_paired_observation_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_paired_observation_r930_insert
 AFTER INSERT ON "v57_batch34_paired_observation_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_paired_observation_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_paired_observation_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_paired_observation_r930_update
 AFTER UPDATE ON "v57_batch34_paired_observation_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_paired_observation_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_paired_observation_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_paired_window_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_paired_window_r930_delete
 AFTER DELETE ON "v57_batch34_paired_window_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_paired_window_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_paired_window_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_paired_window_r930_insert
 AFTER INSERT ON "v57_batch34_paired_window_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_paired_window_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_paired_window_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_paired_window_r930_update
 AFTER UPDATE ON "v57_batch34_paired_window_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_paired_window_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_paired_window_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_policy_decision_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_policy_decision_r930_delete
 AFTER DELETE ON "v57_batch34_policy_decision_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_policy_decision_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_policy_decision_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_policy_decision_r930_insert
 AFTER INSERT ON "v57_batch34_policy_decision_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_policy_decision_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_policy_decision_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_policy_decision_r930_update
 AFTER UPDATE ON "v57_batch34_policy_decision_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_policy_decision_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_policy_decision_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_policy_metric_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_policy_metric_r930_delete
 AFTER DELETE ON "v57_batch34_policy_metric_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_policy_metric_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_policy_metric_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_policy_metric_r930_insert
 AFTER INSERT ON "v57_batch34_policy_metric_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_policy_metric_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_policy_metric_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_policy_metric_r930_update
 AFTER UPDATE ON "v57_batch34_policy_metric_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_policy_metric_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_policy_metric_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_policy_outcome_r930_delete
CREATE TRIGGER cp8_revision_v57_batch34_policy_outcome_r930_delete
 AFTER DELETE ON "v57_batch34_policy_outcome_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_policy_outcome_r930' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_batch34_policy_outcome_r930_insert
CREATE TRIGGER cp8_revision_v57_batch34_policy_outcome_r930_insert
 AFTER INSERT ON "v57_batch34_policy_outcome_r930" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_policy_outcome_r930',NEW.rowid);
 END;

-- cp8_revision_v57_batch34_policy_outcome_r930_update
CREATE TRIGGER cp8_revision_v57_batch34_policy_outcome_r930_update
 AFTER UPDATE ON "v57_batch34_policy_outcome_r930" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_batch34_policy_outcome_r930' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_batch34_policy_outcome_r930',NEW.rowid);
 END;

-- cp8_revision_v57_candidate_coverage_r925_delete
CREATE TRIGGER cp8_revision_v57_candidate_coverage_r925_delete
 AFTER DELETE ON "v57_candidate_coverage_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_candidate_coverage_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_candidate_coverage_r925_insert
CREATE TRIGGER cp8_revision_v57_candidate_coverage_r925_insert
 AFTER INSERT ON "v57_candidate_coverage_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_candidate_coverage_r925',NEW.rowid);
 END;

-- cp8_revision_v57_candidate_coverage_r925_update
CREATE TRIGGER cp8_revision_v57_candidate_coverage_r925_update
 AFTER UPDATE ON "v57_candidate_coverage_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_candidate_coverage_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_candidate_coverage_r925',NEW.rowid);
 END;

-- cp8_revision_v57_capture_opportunity_r925_delete
CREATE TRIGGER cp8_revision_v57_capture_opportunity_r925_delete
 AFTER DELETE ON "v57_capture_opportunity_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_capture_opportunity_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_capture_opportunity_r925_insert
CREATE TRIGGER cp8_revision_v57_capture_opportunity_r925_insert
 AFTER INSERT ON "v57_capture_opportunity_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_capture_opportunity_r925',NEW.rowid);
 END;

-- cp8_revision_v57_capture_opportunity_r925_update
CREATE TRIGGER cp8_revision_v57_capture_opportunity_r925_update
 AFTER UPDATE ON "v57_capture_opportunity_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_capture_opportunity_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_capture_opportunity_r925',NEW.rowid);
 END;

-- cp8_revision_v57_controlled_opportunity_r922_delete
CREATE TRIGGER cp8_revision_v57_controlled_opportunity_r922_delete
 AFTER DELETE ON "v57_controlled_opportunity_r922" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_controlled_opportunity_r922' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_controlled_opportunity_r922_insert
CREATE TRIGGER cp8_revision_v57_controlled_opportunity_r922_insert
 AFTER INSERT ON "v57_controlled_opportunity_r922" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_controlled_opportunity_r922',NEW.rowid);
 END;

-- cp8_revision_v57_controlled_opportunity_r922_update
CREATE TRIGGER cp8_revision_v57_controlled_opportunity_r922_update
 AFTER UPDATE ON "v57_controlled_opportunity_r922" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_controlled_opportunity_r922' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_controlled_opportunity_r922',NEW.rowid);
 END;

-- cp8_revision_v57_critical_error_r815_delete
CREATE TRIGGER cp8_revision_v57_critical_error_r815_delete
 AFTER DELETE ON "v57_critical_error_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_critical_error_r815' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_critical_error_r815_insert
CREATE TRIGGER cp8_revision_v57_critical_error_r815_insert
 AFTER INSERT ON "v57_critical_error_r815" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_critical_error_r815',NEW.rowid);
 END;

-- cp8_revision_v57_critical_error_r815_update
CREATE TRIGGER cp8_revision_v57_critical_error_r815_update
 AFTER UPDATE ON "v57_critical_error_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_critical_error_r815' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_critical_error_r815',NEW.rowid);
 END;

-- cp8_revision_v57_deferred_evidence_job_delete
CREATE TRIGGER cp8_revision_v57_deferred_evidence_job_delete
 AFTER DELETE ON "v57_deferred_evidence_job" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_deferred_evidence_job' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_deferred_evidence_job_insert
CREATE TRIGGER cp8_revision_v57_deferred_evidence_job_insert
 AFTER INSERT ON "v57_deferred_evidence_job" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_deferred_evidence_job',NEW.rowid);
 END;

-- cp8_revision_v57_deferred_evidence_job_update
CREATE TRIGGER cp8_revision_v57_deferred_evidence_job_update
 AFTER UPDATE ON "v57_deferred_evidence_job" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_deferred_evidence_job' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_deferred_evidence_job',NEW.rowid);
 END;

-- cp8_revision_v57_draw_canonical_r811_delete
CREATE TRIGGER cp8_revision_v57_draw_canonical_r811_delete
 AFTER DELETE ON "v57_draw_canonical_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_draw_canonical_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_draw_canonical_r811_insert
CREATE TRIGGER cp8_revision_v57_draw_canonical_r811_insert
 AFTER INSERT ON "v57_draw_canonical_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_draw_canonical_r811',NEW.rowid);
 END;

-- cp8_revision_v57_draw_canonical_r811_update
CREATE TRIGGER cp8_revision_v57_draw_canonical_r811_update
 AFTER UPDATE ON "v57_draw_canonical_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_draw_canonical_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_draw_canonical_r811',NEW.rowid);
 END;

-- cp8_revision_v57_draw_provenance_r811_delete
CREATE TRIGGER cp8_revision_v57_draw_provenance_r811_delete
 AFTER DELETE ON "v57_draw_provenance_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_draw_provenance_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_draw_provenance_r811_insert
CREATE TRIGGER cp8_revision_v57_draw_provenance_r811_insert
 AFTER INSERT ON "v57_draw_provenance_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_draw_provenance_r811',NEW.rowid);
 END;

-- cp8_revision_v57_draw_provenance_r811_update
CREATE TRIGGER cp8_revision_v57_draw_provenance_r811_update
 AFTER UPDATE ON "v57_draw_provenance_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_draw_provenance_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_draw_provenance_r811',NEW.rowid);
 END;

-- cp8_revision_v57_fetch_ledger_r925_delete
CREATE TRIGGER cp8_revision_v57_fetch_ledger_r925_delete
 AFTER DELETE ON "v57_fetch_ledger_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_fetch_ledger_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_fetch_ledger_r925_insert
CREATE TRIGGER cp8_revision_v57_fetch_ledger_r925_insert
 AFTER INSERT ON "v57_fetch_ledger_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_fetch_ledger_r925',NEW.rowid);
 END;

-- cp8_revision_v57_fetch_ledger_r925_update
CREATE TRIGGER cp8_revision_v57_fetch_ledger_r925_update
 AFTER UPDATE ON "v57_fetch_ledger_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_fetch_ledger_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_fetch_ledger_r925',NEW.rowid);
 END;

-- cp8_revision_v57_hypothesis_r925_delete
CREATE TRIGGER cp8_revision_v57_hypothesis_r925_delete
 AFTER DELETE ON "v57_hypothesis_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_hypothesis_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_hypothesis_r925_insert
CREATE TRIGGER cp8_revision_v57_hypothesis_r925_insert
 AFTER INSERT ON "v57_hypothesis_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_hypothesis_r925',NEW.rowid);
 END;

-- cp8_revision_v57_hypothesis_r925_update
CREATE TRIGGER cp8_revision_v57_hypothesis_r925_update
 AFTER UPDATE ON "v57_hypothesis_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_hypothesis_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_hypothesis_r925',NEW.rowid);
 END;

-- cp8_revision_v57_issue_closure_r906_delete
CREATE TRIGGER cp8_revision_v57_issue_closure_r906_delete
 AFTER DELETE ON "v57_issue_closure_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_issue_closure_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_issue_closure_r906_insert
CREATE TRIGGER cp8_revision_v57_issue_closure_r906_insert
 AFTER INSERT ON "v57_issue_closure_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_issue_closure_r906',NEW.rowid);
 END;

-- cp8_revision_v57_issue_closure_r906_update
CREATE TRIGGER cp8_revision_v57_issue_closure_r906_update
 AFTER UPDATE ON "v57_issue_closure_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_issue_closure_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_issue_closure_r906',NEW.rowid);
 END;

-- cp8_revision_v57_issue_projection_r926_delete
CREATE TRIGGER cp8_revision_v57_issue_projection_r926_delete
 AFTER DELETE ON "v57_issue_projection_r926" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_issue_projection_r926' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_issue_projection_r926_insert
CREATE TRIGGER cp8_revision_v57_issue_projection_r926_insert
 AFTER INSERT ON "v57_issue_projection_r926" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_issue_projection_r926',NEW.rowid);
 END;

-- cp8_revision_v57_issue_projection_r926_update
CREATE TRIGGER cp8_revision_v57_issue_projection_r926_update
 AFTER UPDATE ON "v57_issue_projection_r926" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_issue_projection_r926' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_issue_projection_r926',NEW.rowid);
 END;

-- cp8_revision_v57_k3_action_space_1200_delete
CREATE TRIGGER cp8_revision_v57_k3_action_space_1200_delete
 AFTER DELETE ON "v57_k3_action_space_1200" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_k3_action_space_1200' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_k3_action_space_1200_insert
CREATE TRIGGER cp8_revision_v57_k3_action_space_1200_insert
 AFTER INSERT ON "v57_k3_action_space_1200" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_k3_action_space_1200',NEW.rowid);
 END;

-- cp8_revision_v57_k3_action_space_1200_update
CREATE TRIGGER cp8_revision_v57_k3_action_space_1200_update
 AFTER UPDATE ON "v57_k3_action_space_1200" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_k3_action_space_1200' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_k3_action_space_1200',NEW.rowid);
 END;

-- cp8_revision_v57_k3_champion_freeze_delete
CREATE TRIGGER cp8_revision_v57_k3_champion_freeze_delete
 AFTER DELETE ON "v57_k3_champion_freeze" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_k3_champion_freeze' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_k3_champion_freeze_insert
CREATE TRIGGER cp8_revision_v57_k3_champion_freeze_insert
 AFTER INSERT ON "v57_k3_champion_freeze" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_k3_champion_freeze',NEW.rowid);
 END;

-- cp8_revision_v57_k3_champion_freeze_update
CREATE TRIGGER cp8_revision_v57_k3_champion_freeze_update
 AFTER UPDATE ON "v57_k3_champion_freeze" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_k3_champion_freeze' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_k3_champion_freeze',NEW.rowid);
 END;

-- cp8_revision_v57_k3_funnel_trace_delete
CREATE TRIGGER cp8_revision_v57_k3_funnel_trace_delete
 AFTER DELETE ON "v57_k3_funnel_trace" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_k3_funnel_trace' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_k3_funnel_trace_insert
CREATE TRIGGER cp8_revision_v57_k3_funnel_trace_insert
 AFTER INSERT ON "v57_k3_funnel_trace" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_k3_funnel_trace',NEW.rowid);
 END;

-- cp8_revision_v57_k3_funnel_trace_update
CREATE TRIGGER cp8_revision_v57_k3_funnel_trace_update
 AFTER UPDATE ON "v57_k3_funnel_trace" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_k3_funnel_trace' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_k3_funnel_trace',NEW.rowid);
 END;

-- cp8_revision_v57_legacy_postsettlement_context_r919_delete
CREATE TRIGGER cp8_revision_v57_legacy_postsettlement_context_r919_delete
 AFTER DELETE ON "v57_legacy_postsettlement_context_r919" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_legacy_postsettlement_context_r919' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_legacy_postsettlement_context_r919_insert
CREATE TRIGGER cp8_revision_v57_legacy_postsettlement_context_r919_insert
 AFTER INSERT ON "v57_legacy_postsettlement_context_r919" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_legacy_postsettlement_context_r919',NEW.rowid);
 END;

-- cp8_revision_v57_legacy_postsettlement_context_r919_update
CREATE TRIGGER cp8_revision_v57_legacy_postsettlement_context_r919_update
 AFTER UPDATE ON "v57_legacy_postsettlement_context_r919" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_legacy_postsettlement_context_r919' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_legacy_postsettlement_context_r919',NEW.rowid);
 END;

-- cp8_revision_v57_legacy_predraw_context_r915_delete
CREATE TRIGGER cp8_revision_v57_legacy_predraw_context_r915_delete
 AFTER DELETE ON "v57_legacy_predraw_context_r915" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_legacy_predraw_context_r915' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_legacy_predraw_context_r915_insert
CREATE TRIGGER cp8_revision_v57_legacy_predraw_context_r915_insert
 AFTER INSERT ON "v57_legacy_predraw_context_r915" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_legacy_predraw_context_r915',NEW.rowid);
 END;

-- cp8_revision_v57_legacy_predraw_context_r915_update
CREATE TRIGGER cp8_revision_v57_legacy_predraw_context_r915_update
 AFTER UPDATE ON "v57_legacy_predraw_context_r915" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_legacy_predraw_context_r915' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_legacy_predraw_context_r915',NEW.rowid);
 END;

-- cp8_revision_v57_lifecycle_audit_r925_delete
CREATE TRIGGER cp8_revision_v57_lifecycle_audit_r925_delete
 AFTER DELETE ON "v57_lifecycle_audit_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_lifecycle_audit_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_lifecycle_audit_r925_insert
CREATE TRIGGER cp8_revision_v57_lifecycle_audit_r925_insert
 AFTER INSERT ON "v57_lifecycle_audit_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_lifecycle_audit_r925',NEW.rowid);
 END;

-- cp8_revision_v57_lifecycle_audit_r925_update
CREATE TRIGGER cp8_revision_v57_lifecycle_audit_r925_update
 AFTER UPDATE ON "v57_lifecycle_audit_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_lifecycle_audit_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_lifecycle_audit_r925',NEW.rowid);
 END;

-- cp8_revision_v57_long_text_r815_delete
CREATE TRIGGER cp8_revision_v57_long_text_r815_delete
 AFTER DELETE ON "v57_long_text_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_long_text_r815' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_long_text_r815_insert
CREATE TRIGGER cp8_revision_v57_long_text_r815_insert
 AFTER INSERT ON "v57_long_text_r815" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_long_text_r815',NEW.rowid);
 END;

-- cp8_revision_v57_long_text_r815_update
CREATE TRIGGER cp8_revision_v57_long_text_r815_update
 AFTER UPDATE ON "v57_long_text_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_long_text_r815' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_long_text_r815',NEW.rowid);
 END;

-- cp8_revision_v57_oos_label_r35_delete
CREATE TRIGGER cp8_revision_v57_oos_label_r35_delete
 AFTER DELETE ON "v57_oos_label_r35" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_oos_label_r35' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_oos_label_r35_insert
CREATE TRIGGER cp8_revision_v57_oos_label_r35_insert
 AFTER INSERT ON "v57_oos_label_r35" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_oos_label_r35',NEW.rowid);
 END;

-- cp8_revision_v57_oos_label_r35_update
CREATE TRIGGER cp8_revision_v57_oos_label_r35_update
 AFTER UPDATE ON "v57_oos_label_r35" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_oos_label_r35' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_oos_label_r35',NEW.rowid);
 END;

-- cp8_revision_v57_prediction_timing_r925_delete
CREATE TRIGGER cp8_revision_v57_prediction_timing_r925_delete
 AFTER DELETE ON "v57_prediction_timing_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_prediction_timing_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_prediction_timing_r925_insert
CREATE TRIGGER cp8_revision_v57_prediction_timing_r925_insert
 AFTER INSERT ON "v57_prediction_timing_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_prediction_timing_r925',NEW.rowid);
 END;

-- cp8_revision_v57_prediction_timing_r925_update
CREATE TRIGGER cp8_revision_v57_prediction_timing_r925_update
 AFTER UPDATE ON "v57_prediction_timing_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_prediction_timing_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_prediction_timing_r925',NEW.rowid);
 END;

-- cp8_revision_v57_promotion_history_r925_delete
CREATE TRIGGER cp8_revision_v57_promotion_history_r925_delete
 AFTER DELETE ON "v57_promotion_history_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_promotion_history_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_promotion_history_r925_insert
CREATE TRIGGER cp8_revision_v57_promotion_history_r925_insert
 AFTER INSERT ON "v57_promotion_history_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_promotion_history_r925',NEW.rowid);
 END;

-- cp8_revision_v57_promotion_history_r925_update
CREATE TRIGGER cp8_revision_v57_promotion_history_r925_update
 AFTER UPDATE ON "v57_promotion_history_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_promotion_history_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_promotion_history_r925',NEW.rowid);
 END;

-- cp8_revision_v57_promotion_window_r925_delete
CREATE TRIGGER cp8_revision_v57_promotion_window_r925_delete
 AFTER DELETE ON "v57_promotion_window_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_promotion_window_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_promotion_window_r925_insert
CREATE TRIGGER cp8_revision_v57_promotion_window_r925_insert
 AFTER INSERT ON "v57_promotion_window_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_promotion_window_r925',NEW.rowid);
 END;

-- cp8_revision_v57_promotion_window_r925_update
CREATE TRIGGER cp8_revision_v57_promotion_window_r925_update
 AFTER UPDATE ON "v57_promotion_window_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_promotion_window_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_promotion_window_r925',NEW.rowid);
 END;

-- cp8_revision_v57_recovery_generation_r815_delete
CREATE TRIGGER cp8_revision_v57_recovery_generation_r815_delete
 AFTER DELETE ON "v57_recovery_generation_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_recovery_generation_r815' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_recovery_generation_r815_insert
CREATE TRIGGER cp8_revision_v57_recovery_generation_r815_insert
 AFTER INSERT ON "v57_recovery_generation_r815" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_recovery_generation_r815',NEW.rowid);
 END;

-- cp8_revision_v57_recovery_generation_r815_update
CREATE TRIGGER cp8_revision_v57_recovery_generation_r815_update
 AFTER UPDATE ON "v57_recovery_generation_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_recovery_generation_r815' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_recovery_generation_r815',NEW.rowid);
 END;

-- cp8_revision_v57_research_action_label_r811_delete
CREATE TRIGGER cp8_revision_v57_research_action_label_r811_delete
 AFTER DELETE ON "v57_research_action_label_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_action_label_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_action_label_r811_insert
CREATE TRIGGER cp8_revision_v57_research_action_label_r811_insert
 AFTER INSERT ON "v57_research_action_label_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_action_label_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_action_label_r811_update
CREATE TRIGGER cp8_revision_v57_research_action_label_r811_update
 AFTER UPDATE ON "v57_research_action_label_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_action_label_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_action_label_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_artifact_job_r906_delete
CREATE TRIGGER cp8_revision_v57_research_artifact_job_r906_delete
 AFTER DELETE ON "v57_research_artifact_job_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_artifact_job_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_artifact_job_r906_insert
CREATE TRIGGER cp8_revision_v57_research_artifact_job_r906_insert
 AFTER INSERT ON "v57_research_artifact_job_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_artifact_job_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_artifact_job_r906_update
CREATE TRIGGER cp8_revision_v57_research_artifact_job_r906_update
 AFTER UPDATE ON "v57_research_artifact_job_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_artifact_job_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_artifact_job_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_artifact_r811_delete
CREATE TRIGGER cp8_revision_v57_research_artifact_r811_delete
 AFTER DELETE ON "v57_research_artifact_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_artifact_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_artifact_r811_insert
CREATE TRIGGER cp8_revision_v57_research_artifact_r811_insert
 AFTER INSERT ON "v57_research_artifact_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_artifact_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_artifact_r811_update
CREATE TRIGGER cp8_revision_v57_research_artifact_r811_update
 AFTER UPDATE ON "v57_research_artifact_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_artifact_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_artifact_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_artifact_r815_delete
CREATE TRIGGER cp8_revision_v57_research_artifact_r815_delete
 AFTER DELETE ON "v57_research_artifact_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_artifact_r815' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_artifact_r815_insert
CREATE TRIGGER cp8_revision_v57_research_artifact_r815_insert
 AFTER INSERT ON "v57_research_artifact_r815" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_artifact_r815',NEW.rowid);
 END;

-- cp8_revision_v57_research_artifact_r815_update
CREATE TRIGGER cp8_revision_v57_research_artifact_r815_update
 AFTER UPDATE ON "v57_research_artifact_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_artifact_r815' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_artifact_r815',NEW.rowid);
 END;

-- cp8_revision_v57_research_asset_r906_delete
CREATE TRIGGER cp8_revision_v57_research_asset_r906_delete
 AFTER DELETE ON "v57_research_asset_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_asset_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_asset_r906_insert
CREATE TRIGGER cp8_revision_v57_research_asset_r906_insert
 AFTER INSERT ON "v57_research_asset_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_asset_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_asset_r906_update
CREATE TRIGGER cp8_revision_v57_research_asset_r906_update
 AFTER UPDATE ON "v57_research_asset_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_asset_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_asset_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_candidate_outcome_r811_delete
CREATE TRIGGER cp8_revision_v57_research_candidate_outcome_r811_delete
 AFTER DELETE ON "v57_research_candidate_outcome_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_candidate_outcome_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_candidate_outcome_r811_insert
CREATE TRIGGER cp8_revision_v57_research_candidate_outcome_r811_insert
 AFTER INSERT ON "v57_research_candidate_outcome_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_candidate_outcome_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_candidate_outcome_r811_update
CREATE TRIGGER cp8_revision_v57_research_candidate_outcome_r811_update
 AFTER UPDATE ON "v57_research_candidate_outcome_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_candidate_outcome_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_candidate_outcome_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_candidate_score_r900_delete
CREATE TRIGGER cp8_revision_v57_research_candidate_score_r900_delete
 AFTER DELETE ON "v57_research_candidate_score_r900" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_candidate_score_r900' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_candidate_score_r900_insert
CREATE TRIGGER cp8_revision_v57_research_candidate_score_r900_insert
 AFTER INSERT ON "v57_research_candidate_score_r900" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_candidate_score_r900',NEW.rowid);
 END;

-- cp8_revision_v57_research_candidate_score_r900_update
CREATE TRIGGER cp8_revision_v57_research_candidate_score_r900_update
 AFTER UPDATE ON "v57_research_candidate_score_r900" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_candidate_score_r900' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_candidate_score_r900',NEW.rowid);
 END;

-- cp8_revision_v57_research_codegen_r811_delete
CREATE TRIGGER cp8_revision_v57_research_codegen_r811_delete
 AFTER DELETE ON "v57_research_codegen_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_codegen_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_codegen_r811_insert
CREATE TRIGGER cp8_revision_v57_research_codegen_r811_insert
 AFTER INSERT ON "v57_research_codegen_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_codegen_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_codegen_r811_update
CREATE TRIGGER cp8_revision_v57_research_codegen_r811_update
 AFTER UPDATE ON "v57_research_codegen_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_codegen_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_codegen_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_compact_label_r906_delete
CREATE TRIGGER cp8_revision_v57_research_compact_label_r906_delete
 AFTER DELETE ON "v57_research_compact_label_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_compact_label_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_compact_label_r906_insert
CREATE TRIGGER cp8_revision_v57_research_compact_label_r906_insert
 AFTER INSERT ON "v57_research_compact_label_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_compact_label_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_compact_label_r906_update
CREATE TRIGGER cp8_revision_v57_research_compact_label_r906_update
 AFTER UPDATE ON "v57_research_compact_label_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_compact_label_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_compact_label_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_eval_coverage_r906_delete
CREATE TRIGGER cp8_revision_v57_research_eval_coverage_r906_delete
 AFTER DELETE ON "v57_research_eval_coverage_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_eval_coverage_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_eval_coverage_r906_insert
CREATE TRIGGER cp8_revision_v57_research_eval_coverage_r906_insert
 AFTER INSERT ON "v57_research_eval_coverage_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_eval_coverage_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_eval_coverage_r906_update
CREATE TRIGGER cp8_revision_v57_research_eval_coverage_r906_update
 AFTER UPDATE ON "v57_research_eval_coverage_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_eval_coverage_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_eval_coverage_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_eval_r815_delete
CREATE TRIGGER cp8_revision_v57_research_eval_r815_delete
 AFTER DELETE ON "v57_research_eval_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_eval_r815' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_eval_r815_insert
CREATE TRIGGER cp8_revision_v57_research_eval_r815_insert
 AFTER INSERT ON "v57_research_eval_r815" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_eval_r815',NEW.rowid);
 END;

-- cp8_revision_v57_research_eval_r815_update
CREATE TRIGGER cp8_revision_v57_research_eval_r815_update
 AFTER UPDATE ON "v57_research_eval_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_eval_r815' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_eval_r815',NEW.rowid);
 END;

-- cp8_revision_v57_research_experiment_r811_delete
CREATE TRIGGER cp8_revision_v57_research_experiment_r811_delete
 AFTER DELETE ON "v57_research_experiment_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_experiment_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_experiment_r811_insert
CREATE TRIGGER cp8_revision_v57_research_experiment_r811_insert
 AFTER INSERT ON "v57_research_experiment_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_experiment_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_experiment_r811_update
CREATE TRIGGER cp8_revision_v57_research_experiment_r811_update
 AFTER UPDATE ON "v57_research_experiment_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_experiment_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_experiment_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_expert_outcome_r811_delete
CREATE TRIGGER cp8_revision_v57_research_expert_outcome_r811_delete
 AFTER DELETE ON "v57_research_expert_outcome_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_expert_outcome_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_expert_outcome_r811_insert
CREATE TRIGGER cp8_revision_v57_research_expert_outcome_r811_insert
 AFTER INSERT ON "v57_research_expert_outcome_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_expert_outcome_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_expert_outcome_r811_update
CREATE TRIGGER cp8_revision_v57_research_expert_outcome_r811_update
 AFTER UPDATE ON "v57_research_expert_outcome_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_expert_outcome_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_expert_outcome_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_expert_predraw_r811_delete
CREATE TRIGGER cp8_revision_v57_research_expert_predraw_r811_delete
 AFTER DELETE ON "v57_research_expert_predraw_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_expert_predraw_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_expert_predraw_r811_insert
CREATE TRIGGER cp8_revision_v57_research_expert_predraw_r811_insert
 AFTER INSERT ON "v57_research_expert_predraw_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_expert_predraw_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_expert_predraw_r811_update
CREATE TRIGGER cp8_revision_v57_research_expert_predraw_r811_update
 AFTER UPDATE ON "v57_research_expert_predraw_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_expert_predraw_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_expert_predraw_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_hypothesis_r811_delete
CREATE TRIGGER cp8_revision_v57_research_hypothesis_r811_delete
 AFTER DELETE ON "v57_research_hypothesis_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_hypothesis_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_hypothesis_r811_insert
CREATE TRIGGER cp8_revision_v57_research_hypothesis_r811_insert
 AFTER INSERT ON "v57_research_hypothesis_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_hypothesis_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_hypothesis_r811_update
CREATE TRIGGER cp8_revision_v57_research_hypothesis_r811_update
 AFTER UPDATE ON "v57_research_hypothesis_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_hypothesis_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_hypothesis_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_input_pin_r967_delete
CREATE TRIGGER cp8_revision_v57_research_input_pin_r967_delete
 AFTER DELETE ON "v57_research_input_pin_r967" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_input_pin_r967' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_input_pin_r967_insert
CREATE TRIGGER cp8_revision_v57_research_input_pin_r967_insert
 AFTER INSERT ON "v57_research_input_pin_r967" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_input_pin_r967',NEW.rowid);
 END;

-- cp8_revision_v57_research_input_pin_r967_update
CREATE TRIGGER cp8_revision_v57_research_input_pin_r967_update
 AFTER UPDATE ON "v57_research_input_pin_r967" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_input_pin_r967' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_input_pin_r967',NEW.rowid);
 END;

-- cp8_revision_v57_research_learner_update_r906_delete
CREATE TRIGGER cp8_revision_v57_research_learner_update_r906_delete
 AFTER DELETE ON "v57_research_learner_update_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_learner_update_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_learner_update_r906_insert
CREATE TRIGGER cp8_revision_v57_research_learner_update_r906_insert
 AFTER INSERT ON "v57_research_learner_update_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_learner_update_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_learner_update_r906_update
CREATE TRIGGER cp8_revision_v57_research_learner_update_r906_update
 AFTER UPDATE ON "v57_research_learner_update_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_learner_update_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_learner_update_r906',NEW.rowid);
 END;

-- cp8_revision_v57_research_mainline_contribution_r922_delete
CREATE TRIGGER cp8_revision_v57_research_mainline_contribution_r922_delete
 AFTER DELETE ON "v57_research_mainline_contribution_r922" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_mainline_contribution_r922' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_mainline_contribution_r922_insert
CREATE TRIGGER cp8_revision_v57_research_mainline_contribution_r922_insert
 AFTER INSERT ON "v57_research_mainline_contribution_r922" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_mainline_contribution_r922',NEW.rowid);
 END;

-- cp8_revision_v57_research_mainline_contribution_r922_update
CREATE TRIGGER cp8_revision_v57_research_mainline_contribution_r922_update
 AFTER UPDATE ON "v57_research_mainline_contribution_r922" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_mainline_contribution_r922' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_mainline_contribution_r922',NEW.rowid);
 END;

-- cp8_revision_v57_research_predraw_r817_delete
CREATE TRIGGER cp8_revision_v57_research_predraw_r817_delete
 AFTER DELETE ON "v57_research_predraw_r817" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_predraw_r817' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_predraw_r817_insert
CREATE TRIGGER cp8_revision_v57_research_predraw_r817_insert
 AFTER INSERT ON "v57_research_predraw_r817" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_predraw_r817',NEW.rowid);
 END;

-- cp8_revision_v57_research_predraw_r817_update
CREATE TRIGGER cp8_revision_v57_research_predraw_r817_update
 AFTER UPDATE ON "v57_research_predraw_r817" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_predraw_r817' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_predraw_r817',NEW.rowid);
 END;

-- cp8_revision_v57_research_prune_log_r900_delete
CREATE TRIGGER cp8_revision_v57_research_prune_log_r900_delete
 AFTER DELETE ON "v57_research_prune_log_r900" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_prune_log_r900' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_prune_log_r900_insert
CREATE TRIGGER cp8_revision_v57_research_prune_log_r900_insert
 AFTER INSERT ON "v57_research_prune_log_r900" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_prune_log_r900',NEW.rowid);
 END;

-- cp8_revision_v57_research_prune_log_r900_update
CREATE TRIGGER cp8_revision_v57_research_prune_log_r900_update
 AFTER UPDATE ON "v57_research_prune_log_r900" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_prune_log_r900' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_prune_log_r900',NEW.rowid);
 END;

-- cp8_revision_v57_research_queue_r815_delete
CREATE TRIGGER cp8_revision_v57_research_queue_r815_delete
 AFTER DELETE ON "v57_research_queue_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_queue_r815' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_queue_r815_insert
CREATE TRIGGER cp8_revision_v57_research_queue_r815_insert
 AFTER INSERT ON "v57_research_queue_r815" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_queue_r815',NEW.rowid);
 END;

-- cp8_revision_v57_research_queue_r815_update
CREATE TRIGGER cp8_revision_v57_research_queue_r815_update
 AFTER UPDATE ON "v57_research_queue_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_queue_r815' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_queue_r815',NEW.rowid);
 END;

-- cp8_revision_v57_research_run_r811_delete
CREATE TRIGGER cp8_revision_v57_research_run_r811_delete
 AFTER DELETE ON "v57_research_run_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_run_r811' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_run_r811_insert
CREATE TRIGGER cp8_revision_v57_research_run_r811_insert
 AFTER INSERT ON "v57_research_run_r811" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_run_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_run_r811_update
CREATE TRIGGER cp8_revision_v57_research_run_r811_update
 AFTER UPDATE ON "v57_research_run_r811" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_run_r811' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_run_r811',NEW.rowid);
 END;

-- cp8_revision_v57_research_task_r967_delete
CREATE TRIGGER cp8_revision_v57_research_task_r967_delete
 AFTER DELETE ON "v57_research_task_r967" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_task_r967' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_research_task_r967_insert
CREATE TRIGGER cp8_revision_v57_research_task_r967_insert
 AFTER INSERT ON "v57_research_task_r967" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_task_r967',NEW.rowid);
 END;

-- cp8_revision_v57_research_task_r967_update
CREATE TRIGGER cp8_revision_v57_research_task_r967_update
 AFTER UPDATE ON "v57_research_task_r967" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_research_task_r967' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_research_task_r967',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_epoch_r925_delete
CREATE TRIGGER cp8_revision_v57_runtime_epoch_r925_delete
 AFTER DELETE ON "v57_runtime_epoch_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_epoch_r925' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_runtime_epoch_r925_insert
CREATE TRIGGER cp8_revision_v57_runtime_epoch_r925_insert
 AFTER INSERT ON "v57_runtime_epoch_r925" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_epoch_r925',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_epoch_r925_update
CREATE TRIGGER cp8_revision_v57_runtime_epoch_r925_update
 AFTER UPDATE ON "v57_runtime_epoch_r925" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_epoch_r925' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_epoch_r925',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_event_outbox_r926_delete
CREATE TRIGGER cp8_revision_v57_runtime_event_outbox_r926_delete
 AFTER DELETE ON "v57_runtime_event_outbox_r926" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_event_outbox_r926' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_runtime_event_outbox_r926_insert
CREATE TRIGGER cp8_revision_v57_runtime_event_outbox_r926_insert
 AFTER INSERT ON "v57_runtime_event_outbox_r926" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_event_outbox_r926',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_event_outbox_r926_update
CREATE TRIGGER cp8_revision_v57_runtime_event_outbox_r926_update
 AFTER UPDATE ON "v57_runtime_event_outbox_r926" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_event_outbox_r926' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_event_outbox_r926',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_event_r926_delete
CREATE TRIGGER cp8_revision_v57_runtime_event_r926_delete
 AFTER DELETE ON "v57_runtime_event_r926" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_event_r926' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_runtime_event_r926_insert
CREATE TRIGGER cp8_revision_v57_runtime_event_r926_insert
 AFTER INSERT ON "v57_runtime_event_r926" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_event_r926',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_event_r926_update
CREATE TRIGGER cp8_revision_v57_runtime_event_r926_update
 AFTER UPDATE ON "v57_runtime_event_r926" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_event_r926' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_event_r926',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_health_r35_delete
CREATE TRIGGER cp8_revision_v57_runtime_health_r35_delete
 AFTER DELETE ON "v57_runtime_health_r35" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_health_r35' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_runtime_health_r35_insert
CREATE TRIGGER cp8_revision_v57_runtime_health_r35_insert
 AFTER INSERT ON "v57_runtime_health_r35" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_health_r35',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_health_r35_update
CREATE TRIGGER cp8_revision_v57_runtime_health_r35_update
 AFTER UPDATE ON "v57_runtime_health_r35" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_health_r35' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_health_r35',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_truth_snapshot_r906_delete
CREATE TRIGGER cp8_revision_v57_runtime_truth_snapshot_r906_delete
 AFTER DELETE ON "v57_runtime_truth_snapshot_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_truth_snapshot_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_runtime_truth_snapshot_r906_insert
CREATE TRIGGER cp8_revision_v57_runtime_truth_snapshot_r906_insert
 AFTER INSERT ON "v57_runtime_truth_snapshot_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_truth_snapshot_r906',NEW.rowid);
 END;

-- cp8_revision_v57_runtime_truth_snapshot_r906_update
CREATE TRIGGER cp8_revision_v57_runtime_truth_snapshot_r906_update
 AFTER UPDATE ON "v57_runtime_truth_snapshot_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_runtime_truth_snapshot_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_runtime_truth_snapshot_r906',NEW.rowid);
 END;

-- cp8_revision_v57_selected_evidence_r906_delete
CREATE TRIGGER cp8_revision_v57_selected_evidence_r906_delete
 AFTER DELETE ON "v57_selected_evidence_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_selected_evidence_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_selected_evidence_r906_insert
CREATE TRIGGER cp8_revision_v57_selected_evidence_r906_insert
 AFTER INSERT ON "v57_selected_evidence_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_selected_evidence_r906',NEW.rowid);
 END;

-- cp8_revision_v57_selected_evidence_r906_update
CREATE TRIGGER cp8_revision_v57_selected_evidence_r906_update
 AFTER UPDATE ON "v57_selected_evidence_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_selected_evidence_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_selected_evidence_r906',NEW.rowid);
 END;

-- cp8_revision_v57_settlement_outbox_r906_delete
CREATE TRIGGER cp8_revision_v57_settlement_outbox_r906_delete
 AFTER DELETE ON "v57_settlement_outbox_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_settlement_outbox_r906' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_settlement_outbox_r906_insert
CREATE TRIGGER cp8_revision_v57_settlement_outbox_r906_insert
 AFTER INSERT ON "v57_settlement_outbox_r906" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_settlement_outbox_r906',NEW.rowid);
 END;

-- cp8_revision_v57_settlement_outbox_r906_update
CREATE TRIGGER cp8_revision_v57_settlement_outbox_r906_update
 AFTER UPDATE ON "v57_settlement_outbox_r906" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_settlement_outbox_r906' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_settlement_outbox_r906',NEW.rowid);
 END;

-- cp8_revision_v57_settlement_task_r36_delete
CREATE TRIGGER cp8_revision_v57_settlement_task_r36_delete
 AFTER DELETE ON "v57_settlement_task_r36" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_settlement_task_r36' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_settlement_task_r36_insert
CREATE TRIGGER cp8_revision_v57_settlement_task_r36_insert
 AFTER INSERT ON "v57_settlement_task_r36" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_settlement_task_r36',NEW.rowid);
 END;

-- cp8_revision_v57_settlement_task_r36_update
CREATE TRIGGER cp8_revision_v57_settlement_task_r36_update
 AFTER UPDATE ON "v57_settlement_task_r36" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_settlement_task_r36' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_settlement_task_r36',NEW.rowid);
 END;

-- cp8_revision_v57_shadow_observation_r35_delete
CREATE TRIGGER cp8_revision_v57_shadow_observation_r35_delete
 AFTER DELETE ON "v57_shadow_observation_r35" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_shadow_observation_r35' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_shadow_observation_r35_insert
CREATE TRIGGER cp8_revision_v57_shadow_observation_r35_insert
 AFTER INSERT ON "v57_shadow_observation_r35" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_shadow_observation_r35',NEW.rowid);
 END;

-- cp8_revision_v57_shadow_observation_r35_update
CREATE TRIGGER cp8_revision_v57_shadow_observation_r35_update
 AFTER UPDATE ON "v57_shadow_observation_r35" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_shadow_observation_r35' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_shadow_observation_r35',NEW.rowid);
 END;

-- cp8_revision_v57_shadow_sidecar_r815_delete
CREATE TRIGGER cp8_revision_v57_shadow_sidecar_r815_delete
 AFTER DELETE ON "v57_shadow_sidecar_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_shadow_sidecar_r815' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_v57_shadow_sidecar_r815_insert
CREATE TRIGGER cp8_revision_v57_shadow_sidecar_r815_insert
 AFTER INSERT ON "v57_shadow_sidecar_r815" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_shadow_sidecar_r815',NEW.rowid);
 END;

-- cp8_revision_v57_shadow_sidecar_r815_update
CREATE TRIGGER cp8_revision_v57_shadow_sidecar_r815_update
 AFTER UPDATE ON "v57_shadow_sidecar_r815" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='v57_shadow_sidecar_r815' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('v57_shadow_sidecar_r815',NEW.rowid);
 END;

-- cp8_revision_window_feature_sample_delete
CREATE TRIGGER cp8_revision_window_feature_sample_delete
 AFTER DELETE ON "window_feature_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='window_feature_sample' AND source_rowid=OLD.rowid;
 END;

-- cp8_revision_window_feature_sample_insert
CREATE TRIGGER cp8_revision_window_feature_sample_insert
 AFTER INSERT ON "window_feature_sample" BEGIN
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('window_feature_sample',NEW.rowid);
 END;

-- cp8_revision_window_feature_sample_update
CREATE TRIGGER cp8_revision_window_feature_sample_update
 AFTER UPDATE ON "window_feature_sample" BEGIN
 DELETE FROM research_row_revision_v2 WHERE source_table='window_feature_sample' AND source_rowid=OLD.rowid;
 INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('window_feature_sample',NEW.rowid);
 END;

-- decision_audit
CREATE TABLE decision_audit (_id INTEGER PRIMARY KEY AUTOINCREMENT, decision_audit_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, candidate_key TEXT, chain_key TEXT, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, grade TEXT, action TEXT, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, final_cause_score REAL DEFAULT 0, decision_pass INTEGER DEFAULT 0, block_code TEXT, block_reason TEXT, decision_reason TEXT, settled INTEGER DEFAULT 0, result TEXT DEFAULT 'WAIT', hit INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, pnl REAL DEFAULT 0, dynamic_cluster_id TEXT, dynamic_cluster_status TEXT, dynamic_cluster_prototype_key TEXT, dynamic_cluster_feature_key TEXT, dynamic_cluster_sample INTEGER DEFAULT 0, dynamic_cluster_hit_count INTEGER DEFAULT 0, dynamic_cluster_miss_count INTEGER DEFAULT 0, dynamic_cluster_recent_sample INTEGER DEFAULT 0, dynamic_cluster_recent_hit_count INTEGER DEFAULT 0, dynamic_cluster_hit_rate REAL DEFAULT 0, dynamic_cluster_recent_hit_rate REAL DEFAULT 0, dynamic_cluster_confidence REAL DEFAULT 0, dynamic_cluster_distance REAL DEFAULT 0, dynamic_cluster_volatility REAL DEFAULT 0, dynamic_cluster_max_lose INTEGER DEFAULT 0, dynamic_cluster_counter_score REAL DEFAULT 0, dynamic_cluster_version TEXT, dynamic_cluster_reason TEXT, position_need_key TEXT, position_need_summary TEXT, position_need_strength REAL DEFAULT 0, position_need_chaos_risk REAL DEFAULT 0, position_need_repair_score REAL DEFAULT 0, position_need_relay_score REAL DEFAULT 0, number_supply_key TEXT, number_supply_summary TEXT, number_supply_strength REAL DEFAULT 0, number_supply_risk REAL DEFAULT 0, need_supply_match_key TEXT, need_supply_match_score REAL DEFAULT 0, need_supply_third_quality REAL DEFAULT 0, need_supply_role_diversity REAL DEFAULT 0, need_supply_risk REAL DEFAULT 0, need_supply_summary TEXT, k3_role_main TEXT, k3_role_assist TEXT, k3_role_defense TEXT, k3_role_main_number INTEGER DEFAULT 0, k3_role_assist_number INTEGER DEFAULT 0, k3_role_defense_number INTEGER DEFAULT 0, k3_role_structure TEXT, counterfactual_reason_key TEXT, counterfactual_rank INTEGER DEFAULT 0, counterfactual_rank_score REAL DEFAULT 0, counterfactual_why_not_risk REAL DEFAULT 0, counterfactual_why_not TEXT, counterfactual_summary TEXT, cause_ablation_key TEXT, cause_ablation_score REAL DEFAULT 0, cause_ablation_false_cause_count INTEGER DEFAULT 0, cause_ablation_summary TEXT, miss_reason_type TEXT, miss_reason_matrix_key TEXT, miss_reason_detail TEXT, adaptive_judge_candidate_count INTEGER DEFAULT 0, adaptive_judge_status TEXT, adaptive_judge_action TEXT, adaptive_judge_domain TEXT, adaptive_cluster_hit_rate REAL DEFAULT 0, adaptive_cluster_recent_hit_rate REAL DEFAULT 0, adaptive_cluster_sample INTEGER DEFAULT 0, adaptive_cluster_confidence REAL DEFAULT 0, adaptive_cause_score REAL DEFAULT 0, adaptive_counter_score REAL DEFAULT 0, adaptive_promote_score REAL DEFAULT 0, adaptive_freeze_score REAL DEFAULT 0, adaptive_judge_reason TEXT, adaptive_judge_version TEXT, promotion_cluster_status TEXT, promotion_cluster_hit_rate REAL DEFAULT 0, promotion_cluster_recent_hit_rate REAL DEFAULT 0, promotion_cluster_sample INTEGER DEFAULT 0, promotion_cluster_reason TEXT, why_bet TEXT, why_no_bet TEXT, why_hit TEXT, why_miss TEXT, dynamic_causal_summary TEXT, snapshot_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- decision_authorization_v28
CREATE TABLE decision_authorization_v28 (_id INTEGER PRIMARY KEY AUTOINCREMENT, contract_decision_id TEXT NOT NULL UNIQUE, authorization_id TEXT, build_id TEXT, cohort_id TEXT, schema_version TEXT, pipeline_version TEXT, calibration_version TEXT, authorized_mode TEXT, authorized_at INTEGER DEFAULT 0, valid INTEGER DEFAULT 0, calibrated_probability REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, family_lcb REAL DEFAULT 0, coverage REAL DEFAULT 0, third_quality REAL DEFAULT 0, action_utility REAL DEFAULT 0, uncertainty_penalty REAL DEFAULT 0, structure_score REAL DEFAULT 0, lane_candidate TEXT, teacher_policy TEXT, dynamic_policy TEXT, adaptive_policy TEXT, reject_code TEXT, version TEXT);

-- decisions
CREATE TABLE decisions (_id INTEGER PRIMARY KEY AUTOINCREMENT, target_issue TEXT NOT NULL UNIQUE, base_issue TEXT, created_at INTEGER DEFAULT 0, decision TEXT DEFAULT 'HOLD', action TEXT, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, raw_p REAL DEFAULT 0, calibrated_p REAL DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, model_p REAL DEFAULT 0, shape_p REAL DEFAULT 0, combo_p REAL DEFAULT 0, recovery_p REAL DEFAULT 0, similar_p REAL DEFAULT 0, memory_p REAL DEFAULT 0, similar_hit_rate REAL DEFAULT 0, combo_hit_rate REAL DEFAULT 0, effective_sample INTEGER DEFAULT 0, combo_sample INTEGER DEFAULT 0, trend_life REAL DEFAULT 0, drift_risk REAL DEFAULT 0, break_depth REAL DEFAULT 0, recovery_score REAL DEFAULT 0, champion_gap REAL DEFAULT 0, combo_gap REAL DEFAULT 0, hot_reverse_risk REAL DEFAULT 0, false_strong_risk REAL DEFAULT 0, freq100 REAL DEFAULT 0, freq500 REAL DEFAULT 0, freq1000 REAL DEFAULT 0, calibration_error REAL DEFAULT 0, probability_mode TEXT, backtest_mode TEXT, gate_mode TEXT, state_mode TEXT, shape_key TEXT, state_key TEXT, trend_key TEXT, drift_key TEXT, break_key TEXT, recovery_key TEXT, block_code TEXT, reason_summary TEXT, reason_text TEXT, data_ok INTEGER DEFAULT 0, settled INTEGER DEFAULT 0, result TEXT DEFAULT 'WAIT', actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, pnl REAL DEFAULT 0, settle_time INTEGER DEFAULT 0, settled_at INTEGER DEFAULT 0, miss_reason TEXT, deep_score REAL DEFAULT 0, deep_confidence REAL DEFAULT 0, deep_feature_p REAL DEFAULT 0, deep_position_score REAL DEFAULT 0, deep_number_score REAL DEFAULT 0, deep_cycle_score REAL DEFAULT 0, deep_transition_score REAL DEFAULT 0, deep_state_score REAL DEFAULT 0, bias_score REAL DEFAULT 0, bias_confidence REAL DEFAULT 0, profile_score REAL DEFAULT 0, fallback_profile_score REAL DEFAULT 0, runtime_profile_score REAL DEFAULT 0, state_number_profile_score REAL DEFAULT 0, teacher_weight REAL DEFAULT 1, state_confidence REAL DEFAULT 0, audit_penalty REAL DEFAULT 0, audit_boost REAL DEFAULT 0, ai_judge_score REAL DEFAULT 0, ai_judge_confidence REAL DEFAULT 0, evidence_score REAL DEFAULT 0, evidence_risk REAL DEFAULT 0, conflict_level REAL DEFAULT 0, a_plus_score REAL DEFAULT 0, promotion_score REAL DEFAULT 0, weakest_number_score REAL DEFAULT 0, balance_score REAL DEFAULT 0, deep_profile_key TEXT, deep_profile_hit_rate REAL DEFAULT 0, deep_profile_sample INTEGER DEFAULT 0, deep_fallback_level INTEGER DEFAULT 0, fallback_profile_key TEXT, promotion_status TEXT, a_plus_status TEXT, candidate_evidence_key TEXT, snapshot_id TEXT, deep_attribution TEXT, deep_reason TEXT, ai_judge_summary TEXT, learning_summary TEXT, self_diagnosis_text TEXT, manual_backtest_support INTEGER DEFAULT 0, state_sample INTEGER DEFAULT 0, cache_sample INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, runtime_hit_rate REAL DEFAULT 0, number_profile_hit_rate REAL DEFAULT 0, realtime_cost_ms INTEGER DEFAULT 0, realtime_timeout INTEGER DEFAULT 0, stat_scope TEXT, teacher_matrix_sample INTEGER DEFAULT 0, teacher_matrix_positive INTEGER DEFAULT 0, teacher_matrix_negative INTEGER DEFAULT 0, teacher_matrix_state_sample INTEGER DEFAULT 0, teacher_matrix_combo_sample INTEGER DEFAULT 0, teacher_matrix_hit_rate REAL DEFAULT 0, teacher_matrix_recent_hit_rate REAL DEFAULT 0, teacher_matrix_confidence REAL DEFAULT 0, teacher_matrix_score REAL DEFAULT 0, teacher_matrix_prob_adjust REAL DEFAULT 0, teacher_matrix_gate_adjust REAL DEFAULT 0, teacher_matrix_risk_penalty REAL DEFAULT 0, teacher_matrix_position_score REAL DEFAULT 0, teacher_matrix_number_score REAL DEFAULT 0, teacher_matrix_combo_score REAL DEFAULT 0, teacher_matrix_state_score REAL DEFAULT 0, teacher_matrix_applied INTEGER DEFAULT 0, teacher_matrix_status TEXT, teacher_matrix_profile_key TEXT, teacher_matrix_combo_key TEXT, teacher_matrix_summary TEXT, source_type TEXT, is_realtime_real INTEGER DEFAULT 0, pre_draw_reason_fingerprint TEXT, pre_draw_reason_text TEXT, reason_valid_level TEXT, reason_score REAL DEFAULT 0, reason_edge REAL DEFAULT 0, hit_similarity REAL DEFAULT 0, miss_similarity REAL DEFAULT 0, contradiction_score REAL DEFAULT 0, causal_lift REAL DEFAULT 0, causal_confidence REAL DEFAULT 0, valid_reason_count INTEGER DEFAULT 0, false_reason_count INTEGER DEFAULT 0, forward_reason_sample INTEGER DEFAULT 0, forward_reason_hit_rate REAL DEFAULT 0, reason_wilson_score REAL DEFAULT 0, k3_role_summary TEXT, k3_main_number INTEGER DEFAULT 0, k3_support_number INTEGER DEFAULT 0, k3_defense_number INTEGER DEFAULT 0, combo_role_diversity REAL DEFAULT 0, combo_conflict REAL DEFAULT 0, fingerprint_cooling INTEGER DEFAULT 0, elite45_passed INTEGER DEFAULT 0, elite45_status TEXT, elite45_block_code TEXT, elite45_gate REAL DEFAULT 0, elite45_summary TEXT, v20_grade TEXT, v20_action TEXT, v20_candidate_key TEXT, v20_chain_key TEXT, v20_audit_key TEXT, v20_source_key TEXT, v20_rule_key TEXT, v20_same_class_key TEXT, v20_state_key TEXT, v20_position_key TEXT, v20_teacher_key TEXT, v20_rank_key TEXT, v20_guard_key TEXT, v20_shadow_key TEXT, v20_freeze_key TEXT, v20_seed_key TEXT, v20_final_cause_score REAL DEFAULT 0, v20_support_score REAL DEFAULT 0, v20_counter_score REAL DEFAULT 0, v20_confidence REAL DEFAULT 0, v20_sample_trust REAL DEFAULT 0, v20_recent_effectiveness REAL DEFAULT 0, v20_action_confidence REAL DEFAULT 0, v20_teacher_matrix_score REAL DEFAULT 0, v20_position_edge REAL DEFAULT 0, v20_number_teacher_edge REAL DEFAULT 0, v20_combo_role_edge REAL DEFAULT 0, v20_third_guard_edge REAL DEFAULT 0, v20_rank_counterfactual_edge REAL DEFAULT 0, v20_source_health_edge REAL DEFAULT 0, v20_state_health_edge REAL DEFAULT 0, v20_cycle_health_edge REAL DEFAULT 0, v20_seed_similarity REAL DEFAULT 0, v20_feature_distance REAL DEFAULT 0, v20_exact_state_match INTEGER DEFAULT 0, v20_relaxed_state_match INTEGER DEFAULT 0, v20_timing_offset INTEGER DEFAULT 0, v20_timing_bucket TEXT, v20_shadow_health REAL DEFAULT 0, v20_false_strong_risk REAL DEFAULT 0, v20_rule_decay_risk REAL DEFAULT 0, v20_same_class_lose_risk REAL DEFAULT 0, v20_guard_drag_risk REAL DEFAULT 0, v20_position_wrong_risk REAL DEFAULT 0, v20_position_healthy INTEGER DEFAULT 0, v20_number_teacher_support INTEGER DEFAULT 0, v20_rank_counterfactual_ok INTEGER DEFAULT 0, v20_third_guard_ok INTEGER DEFAULT 0, v20_shadow_health_pass INTEGER DEFAULT 0, v20_false_strong_frozen INTEGER DEFAULT 0, v20_same_class_frozen INTEGER DEFAULT 0, v20_rule_active INTEGER DEFAULT 0, v20_rule_frozen INTEGER DEFAULT 0, v20_source_healthy INTEGER DEFAULT 0, v20_state_healthy INTEGER DEFAULT 0, v20_reverse_reason_code TEXT, v20_action_suggestion TEXT, v20_evidence_json TEXT, v20_counter_evidence_json TEXT, v20_reason_atoms TEXT, v20_source_votes TEXT, v20_risk_tags TEXT, v20_decision_reason TEXT, v20_hold_reason TEXT, v20_miss_reason TEXT, dynamic_cluster_id TEXT, dynamic_cluster_status TEXT, dynamic_cluster_prototype_key TEXT, dynamic_cluster_feature_key TEXT, dynamic_cluster_sample INTEGER DEFAULT 0, dynamic_cluster_hit_count INTEGER DEFAULT 0, dynamic_cluster_miss_count INTEGER DEFAULT 0, dynamic_cluster_recent_sample INTEGER DEFAULT 0, dynamic_cluster_recent_hit_count INTEGER DEFAULT 0, dynamic_cluster_hit_rate REAL DEFAULT 0, dynamic_cluster_recent_hit_rate REAL DEFAULT 0, dynamic_cluster_confidence REAL DEFAULT 0, dynamic_cluster_distance REAL DEFAULT 0, dynamic_cluster_volatility REAL DEFAULT 0, dynamic_cluster_max_lose INTEGER DEFAULT 0, dynamic_cluster_counter_score REAL DEFAULT 0, dynamic_cluster_version TEXT, dynamic_cluster_reason TEXT, position_need_key TEXT, position_need_summary TEXT, position_need_strength REAL DEFAULT 0, position_need_chaos_risk REAL DEFAULT 0, position_need_repair_score REAL DEFAULT 0, position_need_relay_score REAL DEFAULT 0, number_supply_key TEXT, number_supply_summary TEXT, number_supply_strength REAL DEFAULT 0, number_supply_risk REAL DEFAULT 0, need_supply_match_key TEXT, need_supply_match_score REAL DEFAULT 0, need_supply_third_quality REAL DEFAULT 0, need_supply_role_diversity REAL DEFAULT 0, need_supply_risk REAL DEFAULT 0, need_supply_summary TEXT, k3_role_main TEXT, k3_role_assist TEXT, k3_role_defense TEXT, k3_role_main_number INTEGER DEFAULT 0, k3_role_assist_number INTEGER DEFAULT 0, k3_role_defense_number INTEGER DEFAULT 0, k3_role_structure TEXT, counterfactual_reason_key TEXT, counterfactual_rank INTEGER DEFAULT 0, counterfactual_rank_score REAL DEFAULT 0, counterfactual_why_not_risk REAL DEFAULT 0, counterfactual_why_not TEXT, counterfactual_summary TEXT, cause_ablation_key TEXT, cause_ablation_score REAL DEFAULT 0, cause_ablation_false_cause_count INTEGER DEFAULT 0, cause_ablation_summary TEXT, miss_reason_type TEXT, miss_reason_matrix_key TEXT, miss_reason_detail TEXT, adaptive_judge_candidate_count INTEGER DEFAULT 0, adaptive_judge_status TEXT, adaptive_judge_action TEXT, adaptive_judge_domain TEXT, adaptive_cluster_hit_rate REAL DEFAULT 0, adaptive_cluster_recent_hit_rate REAL DEFAULT 0, adaptive_cluster_sample INTEGER DEFAULT 0, adaptive_cluster_confidence REAL DEFAULT 0, adaptive_cause_score REAL DEFAULT 0, adaptive_counter_score REAL DEFAULT 0, adaptive_promote_score REAL DEFAULT 0, adaptive_freeze_score REAL DEFAULT 0, adaptive_judge_reason TEXT, adaptive_judge_version TEXT, promotion_cluster_status TEXT, promotion_cluster_hit_rate REAL DEFAULT 0, promotion_cluster_recent_hit_rate REAL DEFAULT 0, promotion_cluster_sample INTEGER DEFAULT 0, promotion_cluster_reason TEXT, why_bet TEXT, why_no_bet TEXT, why_hit TEXT, why_miss TEXT, dynamic_causal_summary TEXT, snapshot_key TEXT, candidate_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, raw_rank_score REAL DEFAULT 0, calibrated_hit_probability REAL DEFAULT 0, contract_decision_id TEXT, candidate_hash TEXT, snapshot_max_issue TEXT, trained_until_issue TEXT, build_id TEXT, cohort_id TEXT, schema_version TEXT, feature_schema_version TEXT, ranking_version TEXT, calibration_version TEXT, gate_policy_version TEXT, pipeline_version TEXT, contract_valid INTEGER DEFAULT 0, contract_status TEXT, coarse_family_key TEXT, mid_family_key TEXT, fine_family_key TEXT, position_family_key TEXT, posterior_matched_level TEXT, posterior_family_key TEXT, posterior_policy TEXT, posterior_version TEXT, posterior_sample INTEGER DEFAULT 0, posterior_hit INTEGER DEFAULT 0, posterior_mean REAL DEFAULT 0, posterior_wilson_lower REAL DEFAULT 0, posterior_recent_rate REAL DEFAULT 0, posterior_drift REAL DEFAULT 0, family_wilson_lower REAL DEFAULT 0, calibration_bucket INTEGER DEFAULT 0, calibration_sample INTEGER DEFAULT 0, calibration_total_sample INTEGER DEFAULT 0, calibration_ready INTEGER DEFAULT 0, calibration_status TEXT, calibration_brier REAL DEFAULT 0, authorization_id TEXT, authorization_valid INTEGER DEFAULT 0, authorized_mode TEXT, authorization_reject_code TEXT, authorized_at INTEGER DEFAULT 0, authorization_probability REAL DEFAULT 0, authorization_gate REAL DEFAULT 0, authorization_edge REAL DEFAULT 0, teacher_policy TEXT, dynamic_policy TEXT, adaptive_policy TEXT, micro_walk_forward_status TEXT, micro_walk_forward_version TEXT, micro_walk_forward_as_of_issue TEXT, micro_walk_forward_sample INTEGER DEFAULT 0, honest_probability REAL DEFAULT 0, action_utility REAL DEFAULT 0, action_structure_score REAL DEFAULT 0, action_discrimination_score REAL DEFAULT 0, action_maturity_score REAL DEFAULT 0, action_uncertainty_penalty REAL DEFAULT 0, action_frequency_support REAL DEFAULT 0, action_utility_version TEXT, lane_candidate TEXT, oos_rank_slot INTEGER DEFAULT 0, oos_candidate_count INTEGER DEFAULT 0, v42_accept_score REAL DEFAULT 0, v42_family_lcb REAL DEFAULT 0, v42_regime_quality REAL DEFAULT 0, v42_association_score REAL DEFAULT 0, v42_distillation_risk REAL DEFAULT 0, v42_support_votes INTEGER DEFAULT 0, v42_regime_family TEXT, v42_risk_coverage_mode TEXT, v42_primary_reject TEXT, v42_secondary_reject TEXT, v42_tertiary_reject TEXT, v42_lane_policy_version TEXT, v43_strategy_support REAL DEFAULT 0, v43_strategy_risk REAL DEFAULT 0, v43_strategy_confidence REAL DEFAULT 0, v43_strategy_diversity REAL DEFAULT 0, v43_strategy_maturity REAL DEFAULT 0, v43_safe_probability REAL DEFAULT 0, v43_probability_residual REAL DEFAULT 0, v43_ablation_fragility REAL DEFAULT 0, v43_probability_lock REAL DEFAULT 0, v43_evidence_group_count INTEGER DEFAULT 0, v43_hard_risk INTEGER DEFAULT 0, v43_dominant_evidence TEXT, v43_missing_evidence TEXT, v43_evidence_fingerprint TEXT, v43_probability_lock_hash TEXT, v43_pipeline_stage TEXT, v43_invariant_status TEXT, v43_strategy_version TEXT, v46_event_probability REAL DEFAULT 0, v46_action_lcb REAL DEFAULT 0, v46_probability_uncertainty REAL DEFAULT 0, v46_expected_value REAL DEFAULT 0, v46_global_calibrated_probability REAL DEFAULT 0, v46_position_adjustment REAL DEFAULT 0, v46_rank_adjustment REAL DEFAULT 0, v46_regime_adjustment REAL DEFAULT 0, v46_calibration_confidence REAL DEFAULT 0, v46_final_rank INTEGER DEFAULT 0, v46_formal_candidate_count INTEGER DEFAULT 0, v46_version TEXT, v46_number_distribution_summary TEXT, v46_probability_trace TEXT, v46_root_cause_code TEXT, v46_root_cause_owner TEXT, v46_final_contract_hash TEXT, v46_lane TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- deep_attribution
CREATE TABLE deep_attribution (_id INTEGER PRIMARY KEY AUTOINCREMENT, attribution_id TEXT NOT NULL UNIQUE, target_issue TEXT, base_issue TEXT, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, main_reason_code TEXT, main_reason_text TEXT, confidence REAL DEFAULT 0, should_reward INTEGER DEFAULT 0, should_punish INTEGER DEFAULT 0, affected_module TEXT, profile_key TEXT, position_impact REAL DEFAULT 0, number_impact REAL DEFAULT 0, cycle_impact REAL DEFAULT 0, transition_impact REAL DEFAULT 0, calibration_impact REAL DEFAULT 0, teacher_matrix_impact REAL DEFAULT 0, reason_impact REAL DEFAULT 0, learning_text TEXT, created_at INTEGER DEFAULT 0);

-- deep_cycle
CREATE TABLE deep_cycle (_id INTEGER PRIMARY KEY AUTOINCREMENT, cycle_key TEXT NOT NULL UNIQUE, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, avg_gap REAL DEFAULT 0, last_seen_gap INTEGER DEFAULT 0, max_gap INTEGER DEFAULT 0, gap_variance REAL DEFAULT 0, cycle_score REAL DEFAULT 0, cycle_bucket INTEGER DEFAULT 0, near_cycle_point INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- deep_profile
CREATE TABLE deep_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE, position INTEGER DEFAULT 0, state_mode TEXT, trend_bucket INTEGER DEFAULT 0, drift_bucket INTEGER DEFAULT 0, break_bucket INTEGER DEFAULT 0, recovery_bucket INTEGER DEFAULT 0, cycle_bucket INTEGER DEFAULT 0, relation_bucket INTEGER DEFAULT 0, total_sample INTEGER DEFAULT 0, total_hit INTEGER DEFAULT 0, total_hit_rate REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, decay_weight REAL DEFAULT 1, active_flag INTEGER DEFAULT 1, stale_flag INTEGER DEFAULT 0, recover_flag INTEGER DEFAULT 0, last_hit_at INTEGER DEFAULT 0, last_miss_at INTEGER DEFAULT 0, fail_count INTEGER DEFAULT 0, recover_count INTEGER DEFAULT 0, avg_final_p REAL DEFAULT 0, avg_edge REAL DEFAULT 0, avg_deep_score REAL DEFAULT 0, teacher_matrix_score REAL DEFAULT 0, reason_score REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- deep_transition
CREATE TABLE deep_transition (_id INTEGER PRIMARY KEY AUTOINCREMENT, transition_key TEXT NOT NULL UNIQUE, position INTEGER DEFAULT 0, from_number INTEGER DEFAULT 0, mid_number INTEGER DEFAULT 0, to_number INTEGER DEFAULT 0, chain_type TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, lift REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, support_score REAL DEFAULT 0, confidence REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- diagnosis_case_log
CREATE TABLE diagnosis_case_log (_id INTEGER PRIMARY KEY AUTOINCREMENT,case_key TEXT UNIQUE,source_type TEXT,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,base_issue TEXT,target_issue TEXT,decision_type TEXT,stat_scope TEXT,position INTEGER DEFAULT 0,n1 INTEGER DEFAULT 0,n2 INTEGER DEFAULT 0,n3 INTEGER DEFAULT 0,k3_mask INTEGER DEFAULT 0,actual_number INTEGER DEFAULT 0,result TEXT,hit INTEGER DEFAULT 0,pnl REAL DEFAULT 0,final_p REAL DEFAULT 0,dynamic_gate REAL DEFAULT 0,edge REAL DEFAULT 0,raw_p REAL DEFAULT 0,calibrated_p REAL DEFAULT 0,teacher_status TEXT,teacher_sample INTEGER DEFAULT 0,teacher_hit_rate REAL DEFAULT 0,teacher_confidence REAL DEFAULT 0,teacher_recent_validity REAL DEFAULT 0,v20_status TEXT,v20_support REAL DEFAULT 0,v20_risk REAL DEFAULT 0,evidence_contribution REAL DEFAULT 0,counter_contribution REAL DEFAULT 0,sample_reliability REAL DEFAULT 0,recent_validity REAL DEFAULT 0,window_state_key TEXT,similar_window_sample INTEGER DEFAULT 0,similar_window_hit_rate REAL DEFAULT 0,miss_reason TEXT,failure_type TEXT,responsible_module TEXT,fix_hint TEXT,module_name TEXT,module_status TEXT,freeze_key TEXT,freeze_level TEXT,judge_status TEXT,judge_score REAL DEFAULT 0,judge_risk REAL DEFAULT 0,shadow_would_hit INTEGER DEFAULT 0,promotion_hint TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT, record_key TEXT, status TEXT);

-- diagnosis_fix_plan
CREATE TABLE diagnosis_fix_plan (_id INTEGER PRIMARY KEY AUTOINCREMENT,plan_key TEXT UNIQUE,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,file_name TEXT,priority INTEGER DEFAULT 0,overall_problem TEXT,problem_evidence TEXT,why_this_file TEXT,what_to_change TEXT,expected_effect TEXT,risk_if_not_change TEXT,dependency TEXT,new_engine_needed INTEGER DEFAULT 0,strategy_gap TEXT,risk_warning TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT, record_key TEXT, source_type TEXT, base_issue TEXT, target_issue TEXT, position INTEGER DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, teacher_status TEXT, v20_status TEXT, window_state_key TEXT, module_name TEXT, status TEXT);

-- diagnostic_log
CREATE TABLE diagnostic_log (_id INTEGER PRIMARY KEY AUTOINCREMENT, log_key TEXT NOT NULL UNIQUE, created_at INTEGER DEFAULT 0, level TEXT, module TEXT, code TEXT, message TEXT, detail TEXT);

-- draw_dependency_input_cp12
CREATE TABLE draw_dependency_input_cp12(_id INTEGER NOT NULL,issue TEXT PRIMARY KEY,open_time INTEGER NOT NULL,fetch_time INTEGER NOT NULL,source_line TEXT,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT NULL,n5 INTEGER NOT NULL,n6 INTEGER NOT NULL,n7 INTEGER NOT NULL,n8 INTEGER NOT NULL,n9 INTEGER NOT NULL,n10 INTEGER NOT NULL,dirty INTEGER NOT NULL CHECK(dirty=0),dirty_reason TEXT,first_seen_at INTEGER NOT NULL,draw_hash_version INTEGER NOT NULL,draw_hash_v2 TEXT NOT NULL,input_hash TEXT NOT NULL,retained_at INTEGER NOT NULL);

-- draw_dependency_source_cp12
CREATE VIEW draw_dependency_source_cp12 AS SELECT _id,issue,open_time,fetch_time,source_line,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,dirty_reason,first_seen_at,draw_hash_version,draw_hash_v2,raw_json FROM draws UNION ALL SELECT _id,issue,open_time,fetch_time,source_line,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,dirty_reason,first_seen_at,draw_hash_version,draw_hash_v2,'' AS raw_json FROM draw_dependency_input_cp12 e WHERE NOT EXISTS(SELECT 1 FROM draws d WHERE d.issue=e.issue);

-- draws
CREATE TABLE draws (_id INTEGER PRIMARY KEY AUTOINCREMENT, issue TEXT NOT NULL UNIQUE, open_time INTEGER DEFAULT 0, fetch_time INTEGER DEFAULT 0, source_line TEXT, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, n4 INTEGER DEFAULT 0, n5 INTEGER DEFAULT 0, n6 INTEGER DEFAULT 0, n7 INTEGER DEFAULT 0, n8 INTEGER DEFAULT 0, n9 INTEGER DEFAULT 0, n10 INTEGER DEFAULT 0, raw_json TEXT, dirty INTEGER DEFAULT 0, dirty_reason TEXT, first_seen_at INTEGER NOT NULL DEFAULT 0, draw_hash_version INTEGER NOT NULL DEFAULT 1, draw_hash_v2 TEXT NOT NULL DEFAULT '');

-- dynamic_causal_state
CREATE TABLE dynamic_causal_state (state_key TEXT PRIMARY KEY, state_value TEXT, updated_at INTEGER DEFAULT 0);

-- dynamic_cause_chain
CREATE TABLE dynamic_cause_chain (_id INTEGER PRIMARY KEY AUTOINCREMENT, chain_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, candidate_key TEXT, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, state_key TEXT, source_key TEXT, rule_key TEXT, same_class_key TEXT, grade TEXT, final_cause_score REAL DEFAULT 0, support_score REAL DEFAULT 0, counter_score REAL DEFAULT 0, confidence REAL DEFAULT 0, sample_trust REAL DEFAULT 0, recent_effectiveness REAL DEFAULT 0, action_suggestion TEXT, position_edge REAL DEFAULT 0, number_teacher_edge REAL DEFAULT 0, combo_role_edge REAL DEFAULT 0, third_guard_edge REAL DEFAULT 0, rank_counterfactual_edge REAL DEFAULT 0, source_health_edge REAL DEFAULT 0, state_health_edge REAL DEFAULT 0, cycle_health_edge REAL DEFAULT 0, seed_similarity REAL DEFAULT 0, shadow_health REAL DEFAULT 0, false_strong_risk REAL DEFAULT 0, rule_decay_risk REAL DEFAULT 0, same_class_lose_risk REAL DEFAULT 0, guard_drag_risk REAL DEFAULT 0, position_wrong_risk REAL DEFAULT 0, position_healthy INTEGER DEFAULT 0, number_teacher_support INTEGER DEFAULT 0, rank_pass INTEGER DEFAULT 0, third_guard_pass INTEGER DEFAULT 0, shadow_pass INTEGER DEFAULT 0, false_strong_frozen INTEGER DEFAULT 0, same_class_frozen INTEGER DEFAULT 0, rule_active INTEGER DEFAULT 0, source_healthy INTEGER DEFAULT 0, state_healthy INTEGER DEFAULT 0, evidence_json TEXT, counter_evidence_json TEXT, risk_tags TEXT, reason_text TEXT, dynamic_cluster_id TEXT, dynamic_cluster_status TEXT, dynamic_cluster_prototype_key TEXT, dynamic_cluster_feature_key TEXT, dynamic_cluster_sample INTEGER DEFAULT 0, dynamic_cluster_hit_count INTEGER DEFAULT 0, dynamic_cluster_miss_count INTEGER DEFAULT 0, dynamic_cluster_recent_sample INTEGER DEFAULT 0, dynamic_cluster_recent_hit_count INTEGER DEFAULT 0, dynamic_cluster_hit_rate REAL DEFAULT 0, dynamic_cluster_recent_hit_rate REAL DEFAULT 0, dynamic_cluster_confidence REAL DEFAULT 0, dynamic_cluster_distance REAL DEFAULT 0, dynamic_cluster_volatility REAL DEFAULT 0, dynamic_cluster_max_lose INTEGER DEFAULT 0, dynamic_cluster_counter_score REAL DEFAULT 0, dynamic_cluster_version TEXT, dynamic_cluster_reason TEXT, position_need_key TEXT, position_need_summary TEXT, position_need_strength REAL DEFAULT 0, position_need_chaos_risk REAL DEFAULT 0, position_need_repair_score REAL DEFAULT 0, position_need_relay_score REAL DEFAULT 0, number_supply_key TEXT, number_supply_summary TEXT, number_supply_strength REAL DEFAULT 0, number_supply_risk REAL DEFAULT 0, need_supply_match_key TEXT, need_supply_match_score REAL DEFAULT 0, need_supply_third_quality REAL DEFAULT 0, need_supply_role_diversity REAL DEFAULT 0, need_supply_risk REAL DEFAULT 0, need_supply_summary TEXT, k3_role_main TEXT, k3_role_assist TEXT, k3_role_defense TEXT, k3_role_main_number INTEGER DEFAULT 0, k3_role_assist_number INTEGER DEFAULT 0, k3_role_defense_number INTEGER DEFAULT 0, k3_role_structure TEXT, counterfactual_reason_key TEXT, counterfactual_rank INTEGER DEFAULT 0, counterfactual_rank_score REAL DEFAULT 0, counterfactual_why_not_risk REAL DEFAULT 0, counterfactual_why_not TEXT, counterfactual_summary TEXT, cause_ablation_key TEXT, cause_ablation_score REAL DEFAULT 0, cause_ablation_false_cause_count INTEGER DEFAULT 0, cause_ablation_summary TEXT, miss_reason_type TEXT, miss_reason_matrix_key TEXT, miss_reason_detail TEXT, adaptive_judge_candidate_count INTEGER DEFAULT 0, adaptive_judge_status TEXT, adaptive_judge_action TEXT, adaptive_judge_domain TEXT, adaptive_cluster_hit_rate REAL DEFAULT 0, adaptive_cluster_recent_hit_rate REAL DEFAULT 0, adaptive_cluster_sample INTEGER DEFAULT 0, adaptive_cluster_confidence REAL DEFAULT 0, adaptive_cause_score REAL DEFAULT 0, adaptive_counter_score REAL DEFAULT 0, adaptive_promote_score REAL DEFAULT 0, adaptive_freeze_score REAL DEFAULT 0, adaptive_judge_reason TEXT, adaptive_judge_version TEXT, promotion_cluster_status TEXT, promotion_cluster_hit_rate REAL DEFAULT 0, promotion_cluster_recent_hit_rate REAL DEFAULT 0, promotion_cluster_sample INTEGER DEFAULT 0, promotion_cluster_reason TEXT, why_bet TEXT, why_no_bet TEXT, why_hit TEXT, why_miss TEXT, dynamic_causal_summary TEXT, snapshot_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- dynamic_domain_stat_v20
CREATE TABLE dynamic_domain_stat_v20 (domain_key TEXT PRIMARY KEY, parent_domain_key TEXT, coarse_domain_key TEXT, status TEXT, version TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, recent_hit_count INTEGER DEFAULT 0, misleading_streak INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, posterior_mean REAL DEFAULT 0, lower_bound REAL DEFAULT 0, upper_bound REAL DEFAULT 1, drift_score REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- dynamic_freeze_matrix
CREATE TABLE dynamic_freeze_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT,freeze_key TEXT UNIQUE,freeze_type TEXT,freeze_value TEXT,freeze_level TEXT,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,frozen INTEGER DEFAULT 0,freeze_until_issue TEXT,freeze_until_time INTEGER DEFAULT 0,real_count INTEGER DEFAULT 0,real_hit INTEGER DEFAULT 0,real_miss INTEGER DEFAULT 0,real_hit_rate REAL DEFAULT 0,shadow_review_count INTEGER DEFAULT 0,shadow_hit INTEGER DEFAULT 0,shadow_miss INTEGER DEFAULT 0,shadow_hit_rate REAL DEFAULT 0,recover_need_hits INTEGER DEFAULT 0,recover_need_rate REAL DEFAULT 0,freeze_reason TEXT,recover_hint TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT, record_key TEXT, source_type TEXT, base_issue TEXT, target_issue TEXT, position INTEGER DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, teacher_status TEXT, v20_status TEXT, window_state_key TEXT, module_name TEXT, status TEXT);

-- dynamic_window_cluster
CREATE TABLE dynamic_window_cluster (_id INTEGER PRIMARY KEY AUTOINCREMENT, cluster_id TEXT NOT NULL UNIQUE, prototype_key TEXT, prototype_vector TEXT, status TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, max_lose_streak INTEGER DEFAULT 0, confidence REAL DEFAULT 0, volatility REAL DEFAULT 0, counter_score REAL DEFAULT 0, prototype_distance REAL DEFAULT 0, last_target_issue TEXT, version TEXT, reason TEXT, created_at INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0, state_key TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- elite45_audit_log
CREATE TABLE elite45_audit_log (_id INTEGER PRIMARY KEY AUTOINCREMENT, audit_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, passed INTEGER DEFAULT 0, status TEXT, block_code TEXT, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, elite_gate REAL DEFAULT 0, reason_edge REAL DEFAULT 0, hit_similarity REAL DEFAULT 0, miss_similarity REAL DEFAULT 0, contradiction_score REAL DEFAULT 0, combo_conflict REAL DEFAULT 0, combo_role_diversity REAL DEFAULT 0, drift_risk REAL DEFAULT 0, break_risk REAL DEFAULT 0, risk_score REAL DEFAULT 0, valid_reason_count INTEGER DEFAULT 0, false_reason_count INTEGER DEFAULT 0, fingerprint_cooling INTEGER DEFAULT 0, reason_level TEXT, reason_fingerprint TEXT, k3_role_summary TEXT, summary_text TEXT);

-- f02_recovery_no_delete
CREATE TRIGGER f02_recovery_no_delete BEFORE DELETE ON v57_settlement_repair_audit_f02 BEGIN SELECT RAISE(ABORT,'F02_RECOVERY_RECEIPT_PERMANENT'); END;

-- f02_recovery_no_update
CREATE TRIGGER f02_recovery_no_update BEFORE UPDATE ON v57_settlement_repair_audit_f02 BEGIN SELECT RAISE(ABORT,'F02_RECOVERY_RECEIPT_IMMUTABLE'); END;

-- f05_cal_frozen_obs_archive_delete
CREATE TRIGGER f05_cal_frozen_obs_archive_delete BEFORE DELETE ON research_candidate_cal_observation_r932 WHEN EXISTS(SELECT 1 FROM research_candidate_cal_study_r932 s WHERE s.study_hash=OLD.study_hash AND s.state='FROZEN') AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_candidate_cal_observation_r932' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'F05_FROZEN_CAL_OBS_ARCHIVE_REQUIRED'); END;

-- f05_cal_frozen_obs_no_replace
CREATE TRIGGER f05_cal_frozen_obs_no_replace BEFORE INSERT ON research_candidate_cal_observation_r932 WHEN EXISTS(SELECT 1 FROM research_candidate_cal_study_r932 s WHERE s.study_hash=NEW.study_hash AND s.state='FROZEN') BEGIN SELECT RAISE(ABORT,'F05_FROZEN_CAL_OBS_FIRST_WRITE'); END;

-- f05_cal_frozen_obs_no_update
CREATE TRIGGER f05_cal_frozen_obs_no_update BEFORE UPDATE ON research_candidate_cal_observation_r932 WHEN EXISTS(SELECT 1 FROM research_candidate_cal_study_r932 s WHERE s.study_hash=OLD.study_hash AND s.state='FROZEN') BEGIN SELECT RAISE(ABORT,'F05_FROZEN_CAL_OBS_IMMUTABLE'); END;

-- f05_cal_model_archive_delete
CREATE TRIGGER f05_cal_model_archive_delete BEFORE DELETE ON research_candidate_cal_model_r932 WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_candidate_cal_model_r932' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'F05_CAL_MODEL_ARCHIVE_REQUIRED'); END;

-- f05_cal_model_no_replace
CREATE TRIGGER f05_cal_model_no_replace BEFORE INSERT ON research_candidate_cal_model_r932 WHEN EXISTS(SELECT 1 FROM research_candidate_cal_model_r932 m WHERE m.study_hash=NEW.study_hash) BEGIN SELECT RAISE(ABORT,'F05_CAL_MODEL_FIRST_WRITE'); END;

-- f05_cal_model_no_update
CREATE TRIGGER f05_cal_model_no_update BEFORE UPDATE ON research_candidate_cal_model_r932 BEGIN SELECT RAISE(ABORT,'F05_CAL_MODEL_IMMUTABLE'); END;

-- f05_exact_probability_no_replace
CREATE TRIGGER f05_exact_probability_no_replace BEFORE INSERT ON research_exact_probability_f05 WHEN EXISTS(SELECT 1 FROM research_exact_probability_f05 e WHERE e.game_code=NEW.game_code AND e.target_issue=NEW.target_issue) BEGIN SELECT RAISE(ABORT,'F05_EXACT_PROBABILITY_FIRST_WRITE'); END;

-- f05_exact_probability_no_update
CREATE TRIGGER f05_exact_probability_no_update BEFORE UPDATE ON research_exact_probability_f05 BEGIN SELECT RAISE(ABORT,'F05_EXACT_PROBABILITY_IMMUTABLE'); END;

-- false_strong_freeze
CREATE TABLE false_strong_freeze (_id INTEGER PRIMARY KEY AUTOINCREMENT, freeze_key TEXT NOT NULL UNIQUE, state_key TEXT, source_key TEXT, rule_key TEXT, same_class_key TEXT, position INTEGER DEFAULT 0, rank_mode TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, consecutive_miss INTEGER DEFAULT 0, recent_false_strong_rate REAL DEFAULT 0, false_strong_risk REAL DEFAULT 0, freeze_level INTEGER DEFAULT 0, freeze_until_issue TEXT, is_frozen INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0, summary_text TEXT);

-- false_strong_profile
CREATE TABLE false_strong_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, false_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, reason_fingerprint TEXT, sample_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, consecutive_miss INTEGER DEFAULT 0, cooldown_until_index INTEGER DEFAULT 0, cooldown_until_time INTEGER DEFAULT 0, miss_rate REAL DEFAULT 0, hit_rate REAL DEFAULT 0, false_strong_score REAL DEFAULT 0, risk_penalty REAL DEFAULT 0, gate_boost REAL DEFAULT 0, status TEXT, summary_text TEXT, updated_at INTEGER DEFAULT 0);

-- forward_validation_stat_v20
CREATE TABLE forward_validation_stat_v20 (domain_key TEXT PRIMARY KEY, parent_domain_key TEXT, coarse_domain_key TEXT, status TEXT, version TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, recent_hit_count INTEGER DEFAULT 0, misleading_streak INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, posterior_mean REAL DEFAULT 0, lower_bound REAL DEFAULT 0, upper_bound REAL DEFAULT 1, drift_score REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- full_candidate_scan
CREATE TABLE full_candidate_scan (_id INTEGER PRIMARY KEY AUTOINCREMENT, candidate_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, raw_rank INTEGER DEFAULT 0, rank_no INTEGER DEFAULT 0, rank_group TEXT, raw_score REAL DEFAULT 0, base_k3_p REAL DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, margin REAL DEFAULT 0, state_key TEXT, source_key TEXT, rule_key TEXT, same_class_key TEXT, reason_atoms TEXT, source_votes TEXT, risk_tags TEXT, grade TEXT, action TEXT, hit INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, settled INTEGER DEFAULT 0, learned INTEGER DEFAULT 0, dynamic_cluster_id TEXT, dynamic_cluster_status TEXT, dynamic_cluster_prototype_key TEXT, dynamic_cluster_feature_key TEXT, dynamic_cluster_sample INTEGER DEFAULT 0, dynamic_cluster_hit_count INTEGER DEFAULT 0, dynamic_cluster_miss_count INTEGER DEFAULT 0, dynamic_cluster_recent_sample INTEGER DEFAULT 0, dynamic_cluster_recent_hit_count INTEGER DEFAULT 0, dynamic_cluster_hit_rate REAL DEFAULT 0, dynamic_cluster_recent_hit_rate REAL DEFAULT 0, dynamic_cluster_confidence REAL DEFAULT 0, dynamic_cluster_distance REAL DEFAULT 0, dynamic_cluster_volatility REAL DEFAULT 0, dynamic_cluster_max_lose INTEGER DEFAULT 0, dynamic_cluster_counter_score REAL DEFAULT 0, dynamic_cluster_version TEXT, dynamic_cluster_reason TEXT, position_need_key TEXT, position_need_summary TEXT, position_need_strength REAL DEFAULT 0, position_need_chaos_risk REAL DEFAULT 0, position_need_repair_score REAL DEFAULT 0, position_need_relay_score REAL DEFAULT 0, number_supply_key TEXT, number_supply_summary TEXT, number_supply_strength REAL DEFAULT 0, number_supply_risk REAL DEFAULT 0, need_supply_match_key TEXT, need_supply_match_score REAL DEFAULT 0, need_supply_third_quality REAL DEFAULT 0, need_supply_role_diversity REAL DEFAULT 0, need_supply_risk REAL DEFAULT 0, need_supply_summary TEXT, k3_role_main TEXT, k3_role_assist TEXT, k3_role_defense TEXT, k3_role_main_number INTEGER DEFAULT 0, k3_role_assist_number INTEGER DEFAULT 0, k3_role_defense_number INTEGER DEFAULT 0, k3_role_structure TEXT, counterfactual_reason_key TEXT, counterfactual_rank INTEGER DEFAULT 0, counterfactual_rank_score REAL DEFAULT 0, counterfactual_why_not_risk REAL DEFAULT 0, counterfactual_why_not TEXT, counterfactual_summary TEXT, cause_ablation_key TEXT, cause_ablation_score REAL DEFAULT 0, cause_ablation_false_cause_count INTEGER DEFAULT 0, cause_ablation_summary TEXT, miss_reason_type TEXT, miss_reason_matrix_key TEXT, miss_reason_detail TEXT, adaptive_judge_candidate_count INTEGER DEFAULT 0, adaptive_judge_status TEXT, adaptive_judge_action TEXT, adaptive_judge_domain TEXT, adaptive_cluster_hit_rate REAL DEFAULT 0, adaptive_cluster_recent_hit_rate REAL DEFAULT 0, adaptive_cluster_sample INTEGER DEFAULT 0, adaptive_cluster_confidence REAL DEFAULT 0, adaptive_cause_score REAL DEFAULT 0, adaptive_counter_score REAL DEFAULT 0, adaptive_promote_score REAL DEFAULT 0, adaptive_freeze_score REAL DEFAULT 0, adaptive_judge_reason TEXT, adaptive_judge_version TEXT, promotion_cluster_status TEXT, promotion_cluster_hit_rate REAL DEFAULT 0, promotion_cluster_recent_hit_rate REAL DEFAULT 0, promotion_cluster_sample INTEGER DEFAULT 0, promotion_cluster_reason TEXT, why_bet TEXT, why_no_bet TEXT, why_hit TEXT, why_miss TEXT, dynamic_causal_summary TEXT, snapshot_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- hierarchical_posterior_v28
CREATE TABLE hierarchical_posterior_v28 (_id INTEGER PRIMARY KEY AUTOINCREMENT, family_level TEXT NOT NULL, family_key TEXT NOT NULL, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, posterior_mean REAL DEFAULT 0, wilson_lower REAL DEFAULT 0, recent_rate REAL DEFAULT 0, drift_score REAL DEFAULT 0, last_target_issue TEXT, updated_at INTEGER DEFAULT 0, version TEXT, UNIQUE(family_level,family_key));

-- idx_ablation_cluster
CREATE INDEX idx_ablation_cluster ON cause_ablation_sample(cluster_id);

-- idx_ablation_score
CREATE INDEX idx_ablation_score ON cause_ablation_sample(total_ablation_score);

-- idx_adaptive_action
CREATE INDEX idx_adaptive_action ON adaptive_judge_sample(action);

-- idx_adaptive_candidate_count
CREATE INDEX idx_adaptive_candidate_count ON adaptive_judge_sample(target_issue, candidate_count);

-- idx_adaptive_cluster
CREATE INDEX idx_adaptive_cluster ON adaptive_judge_sample(cluster_id);

-- idx_adaptive_judge_created
CREATE INDEX idx_adaptive_judge_created ON adaptive_judge_log(created_at);

-- idx_adaptive_judge_status
CREATE INDEX idx_adaptive_judge_status ON adaptive_judge_log(judge_status,judge_score,judge_risk);

-- idx_adaptive_judge_target
CREATE INDEX idx_adaptive_judge_target ON adaptive_judge_log(target_issue,position);

-- idx_adaptive_status_action
CREATE INDEX idx_adaptive_status_action ON adaptive_judge_sample(adaptive_status, action);

-- idx_adaptive_target
CREATE INDEX idx_adaptive_target ON adaptive_judge_sample(target_issue);

-- idx_ai_judge_created
CREATE INDEX idx_ai_judge_created ON ai_judge_log(created_at);

-- idx_ai_judge_target
CREATE INDEX idx_ai_judge_target ON ai_judge_log(target_issue);

-- idx_atom_admission_cp12_state
CREATE INDEX idx_atom_admission_cp12_state ON research_atom_admission_cp12(state,through_issue,matrix_id);

-- idx_attr_module
CREATE INDEX idx_attr_module ON deep_attribution(affected_module);

-- idx_attr_profile
CREATE INDEX idx_attr_profile ON deep_attribution(profile_key);

-- idx_attr_reason
CREATE INDEX idx_attr_reason ON deep_attribution(main_reason_code);

-- idx_attr_target
CREATE INDEX idx_attr_target ON deep_attribution(target_issue);

-- idx_auto_learn_cursor_status
CREATE INDEX idx_auto_learn_cursor_status ON auto_learn_cursor(status);

-- idx_auto_learn_cursor_task
CREATE INDEX idx_auto_learn_cursor_task ON auto_learn_cursor(task_key);

-- idx_b03_action_rank
CREATE INDEX idx_b03_action_rank ON b03_action_event(game_code,target_issue,rank_no);

-- idx_b03_bundle_target
CREATE INDEX idx_b03_bundle_target ON b03_prediction_bundle(game_code,target_issue);

-- idx_b03_integrity_created
CREATE INDEX idx_b03_integrity_created ON b03_integrity_snapshot(created_at);

-- idx_b03_migration_status
CREATE INDEX idx_b03_migration_status ON b03_migration_journal(status,updated_at);

-- idx_b03_number_target
CREATE INDEX idx_b03_number_target ON b03_number_event(game_code,target_issue,position,number);

-- idx_b03_quarantine_issue
CREATE INDEX idx_b03_quarantine_issue ON b03_draw_quarantine(source_game_code,source_issue);

-- idx_b03_replay_target
CREATE INDEX idx_b03_replay_target ON b03_replay_audit(game_code,target_issue,created_at);

-- idx_b03_selected_lane
CREATE INDEX idx_b03_selected_lane ON b03_selected_event(game_code,lane,decision_action);

-- idx_b03_settlement_target
CREATE INDEX idx_b03_settlement_target ON b03_settlement_event(game_code,target_issue,created_at);

-- idx_b03_tx_target
CREATE INDEX idx_b03_tx_target ON b03_transaction_audit(game_code,target_issue,created_at);

-- idx_backtest_created
CREATE INDEX idx_backtest_created ON backtest_report(created_at);

-- idx_backtest_mode_window
CREATE INDEX idx_backtest_mode_window ON backtest_report(mode, window_no, created_at);

-- idx_bias_created
CREATE INDEX idx_bias_created ON bias_evidence(created_at);

-- idx_bias_issue
CREATE INDEX idx_bias_issue ON bias_evidence(issue);

-- idx_bias_status
CREATE INDEX idx_bias_status ON bias_evidence(status);

-- idx_break_memory_key
CREATE INDEX idx_break_memory_key ON break_memory(break_key);

-- idx_calibration_bucket
CREATE INDEX idx_calibration_bucket ON calibration_bucket(bucket_key);

-- idx_candidate_evidence_key
CREATE INDEX idx_candidate_evidence_key ON candidate_evidence(evidence_key);

-- idx_candidate_evidence_score
CREATE INDEX idx_candidate_evidence_score ON candidate_evidence(evidence_score);

-- idx_candidate_evidence_target
CREATE INDEX idx_candidate_evidence_target ON candidate_evidence(target_issue);

-- idx_candidate_scan_factor_cluster
CREATE INDEX idx_candidate_scan_factor_cluster ON full_candidate_scan(cluster_key, factor_key);

-- idx_candidate_scan_main_chain
CREATE INDEX idx_candidate_scan_main_chain ON full_candidate_scan(target_issue, reject_code, real_probe_allowed);

-- idx_candidate_scan_top40
CREATE INDEX idx_candidate_scan_top40 ON full_candidate_scan(target_issue, realtime_top40_rank, adaptive_input_flag);

-- idx_cfr_cluster
CREATE INDEX idx_cfr_cluster ON counterfactual_reason_sample(cluster_id);

-- idx_cfr_rank
CREATE INDEX idx_cfr_rank ON counterfactual_reason_sample(position_rank, k3_rank);

-- idx_cfr_target
CREATE INDEX idx_cfr_target ON counterfactual_reason_sample(target_issue);

-- idx_combo_memory_active
CREATE INDEX idx_combo_memory_active ON combo_memory(active_flag);

-- idx_combo_memory_key
CREATE INDEX idx_combo_memory_key ON combo_memory(shape_key, position, k3_mask);

-- idx_combo_role_key
CREATE INDEX idx_combo_role_key ON combo_role_profile(combo_role_key);

-- idx_combo_role_pos_mask
CREATE INDEX idx_combo_role_pos_mask ON combo_role_profile(position, k3_mask);

-- idx_combo_role_state
CREATE INDEX idx_combo_role_state ON combo_role_profile(state_key);

-- idx_combo_role_status
CREATE INDEX idx_combo_role_status ON combo_role_profile(status);

-- idx_combo_role_updated
CREATE INDEX idx_combo_role_updated ON combo_role_profile(updated_at);

-- idx_cp11_feature_protocol
CREATE INDEX idx_cp11_feature_protocol ON research_training_feature_row_cp11(protocol_hash,row_index);

-- idx_cp12_attachment_protocol
CREATE INDEX idx_cp12_attachment_protocol ON research_training_protocol_attachment_cp12(attachment_hash,protocol_hash);

-- idx_cp12_matrix_input_ready
CREATE INDEX idx_cp12_matrix_input_ready
 ON research_matrix_input_cp12(state,source_issue,source_ordinal,matrix_id,source_receipt_hash);

-- idx_cp12_matrix_input_replay
CREATE INDEX idx_cp12_matrix_input_replay
 ON research_matrix_input_cp12(matrix_id,source_ordinal,source_receipt_hash);

-- idx_cp12_matrix_pair_issue_position_window
CREATE INDEX idx_cp12_matrix_pair_issue_position_window ON research_fixed_pair_r932(target_issue,position,window_id);

-- idx_cp12_matrix_window_structure_window
CREATE INDEX idx_cp12_matrix_window_structure_window ON research_fixed_window_r932(structural_hash,window_id);

-- idx_cp12_model_ref_key
CREATE INDEX idx_cp12_model_ref_key ON research_durable_model_ref_cp12(model_key);

-- idx_cp12_seed_use_state
CREATE INDEX idx_cp12_seed_use_state ON research_molecule_use_cp12(state,seed_hash);

-- idx_cp12_teacher_raw_pin
CREATE INDEX idx_cp12_teacher_raw_pin ON research_action_teacher_witness_cp12(teacher_candidate_id,target_issue,action_id);

-- idx_cp12_working_bucket
CREATE INDEX idx_cp12_working_bucket ON research_working_storage_ref_cp12(bucket);

-- idx_cp2_archive_prune
CREATE INDEX idx_cp2_archive_prune ON research_evidence_archive_v2(source_table,state,last_issue);

-- idx_cp2_capture_due
CREATE INDEX idx_cp2_capture_due ON research_capture_reservation_v2(state,next_issue,cost_budget_us);

-- idx_cp2_finding_queue
CREATE INDEX idx_cp2_finding_queue ON research_finding_v2(state,severity,created_at);

-- idx_cp4_archive_member_lookup
CREATE INDEX idx_cp4_archive_member_lookup ON research_evidence_archive_member_v2(source_table,source_key_hash,row_hash);

-- idx_cp4_molecule_seed_state
CREATE INDEX idx_cp4_molecule_seed_state ON research_molecule_seed_v2(state,position,source_seed_id);

-- idx_cp4_resource_target
CREATE INDEX idx_cp4_resource_target ON research_cp4_candidate_resource(target_issue,candidate_key);

-- idx_cp5_input_binding_archive
CREATE INDEX idx_cp5_input_binding_archive ON research_task_input_binding_v2(archive_id,task_id);

-- idx_cp5_protocol_input
CREATE INDEX idx_cp5_protocol_input ON research_training_protocol_v2(input_hash,candidate_id);

-- idx_cp8_archive_member_rowid
CREATE UNIQUE INDEX idx_cp8_archive_member_rowid ON research_evidence_archive_member_v2(archive_id,source_table,source_rowid);

-- idx_critical_error_open_r815
CREATE INDEX idx_critical_error_open_r815 ON v57_critical_error_r815(resolved_at,domain,created_at);

-- idx_cycle_key
CREATE INDEX idx_cycle_key ON deep_cycle(cycle_key);

-- idx_cycle_pos_num
CREATE INDEX idx_cycle_pos_num ON deep_cycle(position, number);

-- idx_dcw_hit_rate
CREATE INDEX idx_dcw_hit_rate ON dynamic_window_cluster(hit_rate);

-- idx_dcw_prototype
CREATE INDEX idx_dcw_prototype ON dynamic_window_cluster(prototype_key);

-- idx_dcw_recent_hit_rate
CREATE INDEX idx_dcw_recent_hit_rate ON dynamic_window_cluster(recent_hit_rate);

-- idx_dcw_status
CREATE INDEX idx_dcw_status ON dynamic_window_cluster(status);

-- idx_dcw_updated
CREATE INDEX idx_dcw_updated ON dynamic_window_cluster(updated_at);

-- idx_decisions_action
CREATE INDEX idx_decisions_action ON decisions(action);

-- idx_decisions_adaptive_action
CREATE INDEX idx_decisions_adaptive_action ON decisions(adaptive_judge_action);

-- idx_decisions_adaptive_status
CREATE INDEX idx_decisions_adaptive_status ON decisions(adaptive_status, shadow_promotion_status);

-- idx_decisions_created
CREATE INDEX idx_decisions_created ON decisions(created_at);

-- idx_decisions_decision
CREATE INDEX idx_decisions_decision ON decisions(decision);

-- idx_decisions_dynamic_cluster
CREATE INDEX idx_decisions_dynamic_cluster ON decisions(dynamic_cluster_id);

-- idx_decisions_dynamic_status
CREATE INDEX idx_decisions_dynamic_status ON decisions(dynamic_cluster_status);

-- idx_decisions_elite45
CREATE INDEX idx_decisions_elite45 ON decisions(elite45_passed, elite45_status);

-- idx_decisions_factor_cluster
CREATE INDEX idx_decisions_factor_cluster ON decisions(cluster_key, factor_key);

-- idx_decisions_main_chain
CREATE INDEX idx_decisions_main_chain ON decisions(target_issue, decision, reject_code, real_probe_allowed);

-- idx_decisions_need_supply
CREATE INDEX idx_decisions_need_supply ON decisions(need_supply_match_score);

-- idx_decisions_profile
CREATE INDEX idx_decisions_profile ON decisions(deep_profile_key);

-- idx_decisions_real_scope
CREATE INDEX idx_decisions_real_scope ON decisions(decision, stat_scope, source_type, is_realtime_real, settled);

-- idx_decisions_reason_fp
CREATE INDEX idx_decisions_reason_fp ON decisions(pre_draw_reason_fingerprint);

-- idx_decisions_result
CREATE INDEX idx_decisions_result ON decisions(result);

-- idx_decisions_scope
CREATE INDEX idx_decisions_scope ON decisions(stat_scope);

-- idx_decisions_settled
CREATE INDEX idx_decisions_settled ON decisions(settled);

-- idx_decisions_state
CREATE INDEX idx_decisions_state ON decisions(state_key);

-- idx_decisions_target
CREATE INDEX idx_decisions_target ON decisions(target_issue);

-- idx_decisions_tm_profile
CREATE INDEX idx_decisions_tm_profile ON decisions(teacher_matrix_profile_key);

-- idx_decisions_v20_chain
CREATE INDEX idx_decisions_v20_chain ON decisions(v20_chain_key);

-- idx_decisions_v20_grade
CREATE INDEX idx_decisions_v20_grade ON decisions(v20_grade);

-- idx_deep_profile_active
CREATE INDEX idx_deep_profile_active ON deep_profile(active_flag, stale_flag);

-- idx_deep_profile_key
CREATE INDEX idx_deep_profile_key ON deep_profile(profile_key);

-- idx_deep_profile_pos_state
CREATE INDEX idx_deep_profile_pos_state ON deep_profile(position, state_mode);

-- idx_deep_profile_updated
CREATE INDEX idx_deep_profile_updated ON deep_profile(updated_at);

-- idx_diag_case_created
CREATE INDEX idx_diag_case_created ON diagnosis_case_log(created_at);

-- idx_diag_case_failure
CREATE INDEX idx_diag_case_failure ON diagnosis_case_log(failure_type,responsible_module);

-- idx_diag_case_shadow
CREATE INDEX idx_diag_case_shadow ON diagnosis_case_log(decision_type,shadow_would_hit);

-- idx_diag_case_target
CREATE INDEX idx_diag_case_target ON diagnosis_case_log(target_issue,position);

-- idx_diag_created
CREATE INDEX idx_diag_created ON diagnostic_log(created_at);

-- idx_diagnosis_created
CREATE INDEX idx_diagnosis_created ON self_diagnosis_log(created_at);

-- idx_diagnosis_level
CREATE INDEX idx_diagnosis_level ON self_diagnosis_log(level);

-- idx_draws_fetch_time
CREATE INDEX idx_draws_fetch_time ON draws(fetch_time);

-- idx_draws_first_seen_r815
CREATE INDEX idx_draws_first_seen_r815 ON draws(first_seen_at);

-- idx_draws_issue
CREATE INDEX idx_draws_issue ON draws(issue);

-- idx_draws_issue_id_r815
CREATE INDEX idx_draws_issue_id_r815 ON draws(issue DESC,_id DESC);

-- idx_draws_open_time
CREATE INDEX idx_draws_open_time ON draws(open_time);

-- idx_dynamic_causal_state_updated
CREATE INDEX idx_dynamic_causal_state_updated ON dynamic_causal_state(updated_at);

-- idx_dynamic_domain_status_v20
CREATE INDEX idx_dynamic_domain_status_v20 ON dynamic_domain_stat_v20(status,sample_count);

-- idx_elite45_audit_block
CREATE INDEX idx_elite45_audit_block ON elite45_audit_log(block_code);

-- idx_elite45_audit_created
CREATE INDEX idx_elite45_audit_created ON elite45_audit_log(created_at);

-- idx_elite45_audit_key
CREATE INDEX idx_elite45_audit_key ON elite45_audit_log(audit_key);

-- idx_elite45_audit_passed
CREATE INDEX idx_elite45_audit_passed ON elite45_audit_log(passed);

-- idx_elite45_audit_target
CREATE INDEX idx_elite45_audit_target ON elite45_audit_log(target_issue);

-- idx_f02_recovery_target
CREATE INDEX idx_f02_recovery_target ON v57_settlement_repair_audit_f02(target_issue,prior_recovery_count);

-- idx_false_strong_fp
CREATE INDEX idx_false_strong_fp ON false_strong_profile(reason_fingerprint);

-- idx_false_strong_key
CREATE INDEX idx_false_strong_key ON false_strong_profile(false_key);

-- idx_false_strong_pos_num
CREATE INDEX idx_false_strong_pos_num ON false_strong_profile(position, number);

-- idx_false_strong_state
CREATE INDEX idx_false_strong_state ON false_strong_profile(state_key);

-- idx_false_strong_status
CREATE INDEX idx_false_strong_status ON false_strong_profile(status);

-- idx_false_strong_updated
CREATE INDEX idx_false_strong_updated ON false_strong_profile(updated_at);

-- idx_fix_plan_priority
CREATE INDEX idx_fix_plan_priority ON diagnosis_fix_plan(priority,file_name);

-- idx_fix_plan_update
CREATE INDEX idx_fix_plan_update ON diagnosis_fix_plan(updated_at);

-- idx_freeze_matrix_level
CREATE INDEX idx_freeze_matrix_level ON dynamic_freeze_matrix(freeze_level,frozen);

-- idx_freeze_matrix_type
CREATE INDEX idx_freeze_matrix_type ON dynamic_freeze_matrix(freeze_type,freeze_value);

-- idx_freeze_matrix_until
CREATE INDEX idx_freeze_matrix_until ON dynamic_freeze_matrix(freeze_until_time);

-- idx_learning_case_key
CREATE INDEX idx_learning_case_key ON learning_case_log(case_key);

-- idx_learning_case_reason_fp
CREATE INDEX idx_learning_case_reason_fp ON learning_case_log(pre_draw_reason_fingerprint);

-- idx_learning_case_segment
CREATE INDEX idx_learning_case_segment ON learning_case_log(segment_key);

-- idx_learning_case_state
CREATE INDEX idx_learning_case_state ON learning_case_log(state_key);

-- idx_learning_case_target
CREATE INDEX idx_learning_case_target ON learning_case_log(target_issue);

-- idx_learning_case_task
CREATE INDEX idx_learning_case_task ON learning_case_log(task_key);

-- idx_learning_snapshot_created
CREATE INDEX idx_learning_snapshot_created ON learning_snapshot(created_at);

-- idx_learning_snapshot_profile
CREATE INDEX idx_learning_snapshot_profile ON learning_snapshot(profile_key);

-- idx_long_snapshot_issue_r80
CREATE INDEX idx_long_snapshot_issue_r80 ON research_long_snapshot_r80(through_issue,epoch_hash);

-- idx_long_text_expiry_r815
CREATE INDEX idx_long_text_expiry_r815 ON v57_long_text_r815(expires_at);

-- idx_manual_task_started
CREATE INDEX idx_manual_task_started ON manual_backtest_task(started_at);

-- idx_manual_task_status
CREATE INDEX idx_manual_task_status ON manual_backtest_task(status);

-- idx_mccs_adaptive
CREATE INDEX idx_mccs_adaptive ON main_chain_candidate_snapshot(adaptive_input_flag);

-- idx_mccs_candidate
CREATE INDEX idx_mccs_candidate ON main_chain_candidate_snapshot(candidate_key);

-- idx_mccs_reject
CREATE INDEX idx_mccs_reject ON main_chain_candidate_snapshot(reject_code);

-- idx_mccs_target_rank
CREATE INDEX idx_mccs_target_rank ON main_chain_candidate_snapshot(target_issue, realtime_top40_rank);

-- idx_mcrb_code
CREATE INDEX idx_mcrb_code ON main_chain_reject_bucket(reject_code);

-- idx_miss_cluster
CREATE INDEX idx_miss_cluster ON miss_reason_sample(cluster_id);

-- idx_miss_target
CREATE INDEX idx_miss_target ON miss_reason_sample(target_issue);

-- idx_miss_type
CREATE INDEX idx_miss_type ON miss_reason_sample(miss_type);

-- idx_module_effect_name
CREATE INDEX idx_module_effect_name ON module_effect_cache(module_name,effect_status);

-- idx_module_effect_update
CREATE INDEX idx_module_effect_update ON module_effect_cache(updated_at);

-- idx_nsms_pos_k
CREATE INDEX idx_nsms_pos_k ON need_supply_match_sample(position, k3_mask);

-- idx_nsms_score
CREATE INDEX idx_nsms_score ON need_supply_match_sample(match_score);

-- idx_nsms_target
CREATE INDEX idx_nsms_target ON need_supply_match_sample(target_issue);

-- idx_nss_label
CREATE INDEX idx_nss_label ON number_supply_sample(label);

-- idx_nss_supply_type
CREATE INDEX idx_nss_supply_type ON number_supply_sample(main_supply_type);

-- idx_nss_target_pos_num
CREATE INDEX idx_nss_target_pos_num ON number_supply_sample(target_issue, position, number);

-- idx_oos_target_settled
CREATE INDEX idx_oos_target_settled ON oos_action_log_v28(target_issue,settled,processed);

-- idx_pit_base
CREATE INDEX idx_pit_base ON point_in_time_snapshot(base_issue);

-- idx_pit_snapshot_id
CREATE INDEX idx_pit_snapshot_id ON point_in_time_snapshot(snapshot_id);

-- idx_pit_target
CREATE INDEX idx_pit_target ON point_in_time_snapshot(target_issue);

-- idx_pns_need_type
CREATE INDEX idx_pns_need_type ON position_need_sample(main_need_type);

-- idx_pns_target_pos
CREATE INDEX idx_pns_target_pos ON position_need_sample(target_issue, position);

-- idx_prob_audit_bucket
CREATE INDEX idx_prob_audit_bucket ON probability_audit_bucket(bucket_key);

-- idx_prob_audit_status
CREATE INDEX idx_prob_audit_status ON probability_audit_bucket(status);

-- idx_promo_cluster
CREATE INDEX idx_promo_cluster ON promotion_domain_snapshot(cluster_id);

-- idx_promo_main_chain
CREATE INDEX idx_promo_main_chain ON promotion_domain_snapshot(shadow_promotion_status, sample_count);

-- idx_promo_status
CREATE INDEX idx_promo_status ON promotion_domain_snapshot(status);

-- idx_promotion_domain_status_v20
CREATE INDEX idx_promotion_domain_status_v20 ON promotion_domain_stat_v20(status,sample_count);

-- idx_promotion_key
CREATE INDEX idx_promotion_key ON promotion_candidate(promotion_key);

-- idx_promotion_profile
CREATE INDEX idx_promotion_profile ON promotion_candidate(profile_key);

-- idx_promotion_status
CREATE INDEX idx_promotion_status ON promotion_candidate(status);

-- idx_r35_deferred_status
CREATE INDEX idx_r35_deferred_status ON v57_deferred_evidence_job(status,updated_at);

-- idx_r35_health_stage
CREATE INDEX idx_r35_health_stage ON v57_runtime_health_r35(stage,event_at DESC);

-- idx_r35_oos_settled
CREATE INDEX idx_r35_oos_settled ON v57_oos_label_r35(strict_oos,settled_at DESC);

-- idx_r35_shadow_created
CREATE INDEX idx_r35_shadow_created ON v57_shadow_observation_r35(created_at DESC);

-- idx_r35_shadow_settled
CREATE INDEX idx_r35_shadow_settled ON v57_shadow_observation_r35(settled,target_issue);

-- idx_r36_analysis_runnable
CREATE INDEX idx_r36_analysis_runnable ON v57_analysis_request_r36(status,next_attempt_at,lease_until,target_issue);

-- idx_r36_analysis_status
CREATE INDEX idx_r36_analysis_status ON v57_analysis_request_r36(status,lease_until,request_at);

-- idx_r36_settlement_status
CREATE INDEX idx_r36_settlement_status ON v57_settlement_task_r36(status,next_attempt_at,lease_until);

-- idx_r79_legacy_classification_candidate
CREATE INDEX idx_r79_legacy_classification_candidate ON research_legacy_settlement_classification_r79(candidate_key,target_issue,observed_at,receipt_hash);

-- idx_r79_matrix_ready_partition_order
CREATE INDEX idx_r79_matrix_ready_partition_order ON research_matrix_input_cp12(matrix_id,source_ordinal,source_receipt_hash) WHERE state='READY';

-- idx_r80_asset_audit_terminal
CREATE INDEX idx_r80_asset_audit_terminal ON research_legacy_asset_head_audit_r80(status,asset_id);

-- idx_r80_asset_snapshot_order
CREATE INDEX idx_r80_asset_snapshot_order ON research_legacy_asset_head_snapshot_r80(asset_id,generation,settled_ms,target_issue);

-- idx_r80_probe_diagnostic_label
CREATE INDEX idx_r80_probe_diagnostic_label ON r80_probe_diagnostic_ref(label_hash);

-- idx_r80_probe_input_hash
CREATE INDEX idx_r80_probe_input_hash ON r80_probe_forecast(input_hash);

-- idx_r80_probe_label_horizon
CREATE INDEX idx_r80_probe_label_horizon ON r80_probe_label(horizon,status,target_issue);

-- idx_r80_probe_label_target
CREATE INDEX idx_r80_probe_label_target ON r80_probe_label(target_issue,status,completed_at);

-- idx_r80_probe_target
CREATE INDEX idx_r80_probe_target ON r80_probe_forecast(target_issue,horizon);

-- idx_r811_action_label_target
CREATE INDEX idx_r811_action_label_target ON v57_research_action_label_r811(target_issue,global_ordinal);

-- idx_r811_candidate_outcome_issue
CREATE INDEX idx_r811_candidate_outcome_issue ON v57_research_candidate_outcome_r811(candidate_id,target_issue);

-- idx_r811_deferred_runnable
CREATE INDEX idx_r811_deferred_runnable ON v57_deferred_evidence_job(status,next_attempt_at,lease_until,updated_at);

-- idx_r811_draw_provenance_issue
CREATE INDEX idx_r811_draw_provenance_issue ON v57_draw_provenance_r811(parsed_issue,observed_at,source_line);

-- idx_r811_research_artifact_trained
CREATE INDEX idx_r811_research_artifact_trained ON v57_research_artifact_r811(artifact_type,trained_until_issue);

-- idx_r811_research_candidate_status
CREATE INDEX idx_r811_research_candidate_status ON v57_research_candidate_r811(status,updated_at);

-- idx_r811_research_experiment_candidate
CREATE INDEX idx_r811_research_experiment_candidate ON v57_research_experiment_r811(candidate_id,split_role,start_issue);

-- idx_r811_settlement_runnable
CREATE INDEX idx_r811_settlement_runnable ON v57_settlement_task_r36(priority DESC,next_attempt_at,created_at);

-- idx_r817_research_predraw_target
CREATE INDEX idx_r817_research_predraw_target ON v57_research_predraw_r817(target_issue,hit_label,frozen_at);

-- idx_r900_candidate_score_state
CREATE INDEX idx_r900_candidate_score_state ON v57_research_candidate_score_r900(state,updated_at);

-- idx_r900_prune_created
CREATE INDEX idx_r900_prune_created ON v57_research_prune_log_r900(created_at);

-- idx_r906_asset_signature
CREATE INDEX idx_r906_asset_signature ON v57_research_asset_r906(method_signature,created_at);

-- idx_r906_asset_tier
CREATE INDEX idx_r906_asset_tier ON v57_research_asset_r906(tier,runtime_executable,updated_at);

-- idx_r906_certificate_active
CREATE INDEX idx_r906_certificate_active ON v57_research_certificate_r906(status,valid_from_issue,valid_until_issue);

-- idx_r906_closure_status
CREATE INDEX idx_r906_closure_status ON v57_issue_closure_r906(core_status,target_issue);

-- idx_r906_compact_label_issue
CREATE INDEX idx_r906_compact_label_issue ON v57_research_compact_label_r906(target_issue,asset_id);

-- idx_r906_outbox_runnable
CREATE INDEX idx_r906_outbox_runnable ON v57_settlement_outbox_r906(status,priority DESC,next_attempt_at,created_at);

-- idx_r908_algorithm_predraw_issue
CREATE INDEX idx_r908_algorithm_predraw_issue ON v57_algorithm_predraw_r908(target_issue,hit_label,genome_id);

-- idx_r908_algorithm_status
CREATE INDEX idx_r908_algorithm_status ON v57_algorithm_genome_r908(status,generation,updated_at);

-- idx_r914_algorithm_future
CREATE INDEX idx_r914_algorithm_future ON v57_algorithm_genome_r908(status,future_wilson_lower DESC,future_n DESC,updated_at);

-- idx_r915_asset_certificate
CREATE INDEX idx_r915_asset_certificate ON research_new_asset_certificate_v41(status,wilson_lower DESC,paired_delta DESC,future_n DESC);

-- idx_r915_asset_freeze_pending
CREATE INDEX idx_r915_asset_freeze_pending ON research_new_asset_freeze_v41(target_issue,hit_label,asset_id);

-- idx_r915_asset_lifecycle
CREATE INDEX idx_r915_asset_lifecycle ON research_new_asset_genome_v41(status,future_wilson_lower DESC,paired_mainline_delta DESC,updated_ms);

-- idx_r915_atom_proposal_name
CREATE UNIQUE INDEX idx_r915_atom_proposal_name ON research_new_atom_proposal_v41(proposed_name);

-- idx_r915_atom_proposal_state
CREATE INDEX idx_r915_atom_proposal_state ON research_new_atom_proposal_v41(status,created_ms DESC);

-- idx_r915_contribution_issue
CREATE INDEX idx_r915_contribution_issue ON research_new_contribution_v41(target_issue,status,created_ms);

-- idx_r915_evolution_type
CREATE INDEX idx_r915_evolution_type ON research_evolution_event_v41(entity_type,created_ms DESC);

-- idx_r919_legacy_post_status
CREATE INDEX idx_r919_legacy_post_status ON v57_legacy_postsettlement_context_r919(status,settled_at DESC,target_issue DESC);

-- idx_r922_contribution_lane
CREATE INDEX idx_r922_contribution_lane ON v57_research_mainline_contribution_r922(runtime_version,lane,applied,created_at DESC);

-- idx_r922_opportunity_reject
CREATE INDEX idx_r922_opportunity_reject ON v57_controlled_opportunity_r922(runtime_version,gate_code,authorized_action);

-- idx_r922_opportunity_window
CREATE INDEX idx_r922_opportunity_window ON v57_controlled_opportunity_r922(runtime_version,created_at DESC,target_issue DESC);

-- idx_r925_capture_genome
CREATE INDEX idx_r925_capture_genome ON v57_capture_opportunity_r925(genome_id,target_issue);

-- idx_r925_fetch_issue_available
CREATE INDEX idx_r925_fetch_issue_available ON v57_fetch_ledger_r925(latest_issue,first_available_at);

-- idx_r925_fetch_time
CREATE INDEX idx_r925_fetch_time ON v57_fetch_ledger_r925(request_at);

-- idx_r926_event_attempt
CREATE INDEX idx_r926_event_attempt ON v57_runtime_event_r926(attempt_id,seq);

-- idx_r926_event_issue
CREATE INDEX idx_r926_event_issue ON v57_runtime_event_r926(game_code,target_issue,seq);

-- idx_r926_event_outbox
CREATE INDEX idx_r926_event_outbox ON v57_runtime_event_outbox_r926(status,created_at);

-- idx_r926_migration_status
CREATE INDEX idx_r926_migration_status ON v57_schema_migration_journal_r926(domain,revision,status,started_at);

-- idx_r927_resource_attempt
CREATE INDEX idx_r927_resource_attempt ON v57_attempt_resource_r927(attempt_id,started_at);

-- idx_r928_resource_release_time
CREATE INDEX idx_r928_resource_release_time ON v57_attempt_resource_r927(release_code,started_at DESC);

-- idx_r928_resource_status
CREATE INDEX idx_r928_resource_status ON v57_attempt_resource_r927(status,process_id);

-- idx_r929_bundle_one_active
CREATE UNIQUE INDEX idx_r929_bundle_one_active ON research_model_bundle_r929(state) WHERE state='ACTIVE';

-- idx_r929_cert_lookup
CREATE INDEX idx_r929_cert_lookup ON research_certificate_r929(consumer,candidate_id,state,valid_from_issue,valid_until_issue);

-- idx_r929_contribution_issue
CREATE INDEX idx_r929_contribution_issue ON research_mainline_contribution_r929(target_issue,status,created_at);

-- idx_r929_trial_family
CREATE INDEX idx_r929_trial_family ON research_trial_registry_r929(family_id,status,registered_at);

-- idx_r930_atom_value_issue
CREATE INDEX idx_r930_atom_value_issue ON v57_batch34_atom_value_r930(target_issue,category,module_state);

-- idx_r930_capture_transition_issue
CREATE INDEX idx_r930_capture_transition_issue ON v57_batch34_capture_transition_r930(game_code,target_issue,seq);

-- idx_r930_evidence_issue
CREATE INDEX idx_r930_evidence_issue ON v57_batch34_evidence_link_r930(game_code,target_issue,evidence_type);

-- idx_r930_lifecycle_event_algorithm
CREATE INDEX idx_r930_lifecycle_event_algorithm ON v57_batch34_lifecycle_event_r930(algorithm_id,seq);

-- idx_r930_window_state_range
CREATE INDEX idx_r930_window_state_range ON v57_batch34_paired_window_r930(state,first_issue,last_issue);

-- idx_r932_cal_pending
CREATE INDEX idx_r932_cal_pending ON research_candidate_cal_observation_r932(hit,target_issue);

-- idx_r932_cal_ready
CREATE INDEX idx_r932_cal_ready ON research_candidate_cal_study_r932(state,first_issue);

-- idx_r932_capture_candidate
CREATE INDEX idx_r932_capture_candidate ON research_capture_opportunity_r932(candidate_key,target_issue);

-- idx_r932_challenge_endpoint
CREATE INDEX idx_r932_challenge_endpoint ON research_paired_challenge_r932(state,last_issue);

-- idx_r932_creation_groups
CREATE INDEX idx_r932_creation_groups ON research_repair_group_r932(lane,bundle_hash,source_hash,last_issue);

-- idx_r932_creation_quota
CREATE INDEX idx_r932_creation_quota ON research_creation_attempt_r932(lane,kind,created_issue);

-- idx_r932_execution_window
CREATE INDEX idx_r932_execution_window ON research_candidate_execution_r932(candidate_key,structural_hash,target_issue);

-- idx_r932_lifecycle_review
CREATE INDEX idx_r932_lifecycle_review ON research_lifecycle_r932(reviewed_at,candidate_key);

-- idx_r932_module_summary
CREATE INDEX idx_r932_module_summary ON research_module_ablation_r932(code_hash,module,state,target_issue);

-- idx_r932_pair_pending
CREATE INDEX idx_r932_pair_pending ON research_fixed_pair_r932(candidate_hit,target_issue);

-- idx_r932_publication_pending
CREATE INDEX idx_r932_publication_pending ON research_window_publication_r932(state,window_id);

-- idx_r932_schedule_candidate
CREATE INDEX idx_r932_schedule_candidate ON research_schedule_task_r932(candidate_key,selected,target_issue);

-- idx_r932_semantic_alias
CREATE INDEX idx_r932_semantic_alias ON research_semantic_identity_r932(semantic_hash,candidate_key);

-- idx_r932_window_endpoint
CREATE INDEX idx_r932_window_endpoint ON research_fixed_window_r932(state,last_issue);

-- idx_r932_window_generation
CREATE INDEX idx_r932_window_generation ON research_window_generation_r932(candidate_key,generation_id,stage_no);

-- idx_r943_atomic_pending
CREATE INDEX idx_r943_atomic_pending ON atomic_tool_context_r943(hit,target_issue);

-- idx_r947_molecule_state
CREATE INDEX idx_r947_molecule_state ON molecular_tool_proposal_r947(state,created_issue,created_at);

-- idx_r948_integrity_pending
CREATE INDEX idx_r948_integrity_pending ON research_algorithm_head_integrity_r948(status,genome_id);

-- idx_r952_funnel_stage
CREATE INDEX idx_r952_funnel_stage ON r952_candidate_funnel(stage,target_issue);

-- idx_r952_outbox_state
CREATE INDEX idx_r952_outbox_state ON r952_research_outbox(state,updated_at);

-- idx_r958_candidate_training
CREATE INDEX idx_r958_candidate_training ON r952_research_outbox(candidate_id,target_issue,state);

-- idx_r958_frozen_pending
CREATE INDEX idx_r958_frozen_pending ON r958_frozen_predraw(settled,target_issue);

-- idx_r958_pending_settlement
CREATE INDEX idx_r958_pending_settlement ON r952_candidate_predraw(settled,target_issue);

-- idx_r958_world_semantic
CREATE UNIQUE INDEX idx_r958_world_semantic ON r952_world_molecule(resolved_semantic_hash) WHERE resolved_semantic_hash<>'';

-- idx_r966_creation_parent
CREATE INDEX idx_r966_creation_parent ON research_creation_attempt_r932(parent_key,kind,state,created_issue);

-- idx_r967_input_pin
CREATE INDEX idx_r967_input_pin ON v57_research_input_pin_r967(source_table,source_issue,task_id);

-- idx_r967_one_active_lane
CREATE UNIQUE INDEX idx_r967_one_active_lane ON v57_research_task_r967(kind,candidate_id) WHERE kind='MAINTENANCE' AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL');

-- idx_r967_research_structure
CREATE INDEX idx_r967_research_structure ON research_identity_r967(structure_hash,family);

-- idx_r967_task_order
CREATE INDEX idx_r967_task_order ON v57_research_task_r967(kind,candidate_id,target_issue,status);

-- idx_r967_task_ready
CREATE INDEX idx_r967_task_ready ON v57_research_task_r967(kind,status,available_at,updated_at);

-- idx_r969_eval_learner
CREATE INDEX idx_r969_eval_learner ON r967_frozen_evaluation_v2(learner_id,created_issue);

-- idx_r969_run_learner
CREATE INDEX idx_r969_run_learner ON r967_numeric_training_run(learner_id,first_issue);

-- idx_r970_history_original
CREATE INDEX idx_r970_history_original ON research_historical_genome_archive_r967(original_id,program_hash);

-- idx_r970_history_queue
CREATE INDEX idx_r970_history_queue ON research_historical_genome_archive_r967(source_hash,verdict,row_number);

-- idx_r974_compute_issue
CREATE INDEX idx_r974_compute_issue ON r974_computation_receipt(target_issue);

-- idx_r975_evolution_active
CREATE UNIQUE INDEX idx_r975_evolution_active ON r975_evolution_review(parent_id) WHERE state IN ('ACTIVE','READY');

-- idx_r975_evolution_work
CREATE INDEX idx_r975_evolution_work ON r975_evolution_review(state,updated_at);

-- idx_r975_post_authority
CREATE INDEX idx_r975_post_authority ON v57_research_task_r967(kind,authority_ref);

-- idx_r975_task_terminal_block
CREATE INDEX idx_r975_task_terminal_block ON v57_research_task_r967(kind,candidate_id,target_issue) WHERE status IN ('EXPIRED','FAILED_TERMINAL');

-- idx_r975_task_unresolved
CREATE INDEX idx_r975_task_unresolved ON v57_research_task_r967(kind,candidate_id,target_issue) WHERE status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL');

-- idx_r979_outbox_candidate_issue
CREATE INDEX idx_r979_outbox_candidate_issue ON r952_research_outbox(candidate_id,target_issue);

-- idx_r979_outbox_ready_order
CREATE INDEX idx_r979_outbox_ready_order ON r952_research_outbox(updated_at,target_issue,candidate_id) WHERE state NOT IN ('COMPLETED','DEPENDENCY_RETIRED');

-- idx_r979_outbox_unresolved
CREATE INDEX idx_r979_outbox_unresolved ON r952_research_outbox(candidate_id,target_issue) WHERE state NOT IN ('COMPLETED','DEPENDENCY_RETIRED');

-- idx_r979_predraw_candidate_issue
CREATE INDEX idx_r979_predraw_candidate_issue ON r952_candidate_predraw(candidate_id,target_issue);

-- idx_r979_predraw_legacy
CREATE INDEX idx_r979_predraw_legacy ON r952_candidate_predraw(evidence_role,settled,proof_error,target_issue,candidate_id);

-- idx_r980_cal_unsettled_page
CREATE INDEX idx_r980_cal_unsettled_page ON research_candidate_cal_observation_r932(target_issue,study_hash) WHERE hit=-1;

-- idx_realtime_validate_created
CREATE INDEX idx_realtime_validate_created ON realtime_validate(created_at);

-- idx_realtime_validate_target
CREATE INDEX idx_realtime_validate_target ON realtime_validate(target_issue);

-- idx_reason_causal_atom
CREATE INDEX idx_reason_causal_atom ON reason_causal_profile(reason_atom);

-- idx_reason_causal_combo
CREATE INDEX idx_reason_causal_combo ON reason_causal_profile(reason_combo_key);

-- idx_reason_causal_key
CREATE INDEX idx_reason_causal_key ON reason_causal_profile(causal_key);

-- idx_reason_causal_level
CREATE INDEX idx_reason_causal_level ON reason_causal_profile(valid_level);

-- idx_reason_causal_updated
CREATE INDEX idx_reason_causal_updated ON reason_causal_profile(updated_at);

-- idx_reason_contrast_fp
CREATE INDEX idx_reason_contrast_fp ON reason_contrast_profile(reason_fingerprint);

-- idx_reason_contrast_key
CREATE INDEX idx_reason_contrast_key ON reason_contrast_profile(profile_key);

-- idx_reason_contrast_level
CREATE INDEX idx_reason_contrast_level ON reason_contrast_profile(valid_level);

-- idx_reason_contrast_pos_num
CREATE INDEX idx_reason_contrast_pos_num ON reason_contrast_profile(position, number);

-- idx_reason_contrast_state
CREATE INDEX idx_reason_contrast_state ON reason_contrast_profile(state_key);

-- idx_reason_contrast_updated
CREATE INDEX idx_reason_contrast_updated ON reason_contrast_profile(updated_at);

-- idx_reason_sample_created
CREATE INDEX idx_reason_sample_created ON pre_draw_reason_sample(created_at);

-- idx_reason_sample_fp
CREATE INDEX idx_reason_sample_fp ON pre_draw_reason_sample(reason_fingerprint);

-- idx_reason_sample_key
CREATE INDEX idx_reason_sample_key ON pre_draw_reason_sample(sample_key);

-- idx_reason_sample_label
CREATE INDEX idx_reason_sample_label ON pre_draw_reason_sample(label);

-- idx_reason_sample_pos_num
CREATE INDEX idx_reason_sample_pos_num ON pre_draw_reason_sample(position, number);

-- idx_reason_sample_state
CREATE INDEX idx_reason_sample_state ON pre_draw_reason_sample(state_key);

-- idx_reason_sample_target
CREATE INDEX idx_reason_sample_target ON pre_draw_reason_sample(target_issue);

-- idx_research_action_proposal_parent_life_cp12
CREATE INDEX idx_research_action_proposal_parent_life_cp12 ON research_action_proposal_cp12(parent_lifetime_id,action_id);

-- idx_research_action_wait_cp12
CREATE INDEX idx_research_action_wait_cp12 ON research_action_v2(state,action_id) WHERE child_id IS NOT NULL;

-- idx_research_eval_target_r815
CREATE INDEX idx_research_eval_target_r815 ON v57_research_eval_r815(target_issue,candidate_id);

-- idx_research_feedback_rejected_life_cp12
CREATE INDEX idx_research_feedback_rejected_life_cp12 ON research_feedback_rejection_cp12(lifetime_id,target_issue);

-- idx_research_finding_candidate_cp12
CREATE INDEX idx_research_finding_candidate_cp12 ON research_finding_v2(candidate_version,problem_type,source_issue);

-- idx_research_formal_candidate_cp12
CREATE INDEX idx_research_formal_candidate_cp12 ON research_formal_observation_cp12(lifetime_id,target_issue DESC);

-- idx_research_formal_learner_cp12
CREATE INDEX idx_research_formal_learner_cp12 ON research_formal_observation_cp12(learner_id,target_issue,lifetime_id);

-- idx_research_formal_target_cp12
CREATE INDEX idx_research_formal_target_cp12 ON research_formal_observation_cp12(target_issue,position,lifetime_id);

-- idx_research_pair_source_cp12
CREATE INDEX idx_research_pair_source_cp12 ON research_fixed_pair_r932(candidate_hash,window_id,target_issue);

-- idx_research_queue_runnable_r815
CREATE INDEX idx_research_queue_runnable_r815 ON v57_research_queue_r815(state,next_attempt_at,created_at);

-- idx_runtime_profile_key
CREATE INDEX idx_runtime_profile_key ON runtime_profile_cache(profile_key);

-- idx_runtime_profile_pos_mask
CREATE INDEX idx_runtime_profile_pos_mask ON runtime_profile_cache(position, k3_mask);

-- idx_runtime_profile_state
CREATE INDEX idx_runtime_profile_state ON runtime_profile_cache(state_key);

-- idx_runtime_profile_updated
CREATE INDEX idx_runtime_profile_updated ON runtime_profile_cache(updated_at);

-- idx_service_recovery_event_at_v20
CREATE INDEX idx_service_recovery_event_at_v20 ON service_recovery_event_v20(event_at);

-- idx_shadow_adaptive_action
CREATE INDEX idx_shadow_adaptive_action ON shadow_candidates(adaptive_judge_action);

-- idx_shadow_dynamic_cluster
CREATE INDEX idx_shadow_dynamic_cluster ON shadow_candidates(dynamic_cluster_id);

-- idx_shadow_dynamic_status
CREATE INDEX idx_shadow_dynamic_status ON shadow_candidates(dynamic_cluster_status);

-- idx_shadow_elite45
CREATE INDEX idx_shadow_elite45 ON shadow_candidates(elite45_passed, elite45_status);

-- idx_shadow_learned
CREATE INDEX idx_shadow_learned ON shadow_candidates(learned);

-- idx_shadow_main_chain
CREATE INDEX idx_shadow_main_chain ON shadow_candidates(target_issue, rank_no, reject_code);

-- idx_shadow_need_supply
CREATE INDEX idx_shadow_need_supply ON shadow_candidates(need_supply_match_score);

-- idx_shadow_profile
CREATE INDEX idx_shadow_profile ON shadow_candidates(deep_profile_key);

-- idx_shadow_promotion_domain
CREATE INDEX idx_shadow_promotion_domain ON shadow_promotion_cache(domain_type,domain_key);

-- idx_shadow_promotion_status
CREATE INDEX idx_shadow_promotion_status ON shadow_candidates(shadow_promotion_status);

-- idx_shadow_rank
CREATE INDEX idx_shadow_rank ON shadow_candidates(target_issue, rank_no);

-- idx_shadow_reason_fp
CREATE INDEX idx_shadow_reason_fp ON shadow_candidates(pre_draw_reason_fingerprint);

-- idx_shadow_state
CREATE INDEX idx_shadow_state ON shadow_candidates(state_key);

-- idx_shadow_target
CREATE INDEX idx_shadow_target ON shadow_candidates(target_issue);

-- idx_shadow_tm_profile
CREATE INDEX idx_shadow_tm_profile ON shadow_candidates(teacher_matrix_profile_key);

-- idx_shape_memory_active
CREATE INDEX idx_shape_memory_active ON shape_memory(active_flag);

-- idx_shape_memory_key
CREATE INDEX idx_shape_memory_key ON shape_memory(shape_key);

-- idx_shape_vector_created
CREATE INDEX idx_shape_vector_created ON shape_vector(created_at);

-- idx_shape_vector_issue_pos
CREATE INDEX idx_shape_vector_issue_pos ON shape_vector(base_issue, position);

-- idx_shape_vector_reason_fp
CREATE INDEX idx_shape_vector_reason_fp ON shape_vector(reason_fingerprint);

-- idx_shape_vector_shape_key
CREATE INDEX idx_shape_vector_shape_key ON shape_vector(shape_key);

-- idx_shape_vector_state_key
CREATE INDEX idx_shape_vector_state_key ON shape_vector(state_key);

-- idx_shape_vector_target
CREATE INDEX idx_shape_vector_target ON shape_vector(target_issue);

-- idx_state_number_key
CREATE INDEX idx_state_number_key ON state_number_profile(profile_key);

-- idx_state_number_pos_num
CREATE INDEX idx_state_number_pos_num ON state_number_profile(position, number);

-- idx_state_number_state
CREATE INDEX idx_state_number_state ON state_number_profile(state_key);

-- idx_state_number_updated
CREATE INDEX idx_state_number_updated ON state_number_profile(updated_at);

-- idx_store_v20_layer_issue
CREATE INDEX idx_store_v20_layer_issue ON v20_layer_snapshot(layer,as_of_issue);

-- idx_store_v20_shadow_domain
CREATE INDEX idx_store_v20_shadow_domain ON v20_shadow_domain_stat(promotion_status,sample_count);

-- idx_ta_profile_pos_mask
CREATE INDEX idx_ta_profile_pos_mask ON teacher_assist_profile(position, k3_mask);

-- idx_ta_profile_state
CREATE INDEX idx_ta_profile_state ON teacher_assist_profile(state_key);

-- idx_teacher_domain_status_v20
CREATE INDEX idx_teacher_domain_status_v20 ON teacher_domain_stat_v20(status,sample_count);

-- idx_teacher_snapshot_created
CREATE INDEX idx_teacher_snapshot_created ON teacher_snapshot(created_at);

-- idx_teacher_snapshot_status
CREATE INDEX idx_teacher_snapshot_status ON teacher_snapshot(status);

-- idx_tm_combo_key
CREATE INDEX idx_tm_combo_key ON teacher_matrix_combo_profile(combo_key);

-- idx_tm_combo_pos_mask
CREATE INDEX idx_tm_combo_pos_mask ON teacher_matrix_combo_profile(position, k3_mask);

-- idx_tm_combo_state
CREATE INDEX idx_tm_combo_state ON teacher_matrix_combo_profile(state_key);

-- idx_tm_combo_status
CREATE INDEX idx_tm_combo_status ON teacher_matrix_combo_profile(status);

-- idx_tm_profile_key
CREATE INDEX idx_tm_profile_key ON teacher_matrix_profile(profile_key);

-- idx_tm_profile_pos_num
CREATE INDEX idx_tm_profile_pos_num ON teacher_matrix_profile(position, number);

-- idx_tm_profile_state
CREATE INDEX idx_tm_profile_state ON teacher_matrix_profile(state_key);

-- idx_tm_profile_status
CREATE INDEX idx_tm_profile_status ON teacher_matrix_profile(status);

-- idx_tm_profile_updated
CREATE INDEX idx_tm_profile_updated ON teacher_matrix_profile(updated_at);

-- idx_tm_report_created
CREATE INDEX idx_tm_report_created ON teacher_matrix_report(created_at);

-- idx_tm_report_segment
CREATE INDEX idx_tm_report_segment ON teacher_matrix_report(segment_key);

-- idx_tm_report_status
CREATE INDEX idx_tm_report_status ON teacher_matrix_report(status);

-- idx_tm_report_task
CREATE INDEX idx_tm_report_task ON teacher_matrix_report(task_key);

-- idx_tm_sample_base
CREATE INDEX idx_tm_sample_base ON teacher_matrix_sample(base_issue);

-- idx_tm_sample_combo
CREATE INDEX idx_tm_sample_combo ON teacher_matrix_sample(combo_key);

-- idx_tm_sample_created
CREATE INDEX idx_tm_sample_created ON teacher_matrix_sample(created_at);

-- idx_tm_sample_key
CREATE INDEX idx_tm_sample_key ON teacher_matrix_sample(sample_key);

-- idx_tm_sample_label
CREATE INDEX idx_tm_sample_label ON teacher_matrix_sample(label);

-- idx_tm_sample_pos_num
CREATE INDEX idx_tm_sample_pos_num ON teacher_matrix_sample(position, number);

-- idx_tm_sample_profile
CREATE INDEX idx_tm_sample_profile ON teacher_matrix_sample(profile_key);

-- idx_tm_sample_reason_fp
CREATE INDEX idx_tm_sample_reason_fp ON teacher_matrix_sample(reason_fingerprint);

-- idx_tm_sample_state
CREATE INDEX idx_tm_sample_state ON teacher_matrix_sample(state_key);

-- idx_tm_sample_target
CREATE INDEX idx_tm_sample_target ON teacher_matrix_sample(target_issue);

-- idx_tm_state_key
CREATE INDEX idx_tm_state_key ON teacher_matrix_state_profile(state_key);

-- idx_tm_state_status
CREATE INDEX idx_tm_state_status ON teacher_matrix_state_profile(status);

-- idx_tr_case_created
CREATE INDEX idx_tr_case_created ON teacher_replay_case(created_at);

-- idx_tr_case_state
CREATE INDEX idx_tr_case_state ON teacher_replay_case(state_key);

-- idx_tr_case_target
CREATE INDEX idx_tr_case_target ON teacher_replay_case(target_issue);

-- idx_tr_cf_target
CREATE INDEX idx_tr_cf_target ON teacher_replay_counterfactual(target_issue);

-- idx_tr_cursor_status
CREATE INDEX idx_tr_cursor_status ON teacher_replay_cursor(status);

-- idx_tr_report_created
CREATE INDEX idx_tr_report_created ON teacher_replay_report(created_at);

-- idx_tr_stability_state
CREATE INDEX idx_tr_stability_state ON teacher_replay_stability(state_key);

-- idx_trades_adaptive_action
CREATE INDEX idx_trades_adaptive_action ON trades(adaptive_judge_action);

-- idx_trades_bet_time
CREATE INDEX idx_trades_bet_time ON trades(bet_time);

-- idx_trades_dynamic_cluster
CREATE INDEX idx_trades_dynamic_cluster ON trades(dynamic_cluster_id);

-- idx_trades_dynamic_status
CREATE INDEX idx_trades_dynamic_status ON trades(dynamic_cluster_status);

-- idx_trades_elite45
CREATE INDEX idx_trades_elite45 ON trades(elite45_passed, elite45_status);

-- idx_trades_factor_cluster
CREATE INDEX idx_trades_factor_cluster ON trades(cluster_key, factor_key);

-- idx_trades_main_chain
CREATE INDEX idx_trades_main_chain ON trades(target_issue, action, reject_code, real_probe_allowed);

-- idx_trades_position
CREATE INDEX idx_trades_position ON trades(position);

-- idx_trades_profile
CREATE INDEX idx_trades_profile ON trades(deep_profile_key);

-- idx_trades_real_scope
CREATE INDEX idx_trades_real_scope ON trades(action, stat_scope, source_type, is_realtime_real, settled);

-- idx_trades_reason_fp
CREATE INDEX idx_trades_reason_fp ON trades(pre_draw_reason_fingerprint);

-- idx_trades_result
CREATE INDEX idx_trades_result ON trades(result);

-- idx_trades_scope
CREATE INDEX idx_trades_scope ON trades(stat_scope);

-- idx_trades_settle_time
CREATE INDEX idx_trades_settle_time ON trades(settle_time);

-- idx_trades_settled
CREATE INDEX idx_trades_settled ON trades(settled);

-- idx_trades_target
CREATE INDEX idx_trades_target ON trades(target_issue);

-- idx_trades_tm_profile
CREATE INDEX idx_trades_tm_profile ON trades(teacher_matrix_profile_key);

-- idx_trades_v20_grade
CREATE INDEX idx_trades_v20_grade ON trades(v20_grade);

-- idx_trades_v20_same_class
CREATE INDEX idx_trades_v20_same_class ON trades(v20_same_class_key);

-- idx_transition_chain
CREATE INDEX idx_transition_chain ON deep_transition(chain_type);

-- idx_transition_key
CREATE INDEX idx_transition_key ON deep_transition(transition_key);

-- idx_transition_pos_from_to
CREATE INDEX idx_transition_pos_from_to ON deep_transition(position, from_number, to_number);

-- idx_ui_text_key
CREATE INDEX idx_ui_text_key ON ui_text_state(text_key);

-- idx_v20_action_candidate
CREATE INDEX idx_v20_action_candidate ON v20_action_matrix(candidate_key);

-- idx_v20_action_target
CREATE INDEX idx_v20_action_target ON v20_action_matrix(target_issue);

-- idx_v20_audit_candidate
CREATE INDEX idx_v20_audit_candidate ON candidate_audit(candidate_key);

-- idx_v20_audit_target
CREATE INDEX idx_v20_audit_target ON candidate_audit(target_issue);

-- idx_v20_background_cursor_status
CREATE INDEX idx_v20_background_cursor_status ON v20_background_cursor(status,updated_at);

-- idx_v20_causal_atom
CREATE INDEX idx_v20_causal_atom ON causal_factor_matrix(reason_atom);

-- idx_v20_causal_state
CREATE INDEX idx_v20_causal_state ON causal_factor_matrix(state_key);

-- idx_v20_chain_candidate
CREATE INDEX idx_v20_chain_candidate ON dynamic_cause_chain(candidate_key);

-- idx_v20_chain_grade
CREATE INDEX idx_v20_chain_grade ON dynamic_cause_chain(grade);

-- idx_v20_chain_target
CREATE INDEX idx_v20_chain_target ON dynamic_cause_chain(target_issue);

-- idx_v20_cm_atom
CREATE INDEX idx_v20_cm_atom ON v20_cause_matrix(reason_atom);

-- idx_v20_cm_state_pos
CREATE INDEX idx_v20_cm_state_pos ON v20_cause_matrix(state_key, position);

-- idx_v20_decision_audit_action
CREATE INDEX idx_v20_decision_audit_action ON decision_audit(action);

-- idx_v20_decision_audit_target
CREATE INDEX idx_v20_decision_audit_target ON decision_audit(target_issue);

-- idx_v20_false2_same_class
CREATE INDEX idx_v20_false2_same_class ON v20_false_strong(same_class_key, is_frozen);

-- idx_v20_feature_state
CREATE INDEX idx_v20_feature_state ON pre_draw_feature(state_key);

-- idx_v20_feature_target
CREATE INDEX idx_v20_feature_target ON pre_draw_feature(target_issue);

-- idx_v20_freeze_active
CREATE INDEX idx_v20_freeze_active ON false_strong_freeze(is_frozen);

-- idx_v20_freeze_same_class
CREATE INDEX idx_v20_freeze_same_class ON false_strong_freeze(same_class_key);

-- idx_v20_freeze_state
CREATE INDEX idx_v20_freeze_state ON false_strong_freeze(state_key);

-- idx_v20_guard_state_pos
CREATE INDEX idx_v20_guard_state_pos ON third_guard_matrix(state_key, position);

-- idx_v20_layer_snapshot_layer_issue
CREATE INDEX idx_v20_layer_snapshot_layer_issue ON v20_layer_snapshot(layer,as_of_issue);

-- idx_v20_ledger_real
CREATE INDEX idx_v20_ledger_real ON real_trade_ledger(action, stat_scope, source_type, is_realtime_real, result);

-- idx_v20_ledger_target
CREATE INDEX idx_v20_ledger_target ON real_trade_ledger(target_issue);

-- idx_v20_loop_real
CREATE INDEX idx_v20_loop_real ON v20_closed_loop_snapshot(action, stat_scope, source_type, is_realtime_real, result);

-- idx_v20_loop_target
CREATE INDEX idx_v20_loop_target ON v20_closed_loop_snapshot(target_issue);

-- idx_v20_num_sample_pos_num
CREATE INDEX idx_v20_num_sample_pos_num ON pre_draw_number_sample(position, number);

-- idx_v20_num_sample_target
CREATE INDEX idx_v20_num_sample_target ON pre_draw_number_sample(target_issue);

-- idx_v20_position_state_pos
CREATE INDEX idx_v20_position_state_pos ON position_cause_matrix(state_key, position);

-- idx_v20_rank_state_group
CREATE INDEX idx_v20_rank_state_group ON counterfactual_rank(state_key, rank_group);

-- idx_v20_rank_target_rank
CREATE INDEX idx_v20_rank_target_rank ON counterfactual_rank(target_issue, rank_no);

-- idx_v20_rev_reason
CREATE INDEX idx_v20_rev_reason ON v20_reverse_reason_sample(reason_code);

-- idx_v20_rev_target
CREATE INDEX idx_v20_rev_target ON v20_reverse_reason_sample(target_issue);

-- idx_v20_rule2_active
CREATE INDEX idx_v20_rule2_active ON v20_rule_health(is_active, is_frozen);

-- idx_v20_rule2_state
CREATE INDEX idx_v20_rule2_state ON v20_rule_health(state_key);

-- idx_v20_rule_active
CREATE INDEX idx_v20_rule_active ON rule_health_matrix(is_active, is_frozen);

-- idx_v20_rule_state
CREATE INDEX idx_v20_rule_state ON rule_health_matrix(state_key);

-- idx_v20_scan_same_class
CREATE INDEX idx_v20_scan_same_class ON full_candidate_scan(same_class_key);

-- idx_v20_scan_state
CREATE INDEX idx_v20_scan_state ON full_candidate_scan(state_key);

-- idx_v20_scan_target_rank
CREATE INDEX idx_v20_scan_target_rank ON full_candidate_scan(target_issue, rank_no);

-- idx_v20_seed_state_pos
CREATE INDEX idx_v20_seed_state_pos ON seed_cause_expansion(state_key, position);

-- idx_v20_shadow2_state_grade
CREATE INDEX idx_v20_shadow2_state_grade ON v20_shadow_health(state_key, grade);

-- idx_v20_shadow_domain_status
CREATE INDEX idx_v20_shadow_domain_status ON v20_shadow_domain_stat(promotion_status,sample_count);

-- idx_v20_shadow_grade
CREATE INDEX idx_v20_shadow_grade ON shadow_health_gate(grade);

-- idx_v20_shadow_state_pos
CREATE INDEX idx_v20_shadow_state_pos ON shadow_health_gate(state_key, position);

-- idx_v20_sim_distance
CREATE INDEX idx_v20_sim_distance ON v20_similar_window(state_key, feature_distance);

-- idx_v20_sim_target
CREATE INDEX idx_v20_sim_target ON v20_similar_window(target_issue);

-- idx_v20_source_state
CREATE INDEX idx_v20_source_state ON source_health_matrix(state_key);

-- idx_v20_state_health
CREATE INDEX idx_v20_state_health ON state_health_matrix(state_key);

-- idx_v20_teacher_state_pos_num
CREATE INDEX idx_v20_teacher_state_pos_num ON teacher_number_matrix(state_key, position, number);

-- idx_v20_tn_state_pos_num
CREATE INDEX idx_v20_tn_state_pos_num ON v20_teacher_number(state_key, position, number);

-- idx_v20_tn_status
CREATE INDEX idx_v20_tn_status ON v20_teacher_number(teacher_status);

-- idx_v20_ws_exact
CREATE INDEX idx_v20_ws_exact ON v20_window_state(exact_state_key);

-- idx_v20_ws_relaxed
CREATE INDEX idx_v20_ws_relaxed ON v20_window_state(relaxed_state_key);

-- idx_v20_ws_target_pos_len
CREATE INDEX idx_v20_ws_target_pos_len ON v20_window_state(target_issue, position, window_len);

-- idx_v28_oos_publish_queue
CREATE INDEX idx_v28_oos_publish_queue ON oos_action_log_v28(settled,processed,build_id,cohort_id,pipeline_version,calibration_version,target_issue,rank_slot);

-- idx_v28_oos_reliability
CREATE INDEX idx_v28_oos_reliability ON oos_action_log_v28(settled,calibration_version,raw_rank_score,calibrated_probability,hit);

-- idx_v28_oos_target_candidate
CREATE UNIQUE INDEX idx_v28_oos_target_candidate ON oos_action_log_v28(target_issue,candidate_hash,build_id,cohort_id,schema_version,pipeline_version,calibration_version,version);

-- idx_v48_diag_settled
CREATE INDEX idx_v48_diag_settled ON v48_diagnostic_observation(settled,target_issue);

-- idx_v49_attr_target
CREATE INDEX idx_v49_attr_target ON v49_cause_attribution(target_issue);

-- idx_v49_opportunity_created
CREATE INDEX idx_v49_opportunity_created ON v49_best_opportunity(created_at DESC);

-- idx_v50_action_fingerprint
CREATE INDEX idx_v50_action_fingerprint ON v50_position_action(fingerprint,target_issue);

-- idx_v50_action_issue_rank
CREATE INDEX idx_v50_action_issue_rank ON v50_position_action(target_issue,action_rank);

-- idx_v50_attribution_type
CREATE INDEX idx_v50_attribution_type ON v50_cause_attribution(attribution_type,created_at DESC);

-- idx_v50_auth_issue
CREATE UNIQUE INDEX idx_v50_auth_issue ON v50_authorization_audit(target_issue);

-- idx_v50_domain_parent
CREATE INDEX idx_v50_domain_parent ON v50_domain_certificate(parent_prototype,lifecycle,position_scope);

-- idx_v50_domain_status
CREATE INDEX idx_v50_domain_status ON v50_domain_certificate(status,sample,updated_at DESC);

-- idx_v50_expert_lookup
CREATE INDEX idx_v50_expert_lookup ON v50_expert_regret(expert_id,position,state_family,lifecycle,domain_id,sample);

-- idx_v50_issue_cohort_settled
CREATE INDEX idx_v50_issue_cohort_settled ON v50_issue_prediction(cohort_id,settled,target_issue);

-- idx_v50_issue_created
CREATE INDEX idx_v50_issue_created ON v50_issue_prediction(created_at DESC);

-- idx_v50_ledger_error_issue
CREATE INDEX idx_v50_ledger_error_issue ON v50_ledger_error(target_issue,created_at DESC);

-- idx_v50_migration_created
CREATE INDEX idx_v50_migration_created ON v50_migration_log(created_at DESC);

-- idx_v50_number_domain
CREATE INDEX idx_v50_number_domain ON v50_number_event(state_domain,position,number,settled);

-- idx_v50_number_issue_settled
CREATE INDEX idx_v50_number_issue_settled ON v50_number_event(target_issue,settled);

-- idx_v50_performance_issue
CREATE INDEX idx_v50_performance_issue ON v50_performance_span(target_issue,created_at DESC);

-- idx_v50_performance_owner
CREATE INDEX idx_v50_performance_owner ON v50_performance_span(owner,created_at DESC);

-- idx_v50_residual_position
CREATE INDEX idx_v50_residual_position ON v50_online_residual(position,number,sample);

-- idx_v50_selected_cohort
CREATE INDEX idx_v50_selected_cohort ON v50_selected_decision(strategy_cohort,real_trade,settled,target_issue);

-- idx_v50_selected_lane
CREATE INDEX idx_v50_selected_lane ON v50_selected_decision(lane,decision_action,settled,target_issue);

-- idx_v50_state_domain
CREATE INDEX idx_v50_state_domain ON v50_state_history(domain_id,position,target_issue);

-- idx_v50_state_lifecycle
CREATE INDEX idx_v50_state_lifecycle ON v50_state_history(prototype,lifecycle,position,target_issue);

-- idx_v50_teacher_lookup
CREATE INDEX idx_v50_teacher_lookup ON v50_teacher_profile(state_domain,lifecycle,position,number,sample);

-- idx_v51_action_issue_rank
CREATE INDEX idx_v51_action_issue_rank ON v51_position_action(game_code,target_issue,rank_no);

-- idx_v51_number_issue
CREATE INDEX idx_v51_number_issue ON v51_number_event(game_code,target_issue);

-- idx_v51_selected_lane
CREATE INDEX idx_v51_selected_lane ON v51_selected_action(game_code,lane,decision_action,settled);

-- idx_v57_action_space_rank
CREATE INDEX idx_v57_action_space_rank ON v57_k3_action_space_1200(game_code,target_issue,rank_no);

-- idx_v57_champion_game_issue
CREATE INDEX idx_v57_champion_game_issue ON v57_k3_champion_freeze(game_code,target_issue);

-- idx_v57_funnel_game_issue
CREATE INDEX idx_v57_funnel_game_issue ON v57_k3_funnel_trace(game_code,target_issue,position);

-- idx_wfs_cluster
CREATE INDEX idx_wfs_cluster ON window_feature_sample(cluster_id);

-- idx_wfs_decision
CREATE INDEX idx_wfs_decision ON window_feature_sample(decision_type);

-- idx_wfs_pos_k
CREATE INDEX idx_wfs_pos_k ON window_feature_sample(position, k3_mask);

-- idx_wfs_target
CREATE INDEX idx_wfs_target ON window_feature_sample(target_issue);

-- learning_case_log
CREATE TABLE learning_case_log (_id INTEGER PRIMARY KEY AUTOINCREMENT, case_key TEXT NOT NULL UNIQUE, task_key TEXT, segment_key TEXT, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, target_index INTEGER DEFAULT 0, position INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, predicted_k3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, state_key TEXT, state_mode TEXT, trend_key TEXT, shape_key TEXT, drift_key TEXT, break_key TEXT, recovery_key TEXT, transition_key TEXT, expected_shape TEXT, actual_shape TEXT, why_expected_hit TEXT, why_actual_hit TEXT, why_miss TEXT, next_advice TEXT, main_reason_code TEXT, affected_module TEXT, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, trend_score REAL DEFAULT 0, shape_score REAL DEFAULT 0, drift_risk REAL DEFAULT 0, break_risk REAL DEFAULT 0, recovery_score REAL DEFAULT 0, profile_score REAL DEFAULT 0, number_bias REAL DEFAULT 0, combo_bias REAL DEFAULT 0, risk_score REAL DEFAULT 0, teacher_weight REAL DEFAULT 1, state_confidence REAL DEFAULT 0, state_sample INTEGER DEFAULT 0, cache_sample INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, pre_draw_reason_fingerprint TEXT, pre_draw_reason_text TEXT, reason_valid_level TEXT, reason_score REAL DEFAULT 0, reason_edge REAL DEFAULT 0, hit_similarity REAL DEFAULT 0, miss_similarity REAL DEFAULT 0, contradiction_score REAL DEFAULT 0, causal_lift REAL DEFAULT 0, causal_confidence REAL DEFAULT 0, valid_reason_count INTEGER DEFAULT 0, false_reason_count INTEGER DEFAULT 0, k3_role_summary TEXT, elite45_passed INTEGER DEFAULT 0, elite45_summary TEXT, saved INTEGER DEFAULT 0);

-- learning_snapshot
CREATE TABLE learning_snapshot (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_id TEXT NOT NULL UNIQUE, created_at INTEGER DEFAULT 0, source_type TEXT, target_issue TEXT, profile_key TEXT, hit INTEGER DEFAULT 0, affected_module TEXT, module_action TEXT, module_weight_delta REAL DEFAULT 0, confidence REAL DEFAULT 0, main_reason_code TEXT, summary_text TEXT, total_sample INTEGER DEFAULT 0, total_hit_rate REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, decay_weight REAL DEFAULT 1, fail_count INTEGER DEFAULT 0, stale_flag INTEGER DEFAULT 0, recover_flag INTEGER DEFAULT 0, teacher_matrix_sample_count INTEGER DEFAULT 0, teacher_matrix_hit_rate REAL DEFAULT 0, teacher_matrix_confidence REAL DEFAULT 0, reason_sample_count INTEGER DEFAULT 0, reason_edge REAL DEFAULT 0, reason_confidence REAL DEFAULT 0, profile_summary TEXT, gate_adjust_summary TEXT, probability_adjust_summary TEXT, audit_summary TEXT, bias_summary TEXT, promotion_summary TEXT, a_plus_summary TEXT, teacher_matrix_summary TEXT, reason_summary TEXT, active_json TEXT, learning_summary_text TEXT);

-- long_canonical_changed_r80
CREATE TRIGGER long_canonical_changed_r80 AFTER UPDATE ON v57_draw_canonical_r811 WHEN (NEW.status IS NOT OLD.status OR NEW.canonical_draw_hash IS NOT OLD.canonical_draw_hash) AND EXISTS(SELECT 1 FROM research_long_head_r80 h JOIN research_long_epoch_r80 e ON e.epoch_hash=h.epoch_hash WHERE h.status='ACTIVE' AND OLD.target_issue BETWEEN e.first_issue AND h.through_issue) BEGIN UPDATE research_long_head_r80 SET status='INVALID_SOURCE',last_error='CANONICAL_CHANGED:'||OLD.target_issue,generation=generation+1 WHERE singleton=1; END;

-- long_canonical_insert_r81
CREATE TRIGGER long_canonical_insert_r81 AFTER INSERT ON v57_draw_canonical_r811 WHEN EXISTS(SELECT 1 FROM research_long_head_r80 h JOIN research_long_epoch_r80 e ON e.epoch_hash=h.epoch_hash WHERE h.status='ACTIVE' AND NEW.target_issue BETWEEN e.first_issue AND h.through_issue) BEGIN UPDATE research_long_head_r80 SET status='INVALID_SOURCE',last_error='CANONICAL_INSERT:'||NEW.target_issue,generation=generation+1 WHERE singleton=1; END;

-- long_draw_changed_r80
CREATE TRIGGER long_draw_changed_r80 AFTER UPDATE ON draws WHEN (NEW.issue IS NOT OLD.issue OR NEW.open_time IS NOT OLD.open_time OR NEW.dirty IS NOT OLD.dirty OR NEW.n1 IS NOT OLD.n1 OR NEW.n2 IS NOT OLD.n2 OR NEW.n3 IS NOT OLD.n3 OR NEW.n4 IS NOT OLD.n4 OR NEW.n5 IS NOT OLD.n5 OR NEW.n6 IS NOT OLD.n6 OR NEW.n7 IS NOT OLD.n7 OR NEW.n8 IS NOT OLD.n8 OR NEW.n9 IS NOT OLD.n9 OR NEW.n10 IS NOT OLD.n10) AND EXISTS(SELECT 1 FROM research_long_head_r80 h JOIN research_long_epoch_r80 e ON e.epoch_hash=h.epoch_hash WHERE h.status='ACTIVE' AND OLD.issue BETWEEN e.first_issue AND h.through_issue) BEGIN UPDATE research_long_head_r80 SET status='INVALID_SOURCE',last_error='DRAW_CHANGED:'||OLD.issue,generation=generation+1 WHERE singleton=1; END;

-- long_draw_insert_r81
CREATE TRIGGER long_draw_insert_r81 AFTER INSERT ON draws WHEN EXISTS(SELECT 1 FROM research_long_head_r80 h JOIN research_long_epoch_r80 e ON e.epoch_hash=h.epoch_hash WHERE h.status='ACTIVE' AND NEW.issue BETWEEN e.first_issue AND h.through_issue) BEGIN UPDATE research_long_head_r80 SET status='INVALID_SOURCE',last_error='DRAW_INSERT:'||NEW.issue,generation=generation+1 WHERE singleton=1; END;

-- long_publication_receipt_r80
CREATE TRIGGER long_publication_receipt_r80 BEFORE INSERT ON research_long_publication_r80 WHEN EXISTS(SELECT 1 FROM research_long_publication_r80 WHERE receipt_hash=NEW.receipt_hash) BEGIN SELECT RAISE(ABORT,'LONG_PUBLICATION_RECEIPT_EXISTS'); END;

-- long_publication_time_r80
CREATE TRIGGER long_publication_time_r80 BEFORE INSERT ON research_long_publication_r80 WHEN NOT EXISTS(SELECT 1 FROM research_long_snapshot_r80 s WHERE s.snapshot_hash=NEW.snapshot_hash AND s.prepared_at<=NEW.available_at) BEGIN SELECT RAISE(ABORT,'LONG_PUBLICATION_TIME'); END;

-- long_snapshot_capacity_r80
CREATE TRIGGER long_snapshot_capacity_r80 BEFORE INSERT ON research_long_snapshot_r80 WHEN (SELECT COUNT(*) FROM research_long_snapshot_r80)>=5000 BEGIN SELECT RAISE(ABORT,'LONG_SNAPSHOT_CAPACITY'); END;

-- long_snapshot_key_r80
CREATE TRIGGER long_snapshot_key_r80 BEFORE INSERT ON research_long_snapshot_r80 WHEN EXISTS(SELECT 1 FROM research_long_snapshot_r80 WHERE epoch_hash=NEW.epoch_hash AND through_issue=NEW.through_issue) BEGIN SELECT RAISE(ABORT,'LONG_SOURCE_SNAPSHOT_EXISTS'); END;

-- long_source_token_delete_r80
CREATE TRIGGER long_source_token_delete_r80 AFTER DELETE ON v57_draw_canonical_r811 BEGIN UPDATE research_long_source_token_r80 SET generation=generation+1 WHERE singleton=1; END;

-- long_source_token_insert_r80
CREATE TRIGGER long_source_token_insert_r80 AFTER INSERT ON v57_draw_canonical_r811 BEGIN UPDATE research_long_source_token_r80 SET generation=generation+1 WHERE singleton=1; END;

-- long_source_token_update_r80
CREATE TRIGGER long_source_token_update_r80 AFTER UPDATE ON v57_draw_canonical_r811 WHEN NEW.status IS NOT OLD.status OR NEW.canonical_draw_hash IS NOT OLD.canonical_draw_hash BEGIN UPDATE research_long_source_token_r80 SET generation=generation+1 WHERE singleton=1; END;

-- main_chain_candidate_snapshot
CREATE TABLE main_chain_candidate_snapshot (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, candidate_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- main_chain_reject_bucket
CREATE TABLE main_chain_reject_bucket (_id INTEGER PRIMARY KEY AUTOINCREMENT, bucket_key TEXT NOT NULL UNIQUE, reject_code TEXT, sample_count INTEGER DEFAULT 0, candidate_count INTEGER DEFAULT 0, bet_count INTEGER DEFAULT 0, shadow_count INTEGER DEFAULT 0, hold_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, avg_final_p REAL DEFAULT 0, avg_gate REAL DEFAULT 0, avg_edge REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- manual_backtest_task
CREATE TABLE manual_backtest_task (_id INTEGER PRIMARY KEY AUTOINCREMENT, task_id TEXT NOT NULL UNIQUE, task_name TEXT, mode TEXT, status TEXT, progress INTEGER DEFAULT 0, window_size INTEGER DEFAULT 0, start_index INTEGER DEFAULT 0, end_index INTEGER DEFAULT 0, min_train INTEGER DEFAULT 0, max_test INTEGER DEFAULT 0, top_keep INTEGER DEFAULT 0, total_steps INTEGER DEFAULT 0, done_steps INTEGER DEFAULT 0, started_at INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0, finished_at INTEGER DEFAULT 0, summary TEXT, error TEXT);

-- miss_reason_sample
CREATE TABLE miss_reason_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, miss_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, miss_type TEXT, cluster_id TEXT, window_wrong_score REAL DEFAULT 0, position_wrong_score REAL DEFAULT 0, main_number_wrong_score REAL DEFAULT 0, third_drag_score REAL DEFAULT 0, counter_ignored_score REAL DEFAULT 0, detail TEXT, next_advice TEXT);

-- model_weight
CREATE TABLE model_weight (_id INTEGER PRIMARY KEY AUTOINCREMENT, weight_key TEXT NOT NULL UNIQUE, window_weight REAL DEFAULT 0.24, markov_weight REAL DEFAULT 0.14, shape_weight REAL DEFAULT 0.18, combo_weight REAL DEFAULT 0.14, recovery_weight REAL DEFAULT 0.08, similar_weight REAL DEFAULT 0.06, memory_weight REAL DEFAULT 0.04, position_weight REAL DEFAULT 0.08, frequency_weight REAL DEFAULT 0.00, drift_penalty_weight REAL DEFAULT 1.0, hot_reverse_penalty_weight REAL DEFAULT 1.0, false_strong_penalty_weight REAL DEFAULT 1.0, break_penalty_weight REAL DEFAULT 1.0, deep_feature_weight REAL DEFAULT 0.08, bias_weight REAL DEFAULT 0.05, profile_weight REAL DEFAULT 0.08, runtime_profile_weight REAL DEFAULT 0.10, state_number_profile_weight REAL DEFAULT 0.10, teacher_weight REAL DEFAULT 1.0, teacher_matrix_weight REAL DEFAULT 0.18, reason_weight REAL DEFAULT 0.08, elite45_weight REAL DEFAULT 0.06, cycle_weight REAL DEFAULT 0.05, transition_weight REAL DEFAULT 0.05, ai_judge_weight REAL DEFAULT 0.10, audit_weight REAL DEFAULT 0.06, recent_error REAL DEFAULT 0, recent_win_rate REAL DEFAULT 0, recent_frequency REAL DEFAULT 0, recent_pnl REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- module_effect_cache
CREATE TABLE module_effect_cache (_id INTEGER PRIMARY KEY AUTOINCREMENT,cache_key TEXT UNIQUE,module_name TEXT,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,support_count INTEGER DEFAULT 0,bet_count INTEGER DEFAULT 0,hit_count INTEGER DEFAULT 0,miss_count INTEGER DEFAULT 0,hit_rate REAL DEFAULT 0,avg_final_p REAL DEFAULT 0,avg_gate REAL DEFAULT 0,avg_edge REAL DEFAULT 0,profit REAL DEFAULT 0,shadow_would_hit_rate REAL DEFAULT 0,reliability REAL DEFAULT 0,recent_effectiveness REAL DEFAULT 0,effect_status TEXT,problem TEXT,fix_advice TEXT,summary_text TEXT,detail_text TEXT,ext_json TEXT, record_key TEXT, source_type TEXT, base_issue TEXT, target_issue TEXT, position INTEGER DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, teacher_status TEXT, v20_status TEXT, window_state_key TEXT, status TEXT);

-- molecular_tool_proposal_r947
CREATE TABLE molecular_tool_proposal_r947(proposal_hash TEXT PRIMARY KEY,novelty_key TEXT NOT NULL,operator TEXT NOT NULL,parent_indices TEXT NOT NULL,parent_hashes TEXT NOT NULL,state TEXT NOT NULL,created_issue TEXT NOT NULL,created_at INTEGER NOT NULL,selected_at INTEGER NOT NULL DEFAULT 0);

-- monotonic_calibration_v28
CREATE TABLE monotonic_calibration_v28 (_id INTEGER PRIMARY KEY AUTOINCREMENT, calibration_version TEXT NOT NULL, bucket_no INTEGER NOT NULL, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, brier_sum REAL DEFAULT 0, updated_at INTEGER DEFAULT 0, version TEXT, UNIQUE(calibration_version,bucket_no));

-- need_supply_match_sample
CREATE TABLE need_supply_match_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, match_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, main_role_number INTEGER DEFAULT 0, assist_role_number INTEGER DEFAULT 0, defense_role_number INTEGER DEFAULT 0, main_role TEXT, assist_role TEXT, defense_role TEXT, match_score REAL DEFAULT 0, main_role_score REAL DEFAULT 0, assist_role_score REAL DEFAULT 0, defense_role_score REAL DEFAULT 0, third_quality REAL DEFAULT 0, role_diversity REAL DEFAULT 0, supply_risk REAL DEFAULT 0, why_match TEXT, why_not_match TEXT, summary TEXT, state_key TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, sample_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT, updated_at INTEGER DEFAULT 0);

-- number_supply_sample
CREATE TABLE number_supply_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, label INTEGER DEFAULT 0, supply_key TEXT, supply_hot_continue REAL DEFAULT 0, supply_cold_repair REAL DEFAULT 0, supply_omission_return REAL DEFAULT 0, supply_high_low_balance REAL DEFAULT 0, supply_odd_even_balance REAL DEFAULT 0, supply_tail_repair REAL DEFAULT 0, supply_neighbor_relay REAL DEFAULT 0, supply_cross_position_flow REAL DEFAULT 0, supply_crowd_release REAL DEFAULT 0, supply_reverse_break REAL DEFAULT 0, supply_stable_repeat REAL DEFAULT 0, supply_risk REAL DEFAULT 0, total_supply_strength REAL DEFAULT 0, main_supply_type TEXT, summary TEXT, state_key TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, sample_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT, updated_at INTEGER DEFAULT 0);

-- oos_action_log_v28
CREATE TABLE oos_action_log_v28 (_id INTEGER PRIMARY KEY AUTOINCREMENT, contract_decision_id TEXT NOT NULL UNIQUE, candidate_hash TEXT, base_issue TEXT, target_issue TEXT, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, raw_rank_score REAL DEFAULT 0, calibrated_probability REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, coarse_family_key TEXT, mid_family_key TEXT, fine_family_key TEXT, position_family_key TEXT, snapshot_max_issue TEXT, trained_until_issue TEXT, build_id TEXT, cohort_id TEXT, schema_version TEXT, pipeline_version TEXT, calibration_version TEXT, gate_policy_version TEXT, selected_mode TEXT, authorization_id TEXT, authorization_valid INTEGER DEFAULT 0, authorization_reject_code TEXT, sample_source TEXT, created_at INTEGER DEFAULT 0, settled INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, result TEXT, settled_at INTEGER DEFAULT 0, processed INTEGER DEFAULT 0, processed_at INTEGER DEFAULT 0, published_version TEXT, origin_group_key TEXT, rank_slot INTEGER DEFAULT 0, candidate_count INTEGER DEFAULT 0, honest_probability REAL DEFAULT 0, action_utility REAL DEFAULT 0, structure_score REAL DEFAULT 0, discrimination_score REAL DEFAULT 0, maturity_score REAL DEFAULT 0, uncertainty_penalty REAL DEFAULT 0, frequency_support REAL DEFAULT 0, action_utility_version TEXT, lane_candidate TEXT, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_risk REAL DEFAULT 0, v42_accept_score REAL DEFAULT 0, v42_family_lcb REAL DEFAULT 0, v42_regime_quality REAL DEFAULT 0, v42_association_score REAL DEFAULT 0, v42_distillation_risk REAL DEFAULT 0, v42_support_votes INTEGER DEFAULT 0, v42_regime_family TEXT, v42_risk_coverage_mode TEXT, v42_primary_reject TEXT, v42_secondary_reject TEXT, v42_tertiary_reject TEXT, v42_lane_policy_version TEXT, v43_strategy_support REAL DEFAULT 0, v43_strategy_risk REAL DEFAULT 0, v43_strategy_confidence REAL DEFAULT 0, v43_strategy_diversity REAL DEFAULT 0, v43_strategy_maturity REAL DEFAULT 0, v43_safe_probability REAL DEFAULT 0, v43_probability_residual REAL DEFAULT 0, v43_ablation_fragility REAL DEFAULT 0, v43_probability_lock REAL DEFAULT 0, v43_evidence_group_count INTEGER DEFAULT 0, v43_hard_risk INTEGER DEFAULT 0, v43_dominant_evidence TEXT, v43_missing_evidence TEXT, v43_evidence_fingerprint TEXT, v43_probability_lock_hash TEXT, v43_pipeline_stage TEXT, v43_invariant_status TEXT, v43_strategy_version TEXT, window_state_key TEXT, dynamic_cluster_key TEXT, teacher_policy TEXT, posterior_policy TEXT, model_probability REAL DEFAULT 0, feature_0 REAL DEFAULT 0, feature_1 REAL DEFAULT 0, feature_2 REAL DEFAULT 0, feature_3 REAL DEFAULT 0, feature_4 REAL DEFAULT 0, feature_5 REAL DEFAULT 0, feature_6 REAL DEFAULT 0, feature_7 REAL DEFAULT 0, model_version TEXT, version TEXT, v46_event_probability REAL DEFAULT 0, v46_action_lcb REAL DEFAULT 0, v46_probability_uncertainty REAL DEFAULT 0, v46_expected_value REAL DEFAULT 0, v46_global_calibrated_probability REAL DEFAULT 0, v46_position_adjustment REAL DEFAULT 0, v46_rank_adjustment REAL DEFAULT 0, v46_regime_adjustment REAL DEFAULT 0, v46_calibration_confidence REAL DEFAULT 0, v46_final_rank INTEGER DEFAULT 0, v46_formal_candidate_count INTEGER DEFAULT 0, v46_version TEXT, v46_number_distribution_summary TEXT, v46_probability_trace TEXT, v46_root_cause_code TEXT, v46_root_cause_owner TEXT, v46_final_contract_hash TEXT, v46_lane TEXT, settlement_task_id TEXT NOT NULL DEFAULT '', error_code TEXT NOT NULL DEFAULT '');

-- point_in_time_snapshot
CREATE TABLE point_in_time_snapshot (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_id TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, train_start_issue TEXT, train_end_issue TEXT, visible_draw_count INTEGER DEFAULT 0, visible_trade_count INTEGER DEFAULT 0, visible_profile_version INTEGER DEFAULT 0, visible_calibration_version INTEGER DEFAULT 0, visible_teacher_matrix_version INTEGER DEFAULT 0, visible_reason_version INTEGER DEFAULT 0, feature_summary TEXT, candidate_summary TEXT, gate_summary TEXT, decision_summary TEXT, reason_summary TEXT, data_hash TEXT, no_future_leak_ok INTEGER DEFAULT 0, status TEXT, warning_text TEXT, created_at INTEGER DEFAULT 0);

-- position_cause_matrix
CREATE TABLE position_cause_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, position_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent30_sample INTEGER DEFAULT 0, recent30_hit INTEGER DEFAULT 0, recent60_sample INTEGER DEFAULT 0, recent60_hit INTEGER DEFAULT 0, recent120_sample INTEGER DEFAULT 0, recent120_hit INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, shadow_hit_rate REAL DEFAULT 0, max_lose INTEGER DEFAULT 0, current_lose INTEGER DEFAULT 0, false_strong_rate REAL DEFAULT 0, recovery_score REAL DEFAULT 0, cycle_health REAL DEFAULT 0, position_edge REAL DEFAULT 0, position_risk REAL DEFAULT 0, position_healthy INTEGER DEFAULT 0, freeze_until_issue TEXT, status TEXT, updated_at INTEGER DEFAULT 0, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- position_need_sample
CREATE TABLE position_need_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, need_key TEXT, need_hot_continue REAL DEFAULT 0, need_cold_repair REAL DEFAULT 0, need_omission_return REAL DEFAULT 0, need_high_low_balance REAL DEFAULT 0, need_odd_even_balance REAL DEFAULT 0, need_tail_repair REAL DEFAULT 0, need_neighbor_relay REAL DEFAULT 0, need_cross_position_flow REAL DEFAULT 0, need_crowd_release REAL DEFAULT 0, need_reverse_break REAL DEFAULT 0, need_stable_repeat REAL DEFAULT 0, need_chaos_avoid REAL DEFAULT 0, total_need_strength REAL DEFAULT 0, chaos_risk REAL DEFAULT 0, main_need_type TEXT, summary TEXT, state_key TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, sample_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT, updated_at INTEGER DEFAULT 0);

-- pre_draw_feature
CREATE TABLE pre_draw_feature (_id INTEGER PRIMARY KEY AUTOINCREMENT, feature_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, history_count INTEGER DEFAULT 0, position INTEGER DEFAULT 0, window_12_ok INTEGER DEFAULT 0, window_24_ok INTEGER DEFAULT 0, window_48_ok INTEGER DEFAULT 0, window_96_ok INTEGER DEFAULT 0, window_180_ok INTEGER DEFAULT 0, window_360_ok INTEGER DEFAULT 0, window_720_ok INTEGER DEFAULT 0, exact_state_key TEXT, relaxed_state_key TEXT, hot_cold_slope REAL DEFAULT 0, miss_bucket INTEGER DEFAULT 0, density_score REAL DEFAULT 0, repeat_score REAL DEFAULT 0, odd_even_bias REAL DEFAULT 0, big_small_bias REAL DEFAULT 0, neighbor_score REAL DEFAULT 0, same_tail_score REAL DEFAULT 0, position_crowd_score REAL DEFAULT 0, cross_position_sync REAL DEFAULT 0, recent_lose_chain INTEGER DEFAULT 0, timing_offset INTEGER DEFAULT 0, timing_bucket TEXT, trend_state TEXT, shape_state TEXT, entropy_state TEXT, drift_state TEXT, flow_state TEXT, cycle_state TEXT, rank_mode TEXT, board_risk REAL DEFAULT 0, state_key TEXT, source_key TEXT, reason_atoms TEXT, feature_json TEXT, summary_text TEXT);

-- pre_draw_number_sample
CREATE TABLE pre_draw_number_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, label INTEGER DEFAULT 0, state_key TEXT, trend_state TEXT, shape_state TEXT, life_state TEXT, miss_gap INTEGER DEFAULT 0, hot_score REAL DEFAULT 0, cold_score REAL DEFAULT 0, cycle_score REAL DEFAULT 0, migration_score REAL DEFAULT 0, teacher_key TEXT, why_text TEXT);

-- pre_draw_reason_sample
CREATE TABLE pre_draw_reason_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE, source_type TEXT, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, target_index INTEGER DEFAULT 0, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, label INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, state_key TEXT, state_mode TEXT, trend_key TEXT, shape_key TEXT, drift_key TEXT, break_key TEXT, recovery_key TEXT, transition_key TEXT, relation_key TEXT, reason_fingerprint TEXT, reason_atom_text TEXT, reason_combo_key TEXT, valid_level TEXT, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, reason_score REAL DEFAULT 0, reason_edge REAL DEFAULT 0, hit_similarity REAL DEFAULT 0, miss_similarity REAL DEFAULT 0, contradiction_score REAL DEFAULT 0, causal_lift REAL DEFAULT 0, causal_confidence REAL DEFAULT 0, combo_conflict REAL DEFAULT 0, combo_role_diversity REAL DEFAULT 0, drift_risk REAL DEFAULT 0, break_risk REAL DEFAULT 0, recovery_score REAL DEFAULT 0, risk_score REAL DEFAULT 0, valid_reason_count INTEGER DEFAULT 0, false_reason_count INTEGER DEFAULT 0, forward_sample INTEGER DEFAULT 0, forward_hit INTEGER DEFAULT 0, forward_hit_rate REAL DEFAULT 0, wilson_score REAL DEFAULT 0, k3_role TEXT, main_reason_code TEXT, why_hit_text TEXT, why_miss_text TEXT, next_advice TEXT, summary_text TEXT, saved INTEGER DEFAULT 0);

-- probability_audit_bucket
CREATE TABLE probability_audit_bucket (_id INTEGER PRIMARY KEY AUTOINCREMENT, bucket_key TEXT NOT NULL UNIQUE, bucket_low REAL DEFAULT 0, bucket_high REAL DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, avg_predicted_p REAL DEFAULT 0, calibration_error REAL DEFAULT 0, brier_score REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, recent_error REAL DEFAULT 0, status TEXT, updated_at INTEGER DEFAULT 0);

-- promotion_candidate
CREATE TABLE promotion_candidate (_id INTEGER PRIMARY KEY AUTOINCREMENT, promotion_key TEXT NOT NULL UNIQUE, profile_key TEXT, status TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, calibration_error REAL DEFAULT 0, bias_score REAL DEFAULT 0, a_plus_score REAL DEFAULT 0, teacher_matrix_score REAL DEFAULT 0, reason_score REAL DEFAULT 0, promote_reason TEXT, reject_reason TEXT, updated_at INTEGER DEFAULT 0);

-- promotion_domain_snapshot
CREATE TABLE promotion_domain_snapshot (_id INTEGER PRIMARY KEY AUTOINCREMENT, domain_key TEXT NOT NULL UNIQUE, cluster_id TEXT, status TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, max_lose_streak INTEGER DEFAULT 0, confidence REAL DEFAULT 0, action TEXT, reason TEXT, updated_at INTEGER DEFAULT 0, snapshot_key TEXT, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, candidate_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT);

-- promotion_domain_stat_v20
CREATE TABLE promotion_domain_stat_v20 (domain_key TEXT PRIMARY KEY, parent_domain_key TEXT, coarse_domain_key TEXT, status TEXT, version TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, recent_hit_count INTEGER DEFAULT 0, misleading_streak INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, posterior_mean REAL DEFAULT 0, lower_bound REAL DEFAULT 0, upper_bound REAL DEFAULT 1, drift_score REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- r79_legacy_classification_no_delete
CREATE TRIGGER r79_legacy_classification_no_delete BEFORE DELETE ON research_legacy_settlement_classification_r79 BEGIN SELECT RAISE(ABORT,'R79_ORIGINAL_CLASSIFICATION_IMMUTABLE'); END;

-- r79_legacy_classification_no_replace
CREATE TRIGGER r79_legacy_classification_no_replace BEFORE INSERT ON research_legacy_settlement_classification_r79 WHEN EXISTS(SELECT 1 FROM research_legacy_settlement_classification_r79 WHERE receipt_hash=NEW.receipt_hash) BEGIN SELECT RAISE(ABORT,'R79_ORIGINAL_CLASSIFICATION_NO_REPLACE'); END;

-- r79_legacy_classification_no_update
CREATE TRIGGER r79_legacy_classification_no_update BEFORE UPDATE ON research_legacy_settlement_classification_r79 BEGIN SELECT RAISE(ABORT,'R79_ORIGINAL_CLASSIFICATION_IMMUTABLE'); END;

-- r80_asset_audit_fence_0_delete
CREATE TRIGGER r80_asset_audit_fence_0_delete AFTER DELETE ON research_new_asset_genome_v41 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=OLD.asset_id)); END;

-- r80_asset_audit_fence_0_insert
CREATE TRIGGER r80_asset_audit_fence_0_insert AFTER INSERT ON research_new_asset_genome_v41 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=NEW.asset_id)); END;

-- r80_asset_audit_fence_0_update
CREATE TRIGGER r80_asset_audit_fence_0_update AFTER UPDATE ON research_new_asset_genome_v41 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=OLD.asset_id) OR (asset_id=NEW.asset_id)); END;

-- r80_asset_audit_fence_1_delete
CREATE TRIGGER r80_asset_audit_fence_1_delete AFTER DELETE ON research_new_asset_freeze_v41 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=OLD.asset_id)); END;

-- r80_asset_audit_fence_1_insert
CREATE TRIGGER r80_asset_audit_fence_1_insert AFTER INSERT ON research_new_asset_freeze_v41 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=NEW.asset_id)); END;

-- r80_asset_audit_fence_1_update
CREATE TRIGGER r80_asset_audit_fence_1_update AFTER UPDATE ON research_new_asset_freeze_v41 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=OLD.asset_id) OR (asset_id=NEW.asset_id)); END;

-- r80_asset_audit_fence_2_delete
CREATE TRIGGER r80_asset_audit_fence_2_delete AFTER DELETE ON research_candidate_execution_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(OLD.candidate_key,9) AND OLD.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_2_insert
CREATE TRIGGER r80_asset_audit_fence_2_insert AFTER INSERT ON research_candidate_execution_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(NEW.candidate_key,9) AND NEW.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_2_update
CREATE TRIGGER r80_asset_audit_fence_2_update AFTER UPDATE ON research_candidate_execution_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(OLD.candidate_key,9) AND OLD.candidate_key LIKE 'genesis:%') OR (asset_id=substr(NEW.candidate_key,9) AND NEW.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_3_delete
CREATE TRIGGER r80_asset_audit_fence_3_delete AFTER DELETE ON research_capture_opportunity_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(OLD.candidate_key,9) AND OLD.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_3_insert
CREATE TRIGGER r80_asset_audit_fence_3_insert AFTER INSERT ON research_capture_opportunity_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(NEW.candidate_key,9) AND NEW.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_3_update
CREATE TRIGGER r80_asset_audit_fence_3_update AFTER UPDATE ON research_capture_opportunity_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(OLD.candidate_key,9) AND OLD.candidate_key LIKE 'genesis:%') OR (asset_id=substr(NEW.candidate_key,9) AND NEW.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_4_delete
CREATE TRIGGER r80_asset_audit_fence_4_delete AFTER DELETE ON research_capture_event_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(OLD.candidate_key,9) AND OLD.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_4_insert
CREATE TRIGGER r80_asset_audit_fence_4_insert AFTER INSERT ON research_capture_event_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(NEW.candidate_key,9) AND NEW.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_4_update
CREATE TRIGGER r80_asset_audit_fence_4_update AFTER UPDATE ON research_capture_event_r932 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id=substr(OLD.candidate_key,9) AND OLD.candidate_key LIKE 'genesis:%') OR (asset_id=substr(NEW.candidate_key,9) AND NEW.candidate_key LIKE 'genesis:%')); END;

-- r80_asset_audit_fence_5_delete
CREATE TRIGGER r80_asset_audit_fence_5_delete AFTER DELETE ON draws BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=OLD.issue AND hit_label IN(0,1)))); END;

-- r80_asset_audit_fence_5_insert
CREATE TRIGGER r80_asset_audit_fence_5_insert AFTER INSERT ON draws BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=NEW.issue AND hit_label IN(0,1)))); END;

-- r80_asset_audit_fence_5_update
CREATE TRIGGER r80_asset_audit_fence_5_update AFTER UPDATE ON draws BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=OLD.issue AND hit_label IN(0,1))) OR (asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=NEW.issue AND hit_label IN(0,1)))); END;

-- r80_asset_audit_fence_6_delete
CREATE TRIGGER r80_asset_audit_fence_6_delete AFTER DELETE ON draw_dependency_input_cp12 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=OLD.issue AND hit_label IN(0,1)))); END;

-- r80_asset_audit_fence_6_insert
CREATE TRIGGER r80_asset_audit_fence_6_insert AFTER INSERT ON draw_dependency_input_cp12 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=NEW.issue AND hit_label IN(0,1)))); END;

-- r80_asset_audit_fence_6_update
CREATE TRIGGER r80_asset_audit_fence_6_update AFTER UPDATE ON draw_dependency_input_cp12 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=OLD.issue AND hit_label IN(0,1))) OR (asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=NEW.issue AND hit_label IN(0,1)))); END;

-- r80_asset_audit_fence_7_delete
CREATE TRIGGER r80_asset_audit_fence_7_delete AFTER DELETE ON v57_draw_canonical_r811 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=OLD.target_issue AND hit_label IN(0,1)))); END;

-- r80_asset_audit_fence_7_insert
CREATE TRIGGER r80_asset_audit_fence_7_insert AFTER INSERT ON v57_draw_canonical_r811 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=NEW.target_issue AND hit_label IN(0,1)))); END;

-- r80_asset_audit_fence_7_update
CREATE TRIGGER r80_asset_audit_fence_7_update AFTER UPDATE ON v57_draw_canonical_r811 BEGIN UPDATE research_legacy_asset_head_audit_r80 SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ((asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=OLD.target_issue AND hit_label IN(0,1))) OR (asset_id IN(SELECT asset_id FROM research_new_asset_freeze_v41 WHERE target_issue=NEW.target_issue AND hit_label IN(0,1)))); END;

-- r80_atom_binding_immutable
CREATE TRIGGER r80_atom_binding_immutable BEFORE UPDATE ON research_atom_binding_v2 BEGIN SELECT RAISE(ABORT,'ATOM_BINDING_IMMUTABLE'); END;

-- r80_atom_binding_no_replace
CREATE TRIGGER r80_atom_binding_no_replace BEFORE INSERT ON research_atom_binding_v2 WHEN EXISTS(SELECT 1 FROM research_atom_binding_v2 x WHERE x.binding_hash=NEW.binding_hash OR (x.atom_id=NEW.atom_id AND x.candidate_version=NEW.candidate_version AND x.model_hash=NEW.model_hash AND x.cohort_hash=NEW.cohort_hash)) BEGIN SELECT RAISE(ABORT,'ATOM_BINDING_REPLACE_FORBIDDEN'); END;

-- r80_atom_binding_original
CREATE TRIGGER r80_atom_binding_original BEFORE DELETE ON research_atom_binding_v2 WHEN EXISTS(SELECT 1 FROM research_bound_cohort_r80 c WHERE c.cohort_hash=OLD.cohort_hash) BEGIN SELECT RAISE(ABORT,'BOUND_BINDING_ORIGINAL_PERMANENT'); END;

-- r80_bound_active
CREATE INDEX r80_bound_active ON research_bound_cohort_r80(state,next_issue);

-- r80_bound_child
CREATE INDEX r80_bound_child ON research_bound_cohort_r80(child_id);

-- r80_bound_cohort_identity
CREATE TRIGGER r80_bound_cohort_identity BEFORE UPDATE ON research_bound_cohort_r80 WHEN OLD.cohort_hash<>NEW.cohort_hash OR OLD.selection_json<>NEW.selection_json OR OLD.ast<>NEW.ast OR OLD.ast_hash<>NEW.ast_hash OR OLD.manifest_hash<>NEW.manifest_hash OR OLD.source_issue<>NEW.source_issue OR OLD.first_issue<>NEW.first_issue OR OLD.registered_at<>NEW.registered_at BEGIN SELECT RAISE(ABORT,'BOUND_COHORT_IMMUTABLE'); END;

-- r80_bound_input_target
CREATE INDEX r80_bound_input_target ON research_bound_input_r80(target_issue);

-- r80_bound_pending
CREATE INDEX r80_bound_pending ON research_bound_source_r80(settled,target_issue);

-- r80_bound_source_identity
CREATE TRIGGER r80_bound_source_identity BEFORE UPDATE ON research_bound_source_r80 WHEN OLD.cohort_hash<>NEW.cohort_hash OR OLD.candidate_id<>NEW.candidate_id OR OLD.target_issue<>NEW.target_issue OR OLD.role<>NEW.role OR OLD.record_hash<>NEW.record_hash OR OLD.input_hash<>NEW.input_hash OR OLD.context_hash<>NEW.context_hash OR OLD.context_json<>NEW.context_json OR OLD.position<>NEW.position OR OLD.n1<>NEW.n1 OR OLD.n2<>NEW.n2 OR OLD.n3<>NEW.n3 OR OLD.score<>NEW.score OR OLD.captured_at<>NEW.captured_at OR OLD.deadline_at<>NEW.deadline_at OR OLD.settled=1 OR OLD.proof_error<>'' BEGIN SELECT RAISE(ABORT,'BOUND_SOURCE_IMMUTABLE'); END;

-- r80_bound_source_role
CREATE INDEX r80_bound_source_role ON research_bound_source_r80(candidate_id,target_issue);

-- r80_legacy_baseline_no_delete
CREATE TRIGGER r80_legacy_baseline_no_delete BEFORE DELETE ON research_legacy_asset_head_baseline_r80 BEGIN SELECT RAISE(ABORT,'R80_LEGACY_BASELINE_IMMUTABLE'); END;

-- r80_legacy_baseline_no_replace
CREATE TRIGGER r80_legacy_baseline_no_replace BEFORE INSERT ON research_legacy_asset_head_baseline_r80 WHEN EXISTS(SELECT 1 FROM research_legacy_asset_head_baseline_r80 WHERE asset_id=NEW.asset_id) BEGIN SELECT RAISE(ABORT,'R80_LEGACY_BASELINE_NO_REPLACE'); END;

-- r80_legacy_baseline_no_update
CREATE TRIGGER r80_legacy_baseline_no_update BEFORE UPDATE ON research_legacy_asset_head_baseline_r80 BEGIN SELECT RAISE(ABORT,'R80_LEGACY_BASELINE_IMMUTABLE'); END;

-- r80_one_active_cohort
CREATE UNIQUE INDEX r80_one_active_cohort ON research_bound_cohort_r80(substr(ast,-26)) WHERE state IN ('CAPTURING','READY','PROPOSED','REGISTERED');

-- r80_probe_diagnostic_ref
CREATE TABLE r80_probe_diagnostic_ref(input_hash TEXT NOT NULL REFERENCES r80_probe_input(receipt_hash) ON DELETE CASCADE,label_hash TEXT NOT NULL REFERENCES r80_probe_label(label_hash),PRIMARY KEY(input_hash,label_hash));

-- r80_probe_forecast
CREATE TABLE r80_probe_forecast(anchor_issue TEXT NOT NULL,horizon INTEGER NOT NULL CHECK(horizon IN(0,1,2,4,8)),target_issue TEXT NOT NULL,role TEXT NOT NULL CHECK(role='PROBE_RESEARCH_ONLY_NOT_FORMAL'),protocol_hash TEXT NOT NULL,input_hash TEXT NOT NULL,model_hash TEXT NOT NULL,forecast_hash TEXT NOT NULL UNIQUE,receipt_json TEXT NOT NULL,captured_at INTEGER NOT NULL CHECK(captured_at>0),PRIMARY KEY(anchor_issue,horizon));

-- r80_probe_forecast_archive_guard
CREATE TRIGGER r80_probe_forecast_archive_guard BEFORE DELETE ON r80_probe_forecast WHEN NOT COALESCE((OLD.target_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND EXISTS(SELECT 1 FROM r80_probe_label l WHERE l.forecast_hash=OLD.forecast_hash) AND OLD.forecast_hash NOT IN(SELECT l.forecast_hash FROM r80_probe_label l WHERE l.horizon=OLD.horizon AND l.status='SETTLED' ORDER BY l.target_issue DESC LIMIT 48) AND NOT EXISTS(SELECT 1 FROM r80_probe_forecast c WHERE c.target_issue=OLD.target_issue AND c.horizon=0 AND instr(c.receipt_json,OLD.forecast_hash)>0 AND NOT EXISTS(SELECT 1 FROM r80_probe_label l WHERE l.forecast_hash=c.forecast_hash))),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='r80_probe_forecast' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='r80_probe_forecast' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'PROBE_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- r80_probe_forecast_archive_locator
CREATE TRIGGER r80_probe_forecast_archive_locator BEFORE DELETE ON r80_probe_forecast BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='r80_probe_forecast' AND p.source_rowid=OLD.rowid; END;

-- r80_probe_forecast_no_replace
CREATE TRIGGER r80_probe_forecast_no_replace BEFORE INSERT ON r80_probe_forecast WHEN EXISTS(SELECT 1 FROM r80_probe_forecast WHERE anchor_issue=NEW.anchor_issue AND horizon=NEW.horizon) BEGIN SELECT RAISE(ABORT,'R80_PROBE_NO_REPLACE'); END;

-- r80_probe_forecast_no_update
CREATE TRIGGER r80_probe_forecast_no_update BEFORE UPDATE ON r80_probe_forecast BEGIN SELECT RAISE(ABORT,'R80_PROBE_IMMUTABLE'); END;

-- r80_probe_forecast_revision_delete
CREATE TRIGGER r80_probe_forecast_revision_delete AFTER DELETE ON r80_probe_forecast BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='r80_probe_forecast' AND source_rowid=OLD.rowid; END;

-- r80_probe_forecast_revision_insert
CREATE TRIGGER r80_probe_forecast_revision_insert AFTER INSERT ON r80_probe_forecast BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r80_probe_forecast',NEW.rowid); END;

-- r80_probe_input
CREATE TABLE r80_probe_input(anchor_issue TEXT PRIMARY KEY,protocol_hash TEXT NOT NULL,source_hash TEXT NOT NULL,model_hash TEXT NOT NULL,receipt_json TEXT NOT NULL,receipt_hash TEXT NOT NULL UNIQUE,captured_at INTEGER NOT NULL CHECK(captured_at>0));

-- r80_probe_input_archive_guard
CREATE TRIGGER r80_probe_input_archive_guard BEFORE DELETE ON r80_probe_input WHEN NOT COALESCE((OLD.anchor_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND NOT EXISTS(SELECT 1 FROM r80_probe_forecast f WHERE f.input_hash=OLD.receipt_hash)),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='r80_probe_input' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='r80_probe_input' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'PROBE_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- r80_probe_input_archive_locator
CREATE TRIGGER r80_probe_input_archive_locator BEFORE DELETE ON r80_probe_input BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='r80_probe_input' AND p.source_rowid=OLD.rowid; END;

-- r80_probe_input_no_replace
CREATE TRIGGER r80_probe_input_no_replace BEFORE INSERT ON r80_probe_input WHEN EXISTS(SELECT 1 FROM r80_probe_input WHERE anchor_issue=NEW.anchor_issue) BEGIN SELECT RAISE(ABORT,'R80_PROBE_NO_REPLACE'); END;

-- r80_probe_input_no_update
CREATE TRIGGER r80_probe_input_no_update BEFORE UPDATE ON r80_probe_input BEGIN SELECT RAISE(ABORT,'R80_PROBE_IMMUTABLE'); END;

-- r80_probe_input_ref_release
CREATE TRIGGER r80_probe_input_ref_release AFTER DELETE ON r80_probe_input BEGIN DELETE FROM r80_probe_diagnostic_ref WHERE input_hash=OLD.receipt_hash; END;

-- r80_probe_input_revision_delete
CREATE TRIGGER r80_probe_input_revision_delete AFTER DELETE ON r80_probe_input BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='r80_probe_input' AND source_rowid=OLD.rowid; END;

-- r80_probe_input_revision_insert
CREATE TRIGGER r80_probe_input_revision_insert AFTER INSERT ON r80_probe_input BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r80_probe_input',NEW.rowid); END;

-- r80_probe_label
CREATE TABLE r80_probe_label(forecast_hash TEXT PRIMARY KEY,target_issue TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN('SETTLED','REJECTED')),reason TEXT NOT NULL,settlement_hash TEXT NOT NULL,completed_at INTEGER NOT NULL,receipt_json TEXT NOT NULL,label_hash TEXT NOT NULL UNIQUE,horizon INTEGER NOT NULL CHECK(horizon IN(0,1,2,4,8)));

-- r80_probe_label_archive_guard
CREATE TRIGGER r80_probe_label_archive_guard BEFORE DELETE ON r80_probe_label WHEN NOT COALESCE((OLD.target_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND NOT EXISTS(SELECT 1 FROM r80_probe_forecast f WHERE f.forecast_hash=OLD.forecast_hash) AND NOT EXISTS(SELECT 1 FROM r80_probe_diagnostic_ref r WHERE r.label_hash=OLD.label_hash)),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='r80_probe_label' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='r80_probe_label' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'PROBE_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- r80_probe_label_archive_locator
CREATE TRIGGER r80_probe_label_archive_locator BEFORE DELETE ON r80_probe_label BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='r80_probe_label' AND p.source_rowid=OLD.rowid; END;

-- r80_probe_label_no_replace
CREATE TRIGGER r80_probe_label_no_replace BEFORE INSERT ON r80_probe_label WHEN EXISTS(SELECT 1 FROM r80_probe_label WHERE forecast_hash=NEW.forecast_hash) BEGIN SELECT RAISE(ABORT,'R80_PROBE_NO_REPLACE'); END;

-- r80_probe_label_no_update
CREATE TRIGGER r80_probe_label_no_update BEFORE UPDATE ON r80_probe_label BEGIN SELECT RAISE(ABORT,'R80_PROBE_IMMUTABLE'); END;

-- r80_probe_label_revision_delete
CREATE TRIGGER r80_probe_label_revision_delete AFTER DELETE ON r80_probe_label BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='r80_probe_label' AND source_rowid=OLD.rowid; END;

-- r80_probe_label_revision_insert
CREATE TRIGGER r80_probe_label_revision_insert AFTER INSERT ON r80_probe_label BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r80_probe_label',NEW.rowid); END;

-- r80_probe_opportunity
CREATE TABLE r80_probe_opportunity(anchor_issue TEXT PRIMARY KEY,target_issue TEXT NOT NULL,protocol_hash TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN('FROZEN','NOT_RUN')),reason TEXT NOT NULL,consensus_status TEXT NOT NULL,observed_at INTEGER NOT NULL,receipt_json TEXT NOT NULL,receipt_hash TEXT NOT NULL UNIQUE);

-- r80_probe_opportunity_archive_guard
CREATE TRIGGER r80_probe_opportunity_archive_guard BEFORE DELETE ON r80_probe_opportunity WHEN NOT COALESCE((OLD.anchor_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND NOT EXISTS(SELECT 1 FROM r80_probe_forecast f WHERE f.anchor_issue=OLD.anchor_issue)),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='r80_probe_opportunity' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='r80_probe_opportunity' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'PROBE_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- r80_probe_opportunity_archive_locator
CREATE TRIGGER r80_probe_opportunity_archive_locator BEFORE DELETE ON r80_probe_opportunity BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='r80_probe_opportunity' AND p.source_rowid=OLD.rowid; END;

-- r80_probe_opportunity_no_replace
CREATE TRIGGER r80_probe_opportunity_no_replace BEFORE INSERT ON r80_probe_opportunity WHEN EXISTS(SELECT 1 FROM r80_probe_opportunity WHERE anchor_issue=NEW.anchor_issue) BEGIN SELECT RAISE(ABORT,'R80_PROBE_NO_REPLACE'); END;

-- r80_probe_opportunity_no_update
CREATE TRIGGER r80_probe_opportunity_no_update BEFORE UPDATE ON r80_probe_opportunity BEGIN SELECT RAISE(ABORT,'R80_PROBE_IMMUTABLE'); END;

-- r80_probe_opportunity_revision_delete
CREATE TRIGGER r80_probe_opportunity_revision_delete AFTER DELETE ON r80_probe_opportunity BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='r80_probe_opportunity' AND source_rowid=OLD.rowid; END;

-- r80_probe_opportunity_revision_insert
CREATE TRIGGER r80_probe_opportunity_revision_insert AFTER INSERT ON r80_probe_opportunity BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('r80_probe_opportunity',NEW.rowid); END;

-- r80_probe_pending
CREATE VIEW r80_probe_pending AS SELECT f.target_issue,CASE WHEN l.forecast_hash IS NULL THEN 0 ELSE 1 END AS settled,CASE WHEN l.status='REJECTED' THEN l.reason ELSE '' END AS proof_error FROM r80_probe_forecast f LEFT JOIN r80_probe_label l ON l.forecast_hash=f.forecast_hash;

-- r80_probe_ref_delete_guard
CREATE TRIGGER r80_probe_ref_delete_guard BEFORE DELETE ON r80_probe_diagnostic_ref WHEN EXISTS(SELECT 1 FROM r80_probe_input i WHERE i.receipt_hash=OLD.input_hash) BEGIN SELECT RAISE(ABORT,'PROBE_REFERENCE_STILL_IN_USE'); END;

-- r80_probe_ref_no_replace
CREATE TRIGGER r80_probe_ref_no_replace BEFORE INSERT ON r80_probe_diagnostic_ref WHEN EXISTS(SELECT 1 FROM r80_probe_diagnostic_ref r WHERE r.input_hash=NEW.input_hash AND r.label_hash=NEW.label_hash) BEGIN SELECT RAISE(ABORT,'PROBE_REFERENCE_REPLACE_FORBIDDEN'); END;

-- r80_probe_ref_no_update
CREATE TRIGGER r80_probe_ref_no_update BEFORE UPDATE ON r80_probe_diagnostic_ref BEGIN SELECT RAISE(ABORT,'PROBE_REFERENCE_IMMUTABLE'); END;

-- r81_bound_active_cap
CREATE TRIGGER r81_bound_active_cap BEFORE INSERT ON research_bound_cohort_r80 WHEN NEW.state IN ('CAPTURING','READY','PROPOSED','REGISTERED') AND (SELECT COUNT(*) FROM research_bound_cohort_r80 WHERE state IN ('CAPTURING','READY','PROPOSED','REGISTERED'))>=3 BEGIN SELECT RAISE(ABORT,'BOUND_ACTIVE_CAPACITY'); END;

-- r81_bound_reactivate_cap
CREATE TRIGGER r81_bound_reactivate_cap BEFORE UPDATE OF state ON research_bound_cohort_r80 WHEN OLD.state NOT IN ('CAPTURING','READY','PROPOSED','REGISTERED') AND NEW.state IN ('CAPTURING','READY','PROPOSED','REGISTERED') AND (SELECT COUNT(*) FROM research_bound_cohort_r80 WHERE state IN ('CAPTURING','READY','PROPOSED','REGISTERED'))>=3 BEGIN SELECT RAISE(ABORT,'BOUND_ACTIVE_CAPACITY'); END;

-- r81_long_creation_identity
CREATE TRIGGER r81_long_creation_identity BEFORE UPDATE ON research_long_creation_r81 WHEN OLD.source_hash<>NEW.source_hash OR OLD.epoch_hash<>NEW.epoch_hash OR OLD.family<>NEW.family OR OLD.variant<>NEW.variant OR OLD.parent_id<>NEW.parent_id OR OLD.parent_hash<>NEW.parent_hash OR OLD.ast_hash<>NEW.ast_hash OR OLD.ast<>NEW.ast OR OLD.source_issue<>NEW.source_issue OR OLD.intent_id<>NEW.intent_id OR OLD.action_id<>NEW.action_id OR OLD.candidate_id<>NEW.candidate_id OR OLD.coverage_hash<>NEW.coverage_hash OR OLD.created_at<>NEW.created_at BEGIN SELECT RAISE(ABORT,'LONG_CREATION_IMMUTABLE'); END;

-- r81_long_creation_transition
CREATE TRIGGER r81_long_creation_transition BEFORE UPDATE ON research_long_creation_r81 WHEN OLD.state<>'PROPOSED' OR NEW.generation<>OLD.generation+1 BEGIN SELECT RAISE(ABORT,'LONG_CREATION_TERMINAL_OR_GENERATION'); END;

-- r81_long_frozen_immutable
CREATE TRIGGER r81_long_frozen_immutable BEFORE UPDATE ON research_long_frozen_source_r81 BEGIN SELECT RAISE(ABORT,'LONG_FROZEN_SOURCE_IMMUTABLE'); END;

-- r81_long_frozen_no_delete
CREATE TRIGGER r81_long_frozen_no_delete BEFORE DELETE ON research_long_frozen_source_r81 BEGIN SELECT RAISE(ABORT,'LONG_FROZEN_SOURCE_PERMANENT'); END;

-- r81_long_frozen_no_replace
CREATE TRIGGER r81_long_frozen_no_replace BEFORE INSERT ON research_long_frozen_source_r81 WHEN EXISTS(SELECT 1 FROM research_long_frozen_source_r81 x WHERE x.freeze_hash=NEW.freeze_hash AND x.epoch_hash=NEW.epoch_hash) OR (SELECT COUNT(*) FROM research_long_frozen_source_r81 WHERE freeze_hash=NEW.freeze_hash)>=16 BEGIN SELECT RAISE(ABORT,'LONG_FROZEN_SOURCE_REPLACE_OR_CAPACITY'); END;

-- r81_long_spec_immutable
CREATE TRIGGER r81_long_spec_immutable BEFORE UPDATE ON research_long_spec_source_r81 BEGIN SELECT RAISE(ABORT,'LONG_SPEC_PROVENANCE_IMMUTABLE'); END;

-- r81_one_long_proposal
CREATE UNIQUE INDEX r81_one_long_proposal ON research_long_creation_r81((1)) WHERE state='PROPOSED';

-- r82_calibration_pin_research_champion_probability_r82
CREATE TRIGGER r82_calibration_pin_research_champion_probability_r82 BEFORE DELETE ON research_champion_probability_r82 WHEN EXISTS(SELECT 1 FROM research_candidate_cal_observation_r932 o JOIN research_candidate_cal_study_r932 s ON s.study_hash=o.study_hash WHERE o.target_issue=OLD.target_issue AND s.state IN('COLLECTING','READY')) BEGIN SELECT RAISE(ABORT,'R82_ACTIVE_CALIBRATION_CHAMPION_PIN'); END;

-- r82_calibration_pin_research_fixed_champion_r932
CREATE TRIGGER r82_calibration_pin_research_fixed_champion_r932 BEFORE DELETE ON research_fixed_champion_r932 WHEN EXISTS(SELECT 1 FROM research_candidate_cal_observation_r932 o JOIN research_candidate_cal_study_r932 s ON s.study_hash=o.study_hash WHERE o.target_issue=OLD.target_issue AND s.state IN('COLLECTING','READY')) BEGIN SELECT RAISE(ABORT,'R82_ACTIVE_CALIBRATION_CHAMPION_PIN'); END;

-- r82_champion_p_no_replace
CREATE TRIGGER r82_champion_p_no_replace BEFORE INSERT ON research_champion_probability_r82 WHEN EXISTS(SELECT 1 FROM research_champion_probability_r82 x WHERE x.target_issue=NEW.target_issue AND x.bundle_hash=NEW.bundle_hash) BEGIN SELECT RAISE(ABORT,'R82_CHAMPION_PROBABILITY_REPLACE'); END;

-- r82_champion_p_no_update
CREATE TRIGGER r82_champion_p_no_update BEFORE UPDATE ON research_champion_probability_r82 BEGIN SELECT RAISE(ABORT,'R82_CHAMPION_PROBABILITY_IMMUTABLE'); END;

-- r82_champion_probability_archive_guard
CREATE TRIGGER r82_champion_probability_archive_guard BEFORE DELETE ON research_champion_probability_r82 WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_champion_probability_r82' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_champion_probability_r82' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R82_CHAMPION_PROBABILITY_VERIFIED_ARCHIVE_REQUIRED'); END;

-- r82_champion_probability_revision_delete
CREATE TRIGGER r82_champion_probability_revision_delete AFTER DELETE ON research_champion_probability_r82 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_champion_probability_r82' AND source_rowid=OLD.rowid; END;

-- r82_champion_probability_revision_insert
CREATE TRIGGER r82_champion_probability_revision_insert AFTER INSERT ON research_champion_probability_r82 BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_champion_probability_r82',NEW.rowid); END;

-- r932_look_delete
CREATE TRIGGER r932_look_delete BEFORE DELETE ON research_alpha_look_r932 BEGIN SELECT RAISE(ABORT,'R932_LOOK_DENOMINATOR_PERMANENT'); END;

-- r932_look_identity
CREATE TRIGGER r932_look_identity BEFORE UPDATE OF window_id,hypothesis_hash,window_no,first_issue,last_issue,alpha ON research_alpha_look_r932 BEGIN SELECT RAISE(ABORT,'R932_LOOK_IDENTITY_IMMUTABLE'); END;

-- r932_look_terminal
CREATE TRIGGER r932_look_terminal BEFORE UPDATE ON research_alpha_look_r932 WHEN OLD.state<>'REGISTERED' BEGIN SELECT RAISE(ABORT,'R932_TERMINAL_LOOK_IMMUTABLE'); END;

-- r948_roster_commitment_immutable
CREATE TRIGGER r948_roster_commitment_immutable BEFORE UPDATE ON research_roster_manifest_r948 BEGIN SELECT RAISE(ABORT,'R948_ROSTER_COMMITMENT_IMMUTABLE'); END;

-- r948_roster_payload_immutable
CREATE TRIGGER r948_roster_payload_immutable BEFORE UPDATE OF target_issue,input_hash,payload,payload_hash ON research_roster_prepare_r943 BEGIN SELECT RAISE(ABORT,'R948_ROSTER_PAYLOAD_IMMUTABLE'); END;

-- r952_artifact
CREATE TABLE r952_artifact(artifact_hash TEXT PRIMARY KEY,candidate_id TEXT NOT NULL,parent_hash TEXT NOT NULL,target_issue TEXT NOT NULL,delivery_hash TEXT NOT NULL UNIQUE,payload_hash TEXT NOT NULL,created_at INTEGER NOT NULL, model_payload TEXT NOT NULL DEFAULT '', numeric_updates INTEGER NOT NULL DEFAULT 0, training_kind TEXT NOT NULL DEFAULT 'LEGACY_ONLINE', development_receipt_hash TEXT NOT NULL DEFAULT '', development_sample_n INTEGER NOT NULL DEFAULT 0, validation_sample_n INTEGER NOT NULL DEFAULT 0, validation_pass INTEGER NOT NULL DEFAULT 0, validation_hash TEXT NOT NULL DEFAULT '');

-- r952_candidate
CREATE TABLE r952_candidate(candidate_id TEXT PRIMARY KEY,intent_id TEXT NOT NULL,ast TEXT NOT NULL,structural_hash TEXT NOT NULL UNIQUE,parent_id TEXT NOT NULL DEFAULT '',state TEXT NOT NULL,future_n INTEGER NOT NULL DEFAULT 0,created_issue TEXT NOT NULL,may_create_real INTEGER NOT NULL CHECK(may_create_real=0),created_at INTEGER NOT NULL, semantic_hash TEXT NOT NULL DEFAULT '');

-- r952_candidate_funnel
CREATE TABLE r952_candidate_funnel(target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,stage TEXT NOT NULL,stage_hash TEXT NOT NULL,reason TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_id,stage));

-- r952_candidate_predraw
CREATE TABLE r952_candidate_predraw(target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,prediction_mask INTEGER NOT NULL,position INTEGER NOT NULL CHECK(position>=1 AND position<=10),snapshot_hash TEXT NOT NULL,structure_hash TEXT NOT NULL,model_hash TEXT NOT NULL,code_hash TEXT NOT NULL,data_hash TEXT NOT NULL,captured_at INTEGER NOT NULL,settled INTEGER NOT NULL DEFAULT 0,actual_number INTEGER NOT NULL DEFAULT 0,hit INTEGER NOT NULL DEFAULT -1,settlement_hash TEXT NOT NULL DEFAULT '',label_hash TEXT NOT NULL DEFAULT '', frozen_features TEXT NOT NULL DEFAULT '', input_hash TEXT NOT NULL DEFAULT '', evidence_role TEXT NOT NULL DEFAULT 'LEGACY_UNTRAINABLE', proof_error TEXT NOT NULL DEFAULT '',PRIMARY KEY(target_issue,candidate_id));

-- r952_causal_edge
CREATE TABLE r952_causal_edge(edge_hash TEXT PRIMARY KEY,source TEXT NOT NULL,target TEXT NOT NULL,relation_type TEXT NOT NULL,temporal_direction TEXT NOT NULL,hypothesis_hash TEXT NOT NULL,evidence_hash TEXT NOT NULL,confidence REAL NOT NULL,status TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- r952_intent
CREATE TABLE r952_intent(intent_id TEXT PRIMARY KEY,problem_type TEXT NOT NULL,parent_candidates TEXT NOT NULL,evidence_deficit TEXT NOT NULL,target_metrics TEXT NOT NULL,allowed_operators TEXT NOT NULL,budget INTEGER NOT NULL,created_issue TEXT NOT NULL,intent_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL);

-- r952_knowledge
CREATE TABLE r952_knowledge(knowledge_hash TEXT PRIMARY KEY,target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,kind TEXT NOT NULL,payload_hash TEXT NOT NULL,created_at INTEGER NOT NULL, payload TEXT NOT NULL DEFAULT '', payload_format TEXT NOT NULL DEFAULT 'LEGACY_HASH_ONLY');

-- r952_knowledge_admission_delete
CREATE TRIGGER r952_knowledge_admission_delete BEFORE DELETE ON r952_knowledge WHEN (OLD.kind='ATOM_ADMISSION') AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='r952_knowledge' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R958_WORLD_ADMISSION_PERMANENT'); END;

-- r952_knowledge_immutable_update
CREATE TRIGGER r952_knowledge_immutable_update BEFORE UPDATE ON r952_knowledge BEGIN SELECT RAISE(ABORT,'R958_KNOWLEDGE_IMMUTABLE'); END;

-- r952_learning_cursor
CREATE TABLE r952_learning_cursor(consumer_id TEXT PRIMARY KEY,trained_until_issue TEXT NOT NULL DEFAULT '',artifact_hash TEXT NOT NULL DEFAULT '',last_delivery_hash TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL);

-- r952_matrix_feedback
CREATE TABLE r952_matrix_feedback(target_issue TEXT PRIMARY KEY,snapshot_hash TEXT NOT NULL,settlement_hash TEXT NOT NULL,label_hash TEXT NOT NULL,effective_from_issue TEXT NOT NULL,actual_numbers TEXT NOT NULL,feedback_hash TEXT NOT NULL UNIQUE,frozen_at INTEGER NOT NULL,completed_at INTEGER NOT NULL);

-- r952_matrix_feedback_immutable_update
CREATE TRIGGER r952_matrix_feedback_immutable_update BEFORE UPDATE ON r952_matrix_feedback BEGIN SELECT RAISE(ABORT,'R958_MATRIX_EVIDENCE_IMMUTABLE'); END;

-- r952_matrix_snapshot
CREATE TABLE r952_matrix_snapshot(target_issue TEXT PRIMARY KEY,snapshot_hash TEXT NOT NULL,feature_hash TEXT NOT NULL,payload_hash TEXT NOT NULL,state TEXT NOT NULL,created_at INTEGER NOT NULL);

-- r952_matrix_snapshot_immutable_update
CREATE TRIGGER r952_matrix_snapshot_immutable_update BEFORE UPDATE ON r952_matrix_snapshot BEGIN SELECT RAISE(ABORT,'R958_MATRIX_EVIDENCE_IMMUTABLE'); END;

-- r952_module_registry
CREATE TABLE r952_module_registry(module_id TEXT PRIMARY KEY,revision TEXT NOT NULL,extension_point TEXT NOT NULL,owner TEXT NOT NULL,contract_hash TEXT NOT NULL,may_create_real INTEGER NOT NULL CHECK(may_create_real=0),updated_at INTEGER NOT NULL);

-- r952_pareto
CREATE TABLE r952_pareto(candidate_id TEXT PRIMARY KEY,quality REAL NOT NULL,calibration REAL NOT NULL,stability REAL NOT NULL,diversity REAL NOT NULL,latency REAL NOT NULL,memory REAL NOT NULL,evidence_n INTEGER NOT NULL,score_hash TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- r952_recovery_journal
CREATE TABLE r952_recovery_journal(delivery_hash TEXT NOT NULL,attempt INTEGER NOT NULL,state TEXT NOT NULL,error_code TEXT NOT NULL DEFAULT '',event_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(delivery_hash,attempt,state));

-- r952_research_outbox
CREATE TABLE r952_research_outbox(delivery_hash TEXT PRIMARY KEY,target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,settlement_hash TEXT NOT NULL,label_hash TEXT NOT NULL,model_input_hash TEXT NOT NULL DEFAULT '',event_type TEXT NOT NULL,state TEXT NOT NULL,attempt_count INTEGER NOT NULL DEFAULT 0,lease_until INTEGER NOT NULL DEFAULT 0,last_error TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL, lease_owner TEXT NOT NULL DEFAULT '');

-- r952_runtime_event
CREATE TABLE r952_runtime_event(event_hash TEXT PRIMARY KEY,target_issue TEXT NOT NULL,event_type TEXT NOT NULL,entity_id TEXT NOT NULL DEFAULT '',payload_hash TEXT NOT NULL,source_commit TEXT NOT NULL,created_at INTEGER NOT NULL);

-- r952_world_atom
CREATE TABLE r952_world_atom(atom_id TEXT PRIMARY KEY,type TEXT NOT NULL,domain TEXT NOT NULL,producer TEXT NOT NULL,value_schema TEXT NOT NULL,pit_contract TEXT NOT NULL,lineage_hash TEXT NOT NULL,cost_class TEXT NOT NULL,revision TEXT NOT NULL,atom_hash TEXT NOT NULL UNIQUE);

-- r952_world_atom_immutable_delete
CREATE TRIGGER r952_world_atom_immutable_delete BEFORE DELETE ON r952_world_atom BEGIN SELECT RAISE(ABORT,'R958_WORLD_LINEAGE_PERMANENT'); END;

-- r952_world_atom_immutable_update
CREATE TRIGGER r952_world_atom_immutable_update BEFORE UPDATE ON r952_world_atom BEGIN SELECT RAISE(ABORT,'R958_WORLD_IDENTITY_IMMUTABLE'); END;

-- r952_world_molecule
CREATE TABLE r952_world_molecule(molecule_id TEXT PRIMARY KEY,operator TEXT NOT NULL,input_ids TEXT NOT NULL,structural_hash TEXT NOT NULL UNIQUE,semantic_hash TEXT NOT NULL,depth INTEGER NOT NULL,cost INTEGER NOT NULL, parameters TEXT NOT NULL DEFAULT '', compiled_hash TEXT NOT NULL DEFAULT '', resolved_semantic_hash TEXT NOT NULL DEFAULT '', canonical_version TEXT NOT NULL DEFAULT 'LEGACY_DESCRIPTOR');

-- r952_world_molecule_immutable_delete
CREATE TRIGGER r952_world_molecule_immutable_delete BEFORE DELETE ON r952_world_molecule BEGIN SELECT RAISE(ABORT,'R958_WORLD_LINEAGE_PERMANENT'); END;

-- r952_world_molecule_immutable_update
CREATE TRIGGER r952_world_molecule_immutable_update BEFORE UPDATE ON r952_world_molecule BEGIN SELECT RAISE(ABORT,'R958_WORLD_IDENTITY_IMMUTABLE'); END;

-- r958_frozen_candidate
CREATE TABLE r958_frozen_candidate(candidate_id TEXT PRIMARY KEY,learner_id TEXT NOT NULL UNIQUE,ast TEXT NOT NULL,model_payload TEXT NOT NULL,structural_hash TEXT NOT NULL UNIQUE,model_hash TEXT NOT NULL,trained_until_issue TEXT NOT NULL,created_issue TEXT NOT NULL,source_artifact TEXT NOT NULL,created_at INTEGER NOT NULL);

-- r958_frozen_model_delete
CREATE TRIGGER r958_frozen_model_delete BEFORE DELETE ON r958_frozen_candidate BEGIN SELECT RAISE(ABORT,'R958_FROZEN_EVIDENCE_PERMANENT'); END;

-- r958_frozen_model_update
CREATE TRIGGER r958_frozen_model_update BEFORE UPDATE ON r958_frozen_candidate BEGIN SELECT RAISE(ABORT,'R958_FROZEN_MODEL_IMMUTABLE'); END;

-- r958_frozen_predraw
CREATE TABLE r958_frozen_predraw(target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,record_hash TEXT NOT NULL,input_hash TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,score REAL NOT NULL,captured_at INTEGER NOT NULL,settled INTEGER NOT NULL DEFAULT 0,hit INTEGER NOT NULL DEFAULT -1,label_hash TEXT NOT NULL DEFAULT '',settlement_hash TEXT NOT NULL DEFAULT '',proof_error TEXT NOT NULL DEFAULT '', frozen_context TEXT NOT NULL DEFAULT '', context_hash TEXT NOT NULL DEFAULT '',PRIMARY KEY(target_issue,candidate_id));

-- r959_schedule_mode_immutable
CREATE TRIGGER r959_schedule_mode_immutable BEFORE UPDATE ON research_schedule_task_r932 BEGIN SELECT RAISE(ABORT,'R959_SCHEDULE_IMMUTABLE'); END;

-- r967_frozen_candidate_all
CREATE VIEW r967_frozen_candidate_all AS SELECT candidate_id,learner_id,ast,model_payload,structural_hash,model_hash,trained_until_issue,created_issue,source_artifact,created_at,'' AS run_hash,'' AS rule_hash,'' AS integrity_epoch,0 AS update_endpoint FROM r958_frozen_candidate UNION ALL SELECT candidate_id,learner_id,ast,model_payload,structural_hash,model_hash,trained_until_issue,created_issue,source_artifact,created_at,run_hash,rule_hash,integrity_epoch,update_endpoint FROM r967_frozen_evaluation_v2;

-- r967_frozen_evaluation_v2
CREATE TABLE r967_frozen_evaluation_v2(candidate_id TEXT PRIMARY KEY,learner_id TEXT NOT NULL,ast TEXT NOT NULL,model_payload TEXT NOT NULL,structural_hash TEXT NOT NULL UNIQUE,model_hash TEXT NOT NULL,trained_until_issue TEXT NOT NULL,created_issue TEXT NOT NULL,source_artifact TEXT NOT NULL,created_at INTEGER NOT NULL,run_hash TEXT NOT NULL,rule_hash TEXT NOT NULL,integrity_epoch TEXT NOT NULL,update_endpoint INTEGER NOT NULL,UNIQUE(run_hash,update_endpoint));

-- r967_frozen_evaluation_v2_no_delete
CREATE TRIGGER r967_frozen_evaluation_v2_no_delete BEFORE DELETE ON r967_frozen_evaluation_v2 BEGIN SELECT RAISE(ABORT,'R969_STREAM_PROVENANCE_PERMANENT'); END;

-- r967_frozen_evaluation_v2_no_update
CREATE TRIGGER r967_frozen_evaluation_v2_no_update BEFORE UPDATE ON r967_frozen_evaluation_v2 BEGIN SELECT RAISE(ABORT,'R969_STREAM_RECORD_IMMUTABLE'); END;

-- r967_input_pin_active_delete
CREATE TRIGGER r967_input_pin_active_delete
 BEFORE DELETE ON v57_research_input_pin_r967
 WHEN EXISTS(SELECT 1 FROM v57_research_task_r967 t WHERE t.task_id=OLD.task_id
 AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))
 AND NOT EXISTS(SELECT 1 FROM research_task_input_binding_v2 b
 JOIN research_evidence_archive_v2 a ON a.archive_id=b.archive_id
 WHERE b.task_id=OLD.task_id AND b.source_table=OLD.source_table
 AND b.source_issue=OLD.source_issue AND b.source_key_hash='ISSUE_COVERAGE_V1'
 AND b.archive_hash=a.archive_hash AND b.member_manifest_hash=a.manifest_hash
 AND a.state IN('VERIFIED','REFERENCED','PRUNED'))
 BEGIN SELECT RAISE(ABORT,'R967_PENDING_INPUT_PROTECTED'); END;

-- r967_input_pin_identity
CREATE TRIGGER r967_input_pin_identity BEFORE UPDATE ON v57_research_input_pin_r967 BEGIN SELECT RAISE(ABORT,'R967_PIN_IMMUTABLE'); END;

-- r967_numeric_training_input
CREATE TABLE r967_numeric_training_input(learner_id TEXT NOT NULL,target_issue TEXT NOT NULL,run_hash TEXT NOT NULL,input_hash TEXT NOT NULL,model_hash TEXT NOT NULL,record_hash TEXT NOT NULL,PRIMARY KEY(learner_id,target_issue));

-- r967_numeric_training_input_no_delete
CREATE TRIGGER r967_numeric_training_input_no_delete BEFORE DELETE ON r967_numeric_training_input  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='r967_numeric_training_input' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R969_STREAM_PROVENANCE_PERMANENT'); END;

-- r967_numeric_training_input_no_update
CREATE TRIGGER r967_numeric_training_input_no_update BEFORE UPDATE ON r967_numeric_training_input BEGIN SELECT RAISE(ABORT,'R969_STREAM_RECORD_IMMUTABLE'); END;

-- r967_numeric_training_run
CREATE TABLE r967_numeric_training_run(run_hash TEXT PRIMARY KEY,learner_id TEXT NOT NULL,first_issue TEXT NOT NULL,ast_hash TEXT NOT NULL,rule_hash TEXT NOT NULL,initial_payload TEXT NOT NULL,initial_updates INTEGER NOT NULL,source_artifact TEXT NOT NULL,split_rule TEXT NOT NULL,integrity_epoch TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(learner_id,first_issue));

-- r967_numeric_training_run_no_delete
CREATE TRIGGER r967_numeric_training_run_no_delete BEFORE DELETE ON r967_numeric_training_run BEGIN SELECT RAISE(ABORT,'R969_STREAM_PROVENANCE_PERMANENT'); END;

-- r967_numeric_training_run_no_update
CREATE TRIGGER r967_numeric_training_run_no_update BEFORE UPDATE ON r967_numeric_training_run BEGIN SELECT RAISE(ABORT,'R969_STREAM_RECORD_IMMUTABLE'); END;

-- r967_plan_no_replace
CREATE TRIGGER r967_plan_no_replace BEFORE INSERT ON research_experiment_plan_r967 WHEN EXISTS(SELECT 1 FROM research_experiment_plan_r967 WHERE window_id=NEW.window_id OR plan_hash=NEW.plan_hash) BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- r967_planned_window_delete
CREATE TRIGGER r967_planned_window_delete BEFORE DELETE ON research_fixed_window_r932 WHEN (OLD.formal_plan_revision='R967_FORMAL_PLAN_V1' OR EXISTS(SELECT 1 FROM research_plan_required_r967 r WHERE r.window_id=OLD.window_id)) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_fixed_window_r932' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R967_PLANNED_WINDOW_PERMANENT'); END;

-- r967_planned_window_identity
CREATE TRIGGER r967_planned_window_identity BEFORE UPDATE OF window_id,candidate_key,structural_hash,champion_hash,selection_hash,first_issue,last_issue,planned_n,window_no,registered_at,formal_plan_revision ON research_fixed_window_r932 WHEN OLD.formal_plan_revision='R967_FORMAL_PLAN_V1' OR EXISTS(SELECT 1 FROM research_plan_required_r967 r WHERE r.window_id=OLD.window_id) BEGIN SELECT RAISE(ABORT,'R967_PLANNED_WINDOW_IDENTITY_IMMUTABLE'); END;

-- r967_policy_no_replace
CREATE TRIGGER r967_policy_no_replace BEFORE INSERT ON research_gate_policy_r967 WHEN EXISTS(SELECT 1 FROM research_gate_policy_r967 WHERE policy_hash=NEW.policy_hash) BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- r967_required_no_replace
CREATE TRIGGER r967_required_no_replace BEFORE INSERT ON research_plan_required_r967 WHEN EXISTS(SELECT 1 FROM research_plan_required_r967 WHERE window_id=NEW.window_id) BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- r967_research_identity_delete
CREATE TRIGGER r967_research_identity_delete BEFORE DELETE ON research_identity_r967 BEGIN SELECT RAISE(ABORT,'R967_RESEARCH_IDENTITY_IMMUTABLE'); END;

-- r967_research_identity_update
CREATE TRIGGER r967_research_identity_update BEFORE UPDATE ON research_identity_r967 BEGIN SELECT RAISE(ABORT,'R967_RESEARCH_IDENTITY_IMMUTABLE'); END;

-- r967_task_active_delete
CREATE TRIGGER r967_task_active_delete BEFORE DELETE ON v57_research_task_r967 WHEN (OLD.status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_research_task_r967' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R967_PENDING_TASK_PROTECTED'); END;

-- r967_task_identity
CREATE TRIGGER r967_task_identity BEFORE UPDATE OF task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,created_at ON v57_research_task_r967 BEGIN SELECT RAISE(ABORT,'R967_TASK_IDENTITY_IMMUTABLE'); END;

-- r967_task_terminal
CREATE TRIGGER r967_task_terminal BEFORE UPDATE ON v57_research_task_r967 WHEN OLD.status IN ('COMPLETED','EXPIRED','FAILED_TERMINAL') BEGIN SELECT RAISE(ABORT,'R967_TASK_TERMINAL_IMMUTABLE'); END;

-- r967_window_no_replace
CREATE TRIGGER r967_window_no_replace BEFORE INSERT ON research_fixed_window_r932 WHEN EXISTS(SELECT 1 FROM research_fixed_window_r932 w WHERE w.formal_plan_revision='R967_FORMAL_PLAN_V1' AND (w.window_id=NEW.window_id OR (w.candidate_key=NEW.candidate_key AND w.window_no=NEW.window_no))) BEGIN SELECT RAISE(ABORT,'R967_PLANNED_WINDOW_PERMANENT'); END;

-- r969_eval_no_replace
CREATE TRIGGER r969_eval_no_replace BEFORE INSERT ON r967_frozen_evaluation_v2 WHEN EXISTS(SELECT 1 FROM r967_frozen_evaluation_v2 WHERE candidate_id=NEW.candidate_id OR structural_hash=NEW.structural_hash OR (run_hash=NEW.run_hash AND update_endpoint=NEW.update_endpoint)) BEGIN SELECT RAISE(ABORT,'R969_EVAL_DUPLICATE'); END;

-- r969_input_no_replace
CREATE TRIGGER r969_input_no_replace BEFORE INSERT ON r967_numeric_training_input WHEN EXISTS(SELECT 1 FROM r967_numeric_training_input WHERE learner_id=NEW.learner_id AND target_issue=NEW.target_issue) BEGIN SELECT RAISE(ABORT,'R969_INPUT_DUPLICATE'); END;

-- r969_run_no_replace
CREATE TRIGGER r969_run_no_replace BEFORE INSERT ON r967_numeric_training_run WHEN EXISTS(SELECT 1 FROM r967_numeric_training_run WHERE run_hash=NEW.run_hash OR (learner_id=NEW.learner_id AND first_issue=NEW.first_issue)) BEGIN SELECT RAISE(ABORT,'R969_RUN_DUPLICATE'); END;

-- r974_computation_receipt
CREATE TABLE r974_computation_receipt(target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,ast_hash TEXT NOT NULL,model_hash TEXT NOT NULL,predraw_input_hash TEXT NOT NULL,world_input_hash TEXT NOT NULL,world_output_hash TEXT NOT NULL,features TEXT NOT NULL,prediction_mask INTEGER NOT NULL,trace TEXT NOT NULL,receipt_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_id));

-- r974_compute_no_update
CREATE TRIGGER r974_compute_no_update BEFORE UPDATE ON r974_computation_receipt BEGIN SELECT RAISE(ABORT,'R974_COMPUTATION_IMMUTABLE'); END;

-- r974_exact_real_receipt
CREATE TABLE r974_exact_real_receipt(target_issue TEXT PRIMARY KEY,trade_id INTEGER NOT NULL,candidate_id TEXT NOT NULL,model_hash TEXT NOT NULL,certificate_hash TEXT NOT NULL,proof_hash TEXT NOT NULL,recipe_hash TEXT NOT NULL,action_hash TEXT NOT NULL,authorization_hash TEXT NOT NULL,committed_at INTEGER NOT NULL);

-- r974_lineage_no_delete
CREATE TRIGGER r974_lineage_no_delete BEFORE DELETE ON r974_model_lineage BEGIN SELECT RAISE(ABORT,'R974_LINEAGE_PERMANENT'); END;

-- r974_lineage_no_update
CREATE TRIGGER r974_lineage_no_update BEFORE UPDATE ON r974_model_lineage BEGIN SELECT RAISE(ABORT,'R974_LINEAGE_IMMUTABLE'); END;

-- r974_model_lineage
CREATE TABLE r974_model_lineage(child_id TEXT NOT NULL,parent_id TEXT NOT NULL,parent_role TEXT NOT NULL,parent_ast TEXT NOT NULL,evidence_hash TEXT NOT NULL,created_issue TEXT NOT NULL,PRIMARY KEY(child_id,parent_id));

-- r974_prediction_rejection
CREATE TABLE r974_prediction_rejection(target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,certificate_hash TEXT NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_id,certificate_hash));

-- r974_real_no_update
CREATE TRIGGER r974_real_no_update BEFORE UPDATE ON r974_exact_real_receipt BEGIN SELECT RAISE(ABORT,'R974_REAL_RECEIPT_IMMUTABLE'); END;

-- r975_calibration_failure
CREATE TABLE r975_calibration_failure(study_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL);

-- r975_evolution_identity
CREATE TRIGGER r975_evolution_identity BEFORE UPDATE OF review_id,parent_id,ast_hash,endpoint,after_child_issue ON r975_evolution_review BEGIN SELECT RAISE(ABORT,'R975_EVOLUTION_REVIEW_IDENTITY_IMMUTABLE'); END;

-- r975_evolution_review
CREATE TABLE r975_evolution_review(review_id TEXT PRIMARY KEY,parent_id TEXT NOT NULL,ast_hash TEXT NOT NULL,endpoint TEXT NOT NULL,after_child_issue TEXT NOT NULL,cursor_issue TEXT NOT NULL,verified_n INTEGER NOT NULL DEFAULT 0,recent_misses INTEGER NOT NULL DEFAULT 0,earlier_misses INTEGER NOT NULL DEFAULT 0,payload TEXT NOT NULL,evidence_hash TEXT NOT NULL,state TEXT NOT NULL CHECK(state IN ('ACTIVE','READY','CONSUMED','BLOCKED')),reason TEXT NOT NULL DEFAULT '',child_id TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL);

-- r975_evolution_terminal
CREATE TRIGGER r975_evolution_terminal BEFORE UPDATE ON r975_evolution_review WHEN OLD.state IN ('CONSUMED','BLOCKED') BEGIN SELECT RAISE(ABORT,'R975_EVOLUTION_TERMINAL_IMMUTABLE'); END;

-- r979_calibration_head
CREATE TABLE r979_calibration_head(base_study TEXT PRIMARY KEY,current_study TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- r979_calibration_restart
CREATE TABLE r979_calibration_restart(new_study TEXT PRIMARY KEY,base_study TEXT NOT NULL,previous_study TEXT NOT NULL,candidate_key TEXT NOT NULL,first_issue TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL);

-- real_trade_ledger
CREATE TABLE real_trade_ledger (_id INTEGER PRIMARY KEY AUTOINCREMENT, ledger_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, settled_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, grade TEXT, action TEXT, stat_scope TEXT, source_type TEXT, is_realtime_real INTEGER DEFAULT 0, result TEXT DEFAULT 'WAIT', actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, pnl REAL DEFAULT 0, same_class_key TEXT, chain_key TEXT, decision_audit_key TEXT, settlement_reason TEXT, snapshot_key TEXT, candidate_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- realtime_validate
CREATE TABLE realtime_validate (_id INTEGER PRIMARY KEY AUTOINCREMENT, report_id TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, started_at INTEGER DEFAULT 0, finished_at INTEGER DEFAULT 0, created_at INTEGER DEFAULT 0, cost_ms INTEGER DEFAULT 0, window_size INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, total_count INTEGER DEFAULT 0, bet_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hold_count INTEGER DEFAULT 0, shadow_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, win_rate REAL DEFAULT 0, frequency REAL DEFAULT 0, pnl REAL DEFAULT 0, max_drawdown REAL DEFAULT 0, longest_lose INTEGER DEFAULT 0, recent_lose_streak INTEGER DEFAULT 0, avg_final_p REAL DEFAULT 0, avg_gate REAL DEFAULT 0, avg_edge REAL DEFAULT 0, avg_drift_risk REAL DEFAULT 0, avg_break_depth REAL DEFAULT 0, avg_trend_life REAL DEFAULT 0, avg_recovery_score REAL DEFAULT 0, sample_weak INTEGER DEFAULT 0, freq_high INTEGER DEFAULT 0, freq_hard INTEGER DEFAULT 0, hit_weak INTEGER DEFAULT 0, recent_miss_warn INTEGER DEFAULT 0, drawdown_warn INTEGER DEFAULT 0, stable_good INTEGER DEFAULT 0, gate_adjust REAL DEFAULT 0, recommend_gate_adjust REAL DEFAULT 0, ok INTEGER DEFAULT 0, usable INTEGER DEFAULT 0, status TEXT, summary TEXT, detail TEXT, warning_text TEXT, source TEXT, teacher_matrix_sample_count INTEGER DEFAULT 0, teacher_matrix_used_count INTEGER DEFAULT 0, teacher_matrix_avg_confidence REAL DEFAULT 0, teacher_matrix_summary TEXT, reason_sample_count INTEGER DEFAULT 0, elite45_pass_count INTEGER DEFAULT 0, reason_avg_edge REAL DEFAULT 0, reason_summary TEXT);

-- reason_causal_profile
CREATE TABLE reason_causal_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, causal_key TEXT NOT NULL UNIQUE, reason_atom TEXT, reason_combo_key TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, forward_sample INTEGER DEFAULT 0, forward_hit INTEGER DEFAULT 0, forward_miss INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, forward_hit_rate REAL DEFAULT 0, baseline_hit_rate REAL DEFAULT 0, causal_lift REAL DEFAULT 0, causal_confidence REAL DEFAULT 0, decay_score REAL DEFAULT 0, risk_score REAL DEFAULT 0, prob_adjust REAL DEFAULT 0, gate_adjust REAL DEFAULT 0, valid_level TEXT, summary_text TEXT, updated_at INTEGER DEFAULT 0);

-- reason_contrast_profile
CREATE TABLE reason_contrast_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE, reason_fingerprint TEXT, state_key TEXT, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, recent_miss INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, miss_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, reason_edge REAL DEFAULT 0, hit_similarity REAL DEFAULT 0, miss_similarity REAL DEFAULT 0, contradiction_score REAL DEFAULT 0, wilson_score REAL DEFAULT 0, confidence REAL DEFAULT 0, valid_level TEXT, why_text TEXT, summary_text TEXT, updated_at INTEGER DEFAULT 0);

-- research_action_boundary_cp12_no_delete
CREATE TRIGGER research_action_boundary_cp12_no_delete BEFORE DELETE ON research_action_input_boundary_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_INPUT_BOUNDARY_PERMANENT'); END;

-- research_action_boundary_cp12_no_replace
CREATE TRIGGER research_action_boundary_cp12_no_replace BEFORE INSERT ON research_action_input_boundary_cp12 WHEN EXISTS(SELECT 1 FROM research_action_input_boundary_cp12 WHERE action_id=NEW.action_id) BEGIN SELECT RAISE(ABORT,'RESEARCH_INPUT_BOUNDARY_REPLACE_FORBIDDEN'); END;

-- research_action_boundary_cp12_no_update
CREATE TRIGGER research_action_boundary_cp12_no_update BEFORE UPDATE ON research_action_input_boundary_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_INPUT_BOUNDARY_IMMUTABLE'); END;

-- research_action_input_boundary_cp12
CREATE TABLE research_action_input_boundary_cp12(
 action_id TEXT PRIMARY KEY REFERENCES research_action_v2(action_id),source_issue TEXT NOT NULL,
 selection_boundary TEXT NOT NULL,evidence_hash TEXT NOT NULL CHECK(length(evidence_hash)=64),
 boundary_hash TEXT NOT NULL UNIQUE CHECK(length(boundary_hash)=64),created_at INTEGER NOT NULL);

-- research_action_proposal_cp12
CREATE TABLE research_action_proposal_cp12(
 action_id TEXT PRIMARY KEY REFERENCES research_action_v2(action_id),parent_lifetime_id TEXT NOT NULL REFERENCES research_cp4_frozen_lifetime(lifetime_id),ast_hash TEXT NOT NULL CHECK(length(ast_hash)=64),
 ast_json TEXT NOT NULL CHECK(length(ast_json) BETWEEN 1 AND 32768),attempt INTEGER NOT NULL CHECK(attempt BETWEEN 0 AND 31),
 proposal_hash TEXT NOT NULL UNIQUE CHECK(length(proposal_hash)=64),created_at INTEGER NOT NULL);

-- research_action_proposal_cp12_no_delete
CREATE TRIGGER research_action_proposal_cp12_no_delete BEFORE DELETE ON research_action_proposal_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_PROPOSAL_PERMANENT'); END;

-- research_action_proposal_cp12_no_replace
CREATE TRIGGER research_action_proposal_cp12_no_replace BEFORE INSERT ON research_action_proposal_cp12 WHEN EXISTS(SELECT 1 FROM research_action_proposal_cp12 WHERE action_id=NEW.action_id) BEGIN SELECT RAISE(ABORT,'RESEARCH_PROPOSAL_REPLACE_FORBIDDEN'); END;

-- research_action_proposal_cp12_no_update
CREATE TRIGGER research_action_proposal_cp12_no_update BEFORE UPDATE ON research_action_proposal_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_PROPOSAL_IMMUTABLE'); END;

-- research_action_result_binding_cp12
CREATE TABLE research_action_result_binding_cp12(
 action_id TEXT PRIMARY KEY REFERENCES research_action_v2(action_id),child_id TEXT NOT NULL REFERENCES r952_candidate(candidate_id),
 child_lifetime_id TEXT NOT NULL REFERENCES research_cp4_frozen_lifetime(lifetime_id),child_key TEXT NOT NULL,
 first_issue TEXT NOT NULL,comparator_kind TEXT NOT NULL CHECK(comparator_kind IN('PARENT_LIFETIME','FROZEN_CHAMPION')),
 parent_lifetime_id TEXT REFERENCES research_cp4_frozen_lifetime(lifetime_id),
 comparator_window_id TEXT REFERENCES research_cp4_window_binding(window_id),
 champion_bundle_hash TEXT,champion_selection_hash TEXT,champion_identity_hash TEXT,comparator_receipt_hash TEXT,
 binding_hash TEXT NOT NULL UNIQUE CHECK(length(binding_hash)=64),created_at INTEGER NOT NULL,
 CHECK((comparator_kind='PARENT_LIFETIME' AND parent_lifetime_id IS NOT NULL AND comparator_window_id IS NULL AND champion_bundle_hash IS NULL AND champion_selection_hash IS NULL AND champion_identity_hash IS NULL AND comparator_receipt_hash IS NULL)
 OR (comparator_kind='FROZEN_CHAMPION' AND parent_lifetime_id IS NULL AND comparator_window_id IS NOT NULL AND champion_bundle_hash IS NOT NULL AND length(champion_bundle_hash)=64 AND champion_selection_hash IS NOT NULL AND length(champion_selection_hash)=64 AND champion_identity_hash IS NOT NULL AND length(champion_identity_hash)=64 AND comparator_receipt_hash IS NOT NULL AND length(comparator_receipt_hash)=64)));

-- research_action_teacher_binding_cp12
CREATE TABLE research_action_teacher_binding_cp12(action_id TEXT PRIMARY KEY REFERENCES research_action_v2(action_id),source_issue TEXT NOT NULL,selection_boundary TEXT NOT NULL,boundary_hash TEXT NOT NULL CHECK(length(boundary_hash)=64),refs_json TEXT NOT NULL CHECK(length(CAST(refs_json AS BLOB)) BETWEEN 1 AND 4096),witnesses_json TEXT NOT NULL CHECK(length(CAST(witnesses_json AS BLOB)) BETWEEN 1 AND 16384),proof_hash TEXT NOT NULL UNIQUE CHECK(length(proof_hash)=64),created_at INTEGER NOT NULL);

-- research_action_teacher_witness_cp12
CREATE TABLE research_action_teacher_witness_cp12(action_id TEXT NOT NULL REFERENCES research_action_teacher_binding_cp12(action_id),teacher_freeze_hash TEXT NOT NULL CHECK(length(teacher_freeze_hash)=64),teacher_candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,record_hash TEXT NOT NULL CHECK(length(record_hash)=64),context_hash TEXT NOT NULL CHECK(length(context_hash)=64),witness_hash TEXT NOT NULL CHECK(length(witness_hash)=64),PRIMARY KEY(action_id,teacher_freeze_hash));

-- research_action_v2
CREATE TABLE research_action_v2(
 action_id TEXT PRIMARY KEY, finding_id TEXT NOT NULL REFERENCES research_finding_v2(finding_id),
 action_type TEXT NOT NULL CHECK(action_type IN ('MUTATE','CROSSOVER','FUSION','NEW_FAMILY','HOLD')),
 operator TEXT NOT NULL,proposal_json TEXT NOT NULL CHECK(length(proposal_json)<=65536),
 evidence_hash TEXT NOT NULL,
 seed INTEGER NOT NULL, parent_hashes_json TEXT NOT NULL, protocol_hash TEXT NOT NULL,
 planned_endpoint INTEGER NOT NULL CHECK(planned_endpoint>=0), alpha_identity TEXT NOT NULL,
 state TEXT NOT NULL CHECK(state IN ('PROPOSED','CREATED','NO_OP','REJECTED','WAIT_FUTURE','EVALUATED')),
 child_id TEXT, last_error TEXT NOT NULL DEFAULT '', UNIQUE(finding_id,protocol_hash,seed));

-- research_algorithm_head_archive_r948
CREATE TABLE research_algorithm_head_archive_r948(audit_hash TEXT PRIMARY KEY,genome_id TEXT NOT NULL,previous_head_json TEXT NOT NULL,previous_state_hash TEXT NOT NULL,new_state_hash TEXT NOT NULL,epoch_start_issue TEXT NOT NULL,invalid_n INTEGER NOT NULL,evidence_hash TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_algorithm_head_integrity_r948
CREATE TABLE research_algorithm_head_integrity_r948(genome_id TEXT PRIMARY KEY,status TEXT NOT NULL,cursor_issue TEXT NOT NULL,scanned_n INTEGER NOT NULL,invalid_n INTEGER NOT NULL,input_state_hash TEXT NOT NULL,evidence_hash TEXT NOT NULL,epoch_start_issue TEXT NOT NULL,reason TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- research_algorithm_proof_quarantine_r948
CREATE TABLE research_algorithm_proof_quarantine_r948(genome_id TEXT NOT NULL,invalid_issue TEXT NOT NULL,record_hash TEXT NOT NULL,original_hit INTEGER NOT NULL,original_outcome_hash TEXT NOT NULL,reason TEXT NOT NULL,detected_at INTEGER NOT NULL,PRIMARY KEY(genome_id,invalid_issue));

-- research_alpha_alias_r932
CREATE TABLE research_alpha_alias_r932(candidate_key TEXT PRIMARY KEY,hypothesis_hash TEXT NOT NULL);

-- research_alpha_alias_r932_immutable_delete
CREATE TRIGGER research_alpha_alias_r932_immutable_delete BEFORE DELETE ON research_alpha_alias_r932 BEGIN SELECT RAISE(ABORT,'R932_REGISTERED_DENOMINATOR_PERMANENT'); END;

-- research_alpha_alias_r932_immutable_update
CREATE TRIGGER research_alpha_alias_r932_immutable_update BEFORE UPDATE ON research_alpha_alias_r932 BEGIN SELECT RAISE(ABORT,'R932_REGISTERED_IDENTITY_IMMUTABLE'); END;

-- research_alpha_family_r932
CREATE TABLE research_alpha_family_r932(family TEXT PRIMARY KEY,ordinal INTEGER NOT NULL UNIQUE,alpha REAL NOT NULL,registered_issue TEXT NOT NULL);

-- research_alpha_family_r932_immutable_delete
CREATE TRIGGER research_alpha_family_r932_immutable_delete BEFORE DELETE ON research_alpha_family_r932 BEGIN SELECT RAISE(ABORT,'R932_REGISTERED_DENOMINATOR_PERMANENT'); END;

-- research_alpha_family_r932_immutable_update
CREATE TRIGGER research_alpha_family_r932_immutable_update BEFORE UPDATE ON research_alpha_family_r932 BEGIN SELECT RAISE(ABORT,'R932_REGISTERED_IDENTITY_IMMUTABLE'); END;

-- research_alpha_hypothesis_r932
CREATE TABLE research_alpha_hypothesis_r932(hypothesis_hash TEXT PRIMARY KEY,structure_hash TEXT NOT NULL UNIQUE,family TEXT NOT NULL,ordinal INTEGER NOT NULL,alpha REAL NOT NULL,registered_issue TEXT NOT NULL,UNIQUE(family,ordinal));

-- research_alpha_hypothesis_r932_immutable_delete
CREATE TRIGGER research_alpha_hypothesis_r932_immutable_delete BEFORE DELETE ON research_alpha_hypothesis_r932 BEGIN SELECT RAISE(ABORT,'R932_REGISTERED_DENOMINATOR_PERMANENT'); END;

-- research_alpha_hypothesis_r932_immutable_update
CREATE TRIGGER research_alpha_hypothesis_r932_immutable_update BEFORE UPDATE ON research_alpha_hypothesis_r932 BEGIN SELECT RAISE(ABORT,'R932_REGISTERED_IDENTITY_IMMUTABLE'); END;

-- research_alpha_look_r932
CREATE TABLE research_alpha_look_r932(window_id TEXT PRIMARY KEY,hypothesis_hash TEXT NOT NULL,window_no INTEGER NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,alpha REAL NOT NULL,state TEXT NOT NULL,result_hash TEXT NOT NULL,raw_p REAL,UNIQUE(hypothesis_hash,window_no));

-- research_artifact_identity_v2
CREATE TABLE research_artifact_identity_v2(
 artifact_hash TEXT PRIMARY KEY CHECK(length(artifact_hash)=64),
 candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,delivery_hash TEXT NOT NULL UNIQUE,
 payload_hash TEXT NOT NULL CHECK(length(payload_hash)=64),
 development_receipt_hash TEXT NOT NULL CHECK(length(development_receipt_hash)=64),
 validation_hash TEXT NOT NULL CHECK(length(validation_hash)=64),
 training_kind TEXT NOT NULL CHECK(training_kind='DEVELOPMENT_AST3'),
 development_sample_n INTEGER NOT NULL CHECK(development_sample_n BETWEEN 300 AND 1200),
 validation_sample_n INTEGER NOT NULL CHECK(validation_sample_n=120),
 validation_pass INTEGER NOT NULL CHECK(validation_pass=1),created_at INTEGER NOT NULL);

-- research_atom_admission_cp12
CREATE TABLE research_atom_admission_cp12(matrix_id TEXT PRIMARY KEY REFERENCES research_matrix_checkpoint_v2(matrix_id) ON DELETE CASCADE,manifest_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,version INTEGER NOT NULL CHECK(version>=0),payload_hash TEXT NOT NULL,through_issue TEXT NOT NULL,last_receipt TEXT NOT NULL,state TEXT NOT NULL CHECK(state IN ('OBSERVING','VALIDATED','DOWNWEIGHTED')),reason TEXT NOT NULL,expected INTEGER NOT NULL CHECK(expected BETWEEN 0 AND 300),n INTEGER NOT NULL CHECK(n BETWEEN 0 AND 300),observed INTEGER NOT NULL CHECK(observed BETWEEN 0 AND 300),coverage REAL NOT NULL CHECK(coverage BETWEEN 0 AND 1),availability REAL NOT NULL CHECK(availability BETWEEN 0 AND 1),mean REAL NOT NULL,se REAL NOT NULL CHECK(se>=0),lcb REAL NOT NULL,early_mean REAL NOT NULL,recent_mean REAL NOT NULL,assessment_hash TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- research_atom_audit_r932
CREATE TABLE research_atom_audit_r932(target_issue TEXT PRIMARY KEY,revision TEXT NOT NULL,context_hash TEXT NOT NULL,result TEXT NOT NULL,result_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_atom_binding_v2
CREATE TABLE research_atom_binding_v2(
 binding_hash TEXT PRIMARY KEY, atom_id TEXT NOT NULL REFERENCES research_atom_spec_v2(atom_id),
 candidate_version TEXT NOT NULL, model_hash TEXT NOT NULL, cohort_hash TEXT NOT NULL,
 created_issue TEXT NOT NULL, UNIQUE(atom_id,candidate_version,model_hash,cohort_hash));

-- research_atom_catalog_import_cp12
CREATE TABLE research_atom_catalog_import_cp12(
 asset_sha256 TEXT PRIMARY KEY CHECK(length(asset_sha256)=64),
 asset_name TEXT NOT NULL, expected_rows INTEGER NOT NULL CHECK(expected_rows>0), expected_bytes INTEGER NOT NULL CHECK(expected_bytes>0),
 next_row INTEGER NOT NULL DEFAULT 0 CHECK(next_row>=0 AND next_row<=expected_rows),
 next_offset INTEGER NOT NULL DEFAULT 0 CHECK(next_offset>=0 AND next_offset<=expected_bytes),
 prefix_sha256 TEXT NOT NULL CHECK(length(prefix_sha256)=64), sha_state BLOB NOT NULL CHECK(length(sha_state)=111),
 generation INTEGER NOT NULL DEFAULT 0 CHECK(generation>=0),
 state TEXT NOT NULL CHECK(state IN('IMPORTING','COMPLETE')), updated_at INTEGER NOT NULL,
 CHECK(state<>'COMPLETE' OR (next_row=expected_rows AND next_offset=expected_bytes AND prefix_sha256=asset_sha256)));

-- research_atom_context_r932
CREATE TABLE research_atom_context_r932(target_issue TEXT PRIMARY KEY,context_hash TEXT NOT NULL,max_source_issue TEXT NOT NULL,schema_hash TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL);

-- research_atom_quality_archive_r932
CREATE TABLE research_atom_quality_archive_r932(atom_id TEXT NOT NULL,generation INTEGER NOT NULL,quality_json TEXT NOT NULL,archived_at INTEGER NOT NULL,PRIMARY KEY(atom_id,generation));

-- research_atom_quality_r932
CREATE TABLE research_atom_quality_r932(atom_id TEXT PRIMARY KEY,n INTEGER NOT NULL,available_n INTEGER NOT NULL,mean REAL NOT NULL,m2 REAL NOT NULL,total_cost_ns INTEGER NOT NULL,max_cost_ns INTEGER NOT NULL,last_issue TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL);

-- research_atom_recovery_r932
CREATE TABLE research_atom_recovery_r932(atom_id TEXT PRIMARY KEY,revision TEXT NOT NULL,last_issue TEXT NOT NULL,pass_streak INTEGER NOT NULL,generation INTEGER NOT NULL,last_source_issue TEXT NOT NULL);

-- research_atom_schema_v41
CREATE TABLE research_atom_schema_v41(atom_id TEXT PRIMARY KEY,group_id TEXT NOT NULL,name TEXT NOT NULL UNIQUE,datatype TEXT NOT NULL,value_range TEXT NOT NULL,window_spec TEXT NOT NULL,max_lag INTEGER NOT NULL,min_support INTEGER NOT NULL,evidence_stage TEXT NOT NULL,leakage_class TEXT NOT NULL,update_timing TEXT NOT NULL,version INTEGER NOT NULL,cost_class TEXT NOT NULL,enabled INTEGER NOT NULL,semantic_digest TEXT NOT NULL UNIQUE);

-- research_atom_spec_v2
CREATE TABLE research_atom_spec_v2(
 atom_id TEXT PRIMARY KEY, family TEXT NOT NULL, spec_version TEXT NOT NULL,
 params_json TEXT NOT NULL, position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
 output_type TEXT NOT NULL, min_history INTEGER NOT NULL CHECK(min_history>=1),
 spec_hash TEXT NOT NULL UNIQUE, missing_policy TEXT NOT NULL,
 state TEXT NOT NULL CHECK(state IN ('DISCOVERED','REQUIRES_BINDING','OBSERVING','VALIDATED','ACTIVE','DOWNWEIGHTED','RETIRED')));

-- research_bound_cohort_r80
CREATE TABLE research_bound_cohort_r80(cohort_hash TEXT PRIMARY KEY,selection_json TEXT NOT NULL CHECK(length(CAST(selection_json AS BLOB))<=32768),ast TEXT NOT NULL CHECK(length(CAST(ast AS BLOB))<=32768),ast_hash TEXT NOT NULL,manifest_hash TEXT NOT NULL,source_issue TEXT NOT NULL,first_issue TEXT NOT NULL,registered_at INTEGER NOT NULL CHECK(registered_at>0),state TEXT NOT NULL CHECK(state IN('CAPTURING','READY','PROPOSED','REGISTERED','RETIRED')),next_issue TEXT NOT NULL,ready_n INTEGER NOT NULL DEFAULT 0 CHECK(ready_n BETWEEN 0 AND 421),selection_issue TEXT NOT NULL DEFAULT '',action_id TEXT NOT NULL DEFAULT '',child_id TEXT NOT NULL DEFAULT '',intent_id TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT 0,last_reason TEXT NOT NULL DEFAULT '');

-- research_bound_cohort_r80_no_replace
CREATE TRIGGER research_bound_cohort_r80_no_replace BEFORE INSERT ON research_bound_cohort_r80 WHEN EXISTS(SELECT 1 FROM research_bound_cohort_r80 x WHERE x.cohort_hash=NEW.cohort_hash OR (x.state IN ('CAPTURING','READY','PROPOSED','REGISTERED') AND NEW.state IN ('CAPTURING','READY','PROPOSED','REGISTERED') AND substr(x.ast,-26)=substr(NEW.ast,-26))) BEGIN SELECT RAISE(ABORT,'BOUND_REPLACE_FORBIDDEN'); END;

-- research_bound_cohort_r80_permanent
CREATE TRIGGER research_bound_cohort_r80_permanent BEFORE DELETE ON research_bound_cohort_r80 BEGIN SELECT RAISE(ABORT,'BOUND_ORIGINAL_PERMANENT'); END;

-- research_bound_input_r80
CREATE TABLE research_bound_input_r80(cohort_hash TEXT NOT NULL REFERENCES research_bound_cohort_r80(cohort_hash),target_issue TEXT NOT NULL,receipt_hash TEXT NOT NULL,payload TEXT NOT NULL CHECK(length(CAST(payload AS BLOB)) BETWEEN 1 AND 65535),captured_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,available INTEGER NOT NULL CHECK(available IN(0,1)),reason TEXT NOT NULL,PRIMARY KEY(cohort_hash,target_issue));

-- research_bound_input_r80_archive_guard
CREATE TRIGGER research_bound_input_r80_archive_guard BEFORE DELETE ON research_bound_input_r80 WHEN NOT COALESCE((OLD.target_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND EXISTS(SELECT 1 FROM research_bound_cohort_r80 c WHERE c.cohort_hash=OLD.cohort_hash AND (c.state IN('CAPTURING','REGISTERED','RETIRED')) AND NOT EXISTS(SELECT 1 FROM v57_research_task_r967 t WHERE t.candidate_id=c.child_id AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL')))),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_bound_input_r80' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_bound_input_r80' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'BOUND_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- research_bound_input_r80_archive_locator
CREATE TRIGGER research_bound_input_r80_archive_locator BEFORE DELETE ON research_bound_input_r80 BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_bound_input_r80' AND p.source_rowid=OLD.rowid; END;

-- research_bound_input_r80_immutable
CREATE TRIGGER research_bound_input_r80_immutable BEFORE UPDATE ON research_bound_input_r80 BEGIN SELECT RAISE(ABORT,'BOUND_RECEIPT_IMMUTABLE'); END;

-- research_bound_input_r80_no_replace
CREATE TRIGGER research_bound_input_r80_no_replace BEFORE INSERT ON research_bound_input_r80 WHEN EXISTS(SELECT 1 FROM research_bound_input_r80 x WHERE x.cohort_hash=NEW.cohort_hash AND x.target_issue=NEW.target_issue) BEGIN SELECT RAISE(ABORT,'BOUND_REPLACE_FORBIDDEN'); END;

-- research_bound_input_r80_revision_delete
CREATE TRIGGER research_bound_input_r80_revision_delete AFTER DELETE ON research_bound_input_r80 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_bound_input_r80' AND source_rowid=OLD.rowid; END;

-- research_bound_input_r80_revision_insert
CREATE TRIGGER research_bound_input_r80_revision_insert AFTER INSERT ON research_bound_input_r80 BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_bound_input_r80',NEW.rowid); END;

-- research_bound_input_r80_revision_update
CREATE TRIGGER research_bound_input_r80_revision_update AFTER UPDATE ON research_bound_input_r80 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_bound_input_r80' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_bound_input_r80',NEW.rowid); END;

-- research_bound_parent_r80
CREATE TABLE research_bound_parent_r80(cohort_hash TEXT NOT NULL REFERENCES research_bound_cohort_r80(cohort_hash),ordinal INTEGER NOT NULL CHECK(ordinal BETWEEN 0 AND 1),candidate_id TEXT NOT NULL,freeze_hash TEXT NOT NULL,model_hash TEXT NOT NULL,learner_id TEXT NOT NULL,learner_ast_hash TEXT NOT NULL,position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),PRIMARY KEY(cohort_hash,ordinal),UNIQUE(cohort_hash,candidate_id));

-- research_bound_parent_r80_immutable
CREATE TRIGGER research_bound_parent_r80_immutable BEFORE UPDATE ON research_bound_parent_r80 BEGIN SELECT RAISE(ABORT,'BOUND_RECEIPT_IMMUTABLE'); END;

-- research_bound_parent_r80_no_replace
CREATE TRIGGER research_bound_parent_r80_no_replace BEFORE INSERT ON research_bound_parent_r80 WHEN EXISTS(SELECT 1 FROM research_bound_parent_r80 x WHERE x.cohort_hash=NEW.cohort_hash AND (x.ordinal=NEW.ordinal OR x.candidate_id=NEW.candidate_id)) BEGIN SELECT RAISE(ABORT,'BOUND_REPLACE_FORBIDDEN'); END;

-- research_bound_parent_r80_permanent
CREATE TRIGGER research_bound_parent_r80_permanent BEFORE DELETE ON research_bound_parent_r80 BEGIN SELECT RAISE(ABORT,'BOUND_ORIGINAL_PERMANENT'); END;

-- research_bound_source_r80
CREATE TABLE research_bound_source_r80(cohort_hash TEXT NOT NULL REFERENCES research_bound_cohort_r80(cohort_hash),candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,role TEXT NOT NULL CHECK(role='BOUND_FEATURE_SOURCE_R80'),record_hash TEXT NOT NULL,input_hash TEXT NOT NULL,context_hash TEXT NOT NULL,context_json TEXT NOT NULL CHECK(length(CAST(context_json AS BLOB)) BETWEEN 1 AND 131072),position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,score REAL NOT NULL CHECK(score>=0 AND score<=1),captured_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,settled INTEGER NOT NULL DEFAULT 0 CHECK(settled IN(0,1)),hit INTEGER NOT NULL DEFAULT -1,label_hash TEXT NOT NULL DEFAULT '',settlement_hash TEXT NOT NULL DEFAULT '',draw_hash TEXT NOT NULL DEFAULT '',settled_at INTEGER NOT NULL DEFAULT 0,proof_error TEXT NOT NULL DEFAULT '',actual INTEGER NOT NULL DEFAULT 0,draw_open_at INTEGER NOT NULL DEFAULT 0,draw_seen_at INTEGER NOT NULL DEFAULT 0,authority_completed_at INTEGER NOT NULL DEFAULT 0,authority_hash TEXT NOT NULL DEFAULT '',PRIMARY KEY(cohort_hash,candidate_id,target_issue),FOREIGN KEY(cohort_hash,candidate_id) REFERENCES research_bound_parent_r80(cohort_hash,candidate_id));

-- research_bound_source_r80_archive_guard
CREATE TRIGGER research_bound_source_r80_archive_guard BEFORE DELETE ON research_bound_source_r80 WHEN NOT COALESCE((OLD.target_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND ((OLD.settled=1 AND OLD.proof_error='') OR (OLD.settled=0 AND OLD.proof_error='BOUND_STRICT_TIME_REJECTED' AND OLD.hit=-1 AND OLD.label_hash='' AND length(OLD.authority_hash)=64 AND OLD.settled_at>0)) AND EXISTS(SELECT 1 FROM research_bound_cohort_r80 c WHERE c.cohort_hash=OLD.cohort_hash AND (c.state IN('CAPTURING','REGISTERED','RETIRED') OR (c.state IN('READY','PROPOSED') AND c.selection_issue<>'' AND OLD.target_issue>c.selection_issue)) AND NOT EXISTS(SELECT 1 FROM v57_research_task_r967 t WHERE t.candidate_id=c.child_id AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL')))),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_bound_source_r80' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_bound_source_r80' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'BOUND_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- research_bound_source_r80_archive_locator
CREATE TRIGGER research_bound_source_r80_archive_locator BEFORE DELETE ON research_bound_source_r80 BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_bound_source_r80' AND p.source_rowid=OLD.rowid; END;

-- research_bound_source_r80_no_replace
CREATE TRIGGER research_bound_source_r80_no_replace BEFORE INSERT ON research_bound_source_r80 WHEN EXISTS(SELECT 1 FROM research_bound_source_r80 x WHERE x.cohort_hash=NEW.cohort_hash AND x.candidate_id=NEW.candidate_id AND x.target_issue=NEW.target_issue) BEGIN SELECT RAISE(ABORT,'BOUND_REPLACE_FORBIDDEN'); END;

-- research_bound_source_r80_revision_delete
CREATE TRIGGER research_bound_source_r80_revision_delete AFTER DELETE ON research_bound_source_r80 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_bound_source_r80' AND source_rowid=OLD.rowid; END;

-- research_bound_source_r80_revision_insert
CREATE TRIGGER research_bound_source_r80_revision_insert AFTER INSERT ON research_bound_source_r80 BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_bound_source_r80',NEW.rowid); END;

-- research_bound_source_r80_revision_update
CREATE TRIGGER research_bound_source_r80_revision_update AFTER UPDATE ON research_bound_source_r80 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_bound_source_r80' AND source_rowid=OLD.rowid; INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_bound_source_r80',NEW.rowid); END;

-- research_calibration_quality_cp12
CREATE TABLE research_calibration_quality_cp12(study_hash TEXT PRIMARY KEY REFERENCES research_candidate_cal_model_r932(study_hash),model_hash TEXT NOT NULL CHECK(length(model_hash)=64),state TEXT NOT NULL CHECK(state IN('CALIBRATION_QUALIFIED','FITTED_BUT_NOT_QUALIFIED')),reason TEXT NOT NULL,policy_hash TEXT NOT NULL CHECK(length(policy_hash)=64),ece REAL NOT NULL CHECK(ece>=0 AND ece<=1),brier REAL NOT NULL CHECK(brier>=0 AND brier<=1),baseline_brier REAL NOT NULL CHECK(baseline_brier>=0 AND baseline_brier<=1),validation_until TEXT NOT NULL,published_at INTEGER NOT NULL);

-- research_candidate_cal_model_r932
CREATE TABLE research_candidate_cal_model_r932(study_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,trained_until TEXT NOT NULL,validation_until TEXT NOT NULL,model_json TEXT NOT NULL,model_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL);

-- research_candidate_cal_observation_r932
CREATE TABLE research_candidate_cal_observation_r932(study_hash TEXT NOT NULL,target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,structure_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,raw_score REAL NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,prediction_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,hit INTEGER NOT NULL DEFAULT -1,PRIMARY KEY(study_hash,target_issue));

-- research_candidate_cal_study_r932
CREATE TABLE research_candidate_cal_study_r932(study_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,captured_n INTEGER NOT NULL,settled_n INTEGER NOT NULL,state TEXT NOT NULL,first_issue TEXT NOT NULL);

-- research_candidate_execution_r932
CREATE TABLE research_candidate_execution_r932(candidate_key TEXT NOT NULL,target_issue TEXT NOT NULL,structural_hash TEXT NOT NULL,input_hash TEXT NOT NULL,base_issue TEXT NOT NULL,prediction_hash TEXT NOT NULL,ranking_hash TEXT NOT NULL,score_hash TEXT NOT NULL,code_hash TEXT NOT NULL,schema_hash TEXT NOT NULL,atom_graph_hash TEXT NOT NULL,action_count INTEGER NOT NULL,max_error REAL NOT NULL,elapsed_ns INTEGER NOT NULL,payload TEXT NOT NULL,proof_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,PRIMARY KEY(candidate_key,target_issue));

-- research_candidate_identity_r932
CREATE TABLE research_candidate_identity_r932(candidate_key TEXT PRIMARY KEY,family TEXT NOT NULL,structure_hash TEXT NOT NULL,first_issue TEXT NOT NULL);

-- research_capture_event_r932
CREATE TABLE research_capture_event_r932(event_hash TEXT PRIMARY KEY,target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,record_hash TEXT NOT NULL,at_ms INTEGER NOT NULL);

-- research_capture_gap_v2
CREATE TABLE research_capture_gap_v2(
 study_hash TEXT NOT NULL, expected_issue TEXT NOT NULL, observed_issue TEXT NOT NULL,
 reason TEXT NOT NULL, request_ref TEXT NOT NULL, recorded_at INTEGER NOT NULL,
 PRIMARY KEY(study_hash,expected_issue));

-- research_capture_opportunity_r932
CREATE TABLE research_capture_opportunity_r932(target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,system_available INTEGER NOT NULL,eligible INTEGER NOT NULL,selected INTEGER NOT NULL,execution_started INTEGER NOT NULL,frozen INTEGER NOT NULL,full1200 INTEGER NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,record_hash TEXT NOT NULL,cost_ms INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_key));

-- research_capture_reservation_v2
CREATE TABLE research_capture_reservation_v2(
 reservation_id TEXT PRIMARY KEY, candidate_key TEXT NOT NULL,
 freeze_hash TEXT NOT NULL, study_hash TEXT NOT NULL,
 first_issue TEXT NOT NULL, planned_n INTEGER NOT NULL CHECK(planned_n>0),
 schedule_version TEXT NOT NULL, cost_budget_us INTEGER NOT NULL CHECK(cost_budget_us>0),
 state TEXT NOT NULL CHECK(state IN ('RESERVED','ACTIVE','COMPLETE','FAILED_GAP','RETIRED')),
 next_issue TEXT NOT NULL, generation INTEGER NOT NULL DEFAULT 0,
 last_reason TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL,
 UNIQUE(study_hash));

-- research_certificate_gate_r932
CREATE TABLE research_certificate_gate_r932(certificate_hash TEXT NOT NULL,gate_id TEXT NOT NULL,observed REAL NOT NULL,required REAL NOT NULL,pass INTEGER NOT NULL,source TEXT NOT NULL,source_hash TEXT NOT NULL,detail TEXT NOT NULL,gate_hash TEXT NOT NULL,PRIMARY KEY(certificate_hash,gate_id));

-- research_certificate_history_r932
CREATE TABLE research_certificate_history_r932(certificate_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,canonical TEXT NOT NULL,evidence_hash TEXT NOT NULL,issued_at INTEGER NOT NULL);

-- research_certificate_policy_v2
CREATE TABLE research_certificate_policy_v2(
 policy_hash TEXT PRIMARY KEY, policy_json TEXT NOT NULL, registered_issue TEXT NOT NULL,
 state TEXT NOT NULL CHECK(state IN ('STAGED','ACTIVE','RETIRED')));

-- research_certificate_r929
CREATE TABLE research_certificate_r929(candidate_key TEXT PRIMARY KEY NOT NULL,consumer TEXT NOT NULL,namespace TEXT NOT NULL,family_id TEXT NOT NULL,candidate_id TEXT NOT NULL,structural_hash TEXT NOT NULL,evidence_hash TEXT NOT NULL,level TEXT NOT NULL,state TEXT NOT NULL,bundle_hash TEXT NOT NULL,schema_hash TEXT NOT NULL,atom_graph_hash TEXT NOT NULL,model_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,calibration_hash TEXT NOT NULL,domain_hash TEXT NOT NULL,trial_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,code_hash TEXT NOT NULL,valid_from_issue TEXT NOT NULL,valid_until_issue TEXT NOT NULL,maximum_weight REAL NOT NULL,all_gates_pass INTEGER NOT NULL,gate_hash TEXT NOT NULL,reason TEXT NOT NULL,certificate_hash TEXT NOT NULL UNIQUE,issued_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,contract_revision TEXT NOT NULL DEFAULT '');

-- research_certificate_revocation_r932
CREATE TABLE research_certificate_revocation_r932(certificate_hash TEXT PRIMARY KEY,reason TEXT NOT NULL,revoked_at INTEGER NOT NULL);

-- research_challenge_prediction_r932
CREATE TABLE research_challenge_prediction_r932(challenge_id TEXT NOT NULL,target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,prediction_hash TEXT NOT NULL,ordinal INTEGER NOT NULL,probability REAL NOT NULL,calibration_hash TEXT NOT NULL,cost_ns INTEGER NOT NULL,hit INTEGER NOT NULL,frozen_at INTEGER NOT NULL,PRIMARY KEY(challenge_id,target_issue,candidate_key));

-- research_champion_probability_r82
CREATE TABLE research_champion_probability_r82(target_issue TEXT NOT NULL,bundle_hash TEXT NOT NULL,champion_prediction_hash TEXT NOT NULL,input_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,probability REAL,calibration_hash TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,proof_json TEXT NOT NULL,proof_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,PRIMARY KEY(target_issue,bundle_hash),FOREIGN KEY(target_issue,bundle_hash) REFERENCES research_fixed_champion_r932(target_issue,bundle_hash));

-- research_child_teacher_lineage_cp12
CREATE TABLE research_child_teacher_lineage_cp12(child_id TEXT NOT NULL REFERENCES r952_candidate(candidate_id),teacher_freeze_hash TEXT NOT NULL CHECK(length(teacher_freeze_hash)=64),teacher_model_hash TEXT NOT NULL CHECK(length(teacher_model_hash)=64),teacher_manifest_hash TEXT NOT NULL CHECK(length(teacher_manifest_hash)=64),teacher_candidate_id TEXT NOT NULL,witness_issue TEXT NOT NULL,action_id TEXT NOT NULL REFERENCES research_action_teacher_binding_cp12(action_id),binding_proof_hash TEXT NOT NULL CHECK(length(binding_proof_hash)=64),witness_proof_hash TEXT NOT NULL CHECK(length(witness_proof_hash)=64),receipt_hash TEXT NOT NULL UNIQUE CHECK(length(receipt_hash)=64),created_at INTEGER NOT NULL,PRIMARY KEY(child_id,teacher_freeze_hash));

-- research_consumer_choice_r932
CREATE TABLE research_consumer_choice_r932(target_issue TEXT PRIMARY KEY,bundle_hash TEXT NOT NULL,consumer TEXT NOT NULL,candidate_id TEXT NOT NULL,certificate_hash TEXT NOT NULL,roster_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL);

-- research_cp4_candidate_resource
CREATE TABLE research_cp4_candidate_resource(candidate_key TEXT NOT NULL,target_issue TEXT NOT NULL,prediction_hash TEXT NOT NULL,execution_proof_hash TEXT NOT NULL,position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),regime_id INTEGER NOT NULL CHECK(regime_id BETWEEN 0 AND 3),regime_policy_hash TEXT NOT NULL,drift_js REAL NOT NULL CHECK(drift_js>=0),elapsed_ns INTEGER NOT NULL CHECK(elapsed_ns>0),heap_increment_bytes INTEGER NOT NULL CHECK(heap_increment_bytes>=0),heap_samples INTEGER NOT NULL CHECK(heap_samples>0),measured INTEGER NOT NULL CHECK(measured IN(0,1)),history_n INTEGER NOT NULL CHECK(history_n=100),created_at INTEGER NOT NULL,record_hash TEXT NOT NULL,PRIMARY KEY(candidate_key,target_issue),FOREIGN KEY(candidate_key,target_issue) REFERENCES research_candidate_execution_r932(candidate_key,target_issue));

-- research_cp4_candidate_resource_immutable_DELETE
CREATE TRIGGER research_cp4_candidate_resource_immutable_DELETE BEFORE DELETE ON research_cp4_candidate_resource WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_cp4_candidate_resource' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'CP4_RESOURCE_DELETE_REQUIRES_ARCHIVE'); END;

-- research_cp4_candidate_resource_immutable_UPDATE
CREATE TRIGGER research_cp4_candidate_resource_immutable_UPDATE BEFORE UPDATE ON research_cp4_candidate_resource BEGIN SELECT RAISE(ABORT,'CP4_RESOURCE_IMMUTABLE'); END;

-- research_cp4_frozen_lifetime
CREATE TABLE research_cp4_frozen_lifetime(lifetime_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,generation_id TEXT NOT NULL,identity_hash TEXT NOT NULL,identity_json TEXT NOT NULL,policy_hash TEXT NOT NULL,policy_json TEXT NOT NULL,hypothesis_id TEXT NOT NULL UNIQUE,registration_json TEXT NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(candidate_key,generation_id));

-- research_cp4_frozen_lifetime_immutable_DELETE
CREATE TRIGGER research_cp4_frozen_lifetime_immutable_DELETE BEFORE DELETE ON research_cp4_frozen_lifetime BEGIN SELECT RAISE(ABORT,'CP4_FORMAL_IDENTITY_IMMUTABLE'); END;

-- research_cp4_frozen_lifetime_immutable_UPDATE
CREATE TRIGGER research_cp4_frozen_lifetime_immutable_UPDATE BEFORE UPDATE ON research_cp4_frozen_lifetime BEGIN SELECT RAISE(ABORT,'CP4_FORMAL_IDENTITY_IMMUTABLE'); END;

-- research_cp4_window_binding
CREATE TABLE research_cp4_window_binding(window_id TEXT PRIMARY KEY,lifetime_id TEXT NOT NULL REFERENCES research_cp4_frozen_lifetime(lifetime_id),stage INTEGER NOT NULL CHECK(stage BETWEEN 1 AND 5),first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,planned_n INTEGER NOT NULL,plan_hash TEXT NOT NULL UNIQUE,UNIQUE(lifetime_id,stage));

-- research_cp4_window_binding_immutable_DELETE
CREATE TRIGGER research_cp4_window_binding_immutable_DELETE BEFORE DELETE ON research_cp4_window_binding BEGIN SELECT RAISE(ABORT,'CP4_FORMAL_IDENTITY_IMMUTABLE'); END;

-- research_cp4_window_binding_immutable_UPDATE
CREATE TRIGGER research_cp4_window_binding_immutable_UPDATE BEFORE UPDATE ON research_cp4_window_binding BEGIN SELECT RAISE(ABORT,'CP4_FORMAL_IDENTITY_IMMUTABLE'); END;

-- research_creation_attempt_r932
CREATE TABLE research_creation_attempt_r932(attempt_id TEXT PRIMARY KEY,lane TEXT NOT NULL,kind TEXT NOT NULL,parent_key TEXT NOT NULL,group_hash TEXT NOT NULL,seed TEXT NOT NULL,created_issue TEXT NOT NULL,state TEXT NOT NULL,child_key TEXT NOT NULL,child_structure TEXT NOT NULL,reason TEXT NOT NULL,plan_json TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_creation_cursor_r932
CREATE TABLE research_creation_cursor_r932(owner TEXT PRIMARY KEY,last_row INTEGER NOT NULL);

-- research_development_receipt_v2
CREATE TABLE "research_development_receipt_v2"(
 receipt_hash TEXT PRIMARY KEY CHECK(length(receipt_hash)=64),
 artifact_hash TEXT NOT NULL UNIQUE REFERENCES research_artifact_identity_v2(artifact_hash),
 protocol_hash TEXT NOT NULL UNIQUE REFERENCES research_training_protocol_v2(protocol_hash),
 validation_hash TEXT NOT NULL CHECK(length(validation_hash)=64),
 receipt_json TEXT NOT NULL CHECK(length(CAST(receipt_json AS BLOB)) BETWEEN 2 AND 8192),
 validation_json TEXT NOT NULL CHECK(length(CAST(validation_json AS BLOB)) BETWEEN 2 AND 16384),
 created_at INTEGER NOT NULL);

-- research_durable_model_identity_cp12
CREATE TABLE research_durable_model_identity_cp12(model_key TEXT PRIMARY KEY,byte_count INTEGER NOT NULL CHECK(byte_count>=0),ref_count INTEGER NOT NULL DEFAULT 0 CHECK(ref_count>=0));

-- research_durable_model_ref_cp12
CREATE TABLE research_durable_model_ref_cp12(source_table TEXT NOT NULL,source_key TEXT NOT NULL,model_key TEXT NOT NULL REFERENCES research_durable_model_identity_cp12(model_key),byte_count INTEGER NOT NULL CHECK(byte_count>=0),PRIMARY KEY(source_table,source_key));

-- research_elite_r932
CREATE TABLE research_elite_r932(candidate_key TEXT PRIMARY KEY,structural_hash TEXT NOT NULL,future_n INTEGER NOT NULL,hit_rate REAL NOT NULL,brier REAL NOT NULL,ece REAL NOT NULL,paired_delta REAL NOT NULL,missing_gates TEXT NOT NULL,evidence_hash TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- research_evidence_archive_member_v2
CREATE TABLE research_evidence_archive_member_v2(
 archive_id TEXT NOT NULL REFERENCES research_evidence_archive_v2(archive_id),
 source_table TEXT NOT NULL, source_key_hash TEXT NOT NULL, row_hash TEXT NOT NULL,
 key_canonical BLOB NOT NULL, source_generation INTEGER NOT NULL DEFAULT 0 CHECK(source_generation>=0), source_rowid INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(archive_id,source_table,source_key_hash));

-- research_evidence_archive_v2
CREATE TABLE research_evidence_archive_v2(
 archive_id TEXT PRIMARY KEY, source_table TEXT NOT NULL, first_issue TEXT NOT NULL,
 last_issue TEXT NOT NULL, row_count INTEGER NOT NULL CHECK(row_count>=0),
 archive_hash TEXT NOT NULL UNIQUE, relative_path TEXT NOT NULL,
 state TEXT NOT NULL CHECK(state IN ('WRITING','VERIFIED','REFERENCED','PRUNED','EXPIRED_SUMMARIZED')),
 source_schema_hash TEXT NOT NULL DEFAULT '',manifest_hash TEXT NOT NULL DEFAULT '',
 summary_json TEXT NOT NULL DEFAULT '',summary_hash TEXT NOT NULL DEFAULT '',expires_at INTEGER NOT NULL DEFAULT 0,
 created_at INTEGER NOT NULL, verified_at INTEGER);

-- research_evidence_locator_v2
CREATE TABLE research_evidence_locator_v2(
 source_table TEXT NOT NULL,source_key_hash TEXT NOT NULL,archive_id TEXT NOT NULL REFERENCES research_evidence_archive_v2(archive_id),
 row_hash TEXT NOT NULL,key_canonical BLOB NOT NULL,created_at INTEGER NOT NULL,
 PRIMARY KEY(source_table,source_key_hash));

-- research_evolution_event_v41
CREATE TABLE research_evolution_event_v41(event_id TEXT PRIMARY KEY,event_issue TEXT NOT NULL,entity_type TEXT NOT NULL,entity_id TEXT NOT NULL,operation TEXT NOT NULL,parent_ids TEXT NOT NULL,child_ids TEXT NOT NULL,failure_vector TEXT NOT NULL,need_plan TEXT NOT NULL,created_ms INTEGER NOT NULL,record_hash TEXT NOT NULL UNIQUE);

-- research_exact_probability_f05
CREATE TABLE research_exact_probability_f05(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,trade_id INTEGER NOT NULL UNIQUE REFERENCES trades(_id),candidate_id TEXT NOT NULL,model_hash TEXT NOT NULL CHECK(length(model_hash)=64),certificate_hash TEXT NOT NULL CHECK(length(certificate_hash)=64),calibration_hash TEXT NOT NULL CHECK(length(calibration_hash)=64),selection_hash TEXT NOT NULL CHECK(length(selection_hash)=64),scope_policy_hash TEXT NOT NULL CHECK(length(scope_policy_hash)=64),selected_hit_p REAL NOT NULL CHECK(selected_hit_p>0 AND selected_hit_p<1),proof_hash TEXT NOT NULL CHECK(length(proof_hash)=64),action_hash TEXT NOT NULL CHECK(length(action_hash)=64),authorization_hash TEXT NOT NULL,captured_at INTEGER NOT NULL,committed_at INTEGER NOT NULL CHECK(committed_at>=captured_at),PRIMARY KEY(game_code,target_issue));

-- research_experiment_plan_r967
CREATE TABLE research_experiment_plan_r967(window_id TEXT PRIMARY KEY,plan_hash TEXT NOT NULL UNIQUE,policy_hash TEXT NOT NULL,plan_json TEXT NOT NULL,registered_at INTEGER NOT NULL);

-- research_experiment_plan_r967_immutable_delete
CREATE TRIGGER research_experiment_plan_r967_immutable_delete BEFORE DELETE ON research_experiment_plan_r967 BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- research_experiment_plan_r967_immutable_update
CREATE TRIGGER research_experiment_plan_r967_immutable_update BEFORE UPDATE ON research_experiment_plan_r967 BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- research_family_champion_r932
CREATE TABLE research_family_champion_r932(family TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,structural_hash TEXT NOT NULL,certificate_hash TEXT NOT NULL,evidence_hash TEXT NOT NULL,selected_issue TEXT NOT NULL);

-- research_family_creation_cp12
CREATE TABLE research_family_creation_cp12(family TEXT NOT NULL,source_hash TEXT NOT NULL CHECK(length(source_hash)=64),variant INTEGER NOT NULL CHECK(variant BETWEEN 0 AND 39),ast_hash TEXT NOT NULL CHECK(length(ast_hash)=64),ast TEXT NOT NULL CHECK(length(CAST(ast AS BLOB))<=32768),action_id TEXT NOT NULL UNIQUE REFERENCES research_action_v2(action_id),candidate_id TEXT NOT NULL UNIQUE,source_issue TEXT NOT NULL,state TEXT NOT NULL CHECK(state IN('PROPOSED','REGISTERED','NO_OP','HELD')),last_reason TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(family,source_hash),UNIQUE(family,variant));

-- research_family_cursor_cp12
CREATE TABLE research_family_cursor_cp12(family TEXT PRIMARY KEY,next_variant INTEGER NOT NULL DEFAULT 0 CHECK(next_variant BETWEEN 0 AND 40),last_reason TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL);

-- research_family_wheel_cp12
CREATE TABLE research_family_wheel_cp12(singleton INTEGER PRIMARY KEY CHECK(singleton=1),next_family INTEGER NOT NULL CHECK(next_family BETWEEN 0 AND 14),generation INTEGER NOT NULL DEFAULT 0);

-- research_feedback_rejection_cp12
CREATE TABLE research_feedback_rejection_cp12(
 window_id TEXT NOT NULL REFERENCES research_cp4_window_binding(window_id),target_issue TEXT NOT NULL,
 lifetime_id TEXT NOT NULL REFERENCES research_cp4_frozen_lifetime(lifetime_id),
 source_hash TEXT NOT NULL,failure_code TEXT NOT NULL,rejection_hash TEXT NOT NULL UNIQUE CHECK(length(rejection_hash)=64),
 created_at INTEGER NOT NULL,PRIMARY KEY(window_id,target_issue));

-- research_feedback_rejection_cp12_no_delete
CREATE TRIGGER research_feedback_rejection_cp12_no_delete BEFORE DELETE ON research_feedback_rejection_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_REJECTION_PERMANENT'); END;

-- research_feedback_rejection_cp12_no_replace
CREATE TRIGGER research_feedback_rejection_cp12_no_replace BEFORE INSERT ON research_feedback_rejection_cp12 WHEN EXISTS(SELECT 1 FROM research_feedback_rejection_cp12 WHERE window_id=NEW.window_id AND target_issue=NEW.target_issue) BEGIN SELECT RAISE(ABORT,'RESEARCH_REJECTION_REPLACE_FORBIDDEN'); END;

-- research_feedback_rejection_cp12_no_update
CREATE TRIGGER research_feedback_rejection_cp12_no_update BEFORE UPDATE ON research_feedback_rejection_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_REJECTION_IMMUTABLE'); END;

-- research_finding_evidence_v2
CREATE TABLE research_finding_evidence_v2(
 finding_id TEXT NOT NULL REFERENCES research_finding_v2(finding_id),
 evidence_hash TEXT NOT NULL, source_issue TEXT NOT NULL,
 evidence_json TEXT NOT NULL, created_at INTEGER NOT NULL,
 PRIMARY KEY(finding_id,evidence_hash));

-- research_finding_v2
CREATE TABLE research_finding_v2(
 finding_id TEXT PRIMARY KEY, game_code TEXT NOT NULL, candidate_version TEXT NOT NULL, source_issue TEXT NOT NULL,
 episode_start_issue TEXT NOT NULL,
 problem_type TEXT NOT NULL, severity TEXT NOT NULL CHECK(severity IN ('P0','P1','P2')),
 evidence_hash TEXT NOT NULL, evidence_json TEXT NOT NULL, hypothesis TEXT NOT NULL,
 falsification_test TEXT NOT NULL, state TEXT NOT NULL CHECK(state IN ('OPEN','PLANNED','RUNNING','EVALUATING','CONFIRMED','REJECTED','HOLD','ERROR')),
 created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
 UNIQUE(game_code,candidate_version,problem_type,episode_start_issue));

-- research_fixed_champion_r932
CREATE TABLE research_fixed_champion_r932(target_issue TEXT NOT NULL,bundle_hash TEXT NOT NULL,input_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,prediction_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,PRIMARY KEY(target_issue,bundle_hash));

-- research_fixed_pair_r932
CREATE TABLE research_fixed_pair_r932(window_id TEXT NOT NULL,target_issue TEXT NOT NULL,candidate_hash TEXT NOT NULL,champion_prediction_hash TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_hit_p REAL NOT NULL,calibration_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,candidate_hit INTEGER NOT NULL DEFAULT -1,champion_hit INTEGER NOT NULL DEFAULT -1,settled_at INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(window_id,target_issue));

-- research_fixed_window_contract_r932
CREATE TABLE research_fixed_window_contract_r932(window_id TEXT PRIMARY KEY,contract_hash TEXT NOT NULL);

-- research_fixed_window_r932
CREATE TABLE research_fixed_window_r932(window_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,structural_hash TEXT NOT NULL,champion_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,planned_n INTEGER NOT NULL,window_no INTEGER NOT NULL,registered_at INTEGER NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,result_hash TEXT NOT NULL DEFAULT '', formal_plan_revision TEXT NOT NULL DEFAULT 'LEGACY_R932',UNIQUE(candidate_key,window_no));

-- research_fixed_window_statistics_r932
CREATE TABLE research_fixed_window_statistics_r932(window_id TEXT PRIMARY KEY,metrics_json TEXT NOT NULL,result_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_formal_cp12_no_replace
CREATE TRIGGER research_formal_cp12_no_replace BEFORE INSERT ON research_formal_observation_cp12 WHEN EXISTS(SELECT 1 FROM research_formal_observation_cp12 WHERE source_hash=NEW.source_hash OR (lifetime_id=NEW.lifetime_id AND target_issue=NEW.target_issue)) BEGIN SELECT RAISE(ABORT,'RESEARCH_FORMAL_OBSERVATION_REPLACE_FORBIDDEN'); END;

-- research_formal_cp12_no_update
CREATE TRIGGER research_formal_cp12_no_update BEFORE UPDATE ON research_formal_observation_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_FORMAL_OBSERVATION_IMMUTABLE'); END;

-- research_formal_observation_cp12
CREATE TABLE research_formal_observation_cp12(
 source_hash TEXT PRIMARY KEY CHECK(length(source_hash)=64),
 window_id TEXT NOT NULL REFERENCES research_cp4_window_binding(window_id),
 lifetime_id TEXT NOT NULL REFERENCES research_cp4_frozen_lifetime(lifetime_id),
 candidate_key TEXT NOT NULL,learner_id TEXT NOT NULL,target_issue TEXT NOT NULL,
 position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),actual_digit INTEGER NOT NULL CHECK(actual_digit BETWEEN 1 AND 10),
 prediction_mask INTEGER NOT NULL,selected_hit_p REAL NOT NULL CHECK(selected_hit_p BETWEEN 0 AND 1),
 hit INTEGER NOT NULL CHECK(hit IN(0,1)),label_hash TEXT NOT NULL CHECK(length(label_hash)=64),
 settlement_hash TEXT NOT NULL CHECK(length(settlement_hash)=64),frozen_at INTEGER NOT NULL,
 receipt_hash TEXT NOT NULL UNIQUE CHECK(length(receipt_hash)=64),created_at INTEGER NOT NULL,
 UNIQUE(lifetime_id,target_issue),UNIQUE(window_id,target_issue));

-- research_gate_policy_r967
CREATE TABLE research_gate_policy_r967(policy_hash TEXT PRIMARY KEY,policy_json TEXT NOT NULL,registered_at INTEGER NOT NULL);

-- research_gate_policy_r967_immutable_delete
CREATE TRIGGER research_gate_policy_r967_immutable_delete BEFORE DELETE ON research_gate_policy_r967 BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- research_gate_policy_r967_immutable_update
CREATE TRIGGER research_gate_policy_r967_immutable_update BEFORE UPDATE ON research_gate_policy_r967 BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- research_historical_genome_archive_r967
CREATE TABLE research_historical_genome_archive_r967(record_id TEXT PRIMARY KEY,source_hash TEXT NOT NULL,row_number INTEGER NOT NULL,row_hash TEXT NOT NULL,raw_tsv TEXT NOT NULL,original_id TEXT NOT NULL,original_status TEXT NOT NULL,original_reason TEXT NOT NULL,program_hash TEXT NOT NULL,state_hash TEXT NOT NULL,verdict TEXT NOT NULL,reason TEXT NOT NULL,seed_spec TEXT NOT NULL,seed_hash TEXT NOT NULL,imported_issue TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(source_hash,row_number));

-- research_historical_genome_archive_r967_no_delete
CREATE TRIGGER research_historical_genome_archive_r967_no_delete BEFORE DELETE ON research_historical_genome_archive_r967 BEGIN SELECT RAISE(ABORT,'R970_HISTORY_IMMUTABLE'); END;

-- research_historical_genome_archive_r967_no_replace
CREATE TRIGGER research_historical_genome_archive_r967_no_replace BEFORE INSERT ON research_historical_genome_archive_r967 WHEN EXISTS(SELECT 1 FROM research_historical_genome_archive_r967 AS OLD WHERE OLD.record_id=NEW.record_id OR (OLD.source_hash=NEW.source_hash AND OLD.row_number=NEW.row_number)) BEGIN SELECT RAISE(ABORT,'R970_HISTORY_NO_REPLACE'); END;

-- research_historical_genome_archive_r967_no_update
CREATE TRIGGER research_historical_genome_archive_r967_no_update BEFORE UPDATE ON research_historical_genome_archive_r967 BEGIN SELECT RAISE(ABORT,'R970_HISTORY_IMMUTABLE'); END;

-- research_historical_genome_use_r967
CREATE TABLE research_historical_genome_use_r967(record_id TEXT PRIMARY KEY,source_hash TEXT NOT NULL,record_hash TEXT NOT NULL,seed_key TEXT NOT NULL,attempt_id TEXT NOT NULL,boundary_issue TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_historical_genome_use_r967_no_delete
CREATE TRIGGER research_historical_genome_use_r967_no_delete BEFORE DELETE ON research_historical_genome_use_r967 BEGIN SELECT RAISE(ABORT,'R970_HISTORY_IMMUTABLE'); END;

-- research_historical_genome_use_r967_no_replace
CREATE TRIGGER research_historical_genome_use_r967_no_replace BEFORE INSERT ON research_historical_genome_use_r967 WHEN EXISTS(SELECT 1 FROM research_historical_genome_use_r967 AS OLD WHERE OLD.record_id=NEW.record_id) BEGIN SELECT RAISE(ABORT,'R970_HISTORY_NO_REPLACE'); END;

-- research_historical_genome_use_r967_no_update
CREATE TRIGGER research_historical_genome_use_r967_no_update BEFORE UPDATE ON research_historical_genome_use_r967 BEGIN SELECT RAISE(ABORT,'R970_HISTORY_IMMUTABLE'); END;

-- research_historical_source_r967
CREATE TABLE research_historical_source_r967(source_hash TEXT PRIMARY KEY,asset_name TEXT NOT NULL,header TEXT NOT NULL,row_count INTEGER NOT NULL CHECK(row_count BETWEEN 1 AND 125),imported_issue TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_historical_source_r967_no_delete
CREATE TRIGGER research_historical_source_r967_no_delete BEFORE DELETE ON research_historical_source_r967 BEGIN SELECT RAISE(ABORT,'R970_HISTORY_IMMUTABLE'); END;

-- research_historical_source_r967_no_replace
CREATE TRIGGER research_historical_source_r967_no_replace BEFORE INSERT ON research_historical_source_r967 WHEN EXISTS(SELECT 1 FROM research_historical_source_r967 AS OLD WHERE OLD.source_hash=NEW.source_hash) BEGIN SELECT RAISE(ABORT,'R970_HISTORY_NO_REPLACE'); END;

-- research_historical_source_r967_no_update
CREATE TRIGGER research_historical_source_r967_no_update BEFORE UPDATE ON research_historical_source_r967 BEGIN SELECT RAISE(ABORT,'R970_HISTORY_IMMUTABLE'); END;

-- research_identity_r967
CREATE TABLE research_identity_r967(candidate_key TEXT PRIMARY KEY,hypothesis_hash TEXT NOT NULL,structure_hash TEXT NOT NULL,family TEXT NOT NULL,registered_issue TEXT NOT NULL);

-- research_identity_view_r967
CREATE VIEW research_identity_view_r967 AS SELECT a.candidate_key,h.hypothesis_hash,h.structure_hash,h.family,h.registered_issue FROM research_alpha_alias_r932 a JOIN research_alpha_hypothesis_r932 h ON a.hypothesis_hash=h.hypothesis_hash UNION ALL SELECT r.candidate_key,r.hypothesis_hash,r.structure_hash,r.family,r.registered_issue FROM research_identity_r967 r WHERE NOT EXISTS(SELECT 1 FROM research_alpha_alias_r932 a WHERE a.candidate_key=r.candidate_key);

-- research_legacy_asset_head_audit_r80
CREATE TABLE research_legacy_asset_head_audit_r80(asset_id TEXT PRIMARY KEY,input_hash TEXT NOT NULL,cursor_n INTEGER NOT NULL,replay_state TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,last_ms INTEGER NOT NULL,last_issue TEXT NOT NULL,generation INTEGER NOT NULL,copy_issue TEXT NOT NULL);

-- research_legacy_asset_head_baseline_r80
CREATE TABLE research_legacy_asset_head_baseline_r80(asset_id TEXT PRIMARY KEY,input_hash TEXT NOT NULL,state_hash TEXT NOT NULL,update_credit INTEGER NOT NULL,settled_rows INTEGER NOT NULL,trained_until_issue TEXT NOT NULL);

-- research_legacy_asset_head_snapshot_r80
CREATE TABLE research_legacy_asset_head_snapshot_r80(asset_id TEXT NOT NULL,generation INTEGER NOT NULL,target_issue TEXT NOT NULL,settled_ms INTEGER NOT NULL,row_blob TEXT NOT NULL,original_identity TEXT NOT NULL,PRIMARY KEY(asset_id,generation,target_issue));

-- research_legacy_fixed_registry_v41
CREATE TABLE research_legacy_fixed_registry_v41(asset_key TEXT PRIMARY KEY,name_zh TEXT NOT NULL,owner_class TEXT NOT NULL UNIQUE,output_schema TEXT NOT NULL,consumer TEXT NOT NULL,source_digest TEXT NOT NULL,semantic_digest TEXT NOT NULL,db_contract_digest TEXT NOT NULL,baseline_behavior_digest TEXT NOT NULL,locked INTEGER NOT NULL CHECK(locked=1),registered_ms INTEGER NOT NULL);

-- research_legacy_settlement_classification_r79
CREATE TABLE research_legacy_settlement_classification_r79(receipt_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,target_issue TEXT NOT NULL,original_identity_hash TEXT NOT NULL,record_hash TEXT NOT NULL,proof_hash TEXT NOT NULL,capture_event_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,execution_at INTEGER NOT NULL,capture_at INTEGER NOT NULL,draw_hash TEXT NOT NULL,draw_open_time INTEGER NOT NULL,disposition TEXT NOT NULL,reason TEXT NOT NULL,observed_at INTEGER NOT NULL);

-- research_lifecycle_event_r932
CREATE TABLE research_lifecycle_event_r932(event_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,from_state TEXT NOT NULL,to_state TEXT NOT NULL,reason TEXT NOT NULL,evidence_hash TEXT NOT NULL,at_issue TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_lifecycle_r932
CREATE TABLE research_lifecycle_r932(candidate_key TEXT PRIMARY KEY,structural_hash TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,first_prune_reason TEXT NOT NULL,future_n INTEGER NOT NULL,paired_n INTEGER NOT NULL,evidence_hash TEXT NOT NULL,last_issue TEXT NOT NULL,reviewed_at INTEGER NOT NULL);

-- research_long_creation_r81
CREATE TABLE research_long_creation_r81(source_hash TEXT PRIMARY KEY CHECK(length(source_hash)=64),epoch_hash TEXT NOT NULL CHECK(length(epoch_hash)=64),family TEXT NOT NULL CHECK(family IN('LINEAR','LOGISTIC','TREE','NEURAL','HYBRID')),variant INTEGER NOT NULL CHECK(variant BETWEEN 0 AND 39),parent_id TEXT NOT NULL,parent_hash TEXT NOT NULL CHECK(length(parent_hash)=64),ast_hash TEXT NOT NULL CHECK(length(ast_hash)=64),ast TEXT NOT NULL CHECK(length(CAST(ast AS BLOB)) BETWEEN 1 AND 32768),source_issue TEXT NOT NULL,intent_id TEXT NOT NULL,action_id TEXT NOT NULL UNIQUE REFERENCES research_action_v2(action_id),candidate_id TEXT NOT NULL UNIQUE,coverage_hash TEXT NOT NULL CHECK(length(coverage_hash)=64),state TEXT NOT NULL CHECK(state IN('PROPOSED','REGISTERED','NO_OP','HELD')),last_reason TEXT NOT NULL,generation INTEGER NOT NULL CHECK(generation>=0),created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,UNIQUE(epoch_hash,family,variant));

-- research_long_creation_r81_no_delete
CREATE TRIGGER research_long_creation_r81_no_delete BEFORE DELETE ON research_long_creation_r81 BEGIN SELECT RAISE(ABORT,'LONG_IDENTITY_PERMANENT'); END;

-- research_long_creation_r81_no_replace
CREATE TRIGGER research_long_creation_r81_no_replace BEFORE INSERT ON research_long_creation_r81 WHEN EXISTS(SELECT 1 FROM research_long_creation_r81 x WHERE x.source_hash=NEW.source_hash) BEGIN SELECT RAISE(ABORT,'LONG_IDENTITY_REPLACE_FORBIDDEN'); END;

-- research_long_epoch_r80
CREATE TABLE research_long_epoch_r80(epoch_hash TEXT PRIMARY KEY CHECK(length(epoch_hash)=64),protocol_hash TEXT NOT NULL CHECK(length(protocol_hash)=64),first_issue TEXT NOT NULL,created_at INTEGER NOT NULL CHECK(created_at>0),reason TEXT NOT NULL);

-- research_long_epoch_r80_archive_guard
CREATE TRIGGER research_long_epoch_r80_archive_guard BEFORE DELETE ON research_long_epoch_r80 WHEN NOT COALESCE((length(OLD.first_issue)=12 AND OLD.first_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND NOT EXISTS(SELECT 1 FROM research_long_head_r80 h WHERE h.epoch_hash=OLD.epoch_hash) AND NOT EXISTS(SELECT 1 FROM research_long_snapshot_r80 s WHERE s.epoch_hash=OLD.epoch_hash) AND NOT EXISTS(SELECT 1 FROM research_long_journal_r80 j WHERE j.epoch_hash=OLD.epoch_hash) AND NOT EXISTS(SELECT 1 FROM v57_research_input_pin_r967 p JOIN v57_research_task_r967 t ON t.task_id=p.task_id WHERE p.source_table='research_long_epoch_r80' AND p.source_issue=OLD.first_issue AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_long_epoch_r80' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_long_epoch_r80' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'LONG_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- research_long_epoch_r80_archive_locator
CREATE TRIGGER research_long_epoch_r80_archive_locator BEFORE DELETE ON research_long_epoch_r80 BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_long_epoch_r80' AND p.source_rowid=OLD.rowid; END;

-- research_long_epoch_r80_no_replace
CREATE TRIGGER research_long_epoch_r80_no_replace BEFORE INSERT ON research_long_epoch_r80 WHEN EXISTS(SELECT 1 FROM research_long_epoch_r80 WHERE epoch_hash=NEW.epoch_hash) BEGIN SELECT RAISE(ABORT,'LONG_REPLACE_FORBIDDEN'); END;

-- research_long_epoch_r80_no_update
CREATE TRIGGER research_long_epoch_r80_no_update BEFORE UPDATE ON research_long_epoch_r80 BEGIN SELECT RAISE(ABORT,'LONG_IMMUTABLE'); END;

-- research_long_epoch_r80_revision_delete
CREATE TRIGGER research_long_epoch_r80_revision_delete AFTER DELETE ON research_long_epoch_r80 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_long_epoch_r80' AND source_rowid=OLD.rowid; END;

-- research_long_epoch_r80_revision_insert
CREATE TRIGGER research_long_epoch_r80_revision_insert AFTER INSERT ON research_long_epoch_r80 BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_long_epoch_r80',NEW.rowid); END;

-- research_long_frozen_source_r81
CREATE TABLE research_long_frozen_source_r81(freeze_hash TEXT NOT NULL REFERENCES r967_frozen_evaluation_v2(structural_hash),epoch_hash TEXT NOT NULL CHECK(length(epoch_hash)=64),PRIMARY KEY(freeze_hash,epoch_hash));

-- research_long_head_r80
CREATE TABLE research_long_head_r80(singleton INTEGER PRIMARY KEY CHECK(singleton=1),epoch_hash TEXT NOT NULL REFERENCES research_long_epoch_r80(epoch_hash),through_issue TEXT NOT NULL,generation INTEGER NOT NULL CHECK(generation>=0),state_hash TEXT NOT NULL CHECK(length(state_hash)=64),source_chain TEXT NOT NULL CHECK(length(source_chain)=64),availability_max INTEGER NOT NULL CHECK(availability_max>=0),state_blob BLOB NOT NULL CHECK(length(state_blob)=434428),status TEXT NOT NULL CHECK(status IN('ACTIVE','INVALID_SOURCE')),last_error TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- research_long_journal_r80
CREATE TABLE research_long_journal_r80(event_hash TEXT PRIMARY KEY CHECK(length(event_hash)=64),epoch_hash TEXT NOT NULL,through_issue TEXT NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL CHECK(recorded_at>0));

-- research_long_journal_r80_archive_guard
CREATE TRIGGER research_long_journal_r80_archive_guard BEFORE DELETE ON research_long_journal_r80 WHEN NOT COALESCE((length(OLD.through_issue)=12 AND OLD.through_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND NOT EXISTS(SELECT 1 FROM v57_research_input_pin_r967 p JOIN v57_research_task_r967 t ON t.task_id=p.task_id WHERE p.source_table='research_long_journal_r80' AND p.source_issue=OLD.through_issue AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_long_journal_r80' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_long_journal_r80' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'LONG_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- research_long_journal_r80_archive_locator
CREATE TRIGGER research_long_journal_r80_archive_locator BEFORE DELETE ON research_long_journal_r80 BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_long_journal_r80' AND p.source_rowid=OLD.rowid; END;

-- research_long_journal_r80_no_replace
CREATE TRIGGER research_long_journal_r80_no_replace BEFORE INSERT ON research_long_journal_r80 WHEN EXISTS(SELECT 1 FROM research_long_journal_r80 WHERE event_hash=NEW.event_hash) BEGIN SELECT RAISE(ABORT,'LONG_REPLACE_FORBIDDEN'); END;

-- research_long_journal_r80_no_update
CREATE TRIGGER research_long_journal_r80_no_update BEFORE UPDATE ON research_long_journal_r80 BEGIN SELECT RAISE(ABORT,'LONG_IMMUTABLE'); END;

-- research_long_journal_r80_revision_delete
CREATE TRIGGER research_long_journal_r80_revision_delete AFTER DELETE ON research_long_journal_r80 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_long_journal_r80' AND source_rowid=OLD.rowid; END;

-- research_long_journal_r80_revision_insert
CREATE TRIGGER research_long_journal_r80_revision_insert AFTER INSERT ON research_long_journal_r80 BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_long_journal_r80',NEW.rowid); END;

-- research_long_publication_r80
CREATE TABLE research_long_publication_r80(snapshot_hash TEXT PRIMARY KEY REFERENCES research_long_snapshot_r80(snapshot_hash),through_issue TEXT NOT NULL,available_at INTEGER NOT NULL CHECK(available_at>0),mode TEXT NOT NULL CHECK(mode IN('ORIGINAL_RETURN','RECOVERED_AVAILABLE_NOW')),clock_session TEXT NOT NULL,receipt_hash TEXT NOT NULL UNIQUE CHECK(length(receipt_hash)=64));

-- research_long_publication_r80_archive_guard
CREATE TRIGGER research_long_publication_r80_archive_guard BEFORE DELETE ON research_long_publication_r80 WHEN NOT COALESCE((length(OLD.through_issue)=12 AND OLD.through_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND NOT EXISTS(SELECT 1 FROM v57_research_input_pin_r967 p JOIN v57_research_task_r967 t ON t.task_id=p.task_id WHERE p.source_table='research_long_publication_r80' AND p.source_issue=OLD.through_issue AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_long_publication_r80' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_long_publication_r80' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'LONG_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- research_long_publication_r80_archive_locator
CREATE TRIGGER research_long_publication_r80_archive_locator BEFORE DELETE ON research_long_publication_r80 BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_long_publication_r80' AND p.source_rowid=OLD.rowid; END;

-- research_long_publication_r80_no_replace
CREATE TRIGGER research_long_publication_r80_no_replace BEFORE INSERT ON research_long_publication_r80 WHEN EXISTS(SELECT 1 FROM research_long_publication_r80 WHERE snapshot_hash=NEW.snapshot_hash) BEGIN SELECT RAISE(ABORT,'LONG_REPLACE_FORBIDDEN'); END;

-- research_long_publication_r80_no_update
CREATE TRIGGER research_long_publication_r80_no_update BEFORE UPDATE ON research_long_publication_r80 BEGIN SELECT RAISE(ABORT,'LONG_IMMUTABLE'); END;

-- research_long_publication_r80_revision_delete
CREATE TRIGGER research_long_publication_r80_revision_delete AFTER DELETE ON research_long_publication_r80 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_long_publication_r80' AND source_rowid=OLD.rowid; END;

-- research_long_publication_r80_revision_insert
CREATE TRIGGER research_long_publication_r80_revision_insert AFTER INSERT ON research_long_publication_r80 BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_long_publication_r80',NEW.rowid); END;

-- research_long_snapshot_r80
CREATE TABLE research_long_snapshot_r80(snapshot_hash TEXT PRIMARY KEY CHECK(length(snapshot_hash)=64),epoch_hash TEXT NOT NULL REFERENCES research_long_epoch_r80(epoch_hash),through_issue TEXT NOT NULL,generation INTEGER NOT NULL,source_chain TEXT NOT NULL CHECK(length(source_chain)=64),state_hash TEXT NOT NULL CHECK(length(state_hash)=64),payload_json TEXT NOT NULL CHECK(length(CAST(payload_json AS BLOB)) BETWEEN 1 AND 16384),prepared_at INTEGER NOT NULL CHECK(prepared_at>0),UNIQUE(epoch_hash,through_issue));

-- research_long_snapshot_r80_archive_guard
CREATE TRIGGER research_long_snapshot_r80_archive_guard BEFORE DELETE ON research_long_snapshot_r80 WHEN NOT COALESCE((length(OLD.through_issue)=12 AND OLD.through_issue<(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999) AND NOT EXISTS(SELECT 1 FROM research_long_publication_r80 p WHERE p.snapshot_hash=OLD.snapshot_hash) AND NOT EXISTS(SELECT 1 FROM v57_research_input_pin_r967 p JOIN v57_research_task_r967 t ON t.task_id=p.task_id WHERE p.source_table='research_long_snapshot_r80' AND p.source_issue=OLD.through_issue AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))),0) OR NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_long_snapshot_r80' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='research_long_snapshot_r80' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'LONG_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END;

-- research_long_snapshot_r80_archive_locator
CREATE TRIGGER research_long_snapshot_r80_archive_locator BEFORE DELETE ON research_long_snapshot_r80 BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='research_long_snapshot_r80' AND p.source_rowid=OLD.rowid; END;

-- research_long_snapshot_r80_no_replace
CREATE TRIGGER research_long_snapshot_r80_no_replace BEFORE INSERT ON research_long_snapshot_r80 WHEN EXISTS(SELECT 1 FROM research_long_snapshot_r80 WHERE snapshot_hash=NEW.snapshot_hash) BEGIN SELECT RAISE(ABORT,'LONG_REPLACE_FORBIDDEN'); END;

-- research_long_snapshot_r80_no_update
CREATE TRIGGER research_long_snapshot_r80_no_update BEFORE UPDATE ON research_long_snapshot_r80 BEGIN SELECT RAISE(ABORT,'LONG_IMMUTABLE'); END;

-- research_long_snapshot_r80_revision_delete
CREATE TRIGGER research_long_snapshot_r80_revision_delete AFTER DELETE ON research_long_snapshot_r80 BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='research_long_snapshot_r80' AND source_rowid=OLD.rowid; END;

-- research_long_snapshot_r80_revision_insert
CREATE TRIGGER research_long_snapshot_r80_revision_insert AFTER INSERT ON research_long_snapshot_r80 BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('research_long_snapshot_r80',NEW.rowid); END;

-- research_long_source_token_r80
CREATE TABLE research_long_source_token_r80(singleton INTEGER PRIMARY KEY CHECK(singleton=1),generation INTEGER NOT NULL CHECK(generation>=0));

-- research_long_spec_source_r81
CREATE TABLE research_long_spec_source_r81(atom_id TEXT PRIMARY KEY REFERENCES research_atom_spec_v2(atom_id),epoch_hash TEXT NOT NULL CHECK(length(epoch_hash)=64),spec_hash TEXT NOT NULL UNIQUE CHECK(length(spec_hash)=64),registered_at INTEGER NOT NULL CHECK(registered_at>0));

-- research_long_spec_source_r81_no_delete
CREATE TRIGGER research_long_spec_source_r81_no_delete BEFORE DELETE ON research_long_spec_source_r81 BEGIN SELECT RAISE(ABORT,'LONG_IDENTITY_PERMANENT'); END;

-- research_long_spec_source_r81_no_replace
CREATE TRIGGER research_long_spec_source_r81_no_replace BEFORE INSERT ON research_long_spec_source_r81 WHEN EXISTS(SELECT 1 FROM research_long_spec_source_r81 x WHERE x.atom_id=NEW.atom_id) BEGIN SELECT RAISE(ABORT,'LONG_IDENTITY_REPLACE_FORBIDDEN'); END;

-- research_mainline_contribution_r929
CREATE TABLE research_mainline_contribution_r929(target_issue TEXT NOT NULL,consumer TEXT NOT NULL,candidate_id TEXT NOT NULL,certificate_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,requested_weight REAL NOT NULL,authorized_weight REAL NOT NULL,before_rank1_hash TEXT NOT NULL,after_rank1_hash TEXT NOT NULL,input_hash TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,record_hash TEXT PRIMARY KEY NOT NULL,created_at INTEGER NOT NULL,UNIQUE(target_issue,consumer,candidate_id));

-- research_matrix_checkpoint_v2
CREATE TABLE research_matrix_checkpoint_v2(
 matrix_id TEXT PRIMARY KEY, matrix_type TEXT NOT NULL, partition_key TEXT NOT NULL,
 window_size INTEGER NOT NULL CHECK(window_size>=1), feature_manifest_hash TEXT NOT NULL,
 schema_hash TEXT NOT NULL, version INTEGER NOT NULL CHECK(version>=0),
 through_issue TEXT NOT NULL, last_receipt TEXT NOT NULL, payload_codec TEXT NOT NULL,
 payload BLOB NOT NULL CHECK(length(payload)<=262144), payload_hash TEXT NOT NULL,
 sample_count INTEGER NOT NULL CHECK(sample_count>=0), updated_at INTEGER NOT NULL,
 UNIQUE(matrix_type,partition_key,window_size,feature_manifest_hash));

-- research_matrix_input_cp12
CREATE TABLE research_matrix_input_cp12(
 matrix_id TEXT NOT NULL REFERENCES research_matrix_spec_cp12(matrix_id) ON DELETE RESTRICT,
 source_receipt_hash TEXT NOT NULL CHECK(length(source_receipt_hash)=64),
 source_issue TEXT NOT NULL,
 source_ordinal INTEGER NOT NULL CHECK(source_ordinal>=0),
 input_hash TEXT NOT NULL CHECK(length(input_hash)=64),
 proof_hash TEXT NOT NULL CHECK(length(proof_hash)=64),
 input_payload BLOB NOT NULL CHECK(length(input_payload) BETWEEN 45 AND 16384),
 state TEXT NOT NULL DEFAULT 'READY' CHECK(state IN('READY','DONE','QUARANTINED','REPLAY_REQUIRED','EXPIRED_ARCHIVE_REQUIRED')),
 last_error TEXT NOT NULL DEFAULT '',
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 PRIMARY KEY(matrix_id,source_receipt_hash));

-- research_matrix_regime5_cp12
CREATE TABLE research_matrix_regime5_cp12(
 target_issue TEXT NOT NULL, position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
 policy_hash TEXT NOT NULL CHECK(length(policy_hash)=64),
 history_hash TEXT NOT NULL CHECK(length(history_hash)=64),
 score_hash TEXT NOT NULL CHECK(length(score_hash)=64),
 regime_id INTEGER NOT NULL CHECK(regime_id BETWEEN 0 AND 4),
 scores BLOB NOT NULL CHECK(length(scores)=40),
 captured_at INTEGER NOT NULL CHECK(captured_at>0),
 PRIMARY KEY(target_issue,position));

-- research_matrix_spec_cp12
CREATE TABLE research_matrix_spec_cp12(
 matrix_id TEXT PRIMARY KEY CHECK(length(matrix_id)=64),
 matrix_type TEXT NOT NULL,
 partition_key TEXT NOT NULL CHECK(length(partition_key) BETWEEN 1 AND 256),
 window_size INTEGER NOT NULL CHECK(window_size BETWEEN 1 AND 5000),
 spec_manifest_hash TEXT NOT NULL CHECK(length(spec_manifest_hash)=64),
 schema_hash TEXT NOT NULL CHECK(length(schema_hash)=64),
 UNIQUE(matrix_type,partition_key,window_size,spec_manifest_hash));

-- research_matrix_terminal_cp12
CREATE TABLE research_matrix_terminal_cp12(
 matrix_id TEXT NOT NULL REFERENCES research_matrix_spec_cp12(matrix_id) ON DELETE RESTRICT,
 source_receipt_hash TEXT NOT NULL CHECK(length(source_receipt_hash)=64),
 input_hash TEXT NOT NULL CHECK(length(input_hash)=64), proof_hash TEXT NOT NULL CHECK(length(proof_hash)=64),
 terminal_hash TEXT NOT NULL CHECK(length(terminal_hash)=64),
 reason TEXT NOT NULL CHECK(reason='MATRIX_INPUT_EXPIRED_WITHOUT_PROJECTION'),
 source_issue TEXT NOT NULL, created_at INTEGER NOT NULL,
 PRIMARY KEY(matrix_id,source_receipt_hash));

-- research_model_bundle_r929
CREATE TABLE research_model_bundle_r929(bundle_hash TEXT PRIMARY KEY NOT NULL,bundle_id TEXT NOT NULL,game TEXT NOT NULL,schema_hash TEXT NOT NULL,code_hash TEXT NOT NULL,manifest_hash TEXT NOT NULL,key_id TEXT NOT NULL,signature_algorithm TEXT NOT NULL,signature_verified INTEGER NOT NULL,artifact_count INTEGER NOT NULL,total_bytes INTEGER NOT NULL,created_issue TEXT NOT NULL,earliest_issue TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,verified_at INTEGER NOT NULL,activated_at INTEGER NOT NULL DEFAULT 0,rollback_of TEXT NOT NULL DEFAULT '',row_hash TEXT NOT NULL);

-- research_module_ablation_r932
CREATE TABLE research_module_ablation_r932(target_issue TEXT NOT NULL,module TEXT NOT NULL,source_hash TEXT NOT NULL,code_hash TEXT NOT NULL,state TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,module,source_hash,code_hash));

-- research_module_cal_model_r932
CREATE TABLE research_module_cal_model_r932(identity_hash TEXT PRIMARY KEY,validation_until TEXT NOT NULL,model_json TEXT NOT NULL,model_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_module_cal_observation_r932
CREATE TABLE research_module_cal_observation_r932(identity_hash TEXT NOT NULL,target_issue TEXT NOT NULL,source_hash TEXT NOT NULL,original_score REAL NOT NULL,counter_score REAL NOT NULL,original_hit INTEGER NOT NULL CHECK(original_hit IN (0,1)),counter_hit INTEGER NOT NULL CHECK(counter_hit IN (0,1)),record_hash TEXT NOT NULL,PRIMARY KEY(identity_hash,target_issue));

-- research_molecule_import_checkpoint_cp12
CREATE TABLE research_molecule_import_checkpoint_cp12(source_hash TEXT PRIMARY KEY REFERENCES research_molecule_import_cp12(source_hash),asset_name TEXT NOT NULL,expected_rows INTEGER NOT NULL CHECK(expected_rows=4200),expected_bytes INTEGER NOT NULL CHECK(expected_bytes=3265020),legacy_target_row INTEGER NOT NULL CHECK(legacy_target_row BETWEEN 0 AND 4200),scan_row INTEGER NOT NULL CHECK(scan_row BETWEEN 0 AND 4200),scan_offset INTEGER NOT NULL CHECK(scan_offset BETWEEN 0 AND 3265020),scan_prefix TEXT NOT NULL CHECK(length(scan_prefix)=64),scan_sha_state BLOB NOT NULL CHECK(length(scan_sha_state)=111),material_row INTEGER NOT NULL CHECK(material_row BETWEEN 0 AND 4200),material_offset INTEGER NOT NULL CHECK(material_offset BETWEEN 0 AND 3265020),material_prefix TEXT NOT NULL CHECK(length(material_prefix)=64),material_sha_state BLOB NOT NULL CHECK(length(material_sha_state)=111),generation INTEGER NOT NULL DEFAULT 0 CHECK(generation>=0),state TEXT NOT NULL CHECK(state IN('SCANNING','MATERIALIZING','COMPLETE')),updated_at INTEGER NOT NULL,CHECK(state='SCANNING' OR(scan_row=4200 AND scan_offset=3265020 AND scan_prefix=source_hash)),CHECK(state<>'COMPLETE' OR(material_row=4200 AND material_offset=3265020 AND material_prefix=source_hash)));

-- research_molecule_import_cp12
CREATE TABLE research_molecule_import_cp12(source_hash TEXT PRIMARY KEY CHECK(length(source_hash)=64),asset_name TEXT NOT NULL,expected_rows INTEGER NOT NULL CHECK(expected_rows=4200),next_row INTEGER NOT NULL CHECK(next_row BETWEEN 0 AND 4200),state TEXT NOT NULL CHECK(state IN('IMPORTING','COMPLETE')),generation INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL);

-- research_molecule_seed_v2
CREATE TABLE research_molecule_seed_v2(
 seed_hash TEXT PRIMARY KEY,source_seed_id TEXT NOT NULL,
 position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
 graph_json TEXT NOT NULL,output_type TEXT NOT NULL CHECK(output_type IN ('BOOL','SCALAR')),
 state TEXT NOT NULL CHECK(state IN ('DISCOVERED','VALIDATED','ACTIVE','RETIRED')),
 compiler_revision TEXT NOT NULL,source_hash TEXT NOT NULL,alias_of TEXT,imported_at INTEGER NOT NULL,
 UNIQUE(source_seed_id,position,compiler_revision));

-- research_molecule_use_cp12
CREATE TABLE research_molecule_use_cp12(seed_hash TEXT PRIMARY KEY REFERENCES research_molecule_seed_v2(seed_hash),ast_hash TEXT NOT NULL,action_id TEXT NOT NULL UNIQUE REFERENCES research_action_v2(action_id),candidate_id TEXT NOT NULL UNIQUE,state TEXT NOT NULL CHECK(state IN('PROPOSED','REGISTERED','NO_OP','HELD')),created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- research_new_asset_certificate_v41
CREATE TABLE research_new_asset_certificate_v41(certificate_id TEXT PRIMARY KEY,asset_id TEXT NOT NULL,asset_version_hash TEXT NOT NULL,display_name_zh TEXT NOT NULL,evidence_start_issue TEXT NOT NULL,evidence_end_issue TEXT NOT NULL,future_n INTEGER NOT NULL,future_hit_rate REAL NOT NULL,wilson_lower REAL NOT NULL,brier REAL NOT NULL,ece REAL NOT NULL,coverage REAL NOT NULL,block_min REAL NOT NULL,regime_min REAL NOT NULL,max_miss INTEGER NOT NULL,paired_n INTEGER NOT NULL,paired_delta REAL NOT NULL,incremental_info REAL NOT NULL,redundancy REAL NOT NULL,fdr_q REAL NOT NULL,status TEXT NOT NULL,revocation_code TEXT NOT NULL DEFAULT '',evidence_hash TEXT NOT NULL,created_ms INTEGER NOT NULL,updated_ms INTEGER NOT NULL,UNIQUE(asset_id,asset_version_hash));

-- research_new_asset_freeze_v41
CREATE TABLE research_new_asset_freeze_v41(asset_id TEXT NOT NULL,target_issue TEXT NOT NULL,asset_version_hash TEXT NOT NULL,base_issue TEXT NOT NULL,history_hash TEXT NOT NULL,surface_hash TEXT NOT NULL,history_rows INTEGER NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,action_ordinal INTEGER NOT NULL,score REAL NOT NULL,confidence REAL NOT NULL,risk REAL NOT NULL,stability REAL NOT NULL,output_type TEXT NOT NULL DEFAULT 'SCALAR_SCORE',output_shape TEXT NOT NULL DEFAULT '1',output_blob TEXT NOT NULL DEFAULT '',output_hash TEXT NOT NULL DEFAULT '',context_blob TEXT NOT NULL,context_digest TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,frozen_ms INTEGER NOT NULL,actual_number INTEGER NOT NULL DEFAULT 0,hit_label INTEGER NOT NULL DEFAULT -1 CHECK(hit_label IN(-1,0,1)),base_hit INTEGER NOT NULL DEFAULT -1,brier REAL NOT NULL DEFAULT 0,settled_ms INTEGER NOT NULL DEFAULT 0,outcome_hash TEXT NOT NULL DEFAULT '',PRIMARY KEY(asset_id,target_issue));

-- research_new_asset_genome_v41
CREATE TABLE research_new_asset_genome_v41(asset_id TEXT PRIMARY KEY,namespace TEXT NOT NULL CHECK(namespace='GENESIS_NEW'),display_name_zh TEXT NOT NULL,mechanism_spec_zh TEXT NOT NULL,genome_spec TEXT NOT NULL,structural_hash TEXT NOT NULL UNIQUE,behavior_hash TEXT NOT NULL UNIQUE,family_id TEXT NOT NULL,output_type TEXT NOT NULL,generation INTEGER NOT NULL,parent_ids TEXT NOT NULL DEFAULT '',creation_reason TEXT NOT NULL,trained_until_issue TEXT NOT NULL DEFAULT '',earliest_use_issue TEXT NOT NULL DEFAULT '',state_blob TEXT NOT NULL,state_updates INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,may_authorize_real INTEGER NOT NULL DEFAULT 0 CHECK(may_authorize_real=0),future_n INTEGER NOT NULL DEFAULT 0,future_hit INTEGER NOT NULL DEFAULT 0,future_hit_rate REAL NOT NULL DEFAULT 0,future_wilson_lower REAL NOT NULL DEFAULT 0,future_brier REAL NOT NULL DEFAULT 1,future_ece REAL NOT NULL DEFAULT 1,future_coverage REAL NOT NULL DEFAULT 0,future_block_min REAL NOT NULL DEFAULT 0,future_regime_min REAL NOT NULL DEFAULT 0,future_max_miss INTEGER NOT NULL DEFAULT 0,paired_mainline_n INTEGER NOT NULL DEFAULT 0,paired_mainline_delta REAL NOT NULL DEFAULT 0,incremental_info REAL NOT NULL DEFAULT 0,redundancy REAL NOT NULL DEFAULT 1,fdr_q REAL NOT NULL DEFAULT 1,evidence_hash TEXT NOT NULL DEFAULT '',lifecycle_reason TEXT NOT NULL DEFAULT '',created_ms INTEGER NOT NULL,updated_ms INTEGER NOT NULL);

-- research_new_atom_proposal_v41
CREATE TABLE research_new_atom_proposal_v41(proposal_id TEXT PRIMARY KEY,namespace TEXT NOT NULL CHECK(namespace='ATOM_PROPOSAL'),proposed_name TEXT NOT NULL,value_type TEXT NOT NULL,definition_spec TEXT NOT NULL,source_atom_ids TEXT NOT NULL,leakage_class TEXT NOT NULL,update_timing TEXT NOT NULL,value_range TEXT NOT NULL,max_lag INTEGER NOT NULL,cost_class TEXT NOT NULL,consumer_family TEXT NOT NULL,failure_vector TEXT NOT NULL,reducibility_evidence TEXT NOT NULL,status TEXT NOT NULL,reject_reason TEXT NOT NULL DEFAULT '',future_n INTEGER NOT NULL DEFAULT 0,incremental_info REAL NOT NULL DEFAULT 0,activation_schema_version TEXT NOT NULL DEFAULT '',semantic_digest TEXT NOT NULL UNIQUE,created_issue TEXT NOT NULL DEFAULT '',created_ms INTEGER NOT NULL,updated_ms INTEGER NOT NULL);

-- research_new_contribution_v41
CREATE TABLE research_new_contribution_v41(target_issue TEXT NOT NULL,asset_id TEXT NOT NULL,asset_version_hash TEXT NOT NULL,certificate_id TEXT NOT NULL,display_name_zh TEXT NOT NULL,production_rank1_hash TEXT NOT NULL,mixed_rank1_hash TEXT NOT NULL,influence_weight REAL NOT NULL,top_evaluated INTEGER NOT NULL,evidence_hash TEXT NOT NULL,status TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,created_ms INTEGER NOT NULL,PRIMARY KEY(target_issue,asset_id));

-- research_observation_restart_r932
CREATE TABLE research_observation_restart_r932(generation_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,start_window_no INTEGER NOT NULL,previous_window_id TEXT NOT NULL UNIQUE,family_id TEXT NOT NULL,claim_key TEXT NOT NULL,first_issue TEXT NOT NULL,prior_state TEXT NOT NULL,prior_reason TEXT NOT NULL,contract_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,selector_hash TEXT NOT NULL,structural_hash TEXT NOT NULL,record_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_observation_restart_r932_immutable_delete
CREATE TRIGGER research_observation_restart_r932_immutable_delete BEFORE DELETE ON research_observation_restart_r932  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='research_observation_restart_r932' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R932_OBSERVATION_HISTORY_PERMANENT'); END;

-- research_observation_restart_r932_immutable_update
CREATE TRIGGER research_observation_restart_r932_immutable_update BEFORE UPDATE ON research_observation_restart_r932 BEGIN SELECT RAISE(ABORT,'R932_OBSERVATION_GENERATION_IMMUTABLE'); END;

-- research_paired_challenge_r932
CREATE TABLE research_paired_challenge_r932(challenge_id TEXT PRIMARY KEY,purpose TEXT NOT NULL,parent_key TEXT NOT NULL,child_key TEXT NOT NULL,parent_structure TEXT NOT NULL,child_structure TEXT NOT NULL,reason_hash TEXT NOT NULL,state TEXT NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,parent_cal TEXT NOT NULL,child_cal TEXT NOT NULL,contract_hash TEXT NOT NULL,result_json TEXT NOT NULL,result_hash TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- research_plan_migration_v2
CREATE TABLE research_plan_migration_v2(
 migration_id TEXT PRIMARY KEY, source_schema_hash TEXT NOT NULL,
 target_schema_hash TEXT NOT NULL, applied_at INTEGER NOT NULL,
 status TEXT NOT NULL CHECK(status IN ('APPLIED','ROLLED_FORWARD')));

-- research_plan_required_r967
CREATE TABLE research_plan_required_r967(window_id TEXT PRIMARY KEY,plan_hash TEXT NOT NULL);

-- research_plan_required_r967_immutable_delete
CREATE TRIGGER research_plan_required_r967_immutable_delete BEFORE DELETE ON research_plan_required_r967 BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- research_plan_required_r967_immutable_update
CREATE TRIGGER research_plan_required_r967_immutable_update BEFORE UPDATE ON research_plan_required_r967 BEGIN SELECT RAISE(ABORT,'R967_IMMUTABLE_EXPERIMENT_PLAN'); END;

-- research_platform_event_r929
CREATE TABLE research_platform_event_r929(event_hash TEXT PRIMARY KEY NOT NULL,event_type TEXT NOT NULL,entity_id TEXT NOT NULL,previous_hash TEXT NOT NULL,current_hash TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_projection_ack_v2
CREATE TABLE research_projection_ack_v2(
 consumer TEXT NOT NULL, source_hash TEXT NOT NULL, result_hash TEXT NOT NULL,
 through_issue TEXT NOT NULL, created_at INTEGER NOT NULL,
 PRIMARY KEY(consumer,source_hash));

-- research_proxy_capture_r932
CREATE TABLE research_proxy_capture_r932(target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,input_hash TEXT NOT NULL,selected_ordinal INTEGER NOT NULL,raw_score REAL NOT NULL,action_count INTEGER NOT NULL,record_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_key));

-- research_repair_child_r932
CREATE TABLE research_repair_child_r932(child_key TEXT PRIMARY KEY,parent_key TEXT NOT NULL,parent_structure TEXT NOT NULL,child_structure TEXT NOT NULL,repair_target TEXT NOT NULL,changed_modules TEXT NOT NULL,changed_atoms TEXT NOT NULL,expected_effect TEXT NOT NULL,falsification TEXT NOT NULL,kind TEXT NOT NULL,created_issue TEXT NOT NULL,plan_hash TEXT NOT NULL,challenged_id TEXT NOT NULL DEFAULT '',outcome TEXT NOT NULL DEFAULT 'PENDING',created_at INTEGER NOT NULL);

-- research_repair_group_r932
CREATE TABLE research_repair_group_r932(group_hash TEXT PRIMARY KEY,lane TEXT NOT NULL,parent_key TEXT NOT NULL,module TEXT NOT NULL,domain TEXT NOT NULL,boundary TEXT NOT NULL,bundle_hash TEXT NOT NULL,source_hash TEXT NOT NULL,failures INTEGER NOT NULL,gain INTEGER NOT NULL,consumed INTEGER NOT NULL,last_issue TEXT NOT NULL,evidence_hash TEXT NOT NULL);

-- research_result_binding_cp12_no_delete
CREATE TRIGGER research_result_binding_cp12_no_delete BEFORE DELETE ON research_action_result_binding_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_RESULT_BINDING_PERMANENT'); END;

-- research_result_binding_cp12_no_replace
CREATE TRIGGER research_result_binding_cp12_no_replace BEFORE INSERT ON research_action_result_binding_cp12 WHEN EXISTS(SELECT 1 FROM research_action_result_binding_cp12 WHERE action_id=NEW.action_id) BEGIN SELECT RAISE(ABORT,'RESEARCH_RESULT_BINDING_REPLACE_FORBIDDEN'); END;

-- research_result_binding_cp12_no_update
CREATE TRIGGER research_result_binding_cp12_no_update BEFORE UPDATE ON research_action_result_binding_cp12 BEGIN SELECT RAISE(ABORT,'RESEARCH_RESULT_ENDPOINT_IMMUTABLE'); END;

-- research_result_v2
CREATE TABLE research_result_v2(
 action_id TEXT NOT NULL REFERENCES research_action_v2(action_id),
 endpoint_n INTEGER NOT NULL CHECK(endpoint_n IN (120,360)),
 child_id TEXT NOT NULL, future_n INTEGER NOT NULL CHECK(future_n>=0),
 paired_n INTEGER NOT NULL CHECK(paired_n>=0), evidence_hash TEXT NOT NULL,
 metrics_json TEXT NOT NULL, verdict TEXT NOT NULL, recorded_at INTEGER NOT NULL,
 PRIMARY KEY(action_id,endpoint_n));

-- research_retention_permit_v2
CREATE TABLE research_retention_permit_v2(
 source_table TEXT NOT NULL,source_rowid INTEGER NOT NULL,archive_id TEXT NOT NULL REFERENCES research_evidence_archive_v2(archive_id),
 row_hash TEXT NOT NULL,process_session TEXT NOT NULL,created_at INTEGER NOT NULL, source_generation INTEGER NOT NULL DEFAULT 0 CHECK(source_generation>=0),
 PRIMARY KEY(source_table,source_rowid));

-- research_roster_manifest_r948
CREATE TABLE research_roster_manifest_r948(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload_hash TEXT NOT NULL,candidate_count INTEGER NOT NULL,created_at INTEGER NOT NULL);

-- research_roster_prepare_r943
CREATE TABLE research_roster_prepare_r943(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,cursor INTEGER NOT NULL,created_at INTEGER NOT NULL);

-- research_row_revision_v2
CREATE TABLE research_row_revision_v2(
 generation INTEGER PRIMARY KEY AUTOINCREMENT,
 source_table TEXT NOT NULL,source_rowid INTEGER NOT NULL,
 UNIQUE(source_table,source_rowid));

-- research_schedule_plan_r932
CREATE TABLE research_schedule_plan_r932(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,roster_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- research_schedule_task_r932
CREATE TABLE research_schedule_task_r932(target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,family TEXT NOT NULL,structure_hash TEXT NOT NULL,tier TEXT NOT NULL,selected INTEGER NOT NULL,queue_rank INTEGER NOT NULL, execution_mode TEXT NOT NULL DEFAULT 'FULL1200',PRIMARY KEY(target_issue,candidate_key));

-- research_semantic_identity_r932
CREATE TABLE research_semantic_identity_r932(candidate_key TEXT PRIMARY KEY,structural_hash TEXT NOT NULL,semantic_hash TEXT NOT NULL,domain_scope TEXT NOT NULL,registered_issue TEXT NOT NULL);

-- research_similarity_r932
CREATE TABLE research_similarity_r932(left_key TEXT NOT NULL,right_key TEXT NOT NULL,left_structure TEXT NOT NULL,right_structure TEXT NOT NULL,common_n INTEGER NOT NULL,correlation REAL,classification TEXT NOT NULL,evidence_json TEXT NOT NULL,evidence_hash TEXT NOT NULL,last_issue TEXT NOT NULL,checked_at INTEGER NOT NULL,PRIMARY KEY(left_key,right_key));

-- research_task_checkpoint_v2
CREATE TABLE research_task_checkpoint_v2(
 task_id TEXT PRIMARY KEY REFERENCES v57_research_task_r967(task_id),
 input_hash TEXT NOT NULL, model_hash TEXT NOT NULL,
 phase TEXT NOT NULL, cursor INTEGER NOT NULL CHECK(cursor>=0),
 partial_payload BLOB NOT NULL, payload_hash TEXT NOT NULL,
 lease_generation INTEGER NOT NULL CHECK(lease_generation>0),
 updated_at INTEGER NOT NULL);

-- research_task_input_binding_v2
CREATE TABLE research_task_input_binding_v2(
 task_id TEXT NOT NULL REFERENCES v57_research_task_r967(task_id),
 source_table TEXT NOT NULL,source_issue TEXT NOT NULL,
 source_key_hash TEXT NOT NULL,row_hash TEXT NOT NULL,
 archive_id TEXT NOT NULL REFERENCES research_evidence_archive_v2(archive_id),
 archive_hash TEXT NOT NULL,member_manifest_hash TEXT NOT NULL,
 created_at INTEGER NOT NULL,
 PRIMARY KEY(task_id,source_table,source_issue,source_key_hash));

-- research_task_retention_expiry_cp12
CREATE TABLE research_task_retention_expiry_cp12(
 task_id TEXT PRIMARY KEY,kind TEXT NOT NULL CHECK(kind='POST_TRAIN'),target_issue TEXT NOT NULL,
 input_hash TEXT NOT NULL,authority_ref TEXT NOT NULL,cursor_hash TEXT NOT NULL CHECK(length(cursor_hash)=64),
 prior_generation INTEGER NOT NULL CHECK(prior_generation>=0),cutoff_issue TEXT NOT NULL,
 reason TEXT NOT NULL CHECK(reason='POST_TRAIN_INPUT_EXPIRED_WITHOUT_REVIEW'),
 terminal_hash TEXT NOT NULL UNIQUE CHECK(length(terminal_hash)=64),created_at INTEGER NOT NULL);

-- research_training_attachment_cp12
CREATE TABLE research_training_attachment_cp12(
 attachment_hash TEXT PRIMARY KEY CHECK(length(attachment_hash)=64),
 relative_path TEXT NOT NULL UNIQUE,
 byte_count INTEGER NOT NULL CHECK(byte_count BETWEEN 1 AND 8388608),
 created_at INTEGER NOT NULL);

-- research_training_feature_cursor_cp12
CREATE TABLE research_training_feature_cursor_cp12(
 task_id TEXT PRIMARY KEY REFERENCES v57_research_task_r967(task_id),
 protocol_hash TEXT NOT NULL REFERENCES research_training_protocol_v2(protocol_hash),
 input_hash TEXT NOT NULL,next_index INTEGER NOT NULL CHECK(next_index BETWEEN 0 AND 1320),
 prefix_hash TEXT NOT NULL CHECK(length(prefix_hash)=64),updated_at INTEGER NOT NULL);

-- research_training_feature_row_cp11
CREATE TABLE research_training_feature_row_cp11(
 task_id TEXT NOT NULL REFERENCES v57_research_task_r967(task_id),
 row_index INTEGER NOT NULL CHECK(row_index BETWEEN 0 AND 1319),
 protocol_hash TEXT NOT NULL REFERENCES research_training_protocol_v2(protocol_hash),
 input_hash TEXT NOT NULL, target_issue TEXT NOT NULL,
 role TEXT NOT NULL CHECK(role IN('TRAIN','VALIDATION')),
 actual_number INTEGER NOT NULL CHECK(actual_number BETWEEN 1 AND 10),
 feature_payload BLOB NOT NULL CHECK(length(feature_payload) BETWEEN 18 AND 8192),
 feature_hash TEXT NOT NULL CHECK(length(feature_hash)=64),
 source_receipt_hash TEXT NOT NULL CHECK(length(source_receipt_hash)=64),
 raw_row_hash TEXT NOT NULL CHECK(length(raw_row_hash)=64),
 created_at INTEGER NOT NULL,
 PRIMARY KEY(task_id,row_index), UNIQUE(task_id,target_issue));

-- research_training_input_v2
CREATE TABLE research_training_input_v2(
 input_hash TEXT PRIMARY KEY CHECK(length(input_hash)=64),
 relative_path TEXT NOT NULL UNIQUE,
 byte_count INTEGER NOT NULL CHECK(byte_count BETWEEN 1 AND 8388608),
 row_count INTEGER NOT NULL CHECK(row_count BETWEEN 1 AND 5000),
 first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,
 schema_hash TEXT NOT NULL, source_manifest_hash TEXT NOT NULL,
 created_at INTEGER NOT NULL,
 CHECK(first_issue<=last_issue));

-- research_training_protocol_attachment_cp12
CREATE TABLE research_training_protocol_attachment_cp12(
 protocol_hash TEXT NOT NULL REFERENCES research_training_protocol_v2(protocol_hash),
 attachment_hash TEXT NOT NULL REFERENCES research_training_attachment_cp12(attachment_hash),
 PRIMARY KEY(protocol_hash,attachment_hash));

-- research_training_protocol_v2
CREATE TABLE research_training_protocol_v2(
 protocol_hash TEXT PRIMARY KEY CHECK(length(protocol_hash)=64),
 candidate_id TEXT NOT NULL UNIQUE REFERENCES r952_candidate(candidate_id),
 input_hash TEXT NOT NULL REFERENCES research_training_input_v2(input_hash),
 ast_hash TEXT NOT NULL,feature_manifest_hash TEXT NOT NULL,
 model_kind TEXT NOT NULL CHECK(model_kind IN
 ('FIXED_GRAPH','SOFTMAX_LINEAR','OVR_LOGISTIC','HISTOGRAM_TREE','EXPERT_HEDGE','MLP_D32_16_10')),
 train_first TEXT NOT NULL,train_last TEXT NOT NULL,
 embargo_issue TEXT NOT NULL,validation_first TEXT NOT NULL,validation_last TEXT NOT NULL,
 train_n INTEGER NOT NULL CHECK(train_n BETWEEN 300 AND 1200),
 validation_n INTEGER NOT NULL CHECK(validation_n=120),
 seed INTEGER NOT NULL,protocol_json TEXT NOT NULL CHECK(length(protocol_json)<=65536),
 registered_issue TEXT NOT NULL,created_at INTEGER NOT NULL,
 CHECK(train_first<=train_last AND train_last<embargo_issue
       AND embargo_issue<validation_first AND validation_first<=validation_last
       AND validation_last<registered_issue));

-- research_training_registration_row_cp12
CREATE TABLE research_training_registration_row_cp12(
 action_id TEXT NOT NULL REFERENCES research_action_v2(action_id),
 selection_hash TEXT NOT NULL CHECK(length(selection_hash)=64),
 row_index INTEGER NOT NULL CHECK(row_index BETWEEN 0 AND 1319),
 payload TEXT NOT NULL CHECK(length(CAST(payload AS BLOB)) BETWEEN 1 AND 65535),
 payload_hash TEXT NOT NULL CHECK(length(payload_hash)=64),created_at INTEGER NOT NULL,
 PRIMARY KEY(action_id,selection_hash,row_index));

-- research_trial_registry_r929
CREATE TABLE research_trial_registry_r929(trial_id TEXT PRIMARY KEY NOT NULL,identity_hash TEXT NOT NULL UNIQUE,stage TEXT NOT NULL,status TEXT NOT NULL,dataset_hash TEXT NOT NULL,schema_hash TEXT NOT NULL,source_hash TEXT NOT NULL,hyper_hash TEXT NOT NULL,family_id TEXT NOT NULL,structural_hash TEXT NOT NULL,split_hash TEXT NOT NULL,seed INTEGER NOT NULL,sealed_token_hash TEXT NOT NULL DEFAULT '',sealed_consumed INTEGER NOT NULL DEFAULT 0,output_hash TEXT NOT NULL DEFAULT '',failure_code TEXT NOT NULL DEFAULT '',registered_at INTEGER NOT NULL,started_at INTEGER NOT NULL DEFAULT 0,finished_at INTEGER NOT NULL DEFAULT 0,record_hash TEXT NOT NULL);

-- research_window_generation_r932
CREATE TABLE research_window_generation_r932(window_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,generation_id TEXT NOT NULL,start_window_no INTEGER NOT NULL,stage_no INTEGER NOT NULL,claim_key TEXT NOT NULL,record_hash TEXT NOT NULL);

-- research_window_generation_r932_immutable_delete
CREATE TRIGGER research_window_generation_r932_immutable_delete BEFORE DELETE ON research_window_generation_r932 BEGIN SELECT RAISE(ABORT,'R932_OBSERVATION_HISTORY_PERMANENT'); END;

-- research_window_generation_r932_immutable_update
CREATE TRIGGER research_window_generation_r932_immutable_update BEFORE UPDATE ON research_window_generation_r932 BEGIN SELECT RAISE(ABORT,'R932_OBSERVATION_GENERATION_IMMUTABLE'); END;

-- research_window_publication_r932
CREATE TABLE research_window_publication_r932(window_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,result_hash TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,cursor_issue TEXT NOT NULL,verified_n INTEGER NOT NULL,proofs_hash TEXT NOT NULL,max_cost_ns INTEGER NOT NULL,certificate_hash TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- research_working_storage_cp12
CREATE TABLE research_working_storage_cp12(bucket TEXT PRIMARY KEY CHECK(bucket IN('FEATURE_REGISTRATION','CHECKPOINT','MATRIX_CHECKPOINT')),byte_count INTEGER NOT NULL CHECK(byte_count>=0));

-- research_working_storage_ref_cp12
CREATE TABLE research_working_storage_ref_cp12(source_table TEXT NOT NULL,source_key TEXT NOT NULL,bucket TEXT NOT NULL REFERENCES research_working_storage_cp12(bucket),byte_count INTEGER NOT NULL CHECK(byte_count>=0),PRIMARY KEY(source_table,source_key));

-- rule_health_matrix
CREATE TABLE rule_health_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, rule_key TEXT NOT NULL UNIQUE, state_key TEXT, rule_name TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, max_lose INTEGER DEFAULT 0, current_lose INTEGER DEFAULT 0, edge_decay_risk REAL DEFAULT 0, is_active INTEGER DEFAULT 0, is_frozen INTEGER DEFAULT 0, freeze_until_issue TEXT, updated_at INTEGER DEFAULT 0, summary_text TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- runtime_profile_cache
CREATE TABLE runtime_profile_cache (_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, avg_final_p REAL DEFAULT 0, avg_gate REAL DEFAULT 0, risk_score REAL DEFAULT 0, teacher_weight REAL DEFAULT 1, profile_score REAL DEFAULT 0, state_confidence REAL DEFAULT 0, updated_at INTEGER DEFAULT 0, summary_text TEXT);

-- seed_cause_expansion
CREATE TABLE seed_cause_expansion (_id INTEGER PRIMARY KEY AUTOINCREMENT, seed_key TEXT NOT NULL UNIQUE, seed_source_key TEXT, state_key TEXT, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, purity_score REAL DEFAULT 0, seed_similarity REAL DEFAULT 0, false_seed_similarity REAL DEFAULT 0, expansion_pass INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0, summary_text TEXT);

-- self_diagnosis_log
CREATE TABLE self_diagnosis_log (_id INTEGER PRIMARY KEY AUTOINCREMENT, diagnosis_id TEXT NOT NULL UNIQUE, level TEXT, main_code TEXT, main_reason_code TEXT, main_problem TEXT, affected_module TEXT, suggested_action TEXT, short_text TEXT, summary_text TEXT, diagnosis_text TEXT, repair_advice TEXT, history_count INTEGER DEFAULT 0, valid_draw_count INTEGER DEFAULT 0, invalid_draw_count INTEGER DEFAULT 0, duplicate_issue_count INTEGER DEFAULT 0, latest_issue TEXT, candidate_count INTEGER DEFAULT 0, valid_candidate_count INTEGER DEFAULT 0, invalid_candidate_count INTEGER DEFAULT 0, over_gate_candidate_count INTEGER DEFAULT 0, blocked_candidate_count INTEGER DEFAULT 0, real_trade_count INTEGER DEFAULT 0, shadow_trade_count INTEGER DEFAULT 0, hold_trade_count INTEGER DEFAULT 0, trade_hit_count INTEGER DEFAULT 0, trade_miss_count INTEGER DEFAULT 0, mixed_stat_count INTEGER DEFAULT 0, profile_count INTEGER DEFAULT 0, active_profile_count INTEGER DEFAULT 0, stale_profile_count INTEGER DEFAULT 0, weak_profile_count INTEGER DEFAULT 0, teacher_matrix_sample_count INTEGER DEFAULT 0, teacher_matrix_profile_count INTEGER DEFAULT 0, teacher_matrix_strong_count INTEGER DEFAULT 0, teacher_matrix_risk_count INTEGER DEFAULT 0, reason_sample_count INTEGER DEFAULT 0, reason_profile_count INTEGER DEFAULT 0, false_reason_count INTEGER DEFAULT 0, elite45_pass_count INTEGER DEFAULT 0, elite45_block_count INTEGER DEFAULT 0, fatal_count INTEGER DEFAULT 0, bad_count INTEGER DEFAULT 0, warn_count INTEGER DEFAULT 0, data_text TEXT, candidate_text TEXT, trade_text TEXT, profile_text TEXT, teacher_matrix_text TEXT, reason_text TEXT, elite45_text TEXT, decision_text TEXT, runtime_text TEXT, best_candidate_text TEXT, created_at INTEGER DEFAULT 0);

-- service_recovery_event_v20
CREATE TABLE service_recovery_event_v20 (_id INTEGER PRIMARY KEY AUTOINCREMENT, event_at INTEGER DEFAULT 0, heartbeat_age_ms INTEGER DEFAULT 0, source TEXT, stage TEXT, result TEXT, detail TEXT, version TEXT);

-- shadow_candidates
CREATE TABLE shadow_candidates (_id INTEGER PRIMARY KEY AUTOINCREMENT, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, rank_no INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, shape_key TEXT, state_key TEXT, state_mode TEXT, model_p REAL DEFAULT 0, shape_p REAL DEFAULT 0, combo_p REAL DEFAULT 0, recovery_p REAL DEFAULT 0, similar_p REAL DEFAULT 0, memory_p REAL DEFAULT 0, raw_p REAL DEFAULT 0, calibrated_p REAL DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, similar_hit_rate REAL DEFAULT 0, combo_hit_rate REAL DEFAULT 0, effective_sample INTEGER DEFAULT 0, combo_sample INTEGER DEFAULT 0, trend_life REAL DEFAULT 0, drift_risk REAL DEFAULT 0, break_depth REAL DEFAULT 0, recovery_score REAL DEFAULT 0, champion_gap REAL DEFAULT 0, combo_gap REAL DEFAULT 0, hot_reverse_risk REAL DEFAULT 0, false_strong_risk REAL DEFAULT 0, calibration_error REAL DEFAULT 0, probability_mode TEXT, backtest_mode TEXT, deep_score REAL DEFAULT 0, deep_confidence REAL DEFAULT 0, deep_feature_p REAL DEFAULT 0, deep_position_score REAL DEFAULT 0, deep_number_score REAL DEFAULT 0, deep_cycle_score REAL DEFAULT 0, deep_transition_score REAL DEFAULT 0, deep_state_score REAL DEFAULT 0, bias_score REAL DEFAULT 0, bias_confidence REAL DEFAULT 0, profile_score REAL DEFAULT 0, fallback_profile_score REAL DEFAULT 0, runtime_profile_score REAL DEFAULT 0, state_number_profile_score REAL DEFAULT 0, number_bias REAL DEFAULT 0, combo_bias REAL DEFAULT 0, teacher_weight REAL DEFAULT 1, teacher_matrix_weight REAL DEFAULT 1, teacher_matrix_score REAL DEFAULT 0, state_confidence REAL DEFAULT 0, audit_penalty REAL DEFAULT 0, audit_boost REAL DEFAULT 0, promotion_score REAL DEFAULT 0, a_plus_score REAL DEFAULT 0, evidence_score REAL DEFAULT 0, evidence_risk REAL DEFAULT 0, ai_judge_score REAL DEFAULT 0, ai_judge_confidence REAL DEFAULT 0, conflict_level REAL DEFAULT 0, weakest_number_score REAL DEFAULT 0, balance_score REAL DEFAULT 0, rank_score REAL DEFAULT 0, final_rank_score REAL DEFAULT 0, state_sample INTEGER DEFAULT 0, cache_sample INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, deep_profile_key TEXT, deep_profile_hit_rate REAL DEFAULT 0, deep_profile_sample INTEGER DEFAULT 0, deep_fallback_level INTEGER DEFAULT 0, fallback_profile_key TEXT, candidate_evidence_key TEXT, promotion_status TEXT, a_plus_status TEXT, deep_reason TEXT, ai_judge_summary TEXT, teacher_matrix_sample INTEGER DEFAULT 0, teacher_matrix_hit_rate REAL DEFAULT 0, teacher_matrix_recent_hit_rate REAL DEFAULT 0, teacher_matrix_confidence REAL DEFAULT 0, teacher_matrix_prob_adjust REAL DEFAULT 0, teacher_matrix_gate_adjust REAL DEFAULT 0, teacher_matrix_risk_penalty REAL DEFAULT 0, teacher_matrix_applied INTEGER DEFAULT 0, teacher_matrix_profile_key TEXT, teacher_matrix_combo_key TEXT, teacher_matrix_summary TEXT, source_type TEXT, is_realtime_real INTEGER DEFAULT 0, pre_draw_reason_fingerprint TEXT, pre_draw_reason_text TEXT, reason_valid_level TEXT, reason_score REAL DEFAULT 0, reason_edge REAL DEFAULT 0, hit_similarity REAL DEFAULT 0, miss_similarity REAL DEFAULT 0, contradiction_score REAL DEFAULT 0, causal_lift REAL DEFAULT 0, causal_confidence REAL DEFAULT 0, valid_reason_count INTEGER DEFAULT 0, false_reason_count INTEGER DEFAULT 0, forward_reason_sample INTEGER DEFAULT 0, forward_reason_hit_rate REAL DEFAULT 0, reason_wilson_score REAL DEFAULT 0, k3_role_summary TEXT, k3_main_number INTEGER DEFAULT 0, k3_support_number INTEGER DEFAULT 0, k3_defense_number INTEGER DEFAULT 0, combo_role_diversity REAL DEFAULT 0, combo_conflict REAL DEFAULT 0, fingerprint_cooling INTEGER DEFAULT 0, elite45_passed INTEGER DEFAULT 0, elite45_status TEXT, elite45_block_code TEXT, elite45_gate REAL DEFAULT 0, elite45_summary TEXT, v20_grade TEXT, v20_action TEXT, v20_candidate_key TEXT, v20_chain_key TEXT, v20_audit_key TEXT, v20_source_key TEXT, v20_rule_key TEXT, v20_same_class_key TEXT, v20_state_key TEXT, v20_position_key TEXT, v20_teacher_key TEXT, v20_rank_key TEXT, v20_guard_key TEXT, v20_shadow_key TEXT, v20_freeze_key TEXT, v20_seed_key TEXT, v20_final_cause_score REAL DEFAULT 0, v20_support_score REAL DEFAULT 0, v20_counter_score REAL DEFAULT 0, v20_confidence REAL DEFAULT 0, v20_sample_trust REAL DEFAULT 0, v20_recent_effectiveness REAL DEFAULT 0, v20_action_confidence REAL DEFAULT 0, v20_teacher_matrix_score REAL DEFAULT 0, v20_position_edge REAL DEFAULT 0, v20_number_teacher_edge REAL DEFAULT 0, v20_combo_role_edge REAL DEFAULT 0, v20_third_guard_edge REAL DEFAULT 0, v20_rank_counterfactual_edge REAL DEFAULT 0, v20_source_health_edge REAL DEFAULT 0, v20_state_health_edge REAL DEFAULT 0, v20_cycle_health_edge REAL DEFAULT 0, v20_seed_similarity REAL DEFAULT 0, v20_feature_distance REAL DEFAULT 0, v20_exact_state_match INTEGER DEFAULT 0, v20_relaxed_state_match INTEGER DEFAULT 0, v20_timing_offset INTEGER DEFAULT 0, v20_timing_bucket TEXT, v20_shadow_health REAL DEFAULT 0, v20_false_strong_risk REAL DEFAULT 0, v20_rule_decay_risk REAL DEFAULT 0, v20_same_class_lose_risk REAL DEFAULT 0, v20_guard_drag_risk REAL DEFAULT 0, v20_position_wrong_risk REAL DEFAULT 0, v20_position_healthy INTEGER DEFAULT 0, v20_number_teacher_support INTEGER DEFAULT 0, v20_rank_counterfactual_ok INTEGER DEFAULT 0, v20_third_guard_ok INTEGER DEFAULT 0, v20_shadow_health_pass INTEGER DEFAULT 0, v20_false_strong_frozen INTEGER DEFAULT 0, v20_same_class_frozen INTEGER DEFAULT 0, v20_rule_active INTEGER DEFAULT 0, v20_rule_frozen INTEGER DEFAULT 0, v20_source_healthy INTEGER DEFAULT 0, v20_state_healthy INTEGER DEFAULT 0, v20_reverse_reason_code TEXT, v20_action_suggestion TEXT, v20_evidence_json TEXT, v20_counter_evidence_json TEXT, v20_reason_atoms TEXT, v20_source_votes TEXT, v20_risk_tags TEXT, v20_decision_reason TEXT, v20_hold_reason TEXT, v20_miss_reason TEXT, dynamic_cluster_id TEXT, dynamic_cluster_status TEXT, dynamic_cluster_prototype_key TEXT, dynamic_cluster_feature_key TEXT, dynamic_cluster_sample INTEGER DEFAULT 0, dynamic_cluster_hit_count INTEGER DEFAULT 0, dynamic_cluster_miss_count INTEGER DEFAULT 0, dynamic_cluster_recent_sample INTEGER DEFAULT 0, dynamic_cluster_recent_hit_count INTEGER DEFAULT 0, dynamic_cluster_hit_rate REAL DEFAULT 0, dynamic_cluster_recent_hit_rate REAL DEFAULT 0, dynamic_cluster_confidence REAL DEFAULT 0, dynamic_cluster_distance REAL DEFAULT 0, dynamic_cluster_volatility REAL DEFAULT 0, dynamic_cluster_max_lose INTEGER DEFAULT 0, dynamic_cluster_counter_score REAL DEFAULT 0, dynamic_cluster_version TEXT, dynamic_cluster_reason TEXT, position_need_key TEXT, position_need_summary TEXT, position_need_strength REAL DEFAULT 0, position_need_chaos_risk REAL DEFAULT 0, position_need_repair_score REAL DEFAULT 0, position_need_relay_score REAL DEFAULT 0, number_supply_key TEXT, number_supply_summary TEXT, number_supply_strength REAL DEFAULT 0, number_supply_risk REAL DEFAULT 0, need_supply_match_key TEXT, need_supply_match_score REAL DEFAULT 0, need_supply_third_quality REAL DEFAULT 0, need_supply_role_diversity REAL DEFAULT 0, need_supply_risk REAL DEFAULT 0, need_supply_summary TEXT, k3_role_main TEXT, k3_role_assist TEXT, k3_role_defense TEXT, k3_role_main_number INTEGER DEFAULT 0, k3_role_assist_number INTEGER DEFAULT 0, k3_role_defense_number INTEGER DEFAULT 0, k3_role_structure TEXT, counterfactual_reason_key TEXT, counterfactual_rank INTEGER DEFAULT 0, counterfactual_rank_score REAL DEFAULT 0, counterfactual_why_not_risk REAL DEFAULT 0, counterfactual_why_not TEXT, counterfactual_summary TEXT, cause_ablation_key TEXT, cause_ablation_score REAL DEFAULT 0, cause_ablation_false_cause_count INTEGER DEFAULT 0, cause_ablation_summary TEXT, miss_reason_type TEXT, miss_reason_matrix_key TEXT, miss_reason_detail TEXT, adaptive_judge_candidate_count INTEGER DEFAULT 0, adaptive_judge_status TEXT, adaptive_judge_action TEXT, adaptive_judge_domain TEXT, adaptive_cluster_hit_rate REAL DEFAULT 0, adaptive_cluster_recent_hit_rate REAL DEFAULT 0, adaptive_cluster_sample INTEGER DEFAULT 0, adaptive_cluster_confidence REAL DEFAULT 0, adaptive_cause_score REAL DEFAULT 0, adaptive_counter_score REAL DEFAULT 0, adaptive_promote_score REAL DEFAULT 0, adaptive_freeze_score REAL DEFAULT 0, adaptive_judge_reason TEXT, adaptive_judge_version TEXT, promotion_cluster_status TEXT, promotion_cluster_hit_rate REAL DEFAULT 0, promotion_cluster_recent_hit_rate REAL DEFAULT 0, promotion_cluster_sample INTEGER DEFAULT 0, promotion_cluster_reason TEXT, why_bet TEXT, why_no_bet TEXT, why_hit TEXT, why_miss TEXT, dynamic_causal_summary TEXT, hit INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, actual_rank INTEGER DEFAULT 0, learned INTEGER DEFAULT 0, snapshot_key TEXT, candidate_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- shadow_health_gate
CREATE TABLE shadow_health_gate (_id INTEGER PRIMARY KEY AUTOINCREMENT, shadow_key TEXT NOT NULL UNIQUE, shadow_key_raw TEXT, grade TEXT, state_key TEXT, position INTEGER DEFAULT 0, sample30 INTEGER DEFAULT 0, hit30 INTEGER DEFAULT 0, sample60 INTEGER DEFAULT 0, hit60 INTEGER DEFAULT 0, sample120 INTEGER DEFAULT 0, hit120 INTEGER DEFAULT 0, hit_rate30 REAL DEFAULT 0, hit_rate60 REAL DEFAULT 0, hit_rate120 REAL DEFAULT 0, max_shadow_lose INTEGER DEFAULT 0, current_shadow_lose INTEGER DEFAULT 0, decay_risk REAL DEFAULT 0, shadow_health_score REAL DEFAULT 0, promotion_pass INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0, summary_text TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, sample_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- shadow_promotion_cache
CREATE TABLE shadow_promotion_cache (_id INTEGER PRIMARY KEY AUTOINCREMENT,promotion_key TEXT UNIQUE,domain_key TEXT,domain_type TEXT,created_at INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,total_shadow INTEGER DEFAULT 0,would_hit INTEGER DEFAULT 0,would_miss INTEGER DEFAULT 0,would_hit_rate REAL DEFAULT 0,real_bet_rate REAL DEFAULT 0,promotion_status TEXT,promotion_score REAL DEFAULT 0,reliability REAL DEFAULT 0,watch_count INTEGER DEFAULT 0,blocked_count INTEGER DEFAULT 0,summary_text TEXT,detail_text TEXT,ext_json TEXT, record_key TEXT, source_type TEXT, base_issue TEXT, target_issue TEXT, position INTEGER DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, teacher_status TEXT, v20_status TEXT, window_state_key TEXT, module_name TEXT, status TEXT);

-- shape_memory
CREATE TABLE shape_memory (_id INTEGER PRIMARY KEY AUTOINCREMENT, shape_key TEXT NOT NULL, position INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, last240_sample INTEGER DEFAULT 0, last240_hit INTEGER DEFAULT 0, last240_hit_rate REAL DEFAULT 0, last1000_sample INTEGER DEFAULT 0, last1000_hit INTEGER DEFAULT 0, last1000_hit_rate REAL DEFAULT 0, fail_count INTEGER DEFAULT 0, reverse_count INTEGER DEFAULT 0, active_flag INTEGER DEFAULT 1, updated_at INTEGER DEFAULT 0, UNIQUE(shape_key, position));

-- shape_vector
CREATE TABLE shape_vector (_id INTEGER PRIMARY KEY AUTOINCREMENT, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, entropy16 REAL DEFAULT 0, entropy32 REAL DEFAULT 0, entropy64 REAL DEFAULT 0, entropy128 REAL DEFAULT 0, entropy240 REAL DEFAULT 0, entropy480 REAL DEFAULT 0, entropy960 REAL DEFAULT 0, top3_mass16 REAL DEFAULT 0, top3_mass32 REAL DEFAULT 0, top3_mass64 REAL DEFAULT 0, top3_mass128 REAL DEFAULT 0, top3_mass240 REAL DEFAULT 0, top4_mass128 REAL DEFAULT 0, top6_mass240 REAL DEFAULT 0, slope_short REAL DEFAULT 0, slope_mid REAL DEFAULT 0, slope_long REAL DEFAULT 0, drift_short REAL DEFAULT 0, drift_mid REAL DEFAULT 0, drift_long REAL DEFAULT 0, drift_risk REAL DEFAULT 0, markov1_support REAL DEFAULT 0, markov2_support REAL DEFAULT 0, trend_life REAL DEFAULT 0, break_depth REAL DEFAULT 0, recovery_score REAL DEFAULT 0, champion_gap REAL DEFAULT 0, combo_gap REAL DEFAULT 0, hot_reverse_risk REAL DEFAULT 0, false_strong_risk REAL DEFAULT 0, state_mode TEXT, shape_key TEXT, state_key TEXT, p01 REAL DEFAULT 0, p02 REAL DEFAULT 0, p03 REAL DEFAULT 0, p04 REAL DEFAULT 0, p05 REAL DEFAULT 0, p06 REAL DEFAULT 0, p07 REAL DEFAULT 0, p08 REAL DEFAULT 0, p09 REAL DEFAULT 0, p10 REAL DEFAULT 0, m01 REAL DEFAULT 0, m02 REAL DEFAULT 0, m03 REAL DEFAULT 0, m04 REAL DEFAULT 0, m05 REAL DEFAULT 0, m06 REAL DEFAULT 0, m07 REAL DEFAULT 0, m08 REAL DEFAULT 0, m09 REAL DEFAULT 0, m10 REAL DEFAULT 0, deep_score REAL DEFAULT 0, state_score REAL DEFAULT 0, cycle_score REAL DEFAULT 0, transition_score REAL DEFAULT 0, bias_score REAL DEFAULT 0, profile_score REAL DEFAULT 0, runtime_profile_score REAL DEFAULT 0, teacher_matrix_score REAL DEFAULT 0, reason_score REAL DEFAULT 0, reason_edge REAL DEFAULT 0, reason_fingerprint TEXT, deep_profile_key TEXT);

-- source_health_matrix
CREATE TABLE source_health_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, source_key TEXT NOT NULL UNIQUE, state_key TEXT, source_name TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, source_edge REAL DEFAULT 0, source_healthy INTEGER DEFAULT 0, is_frozen INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0, summary_text TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- sqlite_sequence
CREATE TABLE sqlite_sequence(name,seq);

-- state_health_matrix
CREATE TABLE state_health_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, state_health_key TEXT NOT NULL UNIQUE, state_key TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, state_edge REAL DEFAULT 0, state_healthy INTEGER DEFAULT 0, max_lose INTEGER DEFAULT 0, current_lose INTEGER DEFAULT 0, is_frozen INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0, summary_text TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- state_number_profile
CREATE TABLE state_number_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, trend_score REAL DEFAULT 0, shape_score REAL DEFAULT 0, risk_score REAL DEFAULT 0, number_bias REAL DEFAULT 0, confidence REAL DEFAULT 0, why_text TEXT, updated_at INTEGER DEFAULT 0);

-- su1_active_pin
CREATE INDEX su1_active_pin ON su1_retention_pin(object_kind,object_id,released_at);

-- su1_alpha_reservation
CREATE TABLE su1_alpha_reservation (
 reservation_id TEXT PRIMARY KEY NOT NULL,
 origin TEXT NOT NULL CHECK(origin IN ('LEGACY_CP3','LEGACY_R932','SU1')),
 amount REAL NOT NULL CHECK(amount>0 AND amount<=0.05),
 created_at INTEGER NOT NULL
);

-- su1_alpha_reservation_cap
CREATE TRIGGER su1_alpha_reservation_cap BEFORE INSERT ON su1_alpha_reservation
BEGIN
 SELECT CASE WHEN NEW.amount+(SELECT coalesce(sum(amount),0) FROM su1_alpha_reservation)>
 (SELECT global_alpha FROM su1_alpha_root WHERE singleton=1)+0.000000000001
 THEN RAISE(ABORT,'SU1_LIFETIME_GLOBAL_ALPHA_EXHAUSTED') END;
END;

-- su1_alpha_reservation_delete
CREATE TRIGGER su1_alpha_reservation_delete BEFORE DELETE ON su1_alpha_reservation
BEGIN SELECT RAISE(ABORT,'SU1_ALPHA_IRREVOCABLE'); END;

-- su1_alpha_reservation_update
CREATE TRIGGER su1_alpha_reservation_update BEFORE UPDATE ON su1_alpha_reservation
BEGIN SELECT RAISE(ABORT,'SU1_ALPHA_IRREVOCABLE'); END;

-- su1_alpha_root
CREATE TABLE su1_alpha_root (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 global_alpha REAL NOT NULL CHECK(global_alpha>0 AND global_alpha<=0.05),
 registration_hash TEXT NOT NULL CHECK(length(registration_hash)=64),
 created_at INTEGER NOT NULL
);

-- su1_alpha_root_immutable
CREATE TRIGGER su1_alpha_root_immutable BEFORE UPDATE ON su1_alpha_root
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_alpha_root_no_delete
CREATE TRIGGER su1_alpha_root_no_delete BEFORE DELETE ON su1_alpha_root
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_archive_receipt
CREATE TABLE su1_archive_receipt (
 receipt_id TEXT NOT NULL PRIMARY KEY CHECK(length(receipt_id)=64),
 archive_locator TEXT NOT NULL,
 manifest_hash TEXT NOT NULL CHECK(length(manifest_hash)=64),
 dependency_graph_hash TEXT NOT NULL CHECK(length(dependency_graph_hash)=64),
 verified_at INTEGER NOT NULL CHECK(verified_at>0),
 replay_verified INTEGER NOT NULL CHECK(replay_verified=1),
 manifest_json TEXT NOT NULL CHECK(length(CAST(manifest_json AS BLOB))<=65536)
);

-- su1_archive_receipt_immutable
CREATE TRIGGER su1_archive_receipt_immutable BEFORE UPDATE ON su1_archive_receipt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_archive_receipt_no_delete
CREATE TRIGGER su1_archive_receipt_no_delete BEFORE DELETE ON su1_archive_receipt
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_archive_receipt_no_replace
CREATE TRIGGER su1_archive_receipt_no_replace BEFORE INSERT ON su1_archive_receipt
WHEN EXISTS(SELECT 1 FROM su1_archive_receipt WHERE receipt_id=NEW.receipt_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_audit_checkpoint_cas
CREATE TRIGGER su1_audit_checkpoint_cas BEFORE UPDATE ON su1_campaign_audit
BEGIN
 SELECT CASE WHEN OLD.phase IN ('DONE','WAIT_HISTORY') OR NEW.generation<>OLD.generation+1 OR NEW.source_campaign<>OLD.source_campaign OR NEW.variant<>OLD.variant OR NEW.audit_campaign<>OLD.audit_campaign OR NEW.dataset_blob<>OLD.dataset_blob OR NEW.updated_at<OLD.updated_at
 OR OLD.phase='SEALED' AND (NEW.selected_hash IS NOT OLD.selected_hash OR NEW.selected_program IS NOT OLD.selected_program OR NEW.phase NOT IN ('SEALED','DONE'))
 OR NEW.phase='DONE' AND NOT EXISTS(SELECT 1 FROM su1_campaign_audit_open o WHERE o.source_campaign=OLD.source_campaign AND o.variant=OLD.variant)
 THEN RAISE(ABORT,'SU1_AUDIT_CHECKPOINT_CAS') END;
END;

-- su1_audit_no_delete
CREATE TRIGGER su1_audit_no_delete BEFORE DELETE ON su1_campaign_audit
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_audit_open_no_delete
CREATE TRIGGER su1_audit_open_no_delete BEFORE DELETE ON su1_campaign_audit_open
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_audit_open_no_update
CREATE TRIGGER su1_audit_open_no_update BEFORE UPDATE ON su1_campaign_audit_open
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_audit_open_once
CREATE TRIGGER su1_audit_open_once BEFORE INSERT ON su1_campaign_audit_open
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_campaign_audit a WHERE a.source_campaign=NEW.source_campaign AND a.variant=NEW.variant AND (a.phase='SEALED' AND a.selected_hash=NEW.selected_hash OR a.phase='SEARCH' AND a.selected_hash IS NULL AND NEW.selected_hash IS NULL))
 THEN RAISE(ABORT,'SU1_AUDIT_CHAMPION_NOT_FROZEN') END;
END;

-- su1_audit_work
CREATE INDEX su1_audit_work ON su1_campaign_audit(phase,updated_at);

-- su1_blob
CREATE TABLE su1_blob (
 blob_hash TEXT NOT NULL PRIMARY KEY CHECK(length(blob_hash)=64),
 byte_count INTEGER NOT NULL CHECK(byte_count>=0),
 codec TEXT NOT NULL,
 storage_state TEXT NOT NULL CHECK(storage_state IN ('LOCAL','ARCHIVED')),
 local_locator TEXT,
 archive_receipt_id TEXT REFERENCES su1_archive_receipt(receipt_id),
 created_at INTEGER NOT NULL,
 CHECK((storage_state='LOCAL' AND local_locator IS NOT NULL) OR
       (storage_state='ARCHIVED' AND local_locator IS NULL AND archive_receipt_id IS NOT NULL))
);

-- su1_blob_archive_guard
CREATE TRIGGER su1_blob_archive_guard BEFORE UPDATE ON su1_blob
BEGIN
 SELECT CASE WHEN NOT (NEW.blob_hash=OLD.blob_hash AND NEW.byte_count=OLD.byte_count AND NEW.codec=OLD.codec
  AND NEW.created_at=OLD.created_at AND OLD.storage_state='LOCAL' AND NEW.storage_state='ARCHIVED'
  AND NEW.archive_receipt_id IS NOT NULL AND NEW.local_locator IS NULL)
 THEN RAISE(ABORT,'SU1_BLOB_ARCHIVE_ONLY') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_retention_pin WHERE object_kind='BLOB'
  AND object_id=OLD.blob_hash AND released_at IS NULL)
 THEN RAISE(ABORT,'SU1_BLOB_PINNED') END;
END;

-- su1_blob_no_delete
CREATE TRIGGER su1_blob_no_delete BEFORE DELETE ON su1_blob
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_blob_no_replace
CREATE TRIGGER su1_blob_no_replace BEFORE INSERT ON su1_blob
WHEN EXISTS(SELECT 1 FROM su1_blob WHERE blob_hash=NEW.blob_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_burst_checkpoint
CREATE TABLE su1_burst_checkpoint (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 generation INTEGER NOT NULL CHECK(generation>=0),
 dataset_hash TEXT NOT NULL CHECK(length(dataset_hash)=64),
 cursor_json TEXT NOT NULL CHECK(length(CAST(cursor_json AS BLOB))<=65536),
 state_hash TEXT NOT NULL CHECK(length(state_hash)=64),
 updated_at INTEGER NOT NULL
);

-- su1_calibration_attempt
CREATE TABLE su1_calibration_attempt (
 source_strategy TEXT PRIMARY KEY NOT NULL REFERENCES su1_strategy_version(strategy_id),
 result_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 calibrated_strategy TEXT UNIQUE REFERENCES su1_strategy_version(strategy_id),
 first_sequence INTEGER NOT NULL,
 last_sequence INTEGER NOT NULL,
 recorded_at INTEGER NOT NULL,
 CHECK(first_sequence<last_sequence)
);

-- su1_calibration_attempt_immutable
CREATE TRIGGER su1_calibration_attempt_immutable BEFORE UPDATE ON su1_calibration_attempt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_campaign
CREATE TABLE su1_campaign (
 campaign_id TEXT NOT NULL PRIMARY KEY CHECK(length(campaign_id)=64),
 kind TEXT NOT NULL CHECK(kind IN ('HISTORY_REPLAY','LIVE_REFRESH')),
 data_through_sequence INTEGER NOT NULL CHECK(data_through_sequence>0),
 config_hash TEXT NOT NULL CHECK(length(config_hash)=64),
 config_json TEXT NOT NULL CHECK(length(CAST(config_json AS BLOB))<=65536),
 started_at INTEGER NOT NULL,
 finished_at INTEGER,
 status TEXT NOT NULL CHECK(status IN ('RUNNING','DONE','FAILED')),
 programs_evaluated INTEGER NOT NULL DEFAULT 0 CHECK(programs_evaluated>=0),
 CHECK((status='RUNNING' AND finished_at IS NULL) OR (status<>'RUNNING' AND finished_at>=started_at))
);

-- su1_campaign_audit
CREATE TABLE su1_campaign_audit (
 source_campaign TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 variant TEXT NOT NULL CHECK(variant IN ('REAL','TIME_SHUFFLED','PLANTED_SIGNAL','NO_DIRECTOR','NO_MEMORY')),
 audit_campaign TEXT NOT NULL UNIQUE REFERENCES su1_campaign(campaign_id),
 dataset_blob TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 generation INTEGER NOT NULL CHECK(generation>=0),
 checkpoint_hash TEXT REFERENCES su1_blob(blob_hash),
 phase TEXT NOT NULL CHECK(phase IN ('SEARCH','SEALED','DONE','WAIT_HISTORY')),
 selected_hash TEXT CHECK(selected_hash IS NULL OR length(selected_hash)=64),
 selected_program TEXT,
 updated_at INTEGER NOT NULL,
 PRIMARY KEY(source_campaign,variant),
 CHECK(phase<>'SEALED' OR (selected_hash IS NOT NULL AND selected_program IS NOT NULL))
);

-- su1_campaign_audit_open
CREATE TABLE su1_campaign_audit_open (
 source_campaign TEXT NOT NULL,
 variant TEXT NOT NULL,
 selected_hash TEXT,
 report_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 opened_at INTEGER NOT NULL,
 PRIMARY KEY(source_campaign,variant),
 FOREIGN KEY(source_campaign,variant) REFERENCES su1_campaign_audit(source_campaign,variant)
);

-- su1_campaign_curve
CREATE TABLE su1_campaign_curve (
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 generation INTEGER NOT NULL,
 dev_fitness REAL NOT NULL,
 champion_val_fitness REAL NOT NULL,
 champion_family TEXT NOT NULL,
 recorded_at INTEGER NOT NULL,
 PRIMARY KEY(campaign_id,generation)
);

-- su1_campaign_transition
CREATE TRIGGER su1_campaign_transition BEFORE UPDATE ON su1_campaign
BEGIN
 SELECT CASE WHEN NOT (OLD.status='RUNNING' AND NEW.status IN ('DONE','FAILED') AND NEW.campaign_id=OLD.campaign_id
   AND NEW.kind=OLD.kind AND NEW.data_through_sequence=OLD.data_through_sequence AND NEW.config_hash=OLD.config_hash
   AND NEW.started_at=OLD.started_at AND NEW.programs_evaluated>=OLD.programs_evaluated)
 THEN RAISE(ABORT,'SU1_CAMPAIGN_TRANSITION') END;
END;

-- su1_certificate
CREATE TABLE su1_certificate (
 certificate_id TEXT NOT NULL PRIMARY KEY CHECK(length(certificate_id)=64),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 evidence_hash TEXT NOT NULL UNIQUE REFERENCES su1_evidence_look(evidence_hash),
 
 
 baseline_evidence_hash TEXT NOT NULL REFERENCES su1_evidence_look(evidence_hash),
 policy_hash TEXT NOT NULL CHECK(length(policy_hash)=64),
 source_chain_hash TEXT NOT NULL CHECK(length(source_chain_hash)=64),
 issued_at INTEGER NOT NULL,
 
 expires_at INTEGER NOT NULL,
 CHECK(expires_at>issued_at)
);

-- su1_certificate_binding
CREATE TRIGGER su1_certificate_binding BEFORE INSERT ON su1_certificate
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_evidence_look e JOIN su1_eval_cohort c ON c.cohort_id=e.cohort_id
  JOIN su1_strategy_version v ON v.strategy_id=c.strategy_id
  WHERE e.evidence_hash=NEW.evidence_hash AND c.strategy_id=NEW.strategy_id
  AND v.strategy_role='CANDIDATE' AND c.claim_kind IN ('PARENT_CHILD','VS_NULL_BASELINE')
  AND c.evidence_role='CONFIRM' AND e.result_state='PASS' AND e.lower_bound>0
  AND e.source_chain_hash=NEW.source_chain_hash AND NEW.issued_at>=e.recorded_at)
 THEN RAISE(ABORT,'SU1_CERTIFICATE_EVIDENCE') END;
 
 
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_evidence_look b JOIN su1_eval_cohort bc ON bc.cohort_id=b.cohort_id
  JOIN su1_evidence_look e ON e.evidence_hash=NEW.evidence_hash
  JOIN su1_eval_cohort ec ON ec.cohort_id=e.cohort_id
  WHERE b.evidence_hash=NEW.baseline_evidence_hash AND bc.strategy_id=NEW.strategy_id
  AND bc.claim_kind='VS_NULL_BASELINE' AND bc.evidence_role='CONFIRM'
  AND b.result_state='PASS' AND b.lower_bound>0 AND NEW.issued_at>=b.recorded_at
  AND bc.family_root_hash=ec.family_root_hash)
 THEN RAISE(ABORT,'SU1_CERTIFICATE_NULL_BASELINE_REQUIRED') END;
 
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_lineage l JOIN su1_strategy_version pv ON pv.strategy_id=l.parent_strategy_id
   WHERE l.child_strategy_id=NEW.strategy_id AND pv.strategy_role='CANDIDATE')
  AND NOT EXISTS(SELECT 1 FROM su1_evidence_look e JOIN su1_eval_cohort c ON c.cohort_id=e.cohort_id
   JOIN su1_lineage l ON l.child_strategy_id=c.strategy_id AND l.parent_strategy_id=c.parent_strategy_id
   WHERE e.evidence_hash=NEW.evidence_hash AND c.claim_kind='PARENT_CHILD')
 THEN RAISE(ABORT,'SU1_CERTIFICATE_PARENT_CLAIM_REQUIRED') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_evidence_look x JOIN su1_eval_cohort xc ON xc.cohort_id=x.cohort_id
  JOIN su1_evidence_look e ON e.evidence_hash=NEW.evidence_hash
  JOIN su1_eval_cohort ec ON ec.cohort_id=e.cohort_id
  WHERE xc.claim_kind='NULL_CONTROL' AND x.result_state='PASS' AND xc.family_root_hash=ec.family_root_hash)
 THEN RAISE(ABORT,'SU1_NULL_CONTROL_ALARM') END;
END;

-- su1_certificate_immutable
CREATE TRIGGER su1_certificate_immutable BEFORE UPDATE ON su1_certificate
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_certificate_no_delete
CREATE TRIGGER su1_certificate_no_delete BEFORE DELETE ON su1_certificate
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_certificate_no_replace
CREATE TRIGGER su1_certificate_no_replace BEFORE INSERT ON su1_certificate
WHEN EXISTS(SELECT 1 FROM su1_certificate WHERE certificate_id=NEW.certificate_id) OR EXISTS(SELECT 1 FROM su1_certificate WHERE evidence_hash=NEW.evidence_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_certificate_revocation
CREATE TABLE su1_certificate_revocation (
 certificate_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_certificate(certificate_id),
 reason_code TEXT NOT NULL,
 evidence_hash TEXT NOT NULL CHECK(length(evidence_hash)=64),
 revoked_at INTEGER NOT NULL
);

-- su1_certificate_revocation_immutable
CREATE TRIGGER su1_certificate_revocation_immutable BEFORE UPDATE ON su1_certificate_revocation
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_certificate_revocation_no_delete
CREATE TRIGGER su1_certificate_revocation_no_delete BEFORE DELETE ON su1_certificate_revocation
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_certificate_revocation_no_replace
CREATE TRIGGER su1_certificate_revocation_no_replace BEFORE INSERT ON su1_certificate_revocation
WHEN EXISTS(SELECT 1 FROM su1_certificate_revocation WHERE certificate_id=NEW.certificate_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_certificate_runtime
CREATE TABLE su1_certificate_runtime (
 certificate_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_certificate(certificate_id),
 kernel_hash TEXT NOT NULL CHECK(length(kernel_hash)=64),
 quality_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 family_root_hash TEXT NOT NULL REFERENCES su1_family_budget(family_root_hash),
 descriptor_hash TEXT NOT NULL CHECK(length(descriptor_hash)=64)
);

-- su1_certificate_runtime_immutable
CREATE TRIGGER su1_certificate_runtime_immutable BEFORE UPDATE ON su1_certificate_runtime
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_cohort_binding
CREATE TRIGGER su1_cohort_binding BEFORE INSERT ON su1_eval_cohort
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id
  AND created_sequence<=NEW.registered_sequence)
 OR (NEW.parent_strategy_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM su1_strategy_version
  WHERE strategy_id=NEW.parent_strategy_id AND created_sequence<=NEW.registered_sequence))
 THEN RAISE(ABORT,'SU1_COHORT_STRATEGY_NOT_FROZEN') END;
 
 
 SELECT CASE WHEN NEW.evidence_role='CONFIRM' AND NEW.global_claim_ordinal<>(SELECT coalesce(max(global_claim_ordinal),0)+1
  FROM su1_eval_cohort WHERE family_root_hash=NEW.family_root_hash AND evidence_role='CONFIRM')
 THEN RAISE(ABORT,'SU1_GLOBAL_CLAIM_ORDINAL') END;
 SELECT CASE WHEN NEW.evidence_role<>'CONFIRM' AND NEW.global_claim_ordinal<>(SELECT coalesce(max(global_claim_ordinal),999999)+1
  FROM su1_eval_cohort WHERE family_root_hash=NEW.family_root_hash AND evidence_role<>'CONFIRM')
 THEN RAISE(ABORT,'SU1_RESEARCH_CLAIM_ORDINAL') END;
 
 
 SELECT CASE WHEN NEW.parent_strategy_id IS NOT NULL AND NOT (
   (NEW.claim_kind='PARENT_CHILD'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id)='CANDIDATE'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.parent_strategy_id)='CANDIDATE')
  OR (NEW.claim_kind='VS_NULL_BASELINE'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id)='CANDIDATE'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.parent_strategy_id)='NULL_BASELINE')
  OR (NEW.claim_kind='NULL_CONTROL'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id) IN ('NULL_BASELINE','NULL_CONTROL')))
 THEN RAISE(ABORT,'SU1_CLAIM_KIND_ROLE_MISMATCH') END;
 SELECT CASE WHEN NEW.parent_strategy_id IS NULL AND NEW.claim_kind<>'PARENT_CHILD'
 THEN RAISE(ABORT,'SU1_CLAIM_KIND_REQUIRES_COMPARATOR') END;
END;

-- su1_cohort_runtime
CREATE TABLE su1_cohort_runtime (
 cohort_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_eval_cohort(cohort_id),
 last_issue TEXT NOT NULL,
 policy_revision TEXT NOT NULL CHECK(policy_revision='SU1_ALL_CALENDAR_K3_CONFIRM_V1'),
 state TEXT NOT NULL DEFAULT 'OPEN' CHECK(state IN ('OPEN','STOPPED','CLOSED')),
 stop_reason TEXT NOT NULL DEFAULT ''
);

-- su1_cohort_runtime_immutable
CREATE TRIGGER su1_cohort_runtime_immutable BEFORE UPDATE ON su1_cohort_runtime
BEGIN
 SELECT CASE WHEN NEW.cohort_id<>OLD.cohort_id OR NEW.last_issue<>OLD.last_issue OR NEW.policy_revision<>OLD.policy_revision
 OR OLD.state<>'OPEN' OR NEW.state NOT IN ('STOPPED','CLOSED')
 THEN RAISE(ABORT,'SU1_COHORT_RUNTIME_TRANSITION') END;
END;

-- su1_curve_immutable
CREATE TRIGGER su1_curve_immutable BEFORE UPDATE ON su1_campaign_curve
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_curve_monotone
CREATE TRIGGER su1_curve_monotone BEFORE INSERT ON su1_campaign_curve
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_campaign_curve c WHERE c.campaign_id=NEW.campaign_id AND (c.generation>=NEW.generation OR c.champion_val_fitness>NEW.champion_val_fitness))
 THEN RAISE(ABORT,'SU1_CHAMPION_CURVE_ORDER') END;
END;

-- su1_dependency
CREATE TABLE su1_dependency (
 source_kind TEXT NOT NULL,
 source_id TEXT NOT NULL,
 target_kind TEXT NOT NULL,
 target_id TEXT NOT NULL,
 reason TEXT NOT NULL,
 created_at INTEGER NOT NULL,
 PRIMARY KEY(source_kind,source_id,target_kind,target_id)
);

-- su1_dependency_immutable
CREATE TRIGGER su1_dependency_immutable BEFORE UPDATE ON su1_dependency
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_dependency_no_delete
CREATE TRIGGER su1_dependency_no_delete BEFORE DELETE ON su1_dependency
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_dependency_no_replace
CREATE TRIGGER su1_dependency_no_replace BEFORE INSERT ON su1_dependency
WHEN EXISTS(SELECT 1 FROM su1_dependency WHERE source_kind=NEW.source_kind AND source_id=NEW.source_id AND target_kind=NEW.target_kind AND target_id=NEW.target_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_dependency_target
CREATE INDEX su1_dependency_target ON su1_dependency(target_kind,target_id);

-- su1_director_state
CREATE TABLE su1_director_state (
 campaign_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_campaign(campaign_id),
 state_json TEXT NOT NULL CHECK(length(CAST(state_json AS BLOB))<=65536),
 state_hash TEXT NOT NULL CHECK(length(state_hash)=64)
);

-- su1_director_state_immutable
CREATE TRIGGER su1_director_state_immutable BEFORE UPDATE ON su1_director_state
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_draw_delete_clock
CREATE TRIGGER su1_draw_delete_clock AFTER DELETE ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_draw_insert_clock
CREATE TRIGGER su1_draw_insert_clock AFTER INSERT ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_draw_update_clock
CREATE TRIGGER su1_draw_update_clock AFTER UPDATE ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_emergent_family
CREATE TABLE su1_emergent_family (
 family_id TEXT NOT NULL PRIMARY KEY CHECK(family_id='EMERGENT:'||macro_id),
 macro_id TEXT NOT NULL UNIQUE REFERENCES su1_macro(macro_id),
 created_campaign TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 created_at INTEGER NOT NULL
);

-- su1_emergent_family_immutable
CREATE TRIGGER su1_emergent_family_immutable BEFORE UPDATE ON su1_emergent_family
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_era
CREATE TABLE su1_era (
 era_id TEXT NOT NULL PRIMARY KEY CHECK(length(era_id)=64),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 kind TEXT NOT NULL CHECK(kind IN ('DEV','VAL','SEALED')),
 first_sequence INTEGER NOT NULL,
 last_sequence INTEGER NOT NULL,
 UNIQUE(campaign_id,kind),
 CHECK(first_sequence<=last_sequence)
);

-- su1_era_immutable
CREATE TRIGGER su1_era_immutable BEFORE UPDATE ON su1_era
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_era_no_delete
CREATE TRIGGER su1_era_no_delete BEFORE DELETE ON su1_era
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_era_order
CREATE TRIGGER su1_era_order BEFORE INSERT ON su1_era
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_era e WHERE e.campaign_id=NEW.campaign_id AND (
   (NEW.kind='VAL' AND e.kind='DEV' AND e.last_sequence>=NEW.first_sequence) OR
   (NEW.kind='SEALED' AND e.kind IN ('DEV','VAL') AND e.last_sequence>=NEW.first_sequence) OR
   (NEW.kind='DEV' AND e.kind IN ('VAL','SEALED') AND NEW.last_sequence>=e.first_sequence) OR
   (NEW.kind='VAL' AND e.kind='SEALED' AND NEW.last_sequence>=e.first_sequence)))
 THEN RAISE(ABORT,'SU1_ERA_ORDER') END;
 SELECT CASE WHEN NEW.last_sequence>(SELECT data_through_sequence FROM su1_campaign WHERE campaign_id=NEW.campaign_id)
 THEN RAISE(ABORT,'SU1_ERA_BEYOND_DATA') END;
END;

-- su1_eval_cohort
CREATE TABLE su1_eval_cohort (
 cohort_id TEXT NOT NULL PRIMARY KEY CHECK(length(cohort_id)=64),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 parent_strategy_id TEXT REFERENCES su1_strategy_version(strategy_id),
 claim_id TEXT NOT NULL CHECK(length(claim_id)=64),
 evidence_role TEXT NOT NULL CHECK(evidence_role IN ('RESEARCH_FUTURE','CONFIRM','CALIBRATION_FIT','CALIBRATION_VALIDATION')),
 claim_kind TEXT NOT NULL CHECK(claim_kind IN ('PARENT_CHILD','VS_NULL_BASELINE','NULL_CONTROL')),
 registered_sequence INTEGER NOT NULL,
 first_issue INTEGER NOT NULL,
 planned_opportunities INTEGER NOT NULL CHECK(planned_opportunities BETWEEN 1 AND 1000000),
 protocol_hash TEXT NOT NULL CHECK(length(protocol_hash)=64),
 protocol_json TEXT NOT NULL CHECK(length(CAST(protocol_json AS BLOB))<=65536),
 family_root_hash TEXT NOT NULL CHECK(length(family_root_hash)=64),
 family_alpha REAL NOT NULL CHECK(family_alpha>0 AND family_alpha<1),
 global_claim_ordinal INTEGER NOT NULL CHECK(global_claim_ordinal>0),
 look_schedule_json TEXT NOT NULL CHECK(length(CAST(look_schedule_json AS BLOB))<=16384),
 baseline_hash TEXT NOT NULL CHECK(length(baseline_hash)=64),
 created_at INTEGER NOT NULL,
 FOREIGN KEY(family_root_hash,family_alpha) REFERENCES su1_family_budget(family_root_hash,family_alpha),
 UNIQUE(family_root_hash,global_claim_ordinal),
 UNIQUE(claim_id),
 CHECK(parent_strategy_id IS NULL OR parent_strategy_id<>strategy_id),
 CHECK(evidence_role<>'CONFIRM' OR parent_strategy_id IS NOT NULL)
);

-- su1_eval_cohort_immutable
CREATE TRIGGER su1_eval_cohort_immutable BEFORE UPDATE ON su1_eval_cohort
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_eval_cohort_no_delete
CREATE TRIGGER su1_eval_cohort_no_delete BEFORE DELETE ON su1_eval_cohort
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_eval_cohort_no_replace
CREATE TRIGGER su1_eval_cohort_no_replace BEFORE INSERT ON su1_eval_cohort
WHEN EXISTS(SELECT 1 FROM su1_eval_cohort WHERE cohort_id=NEW.cohort_id) OR EXISTS(SELECT 1 FROM su1_eval_cohort WHERE claim_id=NEW.claim_id) OR EXISTS(SELECT 1 FROM su1_eval_cohort WHERE family_root_hash=NEW.family_root_hash AND global_claim_ordinal=NEW.global_claim_ordinal)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_evidence_look
CREATE TABLE su1_evidence_look (
 evidence_hash TEXT NOT NULL PRIMARY KEY CHECK(length(evidence_hash)=64),
 cohort_id TEXT NOT NULL REFERENCES su1_eval_cohort(cohort_id),
 claim_id TEXT NOT NULL,
 global_claim_ordinal INTEGER NOT NULL CHECK(global_claim_ordinal>0),
 look_ordinal INTEGER NOT NULL CHECK(look_ordinal>0),
 protocol_hash TEXT NOT NULL CHECK(length(protocol_hash)=64),
 planned_opportunities INTEGER NOT NULL CHECK(planned_opportunities>0),
 paired_n INTEGER NOT NULL CHECK(paired_n>=0),
 parent_missing_n INTEGER NOT NULL CHECK(parent_missing_n>=0),
 child_missing_n INTEGER NOT NULL CHECK(child_missing_n>=0),
 unpaired_n INTEGER NOT NULL CHECK(unpaired_n>=0),
 parent_hits INTEGER NOT NULL CHECK(parent_hits>=0),
 child_hits INTEGER NOT NULL CHECK(child_hits>=0),
 child_only_wins INTEGER NOT NULL CHECK(child_only_wins>=0),
 parent_only_wins INTEGER NOT NULL CHECK(parent_only_wins>=0),
 paired_delta REAL,
 conservative_mean REAL NOT NULL CHECK(conservative_mean BETWEEN -1 AND 1),
 
 
 
 bound_radius REAL NOT NULL CHECK(bound_radius>0),
 lower_bound REAL NOT NULL CHECK(lower_bound BETWEEN -1 AND 1),
 allocated_alpha REAL NOT NULL CHECK(allocated_alpha>0 AND allocated_alpha<1),
 cumulative_alpha_spent REAL NOT NULL CHECK(cumulative_alpha_spent>0 AND cumulative_alpha_spent<1),
 coverage REAL NOT NULL CHECK(coverage>=0 AND coverage<=1),
 result_state TEXT NOT NULL CHECK(result_state IN ('INSUFFICIENT','FAIL','PASS')),
 source_chain_hash TEXT NOT NULL CHECK(length(source_chain_hash)=64),
 metrics_json TEXT NOT NULL CHECK(length(CAST(metrics_json AS BLOB))<=65536),
 recorded_at INTEGER NOT NULL,
 FOREIGN KEY(cohort_id,look_ordinal,planned_opportunities)
  REFERENCES su1_look_plan(cohort_id,look_ordinal,planned_opportunities),
 UNIQUE(cohort_id,claim_id,look_ordinal),
 CHECK(paired_n+unpaired_n=planned_opportunities),
 CHECK(parent_missing_n<=unpaired_n AND child_missing_n<=unpaired_n
       AND parent_missing_n+child_missing_n>=unpaired_n),
 CHECK(parent_hits<=paired_n AND child_hits<=paired_n),
 CHECK(child_only_wins+parent_only_wins<=paired_n),
 CHECK(child_hits-parent_hits=child_only_wins-parent_only_wins),
 CHECK(result_state<>'PASS' OR lower_bound>0)
);

-- su1_evidence_look_immutable
CREATE TRIGGER su1_evidence_look_immutable BEFORE UPDATE ON su1_evidence_look
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_evidence_look_no_delete
CREATE TRIGGER su1_evidence_look_no_delete BEFORE DELETE ON su1_evidence_look
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_evidence_look_no_replace
CREATE TRIGGER su1_evidence_look_no_replace BEFORE INSERT ON su1_evidence_look
WHEN EXISTS(SELECT 1 FROM su1_evidence_look WHERE evidence_hash=NEW.evidence_hash) OR EXISTS(SELECT 1 FROM su1_evidence_look WHERE cohort_id=NEW.cohort_id AND claim_id=NEW.claim_id AND look_ordinal=NEW.look_ordinal)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_family_budget
CREATE TABLE su1_family_budget (
 family_root_hash TEXT NOT NULL PRIMARY KEY CHECK(length(family_root_hash)=64),
 family_alpha REAL NOT NULL CHECK(family_alpha>0 AND family_alpha<1),
 family_ordinal INTEGER NOT NULL UNIQUE CHECK(family_ordinal>0),
 registration_hash TEXT NOT NULL CHECK(length(registration_hash)=64),
 created_at INTEGER NOT NULL,
 UNIQUE(family_root_hash,family_alpha)
);

-- su1_family_budget_allocation
CREATE TRIGGER su1_family_budget_allocation BEFORE INSERT ON su1_family_budget
BEGIN
 SELECT CASE WHEN NEW.family_ordinal<>(SELECT coalesce(max(family_ordinal),0)+1 FROM su1_family_budget)
 THEN RAISE(ABORT,'SU1_FAMILY_ORDINAL') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_alpha_root WHERE singleton=1
  AND NEW.family_alpha<=global_alpha/(1.0*NEW.family_ordinal*(NEW.family_ordinal+1))+0.000000000001)
 THEN RAISE(ABORT,'SU1_FAMILY_ALPHA_EXCEEDS_GLOBAL_ROOT') END;
END;

-- su1_family_budget_immutable
CREATE TRIGGER su1_family_budget_immutable BEFORE UPDATE ON su1_family_budget
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_family_budget_no_delete
CREATE TRIGGER su1_family_budget_no_delete BEFORE DELETE ON su1_family_budget
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_family_budget_no_replace
CREATE TRIGGER su1_family_budget_no_replace BEFORE INSERT ON su1_family_budget
WHEN EXISTS(SELECT 1 FROM su1_family_budget WHERE family_root_hash=NEW.family_root_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_feedback
CREATE TABLE su1_feedback (
 feedback_hash TEXT NOT NULL PRIMARY KEY CHECK(length(feedback_hash)=64),
 settlement_id TEXT NOT NULL REFERENCES su1_settlement(settlement_id),
 kind TEXT NOT NULL CHECK(kind IN ('RESIDUAL','COVERAGE','DRIFT','CALIBRATION','SYNERGY','DATA_QUALITY')),
 diagnostic_contract_hash TEXT NOT NULL CHECK(length(diagnostic_contract_hash)=64),
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 created_at INTEGER NOT NULL,
 UNIQUE(settlement_id,kind,diagnostic_contract_hash)
);

-- su1_feedback_immutable
CREATE TRIGGER su1_feedback_immutable BEFORE UPDATE ON su1_feedback
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_feedback_no_delete
CREATE TRIGGER su1_feedback_no_delete BEFORE DELETE ON su1_feedback
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_feedback_no_replace
CREATE TRIGGER su1_feedback_no_replace BEFORE INSERT ON su1_feedback
WHEN EXISTS(SELECT 1 FROM su1_feedback WHERE feedback_hash=NEW.feedback_hash) OR EXISTS(SELECT 1 FROM su1_feedback WHERE settlement_id=NEW.settlement_id AND kind=NEW.kind AND diagnostic_contract_hash=NEW.diagnostic_contract_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_freeze_event
CREATE TRIGGER su1_freeze_event AFTER INSERT ON su1_prediction
BEGIN
 INSERT INTO su1_pending_settlement(prediction_id,target_issue,enqueued_at)
 VALUES(NEW.prediction_id,NEW.target_issue,NEW.captured_at);
 INSERT INTO su1_outbox(event_id,event_type,prediction_id,source_record_hash,created_at,updated_at)
 VALUES('FROZEN:'||NEW.prediction_id,'FROZEN',NEW.prediction_id,NEW.record_hash,NEW.captured_at,NEW.captured_at);
END;

-- su1_health_event
CREATE TABLE su1_health_event (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 phase TEXT NOT NULL,
 detail TEXT NOT NULL,
 elapsed_micros INTEGER NOT NULL CHECK(elapsed_micros>=0),
 created_at INTEGER NOT NULL
);

-- su1_kernel_state
CREATE TABLE su1_kernel_state (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 kernel_hash TEXT NOT NULL CHECK(length(kernel_hash)=64),
 updated_at INTEGER NOT NULL
);

-- su1_knowledge_lookup
CREATE INDEX su1_knowledge_lookup ON su1_research_knowledge(context_key,action,result,recorded_at);

-- su1_lineage
CREATE TABLE su1_lineage (
 parent_strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 child_strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 task_id TEXT NOT NULL REFERENCES su1_research_task(task_id),
 operation_hash TEXT NOT NULL CHECK(length(operation_hash)=64),
 created_at INTEGER NOT NULL,
 PRIMARY KEY(parent_strategy_id,child_strategy_id,task_id),
 CHECK(parent_strategy_id<>child_strategy_id)
);

-- su1_lineage_immutable
CREATE TRIGGER su1_lineage_immutable BEFORE UPDATE ON su1_lineage
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_lineage_no_delete
CREATE TRIGGER su1_lineage_no_delete BEFORE DELETE ON su1_lineage
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_lineage_no_replace
CREATE TRIGGER su1_lineage_no_replace BEFORE INSERT ON su1_lineage
WHEN EXISTS(SELECT 1 FROM su1_lineage WHERE parent_strategy_id=NEW.parent_strategy_id AND child_strategy_id=NEW.child_strategy_id AND task_id=NEW.task_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_live_arm_stats
CREATE TABLE su1_live_arm_stats (
 arm TEXT PRIMARY KEY NOT NULL,
 actions INTEGER NOT NULL CHECK(actions>=0),
 hits INTEGER NOT NULL CHECK(hits>=0 AND hits<=actions),
 through_sequence INTEGER NOT NULL
);

-- su1_live_attempt
CREATE TABLE su1_live_attempt (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 sequence_ordinal INTEGER NOT NULL REFERENCES su1_live_target(sequence_ordinal),
 outcome TEXT NOT NULL CHECK(outcome IN ('FROZEN','GATE_SKIP','BUDGET_SKIP','COMPUTE_FAILED')),
 detail TEXT NOT NULL,
 created_at INTEGER NOT NULL,
 PRIMARY KEY(strategy_id,sequence_ordinal)
) WITHOUT ROWID;

-- su1_live_attempt_immutable
CREATE TRIGGER su1_live_attempt_immutable BEFORE UPDATE ON su1_live_attempt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_live_daily
CREATE TABLE su1_live_daily (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 day_key INTEGER NOT NULL,
 actions INTEGER NOT NULL CHECK(actions>=0),
 hits INTEGER NOT NULL CHECK(hits>=0 AND hits<=actions),
 periods_observed INTEGER NOT NULL CHECK(periods_observed>=actions),
 closed_at INTEGER NOT NULL,
 PRIMARY KEY(strategy_id,day_key)
) WITHOUT ROWID;

-- su1_live_daily_close
CREATE TRIGGER su1_live_daily_close BEFORE INSERT ON su1_live_daily
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_live_shadow WHERE strategy_id=NEW.strategy_id AND day_key=NEW.day_key AND hit IS NULL)
 THEN RAISE(ABORT,'SU1_LIVE_DAY_HAS_UNSETTLED') END;
 SELECT CASE WHEN NEW.actions<>(SELECT count(*) FROM su1_live_shadow WHERE strategy_id=NEW.strategy_id AND day_key=NEW.day_key)
   OR NEW.hits<>(SELECT coalesce(sum(hit),0) FROM su1_live_shadow WHERE strategy_id=NEW.strategy_id AND day_key=NEW.day_key)
 THEN RAISE(ABORT,'SU1_LIVE_DAILY_MISMATCH') END;
END;

-- su1_live_daily_immutable
CREATE TRIGGER su1_live_daily_immutable BEFORE UPDATE ON su1_live_daily
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_live_daily_no_delete
CREATE TRIGGER su1_live_daily_no_delete BEFORE DELETE ON su1_live_daily
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_live_day_archive
CREATE TABLE su1_live_day_archive (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 day_key INTEGER NOT NULL,
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 actions INTEGER NOT NULL,
 hits INTEGER NOT NULL,
 created_at INTEGER NOT NULL,
 compacted_at INTEGER,
 PRIMARY KEY(strategy_id,day_key)
);

-- su1_live_day_archive_update
CREATE TRIGGER su1_live_day_archive_update BEFORE UPDATE ON su1_live_day_archive
BEGIN
 SELECT CASE WHEN OLD.compacted_at IS NOT NULL OR NEW.compacted_at IS NULL OR NEW.strategy_id<>OLD.strategy_id OR NEW.day_key<>OLD.day_key OR NEW.payload_hash<>OLD.payload_hash OR NEW.actions<>OLD.actions OR NEW.hits<>OLD.hits OR NEW.created_at<>OLD.created_at
 THEN RAISE(ABORT,'SU1_DAILY_ARCHIVE_IMMUTABLE') END;
END;

-- su1_live_shadow
CREATE TABLE su1_live_shadow (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 sequence_ordinal INTEGER NOT NULL CHECK(sequence_ordinal>0),
 day_key INTEGER NOT NULL CHECK(day_key BETWEEN 20200101 AND 21001231),
 position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
 number_mask INTEGER NOT NULL CHECK(number_mask BETWEEN 7 AND 896),
 captured_at INTEGER NOT NULL,
 target_open_time INTEGER NOT NULL,
 hit INTEGER CHECK(hit IN (0,1)),
 settled_at INTEGER,
 PRIMARY KEY(strategy_id,sequence_ordinal),
 CHECK(captured_at<target_open_time),
 CHECK(((number_mask>>0)&1)+((number_mask>>1)&1)+((number_mask>>2)&1)+((number_mask>>3)&1)+((number_mask>>4)&1)
      +((number_mask>>5)&1)+((number_mask>>6)&1)+((number_mask>>7)&1)+((number_mask>>8)&1)+((number_mask>>9)&1)=3),
 CHECK((hit IS NULL AND settled_at IS NULL) OR (hit IS NOT NULL AND settled_at>=target_open_time))
) WITHOUT ROWID;

-- su1_live_shadow_compact_guard
CREATE TRIGGER su1_live_shadow_compact_guard BEFORE DELETE ON su1_live_shadow
BEGIN
 SELECT CASE WHEN OLD.hit IS NULL OR NOT EXISTS(SELECT 1 FROM su1_live_daily d WHERE d.strategy_id=OLD.strategy_id AND d.day_key=OLD.day_key)
 THEN RAISE(ABORT,'SU1_LIVE_COMPACT_REQUIRES_DAILY') END;
END;

-- su1_live_shadow_freeze
CREATE TRIGGER su1_live_shadow_freeze BEFORE INSERT ON su1_live_shadow
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_research_pool WHERE strategy_id=NEW.strategy_id
   AND state='ACTIVE' AND born_sequence<=NEW.sequence_ordinal)
 THEN RAISE(ABORT,'SU1_LIVE_NOT_IN_ACTIVE_POOL') END;
 SELECT CASE WHEN NEW.hit IS NOT NULL OR NEW.settled_at IS NOT NULL
 THEN RAISE(ABORT,'SU1_LIVE_FREEZE_MUST_BE_UNSETTLED') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_live_shadow WHERE strategy_id=NEW.strategy_id AND sequence_ordinal=NEW.sequence_ordinal)
 THEN RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT') END;
END;

-- su1_live_shadow_pending
CREATE INDEX su1_live_shadow_pending ON su1_live_shadow(sequence_ordinal) WHERE hit IS NULL;

-- su1_live_shadow_settle
CREATE TRIGGER su1_live_shadow_settle BEFORE UPDATE ON su1_live_shadow
BEGIN
 SELECT CASE WHEN NOT (OLD.hit IS NULL AND NEW.hit IS NOT NULL AND NEW.strategy_id=OLD.strategy_id
   AND NEW.sequence_ordinal=OLD.sequence_ordinal AND NEW.day_key=OLD.day_key AND NEW.position=OLD.position
   AND NEW.number_mask=OLD.number_mask AND NEW.captured_at=OLD.captured_at AND NEW.target_open_time=OLD.target_open_time)
 THEN RAISE(ABORT,'SU1_LIVE_SETTLE_ONCE') END;
END;

-- su1_live_shadow_verified_compaction
CREATE TRIGGER su1_live_shadow_verified_compaction BEFORE DELETE ON su1_live_shadow
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_live_day_archive a JOIN su1_blob b ON b.blob_hash=a.payload_hash JOIN su1_archive_receipt r ON r.receipt_id=b.archive_receipt_id WHERE a.strategy_id=OLD.strategy_id AND a.day_key=OLD.day_key AND b.storage_state='ARCHIVED' AND r.replay_verified=1)
 THEN RAISE(ABORT,'SU1_VERIFIED_ARCHIVE_REQUIRED') END;
END;

-- su1_live_target
CREATE TABLE su1_live_target (
 sequence_ordinal INTEGER PRIMARY KEY NOT NULL CHECK(sequence_ordinal>0),
 target_issue TEXT UNIQUE NOT NULL,
 target_open_time INTEGER NOT NULL,
 snapshot_hash TEXT NOT NULL REFERENCES su1_snapshot(snapshot_hash),
 legacy_settlement_hash TEXT,
 completed_at INTEGER,
 CHECK((legacy_settlement_hash IS NULL AND completed_at IS NULL) OR (length(legacy_settlement_hash)=64 AND completed_at>=target_open_time))
);

-- su1_live_target_update
CREATE TRIGGER su1_live_target_update BEFORE UPDATE ON su1_live_target
BEGIN
 SELECT CASE WHEN OLD.completed_at IS NOT NULL OR NEW.completed_at IS NULL OR NEW.sequence_ordinal<>OLD.sequence_ordinal
 OR NEW.target_issue<>OLD.target_issue OR NEW.target_open_time<>OLD.target_open_time OR NEW.snapshot_hash<>OLD.snapshot_hash
 THEN RAISE(ABORT,'SU1_TARGET_IMMUTABLE') END;
END;

-- su1_look_binding
CREATE TRIGGER su1_look_binding BEFORE INSERT ON su1_evidence_look
BEGIN
 SELECT CASE WHEN NEW.planned_opportunities<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_opportunity_outcome a ON a.opportunity_id=o.opportunity_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND a.completed_at<=NEW.recorded_at)
 THEN RAISE(ABORT,'SU1_LOOK_CANONICAL_PREFIX_NOT_COMPLETE') END;
 SELECT CASE WHEN abs(NEW.cumulative_alpha_spent-NEW.allocated_alpha-
  (SELECT coalesce(sum(allocated_alpha),0) FROM su1_evidence_look WHERE cohort_id=NEW.cohort_id))>0.000000000001
 THEN RAISE(ABORT,'SU1_LOOK_ALPHA_LEDGER') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_opportunity o JOIN su1_prediction p ON p.opportunity_id=o.opportunity_id
  LEFT JOIN su1_settlement s ON s.prediction_id=p.prediction_id AND s.committed_at<=NEW.recorded_at
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities AND s.settlement_id IS NULL)
 THEN RAISE(ABORT,'SU1_LOOK_PENDING_SETTLEMENT') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_eval_cohort c WHERE c.cohort_id=NEW.cohort_id
  AND c.claim_id=NEW.claim_id AND c.protocol_hash=NEW.protocol_hash
  AND c.global_claim_ordinal=NEW.global_claim_ordinal
  AND NEW.planned_opportunities<=c.planned_opportunities
  AND NEW.allocated_alpha<=c.family_alpha /
      (1.0*c.global_claim_ordinal*(c.global_claim_ordinal+1)*NEW.look_ordinal*(NEW.look_ordinal+1))
  AND NEW.cumulative_alpha_spent<=c.family_alpha)
 THEN RAISE(ABORT,'SU1_LOOK_PROTOCOL_OR_ALPHA') END;
 SELECT CASE WHEN NEW.planned_opportunities<>(SELECT count(*) FROM su1_opportunity
  WHERE cohort_id=NEW.cohort_id AND opportunity_ordinal<=NEW.planned_opportunities AND deadline_at<=NEW.recorded_at)
 THEN RAISE(ABORT,'SU1_LOOK_OPPORTUNITY_DENOMINATOR') END;
END;

-- su1_look_derived_counts
CREATE TRIGGER su1_look_derived_counts BEFORE INSERT ON su1_evidence_look
BEGIN
 SELECT CASE WHEN NEW.paired_n<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at)
 THEN RAISE(ABORT,'SU1_LOOK_PAIRED_COUNT') END;
 SELECT CASE WHEN NEW.parent_missing_n<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  LEFT JOIN su1_prediction p ON p.opportunity_id=o.opportunity_id AND p.strategy_id=c.parent_strategy_id
  LEFT JOIN su1_settlement s ON s.prediction_id=p.prediction_id AND s.committed_at<=NEW.recorded_at
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities AND s.settlement_id IS NULL)
 THEN RAISE(ABORT,'SU1_LOOK_PARENT_MISSING') END;
 SELECT CASE WHEN NEW.child_missing_n<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  LEFT JOIN su1_prediction p ON p.opportunity_id=o.opportunity_id AND p.strategy_id=c.strategy_id
  LEFT JOIN su1_settlement s ON s.prediction_id=p.prediction_id AND s.committed_at<=NEW.recorded_at
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities AND s.settlement_id IS NULL)
 THEN RAISE(ABORT,'SU1_LOOK_CHILD_MISSING') END;
 SELECT CASE WHEN NEW.parent_hits<>(SELECT coalesce(sum(sa.hit),0) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at)
 OR NEW.child_hits<>(SELECT coalesce(sum(sb.hit),0) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at)
 THEN RAISE(ABORT,'SU1_LOOK_HIT_COUNTS') END;
 SELECT CASE WHEN NEW.child_only_wins<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at AND sa.hit=0 AND sb.hit=1)
 OR NEW.parent_only_wins<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at AND sa.hit=1 AND sb.hit=0)
 THEN RAISE(ABORT,'SU1_LOOK_DISCORDANT_PAIRS') END;
 SELECT CASE WHEN abs(NEW.conservative_mean-
  (1.0*(NEW.child_only_wins-NEW.parent_only_wins-NEW.unpaired_n)/NEW.planned_opportunities))>0.000000001
  OR abs(NEW.coverage-(1.0*NEW.paired_n/NEW.planned_opportunities))>0.000000001
  OR (NEW.paired_n=0 AND NEW.paired_delta IS NOT NULL)
  OR (NEW.paired_n>0 AND (NEW.paired_delta IS NULL OR abs(NEW.paired_delta-
     (1.0*(NEW.child_hits-NEW.parent_hits)/NEW.paired_n))>0.000000001))
  OR abs(NEW.lower_bound-max(-1.0,NEW.conservative_mean-NEW.bound_radius))>0.000000001
 THEN RAISE(ABORT,'SU1_LOOK_DERIVED_METRICS') END;
END;

-- su1_look_plan
CREATE TABLE su1_look_plan (
 cohort_id TEXT NOT NULL REFERENCES su1_eval_cohort(cohort_id),
 look_ordinal INTEGER NOT NULL CHECK(look_ordinal>0),
 planned_opportunities INTEGER NOT NULL CHECK(planned_opportunities>0),
 schedule_hash TEXT NOT NULL CHECK(length(schedule_hash)=64),
 PRIMARY KEY(cohort_id,look_ordinal),
 UNIQUE(cohort_id,look_ordinal,planned_opportunities)
);

-- su1_look_plan_binding
CREATE TRIGGER su1_look_plan_binding BEFORE INSERT ON su1_look_plan
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_opportunity WHERE cohort_id=NEW.cohort_id)
 OR NOT EXISTS(SELECT 1 FROM su1_eval_cohort WHERE cohort_id=NEW.cohort_id
  AND NEW.planned_opportunities<=planned_opportunities)
 THEN RAISE(ABORT,'SU1_LOOK_PLAN_MUST_PRECEDE_OPPORTUNITY') END;
END;

-- su1_look_plan_immutable
CREATE TRIGGER su1_look_plan_immutable BEFORE UPDATE ON su1_look_plan
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_look_plan_no_delete
CREATE TRIGGER su1_look_plan_no_delete BEFORE DELETE ON su1_look_plan
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_look_plan_no_replace
CREATE TRIGGER su1_look_plan_no_replace BEFORE INSERT ON su1_look_plan
WHEN EXISTS(SELECT 1 FROM su1_look_plan WHERE cohort_id=NEW.cohort_id AND look_ordinal=NEW.look_ordinal)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_macro
CREATE TABLE su1_macro (
 macro_id TEXT NOT NULL PRIMARY KEY CHECK(macro_id LIKE 'M%' AND length(macro_id)=11),
 canonical_text TEXT NOT NULL UNIQUE CHECK(length(canonical_text) BETWEEN 3 AND 4096),
 support INTEGER NOT NULL CHECK(support>=2),
 best_fitness REAL NOT NULL,
 born_campaign TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 last_campaign TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 state TEXT NOT NULL CHECK(state IN ('ACTIVE','RETIRED'))
);

-- su1_macro_no_delete
CREATE TRIGGER su1_macro_no_delete BEFORE DELETE ON su1_macro
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_macro_update
CREATE TRIGGER su1_macro_update BEFORE UPDATE ON su1_macro
BEGIN
 SELECT CASE WHEN NEW.macro_id<>OLD.macro_id OR NEW.canonical_text<>OLD.canonical_text OR NEW.born_campaign<>OLD.born_campaign
   OR NEW.support<OLD.support OR (OLD.state='RETIRED' AND NEW.state='ACTIVE')
 THEN RAISE(ABORT,'SU1_MACRO_IDENTITY_OR_MONOTONE') END;
END;

-- su1_mainline_binding
CREATE TABLE su1_mainline_binding (
 target_issue INTEGER PRIMARY KEY,
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 certificate_id TEXT NOT NULL REFERENCES su1_certificate(certificate_id),
 prediction_id TEXT NOT NULL UNIQUE REFERENCES su1_prediction(prediction_id),
 adapter_contract_hash TEXT NOT NULL CHECK(length(adapter_contract_hash)=64),
 original_owner_receipt_hash TEXT NOT NULL CHECK(length(original_owner_receipt_hash)=64),
 bound_at INTEGER NOT NULL
);

-- su1_mainline_binding_immutable
CREATE TRIGGER su1_mainline_binding_immutable BEFORE UPDATE ON su1_mainline_binding
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_mainline_binding_no_delete
CREATE TRIGGER su1_mainline_binding_no_delete BEFORE DELETE ON su1_mainline_binding
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_mainline_binding_no_replace
CREATE TRIGGER su1_mainline_binding_no_replace BEFORE INSERT ON su1_mainline_binding
WHEN EXISTS(SELECT 1 FROM su1_mainline_binding WHERE target_issue=NEW.target_issue) OR EXISTS(SELECT 1 FROM su1_mainline_binding WHERE prediction_id=NEW.prediction_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_mainline_guard
CREATE TRIGGER su1_mainline_guard BEFORE INSERT ON su1_mainline_binding
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_runtime_control WHERE singleton=1 AND mode='MAINLINE_ADAPTER')
 THEN RAISE(ABORT,'SU1_MAINLINE_DISABLED') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_certificate c JOIN su1_prediction p ON p.prediction_id=NEW.prediction_id
  JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash
  WHERE c.certificate_id=NEW.certificate_id AND c.strategy_id=NEW.strategy_id AND p.strategy_id=NEW.strategy_id
  AND p.target_issue=NEW.target_issue AND NEW.bound_at>=p.captured_at
  AND NEW.bound_at>=c.issued_at AND NEW.bound_at<s.target_open_time AND NEW.bound_at<c.expires_at
  AND NOT EXISTS(SELECT 1 FROM su1_certificate_revocation r WHERE r.certificate_id=c.certificate_id))
 THEN RAISE(ABORT,'SU1_MAINLINE_CERTIFICATE_MISMATCH') END;
 
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_certificate c JOIN su1_evidence_look e ON e.evidence_hash=c.evidence_hash
  JOIN su1_eval_cohort ec ON ec.cohort_id=e.cohort_id
  JOIN su1_eval_cohort xc ON xc.family_root_hash=ec.family_root_hash AND xc.claim_kind='NULL_CONTROL'
  JOIN su1_evidence_look x ON x.cohort_id=xc.cohort_id AND x.result_state='PASS'
  WHERE c.certificate_id=NEW.certificate_id)
 THEN RAISE(ABORT,'SU1_NULL_CONTROL_ALARM') END;
END;

-- su1_mainline_head_event
CREATE TABLE su1_mainline_head_event (
 generation INTEGER PRIMARY KEY CHECK(generation>=1),
 event_hash TEXT NOT NULL UNIQUE CHECK(length(event_hash)=64),
 previous_hash TEXT NOT NULL CHECK(length(previous_hash)=64),
 strategy_id TEXT REFERENCES su1_strategy_version(strategy_id),
 certificate_id TEXT REFERENCES su1_certificate(certificate_id),
 operation TEXT NOT NULL CHECK(operation IN ('BOOTSTRAP','PROMOTE','RENEW','FALLBACK','REVOKE','USER_ROLLBACK')),
 policy_hash TEXT NOT NULL CHECK(length(policy_hash)=64),
 recorded_at INTEGER NOT NULL,
 CHECK((strategy_id IS NULL AND certificate_id IS NULL AND operation='REVOKE') OR
       (strategy_id IS NOT NULL AND certificate_id IS NOT NULL))
);

-- su1_mainline_head_no_delete
CREATE TRIGGER su1_mainline_head_no_delete BEFORE DELETE ON su1_mainline_head_event
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_mainline_head_no_update
CREATE TRIGGER su1_mainline_head_no_update BEFORE UPDATE ON su1_mainline_head_event
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_mainline_head_sequence
CREATE TRIGGER su1_mainline_head_sequence BEFORE INSERT ON su1_mainline_head_event
BEGIN
 SELECT CASE WHEN NEW.generation<>coalesce((SELECT max(generation)+1 FROM su1_mainline_head_event),1)
   THEN RAISE(ABORT,'SU1_MAINLINE_HEAD_GENERATION') END;
 SELECT CASE WHEN NEW.previous_hash<>coalesce((SELECT event_hash FROM su1_mainline_head_event ORDER BY generation DESC LIMIT 1),'0000000000000000000000000000000000000000000000000000000000000000')
   THEN RAISE(ABORT,'SU1_MAINLINE_HEAD_PREVIOUS') END;
 SELECT CASE WHEN NEW.certificate_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM su1_certificate c WHERE c.certificate_id=NEW.certificate_id AND c.strategy_id=NEW.strategy_id AND c.issued_at<=NEW.recorded_at AND c.expires_at>NEW.recorded_at AND NOT EXISTS(SELECT 1 FROM su1_certificate_revocation r WHERE r.certificate_id=c.certificate_id))
   THEN RAISE(ABORT,'SU1_MAINLINE_HEAD_CERTIFICATE') END;
END;

-- su1_migration_unit_v2
CREATE TABLE su1_migration_unit_v2(unit INTEGER PRIMARY KEY,sql_hash TEXT NOT NULL,installed_at INTEGER NOT NULL);

-- su1_model_artifact
CREATE TABLE su1_model_artifact (
 artifact_hash TEXT NOT NULL PRIMARY KEY CHECK(length(artifact_hash)=64),
 protocol_hash TEXT NOT NULL REFERENCES su1_train_protocol(protocol_hash),
 program_hash TEXT NOT NULL REFERENCES su1_program(program_hash),
 model_kind TEXT NOT NULL,
 encoder_hash TEXT NOT NULL CHECK(length(encoder_hash)=64),
 weights_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 trained_through_sequence INTEGER NOT NULL CHECK(trained_through_sequence>=0),
 training_report_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 created_at INTEGER NOT NULL
);

-- su1_model_artifact_immutable
CREATE TRIGGER su1_model_artifact_immutable BEFORE UPDATE ON su1_model_artifact
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_model_artifact_no_delete
CREATE TRIGGER su1_model_artifact_no_delete BEFORE DELETE ON su1_model_artifact
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_model_artifact_no_replace
CREATE TRIGGER su1_model_artifact_no_replace BEFORE INSERT ON su1_model_artifact
WHEN EXISTS(SELECT 1 FROM su1_model_artifact WHERE artifact_hash=NEW.artifact_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_model_binding
CREATE TRIGGER su1_model_binding BEFORE INSERT ON su1_model_artifact
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_train_protocol p WHERE p.protocol_hash=NEW.protocol_hash
  AND p.program_hash=NEW.program_hash AND p.validation_last_sequence<=NEW.trained_through_sequence)
 THEN RAISE(ABORT,'SU1_MODEL_PROTOCOL_MISMATCH') END;
END;

-- su1_opportunity
CREATE TABLE su1_opportunity (
 opportunity_id TEXT NOT NULL PRIMARY KEY CHECK(length(opportunity_id)=64),
 cohort_id TEXT NOT NULL REFERENCES su1_eval_cohort(cohort_id),
 target_issue INTEGER NOT NULL,
 sequence_ordinal INTEGER NOT NULL CHECK(sequence_ordinal>0),
 opportunity_ordinal INTEGER NOT NULL CHECK(opportunity_ordinal>0),
 declared_at INTEGER NOT NULL,
 deadline_at INTEGER NOT NULL,
 source_epoch TEXT NOT NULL,
 eligibility_hash TEXT NOT NULL CHECK(length(eligibility_hash)=64),
 role TEXT NOT NULL CHECK(role IN ('RESEARCH_FUTURE','CONFIRM','CALIBRATION_FIT','CALIBRATION_VALIDATION')),
 UNIQUE(cohort_id,target_issue), UNIQUE(cohort_id,opportunity_ordinal),
 CHECK(declared_at<deadline_at)
);

-- su1_opportunity_binding
CREATE TRIGGER su1_opportunity_binding BEFORE INSERT ON su1_opportunity
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_eval_cohort c WHERE c.cohort_id=NEW.cohort_id
  AND c.evidence_role=NEW.role AND NEW.sequence_ordinal>c.registered_sequence
  AND NEW.opportunity_ordinal<=c.planned_opportunities AND NEW.target_issue>=c.first_issue)
 THEN RAISE(ABORT,'SU1_OPPORTUNITY_PROTOCOL_MISMATCH') END;
 SELECT CASE WHEN NEW.role='CONFIRM' AND NOT EXISTS(SELECT 1 FROM su1_look_plan WHERE cohort_id=NEW.cohort_id)
 THEN RAISE(ABORT,'SU1_LOOK_PLAN_NOT_REGISTERED') END;
END;

-- su1_opportunity_deadline
CREATE INDEX su1_opportunity_deadline ON su1_opportunity(deadline_at,cohort_id);

-- su1_opportunity_immutable
CREATE TRIGGER su1_opportunity_immutable BEFORE UPDATE ON su1_opportunity
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_opportunity_no_delete
CREATE TRIGGER su1_opportunity_no_delete BEFORE DELETE ON su1_opportunity
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_opportunity_no_replace
CREATE TRIGGER su1_opportunity_no_replace BEFORE INSERT ON su1_opportunity
WHEN EXISTS(SELECT 1 FROM su1_opportunity WHERE opportunity_id=NEW.opportunity_id) OR EXISTS(SELECT 1 FROM su1_opportunity WHERE cohort_id=NEW.cohort_id AND target_issue=NEW.target_issue) OR EXISTS(SELECT 1 FROM su1_opportunity WHERE cohort_id=NEW.cohort_id AND opportunity_ordinal=NEW.opportunity_ordinal)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_opportunity_outcome
CREATE TABLE su1_opportunity_outcome (
 opportunity_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_opportunity(opportunity_id),
 target_issue INTEGER NOT NULL,
 canonical_draw_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 legacy_settlement_hash TEXT NOT NULL CHECK(length(legacy_settlement_hash)=64),
 source_epoch TEXT NOT NULL,
 opened_at INTEGER NOT NULL,
 completed_at INTEGER NOT NULL CHECK(completed_at>=opened_at)
);

-- su1_opportunity_outcome_binding
CREATE TRIGGER su1_opportunity_outcome_binding BEFORE INSERT ON su1_opportunity_outcome
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_opportunity o WHERE o.opportunity_id=NEW.opportunity_id
  AND o.target_issue=NEW.target_issue AND o.source_epoch=NEW.source_epoch AND NEW.opened_at>=o.deadline_at)
 OR EXISTS(SELECT 1 FROM su1_opportunity_outcome WHERE target_issue=NEW.target_issue
  AND (canonical_draw_hash<>NEW.canonical_draw_hash OR legacy_settlement_hash<>NEW.legacy_settlement_hash))
 THEN RAISE(ABORT,'SU1_OPPORTUNITY_OUTCOME_BINDING') END;
END;

-- su1_opportunity_outcome_immutable
CREATE TRIGGER su1_opportunity_outcome_immutable BEFORE UPDATE ON su1_opportunity_outcome
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_opportunity_outcome_issue
CREATE INDEX su1_opportunity_outcome_issue ON su1_opportunity_outcome(target_issue,opportunity_id);

-- su1_opportunity_outcome_no_delete
CREATE TRIGGER su1_opportunity_outcome_no_delete BEFORE DELETE ON su1_opportunity_outcome
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_opportunity_outcome_no_replace
CREATE TRIGGER su1_opportunity_outcome_no_replace BEFORE INSERT ON su1_opportunity_outcome
WHEN EXISTS(SELECT 1 FROM su1_opportunity_outcome WHERE opportunity_id=NEW.opportunity_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_outbox
CREATE TABLE su1_outbox (
 event_id TEXT NOT NULL PRIMARY KEY,
 event_type TEXT NOT NULL CHECK(event_type IN ('FROZEN','SETTLED')),
 prediction_id TEXT NOT NULL REFERENCES su1_prediction(prediction_id),
 settlement_id TEXT REFERENCES su1_settlement(settlement_id),
 source_record_hash TEXT NOT NULL CHECK(length(source_record_hash)=64),
 state TEXT NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','LEASED','DONE','DEAD')),
 attempt_count INTEGER NOT NULL DEFAULT 0 CHECK(attempt_count>=0),
 lease_generation INTEGER NOT NULL DEFAULT 0 CHECK(lease_generation>=0),
 lease_owner TEXT,
 lease_until INTEGER,
 retry_at INTEGER NOT NULL DEFAULT 0,
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 last_error TEXT NOT NULL DEFAULT '',
 CHECK((event_type='FROZEN' AND settlement_id IS NULL) OR
       (event_type='SETTLED' AND settlement_id IS NOT NULL)),
 CHECK((state='LEASED' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL) OR
       (state<>'LEASED' AND lease_owner IS NULL AND lease_until IS NULL))
);

-- su1_outbox_identity
CREATE TRIGGER su1_outbox_identity BEFORE UPDATE ON su1_outbox
WHEN NEW.event_id<>OLD.event_id OR NEW.event_type<>OLD.event_type OR NEW.prediction_id<>OLD.prediction_id
 OR NEW.settlement_id IS NOT OLD.settlement_id OR NEW.source_record_hash<>OLD.source_record_hash OR NEW.created_at<>OLD.created_at
BEGIN SELECT RAISE(ABORT,'SU1_OUTBOX_IDENTITY_IMMUTABLE'); END;

-- su1_outbox_no_delete
CREATE TRIGGER su1_outbox_no_delete BEFORE DELETE ON su1_outbox
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_outbox_no_replace
CREATE TRIGGER su1_outbox_no_replace BEFORE INSERT ON su1_outbox
WHEN EXISTS(SELECT 1 FROM su1_outbox WHERE event_id=NEW.event_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_outbox_pending
CREATE INDEX su1_outbox_pending ON su1_outbox(state,retry_at,created_at);

-- su1_outbox_transition
CREATE TRIGGER su1_outbox_transition BEFORE UPDATE ON su1_outbox
BEGIN
 SELECT CASE WHEN NOT ((OLD.state IN ('PENDING','LEASED') AND NEW.state='LEASED'
   AND NEW.lease_generation=OLD.lease_generation+1 AND NEW.attempt_count=OLD.attempt_count+1)
  OR (OLD.state='LEASED' AND NEW.state IN ('PENDING','DONE','DEAD')
   AND NEW.lease_generation=OLD.lease_generation AND NEW.attempt_count=OLD.attempt_count)
  OR (OLD.state='DEAD' AND NEW.state='PENDING' AND NEW.lease_generation=OLD.lease_generation
   AND NEW.attempt_count=OLD.attempt_count))
 THEN RAISE(ABORT,'SU1_OUTBOX_STATE_TRANSITION') END;
END;

-- su1_pending_settlement
CREATE TABLE su1_pending_settlement (
 prediction_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_prediction(prediction_id),
 target_issue INTEGER NOT NULL,
 enqueued_at INTEGER NOT NULL
);

-- su1_pending_settlement_delete_guard
CREATE TRIGGER su1_pending_settlement_delete_guard BEFORE DELETE ON su1_pending_settlement
WHEN NOT EXISTS(SELECT 1 FROM su1_settlement WHERE prediction_id=OLD.prediction_id)
BEGIN SELECT RAISE(ABORT,'SU1_PENDING_REQUIRES_SETTLEMENT'); END;

-- su1_pending_settlement_insert_guard
CREATE TRIGGER su1_pending_settlement_insert_guard BEFORE INSERT ON su1_pending_settlement
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_prediction WHERE prediction_id=NEW.prediction_id
  AND target_issue=NEW.target_issue AND captured_at=NEW.enqueued_at)
 OR EXISTS(SELECT 1 FROM su1_pending_settlement WHERE prediction_id=NEW.prediction_id)
 THEN RAISE(ABORT,'SU1_PENDING_BINDING_OR_REPLAY') END;
END;

-- su1_pending_settlement_issue
CREATE INDEX su1_pending_settlement_issue ON su1_pending_settlement(target_issue,prediction_id);

-- su1_pending_settlement_update_guard
CREATE TRIGGER su1_pending_settlement_update_guard BEFORE UPDATE ON su1_pending_settlement
BEGIN SELECT RAISE(ABORT,'SU1_PENDING_IDENTITY_IMMUTABLE'); END;

-- su1_pin_release_only
CREATE TRIGGER su1_pin_release_only BEFORE UPDATE ON su1_retention_pin
BEGIN
 SELECT CASE WHEN NOT(NEW.pin_id=OLD.pin_id AND NEW.object_kind=OLD.object_kind AND NEW.object_id=OLD.object_id
  AND NEW.reason=OLD.reason AND NEW.created_at=OLD.created_at AND OLD.released_at IS NULL AND NEW.released_at IS NOT NULL)
 THEN RAISE(ABORT,'SU1_PIN_RELEASE_ONLY') END;
END;

-- su1_pool_no_delete
CREATE TRIGGER su1_pool_no_delete BEFORE DELETE ON su1_research_pool
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_pool_one_active
CREATE UNIQUE INDEX su1_pool_one_active ON su1_research_pool(strategy_id) WHERE state='ACTIVE';

-- su1_pool_transition
CREATE TRIGGER su1_pool_transition BEFORE UPDATE ON su1_research_pool
BEGIN
 SELECT CASE WHEN NOT (OLD.state='ACTIVE' AND NEW.state='RETIRED' AND NEW.entry_id=OLD.entry_id
   AND NEW.strategy_id=OLD.strategy_id AND NEW.campaign_id=OLD.campaign_id AND NEW.arm=OLD.arm
   AND NEW.born_sequence=OLD.born_sequence AND NEW.prior_fitness=OLD.prior_fitness AND NEW.created_at=OLD.created_at)
 THEN RAISE(ABORT,'SU1_POOL_TRANSITION') END;
END;

-- su1_portable_import
CREATE TABLE su1_portable_import (
 artifact_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 kernel_hash TEXT NOT NULL CHECK(length(kernel_hash)=64),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 source_through_issue TEXT NOT NULL,
 recorded_at INTEGER NOT NULL,
 PRIMARY KEY(artifact_hash,kernel_hash)
);

-- su1_portable_import_immutable
CREATE TRIGGER su1_portable_import_immutable BEFORE UPDATE ON su1_portable_import
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_prediction
CREATE TABLE su1_prediction (
 prediction_id TEXT NOT NULL PRIMARY KEY CHECK(length(prediction_id)=64),
 opportunity_id TEXT NOT NULL REFERENCES su1_opportunity(opportunity_id),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 snapshot_hash TEXT NOT NULL REFERENCES su1_snapshot(snapshot_hash),
 target_issue INTEGER NOT NULL,
 captured_at INTEGER NOT NULL,
 position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
 n1 INTEGER NOT NULL CHECK(n1 BETWEEN 1 AND 10),
 n2 INTEGER NOT NULL CHECK(n2 BETWEEN 1 AND 10),
 n3 INTEGER NOT NULL CHECK(n3 BETWEEN 1 AND 10),
 prediction_mask INTEGER NOT NULL CHECK(prediction_mask BETWEEN 1 AND 1023),
 raw_k3_score REAL NOT NULL CHECK(raw_k3_score>=0 AND raw_k3_score<=1),
 selected_hit_p REAL CHECK(selected_hit_p>=0 AND selected_hit_p<=1),
 calibration_hash TEXT REFERENCES su1_blob(blob_hash),
 probability_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 period_state_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash) CHECK(length(period_state_hash)=64),
 trained_through_sequence INTEGER NOT NULL,
 input_through_sequence INTEGER NOT NULL,
 record_hash TEXT NOT NULL UNIQUE CHECK(length(record_hash)=64),
 UNIQUE(opportunity_id,strategy_id),
 CHECK(n1<n2 AND n2<n3),
 CHECK(prediction_mask=((1 << (n1-1)) | (1 << (n2-1)) | (1 << (n3-1)))),
 CHECK((selected_hit_p IS NULL AND calibration_hash IS NULL) OR
       (selected_hit_p IS NOT NULL AND calibration_hash IS NOT NULL))
);

-- su1_prediction_binding
CREATE TRIGGER su1_prediction_binding BEFORE INSERT ON su1_prediction
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_runtime_control WHERE singleton=1 AND mode<>'DISABLED')
 THEN RAISE(ABORT,'SU1_DISABLED') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_opportunity o JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_snapshot s ON s.snapshot_hash=NEW.snapshot_hash
  JOIN su1_strategy_version v ON v.strategy_id=NEW.strategy_id
  JOIN su1_model_artifact m ON m.artifact_hash=v.artifact_hash
  WHERE o.opportunity_id=NEW.opportunity_id AND o.target_issue=NEW.target_issue
  AND NOT EXISTS(SELECT 1 FROM su1_opportunity_outcome a WHERE a.opportunity_id=o.opportunity_id)
  AND s.target_issue=NEW.target_issue AND s.source_epoch=o.source_epoch
  AND NEW.captured_at>=s.captured_at AND NEW.captured_at>=o.declared_at AND NEW.captured_at<o.deadline_at
  AND NEW.captured_at<s.target_open_time AND v.created_sequence<o.sequence_ordinal
  AND NEW.trained_through_sequence=m.trained_through_sequence
  AND NEW.trained_through_sequence<o.sequence_ordinal AND NEW.input_through_sequence<o.sequence_ordinal
  AND NEW.input_through_sequence>=NEW.trained_through_sequence
  AND (NEW.strategy_id=c.strategy_id OR NEW.strategy_id=c.parent_strategy_id)
  AND ((NEW.calibration_hash IS NULL AND NEW.selected_hit_p IS NULL) OR
       (v.calibration_state='FROZEN' AND NEW.calibration_hash=v.calibration_hash)))
 THEN RAISE(ABORT,'SU1_FREEZE_BINDING_OR_TIME') END;
END;

-- su1_prediction_immutable
CREATE TRIGGER su1_prediction_immutable BEFORE UPDATE ON su1_prediction
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_prediction_issue
CREATE INDEX su1_prediction_issue ON su1_prediction(target_issue,strategy_id);

-- su1_prediction_no_delete
CREATE TRIGGER su1_prediction_no_delete BEFORE DELETE ON su1_prediction
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_prediction_no_replace
CREATE TRIGGER su1_prediction_no_replace BEFORE INSERT ON su1_prediction
WHEN EXISTS(SELECT 1 FROM su1_prediction WHERE prediction_id=NEW.prediction_id) OR EXISTS(SELECT 1 FROM su1_prediction WHERE record_hash=NEW.record_hash) OR EXISTS(SELECT 1 FROM su1_prediction WHERE opportunity_id=NEW.opportunity_id AND strategy_id=NEW.strategy_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_program
CREATE TABLE su1_program (
 program_hash TEXT NOT NULL PRIMARY KEY CHECK(length(program_hash)=64),
 grammar_hash TEXT NOT NULL CHECK(length(grammar_hash)=64),
 input_schema_hash TEXT NOT NULL CHECK(length(input_schema_hash)=64),
 typed_ir_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 state_schema_hash TEXT NOT NULL CHECK(length(state_schema_hash)=64),
 state_byte_limit INTEGER NOT NULL CHECK(state_byte_limit BETWEEN 0 AND 1048576),
 operation_limit INTEGER NOT NULL CHECK(operation_limit BETWEEN 1 AND 1000000),
 created_at INTEGER NOT NULL
);

-- su1_program_immutable
CREATE TRIGGER su1_program_immutable BEFORE UPDATE ON su1_program
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_program_no_delete
CREATE TRIGGER su1_program_no_delete BEFORE DELETE ON su1_program
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_program_no_replace
CREATE TRIGGER su1_program_no_replace BEFORE INSERT ON su1_program
WHEN EXISTS(SELECT 1 FROM su1_program WHERE program_hash=NEW.program_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_quality_receipt
CREATE TABLE su1_quality_receipt (
 strategy_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_strategy_version(strategy_id),
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 recorded_at INTEGER NOT NULL
);

-- su1_quality_receipt_immutable
CREATE TRIGGER su1_quality_receipt_immutable BEFORE UPDATE ON su1_quality_receipt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_relation_asset
CREATE TABLE su1_relation_asset (
 asset_hash TEXT NOT NULL PRIMARY KEY CHECK(length(asset_hash)=64),
 kind TEXT NOT NULL CHECK(kind IN ('PRIMITIVE','MACRO','STATE_TRANSITION')),
 typed_contract_hash TEXT NOT NULL CHECK(length(typed_contract_hash)=64),
 definition_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 material_status TEXT NOT NULL CHECK(material_status='MATERIAL'),
 available_through_sequence INTEGER NOT NULL CHECK(available_through_sequence>=0),
 created_at INTEGER NOT NULL
);

-- su1_relation_asset_immutable
CREATE TRIGGER su1_relation_asset_immutable BEFORE UPDATE ON su1_relation_asset
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_relation_asset_no_delete
CREATE TRIGGER su1_relation_asset_no_delete BEFORE DELETE ON su1_relation_asset
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_relation_asset_no_replace
CREATE TRIGGER su1_relation_asset_no_replace BEFORE INSERT ON su1_relation_asset
WHEN EXISTS(SELECT 1 FROM su1_relation_asset WHERE asset_hash=NEW.asset_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_research_knowledge
CREATE TABLE su1_research_knowledge (
 attempt_id TEXT PRIMARY KEY REFERENCES su1_search_attempt(attempt_id),
 context_key TEXT NOT NULL,
 hypothesis TEXT NOT NULL,
 action TEXT NOT NULL,
 cost_units INTEGER NOT NULL CHECK(cost_units>=1),
 result TEXT NOT NULL,
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 recorded_at INTEGER NOT NULL
);

-- su1_research_knowledge_immutable
CREATE TRIGGER su1_research_knowledge_immutable BEFORE UPDATE ON su1_research_knowledge
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_research_knowledge_no_delete
CREATE TRIGGER su1_research_knowledge_no_delete BEFORE DELETE ON su1_research_knowledge
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_research_pool
CREATE TABLE su1_research_pool (
 entry_id TEXT NOT NULL PRIMARY KEY CHECK(length(entry_id)=64),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 arm TEXT NOT NULL,
 born_sequence INTEGER NOT NULL CHECK(born_sequence>0),
 prior_fitness REAL NOT NULL,
 state TEXT NOT NULL CHECK(state IN ('ACTIVE','RETIRED')),
 retired_sequence INTEGER,
 retire_reason TEXT,
 created_at INTEGER NOT NULL,
 CHECK((state='ACTIVE' AND retired_sequence IS NULL AND retire_reason IS NULL) OR
       (state='RETIRED' AND retired_sequence>=born_sequence AND retire_reason IS NOT NULL))
);

-- su1_research_task
CREATE TABLE su1_research_task (
 task_id TEXT NOT NULL PRIMARY KEY CHECK(length(task_id)=64),
 intent_hash TEXT NOT NULL UNIQUE CHECK(length(intent_hash)=64),
 feedback_hash TEXT REFERENCES su1_feedback(feedback_hash),
 kind TEXT NOT NULL CHECK(kind IN ('DISCOVER','TRAIN','CALIBRATE','COMPARE','FUSE','REPLAY')),
 input_hash TEXT NOT NULL CHECK(length(input_hash)=64),
 boundary_sequence INTEGER NOT NULL CHECK(boundary_sequence>=0),
 state TEXT NOT NULL CHECK(state IN ('PENDING','LEASED','WAITING','COMPLETED','FAILED','CANCELLED')),
 checkpoint_hash TEXT REFERENCES su1_blob(blob_hash),
 output_hash TEXT REFERENCES su1_blob(blob_hash),
 lease_generation INTEGER NOT NULL DEFAULT 0 CHECK(lease_generation>=0),
 lease_owner TEXT,
 lease_until INTEGER,
 attempt_count INTEGER NOT NULL DEFAULT 0 CHECK(attempt_count>=0),
 cpu_budget_ms INTEGER NOT NULL CHECK(cpu_budget_ms BETWEEN 1 AND 60000),
 byte_budget INTEGER NOT NULL CHECK(byte_budget BETWEEN 1 AND 33554432),
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 last_error TEXT NOT NULL DEFAULT '',
 CHECK((state='LEASED' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL) OR
       (state<>'LEASED' AND lease_owner IS NULL AND lease_until IS NULL)),
 CHECK(state<>'COMPLETED' OR output_hash IS NOT NULL)
);

-- su1_research_task_no_delete
CREATE TRIGGER su1_research_task_no_delete BEFORE DELETE ON su1_research_task
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_research_task_no_replace
CREATE TRIGGER su1_research_task_no_replace BEFORE INSERT ON su1_research_task
WHEN EXISTS(SELECT 1 FROM su1_research_task WHERE task_id=NEW.task_id) OR EXISTS(SELECT 1 FROM su1_research_task WHERE intent_hash=NEW.intent_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_retention_audit_insert
CREATE TRIGGER su1_retention_audit_insert AFTER INSERT ON su1_campaign_audit
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_burst_checkpoint_insert
CREATE TRIGGER su1_retention_burst_checkpoint_insert AFTER INSERT ON su1_burst_checkpoint
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_burst_checkpoint_update
CREATE TRIGGER su1_retention_burst_checkpoint_update AFTER UPDATE ON su1_burst_checkpoint
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_certificate_insert
CREATE TRIGGER su1_retention_certificate_insert AFTER INSERT ON su1_certificate
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_certificate_update
CREATE TRIGGER su1_retention_certificate_update AFTER UPDATE ON su1_certificate
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_clock
CREATE TABLE su1_retention_clock(singleton INTEGER PRIMARY KEY CHECK(singleton=1),generation INTEGER NOT NULL);

-- su1_retention_cohort_runtime_insert
CREATE TRIGGER su1_retention_cohort_runtime_insert AFTER INSERT ON su1_cohort_runtime
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_cohort_runtime_update
CREATE TRIGGER su1_retention_cohort_runtime_update AFTER UPDATE ON su1_cohort_runtime
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_dependency_insert
CREATE TRIGGER su1_retention_dependency_insert AFTER INSERT ON su1_dependency
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_head_insert
CREATE TRIGGER su1_retention_head_insert AFTER INSERT ON su1_mainline_head_event
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_lineage_insert
CREATE TRIGGER su1_retention_lineage_insert AFTER INSERT ON su1_lineage
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_live_target_insert
CREATE TRIGGER su1_retention_live_target_insert AFTER INSERT ON su1_live_target
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_live_target_update
CREATE TRIGGER su1_retention_live_target_update AFTER UPDATE ON su1_live_target
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_pin
CREATE TABLE su1_retention_pin (
 pin_id TEXT NOT NULL PRIMARY KEY CHECK(length(pin_id)=64),
 object_kind TEXT NOT NULL,
 object_id TEXT NOT NULL,
 reason TEXT NOT NULL CHECK(reason IN ('ACTIVE_STRATEGY','OPEN_COHORT','PENDING_EVENT','LEASED_TASK','REPLAY','MAINLINE')),
 created_at INTEGER NOT NULL,
 released_at INTEGER,
 CHECK(released_at IS NULL OR released_at>=created_at)
);

-- su1_retention_pin_no_delete
CREATE TRIGGER su1_retention_pin_no_delete BEFORE DELETE ON su1_retention_pin
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_retention_pin_no_replace
CREATE TRIGGER su1_retention_pin_no_replace BEFORE INSERT ON su1_retention_pin
WHEN EXISTS(SELECT 1 FROM su1_retention_pin WHERE pin_id=NEW.pin_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_retention_prediction_insert
CREATE TRIGGER su1_retention_prediction_insert AFTER INSERT ON su1_prediction
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_prediction_update
CREATE TRIGGER su1_retention_prediction_update AFTER UPDATE ON su1_prediction
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_research_pool_insert
CREATE TRIGGER su1_retention_research_pool_insert AFTER INSERT ON su1_research_pool
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_research_pool_update
CREATE TRIGGER su1_retention_research_pool_update AFTER UPDATE ON su1_research_pool
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_research_task_insert
CREATE TRIGGER su1_retention_research_task_insert AFTER INSERT ON su1_research_task
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_research_task_update
CREATE TRIGGER su1_retention_research_task_update AFTER UPDATE ON su1_research_task
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_revocation_insert
CREATE TRIGGER su1_retention_revocation_insert AFTER INSERT ON su1_certificate_revocation
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_settlement_insert
CREATE TRIGGER su1_retention_settlement_insert AFTER INSERT ON su1_settlement
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_retention_settlement_update
CREATE TRIGGER su1_retention_settlement_update AFTER UPDATE ON su1_settlement
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

-- su1_runtime_checkpoint
CREATE TABLE su1_runtime_checkpoint (
 checkpoint_key TEXT NOT NULL PRIMARY KEY,
 cursor_issue INTEGER NOT NULL DEFAULT 0,
 updated_at INTEGER NOT NULL,
 deferred_count INTEGER NOT NULL DEFAULT 0 CHECK(deferred_count>=0),
 last_error TEXT NOT NULL DEFAULT ''
);

-- su1_runtime_control
CREATE TABLE su1_runtime_control (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 mode TEXT NOT NULL DEFAULT 'DISABLED' CHECK(mode IN ('DISABLED','RESEARCH','SHADOW','MAINLINE_ADAPTER')),
 contract_hash TEXT NOT NULL DEFAULT '',
 updated_at INTEGER NOT NULL DEFAULT 0,
 reason TEXT NOT NULL DEFAULT 'DEFAULT_CLOSED'
);

-- su1_runtime_program
CREATE TABLE su1_runtime_program (
 strategy_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_strategy_version(strategy_id),
 canonical_text TEXT NOT NULL CHECK(length(canonical_text) BETWEEN 3 AND 4096),
 coverage REAL NOT NULL CHECK(coverage>0 AND coverage<=1),
 margin_gate INTEGER NOT NULL CHECK(margin_gate IN (0,1)),
 parent_id TEXT REFERENCES su1_strategy_version(strategy_id),
 created_at INTEGER NOT NULL,
 kernel_hash TEXT NOT NULL DEFAULT '0000000000000000000000000000000000000000000000000000000000000000' CHECK(length(kernel_hash)=64)
);

-- su1_runtime_program_immutable
CREATE TRIGGER su1_runtime_program_immutable BEFORE UPDATE ON su1_runtime_program
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_runtime_program_no_delete
CREATE TRIGGER su1_runtime_program_no_delete BEFORE DELETE ON su1_runtime_program
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_schema_revision
CREATE TABLE su1_schema_revision (
 revision INTEGER PRIMARY KEY CHECK(revision=1),
 contract_name TEXT NOT NULL CHECK(contract_name='SU1_STORAGE_V1')
);

-- su1_sealed_cancelled
CREATE TABLE su1_sealed_cancelled (
 campaign_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_sealed_plan(campaign_id),
 reason TEXT NOT NULL,
 cancelled_at INTEGER NOT NULL
);

-- su1_sealed_cancelled_immutable
CREATE TRIGGER su1_sealed_cancelled_immutable BEFORE UPDATE ON su1_sealed_cancelled
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_sealed_open
CREATE TABLE su1_sealed_open (
 era_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_era(era_id),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 opened_at INTEGER NOT NULL,
 result_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash)
);

-- su1_sealed_open_binding
CREATE TRIGGER su1_sealed_open_binding BEFORE INSERT ON su1_sealed_open
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_era WHERE era_id=NEW.era_id AND kind='SEALED' AND campaign_id=NEW.campaign_id)
 THEN RAISE(ABORT,'SU1_SEALED_OPEN_BINDING') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_sealed_open WHERE campaign_id=NEW.campaign_id)
 THEN RAISE(ABORT,'SU1_ONE_SEALED_OPEN_PER_CAMPAIGN') END;
END;

-- su1_sealed_open_immutable
CREATE TRIGGER su1_sealed_open_immutable BEFORE UPDATE ON su1_sealed_open
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_sealed_open_no_delete
CREATE TRIGGER su1_sealed_open_no_delete BEFORE DELETE ON su1_sealed_open
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_sealed_plan
CREATE TABLE su1_sealed_plan (
 campaign_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_campaign(campaign_id),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 dev_first INTEGER NOT NULL,
 dev_last INTEGER NOT NULL,
 val_first INTEGER NOT NULL,
 val_last INTEGER NOT NULL,
 first_sequence INTEGER NOT NULL,
 last_sequence INTEGER NOT NULL,
 selected_at INTEGER NOT NULL,
 CHECK(dev_first<=dev_last AND dev_last<val_first AND val_first<=val_last AND val_last<first_sequence AND first_sequence<=last_sequence)
);

-- su1_sealed_plan_immutable
CREATE TRIGGER su1_sealed_plan_immutable BEFORE UPDATE ON su1_sealed_plan
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_sealed_result
CREATE TABLE su1_sealed_result (
 plan_campaign TEXT PRIMARY KEY NOT NULL REFERENCES su1_sealed_plan(campaign_id),
 open_campaign TEXT UNIQUE NOT NULL REFERENCES su1_campaign(campaign_id),
 eligibility TEXT NOT NULL CHECK(eligibility IN ('PASS_SCREEN','FAIL_SCREEN')),
 recorded_at INTEGER NOT NULL
);

-- su1_sealed_result_immutable
CREATE TRIGGER su1_sealed_result_immutable BEFORE UPDATE ON su1_sealed_result
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_sealed_reuse_guard
CREATE TRIGGER su1_sealed_reuse_guard BEFORE INSERT ON su1_era
WHEN NEW.kind='SEALED'
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_sealed_open o JOIN su1_era e ON e.era_id=o.era_id
   WHERE NEW.first_sequence<=e.last_sequence AND NEW.last_sequence>=e.first_sequence)
 THEN RAISE(ABORT,'SU1_SEALED_SPAN_ALREADY_OPENED') END;
END;

-- su1_search_attempt
CREATE TABLE su1_search_attempt (
 attempt_id TEXT PRIMARY KEY NOT NULL CHECK(length(attempt_id)=64),
 genome_key TEXT NOT NULL CHECK(length(genome_key)=64),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 program_text TEXT NOT NULL,
 arm TEXT NOT NULL,
 operator TEXT NOT NULL,
 parent_id TEXT,
 dev_fitness REAL,
 val_fitness REAL,
 result TEXT NOT NULL CHECK(result IN ('EVALUATED','REJECTED','YIELD')),
 detail TEXT NOT NULL,
 created_at INTEGER NOT NULL
);

-- su1_search_attempt_immutable
CREATE TRIGGER su1_search_attempt_immutable BEFORE UPDATE ON su1_search_attempt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_settlement
CREATE TABLE su1_settlement (
 settlement_id TEXT NOT NULL PRIMARY KEY CHECK(length(settlement_id)=64),
 prediction_id TEXT NOT NULL UNIQUE REFERENCES su1_prediction(prediction_id),
 target_issue INTEGER NOT NULL,
 canonical_draw_hash TEXT NOT NULL CHECK(length(canonical_draw_hash)=64),
 legacy_settlement_hash TEXT NOT NULL CHECK(length(legacy_settlement_hash)=64),
 actual INTEGER NOT NULL CHECK(actual BETWEEN 1 AND 10),
 hit INTEGER NOT NULL CHECK(hit IN (0,1)),
 label_hash TEXT NOT NULL UNIQUE CHECK(length(label_hash)=64),
 committed_at INTEGER NOT NULL
);

-- su1_settlement_binding
CREATE TRIGGER su1_settlement_binding BEFORE INSERT ON su1_settlement
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_settlement WHERE target_issue=NEW.target_issue
  AND (canonical_draw_hash<>NEW.canonical_draw_hash OR legacy_settlement_hash<>NEW.legacy_settlement_hash))
 THEN RAISE(ABORT,'SU1_TARGET_TERMINAL_CONFLICT') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_prediction p JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash
  JOIN su1_opportunity_outcome o ON o.opportunity_id=p.opportunity_id
  WHERE p.prediction_id=NEW.prediction_id AND p.target_issue=NEW.target_issue
  AND NEW.canonical_draw_hash=o.canonical_draw_hash AND NEW.legacy_settlement_hash=o.legacy_settlement_hash
  AND NEW.committed_at>=o.completed_at
  AND NEW.committed_at>=s.target_open_time
  AND NEW.hit=CASE WHEN NEW.actual IN (p.n1,p.n2,p.n3) THEN 1 ELSE 0 END)
 THEN RAISE(ABORT,'SU1_SETTLEMENT_BINDING') END;
END;

-- su1_settlement_event
CREATE TRIGGER su1_settlement_event AFTER INSERT ON su1_settlement
BEGIN
 DELETE FROM su1_pending_settlement WHERE prediction_id=NEW.prediction_id;
 INSERT INTO su1_outbox(event_id,event_type,prediction_id,settlement_id,source_record_hash,created_at,updated_at)
 VALUES('SETTLED:'||NEW.settlement_id,'SETTLED',NEW.prediction_id,NEW.settlement_id,NEW.label_hash,NEW.committed_at,NEW.committed_at);
END;

-- su1_settlement_immutable
CREATE TRIGGER su1_settlement_immutable BEFORE UPDATE ON su1_settlement
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_settlement_issue
CREATE INDEX su1_settlement_issue ON su1_settlement(target_issue,prediction_id);

-- su1_settlement_no_delete
CREATE TRIGGER su1_settlement_no_delete BEFORE DELETE ON su1_settlement
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_settlement_no_replace
CREATE TRIGGER su1_settlement_no_replace BEFORE INSERT ON su1_settlement
WHEN EXISTS(SELECT 1 FROM su1_settlement WHERE settlement_id=NEW.settlement_id) OR EXISTS(SELECT 1 FROM su1_settlement WHERE prediction_id=NEW.prediction_id) OR EXISTS(SELECT 1 FROM su1_settlement WHERE label_hash=NEW.label_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_snapshot
CREATE TABLE su1_snapshot (
 snapshot_hash TEXT NOT NULL PRIMARY KEY CHECK(length(snapshot_hash)=64),
 game TEXT NOT NULL,
 target_issue INTEGER NOT NULL,
 target_open_time INTEGER NOT NULL,
 source_max_issue INTEGER NOT NULL,
 source_available_at INTEGER NOT NULL,
 captured_at INTEGER NOT NULL,
 source_epoch TEXT NOT NULL,
 source_version_hash TEXT NOT NULL CHECK(length(source_version_hash)=64),
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 CHECK(source_max_issue<target_issue),
 CHECK(source_available_at<=captured_at AND captured_at<target_open_time)
);

-- su1_snapshot_immutable
CREATE TRIGGER su1_snapshot_immutable BEFORE UPDATE ON su1_snapshot
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_snapshot_issue
CREATE INDEX su1_snapshot_issue ON su1_snapshot(target_issue,captured_at);

-- su1_snapshot_no_delete
CREATE TRIGGER su1_snapshot_no_delete BEFORE DELETE ON su1_snapshot
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_snapshot_no_replace
CREATE TRIGGER su1_snapshot_no_replace BEFORE INSERT ON su1_snapshot
WHEN EXISTS(SELECT 1 FROM su1_snapshot WHERE snapshot_hash=NEW.snapshot_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_source_clock
CREATE TABLE su1_source_clock (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 generation INTEGER NOT NULL CHECK(generation>=0)
);

-- su1_strategy_binding
CREATE TRIGGER su1_strategy_binding BEFORE INSERT ON su1_strategy_version
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_model_artifact m WHERE m.artifact_hash=NEW.artifact_hash
  AND m.program_hash=NEW.program_hash AND m.trained_through_sequence<=NEW.created_sequence)
 THEN RAISE(ABORT,'SU1_STRATEGY_ARTIFACT_MISMATCH') END;
END;

-- su1_strategy_version
CREATE TABLE su1_strategy_version (
 strategy_id TEXT NOT NULL PRIMARY KEY CHECK(length(strategy_id)=64),
 codec TEXT NOT NULL CHECK(codec='SU1'),
 program_hash TEXT NOT NULL REFERENCES su1_program(program_hash),
 artifact_hash TEXT NOT NULL REFERENCES su1_model_artifact(artifact_hash),
 selector_hash TEXT NOT NULL CHECK(length(selector_hash)=64),
 calibration_hash TEXT REFERENCES su1_blob(blob_hash),
 calibration_state TEXT NOT NULL CHECK(calibration_state IN ('MISSING','FROZEN')),
 action_contract_hash TEXT NOT NULL CHECK(length(action_contract_hash)=64),
 state_initial_hash TEXT NOT NULL CHECK(length(state_initial_hash)=64),
 created_sequence INTEGER NOT NULL CHECK(created_sequence>=0),
 created_at INTEGER NOT NULL,
 
 
 
 strategy_role TEXT NOT NULL DEFAULT 'CANDIDATE' CHECK(strategy_role IN ('CANDIDATE','NULL_BASELINE','NULL_CONTROL')),
 CHECK((calibration_state='MISSING' AND calibration_hash IS NULL) OR
       (calibration_state='FROZEN' AND calibration_hash IS NOT NULL))
);

-- su1_strategy_version_immutable
CREATE TRIGGER su1_strategy_version_immutable BEFORE UPDATE ON su1_strategy_version
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_strategy_version_no_delete
CREATE TRIGGER su1_strategy_version_no_delete BEFORE DELETE ON su1_strategy_version
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_strategy_version_no_replace
CREATE TRIGGER su1_strategy_version_no_replace BEFORE INSERT ON su1_strategy_version
WHEN EXISTS(SELECT 1 FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- su1_task_identity
CREATE TRIGGER su1_task_identity BEFORE UPDATE ON su1_research_task
WHEN NEW.task_id<>OLD.task_id OR NEW.intent_hash<>OLD.intent_hash OR NEW.feedback_hash IS NOT OLD.feedback_hash
 OR NEW.kind<>OLD.kind OR NEW.input_hash<>OLD.input_hash OR NEW.boundary_sequence<>OLD.boundary_sequence
 OR NEW.cpu_budget_ms<>OLD.cpu_budget_ms OR NEW.byte_budget<>OLD.byte_budget OR NEW.created_at<>OLD.created_at
BEGIN SELECT RAISE(ABORT,'SU1_TASK_IDENTITY_IMMUTABLE'); END;

-- su1_task_pending
CREATE INDEX su1_task_pending ON su1_research_task(state,updated_at);

-- su1_task_transition
CREATE TRIGGER su1_task_transition BEFORE UPDATE ON su1_research_task
BEGIN
 SELECT CASE WHEN NOT ((OLD.state IN ('PENDING','WAITING','LEASED') AND NEW.state='LEASED'
   AND NEW.lease_generation=OLD.lease_generation+1 AND NEW.attempt_count=OLD.attempt_count+1)
  OR (OLD.state='LEASED' AND NEW.state IN ('WAITING','COMPLETED','FAILED','CANCELLED')
   AND NEW.lease_generation=OLD.lease_generation AND NEW.attempt_count=OLD.attempt_count)
  OR (OLD.state IN ('PENDING','WAITING') AND NEW.state='CANCELLED'
   AND NEW.lease_generation=OLD.lease_generation AND NEW.attempt_count=OLD.attempt_count))
 THEN RAISE(ABORT,'SU1_TASK_STATE_TRANSITION') END;
END;

-- su1_train_protocol
CREATE TABLE su1_train_protocol (
 protocol_hash TEXT NOT NULL PRIMARY KEY CHECK(length(protocol_hash)=64),
 program_hash TEXT NOT NULL REFERENCES su1_program(program_hash),
 dataset_hash TEXT NOT NULL CHECK(length(dataset_hash)=64),
 split_hash TEXT NOT NULL CHECK(length(split_hash)=64),
 protocol_json TEXT NOT NULL CHECK(length(CAST(protocol_json AS BLOB))<=65536),
 train_first_sequence INTEGER NOT NULL,
 train_last_sequence INTEGER NOT NULL,
 validation_first_sequence INTEGER NOT NULL,
 validation_last_sequence INTEGER NOT NULL,
 target_contract_hash TEXT NOT NULL CHECK(length(target_contract_hash)=64),
 created_at INTEGER NOT NULL,
 CHECK(train_first_sequence<=train_last_sequence AND train_last_sequence<validation_first_sequence
       AND validation_first_sequence<=validation_last_sequence)
);

-- su1_train_protocol_immutable
CREATE TRIGGER su1_train_protocol_immutable BEFORE UPDATE ON su1_train_protocol
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

-- su1_train_protocol_no_delete
CREATE TRIGGER su1_train_protocol_no_delete BEFORE DELETE ON su1_train_protocol
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- su1_train_protocol_no_replace
CREATE TRIGGER su1_train_protocol_no_replace BEFORE INSERT ON su1_train_protocol
WHEN EXISTS(SELECT 1 FROM su1_train_protocol WHERE protocol_hash=NEW.protocol_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;

-- teacher_assist_profile
CREATE TABLE teacher_assist_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, assist_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, lift REAL DEFAULT 0, confidence REAL DEFAULT 0, conflict_rate REAL DEFAULT 0, prob_adjust REAL DEFAULT 0, gate_adjust REAL DEFAULT 0, sort_adjust REAL DEFAULT 0, status TEXT, summary_text TEXT, updated_at INTEGER DEFAULT 0);

-- teacher_domain_stat_v20
CREATE TABLE teacher_domain_stat_v20 (domain_key TEXT PRIMARY KEY, parent_domain_key TEXT, coarse_domain_key TEXT, status TEXT, version TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, recent_hit_count INTEGER DEFAULT 0, misleading_streak INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, posterior_mean REAL DEFAULT 0, lower_bound REAL DEFAULT 0, upper_bound REAL DEFAULT 1, drift_score REAL DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- teacher_matrix_combo_profile
CREATE TABLE teacher_matrix_combo_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, combo_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_hit INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, confidence REAL DEFAULT 0, combo_score REAL DEFAULT 0, risk_score REAL DEFAULT 0, prob_adjust REAL DEFAULT 0, gate_adjust REAL DEFAULT 0, risk_penalty REAL DEFAULT 0, updated_at INTEGER DEFAULT 0, status TEXT, summary_text TEXT);

-- teacher_matrix_profile
CREATE TABLE teacher_matrix_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, profile_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, positive_count INTEGER DEFAULT 0, negative_count INTEGER DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_positive INTEGER DEFAULT 0, recent_negative INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, confidence REAL DEFAULT 0, position_score REAL DEFAULT 0, number_score REAL DEFAULT 0, state_score REAL DEFAULT 0, trend_score REAL DEFAULT 0, shape_score REAL DEFAULT 0, risk_score REAL DEFAULT 0, prob_adjust REAL DEFAULT 0, gate_adjust REAL DEFAULT 0, risk_penalty REAL DEFAULT 0, teacher_weight REAL DEFAULT 1, updated_at INTEGER DEFAULT 0, last_hit_at INTEGER DEFAULT 0, last_miss_at INTEGER DEFAULT 0, status TEXT, why_text TEXT, summary_text TEXT);

-- teacher_matrix_report
CREATE TABLE teacher_matrix_report (_id INTEGER PRIMARY KEY AUTOINCREMENT, report_id TEXT NOT NULL UNIQUE, task_key TEXT, segment_key TEXT, source_type TEXT, created_at INTEGER DEFAULT 0, started_at INTEGER DEFAULT 0, finished_at INTEGER DEFAULT 0, cost_ms INTEGER DEFAULT 0, window_size INTEGER DEFAULT 0, target_count INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, positive_count INTEGER DEFAULT 0, negative_count INTEGER DEFAULT 0, profile_count INTEGER DEFAULT 0, combo_profile_count INTEGER DEFAULT 0, strong_profile_count INTEGER DEFAULT 0, risk_profile_count INTEGER DEFAULT 0, applied_count INTEGER DEFAULT 0, reason_sample_count INTEGER DEFAULT 0, reason_profile_count INTEGER DEFAULT 0, avg_hit_rate REAL DEFAULT 0, avg_confidence REAL DEFAULT 0, avg_prob_adjust REAL DEFAULT 0, avg_gate_adjust REAL DEFAULT 0, avg_reason_edge REAL DEFAULT 0, status TEXT, summary_text TEXT, error_text TEXT);

-- teacher_matrix_sample
CREATE TABLE teacher_matrix_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE, source_type TEXT, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, target_index INTEGER DEFAULT 0, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, label INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, state_key TEXT, state_mode TEXT, trend_key TEXT, shape_key TEXT, drift_key TEXT, break_key TEXT, recovery_key TEXT, transition_key TEXT, hot_cold_key TEXT, odd_even_key TEXT, high_low_key TEXT, relation_key TEXT, profile_key TEXT, combo_key TEXT, trend_score REAL DEFAULT 0, shape_score REAL DEFAULT 0, drift_risk REAL DEFAULT 0, break_risk REAL DEFAULT 0, recovery_score REAL DEFAULT 0, number_score REAL DEFAULT 0, position_score REAL DEFAULT 0, risk_score REAL DEFAULT 0, confidence REAL DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, reason_fingerprint TEXT, reason_edge REAL DEFAULT 0, hit_similarity REAL DEFAULT 0, miss_similarity REAL DEFAULT 0, reason_level TEXT, why_positive TEXT, why_negative TEXT, why_miss TEXT, next_advice TEXT, main_reason_code TEXT, affected_module TEXT, saved INTEGER DEFAULT 0);

-- teacher_matrix_state_profile
CREATE TABLE teacher_matrix_state_profile (_id INTEGER PRIMARY KEY AUTOINCREMENT, state_profile_key TEXT NOT NULL UNIQUE, state_key TEXT, state_mode TEXT, sample_count INTEGER DEFAULT 0, positive_count INTEGER DEFAULT 0, negative_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_sample INTEGER DEFAULT 0, recent_positive INTEGER DEFAULT 0, recent_hit_rate REAL DEFAULT 0, confidence REAL DEFAULT 0, state_score REAL DEFAULT 0, risk_score REAL DEFAULT 0, prob_adjust REAL DEFAULT 0, gate_adjust REAL DEFAULT 0, updated_at INTEGER DEFAULT 0, status TEXT, summary_text TEXT);

-- teacher_number_matrix
CREATE TABLE teacher_number_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, teacher_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, positive_count INTEGER DEFAULT 0, negative_count INTEGER DEFAULT 0, recent30_sample INTEGER DEFAULT 0, recent30_positive INTEGER DEFAULT 0, recent60_sample INTEGER DEFAULT 0, recent60_positive INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, teacher_edge REAL DEFAULT 0, confidence REAL DEFAULT 0, support_pass INTEGER DEFAULT 0, risk_score REAL DEFAULT 0, life_state TEXT, updated_at INTEGER DEFAULT 0, why_text TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- teacher_replay_case
CREATE TABLE teacher_replay_case (_id INTEGER PRIMARY KEY AUTOINCREMENT, case_key TEXT NOT NULL UNIQUE, source_type TEXT, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, target_index INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, state_key TEXT, state_mode TEXT, shape_key TEXT, trend_key TEXT, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, lift REAL DEFAULT 0, confidence REAL DEFAULT 0, risk_score REAL DEFAULT 0, reason_type TEXT, why_hit TEXT, why_miss TEXT, counterfactual_text TEXT, stability_text TEXT, summary_text TEXT, saved INTEGER DEFAULT 0);

-- teacher_replay_counterfactual
CREATE TABLE teacher_replay_counterfactual (_id INTEGER PRIMARY KEY AUTOINCREMENT, cf_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, chosen_position INTEGER DEFAULT 0, chosen_k3_mask INTEGER DEFAULT 0, better_position INTEGER DEFAULT 0, better_k3_mask INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, miss_type TEXT, summary_text TEXT);

-- teacher_replay_cursor
CREATE TABLE teacher_replay_cursor (_id INTEGER PRIMARY KEY AUTOINCREMENT, cursor_key TEXT NOT NULL UNIQUE, task_type TEXT, window_size INTEGER DEFAULT 0, segment_start_index INTEGER DEFAULT 0, segment_end_index INTEGER DEFAULT 0, next_target_index INTEGER DEFAULT 0, last_target_issue TEXT, used_targets TEXT, status TEXT, progress TEXT, updated_at INTEGER DEFAULT 0);

-- teacher_replay_report
CREATE TABLE teacher_replay_report (_id INTEGER PRIMARY KEY AUTOINCREMENT, report_id TEXT NOT NULL UNIQUE, task_key TEXT, segment_key TEXT, status TEXT, created_at INTEGER DEFAULT 0, started_at INTEGER DEFAULT 0, finished_at INTEGER DEFAULT 0, cost_ms INTEGER DEFAULT 0, target_count INTEGER DEFAULT 0, case_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, counterfactual_count INTEGER DEFAULT 0, stability_count INTEGER DEFAULT 0, teacher_matrix_feed_count INTEGER DEFAULT 0, reason_feed_count INTEGER DEFAULT 0, summary_text TEXT, error_text TEXT);

-- teacher_replay_stability
CREATE TABLE teacher_replay_stability (_id INTEGER PRIMARY KEY AUTOINCREMENT, stability_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, stability_score REAL DEFAULT 0, bad_rate REAL DEFAULT 0, updated_at INTEGER DEFAULT 0, summary_text TEXT);

-- teacher_snapshot
CREATE TABLE teacher_snapshot (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_id TEXT NOT NULL UNIQUE, source_type TEXT, status TEXT, created_at INTEGER DEFAULT 0, started_at INTEGER DEFAULT 0, finished_at INTEGER DEFAULT 0, cost_ms INTEGER DEFAULT 0, window_size INTEGER DEFAULT 0, total_history INTEGER DEFAULT 0, total_steps INTEGER DEFAULT 0, test_count INTEGER DEFAULT 0, start_issue TEXT, end_issue TEXT, bet_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hold_count INTEGER DEFAULT 0, shadow_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, win_rate REAL DEFAULT 0, frequency REAL DEFAULT 0, pnl REAL DEFAULT 0, total_profit REAL DEFAULT 0, max_drawdown REAL DEFAULT 0, avg_profit_per_bet REAL DEFAULT 0, calibration_error REAL DEFAULT 0, brier_score REAL DEFAULT 0, a_plus_count INTEGER DEFAULT 0, a_plus_hit_rate REAL DEFAULT 0, bias_score_avg REAL DEFAULT 0, gate_adjust REAL DEFAULT 0, probability_adjust REAL DEFAULT 0, risk_adjust REAL DEFAULT 0, teacher_confidence REAL DEFAULT 0, stability_score REAL DEFAULT 0, overfit_risk REAL DEFAULT 0, profile_freshness REAL DEFAULT 0, position_weights TEXT, number_weights TEXT, mode_weights TEXT, teacher_matrix_sample_count INTEGER DEFAULT 0, teacher_matrix_profile_count INTEGER DEFAULT 0, teacher_matrix_avg_hit_rate REAL DEFAULT 0, teacher_matrix_avg_confidence REAL DEFAULT 0, teacher_matrix_gate_adjust REAL DEFAULT 0, teacher_matrix_prob_adjust REAL DEFAULT 0, reason_sample_count INTEGER DEFAULT 0, reason_profile_count INTEGER DEFAULT 0, reason_avg_edge REAL DEFAULT 0, reason_avg_confidence REAL DEFAULT 0, elite45_pass_count INTEGER DEFAULT 0, top_profiles TEXT, top_miss_reasons TEXT, top_hit_reasons TEXT, promoted_profiles TEXT, rejected_profiles TEXT, high_win_shape_text TEXT, high_risk_shape_text TEXT, learning_summary TEXT, gate_summary TEXT, probability_summary TEXT, audit_summary TEXT, bias_summary TEXT, promotion_summary TEXT, a_plus_summary TEXT, teacher_matrix_summary TEXT, reason_summary TEXT, elite45_summary TEXT, summary_text TEXT, error_text TEXT);

-- third_guard_matrix
CREATE TABLE third_guard_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, guard_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, main_mask INTEGER DEFAULT 0, guard_number INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, save_count INTEGER DEFAULT 0, drag_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, save_rate REAL DEFAULT 0, drag_rate REAL DEFAULT 0, third_guard_net REAL DEFAULT 0, third_guard_pass INTEGER DEFAULT 0, guard_role TEXT, risk_score REAL DEFAULT 0, updated_at INTEGER DEFAULT 0, summary_text TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, recent_sample_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT);

-- trades
CREATE TABLE trades (_id INTEGER PRIMARY KEY AUTOINCREMENT, target_issue TEXT NOT NULL UNIQUE, base_issue TEXT, bet_time INTEGER DEFAULT 0, created_at INTEGER DEFAULT 0, settle_time INTEGER DEFAULT 0, settled_at INTEGER DEFAULT 0, action TEXT, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, raw_p REAL DEFAULT 0, calibrated_p REAL DEFAULT 0, final_p REAL DEFAULT 0, dynamic_gate REAL DEFAULT 0, edge REAL DEFAULT 0, model_p REAL DEFAULT 0, shape_p REAL DEFAULT 0, combo_p REAL DEFAULT 0, recovery_p REAL DEFAULT 0, similar_p REAL DEFAULT 0, memory_p REAL DEFAULT 0, calibration_error REAL DEFAULT 0, similar_hit_rate REAL DEFAULT 0, combo_hit_rate REAL DEFAULT 0, effective_sample INTEGER DEFAULT 0, combo_sample INTEGER DEFAULT 0, trend_life REAL DEFAULT 0, drift_risk REAL DEFAULT 0, break_depth REAL DEFAULT 0, recovery_score REAL DEFAULT 0, champion_gap REAL DEFAULT 0, combo_gap REAL DEFAULT 0, hot_reverse_risk REAL DEFAULT 0, false_strong_risk REAL DEFAULT 0, state_mode TEXT, shape_key TEXT, state_key TEXT, reason_summary TEXT, result TEXT DEFAULT 'WAIT', actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, pnl REAL DEFAULT 0, miss_reason TEXT, settlement_reason TEXT, settled INTEGER DEFAULT 0, stat_scope TEXT, deep_score REAL DEFAULT 0, deep_confidence REAL DEFAULT 0, deep_feature_p REAL DEFAULT 0, deep_position_score REAL DEFAULT 0, deep_number_score REAL DEFAULT 0, deep_cycle_score REAL DEFAULT 0, deep_transition_score REAL DEFAULT 0, deep_state_score REAL DEFAULT 0, bias_score REAL DEFAULT 0, bias_confidence REAL DEFAULT 0, profile_score REAL DEFAULT 0, fallback_profile_score REAL DEFAULT 0, runtime_profile_score REAL DEFAULT 0, state_number_profile_score REAL DEFAULT 0, teacher_weight REAL DEFAULT 1, state_confidence REAL DEFAULT 0, audit_penalty REAL DEFAULT 0, audit_boost REAL DEFAULT 0, ai_judge_score REAL DEFAULT 0, ai_judge_confidence REAL DEFAULT 0, evidence_score REAL DEFAULT 0, evidence_risk REAL DEFAULT 0, conflict_level REAL DEFAULT 0, a_plus_score REAL DEFAULT 0, promotion_score REAL DEFAULT 0, weakest_number_score REAL DEFAULT 0, balance_score REAL DEFAULT 0, deep_profile_key TEXT, deep_profile_hit_rate REAL DEFAULT 0, deep_profile_sample INTEGER DEFAULT 0, deep_fallback_level INTEGER DEFAULT 0, fallback_profile_key TEXT, promotion_status TEXT, a_plus_status TEXT, candidate_evidence_key TEXT, snapshot_id TEXT, deep_attribution TEXT, deep_reason TEXT, ai_judge_summary TEXT, learning_summary TEXT, self_diagnosis_text TEXT, manual_backtest_support INTEGER DEFAULT 0, attribution_reason_code TEXT, attribution_text TEXT, affected_module TEXT, teacher_matrix_sample INTEGER DEFAULT 0, teacher_matrix_positive INTEGER DEFAULT 0, teacher_matrix_negative INTEGER DEFAULT 0, teacher_matrix_state_sample INTEGER DEFAULT 0, teacher_matrix_combo_sample INTEGER DEFAULT 0, teacher_matrix_hit_rate REAL DEFAULT 0, teacher_matrix_recent_hit_rate REAL DEFAULT 0, teacher_matrix_confidence REAL DEFAULT 0, teacher_matrix_score REAL DEFAULT 0, teacher_matrix_prob_adjust REAL DEFAULT 0, teacher_matrix_gate_adjust REAL DEFAULT 0, teacher_matrix_risk_penalty REAL DEFAULT 0, teacher_matrix_position_score REAL DEFAULT 0, teacher_matrix_number_score REAL DEFAULT 0, teacher_matrix_combo_score REAL DEFAULT 0, teacher_matrix_state_score REAL DEFAULT 0, teacher_matrix_applied INTEGER DEFAULT 0, teacher_matrix_status TEXT, teacher_matrix_profile_key TEXT, teacher_matrix_combo_key TEXT, teacher_matrix_summary TEXT, source_type TEXT, is_realtime_real INTEGER DEFAULT 0, pre_draw_reason_fingerprint TEXT, pre_draw_reason_text TEXT, reason_valid_level TEXT, reason_score REAL DEFAULT 0, reason_edge REAL DEFAULT 0, hit_similarity REAL DEFAULT 0, miss_similarity REAL DEFAULT 0, contradiction_score REAL DEFAULT 0, causal_lift REAL DEFAULT 0, causal_confidence REAL DEFAULT 0, valid_reason_count INTEGER DEFAULT 0, false_reason_count INTEGER DEFAULT 0, forward_reason_sample INTEGER DEFAULT 0, forward_reason_hit_rate REAL DEFAULT 0, reason_wilson_score REAL DEFAULT 0, k3_role_summary TEXT, k3_main_number INTEGER DEFAULT 0, k3_support_number INTEGER DEFAULT 0, k3_defense_number INTEGER DEFAULT 0, combo_role_diversity REAL DEFAULT 0, combo_conflict REAL DEFAULT 0, fingerprint_cooling INTEGER DEFAULT 0, elite45_passed INTEGER DEFAULT 0, elite45_status TEXT, elite45_block_code TEXT, elite45_gate REAL DEFAULT 0, elite45_summary TEXT, v20_grade TEXT, v20_action TEXT, v20_candidate_key TEXT, v20_chain_key TEXT, v20_audit_key TEXT, v20_source_key TEXT, v20_rule_key TEXT, v20_same_class_key TEXT, v20_state_key TEXT, v20_position_key TEXT, v20_teacher_key TEXT, v20_rank_key TEXT, v20_guard_key TEXT, v20_shadow_key TEXT, v20_freeze_key TEXT, v20_seed_key TEXT, v20_final_cause_score REAL DEFAULT 0, v20_support_score REAL DEFAULT 0, v20_counter_score REAL DEFAULT 0, v20_confidence REAL DEFAULT 0, v20_sample_trust REAL DEFAULT 0, v20_recent_effectiveness REAL DEFAULT 0, v20_action_confidence REAL DEFAULT 0, v20_teacher_matrix_score REAL DEFAULT 0, v20_position_edge REAL DEFAULT 0, v20_number_teacher_edge REAL DEFAULT 0, v20_combo_role_edge REAL DEFAULT 0, v20_third_guard_edge REAL DEFAULT 0, v20_rank_counterfactual_edge REAL DEFAULT 0, v20_source_health_edge REAL DEFAULT 0, v20_state_health_edge REAL DEFAULT 0, v20_cycle_health_edge REAL DEFAULT 0, v20_seed_similarity REAL DEFAULT 0, v20_feature_distance REAL DEFAULT 0, v20_exact_state_match INTEGER DEFAULT 0, v20_relaxed_state_match INTEGER DEFAULT 0, v20_timing_offset INTEGER DEFAULT 0, v20_timing_bucket TEXT, v20_shadow_health REAL DEFAULT 0, v20_false_strong_risk REAL DEFAULT 0, v20_rule_decay_risk REAL DEFAULT 0, v20_same_class_lose_risk REAL DEFAULT 0, v20_guard_drag_risk REAL DEFAULT 0, v20_position_wrong_risk REAL DEFAULT 0, v20_position_healthy INTEGER DEFAULT 0, v20_number_teacher_support INTEGER DEFAULT 0, v20_rank_counterfactual_ok INTEGER DEFAULT 0, v20_third_guard_ok INTEGER DEFAULT 0, v20_shadow_health_pass INTEGER DEFAULT 0, v20_false_strong_frozen INTEGER DEFAULT 0, v20_same_class_frozen INTEGER DEFAULT 0, v20_rule_active INTEGER DEFAULT 0, v20_rule_frozen INTEGER DEFAULT 0, v20_source_healthy INTEGER DEFAULT 0, v20_state_healthy INTEGER DEFAULT 0, v20_reverse_reason_code TEXT, v20_action_suggestion TEXT, v20_evidence_json TEXT, v20_counter_evidence_json TEXT, v20_reason_atoms TEXT, v20_source_votes TEXT, v20_risk_tags TEXT, v20_decision_reason TEXT, v20_hold_reason TEXT, v20_miss_reason TEXT, dynamic_cluster_id TEXT, dynamic_cluster_status TEXT, dynamic_cluster_prototype_key TEXT, dynamic_cluster_feature_key TEXT, dynamic_cluster_sample INTEGER DEFAULT 0, dynamic_cluster_hit_count INTEGER DEFAULT 0, dynamic_cluster_miss_count INTEGER DEFAULT 0, dynamic_cluster_recent_sample INTEGER DEFAULT 0, dynamic_cluster_recent_hit_count INTEGER DEFAULT 0, dynamic_cluster_hit_rate REAL DEFAULT 0, dynamic_cluster_recent_hit_rate REAL DEFAULT 0, dynamic_cluster_confidence REAL DEFAULT 0, dynamic_cluster_distance REAL DEFAULT 0, dynamic_cluster_volatility REAL DEFAULT 0, dynamic_cluster_max_lose INTEGER DEFAULT 0, dynamic_cluster_counter_score REAL DEFAULT 0, dynamic_cluster_version TEXT, dynamic_cluster_reason TEXT, position_need_key TEXT, position_need_summary TEXT, position_need_strength REAL DEFAULT 0, position_need_chaos_risk REAL DEFAULT 0, position_need_repair_score REAL DEFAULT 0, position_need_relay_score REAL DEFAULT 0, number_supply_key TEXT, number_supply_summary TEXT, number_supply_strength REAL DEFAULT 0, number_supply_risk REAL DEFAULT 0, need_supply_match_key TEXT, need_supply_match_score REAL DEFAULT 0, need_supply_third_quality REAL DEFAULT 0, need_supply_role_diversity REAL DEFAULT 0, need_supply_risk REAL DEFAULT 0, need_supply_summary TEXT, k3_role_main TEXT, k3_role_assist TEXT, k3_role_defense TEXT, k3_role_main_number INTEGER DEFAULT 0, k3_role_assist_number INTEGER DEFAULT 0, k3_role_defense_number INTEGER DEFAULT 0, k3_role_structure TEXT, counterfactual_reason_key TEXT, counterfactual_rank INTEGER DEFAULT 0, counterfactual_rank_score REAL DEFAULT 0, counterfactual_why_not_risk REAL DEFAULT 0, counterfactual_why_not TEXT, counterfactual_summary TEXT, cause_ablation_key TEXT, cause_ablation_score REAL DEFAULT 0, cause_ablation_false_cause_count INTEGER DEFAULT 0, cause_ablation_summary TEXT, miss_reason_type TEXT, miss_reason_matrix_key TEXT, miss_reason_detail TEXT, adaptive_judge_candidate_count INTEGER DEFAULT 0, adaptive_judge_status TEXT, adaptive_judge_action TEXT, adaptive_judge_domain TEXT, adaptive_cluster_hit_rate REAL DEFAULT 0, adaptive_cluster_recent_hit_rate REAL DEFAULT 0, adaptive_cluster_sample INTEGER DEFAULT 0, adaptive_cluster_confidence REAL DEFAULT 0, adaptive_cause_score REAL DEFAULT 0, adaptive_counter_score REAL DEFAULT 0, adaptive_promote_score REAL DEFAULT 0, adaptive_freeze_score REAL DEFAULT 0, adaptive_judge_reason TEXT, adaptive_judge_version TEXT, promotion_cluster_status TEXT, promotion_cluster_hit_rate REAL DEFAULT 0, promotion_cluster_recent_hit_rate REAL DEFAULT 0, promotion_cluster_sample INTEGER DEFAULT 0, promotion_cluster_reason TEXT, why_bet TEXT, why_no_bet TEXT, why_hit TEXT, why_miss TEXT, dynamic_causal_summary TEXT, snapshot_key TEXT, candidate_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, window_state_key TEXT, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, raw_rank_score REAL DEFAULT 0, calibrated_hit_probability REAL DEFAULT 0, contract_decision_id TEXT, candidate_hash TEXT, snapshot_max_issue TEXT, trained_until_issue TEXT, build_id TEXT, cohort_id TEXT, schema_version TEXT, feature_schema_version TEXT, ranking_version TEXT, calibration_version TEXT, gate_policy_version TEXT, pipeline_version TEXT, contract_valid INTEGER DEFAULT 0, contract_status TEXT, coarse_family_key TEXT, mid_family_key TEXT, fine_family_key TEXT, position_family_key TEXT, posterior_matched_level TEXT, posterior_family_key TEXT, posterior_policy TEXT, posterior_version TEXT, posterior_sample INTEGER DEFAULT 0, posterior_hit INTEGER DEFAULT 0, posterior_mean REAL DEFAULT 0, posterior_wilson_lower REAL DEFAULT 0, posterior_recent_rate REAL DEFAULT 0, posterior_drift REAL DEFAULT 0, family_wilson_lower REAL DEFAULT 0, calibration_bucket INTEGER DEFAULT 0, calibration_sample INTEGER DEFAULT 0, calibration_total_sample INTEGER DEFAULT 0, calibration_ready INTEGER DEFAULT 0, calibration_status TEXT, calibration_brier REAL DEFAULT 0, authorization_id TEXT, authorization_valid INTEGER DEFAULT 0, authorized_mode TEXT, authorization_reject_code TEXT, authorized_at INTEGER DEFAULT 0, authorization_probability REAL DEFAULT 0, authorization_gate REAL DEFAULT 0, authorization_edge REAL DEFAULT 0, teacher_policy TEXT, dynamic_policy TEXT, adaptive_policy TEXT, micro_walk_forward_status TEXT, micro_walk_forward_version TEXT, micro_walk_forward_as_of_issue TEXT, micro_walk_forward_sample INTEGER DEFAULT 0, honest_probability REAL DEFAULT 0, action_utility REAL DEFAULT 0, action_structure_score REAL DEFAULT 0, action_discrimination_score REAL DEFAULT 0, action_maturity_score REAL DEFAULT 0, action_uncertainty_penalty REAL DEFAULT 0, action_frequency_support REAL DEFAULT 0, action_utility_version TEXT, lane_candidate TEXT, oos_rank_slot INTEGER DEFAULT 0, oos_candidate_count INTEGER DEFAULT 0, v42_accept_score REAL DEFAULT 0, v42_family_lcb REAL DEFAULT 0, v42_regime_quality REAL DEFAULT 0, v42_association_score REAL DEFAULT 0, v42_distillation_risk REAL DEFAULT 0, v42_support_votes INTEGER DEFAULT 0, v42_regime_family TEXT, v42_risk_coverage_mode TEXT, v42_primary_reject TEXT, v42_secondary_reject TEXT, v42_tertiary_reject TEXT, v42_lane_policy_version TEXT, v43_strategy_support REAL DEFAULT 0, v43_strategy_risk REAL DEFAULT 0, v43_strategy_confidence REAL DEFAULT 0, v43_strategy_diversity REAL DEFAULT 0, v43_strategy_maturity REAL DEFAULT 0, v43_safe_probability REAL DEFAULT 0, v43_probability_residual REAL DEFAULT 0, v43_ablation_fragility REAL DEFAULT 0, v43_probability_lock REAL DEFAULT 0, v43_evidence_group_count INTEGER DEFAULT 0, v43_hard_risk INTEGER DEFAULT 0, v43_dominant_evidence TEXT, v43_missing_evidence TEXT, v43_evidence_fingerprint TEXT, v43_probability_lock_hash TEXT, v43_pipeline_stage TEXT, v43_invariant_status TEXT, v43_strategy_version TEXT, v46_event_probability REAL DEFAULT 0, v46_action_lcb REAL DEFAULT 0, v46_probability_uncertainty REAL DEFAULT 0, v46_expected_value REAL DEFAULT 0, v46_global_calibrated_probability REAL DEFAULT 0, v46_position_adjustment REAL DEFAULT 0, v46_rank_adjustment REAL DEFAULT 0, v46_regime_adjustment REAL DEFAULT 0, v46_calibration_confidence REAL DEFAULT 0, v46_final_rank INTEGER DEFAULT 0, v46_formal_candidate_count INTEGER DEFAULT 0, v46_version TEXT, v46_number_distribution_summary TEXT, v46_probability_trace TEXT, v46_root_cause_code TEXT, v46_root_cause_owner TEXT, v46_final_contract_hash TEXT, v46_lane TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- trg_b03_action_event_no_delete
CREATE TRIGGER trg_b03_action_event_no_delete BEFORE DELETE ON b03_action_event WHEN (length(OLD.target_issue)<>12 OR OLD.target_issue>=COALESCE((SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 119),'')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='b03_action_event' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END;

-- trg_b03_action_event_no_update
CREATE TRIGGER trg_b03_action_event_no_update BEFORE UPDATE ON b03_action_event BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_UPDATE'); END;

-- trg_b03_authorization_event_no_delete
CREATE TRIGGER trg_b03_authorization_event_no_delete BEFORE DELETE ON b03_authorization_event WHEN (length(OLD.target_issue)<>12 OR OLD.target_issue>=COALESCE((SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999),'')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='b03_authorization_event' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END;

-- trg_b03_authorization_event_no_update
CREATE TRIGGER trg_b03_authorization_event_no_update BEFORE UPDATE ON b03_authorization_event BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_UPDATE'); END;

-- trg_b03_draw_quarantine_no_delete
CREATE TRIGGER trg_b03_draw_quarantine_no_delete BEFORE DELETE ON b03_draw_quarantine WHEN (length(OLD.source_issue)<>12 OR OLD.source_issue>=COALESCE((SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999),'')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='b03_draw_quarantine' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END;

-- trg_b03_draw_quarantine_no_update
CREATE TRIGGER trg_b03_draw_quarantine_no_update BEFORE UPDATE ON b03_draw_quarantine WHEN OLD.source_database<>NEW.source_database OR OLD.source_game_code<>NEW.source_game_code OR OLD.source_table<>NEW.source_table OR OLD.source_issue<>NEW.source_issue OR OLD.source_numbers<>NEW.source_numbers OR OLD.row_hash<>NEW.row_hash BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_SOURCE'); END;

-- trg_b03_number_event_no_delete
CREATE TRIGGER trg_b03_number_event_no_delete BEFORE DELETE ON b03_number_event WHEN (length(OLD.target_issue)<>12 OR OLD.target_issue>=COALESCE((SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 119),'')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='b03_number_event' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END;

-- trg_b03_number_event_no_update
CREATE TRIGGER trg_b03_number_event_no_update BEFORE UPDATE ON b03_number_event BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_UPDATE'); END;

-- trg_b03_permutation_event_no_delete
CREATE TRIGGER trg_b03_permutation_event_no_delete BEFORE DELETE ON b03_permutation_event WHEN (length(OLD.target_issue)<>12 OR OLD.target_issue>=COALESCE((SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 119),'')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='b03_permutation_event' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END;

-- trg_b03_permutation_event_no_update
CREATE TRIGGER trg_b03_permutation_event_no_update BEFORE UPDATE ON b03_permutation_event BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_UPDATE'); END;

-- trg_b03_prediction_bundle_no_delete
CREATE TRIGGER trg_b03_prediction_bundle_no_delete BEFORE DELETE ON b03_prediction_bundle WHEN (length(OLD.target_issue)<>12 OR OLD.target_issue>=COALESCE((SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999),'')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='b03_prediction_bundle' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END;

-- trg_b03_prediction_bundle_no_update
CREATE TRIGGER trg_b03_prediction_bundle_no_update BEFORE UPDATE ON b03_prediction_bundle BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_UPDATE'); END;

-- trg_b03_selected_event_no_delete
CREATE TRIGGER trg_b03_selected_event_no_delete BEFORE DELETE ON b03_selected_event WHEN (length(OLD.target_issue)<>12 OR OLD.target_issue>=COALESCE((SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999),'')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='b03_selected_event' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END;

-- trg_b03_selected_event_no_update
CREATE TRIGGER trg_b03_selected_event_no_update BEFORE UPDATE ON b03_selected_event BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_UPDATE'); END;

-- trg_b03_settlement_event_no_delete
CREATE TRIGGER trg_b03_settlement_event_no_delete BEFORE DELETE ON b03_settlement_event WHEN (length(OLD.target_issue)<>12 OR OLD.target_issue>=COALESCE((SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999),'')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='b03_settlement_event' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_DELETE_WITHIN_RETENTION'); END;

-- trg_b03_settlement_event_no_update
CREATE TRIGGER trg_b03_settlement_event_no_update BEFORE UPDATE ON b03_settlement_event BEGIN SELECT RAISE(ABORT,'B03_IMMUTABLE_UPDATE'); END;

-- trg_r926_runtime_event_no_delete
CREATE TRIGGER trg_r926_runtime_event_no_delete BEFORE DELETE ON v57_runtime_event_r926  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_runtime_event_r926' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R926_RUNTIME_EVENT_APPEND_ONLY'); END;

-- trg_r926_runtime_event_no_update
CREATE TRIGGER trg_r926_runtime_event_no_update BEFORE UPDATE ON v57_runtime_event_r926 BEGIN SELECT RAISE(ABORT,'R926_RUNTIME_EVENT_APPEND_ONLY'); END;

-- trg_r927_e02c_ftrl_evidence_delete_freeze
CREATE TRIGGER trg_r927_e02c_ftrl_evidence_delete_freeze BEFORE DELETE ON v57_selected_evidence_r906 WHEN (EXISTS(SELECT 1 FROM v57_runtime_event_r926 WHERE game_code='Binance1-PK10' AND target_issue=OLD.target_issue AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE' AND source='ResearchAdaptiveLearnerStoreR811')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_selected_evidence_r906' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END;

-- trg_r927_e02c_ftrl_evidence_insert_freeze
CREATE TRIGGER trg_r927_e02c_ftrl_evidence_insert_freeze BEFORE INSERT ON v57_selected_evidence_r906 WHEN EXISTS(SELECT 1 FROM v57_runtime_event_r926 WHERE game_code='Binance1-PK10' AND target_issue=NEW.target_issue AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE' AND source='ResearchAdaptiveLearnerStoreR811') BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END;

-- trg_r927_e02c_ftrl_evidence_update_freeze
CREATE TRIGGER trg_r927_e02c_ftrl_evidence_update_freeze BEFORE UPDATE OF target_issue,selected_position,raw_action_p_value,rank_score_value,third_margin_value,entropy_risk_value,stability_value,window_consensus_value,raw_action_p_status,rank_status,third_margin_status,entropy_risk_status,stability_status,window_consensus_status ON v57_selected_evidence_r906 WHEN EXISTS(SELECT 1 FROM v57_runtime_event_r926 WHERE game_code='Binance1-PK10' AND target_issue=OLD.target_issue AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE' AND source='ResearchAdaptiveLearnerStoreR811') BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END;

-- trg_r927_e02c_ftrl_oos_delete_freeze
CREATE TRIGGER trg_r927_e02c_ftrl_oos_delete_freeze BEFORE DELETE ON v57_oos_label_r35 WHEN (EXISTS(SELECT 1 FROM v57_runtime_event_r926 WHERE game_code='Binance1-PK10' AND target_issue=OLD.target_issue AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE' AND source='ResearchAdaptiveLearnerStoreR811')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_oos_label_r35' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'E02C_FTRL_LABEL_FROZEN_AFTER_LEARN'); END;

-- trg_r927_e02c_ftrl_oos_update_freeze
CREATE TRIGGER trg_r927_e02c_ftrl_oos_update_freeze BEFORE UPDATE OF target_issue,strict_oos,prediction_created_at,draw_open_time,hit_label,selected_position,selected_hit_p ON v57_oos_label_r35 WHEN EXISTS(SELECT 1 FROM v57_runtime_event_r926 WHERE game_code='Binance1-PK10' AND target_issue=OLD.target_issue AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE' AND source='ResearchAdaptiveLearnerStoreR811') BEGIN SELECT RAISE(ABORT,'E02C_FTRL_LABEL_FROZEN_AFTER_LEARN'); END;

-- trg_r927_e02c_tiny_label_delete_freeze
CREATE TRIGGER trg_r927_e02c_tiny_label_delete_freeze BEFORE DELETE ON v57_research_compact_label_r906 WHEN (OLD.asset_id='PRODUCTION_SELECTED' AND EXISTS(SELECT 1 FROM v57_runtime_event_r926 WHERE game_code='Binance1-PK10' AND target_issue=OLD.target_issue AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE' AND source='ResearchTinyDeepLearnerR909')) AND NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_research_compact_label_r906' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'E02C_TINY_LABEL_FROZEN_AFTER_LEARN'); END;

-- trg_r927_e02c_tiny_label_update_freeze
CREATE TRIGGER trg_r927_e02c_tiny_label_update_freeze BEFORE UPDATE OF asset_id,target_issue,feature_blob,hit_label ON v57_research_compact_label_r906 WHEN OLD.asset_id='PRODUCTION_SELECTED' AND EXISTS(SELECT 1 FROM v57_runtime_event_r926 WHERE game_code='Binance1-PK10' AND target_issue=OLD.target_issue AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE' AND source='ResearchTinyDeepLearnerR909') BEGIN SELECT RAISE(ABORT,'E02C_TINY_LABEL_FROZEN_AFTER_LEARN'); END;

-- trg_r930_ablation_no_delete
CREATE TRIGGER trg_r930_ablation_no_delete BEFORE DELETE ON v57_batch34_ablation_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_ablation_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_ablation_no_update
CREATE TRIGGER trg_r930_ablation_no_update BEFORE UPDATE ON v57_batch34_ablation_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_atom_timing_no_delete
CREATE TRIGGER trg_r930_atom_timing_no_delete BEFORE DELETE ON v57_batch34_atom_timing_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_atom_timing_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_atom_timing_no_update
CREATE TRIGGER trg_r930_atom_timing_no_update BEFORE UPDATE ON v57_batch34_atom_timing_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_atom_value_no_delete
CREATE TRIGGER trg_r930_atom_value_no_delete BEFORE DELETE ON v57_batch34_atom_value_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_atom_value_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_atom_value_no_update
CREATE TRIGGER trg_r930_atom_value_no_update BEFORE UPDATE ON v57_batch34_atom_value_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_bundle_identity_immutable
CREATE TRIGGER trg_r930_bundle_identity_immutable BEFORE UPDATE OF bundle_sha256,previous_bundle_sha256,contract_sha256,rollback_group,created_at ON v57_batch34_bundle_r930 BEGIN SELECT RAISE(ABORT,'R930_BUNDLE_IDENTITY_IMMUTABLE'); END;

-- trg_r930_capture_transition_no_delete
CREATE TRIGGER trg_r930_capture_transition_no_delete BEFORE DELETE ON v57_batch34_capture_transition_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_capture_transition_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_capture_transition_no_update
CREATE TRIGGER trg_r930_capture_transition_no_update BEFORE UPDATE ON v57_batch34_capture_transition_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_evidence_link_no_delete
CREATE TRIGGER trg_r930_evidence_link_no_delete BEFORE DELETE ON v57_batch34_evidence_link_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_evidence_link_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_evidence_link_no_update
CREATE TRIGGER trg_r930_evidence_link_no_update BEFORE UPDATE ON v57_batch34_evidence_link_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_failure_case_no_delete
CREATE TRIGGER trg_r930_failure_case_no_delete BEFORE DELETE ON v57_batch34_failure_case_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_failure_case_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_failure_case_no_update
CREATE TRIGGER trg_r930_failure_case_no_update BEFORE UPDATE ON v57_batch34_failure_case_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_family_member_no_delete
CREATE TRIGGER trg_r930_family_member_no_delete BEFORE DELETE ON v57_batch34_family_member_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_family_member_no_update
CREATE TRIGGER trg_r930_family_member_no_update BEFORE UPDATE ON v57_batch34_family_member_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_genome_edge_no_delete
CREATE TRIGGER trg_r930_genome_edge_no_delete BEFORE DELETE ON v57_batch34_genome_edge_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_genome_edge_no_update
CREATE TRIGGER trg_r930_genome_edge_no_update BEFORE UPDATE ON v57_batch34_genome_edge_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_genome_no_delete
CREATE TRIGGER trg_r930_genome_no_delete BEFORE DELETE ON v57_batch34_genome_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_genome_no_update
CREATE TRIGGER trg_r930_genome_no_update BEFORE UPDATE ON v57_batch34_genome_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_hypothesis_identity_immutable
CREATE TRIGGER trg_r930_hypothesis_identity_immutable BEFORE UPDATE OF family,global_index,alpha,window_id,reserved_at ON v57_batch34_hypothesis_r930 BEGIN SELECT RAISE(ABORT,'R930_HYPOTHESIS_IDENTITY_IMMUTABLE'); END;

-- trg_r930_lifecycle_event_no_delete
CREATE TRIGGER trg_r930_lifecycle_event_no_delete BEFORE DELETE ON v57_batch34_lifecycle_event_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_lifecycle_event_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_lifecycle_event_no_update
CREATE TRIGGER trg_r930_lifecycle_event_no_update BEFORE UPDATE ON v57_batch34_lifecycle_event_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_lifecycle_identity_immutable
CREATE TRIGGER trg_r930_lifecycle_identity_immutable BEFORE UPDATE OF parent_algorithm_id,genome_sha256,created_at ON v57_batch34_lifecycle_r930 BEGIN SELECT RAISE(ABORT,'R930_LIFECYCLE_IDENTITY_IMMUTABLE'); END;

-- trg_r930_module_event_no_delete
CREATE TRIGGER trg_r930_module_event_no_delete BEFORE DELETE ON v57_batch34_module_event_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_module_event_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_module_event_no_update
CREATE TRIGGER trg_r930_module_event_no_update BEFORE UPDATE ON v57_batch34_module_event_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_paired_observation_no_delete
CREATE TRIGGER trg_r930_paired_observation_no_delete BEFORE DELETE ON v57_batch34_paired_observation_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_paired_observation_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_paired_observation_no_update
CREATE TRIGGER trg_r930_paired_observation_no_update BEFORE UPDATE ON v57_batch34_paired_observation_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_policy_decision_no_delete
CREATE TRIGGER trg_r930_policy_decision_no_delete BEFORE DELETE ON v57_batch34_policy_decision_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_policy_decision_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_policy_decision_no_update
CREATE TRIGGER trg_r930_policy_decision_no_update BEFORE UPDATE ON v57_batch34_policy_decision_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_policy_metric_no_delete
CREATE TRIGGER trg_r930_policy_metric_no_delete BEFORE DELETE ON v57_batch34_policy_metric_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_policy_metric_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_policy_metric_no_update
CREATE TRIGGER trg_r930_policy_metric_no_update BEFORE UPDATE ON v57_batch34_policy_metric_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_policy_outcome_no_delete
CREATE TRIGGER trg_r930_policy_outcome_no_delete BEFORE DELETE ON v57_batch34_policy_outcome_r930  WHEN NOT EXISTS(SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id WHERE p.source_table='v57_batch34_policy_outcome_r930' AND p.source_rowid=OLD.rowid AND a.state IN ('VERIFIED','REFERENCED','PRUNED')) BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END;

-- trg_r930_policy_outcome_no_update
CREATE TRIGGER trg_r930_policy_outcome_no_update BEFORE UPDATE ON v57_batch34_policy_outcome_r930 BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END;

-- trg_r930_window_identity_immutable
CREATE TRIGGER trg_r930_window_identity_immutable BEFORE UPDATE OF candidate_sha256,champion_sha256,policy_sha256,first_issue,last_issue,planned_issues,declared_at ON v57_batch34_paired_window_r930 BEGIN SELECT RAISE(ABORT,'R930_WINDOW_IDENTITY_IMMUTABLE'); END;

-- ui_text_state
CREATE TABLE ui_text_state (_id INTEGER PRIMARY KEY AUTOINCREMENT, text_key TEXT NOT NULL UNIQUE, expanded INTEGER DEFAULT 0, scroll_y INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- uq_r930_one_active_bundle
CREATE UNIQUE INDEX uq_r930_one_active_bundle ON v57_batch34_bundle_r930(active) WHERE active=1;

-- uq_v51_issue_game_target
CREATE UNIQUE INDEX uq_v51_issue_game_target ON v51_issue_prediction(game_code,target_issue);

-- v20_action_matrix
CREATE TABLE v20_action_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, action_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, candidate_key TEXT, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, k3_legal INTEGER DEFAULT 0, grade TEXT, action TEXT, action_confidence REAL DEFAULT 0, bet_now_pass INTEGER DEFAULT 0, hold_reason TEXT, shadow_reason TEXT, freeze_reason TEXT);

-- v20_background_cursor
CREATE TABLE v20_background_cursor (task_key TEXT PRIMARY KEY, cursor_issue TEXT, target_issue TEXT, status TEXT, version TEXT, processed_count INTEGER DEFAULT 0, pending_count INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- v20_cause_matrix
CREATE TABLE v20_cause_matrix (_id INTEGER PRIMARY KEY AUTOINCREMENT, cause_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, reason_atom TEXT, support_sample INTEGER DEFAULT 0, support_hit INTEGER DEFAULT 0, counter_sample INTEGER DEFAULT 0, counter_hit INTEGER DEFAULT 0, support_score REAL DEFAULT 0, counter_score REAL DEFAULT 0, lift REAL DEFAULT 0, confidence REAL DEFAULT 0, sample_trust REAL DEFAULT 0, recent_effectiveness REAL DEFAULT 0, decay_risk REAL DEFAULT 0, action_suggestion TEXT, updated_at INTEGER DEFAULT 0, summary_text TEXT);

-- v20_closed_loop_snapshot
CREATE TABLE v20_closed_loop_snapshot (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, settled_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, action TEXT, stat_scope TEXT, source_type TEXT, is_realtime_real INTEGER DEFAULT 0, result TEXT DEFAULT 'WAIT', actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, pnl REAL DEFAULT 0, window_state_key TEXT, cause_key TEXT, teacher_key TEXT, similar_key TEXT, reverse_key TEXT, action_key TEXT, decision_audit_key TEXT, closed_loop_text TEXT, dynamic_cluster_id TEXT, dynamic_cluster_status TEXT, dynamic_cluster_prototype_key TEXT, dynamic_cluster_feature_key TEXT, dynamic_cluster_sample INTEGER DEFAULT 0, dynamic_cluster_hit_count INTEGER DEFAULT 0, dynamic_cluster_miss_count INTEGER DEFAULT 0, dynamic_cluster_recent_sample INTEGER DEFAULT 0, dynamic_cluster_recent_hit_count INTEGER DEFAULT 0, dynamic_cluster_hit_rate REAL DEFAULT 0, dynamic_cluster_recent_hit_rate REAL DEFAULT 0, dynamic_cluster_confidence REAL DEFAULT 0, dynamic_cluster_distance REAL DEFAULT 0, dynamic_cluster_volatility REAL DEFAULT 0, dynamic_cluster_max_lose INTEGER DEFAULT 0, dynamic_cluster_counter_score REAL DEFAULT 0, dynamic_cluster_version TEXT, dynamic_cluster_reason TEXT, position_need_key TEXT, position_need_summary TEXT, position_need_strength REAL DEFAULT 0, position_need_chaos_risk REAL DEFAULT 0, position_need_repair_score REAL DEFAULT 0, position_need_relay_score REAL DEFAULT 0, number_supply_key TEXT, number_supply_summary TEXT, number_supply_strength REAL DEFAULT 0, number_supply_risk REAL DEFAULT 0, need_supply_match_key TEXT, need_supply_match_score REAL DEFAULT 0, need_supply_third_quality REAL DEFAULT 0, need_supply_role_diversity REAL DEFAULT 0, need_supply_risk REAL DEFAULT 0, need_supply_summary TEXT, k3_role_main TEXT, k3_role_assist TEXT, k3_role_defense TEXT, k3_role_main_number INTEGER DEFAULT 0, k3_role_assist_number INTEGER DEFAULT 0, k3_role_defense_number INTEGER DEFAULT 0, k3_role_structure TEXT, counterfactual_reason_key TEXT, counterfactual_rank INTEGER DEFAULT 0, counterfactual_rank_score REAL DEFAULT 0, counterfactual_why_not_risk REAL DEFAULT 0, counterfactual_why_not TEXT, counterfactual_summary TEXT, cause_ablation_key TEXT, cause_ablation_score REAL DEFAULT 0, cause_ablation_false_cause_count INTEGER DEFAULT 0, cause_ablation_summary TEXT, miss_reason_type TEXT, miss_reason_matrix_key TEXT, miss_reason_detail TEXT, adaptive_judge_candidate_count INTEGER DEFAULT 0, adaptive_judge_status TEXT, adaptive_judge_action TEXT, adaptive_judge_domain TEXT, adaptive_cluster_hit_rate REAL DEFAULT 0, adaptive_cluster_recent_hit_rate REAL DEFAULT 0, adaptive_cluster_sample INTEGER DEFAULT 0, adaptive_cluster_confidence REAL DEFAULT 0, adaptive_cause_score REAL DEFAULT 0, adaptive_counter_score REAL DEFAULT 0, adaptive_promote_score REAL DEFAULT 0, adaptive_freeze_score REAL DEFAULT 0, adaptive_judge_reason TEXT, adaptive_judge_version TEXT, promotion_cluster_status TEXT, promotion_cluster_hit_rate REAL DEFAULT 0, promotion_cluster_recent_hit_rate REAL DEFAULT 0, promotion_cluster_sample INTEGER DEFAULT 0, promotion_cluster_reason TEXT, why_bet TEXT, why_no_bet TEXT, why_hit TEXT, why_miss TEXT, dynamic_causal_summary TEXT, candidate_key TEXT, candidate_rank INTEGER DEFAULT 0, realtime_top40_rank INTEGER DEFAULT 0, adaptive_input_flag INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, dynamic_window_state_key TEXT, final_decision_window_state_key TEXT, probability_window_state_key TEXT, cluster_key TEXT, factor_key TEXT, position_need_score REAL DEFAULT 0, number_supply_score REAL DEFAULT 0, coverage_quality REAL DEFAULT 0, third_quality REAL DEFAULT 0, counter_evidence_score REAL DEFAULT 0, similar_sample_count INTEGER DEFAULT 0, similar_hit_rate REAL DEFAULT 0, teacher_status TEXT, teacher_status_class TEXT, teacher_number_status TEXT, teacher_sample_reliability REAL DEFAULT 0, teacher_recent_validity REAL DEFAULT 0, teacher_evidence_contribution REAL DEFAULT 0, teacher_counter_contribution REAL DEFAULT 0, v20_status TEXT, v20_action_advice TEXT, v20_sample_reliability REAL DEFAULT 0, v20_recent_validity REAL DEFAULT 0, v20_evidence_contribution REAL DEFAULT 0, v20_counter_contribution REAL DEFAULT 0, adaptive_status TEXT, shadow_promotion_status TEXT, final_p_before_diagnosis REAL DEFAULT 0, final_p_after_diagnosis REAL DEFAULT 0, gate REAL DEFAULT 0, edge REAL DEFAULT 0, reject_code TEXT, real_probe_allowed INTEGER DEFAULT 0, real_probe_reason TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, predecision_opportunity INTEGER DEFAULT 0, risk_severity TEXT, hard_block_flag INTEGER DEFAULT 0, hard_block_source TEXT, evidence_source TEXT, evidence_settled_only INTEGER DEFAULT 0, evidence_sample_count INTEGER DEFAULT 0, evidence_hit_count INTEGER DEFAULT 0, evidence_miss_count INTEGER DEFAULT 0, promotion_coarse_domain TEXT, promotion_mid_domain TEXT, promotion_fine_domain TEXT, bet_source_engine TEXT, decision_gate_version TEXT, field_chain_version TEXT, schema_fix_version TEXT, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- v20_false_strong
CREATE TABLE v20_false_strong (_id INTEGER PRIMARY KEY AUTOINCREMENT, false_key TEXT NOT NULL UNIQUE, state_key TEXT, same_class_key TEXT, position INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, consecutive_miss INTEGER DEFAULT 0, false_strong_risk REAL DEFAULT 0, freeze_level INTEGER DEFAULT 0, freeze_until_issue TEXT, is_frozen INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- v20_frequency_state
CREATE TABLE v20_frequency_state (state_key TEXT PRIMARY KEY, as_of_issue TEXT, status TEXT, version TEXT, frequency_20 REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt REAL DEFAULT 0, valid_issue_count INTEGER DEFAULT 0, real_bet_count INTEGER DEFAULT 0, qualified_opportunity_count INTEGER DEFAULT 0, recent_probe_count INTEGER DEFAULT 0, recent_lose_count INTEGER DEFAULT 0, debt_frozen INTEGER DEFAULT 0, required_floor_bet_count INTEGER DEFAULT 0, target_bet_count INTEGER DEFAULT 0, floor_debt_count INTEGER DEFAULT 0, target_debt_count INTEGER DEFAULT 0, no_upper_cap INTEGER DEFAULT 1, observed_20 INTEGER DEFAULT 0, observed_50 INTEGER DEFAULT 0, observed_100 INTEGER DEFAULT 0, observed_300 INTEGER DEFAULT 0, bet_count_20 INTEGER DEFAULT 0, bet_count_50 INTEGER DEFAULT 0, bet_count_100 INTEGER DEFAULT 0, bet_count_300 INTEGER DEFAULT 0, floor_debt_20 INTEGER DEFAULT 0, floor_debt_50 INTEGER DEFAULT 0, floor_debt_100 INTEGER DEFAULT 0, floor_debt_300 INTEGER DEFAULT 0, target_debt_20 INTEGER DEFAULT 0, target_debt_50 INTEGER DEFAULT 0, target_debt_100 INTEGER DEFAULT 0, target_debt_300 INTEGER DEFAULT 0, qualified_floor_candidate_count INTEGER DEFAULT 0, urgent_window TEXT, issues_since_last_real_bet INTEGER DEFAULT 999, issues_since_last_floor_bet INTEGER DEFAULT 999, consecutive_floor_bets INTEGER DEFAULT 0, floor_pacing_min_gap INTEGER DEFAULT 2147483647, floor_pacing_ready INTEGER DEFAULT 0, floor_pacing_reason TEXT, updated_at INTEGER DEFAULT 0);

-- v20_layer_snapshot
CREATE TABLE v20_layer_snapshot (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE, layer TEXT, as_of_issue TEXT, max_included_issue TEXT, schema_version TEXT, strategy_version TEXT, freshness_status TEXT, payload TEXT, sample_count INTEGER DEFAULT 0, created_at INTEGER DEFAULT 0, expires_at INTEGER DEFAULT 0);

-- v20_link_candidate_snapshot
CREATE TABLE v20_link_candidate_snapshot (_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE, base_gate REAL DEFAULT 0, risk_adjusted_gate REAL DEFAULT 0, frequency_gate_adjust REAL DEFAULT 0, effective_probe_gate REAL DEFAULT 0, raw_edge REAL DEFAULT 0, effective_probe_edge REAL DEFAULT 0, frequency_50 REAL DEFAULT 0, frequency_100 REAL DEFAULT 0, frequency_300 REAL DEFAULT 0, frequency_debt_score REAL DEFAULT 0, frequency_support_score REAL DEFAULT 0, qualified_probe_score REAL DEFAULT 0, shadow_promotion_score REAL DEFAULT 0, need_supply_score REAL DEFAULT 0, counter_risk REAL DEFAULT 0, qualified_probe INTEGER DEFAULT 0, real_trade INTEGER DEFAULT 0, coverage_available INTEGER DEFAULT 0, third_quality_available INTEGER DEFAULT 0, promotion_domain_available INTEGER DEFAULT 0, frequency_state TEXT, frequency_state_version TEXT, shadow_promotion_domain TEXT, decision_path TEXT, bet_domain TEXT, probe_reject_reason TEXT, probe_eligibility_version TEXT, teacher_as_of_issue TEXT, teacher_issue_lag INTEGER DEFAULT 0, teacher_fresh_by_issue INTEGER DEFAULT 0, teacher_snapshot_created_at INTEGER DEFAULT 0, teacher_number_history_sample INTEGER DEFAULT 0, teacher_domain_shadow_sample INTEGER DEFAULT 0, teacher_domain_real_sample INTEGER DEFAULT 0, teacher_apply_error TEXT, predecision_domain_version TEXT, teacher_runtime_apply_version TEXT, block_tier TEXT, hard_risk_code TEXT, observe_reason TEXT, probe_eligible INTEGER DEFAULT 0, shadow_probe_eligible INTEGER DEFAULT 0, probe_level TEXT, shadow_settled_count INTEGER DEFAULT 0, shadow_domain_sample INTEGER DEFAULT 0, promotion_wilson_lower_bound REAL DEFAULT 0, promotion_eligibility_reason TEXT, rule_health_status TEXT, snapshot_freshness TEXT, warm_profile_freshness TEXT, cold_profile_freshness TEXT, deep_profile_freshness TEXT, decision_version_v2 TEXT, evidence_as_of_issue TEXT, snapshot_as_of_issue TEXT, snapshot_created_at INTEGER DEFAULT 0, frequency_state_updated_at INTEGER DEFAULT 0, frequency_floor_required_count INTEGER DEFAULT 0, frequency_floor_target_count INTEGER DEFAULT 0, frequency_floor_actual_count INTEGER DEFAULT 0, frequency_floor_debt_count INTEGER DEFAULT 0, frequency_target_debt_count INTEGER DEFAULT 0, frequency_floor_mandatory INTEGER DEFAULT 0, frequency_floor_no_upper_cap INTEGER DEFAULT 1, frequency_floor_rank_score REAL DEFAULT 0, frequency_floor_version TEXT, real_bet_frequency_20 REAL DEFAULT 0, frequency_observed_20 INTEGER DEFAULT 0, frequency_observed_50 INTEGER DEFAULT 0, frequency_observed_100 INTEGER DEFAULT 0, frequency_observed_300 INTEGER DEFAULT 0, frequency_bet_count_20 INTEGER DEFAULT 0, frequency_bet_count_50 INTEGER DEFAULT 0, frequency_bet_count_100 INTEGER DEFAULT 0, frequency_bet_count_300 INTEGER DEFAULT 0, frequency_debt_20 INTEGER DEFAULT 0, frequency_debt_50 INTEGER DEFAULT 0, frequency_debt_100 INTEGER DEFAULT 0, frequency_debt_300 INTEGER DEFAULT 0, frequency_target_debt_20 INTEGER DEFAULT 0, frequency_target_debt_50 INTEGER DEFAULT 0, frequency_target_debt_100 INTEGER DEFAULT 0, frequency_target_debt_300 INTEGER DEFAULT 0, frequency_urgent_window TEXT, frequency_issues_since_last_real_bet INTEGER DEFAULT 999, frequency_issues_since_last_floor_bet INTEGER DEFAULT 999, frequency_consecutive_floor_bets INTEGER DEFAULT 0, frequency_floor_min_gap INTEGER DEFAULT 2147483647, frequency_floor_pacing_ready INTEGER DEFAULT 0, frequency_floor_pacing_reason TEXT, decision_quality_path TEXT, evidence_domain_frozen INTEGER DEFAULT 0, evidence_domain_frozen_issue TEXT, evidence_domain_frozen_at INTEGER DEFAULT 0, dynamic_matched_level TEXT, dynamic_real_sample INTEGER DEFAULT 0, dynamic_shadow_sample INTEGER DEFAULT 0, module_build_version TEXT);

-- v20_module_registry
CREATE TABLE v20_module_registry (module_name TEXT PRIMARY KEY, version TEXT, cohort TEXT, status TEXT, detail TEXT, registered_at INTEGER DEFAULT 0);

-- v20_reverse_reason_sample
CREATE TABLE v20_reverse_reason_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, reverse_key TEXT NOT NULL UNIQUE, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, state_key TEXT, reason_code TEXT, early1_risk REAL DEFAULT 0, early2_risk REAL DEFAULT 0, false_strong_risk REAL DEFAULT 0, rank_wrong_risk REAL DEFAULT 0, teacher_decay_risk REAL DEFAULT 0, rule_decay_risk REAL DEFAULT 0, sample_weak_risk REAL DEFAULT 0, noise_score REAL DEFAULT 0, position_wrong_risk REAL DEFAULT 0, number_cover_wrong_risk REAL DEFAULT 0, reverse_text TEXT);

-- v20_rule_health
CREATE TABLE v20_rule_health (_id INTEGER PRIMARY KEY AUTOINCREMENT, rule_key TEXT NOT NULL UNIQUE, state_key TEXT, rule_name TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, current_lose INTEGER DEFAULT 0, max_lose INTEGER DEFAULT 0, edge_decay_risk REAL DEFAULT 0, is_active INTEGER DEFAULT 0, is_frozen INTEGER DEFAULT 0, freeze_until_issue TEXT, updated_at INTEGER DEFAULT 0);

-- v20_shadow_domain_stat
CREATE TABLE v20_shadow_domain_stat (_id INTEGER PRIMARY KEY AUTOINCREMENT, domain_key TEXT NOT NULL UNIQUE, parent_domain_key TEXT, promotion_status TEXT, version TEXT, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, recent_hit_count INTEGER DEFAULT 0, misleading_streak INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, posterior_mean REAL DEFAULT 0, wilson_lower_bound REAL DEFAULT 0, drift_score REAL DEFAULT 0, last_settled_issue TEXT, updated_at INTEGER DEFAULT 0);

-- v20_shadow_health
CREATE TABLE v20_shadow_health (_id INTEGER PRIMARY KEY AUTOINCREMENT, shadow_key TEXT NOT NULL UNIQUE, state_key TEXT, position INTEGER DEFAULT 0, grade TEXT, sample30 INTEGER DEFAULT 0, hit30 INTEGER DEFAULT 0, sample60 INTEGER DEFAULT 0, hit60 INTEGER DEFAULT 0, sample120 INTEGER DEFAULT 0, hit120 INTEGER DEFAULT 0, hit_rate30 REAL DEFAULT 0, hit_rate60 REAL DEFAULT 0, hit_rate120 REAL DEFAULT 0, current_shadow_lose INTEGER DEFAULT 0, shadow_health_score REAL DEFAULT 0, promotion_pass INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- v20_similar_window
CREATE TABLE v20_similar_window (_id INTEGER PRIMARY KEY AUTOINCREMENT, similar_key TEXT NOT NULL UNIQUE, target_issue TEXT, matched_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, state_key TEXT, exact_state_key TEXT, relaxed_state_key TEXT, feature_distance REAL DEFAULT 0, exact_match INTEGER DEFAULT 0, relaxed_match INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, confidence REAL DEFAULT 0, summary_text TEXT);

-- v20_teacher_number
CREATE TABLE v20_teacher_number (_id INTEGER PRIMARY KEY AUTOINCREMENT, teacher_key TEXT NOT NULL UNIQUE, state_key TEXT, profile_key TEXT, position INTEGER DEFAULT 0, number INTEGER DEFAULT 0, sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, recent30_sample INTEGER DEFAULT 0, recent30_hit INTEGER DEFAULT 0, recent60_sample INTEGER DEFAULT 0, recent60_hit INTEGER DEFAULT 0, recent120_sample INTEGER DEFAULT 0, recent120_hit INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, teacher_edge REAL DEFAULT 0, confidence REAL DEFAULT 0, support_score REAL DEFAULT 0, risk_score REAL DEFAULT 0, teacher_status TEXT DEFAULT 'EMPTY', why_hit_text TEXT, why_miss_text TEXT, updated_at INTEGER DEFAULT 0);

-- v20_window_state
CREATE TABLE v20_window_state (_id INTEGER PRIMARY KEY AUTOINCREMENT, state_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, window_len INTEGER DEFAULT 0, window_start_issue TEXT, window_end_issue TEXT, exact_state_key TEXT, relaxed_state_key TEXT, hot_slope REAL DEFAULT 0, cold_slope REAL DEFAULT 0, miss_bucket INTEGER DEFAULT 0, density_score REAL DEFAULT 0, repeat_score REAL DEFAULT 0, odd_even_bias REAL DEFAULT 0, big_small_bias REAL DEFAULT 0, neighbor_score REAL DEFAULT 0, same_tail_score REAL DEFAULT 0, position_crowd_score REAL DEFAULT 0, cross_position_sync_score REAL DEFAULT 0, squeeze_score REAL DEFAULT 0, recent_lose_chain INTEGER DEFAULT 0, timing_offset INTEGER DEFAULT 0, timing_bucket TEXT, feature_distance REAL DEFAULT 0, sample_trust REAL DEFAULT 0, feature_json TEXT, summary_text TEXT);

-- v48_diagnostic_observation
CREATE TABLE v48_diagnostic_observation (_id INTEGER PRIMARY KEY AUTOINCREMENT, target_issue TEXT NOT NULL UNIQUE, base_issue TEXT, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, event_p REAL DEFAULT 0, action_lcb REAL DEFAULT 0, model_probability REAL DEFAULT 0, initial_rank INTEGER DEFAULT 0, teacher_rank INTEGER DEFAULT 0, dynamic_rank INTEGER DEFAULT 0, calibrated_rank INTEGER DEFAULT 0, final_rank INTEGER DEFAULT 0, build_id TEXT, cohort_id TEXT, schema_version TEXT, pipeline_version TEXT, model_version TEXT, calibration_version TEXT, ranking_version TEXT, identity_status TEXT, champion_status TEXT, action_status TEXT, action_block_code TEXT, root_code TEXT, root_detail TEXT, created_at INTEGER DEFAULT 0, settled INTEGER DEFAULT 0, actual_number INTEGER DEFAULT 0, hit INTEGER DEFAULT 0, result TEXT, settled_at INTEGER DEFAULT 0);

-- v48_model_artifact
CREATE TABLE v48_model_artifact (model_id TEXT PRIMARY KEY, artifact_version TEXT, feature_version TEXT, payload TEXT, checksum TEXT, promoted INTEGER DEFAULT 0, updated_at INTEGER DEFAULT 0);

-- v49_best_opportunity
CREATE TABLE v49_best_opportunity (target_issue TEXT PRIMARY KEY,score REAL,action_p REAL,lane TEXT,created_at INTEGER);

-- v49_cause_attribution
CREATE TABLE v49_cause_attribution (_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue TEXT,position INTEGER,result TEXT,primary_cause TEXT,secondary_cause TEXT,counter_evidence TEXT,ablation_blob TEXT,responsible_module TEXT,created_at INTEGER,version TEXT);

-- v49_dirichlet_calibration
CREATE TABLE v49_dirichlet_calibration (position INTEGER PRIMARY KEY,a_diag TEXT,a_off REAL,bias TEXT,sample INTEGER DEFAULT 0,updated_at INTEGER,version TEXT);

-- v49_expert_stats
CREATE TABLE v49_expert_stats (expert_key TEXT PRIMARY KEY,expert_name TEXT,family TEXT,position INTEGER,sample INTEGER DEFAULT 0,logloss_sum REAL DEFAULT 0,brier_sum REAL DEFAULT 0,top3_hit INTEGER DEFAULT 0,recent_quality REAL DEFAULT 0.5,updated_at INTEGER);

-- v49_position_action
CREATE TABLE v49_position_action (_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,action_p REAL,opportunity REAL,agreement REAL,uncertainty REAL,family TEXT,lifecycle TEXT,switch_risk REAL,family_strength REAL,lane TEXT,contract_hash TEXT,settled INTEGER DEFAULT 0,hit INTEGER DEFAULT 0,UNIQUE(target_issue,position) ON CONFLICT REPLACE);

-- v49_predraw_snapshot
CREATE TABLE v49_predraw_snapshot (target_issue TEXT PRIMARY KEY,base_issue TEXT,created_at INTEGER,feature_checksum TEXT,state_checksum TEXT,raw_probability_blob TEXT,calibrated_probability_blob TEXT,expert_blob TEXT,top3_blob TEXT,opportunity_blob TEXT,lane TEXT,selected_position INTEGER,selected_action_p REAL,selected_score REAL,primary_cause TEXT,secondary_cause TEXT,counter_evidence TEXT,contract_hash TEXT,settled INTEGER DEFAULT 0,actual_blob TEXT,position_hit_count INTEGER DEFAULT 0,selected_hit INTEGER DEFAULT 0,attribution TEXT,version TEXT);

-- v49_teacher_posterior
CREATE TABLE v49_teacher_posterior (teacher_key TEXT PRIMARY KEY,family TEXT,lifecycle TEXT,position INTEGER,number INTEGER,sample INTEGER DEFAULT 0,hit INTEGER DEFAULT 0,recent_sample REAL DEFAULT 0,recent_hit REAL DEFAULT 0,updated_at INTEGER);

-- v49_ui_cache
CREATE TABLE v49_ui_cache (cache_key TEXT PRIMARY KEY,content TEXT,content_hash TEXT,updated_at INTEGER);

-- v50_authorization_audit
CREATE TABLE v50_authorization_audit (decision_id TEXT PRIMARY KEY,target_issue TEXT NOT NULL,contract_hash TEXT NOT NULL,authorization_signature TEXT NOT NULL,authorization_owner TEXT NOT NULL,strategy_cohort TEXT NOT NULL,probability_owner TEXT NOT NULL,rank_owner TEXT NOT NULL,lane_owner TEXT NOT NULL,bet_owner TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v50_cause_attribution
CREATE TABLE v50_cause_attribution (target_issue TEXT PRIMARY KEY,attribution_type TEXT NOT NULL,actual_number INTEGER NOT NULL,selected_position INTEGER NOT NULL,selected_hit INTEGER NOT NULL,ablation_blob TEXT NOT NULL,responsibility_blob TEXT NOT NULL,prediction_contract_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v50_domain_certificate
CREATE TABLE v50_domain_certificate (domain_id TEXT PRIMARY KEY,parent_prototype TEXT NOT NULL,lifecycle TEXT NOT NULL,position_scope TEXT NOT NULL,prototype_vector TEXT NOT NULL,sample INTEGER NOT NULL DEFAULT 0,recent_sample INTEGER NOT NULL DEFAULT 0,hit_count INTEGER NOT NULL DEFAULT 0,recent_hit_count INTEGER NOT NULL DEFAULT 0,block0_sample INTEGER NOT NULL DEFAULT 0,block0_hits INTEGER NOT NULL DEFAULT 0,block1_sample INTEGER NOT NULL DEFAULT 0,block1_hits INTEGER NOT NULL DEFAULT 0,block2_sample INTEGER NOT NULL DEFAULT 0,block2_hits INTEGER NOT NULL DEFAULT 0,block3_sample INTEGER NOT NULL DEFAULT 0,block3_hits INTEGER NOT NULL DEFAULT 0,rank1_sample INTEGER NOT NULL DEFAULT 0,rank1_hits INTEGER NOT NULL DEFAULT 0,calibration_error REAL NOT NULL DEFAULT 1,max_lose_streak INTEGER NOT NULL DEFAULT 0,current_lose_streak INTEGER NOT NULL DEFAULT 0,cause_validity REAL NOT NULL DEFAULT 0,counter_failure REAL NOT NULL DEFAULT 0,transition_validity REAL NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'DISCOVERED',updated_at INTEGER NOT NULL);

-- v50_expert_regret
CREATE TABLE v50_expert_regret (expert_id TEXT NOT NULL,position INTEGER NOT NULL,state_family TEXT NOT NULL,lifecycle TEXT NOT NULL,domain_id TEXT NOT NULL,uncertainty_bucket TEXT NOT NULL,sample INTEGER NOT NULL DEFAULT 0,log_loss_sum REAL NOT NULL DEFAULT 0,brier_sum REAL NOT NULL DEFAULT 0,top3_hit INTEGER NOT NULL DEFAULT 0,rank_contribution REAL NOT NULL DEFAULT 0,calibration_error REAL NOT NULL DEFAULT 0,recent_loss REAL NOT NULL DEFAULT 0,mislead_run INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'WATCH',updated_at INTEGER NOT NULL,PRIMARY KEY(expert_id,position,state_family,lifecycle,domain_id,uncertainty_bucket));

-- v50_issue_prediction
CREATE TABLE v50_issue_prediction (target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,history_max_issue TEXT NOT NULL,created_at INTEGER NOT NULL,history_hash TEXT NOT NULL,feature_hash TEXT NOT NULL,state_hash TEXT NOT NULL,expert_hash TEXT NOT NULL,number_probability_hash TEXT NOT NULL,action_hash TEXT NOT NULL,model_hash TEXT NOT NULL,cohort_id TEXT NOT NULL,build_id TEXT NOT NULL,strategy_version TEXT NOT NULL,feature_schema TEXT NOT NULL,state_schema TEXT NOT NULL,expert_schema TEXT NOT NULL,action_schema TEXT NOT NULL,number_probability_blob TEXT NOT NULL,action_blob TEXT NOT NULL,hypothesis_blob TEXT NOT NULL,selected_position INTEGER DEFAULT 0,lane TEXT NOT NULL,authorization_state TEXT NOT NULL,contract_hash TEXT NOT NULL,settled INTEGER NOT NULL DEFAULT 0,settled_at INTEGER DEFAULT 0,actual_blob TEXT NOT NULL DEFAULT '',learning_status TEXT NOT NULL DEFAULT '');

-- v50_ledger_error
CREATE TABLE v50_ledger_error (_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue TEXT,failure_code TEXT,owner TEXT,detail TEXT,created_at INTEGER,resolved INTEGER DEFAULT 0);

-- v50_migration_log
CREATE TABLE v50_migration_log (_id INTEGER PRIMARY KEY AUTOINCREMENT,from_version INTEGER,to_version INTEGER,schema_checksum TEXT,status TEXT,error_code TEXT,error_detail TEXT,created_at INTEGER);

-- v50_number_event
CREATE TABLE v50_number_event (target_issue TEXT NOT NULL,position INTEGER NOT NULL,number INTEGER NOT NULL,predicted_p REAL NOT NULL,actual_label INTEGER DEFAULT -1,number_rank INTEGER NOT NULL,expert_contribution TEXT NOT NULL,teacher_contribution TEXT NOT NULL,state_domain TEXT NOT NULL,feature_hash TEXT NOT NULL,probability_hash TEXT NOT NULL,settled INTEGER NOT NULL DEFAULT 0,settled_at INTEGER DEFAULT 0,PRIMARY KEY(target_issue,position,number));

-- v50_online_residual
CREATE TABLE v50_online_residual (domain_id TEXT NOT NULL,lifecycle TEXT NOT NULL,position INTEGER NOT NULL,number INTEGER NOT NULL,sample INTEGER NOT NULL DEFAULT 0,delta REAL NOT NULL DEFAULT 0,mislead_run INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL,PRIMARY KEY(domain_id,lifecycle,position,number));

-- v50_performance_span
CREATE TABLE v50_performance_span (trace_id TEXT NOT NULL,span_id TEXT NOT NULL,parent_span_id TEXT NOT NULL,target_issue TEXT NOT NULL,owner TEXT NOT NULL,started_at_nanos INTEGER NOT NULL,inclusive_nanos INTEGER NOT NULL,exclusive_nanos INTEGER NOT NULL,success INTEGER NOT NULL,error_code TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(trace_id,span_id));

-- v50_position_action
CREATE TABLE v50_position_action (target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,raw_action_p REAL NOT NULL,third_p REAL NOT NULL,third_margin REAL NOT NULL,entropy REAL NOT NULL,effective_consensus REAL NOT NULL,rank_score REAL NOT NULL,rank_adjusted_p REAL NOT NULL,action_rank INTEGER NOT NULL,lifecycle TEXT NOT NULL,state_domain TEXT NOT NULL,fingerprint TEXT NOT NULL,action_hash TEXT NOT NULL,actual_hit INTEGER DEFAULT -1,settled INTEGER DEFAULT 0,settled_at INTEGER DEFAULT 0,PRIMARY KEY(target_issue,position));

-- v50_schema_meta
CREATE TABLE v50_schema_meta (meta_key TEXT PRIMARY KEY,meta_value TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- v50_selected_decision
CREATE TABLE v50_selected_decision (target_issue TEXT PRIMARY KEY,decision_id TEXT NOT NULL,selected_position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,raw_action_p REAL NOT NULL,rank_adjusted_p REAL NOT NULL,selected_hit_p REAL NOT NULL,uncertainty REAL NOT NULL,domain_status TEXT NOT NULL,lane TEXT NOT NULL,decision_action TEXT NOT NULL,primary_cause TEXT NOT NULL,secondary_cause TEXT NOT NULL,third_cause TEXT NOT NULL,counter_evidence TEXT NOT NULL,necessary_condition TEXT NOT NULL,falsifier_condition TEXT NOT NULL,contract_hash TEXT NOT NULL,authorization_signature TEXT NOT NULL,authorization_owner TEXT NOT NULL,strategy_cohort TEXT NOT NULL,probability_owner TEXT NOT NULL,rank_owner TEXT NOT NULL,lane_owner TEXT NOT NULL,real_trade INTEGER NOT NULL DEFAULT 0,settled INTEGER NOT NULL DEFAULT 0,result TEXT NOT NULL DEFAULT '',pnl REAL NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,settled_at INTEGER DEFAULT 0);

-- v50_state_history
CREATE TABLE v50_state_history (target_issue TEXT NOT NULL,position INTEGER NOT NULL,prototype TEXT NOT NULL,lifecycle TEXT NOT NULL,domain_id TEXT NOT NULL,coarse_run INTEGER NOT NULL,medium_run INTEGER NOT NULL,fine_run INTEGER NOT NULL,hazard REAL NOT NULL,transition_risk REAL NOT NULL,confidence REAL NOT NULL,state_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,position));

-- v50_teacher_profile
CREATE TABLE v50_teacher_profile (evidence_level TEXT NOT NULL,state_domain TEXT NOT NULL,lifecycle TEXT NOT NULL,position INTEGER NOT NULL,number INTEGER NOT NULL,sample INTEGER NOT NULL DEFAULT 0,hits INTEGER NOT NULL DEFAULT 0,recent_sample INTEGER NOT NULL DEFAULT 0,recent_hits INTEGER NOT NULL DEFAULT 0,oos_sample INTEGER NOT NULL DEFAULT 0,real_sample INTEGER NOT NULL DEFAULT 0,shadow_sample INTEGER NOT NULL DEFAULT 0,block0_sample INTEGER NOT NULL DEFAULT 0,block0_hits INTEGER NOT NULL DEFAULT 0,block1_sample INTEGER NOT NULL DEFAULT 0,block1_hits INTEGER NOT NULL DEFAULT 0,block2_sample INTEGER NOT NULL DEFAULT 0,block2_hits INTEGER NOT NULL DEFAULT 0,block3_sample INTEGER NOT NULL DEFAULT 0,block3_hits INTEGER NOT NULL DEFAULT 0,mislead_count INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL,PRIMARY KEY(evidence_level,state_domain,lifecycle,position,number));

-- v51_decision_authorization
CREATE TABLE v51_decision_authorization(decision_id TEXT PRIMARY KEY,target_issue TEXT NOT NULL,contract_hash TEXT NOT NULL,artifact_bundle_hash TEXT NOT NULL,cohort_id TEXT NOT NULL,probability_owner TEXT NOT NULL,rank_owner TEXT NOT NULL,lane_owner TEXT NOT NULL,authorization_hash TEXT NOT NULL,created_at INTEGER NOT NULL, game_code TEXT NOT NULL DEFAULT 'Binance1-PK10', bundle_hash TEXT NOT NULL DEFAULT '', record_hash TEXT NOT NULL DEFAULT '');

-- v51_issue_prediction
CREATE TABLE v51_issue_prediction(target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,history_hash TEXT NOT NULL,feature_hash TEXT NOT NULL,state_hash TEXT NOT NULL,expert_hash TEXT NOT NULL,artifact_bundle_hash TEXT NOT NULL,cohort_id TEXT NOT NULL,status TEXT NOT NULL,created_at INTEGER NOT NULL,settled INTEGER NOT NULL DEFAULT 0,actual_permutation TEXT DEFAULT '',result TEXT DEFAULT '', game_code TEXT NOT NULL DEFAULT 'Binance1-PK10', bundle_hash TEXT NOT NULL DEFAULT '', contract_hash TEXT NOT NULL DEFAULT '', record_hash TEXT NOT NULL DEFAULT '');

-- v51_number_event
CREATE TABLE v51_number_event(target_issue TEXT NOT NULL,position INTEGER NOT NULL,number INTEGER NOT NULL,raw_score REAL NOT NULL,number_p REAL NOT NULL,number_rank INTEGER NOT NULL,state_id TEXT NOT NULL,domain_id TEXT NOT NULL,actual_label INTEGER NOT NULL DEFAULT -1,expert_hash TEXT NOT NULL, game_code TEXT NOT NULL DEFAULT 'Binance1-PK10', bundle_hash TEXT NOT NULL DEFAULT '', contract_hash TEXT NOT NULL DEFAULT '', record_hash TEXT NOT NULL DEFAULT '',PRIMARY KEY(target_issue,position,number));

-- v51_performance_span
CREATE TABLE v51_performance_span(_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue TEXT NOT NULL,trace_id TEXT NOT NULL,span_name TEXT NOT NULL,parent_name TEXT NOT NULL,elapsed_ms INTEGER NOT NULL,exclusive_ms INTEGER NOT NULL,success INTEGER NOT NULL,failure_owner TEXT NOT NULL,created_at INTEGER NOT NULL, game_code TEXT NOT NULL DEFAULT 'Binance1-PK10', bundle_hash TEXT NOT NULL DEFAULT '', record_hash TEXT NOT NULL DEFAULT '');

-- v51_permutation_matrix
CREATE TABLE v51_permutation_matrix(target_issue TEXT PRIMARY KEY,matrix_blob TEXT NOT NULL,row_error REAL NOT NULL,column_error REAL NOT NULL,iterations INTEGER NOT NULL,converged INTEGER NOT NULL,used_fallback INTEGER NOT NULL,map_blob TEXT NOT NULL,matrix_hash TEXT NOT NULL, game_code TEXT NOT NULL DEFAULT 'Binance1-PK10', bundle_hash TEXT NOT NULL DEFAULT '', contract_hash TEXT NOT NULL DEFAULT '', record_hash TEXT NOT NULL DEFAULT '');

-- v51_position_action
CREATE TABLE v51_position_action(target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT NULL,raw_action_p REAL NOT NULL,rank_score REAL NOT NULL,selected_hit_p REAL NOT NULL DEFAULT 0,rank_no INTEGER NOT NULL,hit_label INTEGER NOT NULL DEFAULT -1,fingerprint TEXT NOT NULL,meta_blob TEXT NOT NULL,action_hash TEXT NOT NULL, game_code TEXT NOT NULL DEFAULT 'Binance1-PK10', bundle_hash TEXT NOT NULL DEFAULT '', contract_hash TEXT NOT NULL DEFAULT '', record_hash TEXT NOT NULL DEFAULT '',PRIMARY KEY(target_issue,position));

-- v51_selected_action
CREATE TABLE v51_selected_action(target_issue TEXT PRIMARY KEY,decision_id TEXT NOT NULL UNIQUE,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_hit_p REAL NOT NULL,predictability REAL NOT NULL,lane TEXT NOT NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT NULL,contract_hash TEXT NOT NULL,authorization_hash TEXT NOT NULL,settled INTEGER NOT NULL DEFAULT 0,result TEXT DEFAULT '', game_code TEXT NOT NULL DEFAULT 'Binance1-PK10', domain_id TEXT NOT NULL DEFAULT '', rank_no INTEGER NOT NULL DEFAULT 0, bundle_hash TEXT NOT NULL DEFAULT '', record_hash TEXT NOT NULL DEFAULT '', settlement_hash TEXT NOT NULL DEFAULT '', raw_action_p REAL NOT NULL DEFAULT -1, rank_score REAL NOT NULL DEFAULT 0, third_margin REAL NOT NULL DEFAULT 0, entropy REAL NOT NULL DEFAULT -1, stability REAL NOT NULL DEFAULT -1, window_consensus REAL NOT NULL DEFAULT -1, assignment_support REAL NOT NULL DEFAULT -1, shape_lifecycle TEXT NOT NULL DEFAULT 'UNKNOWN', shape_prototype TEXT NOT NULL DEFAULT '', shape_run_length INTEGER NOT NULL DEFAULT 0, shape_change_risk REAL NOT NULL DEFAULT -1, shape_confidence REAL NOT NULL DEFAULT -1);

-- v57_algorithm_genome_r908
CREATE TABLE v57_algorithm_genome_r908 ( genome_id TEXT PRIMARY KEY, display_name_zh TEXT NOT NULL, genome_spec TEXT NOT NULL, structural_hash TEXT NOT NULL UNIQUE, behavior_hash TEXT NOT NULL DEFAULT '', generation INTEGER NOT NULL, parent_ids TEXT NOT NULL DEFAULT '', trained_until_issue TEXT NOT NULL DEFAULT '', earliest_use_issue TEXT NOT NULL DEFAULT '', dev_n INTEGER NOT NULL DEFAULT 0, dev_hit INTEGER NOT NULL DEFAULT 0, dev_hit_rate REAL, dev_wilson_lower REAL, dev_block_min REAL, dev_max_miss INTEGER NOT NULL DEFAULT 0, novelty REAL NOT NULL DEFAULT 0, complexity REAL NOT NULL DEFAULT 0, state_blob TEXT NOT NULL, state_updates INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL, may_authorize_real INTEGER NOT NULL DEFAULT 0 CHECK(may_authorize_real=0), created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL , future_n INTEGER NOT NULL DEFAULT 0, future_hit INTEGER NOT NULL DEFAULT 0, future_hit_rate REAL, future_wilson_lower REAL, future_last_issue TEXT NOT NULL DEFAULT '', future_evidence_hash TEXT NOT NULL DEFAULT '', mechanism_spec_zh TEXT NOT NULL DEFAULT '', future_brier REAL NOT NULL DEFAULT 1, future_ece REAL NOT NULL DEFAULT 1, future_coverage REAL NOT NULL DEFAULT 0, future_block_min REAL NOT NULL DEFAULT 0, future_regime_min REAL NOT NULL DEFAULT 0, future_max_miss INTEGER NOT NULL DEFAULT 0, paired_mainline_n INTEGER NOT NULL DEFAULT 0, paired_mainline_delta REAL NOT NULL DEFAULT 0, lifecycle_reason TEXT NOT NULL DEFAULT '', research_genome_contract TEXT NOT NULL DEFAULT '', future_fdr_q REAL NOT NULL DEFAULT 1, mutation_intent TEXT NOT NULL DEFAULT '', creation_reason TEXT NOT NULL DEFAULT '');

-- v57_algorithm_predraw_r908
CREATE TABLE v57_algorithm_predraw_r908 ( genome_id TEXT NOT NULL, target_issue TEXT NOT NULL, position INTEGER NOT NULL, n1 INTEGER NOT NULL, n2 INTEGER NOT NULL, n3 INTEGER NOT NULL, selected_ordinal INTEGER NOT NULL, score REAL NOT NULL, feature_blob TEXT NOT NULL, feature_hash TEXT NOT NULL, frozen_at INTEGER NOT NULL, record_hash TEXT NOT NULL UNIQUE, actual_number INTEGER NOT NULL DEFAULT 0, hit_label INTEGER NOT NULL DEFAULT -1 CHECK(hit_label IN (-1,0,1)), settled_at INTEGER NOT NULL DEFAULT 0, outcome_hash TEXT NOT NULL DEFAULT '', PRIMARY KEY(genome_id,target_issue) );

-- v57_analysis_request_r36
CREATE TABLE v57_analysis_request_r36(target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,status TEXT NOT NULL,generation INTEGER NOT NULL,request_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL DEFAULT 0,started_at INTEGER NOT NULL DEFAULT 0,finished_at INTEGER NOT NULL DEFAULT 0,lease_owner TEXT NOT NULL DEFAULT '',lease_until INTEGER NOT NULL DEFAULT 0,attempts INTEGER NOT NULL DEFAULT 0,last_stage TEXT NOT NULL DEFAULT '',error_code TEXT NOT NULL DEFAULT '',history_hash TEXT NOT NULL DEFAULT '',canonical_hash TEXT NOT NULL DEFAULT '',request_hash TEXT NOT NULL UNIQUE,next_attempt_at INTEGER NOT NULL DEFAULT 0,repair_version TEXT NOT NULL DEFAULT '',first_error_code TEXT NOT NULL DEFAULT '',last_transition_at INTEGER NOT NULL DEFAULT 0);

-- v57_atom_edge_r925
CREATE TABLE v57_atom_edge_r925 (left_atom TEXT NOT NULL, right_atom TEXT NOT NULL, correlation REAL NOT NULL, support_issues INTEGER NOT NULL, last_issue TEXT NOT NULL, PRIMARY KEY(left_atom,right_atom));

-- v57_atom_quality_r925
CREATE TABLE v57_atom_quality_r925 (atom_id TEXT PRIMARY KEY, support_issues INTEGER NOT NULL, mean REAL NOT NULL, variance REAL NOT NULL, drift_score REAL NOT NULL, observation_state TEXT NOT NULL, semantic_tier TEXT NOT NULL, last_issue TEXT NOT NULL, consumer_scope TEXT NOT NULL, updated_at INTEGER NOT NULL);

-- v57_attempt_resource_r927
CREATE TABLE v57_attempt_resource_r927 (execution_id TEXT PRIMARY KEY,attempt_id TEXT NOT NULL,target_issue TEXT NOT NULL,process_id TEXT NOT NULL,release_code INTEGER NOT NULL,started_at INTEGER NOT NULL,finished_at INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,elapsed_ms INTEGER NOT NULL DEFAULT -1,heap_peak_bytes INTEGER NOT NULL DEFAULT -1,heap_samples INTEGER NOT NULL DEFAULT 0,gc_count INTEGER NOT NULL DEFAULT -1,gc_time_ms INTEGER NOT NULL DEFAULT -1,gc_status TEXT NOT NULL DEFAULT 'UNMEASURED',lock_wait_ms INTEGER NOT NULL DEFAULT 0,lock_wait_max_ms INTEGER NOT NULL DEFAULT 0,db_body_max_ms INTEGER NOT NULL DEFAULT 0,db_slice_overruns INTEGER NOT NULL DEFAULT 0,budget_hash TEXT NOT NULL,stage_samples TEXT NOT NULL DEFAULT '',reason TEXT NOT NULL DEFAULT '');

-- v57_batch34_ablation_r930
CREATE TABLE v57_batch34_ablation_r930(case_sha256 TEXT NOT NULL,disabled_module TEXT NOT NULL,replay_sha256 TEXT NOT NULL,canonical_writes_suppressed INTEGER NOT NULL,selection_changed INTEGER NOT NULL,probability_delta REAL NOT NULL,evidence_cases INTEGER NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(case_sha256,disabled_module));

-- v57_batch34_alpha_ledger_r930
CREATE TABLE v57_batch34_alpha_ledger_r930(family TEXT PRIMARY KEY,initial_alpha REAL NOT NULL,available_alpha REAL NOT NULL,global_index INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- v57_batch34_atom_timing_r930
CREATE TABLE v57_batch34_atom_timing_r930(timing_id TEXT PRIMARY KEY,atom_id TEXT NOT NULL,target_issue TEXT NOT NULL,cost_ns INTEGER NOT NULL,budget_ns INTEGER NOT NULL,within_budget INTEGER NOT NULL,created_at INTEGER NOT NULL);

-- v57_batch34_atom_value_r930
CREATE TABLE v57_batch34_atom_value_r930(atom_id TEXT NOT NULL,target_issue TEXT NOT NULL,category TEXT NOT NULL,module_state TEXT NOT NULL,semantic_type TEXT NOT NULL,present INTEGER NOT NULL,missing_reason TEXT NOT NULL,max_source_issue TEXT NOT NULL,cost_ns INTEGER NOT NULL,value_sha256 TEXT NOT NULL,implementation_sha256 TEXT NOT NULL,contract_sha256 TEXT NOT NULL,legacy_fixed INTEGER NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(atom_id,target_issue));

-- v57_batch34_bundle_r930
CREATE TABLE v57_batch34_bundle_r930(generation_id TEXT PRIMARY KEY,bundle_sha256 TEXT NOT NULL UNIQUE,previous_bundle_sha256 TEXT NOT NULL,contract_sha256 TEXT NOT NULL,state TEXT NOT NULL,activation_issue TEXT NOT NULL,active INTEGER NOT NULL,rollback_group TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- v57_batch34_capture_r930
CREATE TABLE v57_batch34_capture_r930(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,opportunity_sha256 TEXT NOT NULL,current_state TEXT NOT NULL,sequence INTEGER NOT NULL,system_available INTEGER NOT NULL,eligible INTEGER NOT NULL,selected INTEGER NOT NULL,execution_started INTEGER NOT NULL,frozen INTEGER NOT NULL,failed INTEGER NOT NULL,lane TEXT NOT NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT NULL,selected_hit_p REAL NOT NULL,predictability REAL NOT NULL,canonical_hash TEXT NOT NULL,snapshot_hash TEXT NOT NULL,frozen_payload_hash TEXT NOT NULL,last_failure TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(game_code,target_issue));

-- v57_batch34_capture_transition_r930
CREATE TABLE v57_batch34_capture_transition_r930(seq INTEGER PRIMARY KEY AUTOINCREMENT,transition_id TEXT NOT NULL UNIQUE,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,from_state TEXT NOT NULL,to_state TEXT NOT NULL,sequence_before INTEGER NOT NULL,sequence_after INTEGER NOT NULL,reason TEXT NOT NULL,payload_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_batch34_evidence_link_r930
CREATE TABLE v57_batch34_evidence_link_r930(evidence_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,evidence_type TEXT NOT NULL,owner TEXT NOT NULL,payload_hash TEXT NOT NULL,detail TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_batch34_failure_case_r930
CREATE TABLE v57_batch34_failure_case_r930(case_sha256 TEXT PRIMARY KEY,target_issue TEXT NOT NULL,selected_action_sha256 TEXT NOT NULL,frozen_input_sha256 TEXT NOT NULL,actual_outcome_sha256 TEXT NOT NULL,failure_class TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_batch34_family_member_r930
CREATE TABLE v57_batch34_family_member_r930(family_key TEXT NOT NULL,algorithm_id TEXT NOT NULL,structural_sha256 TEXT NOT NULL,probe_behavior_sha256 TEXT NOT NULL,residual_signature_sha256 TEXT NOT NULL,statistical_family TEXT NOT NULL,artifact_sha256 TEXT NOT NULL,role TEXT NOT NULL,future_support INTEGER NOT NULL,registered_at INTEGER NOT NULL,PRIMARY KEY(family_key,algorithm_id));

-- v57_batch34_genome_edge_r930
CREATE TABLE v57_batch34_genome_edge_r930(genome_id TEXT NOT NULL,edge_ordinal INTEGER NOT NULL,from_node TEXT NOT NULL,to_node TEXT NOT NULL,edge_type TEXT NOT NULL,edge_sha256 TEXT NOT NULL,PRIMARY KEY(genome_id,edge_ordinal));

-- v57_batch34_genome_r930
CREATE TABLE v57_batch34_genome_r930(genome_id TEXT PRIMARY KEY,statistical_family TEXT NOT NULL,novelty_class TEXT NOT NULL,expression_dsl TEXT NOT NULL,state_update_dsl TEXT NOT NULL,type_contract TEXT NOT NULL,range_contract TEXT NOT NULL,worst_case_cost_ns INTEGER NOT NULL,pit_safe INTEGER NOT NULL,deterministic INTEGER NOT NULL,hard_constraints_passed INTEGER NOT NULL,genome_sha256 TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL);

-- v57_batch34_hypothesis_r930
CREATE TABLE v57_batch34_hypothesis_r930(hypothesis_id TEXT PRIMARY KEY,family TEXT NOT NULL,global_index INTEGER NOT NULL,alpha REAL NOT NULL,window_id TEXT NOT NULL,state TEXT NOT NULL,p_value REAL,evidence_sha256 TEXT NOT NULL DEFAULT '',reserved_at INTEGER NOT NULL,decided_at INTEGER NOT NULL DEFAULT 0);

-- v57_batch34_lifecycle_event_r930
CREATE TABLE v57_batch34_lifecycle_event_r930(seq INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT NOT NULL UNIQUE,algorithm_id TEXT NOT NULL,from_state TEXT NOT NULL,to_state TEXT NOT NULL,added_future_support INTEGER NOT NULL,evidence_sha256 TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_batch34_lifecycle_r930
CREATE TABLE v57_batch34_lifecycle_r930(algorithm_id TEXT PRIMARY KEY,parent_algorithm_id TEXT NOT NULL,genome_sha256 TEXT NOT NULL,repair_target TEXT NOT NULL,falsification_rule TEXT NOT NULL,state TEXT NOT NULL,future_support INTEGER NOT NULL,last_evidence_sha256 TEXT NOT NULL,first_prune_reason TEXT NOT NULL,first_prune_metrics TEXT NOT NULL,first_prune_issue TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- v57_batch34_module_event_r930
CREATE TABLE v57_batch34_module_event_r930(event_id TEXT PRIMARY KEY,module_id TEXT NOT NULL,from_state TEXT NOT NULL,to_state TEXT NOT NULL,reason TEXT NOT NULL,evidence_sha256 TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_batch34_module_state_r930
CREATE TABLE v57_batch34_module_state_r930(module_id TEXT PRIMARY KEY,module_state TEXT NOT NULL,reason TEXT NOT NULL,implementation_sha256 TEXT NOT NULL,contract_sha256 TEXT NOT NULL,certificate_sha256 TEXT NOT NULL,updated_at INTEGER NOT NULL);

-- v57_batch34_paired_observation_r930
CREATE TABLE v57_batch34_paired_observation_r930(window_id TEXT NOT NULL,target_issue TEXT NOT NULL,candidate_hit INTEGER NOT NULL,champion_hit INTEGER NOT NULL,candidate_probability REAL NOT NULL,champion_probability REAL NOT NULL,payload_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(window_id,target_issue));

-- v57_batch34_paired_window_r930
CREATE TABLE v57_batch34_paired_window_r930(window_id TEXT PRIMARY KEY,candidate_sha256 TEXT NOT NULL,champion_sha256 TEXT NOT NULL,policy_sha256 TEXT NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,planned_issues INTEGER NOT NULL,observations INTEGER NOT NULL DEFAULT 0,state TEXT NOT NULL,terminal_decision_sha256 TEXT NOT NULL DEFAULT '',declared_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- v57_batch34_policy_decision_r930
CREATE TABLE v57_batch34_policy_decision_r930(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,policy_sha256 TEXT NOT NULL,predictability REAL NOT NULL,selected_hit_p REAL NOT NULL,lane TEXT NOT NULL,selected INTEGER NOT NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT NULL,canonical_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(game_code,target_issue));

-- v57_batch34_policy_metric_r930
CREATE TABLE v57_batch34_policy_metric_r930(metric_id TEXT PRIMARY KEY,policy_sha256 TEXT NOT NULL,start_issue TEXT NOT NULL,end_issue TEXT NOT NULL,available_n INTEGER NOT NULL,selected_n INTEGER NOT NULL,settled_selected_n INTEGER NOT NULL,calibrated_n INTEGER NOT NULL,hits INTEGER NOT NULL,coverage REAL NOT NULL,hit_rate REAL NOT NULL,brier REAL NOT NULL,risk REAL NOT NULL,denominator TEXT NOT NULL,frequency_debt INTEGER NOT NULL,created_at INTEGER NOT NULL);

-- v57_batch34_policy_outcome_r930
CREATE TABLE v57_batch34_policy_outcome_r930(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,hit_label INTEGER NOT NULL,settlement_sha256 TEXT NOT NULL,draw_sha256 TEXT NOT NULL,status TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(game_code,target_issue));

-- v57_batch34_schema_meta_r930
CREATE TABLE v57_batch34_schema_meta_r930(singleton INTEGER PRIMARY KEY CHECK(singleton=1),revision TEXT NOT NULL,contract_hash TEXT NOT NULL,state TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '');

-- v57_candidate_coverage_r925
CREATE TABLE v57_candidate_coverage_r925 (genome_id TEXT PRIMARY KEY, system_capture_coverage REAL NOT NULL, candidate_execution_coverage REAL, execution_opportunities INTEGER NOT NULL, coverage_contract TEXT NOT NULL, updated_at INTEGER NOT NULL);

-- v57_capture_opportunity_r925
CREATE TABLE v57_capture_opportunity_r925 (target_issue TEXT NOT NULL, genome_id TEXT NOT NULL, opportunity_at INTEGER NOT NULL, status TEXT NOT NULL, cost_ms INTEGER NOT NULL DEFAULT 0, reason TEXT NOT NULL DEFAULT '', PRIMARY KEY(target_issue,genome_id));

-- v57_controlled_opportunity_r922
CREATE TABLE v57_controlled_opportunity_r922(target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,runtime_version TEXT NOT NULL,gate_eligible INTEGER NOT NULL,gate_code TEXT NOT NULL,gate_detail TEXT NOT NULL,governance_lane TEXT NOT NULL,governance_candidate TEXT NOT NULL,governance_status TEXT NOT NULL,governance_weight REAL NOT NULL,governance_applied INTEGER NOT NULL,controlled_influence_eligible INTEGER NOT NULL,authorized_action TEXT NOT NULL,authorized_reject TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,raw_action_p REAL NOT NULL,third_margin REAL NOT NULL,action_stability REAL NOT NULL,entropy REAL NOT NULL,assignment_support REAL NOT NULL,falsifier REAL NOT NULL,rank_retention REAL NOT NULL,third_robustness REAL NOT NULL,margin_robustness REAL NOT NULL,observation_target REAL NOT NULL CHECK(observation_target=0.15),record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL);

-- v57_critical_error_r815
CREATE TABLE v57_critical_error_r815(id INTEGER PRIMARY KEY AUTOINCREMENT,event_key TEXT NOT NULL UNIQUE,domain TEXT NOT NULL,target_issue TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT 0,error_code TEXT NOT NULL,throwable_class TEXT NOT NULL DEFAULT '',detail TEXT NOT NULL DEFAULT '',retryable INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,resolved_at INTEGER NOT NULL DEFAULT 0);

-- v57_deferred_evidence_job
CREATE TABLE v57_deferred_evidence_job(target_issue TEXT PRIMARY KEY,job_type TEXT NOT NULL,status TEXT NOT NULL,attempts INTEGER NOT NULL DEFAULT 0,bundle_hash TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '',record_hash TEXT NOT NULL UNIQUE, snapshot_blob BLOB, snapshot_hash TEXT NOT NULL DEFAULT '', snapshot_schema INTEGER NOT NULL DEFAULT 0, lease_owner TEXT NOT NULL DEFAULT '', lease_until INTEGER NOT NULL DEFAULT 0, next_attempt_at INTEGER NOT NULL DEFAULT 0, completed_at INTEGER NOT NULL DEFAULT 0, number_rows INTEGER NOT NULL DEFAULT 0, action_rows INTEGER NOT NULL DEFAULT 0, last_stage TEXT NOT NULL DEFAULT '', core_materialized INTEGER NOT NULL DEFAULT 0, core_next_ordinal INTEGER NOT NULL DEFAULT 0, next_action_ordinal INTEGER NOT NULL DEFAULT 0, cursor_version INTEGER NOT NULL DEFAULT 1);

-- v57_draw_canonical_r811
CREATE TABLE v57_draw_canonical_r811(target_issue TEXT PRIMARY KEY,status TEXT NOT NULL,canonical_draw_hash TEXT NOT NULL DEFAULT '',evidence_hash TEXT NOT NULL,source_count INTEGER NOT NULL DEFAULT 0,conflict_count INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL);

-- v57_draw_provenance_r811
CREATE TABLE v57_draw_provenance_r811(provenance_id TEXT PRIMARY KEY,endpoint_id TEXT NOT NULL,request_at INTEGER NOT NULL,response_at INTEGER NOT NULL,result_code TEXT NOT NULL,raw_payload_sha256 TEXT NOT NULL,parsed_issue TEXT NOT NULL,permutation TEXT NOT NULL,server_open_time INTEGER NOT NULL,parser_version TEXT NOT NULL,source_line TEXT NOT NULL,draw_hash TEXT NOT NULL,observed_at INTEGER NOT NULL);

-- v57_exploration_r83
CREATE TABLE v57_exploration_r83(target_issue TEXT PRIMARY KEY,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,score REAL NOT NULL,threshold REAL NOT NULL,history_n INTEGER NOT NULL,recent_rate REAL NOT NULL,action TEXT NOT NULL,mode TEXT NOT NULL,decided_at INTEGER NOT NULL);

-- v57_fetch_ledger_r925
CREATE TABLE v57_fetch_ledger_r925 (request_id TEXT PRIMARY KEY, method TEXT NOT NULL, endpoint TEXT NOT NULL, request_at INTEGER NOT NULL, response_at INTEGER, latency_ms INTEGER, http_code INTEGER, response_hash TEXT, source_line TEXT, latest_issue TEXT, status TEXT NOT NULL, error_code TEXT, provider TEXT NOT NULL DEFAULT '', payload_hash TEXT NOT NULL DEFAULT '', first_available_at INTEGER NOT NULL DEFAULT 0, received_at INTEGER NOT NULL DEFAULT 0, draw_at INTEGER NOT NULL DEFAULT 0, clock_epoch TEXT NOT NULL DEFAULT '', label_version TEXT NOT NULL DEFAULT '');

-- v57_hypothesis_r925
CREATE TABLE v57_hypothesis_r925 (genome_id TEXT PRIMARY KEY, family_id TEXT NOT NULL, structural_hash TEXT NOT NULL, registration_origin TEXT NOT NULL, registered_at INTEGER NOT NULL);

-- v57_issue_closure_r906
CREATE TABLE v57_issue_closure_r906 ( target_issue TEXT PRIMARY KEY, draw_hash TEXT NOT NULL, predraw_record_hash TEXT NOT NULL DEFAULT '', eligibility TEXT NOT NULL, core_status TEXT NOT NULL, actual_number INTEGER NOT NULL DEFAULT 0, hit_label INTEGER NOT NULL DEFAULT -1, oos_record_hash TEXT NOT NULL DEFAULT '', real_record_hash TEXT NOT NULL DEFAULT '', settlement_hash TEXT NOT NULL DEFAULT '', previous_task_status TEXT NOT NULL DEFAULT '', exclusion_or_error_code TEXT NOT NULL DEFAULT '', classified_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0, record_hash TEXT NOT NULL UNIQUE );

-- v57_issue_projection_r926
CREATE TABLE v57_issue_projection_r926(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,current_state TEXT NOT NULL,state_rank INTEGER NOT NULL,epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,last_event_id TEXT NOT NULL,event_watermark INTEGER NOT NULL,request_seen INTEGER NOT NULL DEFAULT 0,accepted_seen INTEGER NOT NULL DEFAULT 0,rejected_seen INTEGER NOT NULL DEFAULT 0,queued_seen INTEGER NOT NULL DEFAULT 0,started_seen INTEGER NOT NULL DEFAULT 0,frozen_seen INTEGER NOT NULL DEFAULT 0,canonical_seen INTEGER NOT NULL DEFAULT 0,settled_seen INTEGER NOT NULL DEFAULT 0,full_seen INTEGER NOT NULL DEFAULT 0,learned_seen INTEGER NOT NULL DEFAULT 0,recovery_seen INTEGER NOT NULL DEFAULT 0,last_error_code TEXT NOT NULL DEFAULT '',as_of INTEGER NOT NULL,PRIMARY KEY(game_code,target_issue));

-- v57_k3_action_space_1200
CREATE TABLE v57_k3_action_space_1200(target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT NULL,raw_action_p REAL NOT NULL,rank_score REAL NOT NULL,selected_hit_p REAL NOT NULL DEFAULT 0,rank_no INTEGER NOT NULL,hit_label INTEGER NOT NULL DEFAULT -1,fingerprint TEXT NOT NULL,meta_blob TEXT NOT NULL,action_hash TEXT NOT NULL,game_code TEXT NOT NULL DEFAULT 'Binance1-PK10',bundle_hash TEXT NOT NULL DEFAULT '',contract_hash TEXT NOT NULL DEFAULT '',record_hash TEXT NOT NULL DEFAULT '',PRIMARY KEY(target_issue,position,n1,n2,n3));

-- v57_k3_champion_freeze
CREATE TABLE v57_k3_champion_freeze(target_issue TEXT PRIMARY KEY,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,rank_score REAL NOT NULL,action_hash TEXT NOT NULL,ranker_artifact TEXT NOT NULL,artifact_proven INTEGER NOT NULL,frozen INTEGER NOT NULL,game_code TEXT NOT NULL DEFAULT 'Binance1-PK10',bundle_hash TEXT NOT NULL DEFAULT '',record_hash TEXT NOT NULL DEFAULT '');

-- v57_k3_funnel_trace
CREATE TABLE v57_k3_funnel_trace(target_issue TEXT NOT NULL,position INTEGER NOT NULL,trace_blob TEXT NOT NULL,final3 TEXT NOT NULL,matrix_hash TEXT NOT NULL,game_code TEXT NOT NULL DEFAULT 'Binance1-PK10',bundle_hash TEXT NOT NULL DEFAULT '',record_hash TEXT NOT NULL DEFAULT '',PRIMARY KEY(target_issue,position));

-- v57_legacy_postsettlement_context_r919
CREATE TABLE v57_legacy_postsettlement_context_r919 (target_issue TEXT PRIMARY KEY,status TEXT NOT NULL,actual_number INTEGER NOT NULL,hit_label INTEGER NOT NULL,attribution_summary TEXT NOT NULL,feedback_summary TEXT NOT NULL,predraw_record_hash TEXT NOT NULL,draw_hash TEXT NOT NULL,settled_at INTEGER NOT NULL,record_hash TEXT NOT NULL UNIQUE);

-- v57_legacy_predraw_context_r915
CREATE TABLE v57_legacy_predraw_context_r915 (target_issue TEXT PRIMARY KEY,position INTEGER NOT NULL,teacher_summary TEXT NOT NULL,reasoning_summary TEXT NOT NULL,reverse_summary TEXT NOT NULL,causal_summary TEXT NOT NULL,feedback_contract TEXT NOT NULL,trend_summary TEXT NOT NULL,shape_summary TEXT NOT NULL,relation_summary TEXT NOT NULL,namespace TEXT NOT NULL,frozen_at INTEGER NOT NULL,record_hash TEXT NOT NULL);

-- v57_lifecycle_audit_r925
CREATE TABLE v57_lifecycle_audit_r925 (genome_id TEXT PRIMARY KEY, first_prune_reason TEXT NOT NULL, first_prune_metrics TEXT NOT NULL, first_prune_issue TEXT NOT NULL, first_prune_at INTEGER NOT NULL, prune_policy_version TEXT NOT NULL, original_available INTEGER NOT NULL, latest_status TEXT NOT NULL, latest_reason TEXT NOT NULL, updated_at INTEGER NOT NULL);

-- v57_long_text_r815
CREATE TABLE v57_long_text_r815(token TEXT NOT NULL,chunk_index INTEGER NOT NULL,chunk_text TEXT NOT NULL,chunk_sha256 TEXT NOT NULL,report_sha256 TEXT NOT NULL,created_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,PRIMARY KEY(token,chunk_index));

-- v57_oos_label_r35
CREATE TABLE v57_oos_label_r35(target_issue TEXT PRIMARY KEY,game_code TEXT NOT NULL,base_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_hit_p REAL NOT NULL DEFAULT -1,actual_number INTEGER NOT NULL,hit_label INTEGER NOT NULL,strict_oos INTEGER NOT NULL,pre_draw_frozen INTEGER NOT NULL,prediction_created_at INTEGER NOT NULL,draw_open_time INTEGER NOT NULL DEFAULT 0,settled_at INTEGER NOT NULL,brier REAL NOT NULL DEFAULT -1,provenance_hash TEXT NOT NULL UNIQUE,record_hash TEXT NOT NULL UNIQUE,error_code TEXT NOT NULL DEFAULT '',CHECK(game_code='Binance1-PK10'),CHECK(position BETWEEN 1 AND 10),CHECK(hit_label IN (0,1)),CHECK(strict_oos IN (0,1)),CHECK(pre_draw_frozen=1));

-- v57_prediction_timing_r925
CREATE TABLE v57_prediction_timing_r925 (target_issue TEXT PRIMARY KEY, process_id TEXT NOT NULL, release_code INTEGER NOT NULL, request_at INTEGER NOT NULL, runtime_at INTEGER NOT NULL, durable_at INTEGER NOT NULL, queue_ms INTEGER NOT NULL, compute_ms INTEGER NOT NULL, commit_ms INTEGER NOT NULL, total_ms INTEGER NOT NULL, sample_class TEXT NOT NULL, attempt_id TEXT NOT NULL DEFAULT '');

-- v57_promotion_history_r925
CREATE TABLE v57_promotion_history_r925 (genome_id TEXT NOT NULL, start_issue TEXT NOT NULL, initial_n INTEGER NOT NULL, initial_evidence_hash TEXT NOT NULL, registered_at INTEGER NOT NULL, PRIMARY KEY(genome_id,start_issue));

-- v57_promotion_window_r925
CREATE TABLE v57_promotion_window_r925 (genome_id TEXT PRIMARY KEY, start_issue TEXT NOT NULL, initial_n INTEGER NOT NULL, initial_evidence_hash TEXT NOT NULL, registered_at INTEGER NOT NULL);

-- v57_recovery_generation_r815
CREATE TABLE v57_recovery_generation_r815(generation INTEGER PRIMARY KEY,state TEXT NOT NULL,trigger_mask INTEGER NOT NULL DEFAULT 0,attempt_count INTEGER NOT NULL DEFAULT 0,window_started_at INTEGER NOT NULL,last_attempt_at INTEGER NOT NULL DEFAULT 0,completed_at INTEGER NOT NULL DEFAULT 0,detail TEXT NOT NULL DEFAULT '');

-- v57_research_action_label_r811
CREATE TABLE v57_research_action_label_r811(target_issue TEXT NOT NULL,global_ordinal INTEGER NOT NULL,action_hash TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,actual_number INTEGER NOT NULL,hit_label INTEGER NOT NULL,label_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,global_ordinal));

-- v57_research_artifact_job_r906
CREATE TABLE v57_research_artifact_job_r906 ( job_id TEXT PRIMARY KEY, asset_id TEXT NOT NULL, job_status TEXT NOT NULL, dataset_hash TEXT NOT NULL, data_until_issue TEXT NOT NULL, feature_schema_hash TEXT NOT NULL, canonical_method_spec TEXT NOT NULL, seed INTEGER NOT NULL, protocol_hash TEXT NOT NULL, artifact_hash TEXT NOT NULL DEFAULT '', manifest_hash TEXT NOT NULL DEFAULT '', parity_status TEXT NOT NULL DEFAULT 'NOT_RUN', last_error_code TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(asset_id,dataset_hash,protocol_hash) );

-- v57_research_artifact_r811
CREATE TABLE v57_research_artifact_r811(artifact_id TEXT PRIMARY KEY,artifact_type TEXT NOT NULL,trained_until_issue TEXT NOT NULL,feature_schema_hash TEXT NOT NULL,artifact_blob BLOB NOT NULL,artifact_hash TEXT NOT NULL UNIQUE,status TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_research_artifact_r815
CREATE TABLE v57_research_artifact_r815(artifact_id TEXT PRIMARY KEY,candidate_id TEXT NOT NULL,trained_until_issue TEXT NOT NULL,schema_sha256 TEXT NOT NULL,dataset_sha256 TEXT NOT NULL,artifact_sha256 TEXT NOT NULL UNIQUE,status TEXT NOT NULL,created_at INTEGER NOT NULL,detail TEXT NOT NULL DEFAULT '');

-- v57_research_asset_r906
CREATE TABLE v57_research_asset_r906 ( asset_id TEXT PRIMARY KEY, display_name_zh TEXT NOT NULL, normalized_name_zh TEXT NOT NULL UNIQUE, candidate_type TEXT NOT NULL, novelty_class TEXT NOT NULL, tier TEXT NOT NULL, method_signature TEXT NOT NULL, canonical_spec TEXT NOT NULL, representation_family TEXT NOT NULL, learner_family TEXT NOT NULL, update_rule TEXT NOT NULL, objective_family TEXT NOT NULL, calibration_family TEXT NOT NULL, risk_family TEXT NOT NULL, falsifier_family TEXT NOT NULL, parent_asset_ids TEXT NOT NULL DEFAULT '', generation INTEGER NOT NULL, genetic_operator TEXT NOT NULL, feature_schema_hash TEXT NOT NULL DEFAULT '', artifact_status TEXT NOT NULL, artifact_hash TEXT NOT NULL DEFAULT '', trained_until_issue TEXT NOT NULL DEFAULT '', earliest_use_issue TEXT NOT NULL DEFAULT '', runtime_executable INTEGER NOT NULL DEFAULT 0, may_authorize_real INTEGER NOT NULL DEFAULT 0 CHECK(may_authorize_real=0), last_transition_code TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, registry_hash TEXT NOT NULL UNIQUE );

-- v57_research_candidate_outcome_r811
CREATE TABLE v57_research_candidate_outcome_r811(candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,selected_ordinal INTEGER NOT NULL,score REAL NOT NULL,probability REAL NOT NULL,hit_label INTEGER NOT NULL,regime_id TEXT NOT NULL,ablated_feature TEXT NOT NULL,ablated_ordinal INTEGER NOT NULL,ablated_score REAL NOT NULL,ablated_probability REAL NOT NULL,ablated_hit_label INTEGER NOT NULL,ablated_regime_id TEXT NOT NULL,latency_ms REAL NOT NULL,ram_peak_bytes INTEGER NOT NULL,prediction_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,PRIMARY KEY(candidate_id,target_issue));

-- v57_research_candidate_r811
CREATE TABLE v57_research_candidate_r811(candidate_id TEXT PRIMARY KEY,run_id TEXT NOT NULL,hypothesis_id TEXT NOT NULL DEFAULT '',candidate_type TEXT NOT NULL,parent_champion_hash TEXT NOT NULL,canonical_spec TEXT NOT NULL,dataset_hash TEXT NOT NULL DEFAULT '',formula_ast TEXT NOT NULL DEFAULT '',changed_symbols TEXT NOT NULL,forbidden_symbols TEXT NOT NULL,seed INTEGER NOT NULL,complexity REAL NOT NULL DEFAULT 0,code_hash TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT 0,genetic_operator TEXT NOT NULL DEFAULT 'DIRECT_SPEC',parent_formula_ids TEXT NOT NULL DEFAULT '',feature_dependencies TEXT NOT NULL DEFAULT '',artifact_hash TEXT NOT NULL DEFAULT '',runtime_executable INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- v57_research_candidate_r815
CREATE TABLE v57_research_candidate_r815(candidate_id TEXT PRIMARY KEY,candidate_type TEXT NOT NULL,genome_sha256 TEXT NOT NULL,schema_sha256 TEXT NOT NULL,dataset_sha256 TEXT NOT NULL,game_code TEXT NOT NULL,state TEXT NOT NULL,created_target_issue TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,detail TEXT NOT NULL DEFAULT '');

-- v57_research_candidate_score_r900
CREATE TABLE v57_research_candidate_score_r900(candidate_id TEXT PRIMARY KEY,future_n INTEGER NOT NULL DEFAULT 0,hit_n INTEGER NOT NULL DEFAULT 0,hit_rate REAL NOT NULL DEFAULT 0,wilson_lower REAL NOT NULL DEFAULT 0,wilson_upper REAL NOT NULL DEFAULT 1,brier REAL NOT NULL DEFAULT -1,state TEXT NOT NULL DEFAULT 'INCUBATING',last_issue TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL DEFAULT 0);

-- v57_research_certificate_r906
CREATE TABLE v57_research_certificate_r906 ( certificate_id TEXT PRIMARY KEY, asset_id TEXT NOT NULL, certificate_type TEXT NOT NULL, evidence_start_issue TEXT NOT NULL, evidence_end_issue TEXT NOT NULL, future_n INTEGER NOT NULL, coverage REAL NOT NULL, paired_delta REAL NOT NULL, adjusted_lower REAL NOT NULL, brier REAL NOT NULL, ece REAL NOT NULL, block_stability REAL NOT NULL, drift_score REAL NOT NULL, latency_p95_ms REAL NOT NULL, ram_p95_bytes INTEGER NOT NULL, valid_from_issue TEXT NOT NULL, valid_until_issue TEXT NOT NULL, status TEXT NOT NULL, revocation_code TEXT NOT NULL DEFAULT '', evidence_hash TEXT NOT NULL, record_hash TEXT NOT NULL UNIQUE, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL );

-- v57_research_codegen_r811
CREATE TABLE v57_research_codegen_r811(candidate_id TEXT PRIMARY KEY,bundle_path TEXT NOT NULL DEFAULT '',java_files TEXT NOT NULL DEFAULT '',patch_sha256 TEXT NOT NULL DEFAULT '',manifest_sha256 TEXT NOT NULL DEFAULT '',compile_status TEXT NOT NULL DEFAULT 'NOT_RUN',owner_audit_status TEXT NOT NULL DEFAULT 'NOT_RUN',java_source TEXT NOT NULL DEFAULT '',patch_text TEXT NOT NULL DEFAULT '',manifest_text TEXT NOT NULL DEFAULT '',fixtures_text TEXT NOT NULL DEFAULT '',bundle_hash TEXT NOT NULL DEFAULT '',generated_at INTEGER NOT NULL);

-- v57_research_compact_label_r906
CREATE TABLE v57_research_compact_label_r906 ( asset_id TEXT NOT NULL, target_issue TEXT NOT NULL, selected_ordinal INTEGER NOT NULL, feature_schema_hash TEXT NOT NULL, feature_blob BLOB NOT NULL, feature_hash TEXT NOT NULL, predicted_probability REAL NOT NULL, actual_number INTEGER NOT NULL, hit_label INTEGER NOT NULL CHECK(hit_label IN (0,1)), predraw_hash TEXT NOT NULL, draw_hash TEXT NOT NULL, outcome_hash TEXT NOT NULL UNIQUE, frozen_at INTEGER NOT NULL, settled_at INTEGER NOT NULL, PRIMARY KEY(asset_id,target_issue) );

-- v57_research_eval_coverage_r906
CREATE TABLE v57_research_eval_coverage_r906 ( asset_id TEXT PRIMARY KEY, eligible_n INTEGER NOT NULL DEFAULT 0, scheduled_n INTEGER NOT NULL DEFAULT 0, frozen_n INTEGER NOT NULL DEFAULT 0, settled_n INTEGER NOT NULL DEFAULT 0, hit_n INTEGER NOT NULL DEFAULT 0, first_eligible_issue TEXT NOT NULL DEFAULT '', last_eligible_issue TEXT NOT NULL DEFAULT '', last_frozen_issue TEXT NOT NULL DEFAULT '', last_settled_issue TEXT NOT NULL DEFAULT '', max_gap_issues INTEGER NOT NULL DEFAULT 0, hit_rate REAL, interval_lower REAL, interval_upper REAL, interval_method TEXT NOT NULL DEFAULT '', brier REAL, ece REAL, block_stability REAL, drift_score REAL, latency_p95_ms REAL, ram_p95_bytes INTEGER, evidence_hash TEXT NOT NULL, updated_at INTEGER NOT NULL );

-- v57_research_eval_r815
CREATE TABLE v57_research_eval_r815(candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,artifact_sha256 TEXT NOT NULL,selected INTEGER NOT NULL,hit INTEGER NOT NULL,selected_p REAL,brier REAL,log_loss REAL,block_id TEXT NOT NULL,regime_id TEXT NOT NULL,settled_at INTEGER NOT NULL,record_hash TEXT NOT NULL,PRIMARY KEY(candidate_id,target_issue));

-- v57_research_experiment_r811
CREATE TABLE v57_research_experiment_r811(experiment_id TEXT PRIMARY KEY,candidate_id TEXT NOT NULL,fold_id TEXT NOT NULL,split_role TEXT NOT NULL,start_issue TEXT NOT NULL,end_issue TEXT NOT NULL,n INTEGER NOT NULL,eligible_n INTEGER NOT NULL DEFAULT 0,selected_n INTEGER NOT NULL DEFAULT 0,hit_n INTEGER NOT NULL DEFAULT 0,hit_rate REAL,coverage REAL,wilson_lower REAL,beta_lower REAL,ece REAL,brier REAL,coverage_cv REAL,longest_lose INTEGER,latency_p95_ms REAL,ram_peak_bytes INTEGER,p_value REAL,fdr_pass INTEGER NOT NULL DEFAULT 0,result_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_research_expert_outcome_r811
CREATE TABLE v57_research_expert_outcome_r811(target_issue TEXT NOT NULL,expert_id TEXT NOT NULL,position INTEGER NOT NULL,regime_id TEXT NOT NULL DEFAULT '',actual_number INTEGER NOT NULL,p_actual REAL NOT NULL,log_loss REAL NOT NULL,multiclass_brier REAL NOT NULL,actual_rank INTEGER NOT NULL,k3_probability REAL NOT NULL DEFAULT -1,k3_hit INTEGER NOT NULL DEFAULT -1,record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,expert_id,position));

-- v57_research_expert_predraw_r811
CREATE TABLE v57_research_expert_predraw_r811(target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,history_hash TEXT NOT NULL,canonical_hash TEXT NOT NULL,schema_version INTEGER NOT NULL,expert_count INTEGER NOT NULL,blob BLOB NOT NULL,blob_sha256 TEXT NOT NULL,frozen_at INTEGER NOT NULL,created_at INTEGER NOT NULL);

-- v57_research_hypothesis_r811
CREATE TABLE v57_research_hypothesis_r811(hypothesis_id TEXT PRIMARY KEY,run_id TEXT NOT NULL,failure_layer TEXT NOT NULL,mechanism TEXT NOT NULL,positive_evidence TEXT NOT NULL,counter_evidence TEXT NOT NULL,affected_regime TEXT NOT NULL DEFAULT '',candidate_family TEXT NOT NULL,acceptance_json TEXT NOT NULL,status TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- v57_research_input_pin_r967
CREATE TABLE v57_research_input_pin_r967(task_id TEXT NOT NULL,source_table TEXT NOT NULL,source_issue TEXT NOT NULL,PRIMARY KEY(task_id,source_table,source_issue));

-- v57_research_lane_cursor_r967
CREATE TABLE v57_research_lane_cursor_r967(lane INTEGER PRIMARY KEY CHECK(lane>=0 AND lane<22),desired_issue TEXT NOT NULL,served_issue TEXT NOT NULL DEFAULT '',next_sequence INTEGER NOT NULL DEFAULT 0,active_task_id TEXT NOT NULL DEFAULT '',last_outcome TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL);

-- v57_research_learner_update_r906
CREATE TABLE v57_research_learner_update_r906 ( learner_id TEXT NOT NULL, target_issue TEXT NOT NULL, prior_artifact_hash TEXT NOT NULL, feature_hash TEXT NOT NULL, outcome_hash TEXT NOT NULL, new_artifact_hash TEXT NOT NULL, trained_until_issue TEXT NOT NULL, earliest_use_issue TEXT NOT NULL, update_status TEXT NOT NULL, updated_at INTEGER NOT NULL, record_hash TEXT NOT NULL UNIQUE, PRIMARY KEY(learner_id,target_issue) );

-- v57_research_mainline_contribution_r922
CREATE TABLE v57_research_mainline_contribution_r922(target_issue TEXT PRIMARY KEY,runtime_version TEXT NOT NULL,lane TEXT NOT NULL,candidate_id TEXT NOT NULL,candidate_name TEXT NOT NULL,status TEXT NOT NULL,applied INTEGER NOT NULL,influence_weight REAL NOT NULL,future_n INTEGER NOT NULL,hit_rate REAL NOT NULL,wilson_lower REAL NOT NULL,brier REAL NOT NULL,ece REAL NOT NULL,coverage REAL NOT NULL,paired_n INTEGER NOT NULL,paired_delta REAL NOT NULL,fdr_q REAL NOT NULL,evidence_hash TEXT NOT NULL,rank1_hash TEXT NOT NULL,controlled_influence_eligible INTEGER NOT NULL,direct_real_authority INTEGER NOT NULL CHECK(direct_real_authority=0),record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL);

-- v57_research_predraw_r817
CREATE TABLE v57_research_predraw_r817(candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,phase TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_ordinal INTEGER NOT NULL,score REAL NOT NULL,probability REAL NOT NULL,formula_probability REAL NOT NULL DEFAULT 0,adaptive_probability REAL NOT NULL DEFAULT -1,adaptive_artifact_hash TEXT NOT NULL DEFAULT '',adaptive_trained_until TEXT NOT NULL DEFAULT '',adaptive_updates INTEGER NOT NULL DEFAULT 0,adaptive_weight REAL NOT NULL DEFAULT 0,formula_hash TEXT NOT NULL,input_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,actual_number INTEGER NOT NULL DEFAULT 0,hit_label INTEGER NOT NULL DEFAULT -1,settled_at INTEGER NOT NULL DEFAULT 0,outcome_hash TEXT NOT NULL DEFAULT '',record_hash TEXT NOT NULL UNIQUE,PRIMARY KEY(candidate_id,target_issue));

-- v57_research_prune_log_r900
CREATE TABLE v57_research_prune_log_r900(id INTEGER PRIMARY KEY AUTOINCREMENT,candidate_id TEXT NOT NULL,reason TEXT NOT NULL,future_n INTEGER NOT NULL,hit_n INTEGER NOT NULL,hit_rate REAL NOT NULL,wilson_upper REAL NOT NULL,snapshot_hash TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_research_queue_r815
CREATE TABLE v57_research_queue_r815(target_issue TEXT PRIMARY KEY,settlement_hash TEXT NOT NULL,state TEXT NOT NULL,attempts INTEGER NOT NULL DEFAULT 0,next_attempt_at INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '');

-- v57_research_run_r811
CREATE TABLE v57_research_run_r811(run_id TEXT PRIMARY KEY,state TEXT NOT NULL,target_issue TEXT NOT NULL DEFAULT '',data_until_issue TEXT NOT NULL DEFAULT '',input_hash TEXT NOT NULL,dataset_hash TEXT NOT NULL DEFAULT '',champion_hash TEXT NOT NULL DEFAULT '',generated_count INTEGER NOT NULL DEFAULT 0,evaluated_count INTEGER NOT NULL DEFAULT 0,shadow_eligible_count INTEGER NOT NULL DEFAULT 0,error_code TEXT NOT NULL DEFAULT '',error_detail TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);

-- v57_research_task_r967
CREATE TABLE v57_research_task_r967(task_id TEXT PRIMARY KEY,kind TEXT NOT NULL,candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,stream_seq TEXT NOT NULL,input_hash TEXT NOT NULL,authority_ref TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN ('PENDING','RUNNING','YIELDED','WAIT_INPUT','COMPLETED','EXPIRED','FAILED_TERMINAL')),cursor TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT 0,lease_owner TEXT NOT NULL DEFAULT '',lease_until INTEGER NOT NULL DEFAULT 0,available_at INTEGER NOT NULL DEFAULT 0,attempt_count INTEGER NOT NULL DEFAULT 0,last_error TEXT NOT NULL DEFAULT '',result_hash TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,UNIQUE(kind,candidate_id,target_issue,stream_seq));

-- v57_runtime_epoch_r925
CREATE TABLE v57_runtime_epoch_r925 (epoch_id TEXT PRIMARY KEY, kind TEXT NOT NULL, started_at INTEGER NOT NULL, start_issue TEXT NOT NULL, release_code INTEGER NOT NULL);

-- v57_runtime_event_outbox_r926
CREATE TABLE v57_runtime_event_outbox_r926(outbox_id TEXT PRIMARY KEY,dedup_key TEXT NOT NULL UNIQUE,target_issue TEXT NOT NULL,epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,event_type TEXT NOT NULL,stage TEXT NOT NULL,state_rank INTEGER NOT NULL,payload_text TEXT NOT NULL DEFAULT '',payload_hash TEXT NOT NULL,source TEXT NOT NULL,status TEXT NOT NULL,attempts INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '');

-- v57_runtime_event_r926
CREATE TABLE v57_runtime_event_r926(seq INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT NOT NULL UNIQUE,dedup_key TEXT NOT NULL UNIQUE,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,event_type TEXT NOT NULL,stage TEXT NOT NULL,state_rank INTEGER NOT NULL,payload_text TEXT NOT NULL DEFAULT '',payload_hash TEXT NOT NULL,source TEXT NOT NULL,created_at INTEGER NOT NULL);

-- v57_runtime_health_r35
CREATE TABLE v57_runtime_health_r35(_id INTEGER PRIMARY KEY AUTOINCREMENT,event_at INTEGER NOT NULL,stage TEXT NOT NULL,target_issue TEXT NOT NULL DEFAULT '',status TEXT NOT NULL,elapsed_ms INTEGER NOT NULL DEFAULT 0,active_concurrency INTEGER NOT NULL DEFAULT 0,max_concurrency INTEGER NOT NULL DEFAULT 0,oom_count INTEGER NOT NULL DEFAULT 0,detail TEXT NOT NULL DEFAULT '',record_hash TEXT NOT NULL UNIQUE, attempt_id TEXT NOT NULL DEFAULT '', event_type TEXT NOT NULL DEFAULT '', request_at INTEGER NOT NULL DEFAULT 0, commit_at INTEGER NOT NULL DEFAULT 0, terminal INTEGER NOT NULL DEFAULT 0);

-- v57_runtime_truth_snapshot_r906
CREATE TABLE v57_runtime_truth_snapshot_r906 ( snapshot_id TEXT PRIMARY KEY, generated_at INTEGER NOT NULL, db_data_version INTEGER NOT NULL, latest_draw_issue TEXT NOT NULL, latest_canonical_issue TEXT NOT NULL, latest_closure_issue TEXT NOT NULL, latest_oos_issue TEXT NOT NULL, payload_json TEXT NOT NULL, payload_hash TEXT NOT NULL UNIQUE );

-- v57_schema_domain_r926
CREATE TABLE v57_schema_domain_r926(domain TEXT PRIMARY KEY,revision INTEGER NOT NULL,contract_hash TEXT NOT NULL,fingerprint TEXT NOT NULL,state TEXT NOT NULL,cursor TEXT NOT NULL DEFAULT '',started_at INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '');

-- v57_schema_migration_journal_r926
CREATE TABLE v57_schema_migration_journal_r926(domain TEXT NOT NULL,revision INTEGER NOT NULL,step_id TEXT NOT NULL,status TEXT NOT NULL,attempt INTEGER NOT NULL DEFAULT 0,cursor TEXT NOT NULL DEFAULT '',before_hash TEXT NOT NULL DEFAULT '',after_hash TEXT NOT NULL DEFAULT '',started_at INTEGER NOT NULL DEFAULT 0,completed_at INTEGER NOT NULL DEFAULT 0,error_code TEXT NOT NULL DEFAULT '',PRIMARY KEY(domain,revision,step_id));

-- v57_selected_evidence_r906
CREATE TABLE v57_selected_evidence_r906 ( target_issue TEXT PRIMARY KEY, action_ordinal INTEGER NOT NULL, action_hash TEXT NOT NULL, raw_action_p_value REAL, raw_action_p_status TEXT NOT NULL, raw_action_p_source TEXT NOT NULL, raw_action_p_reason TEXT NOT NULL DEFAULT '', rank_no INTEGER, rank_score_value REAL, rank_status TEXT NOT NULL, third_margin_value REAL, third_margin_status TEXT NOT NULL, entropy_risk_value REAL, entropy_risk_status TEXT NOT NULL, entropy_risk_source TEXT NOT NULL, stability_value REAL, stability_status TEXT NOT NULL, stability_source TEXT NOT NULL, stability_reason TEXT NOT NULL DEFAULT '', window_consensus_value REAL, window_consensus_status TEXT NOT NULL, assignment_support_value REAL, assignment_support_status TEXT NOT NULL, metric_schema_hash TEXT NOT NULL, frozen_at INTEGER NOT NULL, record_hash TEXT NOT NULL UNIQUE );

-- v57_settlement_outbox_r906
CREATE TABLE v57_settlement_outbox_r906 ( event_id TEXT PRIMARY KEY, event_type TEXT NOT NULL, target_issue TEXT NOT NULL, subject_id TEXT NOT NULL DEFAULT '', payload_hash TEXT NOT NULL, status TEXT NOT NULL, priority INTEGER NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, next_attempt_at INTEGER NOT NULL DEFAULT 0, lease_owner TEXT NOT NULL DEFAULT '', lease_generation INTEGER NOT NULL DEFAULT 0, process_boot_id TEXT NOT NULL DEFAULT '', lease_until INTEGER NOT NULL DEFAULT 0, last_progress_at INTEGER NOT NULL DEFAULT 0, last_error_code TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0, UNIQUE(event_type,target_issue,subject_id) );

-- v57_settlement_repair_audit_f02
CREATE TABLE v57_settlement_repair_audit_f02(recovery_id TEXT PRIMARY KEY,target_issue TEXT NOT NULL,prior_generation INTEGER NOT NULL,prior_recovery_count INTEGER NOT NULL,prior_status TEXT NOT NULL,prior_code TEXT NOT NULL,prior_draw_hash TEXT NOT NULL,prior_snapshot_json TEXT NOT NULL,prior_snapshot_hash TEXT NOT NULL UNIQUE,reason TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(target_issue,prior_recovery_count));

-- v57_settlement_task_r36
CREATE TABLE v57_settlement_task_r36(target_issue TEXT PRIMARY KEY,draw_issue TEXT NOT NULL,status TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,completed_at INTEGER NOT NULL DEFAULT 0,lease_owner TEXT NOT NULL DEFAULT '',lease_until INTEGER NOT NULL DEFAULT 0,attempts INTEGER NOT NULL DEFAULT 0,next_attempt_at INTEGER NOT NULL DEFAULT 0,draw_hash TEXT NOT NULL,settlement_hash TEXT NOT NULL DEFAULT '',last_error TEXT NOT NULL DEFAULT '', lease_generation INTEGER NOT NULL DEFAULT 0, process_boot_id TEXT NOT NULL DEFAULT '', last_progress_at INTEGER NOT NULL DEFAULT 0, eligibility TEXT NOT NULL DEFAULT 'UNKNOWN', failure_class TEXT NOT NULL DEFAULT '', recovery_count INTEGER NOT NULL DEFAULT 0, task_class TEXT NOT NULL DEFAULT 'BACKLOG', priority INTEGER NOT NULL DEFAULT 0, terminal_code TEXT NOT NULL DEFAULT '', source TEXT NOT NULL DEFAULT '');

-- v57_shadow_observation_r35
CREATE TABLE v57_shadow_observation_r35(target_issue TEXT PRIMARY KEY,game_code TEXT NOT NULL,base_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_hit_p REAL NOT NULL DEFAULT -1,predictability REAL NOT NULL DEFAULT 0,lane TEXT NOT NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT NULL,bundle_hash TEXT NOT NULL,contract_hash TEXT NOT NULL,authorization_hash TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,pre_draw_frozen INTEGER NOT NULL DEFAULT 1,model_proven INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,settled INTEGER NOT NULL DEFAULT 0,actual_number INTEGER NOT NULL DEFAULT 0,hit_label INTEGER NOT NULL DEFAULT -1,settled_at INTEGER NOT NULL DEFAULT 0,settlement_hash TEXT NOT NULL DEFAULT '',error_code TEXT NOT NULL DEFAULT '', canonical_reason_json TEXT NOT NULL DEFAULT '', canonical_reason_hash TEXT NOT NULL DEFAULT '', analysis_attempt_id TEXT NOT NULL DEFAULT '', history_hash TEXT NOT NULL DEFAULT '', history_max_issue TEXT NOT NULL DEFAULT '', action_space_hash TEXT NOT NULL DEFAULT '', snapshot_hash TEXT NOT NULL DEFAULT '', request_at INTEGER NOT NULL DEFAULT 0, commit_at INTEGER NOT NULL DEFAULT 0, deadline_at INTEGER NOT NULL DEFAULT 0, runtime_version TEXT NOT NULL DEFAULT '',CHECK(game_code='Binance1-PK10'),CHECK(position BETWEEN 1 AND 10),CHECK(n1 BETWEEN 1 AND 10),CHECK(n2 BETWEEN 1 AND 10),CHECK(n3 BETWEEN 1 AND 10),CHECK(pre_draw_frozen=1),CHECK(model_proven IN (0,1)),CHECK(settled IN (0,1)));

-- v57_shadow_sidecar_r815
CREATE TABLE v57_shadow_sidecar_r815(target_issue TEXT PRIMARY KEY,settlement_hash TEXT NOT NULL,canonical_hash TEXT NOT NULL,actual_number INTEGER NOT NULL,hit_label INTEGER NOT NULL,created_at INTEGER NOT NULL,retry_count INTEGER NOT NULL DEFAULT 0,last_error TEXT NOT NULL DEFAULT '',CHECK(hit_label IN (0,1)));

-- window_feature_sample
CREATE TABLE window_feature_sample (_id INTEGER PRIMARY KEY AUTOINCREMENT, sample_key TEXT NOT NULL UNIQUE, base_issue TEXT, target_issue TEXT, created_at INTEGER DEFAULT 0, position INTEGER DEFAULT 0, n1 INTEGER DEFAULT 0, n2 INTEGER DEFAULT 0, n3 INTEGER DEFAULT 0, k3_mask INTEGER DEFAULT 0, vector_key TEXT, raw_key TEXT, cluster_id TEXT, cluster_status TEXT, distance REAL DEFAULT 0, would_hit INTEGER DEFAULT 0, decision_type TEXT, trend_slope12 REAL DEFAULT 0, trend_slope24 REAL DEFAULT 0, trend_slope48 REAL DEFAULT 0, trend_slope96 REAL DEFAULT 0, trend_slope360 REAL DEFAULT 0, trend_life REAL DEFAULT 0, trend_fatigue REAL DEFAULT 0, omission_pressure REAL DEFAULT 0, hot_cold_transition REAL DEFAULT 0, density_score REAL DEFAULT 0, repeat_rhythm REAL DEFAULT 0, odd_even_bias REAL DEFAULT 0, high_low_bias REAL DEFAULT 0, tail_pressure REAL DEFAULT 0, neighbor_pressure REAL DEFAULT 0, cross_position_flow REAL DEFAULT 0, cross_position_crowd REAL DEFAULT 0, same_number_crowd REAL DEFAULT 0, timing_offset REAL DEFAULT 0, counter_evidence REAL DEFAULT 0, third_quality REAL DEFAULT 0, summary TEXT, state_key TEXT, window_state_key TEXT, cluster_key TEXT, factor_key TEXT, factor_id TEXT, factor_name TEXT, sample_count INTEGER DEFAULT 0, recent_sample_count INTEGER DEFAULT 0, hit_count INTEGER DEFAULT 0, miss_count INTEGER DEFAULT 0, hit_rate REAL DEFAULT 0, recent_hit_rate REAL DEFAULT 0, evidence_score REAL DEFAULT 0, anti_evidence_score REAL DEFAULT 0, net_evidence_score REAL DEFAULT 0, sample_reliability REAL DEFAULT 0, recent_validity REAL DEFAULT 0, action_advice TEXT, reject_code TEXT, updated_at INTEGER DEFAULT 0);