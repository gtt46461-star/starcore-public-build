#!/usr/bin/env python3
"""Compile complete SQLite statements and the pinned atom catalog into Java; --check is read-only."""
import argparse, json, re, sqlite3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'

def statements(path):
    result=[];pending=''
    for line in path.read_text().splitlines(True):
        pending+=line
        if sqlite3.complete_statement(pending):
            sql=re.sub(r'--[^\n]*','',pending).strip()
            if sql: result.append(sql)
            pending=''
    if re.sub(r'--[^\n]*','',pending).strip():raise ValueError('incomplete SQL')
    return result

def outputs():
    sql=[]
    for name in ['strategy_su1.sql','strategy_su1_foundry.sql','strategy_su1_runtime.sql']:
        sql+=statements(ROOT/'schema'/name)
    objects=[]
    for s in sql:
        m=re.match(r'CREATE (?:UNIQUE )?(?:TABLE|INDEX|TRIGGER) IF NOT EXISTS (\w+)',s)
        if m:objects.append(m[1])
    out='package com.ethk3.novaonepro.strategyv2;\n/** Generated from reviewed schema/strategy_su1*.sql; never split on semicolons. */\npublic final class SqlStatementsSU1 {\n private SqlStatementsSU1(){}\n'
    out+=' public static final String[] SQL={\n'+',\n'.join(json.dumps(s,ensure_ascii=False) for s in sql)+'\n };\n'
    out+=' public static final String[] OBJECTS={'+','.join(json.dumps(s) for s in objects)+'};\n}\n'
    cat=json.loads((ROOT/'app/src/main/assets/strategy_su1/atom_catalog_lifted.json').read_text())
    def key(p):return ';'.join(k+'='+str(v) for k,v in p)
    score=['ATOM('+f+','+key(p)+')' for f,p in cat['score']]
    cond=['ATOMC('+f+','+str(q)+','+key(p)+')' for f,q,p in cat['cond']]
    def array(values):return ','.join(json.dumps(s) for s in values)
    atom='package com.ethk3.novaonepro.strategyv2;\nimport java.util.*;\n/** Generated from the pinned production atom catalog by generate_strategy_su1_schema.py. */\npublic final class AtomCatalogSU1 {\n private AtomCatalogSU1(){}\n'
    atom+=' private static final String[] SCORES={'+array(score)+'};\n private static final String[] CONDITIONS={'+array(cond)+'};\n'
    atom+=' private static final Set<String> SCORE_KEYS=new HashSet<>(Arrays.asList('+array([f+'|'+key(p) for f,p in cat['score']])+'));\n'
    atom+=' private static final Set<String> CONDITION_KEYS=new HashSet<>(Arrays.asList('+array(sorted(set(f+'|'+key(p) for f,q,p in cat['cond'])))+'));\n'
    atom+=''' public static List<String> scoreAtoms(){return Collections.unmodifiableList(Arrays.asList(SCORES.clone()));}
 public static List<String> conditions(){return Collections.unmodifiableList(Arrays.asList(CONDITIONS.clone()));}
 public static void requireScore(String family,String params){if(!SCORE_KEYS.contains(family+"|"+params))throw new IllegalArgumentException("SU1_UNREGISTERED_SCORE_ATOM");}
 public static void requireCondition(String family,String params){if(!CONDITION_KEYS.contains(family+"|"+params))throw new IllegalArgumentException("SU1_UNREGISTERED_CONDITION_ATOM");}
 public static List<String> sampleConditions(Random rng,int n){List<String> all=new ArrayList<>(conditions());Collections.shuffle(all,rng);return all.subList(0,Math.min(n,all.size()));}
}
'''
    return {'SqlStatementsSU1.java':out,'AtomCatalogSU1.java':atom}

def main():
    p=argparse.ArgumentParser();p.add_argument('--check',action='store_true');a=p.parse_args()
    for name,text in outputs().items():
        path=JAVA/name
        if a.check:
            if not path.is_file() or path.read_text()!=text:raise SystemExit('SU1_GENERATED_DRIFT:'+name)
        else:path.write_text(text)
    print('SU1_SCHEMA_AND_CATALOG_VERIFIED' if a.check else 'SU1_SCHEMA_AND_CATALOG_GENERATED')
if __name__=='__main__':main()
