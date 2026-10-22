package com.ethk3.novaonepro;

import java.util.Locale;

/** Predraw-only market-regime router. Runtime health is a separate namespace and never a market label. */
public final class DomainRouterR929 {
    public static final String REVISION="R932_DOMAIN_ROUTER_V2";
    public static final int MIN_CAL_SUPPORT=120;
    public static final int DEFAULT_HYSTERESIS_ISSUES=3;
    private DomainRouterR929(){}

    public enum Route { DOMAIN, GLOBAL_FALLBACK, PAUSE }
    public static final class DomainEvidence {
        public final String domainId,domainSchemaHash,certificateHash;
        public final int calibrationN,futureN; public final boolean certificateActive;
        public final double ece,pairedDelta;
        public DomainEvidence(String domainId,String domainSchemaHash,String certificateHash,int calibrationN,
                              int futureN,boolean certificateActive,double ece,double pairedDelta){
            this.domainId=required(domainId);this.domainSchemaHash=hash(domainSchemaHash);this.certificateHash=hash(certificateHash);
            this.calibrationN=Math.max(0,calibrationN);this.futureN=Math.max(0,futureN);this.certificateActive=certificateActive;
            this.ece=ece;this.pairedDelta=pairedDelta;
        }
        public boolean supported(){return certificateActive&&calibrationN>=MIN_CAL_SUPPORT&&futureN>=120&&finite(pairedDelta)&&pairedDelta>0&&finite(ece)&&ece>=0&&ece<=0.04d;}
    }
    public static final class Decision {
        public final Route route; public final String domainId,reason,routeHash; public final double weightMultiplier;
        Decision(Route route,String domainId,String reason,double weightMultiplier,String schemaHash,String targetIssue){
            this.route=route;this.domainId=domainId;this.reason=reason;this.weightMultiplier=weightMultiplier;
            this.routeHash=V51ChecksumEngine.sha256(REVISION+'|'+route+'|'+domainId+'|'+reason+'|'+weightMultiplier+'|'+schemaHash+'|'+targetIssue);
        }
    }

    public static Decision route(final DbStore store,final String targetIssue,final V51Models.StateTrajectorySnapshot state,
                                 final DomainEvidence evidence,final boolean runtimeHealthy){
        final String target=required(targetIssue);
        final String schema=evidence==null?V51ChecksumEngine.sha256("NO_DOMAIN_SCHEMA"):evidence.domainSchemaHash;
        if(state==null)return new Decision(Route.PAUSE,"UNKNOWN","STATE_MISSING",0.0d,schema,target);
        if(!target.equals(state.targetIssue)||state.position<1||state.position>10)
            return new Decision(Route.PAUSE,"UNKNOWN","STATE_TARGET_OR_POSITION_MISMATCH",0,schema,target);
        final String current=required(state.domainId);
        final String binding=V51ChecksumEngine.sha256(schema+'|'+state.checksum+'|'+current+'|'+runtimeHealthy+'|'
                +(evidence==null?"NO_CERT":evidence.certificateHash+'|'+evidence.calibrationN+'|'+evidence.futureN+'|'+evidence.ece+'|'+evidence.pairedDelta+'|'+evidence.certificateActive));
        if(store==null)return new Decision(Route.GLOBAL_FALLBACK,current,"PERSISTENCE_REQUIRED_FOR_HYSTERESIS",0,binding,target);
        Decision result=store.coordinatorR36().runAtomicWrite("R932_DOMAIN_ROUTE",new DatabaseCoordinator.WriteCallable<Decision>(){
            public Decision call(android.database.sqlite.SQLiteDatabase db){
                String key="r932_domain_position_"+state.position+"_";
                String last=read(db,key+"issue");
                if(target.equals(last)){
                    if(!binding.equals(read(db,key+"binding")))return new Decision(Route.PAUSE,current,"SAME_ISSUE_INPUT_DRIFT",0,binding,target);
                    Route prior=Route.valueOf(read(db,key+"route"));String reason=read(db,key+"reason");
                    return new Decision(prior,current,reason,prior==Route.DOMAIN?1:0,binding,target);
                }
                if(last.length()>0&&PitFeatureDagR929.compareIssues(target,last)<0)
                    return new Decision(Route.PAUSE,current,"OUT_OF_ORDER_ROUTE",0,binding,target);
                boolean sameSchema=schema.equals(read(db,key+"schema"));
                String prior=sameSchema?read(db,key+"active"):"",candidate=sameSchema?read(db,key+"candidate"):"";
                int streak=sameSchema?parseInt(read(db,key+"streak"),0):0;
                boolean supported=evidence!=null&&current.equals(evidence.domainId)&&evidence.supported();
                Route route;String reason;
                if(!runtimeHealthy){route=Route.PAUSE;reason="RUNTIME_HEALTH_NAMESPACE_PAUSE";streak=0;}
                else if(!supported){route=Route.GLOBAL_FALLBACK;reason="DOMAIN_UNSUPPORTED_OR_CERT_MISMATCH";streak=0;}
                else{
                    streak=current.equals(candidate)?Math.min(DEFAULT_HYSTERESIS_ISSUES,streak+1):1;
                    if(current.equals(prior)||streak>=DEFAULT_HYSTERESIS_ISSUES){route=Route.DOMAIN;reason="SUPPORTED_PREDRAW_DOMAIN";prior=current;}
                    else{route=Route.GLOBAL_FALLBACK;reason="HYSTERESIS_WAIT_"+streak;}
                }
                java.util.Map<String,String> values=new java.util.LinkedHashMap<String,String>();
                values.put(key+"issue",target);values.put(key+"binding",binding);values.put(key+"schema",schema);
                values.put(key+"active",prior);values.put(key+"candidate",supported?current:"");values.put(key+"streak",String.valueOf(streak));
                values.put(key+"route",route.name());values.put(key+"reason",reason);DbStore.putStatesR36(db,values);
                return new Decision(route,current,reason,route==Route.DOMAIN?1:0,binding,target);
            }},null);
        if(result==null)throw new IllegalStateException("R932_DOMAIN_ROUTE_COMMIT_FAILED");return result;
    }
    public static String installedSchemaHash(){return V51ChecksumEngine.sha256(REVISION+'|'+V51StateModelArtifact.ARTIFACT_ID+'|'
        +java.util.Arrays.deepToString(V51StateModelArtifact.CENTERS)+'|'+java.util.Arrays.toString(V51StateModelArtifact.SCALE)+'|'
        +java.util.Arrays.toString(V51StateModelArtifact.IDS)+"|HYSTERESIS="+DEFAULT_HYSTERESIS_ISSUES);}
    private static String read(android.database.sqlite.SQLiteDatabase db,String key){
        try(android.database.Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{key})){
            return c.moveToFirst()&&!c.isNull(0)?c.getString(0):"";
        }
    }

    public static void requirePredraw(String maxSourceIssue,String targetIssue,long availableAt,long drawAt){
        FeatureDagContractR929.requirePointInTime(maxSourceIssue,targetIssue,availableAt,drawAt);
    }
    private static int parseInt(String x,int d){try{return Integer.parseInt(x);}catch(Throwable e){return d;}}
    private static String required(String x){String v=x==null?"":x.trim();if(v.length()==0)throw new IllegalArgumentException("R929_DOMAIN_REQUIRED");return v;}
    private static String hash(String x){String v=required(x).toLowerCase(Locale.US);if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R929_DOMAIN_HASH");return v;}
    private static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}
}
