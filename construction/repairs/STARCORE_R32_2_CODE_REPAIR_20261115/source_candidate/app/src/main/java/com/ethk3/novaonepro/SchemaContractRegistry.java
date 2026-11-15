package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete V50 PRIME SQLite schema contract. */
public final class SchemaContractRegistry {
    public static final String VERSION = V50BuildManifest.SCHEMA_ID;

    public static final String TABLE_SCHEMA_META = "v50_schema_meta";
    public static final String TABLE_MIGRATION_LOG = "v50_migration_log";
    public static final String TABLE_LEDGER_ERROR = "v50_ledger_error";
    public static final String TABLE_ISSUE = "v50_issue_prediction";
    public static final String TABLE_NUMBER = "v50_number_event";
    public static final String TABLE_ACTION = "v50_position_action";
    public static final String TABLE_SELECTED = "v50_selected_decision";
    public static final String TABLE_AUTH = "v50_authorization_audit";
    public static final String TABLE_STATE_HISTORY = "v50_state_history";
    public static final String TABLE_TEACHER = "v50_teacher_profile";
    public static final String TABLE_EXPERT = "v50_expert_regret";
    public static final String TABLE_ONLINE_RESIDUAL = "v50_online_residual";
    public static final String TABLE_DOMAIN = "v50_domain_certificate";
    public static final String TABLE_ATTRIBUTION = "v50_cause_attribution";
    public static final String TABLE_PERFORMANCE = "v50_performance_span";

    public static final class Column {
        public final String name;
        public final String definition;
        public final boolean required;

        Column(String name, String definition, boolean required) {
            this.name = name;
            this.definition = definition;
            this.required = required;
        }
    }

    public static final class Index {
        public final String name;
        public final String sql;

        Index(String name, String sql) {
            this.name = name;
            this.sql = sql;
        }
    }

    public static final class Table {
        public final String name;
        public final String createSql;
        public final List<Column> columns;
        public final List<Index> indexes;

        Table(String name, String createSql, List<Column> columns, List<Index> indexes) {
            this.name = name;
            this.createSql = createSql;
            this.columns = Collections.unmodifiableList(columns);
            this.indexes = Collections.unmodifiableList(indexes);
        }
    }

    private static final ArrayList<Table> TABLES = new ArrayList<Table>();

    static {
        TABLES.add(
                table(
                        TABLE_SCHEMA_META,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_SCHEMA_META
                                + " (meta_key TEXT PRIMARY KEY,meta_value TEXT NOT NULL,updated_at"
                                + " INTEGER NOT NULL)",
                        cols(
                                new String[][] {
                                    {"meta_key", "TEXT PRIMARY KEY", "1"},
                                    {"meta_value", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"updated_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        none()));

        TABLES.add(
                table(
                        TABLE_MIGRATION_LOG,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_MIGRATION_LOG
                                + " (_id INTEGER PRIMARY KEY AUTOINCREMENT,from_version"
                                + " INTEGER,to_version INTEGER,schema_checksum TEXT,status"
                                + " TEXT,error_code TEXT,error_detail TEXT,created_at INTEGER)",
                        cols(
                                new String[][] {
                                    {"_id", "INTEGER PRIMARY KEY AUTOINCREMENT", "1"},
                                    {"from_version", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"to_version", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"schema_checksum", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"status", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"error_code", "TEXT NOT NULL DEFAULT ''", "0"},
                                    {"error_detail", "TEXT NOT NULL DEFAULT ''", "0"},
                                    {"created_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_migration_created",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_migration_created ON "
                                                + TABLE_MIGRATION_LOG
                                                + "(created_at DESC)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_LEDGER_ERROR,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_LEDGER_ERROR
                                + " (_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue"
                                + " TEXT,failure_code TEXT,owner TEXT,detail TEXT,created_at"
                                + " INTEGER,resolved INTEGER DEFAULT 0)",
                        cols(
                                new String[][] {
                                    {"_id", "INTEGER PRIMARY KEY AUTOINCREMENT", "1"},
                                    {"target_issue", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"failure_code", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"detail", "TEXT NOT NULL DEFAULT ''", "0"},
                                    {"created_at", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"resolved", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_ledger_error_issue",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_ledger_error_issue ON "
                                                + TABLE_LEDGER_ERROR
                                                + "(target_issue,created_at DESC)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_ISSUE,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_ISSUE
                                + " (target_issue TEXT PRIMARY KEY,base_issue TEXT NOT"
                                + " NULL,history_max_issue TEXT NOT NULL,created_at INTEGER NOT"
                                + " NULL,history_hash TEXT NOT NULL,feature_hash TEXT NOT"
                                + " NULL,state_hash TEXT NOT NULL,expert_hash TEXT NOT"
                                + " NULL,number_probability_hash TEXT NOT NULL,action_hash TEXT NOT"
                                + " NULL,model_hash TEXT NOT NULL,cohort_id TEXT NOT NULL,build_id"
                                + " TEXT NOT NULL,strategy_version TEXT NOT NULL,feature_schema"
                                + " TEXT NOT NULL,state_schema TEXT NOT NULL,expert_schema TEXT NOT"
                                + " NULL,action_schema TEXT NOT NULL,number_probability_blob TEXT"
                                + " NOT NULL,action_blob TEXT NOT NULL,hypothesis_blob TEXT NOT"
                                + " NULL,selected_position INTEGER DEFAULT 0,lane TEXT NOT"
                                + " NULL,authorization_state TEXT NOT NULL,contract_hash TEXT NOT"
                                + " NULL,settled INTEGER NOT NULL DEFAULT 0,settled_at INTEGER"
                                + " DEFAULT 0,actual_blob TEXT NOT NULL DEFAULT '',learning_status"
                                + " TEXT NOT NULL DEFAULT '')",
                        cols(
                                new String[][] {
                                    {"target_issue", "TEXT PRIMARY KEY", "1"},
                                    {"base_issue", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"history_max_issue", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"created_at", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"history_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"feature_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"state_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"expert_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"number_probability_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"action_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"model_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"cohort_id", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"build_id", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"strategy_version", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"feature_schema", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"state_schema", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"expert_schema", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"action_schema", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"number_probability_blob", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"action_blob", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"hypothesis_blob", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"selected_position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"lane", "TEXT NOT NULL DEFAULT 'SHADOW'", "1"},
                                    {"authorization_state", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"contract_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"settled", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"settled_at", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"actual_blob", "TEXT NOT NULL DEFAULT ''", "0"},
                                    {"learning_status", "TEXT NOT NULL DEFAULT ''", "0"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_issue_created",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_issue_created ON "
                                                + TABLE_ISSUE
                                                + "(created_at DESC)"
                                    },
                                    {
                                        "idx_v50_issue_cohort_settled",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_issue_cohort_settled ON"
                                            + " "
                                                + TABLE_ISSUE
                                                + "(cohort_id,settled,target_issue)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_NUMBER,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_NUMBER
                                + " (target_issue TEXT NOT NULL,position INTEGER NOT NULL,number"
                                + " INTEGER NOT NULL,predicted_p REAL NOT NULL,actual_label INTEGER"
                                + " DEFAULT -1,number_rank INTEGER NOT NULL,expert_contribution"
                                + " TEXT NOT NULL,teacher_contribution TEXT NOT NULL,state_domain"
                                + " TEXT NOT NULL,feature_hash TEXT NOT NULL,probability_hash TEXT"
                                + " NOT NULL,settled INTEGER NOT NULL DEFAULT 0,settled_at INTEGER"
                                + " DEFAULT 0,PRIMARY KEY(target_issue,position,number))",
                        cols(
                                new String[][] {
                                    {"target_issue", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"number", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"predicted_p", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"actual_label", "INTEGER NOT NULL DEFAULT -1", "1"},
                                    {"number_rank", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"expert_contribution", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"teacher_contribution", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"state_domain", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"feature_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"probability_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"settled", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"settled_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_number_issue_settled",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_number_issue_settled ON"
                                            + " "
                                                + TABLE_NUMBER
                                                + "(target_issue,settled)"
                                    },
                                    {
                                        "idx_v50_number_domain",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_number_domain ON "
                                                + TABLE_NUMBER
                                                + "(state_domain,position,number,settled)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_ACTION,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_ACTION
                                + " (target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1"
                                + " INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT"
                                + " NULL,raw_action_p REAL NOT NULL,third_p REAL NOT"
                                + " NULL,third_margin REAL NOT NULL,entropy REAL NOT"
                                + " NULL,effective_consensus REAL NOT NULL,rank_score REAL NOT"
                                + " NULL,rank_adjusted_p REAL NOT NULL,action_rank INTEGER NOT"
                                + " NULL,lifecycle TEXT NOT NULL,state_domain TEXT NOT"
                                + " NULL,fingerprint TEXT NOT NULL,action_hash TEXT NOT"
                                + " NULL,actual_hit INTEGER DEFAULT -1,settled INTEGER DEFAULT"
                                + " 0,settled_at INTEGER DEFAULT 0,PRIMARY"
                                + " KEY(target_issue,position))",
                        cols(
                                new String[][] {
                                    {"target_issue", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"n1", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"n2", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"n3", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"raw_action_p", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"third_p", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"third_margin", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"entropy", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"effective_consensus", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"rank_score", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"rank_adjusted_p", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"action_rank", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"lifecycle", "TEXT NOT NULL DEFAULT 'UNKNOWN'", "1"},
                                    {"state_domain", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"fingerprint", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"action_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"actual_hit", "INTEGER NOT NULL DEFAULT -1", "1"},
                                    {"settled", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"settled_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_action_issue_rank",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_action_issue_rank ON "
                                                + TABLE_ACTION
                                                + "(target_issue,action_rank)"
                                    },
                                    {
                                        "idx_v50_action_fingerprint",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_action_fingerprint ON "
                                                + TABLE_ACTION
                                                + "(fingerprint,target_issue)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_SELECTED,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_SELECTED
                                + " (target_issue TEXT PRIMARY KEY,decision_id TEXT NOT"
                                + " NULL,selected_position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2"
                                + " INTEGER NOT NULL,n3 INTEGER NOT NULL,raw_action_p REAL NOT"
                                + " NULL,rank_adjusted_p REAL NOT NULL,selected_hit_p REAL NOT"
                                + " NULL,uncertainty REAL NOT NULL,domain_status TEXT NOT NULL,lane"
                                + " TEXT NOT NULL,decision_action TEXT NOT NULL,primary_cause TEXT"
                                + " NOT NULL,secondary_cause TEXT NOT NULL,third_cause TEXT NOT"
                                + " NULL,counter_evidence TEXT NOT NULL,necessary_condition TEXT"
                                + " NOT NULL,falsifier_condition TEXT NOT NULL,contract_hash TEXT"
                                + " NOT NULL,authorization_signature TEXT NOT"
                                + " NULL,authorization_owner TEXT NOT NULL,strategy_cohort TEXT NOT"
                                + " NULL,probability_owner TEXT NOT NULL,rank_owner TEXT NOT"
                                + " NULL,lane_owner TEXT NOT NULL,real_trade INTEGER NOT NULL"
                                + " DEFAULT 0,settled INTEGER NOT NULL DEFAULT 0,result TEXT NOT"
                                + " NULL DEFAULT '',pnl REAL NOT NULL DEFAULT 0,created_at INTEGER"
                                + " NOT NULL,settled_at INTEGER DEFAULT 0)",
                        cols(
                                new String[][] {
                                    {"target_issue", "TEXT PRIMARY KEY", "1"},
                                    {"decision_id", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"selected_position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"n1", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"n2", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"n3", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"raw_action_p", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"rank_adjusted_p", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"selected_hit_p", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"uncertainty", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"domain_status", "TEXT NOT NULL DEFAULT 'WATCH'", "1"},
                                    {"lane", "TEXT NOT NULL DEFAULT 'SHADOW'", "1"},
                                    {"decision_action", "TEXT NOT NULL DEFAULT 'HOLD'", "1"},
                                    {"primary_cause", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"secondary_cause", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"third_cause", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"counter_evidence", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"necessary_condition", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"falsifier_condition", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"contract_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"authorization_signature", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"authorization_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"strategy_cohort", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"probability_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"rank_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"lane_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"real_trade", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"settled", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"result", "TEXT NOT NULL DEFAULT ''", "0"},
                                    {"pnl", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"created_at", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"settled_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_selected_cohort",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_selected_cohort ON "
                                                + TABLE_SELECTED
                                                + "(strategy_cohort,real_trade,settled,target_issue)"
                                    },
                                    {
                                        "idx_v50_selected_lane",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_selected_lane ON "
                                                + TABLE_SELECTED
                                                + "(lane,decision_action,settled,target_issue)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_AUTH,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_AUTH
                                + " (decision_id TEXT PRIMARY KEY,target_issue TEXT NOT"
                                + " NULL,contract_hash TEXT NOT NULL,authorization_signature TEXT"
                                + " NOT NULL,authorization_owner TEXT NOT NULL,strategy_cohort TEXT"
                                + " NOT NULL,probability_owner TEXT NOT NULL,rank_owner TEXT NOT"
                                + " NULL,lane_owner TEXT NOT NULL,bet_owner TEXT NOT"
                                + " NULL,created_at INTEGER NOT NULL)",
                        cols(
                                new String[][] {
                                    {"decision_id", "TEXT PRIMARY KEY", "1"},
                                    {"target_issue", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"contract_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"authorization_signature", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"authorization_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"strategy_cohort", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"probability_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"rank_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"lane_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"bet_owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"created_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_auth_issue",
                                        "CREATE UNIQUE INDEX IF NOT EXISTS idx_v50_auth_issue ON "
                                                + TABLE_AUTH
                                                + "(target_issue)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_STATE_HISTORY,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_STATE_HISTORY
                                + " (target_issue TEXT NOT NULL,position INTEGER NOT NULL,prototype"
                                + " TEXT NOT NULL,lifecycle TEXT NOT NULL,domain_id TEXT NOT"
                                + " NULL,coarse_run INTEGER NOT NULL,medium_run INTEGER NOT"
                                + " NULL,fine_run INTEGER NOT NULL,hazard REAL NOT"
                                + " NULL,transition_risk REAL NOT NULL,confidence REAL NOT"
                                + " NULL,state_hash TEXT NOT NULL,created_at INTEGER NOT"
                                + " NULL,PRIMARY KEY(target_issue,position))",
                        cols(
                                new String[][] {
                                    {"target_issue", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"prototype", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"lifecycle", "TEXT NOT NULL DEFAULT 'UNKNOWN'", "1"},
                                    {"domain_id", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"coarse_run", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"medium_run", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"fine_run", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"hazard", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"transition_risk", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"confidence", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"state_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"created_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_state_domain",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_state_domain ON "
                                                + TABLE_STATE_HISTORY
                                                + "(domain_id,position,target_issue)"
                                    },
                                    {
                                        "idx_v50_state_lifecycle",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_state_lifecycle ON "
                                                + TABLE_STATE_HISTORY
                                                + "(prototype,lifecycle,position,target_issue)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_TEACHER,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_TEACHER
                                + " (evidence_level TEXT NOT NULL,state_domain TEXT NOT"
                                + " NULL,lifecycle TEXT NOT NULL,position INTEGER NOT NULL,number"
                                + " INTEGER NOT NULL,sample INTEGER NOT NULL DEFAULT 0,hits INTEGER"
                                + " NOT NULL DEFAULT 0,recent_sample INTEGER NOT NULL DEFAULT"
                                + " 0,recent_hits INTEGER NOT NULL DEFAULT 0,oos_sample INTEGER NOT"
                                + " NULL DEFAULT 0,real_sample INTEGER NOT NULL DEFAULT"
                                + " 0,shadow_sample INTEGER NOT NULL DEFAULT 0,block0_sample"
                                + " INTEGER NOT NULL DEFAULT 0,block0_hits INTEGER NOT NULL DEFAULT"
                                + " 0,block1_sample INTEGER NOT NULL DEFAULT 0,block1_hits INTEGER"
                                + " NOT NULL DEFAULT 0,block2_sample INTEGER NOT NULL DEFAULT"
                                + " 0,block2_hits INTEGER NOT NULL DEFAULT 0,block3_sample INTEGER"
                                + " NOT NULL DEFAULT 0,block3_hits INTEGER NOT NULL DEFAULT"
                                + " 0,mislead_count INTEGER NOT NULL DEFAULT 0,updated_at INTEGER"
                                + " NOT NULL,PRIMARY"
                                + " KEY(evidence_level,state_domain,lifecycle,position,number))",
                        cols(
                                new String[][] {
                                    {"evidence_level", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"state_domain", "TEXT NOT NULL DEFAULT '*'", "1"},
                                    {"lifecycle", "TEXT NOT NULL DEFAULT 'UNKNOWN'", "1"},
                                    {"position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"number", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"recent_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"recent_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"oos_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"real_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"shadow_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block0_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block0_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block1_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block1_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block2_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block2_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block3_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block3_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"mislead_count", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"updated_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_teacher_lookup",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_teacher_lookup ON "
                                                + TABLE_TEACHER
                                                + "(state_domain,lifecycle,position,number,sample)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_EXPERT,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_EXPERT
                                + " (expert_id TEXT NOT NULL,position INTEGER NOT NULL,state_family"
                                + " TEXT NOT NULL,lifecycle TEXT NOT NULL,domain_id TEXT NOT"
                                + " NULL,uncertainty_bucket TEXT NOT NULL,sample INTEGER NOT NULL"
                                + " DEFAULT 0,log_loss_sum REAL NOT NULL DEFAULT 0,brier_sum REAL"
                                + " NOT NULL DEFAULT 0,top3_hit INTEGER NOT NULL DEFAULT"
                                + " 0,rank_contribution REAL NOT NULL DEFAULT 0,calibration_error"
                                + " REAL NOT NULL DEFAULT 0,recent_loss REAL NOT NULL DEFAULT"
                                + " 0,mislead_run INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL"
                                + " DEFAULT 'WATCH',updated_at INTEGER NOT NULL,PRIMARY"
                                + " KEY(expert_id,position,state_family,lifecycle,domain_id,uncertainty_bucket))",
                        cols(
                                new String[][] {
                                    {"expert_id", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"state_family", "TEXT NOT NULL DEFAULT '*'", "1"},
                                    {"lifecycle", "TEXT NOT NULL DEFAULT 'UNKNOWN'", "1"},
                                    {"domain_id", "TEXT NOT NULL DEFAULT '*'", "1"},
                                    {"uncertainty_bucket", "TEXT NOT NULL DEFAULT 'MID'", "1"},
                                    {"sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"log_loss_sum", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"brier_sum", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"top3_hit", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"rank_contribution", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"calibration_error", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"recent_loss", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"mislead_run", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"status", "TEXT NOT NULL DEFAULT 'WATCH'", "1"},
                                    {"updated_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_expert_lookup",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_expert_lookup ON "
                                                + TABLE_EXPERT
                                                + "(expert_id,position,state_family,lifecycle,domain_id,sample)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_ONLINE_RESIDUAL,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_ONLINE_RESIDUAL
                                + " (domain_id TEXT NOT NULL,lifecycle TEXT NOT NULL,position"
                                + " INTEGER NOT NULL,number INTEGER NOT NULL,sample INTEGER NOT"
                                + " NULL DEFAULT 0,delta REAL NOT NULL DEFAULT 0,mislead_run"
                                + " INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL,PRIMARY"
                                + " KEY(domain_id,lifecycle,position,number))",
                        cols(
                                new String[][] {
                                    {"domain_id", "TEXT NOT NULL DEFAULT '*'", "1"},
                                    {"lifecycle", "TEXT NOT NULL DEFAULT 'UNKNOWN'", "1"},
                                    {"position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"number", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"delta", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"mislead_run", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"updated_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_residual_position",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_residual_position ON "
                                                + TABLE_ONLINE_RESIDUAL
                                                + "(position,number,sample)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_DOMAIN,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_DOMAIN
                                + " (domain_id TEXT PRIMARY KEY,parent_prototype TEXT NOT"
                                + " NULL,lifecycle TEXT NOT NULL,position_scope TEXT NOT"
                                + " NULL,prototype_vector TEXT NOT NULL,sample INTEGER NOT NULL"
                                + " DEFAULT 0,recent_sample INTEGER NOT NULL DEFAULT 0,hit_count"
                                + " INTEGER NOT NULL DEFAULT 0,recent_hit_count INTEGER NOT NULL"
                                + " DEFAULT 0,block0_sample INTEGER NOT NULL DEFAULT 0,block0_hits"
                                + " INTEGER NOT NULL DEFAULT 0,block1_sample INTEGER NOT NULL"
                                + " DEFAULT 0,block1_hits INTEGER NOT NULL DEFAULT 0,block2_sample"
                                + " INTEGER NOT NULL DEFAULT 0,block2_hits INTEGER NOT NULL DEFAULT"
                                + " 0,block3_sample INTEGER NOT NULL DEFAULT 0,block3_hits INTEGER"
                                + " NOT NULL DEFAULT 0,rank1_sample INTEGER NOT NULL DEFAULT"
                                + " 0,rank1_hits INTEGER NOT NULL DEFAULT 0,calibration_error REAL"
                                + " NOT NULL DEFAULT 1,max_lose_streak INTEGER NOT NULL DEFAULT"
                                + " 0,current_lose_streak INTEGER NOT NULL DEFAULT 0,cause_validity"
                                + " REAL NOT NULL DEFAULT 0,counter_failure REAL NOT NULL DEFAULT"
                                + " 0,transition_validity REAL NOT NULL DEFAULT 0,status TEXT NOT"
                                + " NULL DEFAULT 'DISCOVERED',updated_at INTEGER NOT NULL)",
                        cols(
                                new String[][] {
                                    {"domain_id", "TEXT PRIMARY KEY", "1"},
                                    {"parent_prototype", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"lifecycle", "TEXT NOT NULL DEFAULT 'UNKNOWN'", "1"},
                                    {"position_scope", "TEXT NOT NULL DEFAULT '*'", "1"},
                                    {"prototype_vector", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"recent_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"hit_count", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"recent_hit_count", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block0_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block0_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block1_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block1_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block2_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block2_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block3_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"block3_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"rank1_sample", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"rank1_hits", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"calibration_error", "REAL NOT NULL DEFAULT 1", "1"},
                                    {"max_lose_streak", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"current_lose_streak", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"cause_validity", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"counter_failure", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"transition_validity", "REAL NOT NULL DEFAULT 0", "1"},
                                    {"status", "TEXT NOT NULL DEFAULT 'DISCOVERED'", "1"},
                                    {"updated_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_domain_status",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_domain_status ON "
                                                + TABLE_DOMAIN
                                                + "(status,sample,updated_at DESC)"
                                    },
                                    {
                                        "idx_v50_domain_parent",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_domain_parent ON "
                                                + TABLE_DOMAIN
                                                + "(parent_prototype,lifecycle,position_scope)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_ATTRIBUTION,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_ATTRIBUTION
                                + " (target_issue TEXT PRIMARY KEY,attribution_type TEXT NOT"
                                + " NULL,actual_number INTEGER NOT NULL,selected_position INTEGER"
                                + " NOT NULL,selected_hit INTEGER NOT NULL,ablation_blob TEXT NOT"
                                + " NULL,responsibility_blob TEXT NOT NULL,prediction_contract_hash"
                                + " TEXT NOT NULL,created_at INTEGER NOT NULL)",
                        cols(
                                new String[][] {
                                    {"target_issue", "TEXT PRIMARY KEY", "1"},
                                    {"attribution_type", "TEXT NOT NULL DEFAULT 'UNKNOWN'", "1"},
                                    {"actual_number", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"selected_position", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"selected_hit", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"ablation_blob", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"responsibility_blob", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"prediction_contract_hash", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"created_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_attribution_type",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_attribution_type ON "
                                                + TABLE_ATTRIBUTION
                                                + "(attribution_type,created_at DESC)"
                                    }
                                })));

        TABLES.add(
                table(
                        TABLE_PERFORMANCE,
                        "CREATE TABLE IF NOT EXISTS "
                                + TABLE_PERFORMANCE
                                + " (trace_id TEXT NOT NULL,span_id TEXT NOT NULL,parent_span_id"
                                + " TEXT NOT NULL,target_issue TEXT NOT NULL,owner TEXT NOT"
                                + " NULL,started_at_nanos INTEGER NOT NULL,inclusive_nanos INTEGER"
                                + " NOT NULL,exclusive_nanos INTEGER NOT NULL,success INTEGER NOT"
                                + " NULL,error_code TEXT NOT NULL,created_at INTEGER NOT"
                                + " NULL,PRIMARY KEY(trace_id,span_id))",
                        cols(
                                new String[][] {
                                    {"trace_id", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"span_id", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"parent_span_id", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"target_issue", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"owner", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"started_at_nanos", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"inclusive_nanos", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"exclusive_nanos", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"success", "INTEGER NOT NULL DEFAULT 0", "1"},
                                    {"error_code", "TEXT NOT NULL DEFAULT ''", "1"},
                                    {"created_at", "INTEGER NOT NULL DEFAULT 0", "1"}
                                }),
                        indexes(
                                new String[][] {
                                    {
                                        "idx_v50_performance_issue",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_performance_issue ON "
                                                + TABLE_PERFORMANCE
                                                + "(target_issue,created_at DESC)"
                                    },
                                    {
                                        "idx_v50_performance_owner",
                                        "CREATE INDEX IF NOT EXISTS idx_v50_performance_owner ON "
                                                + TABLE_PERFORMANCE
                                                + "(owner,created_at DESC)"
                                    }
                                })));
    }

    private SchemaContractRegistry() {}

    public static List<Table> tables() {
        return Collections.unmodifiableList(TABLES);
    }

    public static String checksum() {
        StringBuilder sb = new StringBuilder(VERSION);
        for (int i = 0; i < TABLES.size(); i++) {
            Table t = TABLES.get(i);
            sb.append('|').append(t.name).append('=').append(t.createSql);
            for (int c = 0; c < t.columns.size(); c++) {
                Column col = t.columns.get(c);
                sb.append('|')
                        .append(col.name)
                        .append(':')
                        .append(col.definition)
                        .append(':')
                        .append(col.required);
            }
            for (int x = 0; x < t.indexes.size(); x++) sb.append('|').append(t.indexes.get(x).sql);
        }
        return V50ChecksumEngine.sha256(sb.toString());
    }

    private static Table table(
            String name, String createSql, List<Column> columns, List<Index> indexes) {
        return new Table(name, createSql, columns, indexes);
    }

    private static List<Column> cols(String[][] values) {
        ArrayList<Column> out = new ArrayList<Column>();
        for (int i = 0; i < values.length; i++) {
            out.add(new Column(values[i][0], values[i][1], "1".equals(values[i][2])));
        }
        return out;
    }

    private static List<Index> indexes(String[][] values) {
        ArrayList<Index> out = new ArrayList<Index>();
        for (int i = 0; i < values.length; i++) out.add(new Index(values[i][0], values[i][1]));
        return out;
    }

    private static List<Index> none() {
        return new ArrayList<Index>();
    }

    /* STARCORE_TIANSHU_B02_MATERIALIZED_20260805 */
    public static final String B02_VERSION = "STARCORE_TIANSHU_B02_SCHEMA_V1";
    public static final String B02_GAME_CODE = "Binance1-PK10";
    public static final String B02_DATABASE_PARTITION = "starcore_tianshu_binance_k3";

    public static final class B02Column {
        public final String name, definition, owner;
        public final boolean required, immutable, sensitive;

        public B02Column(
                String name,
                String definition,
                String owner,
                boolean required,
                boolean immutable,
                boolean sensitive) {
            this.name = safeB02(name);
            this.definition = safeB02(definition);
            this.owner = safeB02(owner);
            this.required = required;
            this.immutable = immutable;
            this.sensitive = sensitive;
        }

        public String canonical() {
            return name
                    + ':'
                    + definition
                    + ':'
                    + owner
                    + ':'
                    + (required ? 1 : 0)
                    + ':'
                    + (immutable ? 1 : 0)
                    + ':'
                    + (sensitive ? 1 : 0);
        }
    }

    public static final class B02Index {
        public final String name, sql, purpose;
        public final boolean unique;

        public B02Index(String name, String sql, String purpose, boolean unique) {
            this.name = safeB02(name);
            this.sql = safeB02(sql);
            this.purpose = safeB02(purpose);
            this.unique = unique;
        }

        public String canonical() {
            return name + ':' + sql + ':' + purpose + ':' + (unique ? 1 : 0);
        }
    }

    public static final class B02Table {
        public final String name, createSql, owner, partitionKey, primaryKey;
        public final java.util.List<B02Column> columns;
        public final java.util.List<B02Index> indexes;

        public B02Table(
                String name,
                String createSql,
                String owner,
                String partitionKey,
                String primaryKey,
                java.util.List<B02Column> columns,
                java.util.List<B02Index> indexes) {
            this.name = safeB02(name);
            this.createSql = safeB02(createSql);
            this.owner = safeB02(owner);
            this.partitionKey = safeB02(partitionKey);
            this.primaryKey = safeB02(primaryKey);
            this.columns =
                    java.util.Collections.unmodifiableList(
                            new java.util.ArrayList<B02Column>(columns));
            this.indexes =
                    java.util.Collections.unmodifiableList(
                            new java.util.ArrayList<B02Index>(indexes));
            validate();
        }

        private void validate() {
            if (name.length() == 0
                    || createSql.indexOf("CREATE TABLE IF NOT EXISTS") < 0
                    || columns.size() == 0) throw new IllegalArgumentException("B02_SCHEMA_TABLE");
        }

        public String canonical() {
            StringBuilder sb =
                    new StringBuilder(name)
                            .append('|')
                            .append(createSql)
                            .append('|')
                            .append(owner)
                            .append('|')
                            .append(partitionKey)
                            .append('|')
                            .append(primaryKey);
            for (B02Column c : columns) sb.append('|').append(c.canonical());
            for (B02Index x : indexes) sb.append('|').append(x.canonical());
            return sb.toString();
        }
    }

    private static String safeB02(String value) {
        return value == null ? "" : value;
    }

    private static B02Column c(String n, String d, boolean r, boolean i, boolean s) {
        return new B02Column(n, d, "SchemaContractRegistry", r, i, s);
    }

    private static B02Index x(String n, String sql, String purpose, boolean unique) {
        return new B02Index(n, sql, purpose, unique);
    }

    public static java.util.List<B02Table> b02Tables() {
        java.util.ArrayList<B02Table> out = new java.util.ArrayList<B02Table>();
        {
            java.util.ArrayList<B02Column> cs = new java.util.ArrayList<B02Column>();
            cs.add(c("game_code", "TEXT NOT NULL", true, true, false));
            cs.add(c("provider_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("gmt_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("raw_open_time", "INTEGER NOT NULL", true, true, false));
            cs.add(c("provider_open_time", "INTEGER NOT NULL", true, true, false));
            cs.add(c("fetch_time", "INTEGER NOT NULL", true, true, false));
            cs.add(c("source_line", "TEXT NOT NULL", true, true, false));
            cs.add(c("canonical_draw_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("raw_payload_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("numbers_blob", "TEXT NOT NULL", true, true, true));
            cs.add(c("dirty", "INTEGER NOT NULL DEFAULT 0", true, true, false));
            cs.add(c("dirty_reason", "TEXT NOT NULL DEFAULT ''", true, true, false));
            cs.add(c("created_at", "INTEGER NOT NULL", true, true, false));
            java.util.ArrayList<B02Index> ix = new java.util.ArrayList<B02Index>();
            ix.add(
                    x(
                            "idx_b02_draw_ledger_game_issue",
                            "CREATE INDEX IF NOT EXISTS idx_b02_draw_ledger_game_issue ON"
                                + " b02_draw_ledger(game_code,issue)",
                            "game partition and issue lookup",
                            false));
            ix.add(
                    x(
                            "uidx_b02_draw_game_issue_hash",
                            "CREATE UNIQUE INDEX IF NOT EXISTS uidx_b02_draw_game_issue_hash ON"
                                + " b02_draw_ledger(game_code,issue,canonical_draw_hash)",
                            "idempotent draw identity",
                            true));
            out.add(
                    new B02Table(
                            "b02_draw_ledger",
                            "CREATE TABLE IF NOT EXISTS b02_draw_ledger (game_code TEXT NOT"
                                + " NULL,provider_issue TEXT NOT NULL,gmt_issue TEXT NOT NULL,issue"
                                + " TEXT NOT NULL,raw_open_time INTEGER NOT NULL,provider_open_time"
                                + " INTEGER NOT NULL,fetch_time INTEGER NOT NULL,source_line TEXT"
                                + " NOT NULL,canonical_draw_hash TEXT NOT NULL,raw_payload_hash"
                                + " TEXT NOT NULL,numbers_blob TEXT NOT NULL,dirty INTEGER NOT NULL"
                                + " DEFAULT 0,dirty_reason TEXT NOT NULL DEFAULT '',created_at"
                                + " INTEGER NOT NULL)",
                            "SchemaContractRegistry",
                            "game_code",
                            "issue",
                            cs,
                            ix));
        }
        {
            java.util.ArrayList<B02Column> cs = new java.util.ArrayList<B02Column>();
            cs.add(c("game_code", "TEXT NOT NULL", true, true, false));
            cs.add(c("issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("first_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("second_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("first_source", "TEXT NOT NULL", true, true, false));
            cs.add(c("second_source", "TEXT NOT NULL", true, true, false));
            cs.add(c("quarantine_reason", "TEXT NOT NULL", true, true, false));
            cs.add(c("created_at", "INTEGER NOT NULL", true, true, false));
            cs.add(c("resolved", "INTEGER NOT NULL DEFAULT 0", true, true, false));
            java.util.ArrayList<B02Index> ix = new java.util.ArrayList<B02Index>();
            ix.add(
                    x(
                            "idx_b02_draw_conflict_game_issue",
                            "CREATE INDEX IF NOT EXISTS idx_b02_draw_conflict_game_issue ON"
                                + " b02_draw_conflict(game_code,issue)",
                            "game partition and issue lookup",
                            false));
            out.add(
                    new B02Table(
                            "b02_draw_conflict",
                            "CREATE TABLE IF NOT EXISTS b02_draw_conflict (game_code TEXT NOT"
                                + " NULL,issue TEXT NOT NULL,first_hash TEXT NOT NULL,second_hash"
                                + " TEXT NOT NULL,first_source TEXT NOT NULL,second_source TEXT NOT"
                                + " NULL,quarantine_reason TEXT NOT NULL,created_at INTEGER NOT"
                                + " NULL,resolved INTEGER NOT NULL DEFAULT 0)",
                            "SchemaContractRegistry",
                            "game_code",
                            "game_code",
                            cs,
                            ix));
        }
        {
            java.util.ArrayList<B02Column> cs = new java.util.ArrayList<B02Column>();
            cs.add(c("recovery_id", "TEXT PRIMARY KEY", true, true, false));
            cs.add(c("game_code", "TEXT NOT NULL", true, true, false));
            cs.add(c("from_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("to_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("request_date", "TEXT NOT NULL", true, true, false));
            cs.add(c("attempt", "INTEGER NOT NULL DEFAULT 0", true, true, false));
            cs.add(c("next_attempt_at", "INTEGER NOT NULL DEFAULT 0", true, true, false));
            cs.add(c("state", "TEXT NOT NULL", true, true, false));
            cs.add(c("result_hash", "TEXT NOT NULL DEFAULT ''", true, true, true));
            cs.add(c("recovered_issues", "TEXT NOT NULL DEFAULT ''", true, true, false));
            cs.add(c("conflict_issues", "TEXT NOT NULL DEFAULT ''", true, true, false));
            cs.add(c("complete", "INTEGER NOT NULL DEFAULT 0", true, true, false));
            cs.add(c("created_at", "INTEGER NOT NULL", true, true, false));
            cs.add(c("updated_at", "INTEGER NOT NULL", true, true, false));
            java.util.ArrayList<B02Index> ix = new java.util.ArrayList<B02Index>();
            ix.add(
                    x(
                            "idx_b02_gap_recovery_game_issue",
                            "CREATE INDEX IF NOT EXISTS idx_b02_gap_recovery_game_issue ON"
                                + " b02_gap_recovery(game_code,target_issue)",
                            "game partition and issue lookup",
                            false));
            out.add(
                    new B02Table(
                            "b02_gap_recovery",
                            "CREATE TABLE IF NOT EXISTS b02_gap_recovery (recovery_id TEXT PRIMARY"
                                + " KEY,game_code TEXT NOT NULL,from_issue TEXT NOT NULL,to_issue"
                                + " TEXT NOT NULL,request_date TEXT NOT NULL,attempt INTEGER NOT"
                                + " NULL DEFAULT 0,next_attempt_at INTEGER NOT NULL DEFAULT 0,state"
                                + " TEXT NOT NULL,result_hash TEXT NOT NULL DEFAULT"
                                + " '',recovered_issues TEXT NOT NULL DEFAULT '',conflict_issues"
                                + " TEXT NOT NULL DEFAULT '',complete INTEGER NOT NULL DEFAULT"
                                + " 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)",
                            "SchemaContractRegistry",
                            "game_code",
                            "recovery_id",
                            cs,
                            ix));
        }
        {
            java.util.ArrayList<B02Column> cs = new java.util.ArrayList<B02Column>();
            cs.add(c("game_code", "TEXT NOT NULL", true, true, false));
            cs.add(c("base_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("target_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("history_max_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("frozen_at", "INTEGER NOT NULL", true, true, false));
            cs.add(c("provider_cutoff_time", "INTEGER NOT NULL", true, true, false));
            cs.add(c("history_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("draw_contract_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("feature_schema_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("state_schema_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("model_bundle_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("visible_issues", "TEXT NOT NULL", true, true, false));
            cs.add(c("future_leak_free", "INTEGER NOT NULL", true, true, false));
            cs.add(c("shadow_only", "INTEGER NOT NULL", true, true, false));
            java.util.ArrayList<B02Index> ix = new java.util.ArrayList<B02Index>();
            ix.add(
                    x(
                            "idx_b02_pit_snapshot_game_issue",
                            "CREATE INDEX IF NOT EXISTS idx_b02_pit_snapshot_game_issue ON"
                                + " b02_pit_snapshot(game_code,target_issue)",
                            "game partition and issue lookup",
                            false));
            out.add(
                    new B02Table(
                            "b02_pit_snapshot",
                            "CREATE TABLE IF NOT EXISTS b02_pit_snapshot (game_code TEXT NOT"
                                + " NULL,base_issue TEXT NOT NULL,target_issue TEXT NOT"
                                + " NULL,history_max_issue TEXT NOT NULL,frozen_at INTEGER NOT"
                                + " NULL,provider_cutoff_time INTEGER NOT NULL,history_hash TEXT"
                                + " NOT NULL,draw_contract_hash TEXT NOT NULL,feature_schema_hash"
                                + " TEXT NOT NULL,state_schema_hash TEXT NOT NULL,model_bundle_hash"
                                + " TEXT NOT NULL,visible_issues TEXT NOT NULL,future_leak_free"
                                + " INTEGER NOT NULL,shadow_only INTEGER NOT NULL)",
                            "SchemaContractRegistry",
                            "game_code",
                            "game_code",
                            cs,
                            ix));
        }
        {
            java.util.ArrayList<B02Column> cs = new java.util.ArrayList<B02Column>();
            cs.add(c("packet_id", "TEXT PRIMARY KEY", true, true, false));
            cs.add(c("game_code", "TEXT NOT NULL", true, true, false));
            cs.add(c("base_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("target_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("created_at", "INTEGER NOT NULL", true, true, false));
            cs.add(c("pit_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("draw_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("feature_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("state_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("expert_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("number_probability_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("action_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("certificate_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("evidence_ids", "TEXT NOT NULL", true, true, true));
            cs.add(c("counter_evidence_ids", "TEXT NOT NULL", true, true, true));
            cs.add(c("complete", "INTEGER NOT NULL", true, true, false));
            cs.add(c("shadow_only", "INTEGER NOT NULL", true, true, false));
            java.util.ArrayList<B02Index> ix = new java.util.ArrayList<B02Index>();
            ix.add(
                    x(
                            "idx_b02_evidence_packet_game_issue",
                            "CREATE INDEX IF NOT EXISTS idx_b02_evidence_packet_game_issue ON"
                                + " b02_evidence_packet(game_code,target_issue)",
                            "game partition and issue lookup",
                            false));
            out.add(
                    new B02Table(
                            "b02_evidence_packet",
                            "CREATE TABLE IF NOT EXISTS b02_evidence_packet (packet_id TEXT PRIMARY"
                                + " KEY,game_code TEXT NOT NULL,base_issue TEXT NOT"
                                + " NULL,target_issue TEXT NOT NULL,created_at INTEGER NOT"
                                + " NULL,pit_hash TEXT NOT NULL,draw_hash TEXT NOT"
                                + " NULL,feature_hash TEXT NOT NULL,state_hash TEXT NOT"
                                + " NULL,expert_hash TEXT NOT NULL,number_probability_hash TEXT NOT"
                                + " NULL,action_hash TEXT NOT NULL,certificate_hash TEXT NOT"
                                + " NULL,evidence_ids TEXT NOT NULL,counter_evidence_ids TEXT NOT"
                                + " NULL,complete INTEGER NOT NULL,shadow_only INTEGER NOT NULL)",
                            "SchemaContractRegistry",
                            "game_code",
                            "packet_id",
                            cs,
                            ix));
        }
        {
            java.util.ArrayList<B02Column> cs = new java.util.ArrayList<B02Column>();
            cs.add(c("game_code", "TEXT NOT NULL", true, true, false));
            cs.add(c("target_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("action_id", "TEXT NOT NULL", true, true, false));
            cs.add(c("position", "INTEGER NOT NULL", true, true, false));
            cs.add(c("n1", "INTEGER NOT NULL", true, true, false));
            cs.add(c("n2", "INTEGER NOT NULL", true, true, false));
            cs.add(c("n3", "INTEGER NOT NULL", true, true, false));
            cs.add(c("action_index", "INTEGER NOT NULL", true, true, false));
            cs.add(c("raw_probability", "REAL NOT NULL", true, true, false));
            cs.add(c("calibrated_probability", "REAL NOT NULL", true, true, false));
            cs.add(c("selected_hit_p", "REAL NOT NULL", true, true, false));
            cs.add(c("lane", "TEXT NOT NULL", true, true, false));
            cs.add(c("evidence_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("shadow_only", "INTEGER NOT NULL", true, true, false));
            cs.add(c("created_at", "INTEGER NOT NULL", true, true, false));
            java.util.ArrayList<B02Index> ix = new java.util.ArrayList<B02Index>();
            ix.add(
                    x(
                            "idx_b02_action_ledger_game_issue",
                            "CREATE INDEX IF NOT EXISTS idx_b02_action_ledger_game_issue ON"
                                + " b02_action_ledger(game_code,target_issue)",
                            "game partition and issue lookup",
                            false));
            out.add(
                    new B02Table(
                            "b02_action_ledger",
                            "CREATE TABLE IF NOT EXISTS b02_action_ledger (game_code TEXT NOT"
                                + " NULL,target_issue TEXT NOT NULL,action_id TEXT NOT"
                                + " NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER"
                                + " NOT NULL,n3 INTEGER NOT NULL,action_index INTEGER NOT"
                                + " NULL,raw_probability REAL NOT NULL,calibrated_probability REAL"
                                + " NOT NULL,selected_hit_p REAL NOT NULL,lane TEXT NOT"
                                + " NULL,evidence_hash TEXT NOT NULL,shadow_only INTEGER NOT"
                                + " NULL,created_at INTEGER NOT NULL)",
                            "SchemaContractRegistry",
                            "game_code",
                            "game_code",
                            cs,
                            ix));
        }
        {
            java.util.ArrayList<B02Column> cs = new java.util.ArrayList<B02Column>();
            cs.add(c("certificate_id", "TEXT PRIMARY KEY", true, true, false));
            cs.add(c("game_code", "TEXT NOT NULL", true, true, false));
            cs.add(c("target_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("domain_id", "TEXT NOT NULL", true, true, false));
            cs.add(c("sample", "INTEGER NOT NULL", true, true, false));
            cs.add(c("oos_hit_rate", "REAL NOT NULL", true, true, false));
            cs.add(c("oos_coverage", "REAL NOT NULL", true, true, false));
            cs.add(c("ece", "REAL NOT NULL", true, true, false));
            cs.add(c("block_results", "TEXT NOT NULL", true, true, false));
            cs.add(c("artifact_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("certificate_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("proven", "INTEGER NOT NULL", true, true, false));
            cs.add(c("shadow_only", "INTEGER NOT NULL", true, true, false));
            cs.add(c("created_at", "INTEGER NOT NULL", true, true, false));
            java.util.ArrayList<B02Index> ix = new java.util.ArrayList<B02Index>();
            ix.add(
                    x(
                            "idx_b02_certificate_game_issue",
                            "CREATE INDEX IF NOT EXISTS idx_b02_certificate_game_issue ON"
                                + " b02_certificate(game_code,target_issue)",
                            "game partition and issue lookup",
                            false));
            out.add(
                    new B02Table(
                            "b02_certificate",
                            "CREATE TABLE IF NOT EXISTS b02_certificate (certificate_id TEXT"
                                + " PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT NOT"
                                + " NULL,domain_id TEXT NOT NULL,sample INTEGER NOT"
                                + " NULL,oos_hit_rate REAL NOT NULL,oos_coverage REAL NOT NULL,ece"
                                + " REAL NOT NULL,block_results TEXT NOT NULL,artifact_hash TEXT"
                                + " NOT NULL,certificate_hash TEXT NOT NULL,proven INTEGER NOT"
                                + " NULL,shadow_only INTEGER NOT NULL,created_at INTEGER NOT NULL)",
                            "SchemaContractRegistry",
                            "game_code",
                            "certificate_id",
                            cs,
                            ix));
        }
        {
            java.util.ArrayList<B02Column> cs = new java.util.ArrayList<B02Column>();
            cs.add(c("finding_id", "TEXT PRIMARY KEY", true, true, false));
            cs.add(c("game_code", "TEXT NOT NULL", true, true, false));
            cs.add(c("target_issue", "TEXT NOT NULL", true, true, false));
            cs.add(c("severity", "TEXT NOT NULL", true, true, false));
            cs.add(c("owner", "TEXT NOT NULL", true, true, false));
            cs.add(c("code", "TEXT NOT NULL", true, true, false));
            cs.add(c("summary", "TEXT NOT NULL", true, true, false));
            cs.add(c("evidence_hash", "TEXT NOT NULL", true, true, true));
            cs.add(c("repair_action", "TEXT NOT NULL", true, true, false));
            cs.add(c("created_at", "INTEGER NOT NULL", true, true, false));
            cs.add(c("blocks_production", "INTEGER NOT NULL", true, true, false));
            java.util.ArrayList<B02Index> ix = new java.util.ArrayList<B02Index>();
            ix.add(
                    x(
                            "idx_b02_diagnostic_finding_game_issue",
                            "CREATE INDEX IF NOT EXISTS idx_b02_diagnostic_finding_game_issue ON"
                                + " b02_diagnostic_finding(game_code,target_issue)",
                            "game partition and issue lookup",
                            false));
            out.add(
                    new B02Table(
                            "b02_diagnostic_finding",
                            "CREATE TABLE IF NOT EXISTS b02_diagnostic_finding (finding_id TEXT"
                                + " PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT NOT"
                                + " NULL,severity TEXT NOT NULL,owner TEXT NOT NULL,code TEXT NOT"
                                + " NULL,summary TEXT NOT NULL,evidence_hash TEXT NOT"
                                + " NULL,repair_action TEXT NOT NULL,created_at INTEGER NOT"
                                + " NULL,blocks_production INTEGER NOT NULL)",
                            "SchemaContractRegistry",
                            "game_code",
                            "finding_id",
                            cs,
                            ix));
        }
        return java.util.Collections.unmodifiableList(out);
    }

    public static final class B02MigrationInvariant {
        public final String id, tableName, predicate, failureMode;
        public final boolean hard;

        public B02MigrationInvariant(
                String id, String tableName, String predicate, String failureMode, boolean hard) {
            this.id = safeB02(id);
            this.tableName = safeB02(tableName);
            this.predicate = safeB02(predicate);
            this.failureMode = safeB02(failureMode);
            this.hard = hard;
        }

        public String canonical() {
            return id
                    + ':'
                    + tableName
                    + ':'
                    + predicate
                    + ':'
                    + failureMode
                    + ':'
                    + (hard ? 1 : 0);
        }
    }

    public static java.util.List<B02MigrationInvariant> b02MigrationInvariants() {
        java.util.ArrayList<B02MigrationInvariant> out =
                new java.util.ArrayList<B02MigrationInvariant>();
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-001",
                        "b02_draw_ledger",
                        "game_code_isolated",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-002",
                        "b02_draw_conflict",
                        "primary_key_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-003",
                        "b02_gap_recovery",
                        "hash_immutable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-004",
                        "b02_pit_snapshot",
                        "write_idempotent",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-005",
                        "b02_evidence_packet",
                        "no_cross_game_mix",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-006",
                        "b02_action_ledger",
                        "shadow_only_default",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-007",
                        "b02_certificate",
                        "atomic_prediction_settlement",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-008",
                        "b02_diagnostic_finding",
                        "replay_hash_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-009",
                        "b02_draw_ledger",
                        "game_code_isolated",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-010",
                        "b02_draw_conflict",
                        "primary_key_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-011",
                        "b02_gap_recovery",
                        "hash_immutable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-012",
                        "b02_pit_snapshot",
                        "write_idempotent",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-013",
                        "b02_evidence_packet",
                        "no_cross_game_mix",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-014",
                        "b02_action_ledger",
                        "shadow_only_default",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-015",
                        "b02_certificate",
                        "atomic_prediction_settlement",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-016",
                        "b02_diagnostic_finding",
                        "replay_hash_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-017",
                        "b02_draw_ledger",
                        "game_code_isolated",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-018",
                        "b02_draw_conflict",
                        "primary_key_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-019",
                        "b02_gap_recovery",
                        "hash_immutable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-020",
                        "b02_pit_snapshot",
                        "write_idempotent",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-021",
                        "b02_evidence_packet",
                        "no_cross_game_mix",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-022",
                        "b02_action_ledger",
                        "shadow_only_default",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-023",
                        "b02_certificate",
                        "atomic_prediction_settlement",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-024",
                        "b02_diagnostic_finding",
                        "replay_hash_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-025",
                        "b02_draw_ledger",
                        "game_code_isolated",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-026",
                        "b02_draw_conflict",
                        "primary_key_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-027",
                        "b02_gap_recovery",
                        "hash_immutable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-028",
                        "b02_pit_snapshot",
                        "write_idempotent",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-029",
                        "b02_evidence_packet",
                        "no_cross_game_mix",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-030",
                        "b02_action_ledger",
                        "shadow_only_default",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-031",
                        "b02_certificate",
                        "atomic_prediction_settlement",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-032",
                        "b02_diagnostic_finding",
                        "replay_hash_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-033",
                        "b02_draw_ledger",
                        "game_code_isolated",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-034",
                        "b02_draw_conflict",
                        "primary_key_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-035",
                        "b02_gap_recovery",
                        "hash_immutable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-036",
                        "b02_pit_snapshot",
                        "write_idempotent",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-037",
                        "b02_evidence_packet",
                        "no_cross_game_mix",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-038",
                        "b02_action_ledger",
                        "shadow_only_default",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-039",
                        "b02_certificate",
                        "atomic_prediction_settlement",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-040",
                        "b02_diagnostic_finding",
                        "replay_hash_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-041",
                        "b02_draw_ledger",
                        "game_code_isolated",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-042",
                        "b02_draw_conflict",
                        "primary_key_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-043",
                        "b02_gap_recovery",
                        "hash_immutable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-044",
                        "b02_pit_snapshot",
                        "write_idempotent",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-045",
                        "b02_evidence_packet",
                        "no_cross_game_mix",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-046",
                        "b02_action_ledger",
                        "shadow_only_default",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-047",
                        "b02_certificate",
                        "atomic_prediction_settlement",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-048",
                        "b02_diagnostic_finding",
                        "replay_hash_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-049",
                        "b02_draw_ledger",
                        "game_code_isolated",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-050",
                        "b02_draw_conflict",
                        "primary_key_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-051",
                        "b02_gap_recovery",
                        "hash_immutable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-052",
                        "b02_pit_snapshot",
                        "write_idempotent",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-053",
                        "b02_evidence_packet",
                        "no_cross_game_mix",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-054",
                        "b02_action_ledger",
                        "shadow_only_default",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-055",
                        "b02_certificate",
                        "atomic_prediction_settlement",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-056",
                        "b02_diagnostic_finding",
                        "replay_hash_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-057",
                        "b02_draw_ledger",
                        "game_code_isolated",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-058",
                        "b02_draw_conflict",
                        "primary_key_stable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-059",
                        "b02_gap_recovery",
                        "hash_immutable",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-060",
                        "b02_pit_snapshot",
                        "write_idempotent",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-061",
                        "b02_evidence_packet",
                        "no_cross_game_mix",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-062",
                        "b02_action_ledger",
                        "shadow_only_default",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-063",
                        "b02_certificate",
                        "atomic_prediction_settlement",
                        "ABORT_MIGRATION",
                        true));
        out.add(
                new B02MigrationInvariant(
                        "B02-MIG-064",
                        "b02_diagnostic_finding",
                        "replay_hash_stable",
                        "ABORT_MIGRATION",
                        true));
        return java.util.Collections.unmodifiableList(out);
    }

    public static String b02Checksum() {
        StringBuilder sb = new StringBuilder(B02_VERSION);
        for (B02Table t : b02Tables()) sb.append('|').append(t.canonical());
        for (B02MigrationInvariant i : b02MigrationInvariants())
            sb.append('|').append(i.canonical());
        return V50ChecksumEngine.sha256(sb.toString());
    }

    public static boolean b02SchemaSelfTest() {
        java.util.List<B02Table> ts = b02Tables();
        if (ts.size() != 8 || b02MigrationInvariants().size() != 64) return false;
        java.util.HashSet<String> names = new java.util.HashSet<String>();
        for (B02Table t : ts) {
            if (!names.add(t.name) || t.createSql.indexOf("game_code") < 0) return false;
        }
        return b02Checksum().length() == 64;
    }
}
