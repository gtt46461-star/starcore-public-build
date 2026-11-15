package com.ethk3.novaonepro.r952;

import android.database.sqlite.SQLiteDatabase;

public final class CausalEvidenceLedgerR952 {private CausalEvidenceLedgerR952(){}public static void put(SQLiteDatabase db,CausalEdgeR952 e){db.execSQL("INSERT OR REPLACE INTO r952_causal_edge(edge_hash,source,target,relation_type,temporal_direction,hypothesis_hash,evidence_hash,confidence,status,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{e.edgeHash,e.source,e.target,e.relationType,e.temporalDirection,e.hypothesisHash,e.evidenceHash,Double.valueOf(e.confidence),e.status.name(),Long.valueOf(System.currentTimeMillis())});}}
