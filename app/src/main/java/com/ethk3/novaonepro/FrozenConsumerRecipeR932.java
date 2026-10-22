package com.ethk3.novaonepro;

import java.util.*;
import org.json.*;

/** Frozen predraw consumer program/state and the shared production/replay execution kernels.
 * No certificate lookup, database access, settlement, or REAL authority exists in this owner. */
public final class FrozenConsumerRecipeR932 {
    static final String REVISION="CP11_FROZEN_CONSUMER_COMPACT_V1";
    static final String PRIOR_REVISION="R947_FROZEN_CONSUMER_CERT_BINDING_V2";
    static final String LEGACY_REVISION="R932_FROZEN_CONSUMER_V1";
    private FrozenConsumerRecipeR932(){}
    static final class Mixed {
        final V51Models.PositionAction[] actions;final V51Models.PositionAction rankOne;
        Mixed(V51Models.PositionAction[] actions,V51Models.PositionAction rankOne){this.actions=actions;this.rankOne=rankOne;}
    }
    static final class Recipe {
        final String revision,lane,id,spec,state,structure,evidence,certificate,base,target,historyHash,surfaceHash,encoded;
        final String evaluationRule,evaluationEpoch,contextJson,contextHash;
        final double weight;final int historyRows;final long elapsedMs;
        final HistoricalStrategyInventionR908.Profile profile;
        final List<Models.Draw> frozenHistory;
        Recipe(JSONObject root)throws Exception {
            RecipeBudgetScopeCP12.beginRecipe();
            if(root.getString("spec").getBytes(java.nio.charset.StandardCharsets.UTF_8).length>32768||root.getString("state").getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144)throw new IllegalArgumentException("RECIPE_MODEL_BOUND");
            revision=root.getString("revision");
            boolean legacy=LEGACY_REVISION.equals(revision);
            if((!REVISION.equals(revision)&&!PRIOR_REVISION.equals(revision)&&!legacy)||!SourceManifestR815.SOURCE_TREE_SHA256.equals(root.getString("source_sha256")))throw new IllegalArgumentException("FROZEN_CONSUMER_REVISION");
            lane=root.getString("lane");id=root.getString("id");
            evaluationRule=root.optString("evaluation_rule","");evaluationEpoch=root.optString("evaluation_epoch","");
            if(ResearchEvaluationRunR967.versioned(id)&&(!ResearchEvaluationRunR967.eligibleEvaluation(id,root.getString("target"))||!TrainingContinuationR973.compatible(evaluationRule)||!evaluationEpoch.matches("[0-9a-f]{64}")))throw new IllegalArgumentException("R969_FROZEN_RECIPE_SEMANTIC_BINDING");
            contextJson=root.optString("frozen_context","");contextHash=root.optString("context_hash","");
            if(root.getString("spec").startsWith("AST3:")){
                if(contextJson.isEmpty()||contextJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144
                    ||!contextHash.matches("[0-9a-f]{64}")||!V51ChecksumEngine.sha256(contextJson).equals(contextHash))
                    throw new IllegalArgumentException("RECIPE_INCOMPLETE");
            }else if(!contextJson.isEmpty()||!contextHash.isEmpty())throw new IllegalArgumentException("LEGACY_RECIPE_CONTEXT");
            spec=root.getString("spec");state=root.getString("state");structure=root.getString("structure");evidence=root.getString("evidence");certificate=root.getString("certificate");
            base=root.getString("base");target=root.getString("target");historyHash=root.getString("history_hash");surfaceHash=root.getString("surface_hash");historyRows=root.getInt("history_rows");elapsedMs=root.getLong("elapsed_ms");weight=root.getDouble("weight");
            if(!TargetIssueResolver.exact(base,target).valid||!Double.isFinite(weight)||weight<=0||weight>(CertifiedPredictionR974.LANE.equals(lane)?1.0:.30)||id.length()==0||!structure.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("FROZEN_CONSUMER_CONTRACT");
            if(!legacy&&(!evidence.matches("[0-9a-f]{64}")||!certificate.matches("[0-9a-f]{64}")))throw new IllegalArgumentException("FROZEN_CONSUMER_EVIDENCE_CERTIFICATE_BINDING");
            List<Models.Draw> draws;String previous="";
            if(REVISION.equals(revision)&&root.has("history_wire")){
                if(root.getJSONArray("history").length()!=0)throw new IllegalArgumentException("RECIPE_TWO_HISTORY_OWNERS");
                if(historyRows>(id.startsWith(FrozenCandidateAdapterR958.PREFIX)?5000:480))throw new IllegalArgumentException("FROZEN_CONSUMER_HISTORY_LIMIT");
                draws=FrozenHistoryWireCP11.decode(root.getString("history_wire"),historyRows,base);
                if(!draws.isEmpty())previous=draws.get(draws.size()-1).issue;
            }else{
            JSONArray history=root.getJSONArray("history");draws=new ArrayList<>();
            if(history.length()>(id.startsWith(FrozenCandidateAdapterR958.PREFIX)?5000:480))throw new IllegalArgumentException("FROZEN_CONSUMER_HISTORY_LIMIT");
            for(int i=0;i<history.length();i++){
                JSONObject row=history.getJSONObject(i);Models.Draw draw=new Models.Draw();draw.issue=row.getString("issue");draw.openTime=row.getLong("open_time");draw.fetchTime=row.getLong("fetch_time");
                JSONArray numbers=row.getJSONArray("numbers");if(numbers.length()!=10)throw new IllegalArgumentException("FROZEN_CONSUMER_PERMUTATION");
                for(int n=0;n<10;n++)draw.numbers[n]=numbers.getInt(n);
                if(!draw.isValid()||!TargetIssueResolver.isValid(draw.issue)||TargetIssueResolver.compare(draw.issue,base)>0||(previous.length()>0&&TargetIssueResolver.compare(previous,draw.issue)>=0))throw new IllegalArgumentException("FROZEN_CONSUMER_HISTORY_PIT");
                previous=draw.issue;draws.add(draw);
            }
            }
            boolean needsHistory=lane.equals(CertifiedPredictionR974.LANE)||lane.equals("INVENTED_ALGORITHM_CERTIFIED")||lane.equals("GENESIS_NEW_CERTIFIED")||(lane.equals("FORMULA_DSL_ACTIVE_SHADOW")&&ResearchCompiledGraphRuntimeR906.compile(ResearchMethodPlanR906.parseCanonical(spec)).family==ResearchCompiledGraphRuntimeR906.Family.HISTORICAL_SYNTHESIS);
            if(needsHistory&&(draws.size()<(id.startsWith(FrozenCandidateAdapterR958.PREFIX)?20:24)||!previous.equals(base)))throw new IllegalArgumentException("FROZEN_CONSUMER_HISTORY_MISSING");
            frozenHistory=Collections.unmodifiableList(draws);
            profile=needsHistory&&!id.startsWith(FrozenCandidateAdapterR958.PREFIX)?HistoricalStrategyInventionR908.Profile.from(draws):null;
            encoded=root.toString();if(encoded.length()>1048576)throw new IllegalArgumentException("RECIPE_ENCODED_BOUND");
        }
    }
    static Recipe freeze(String lane,String id,String spec,String state,String structure,String evidence,String certificate,double weight,
            String base,String target,HistoricalStrategyInventionR908.Profile profile,V51RuntimeResult runtime)throws Exception {
        RecipeBudgetScopeCP12.requireActive();
        JSONObject root=new JSONObject();root.put("revision",REVISION);root.put("source_sha256",SourceManifestR815.SOURCE_TREE_SHA256);root.put("lane",lane);root.put("id",id);root.put("spec",spec);root.put("state",state);root.put("structure",structure);root.put("evidence",evidence);root.put("certificate",certificate);root.put("weight",weight);root.put("base",base);root.put("target",target);
        root.put("history_hash",runtime==null?"":runtime.context.historyHash);root.put("surface_hash",runtime==null?"":runtime.checksum);root.put("history_rows",runtime==null?0:runtime.context.historyRows);root.put("elapsed_ms",runtime==null?0:runtime.elapsedMs);
        JSONArray history=new JSONArray();if(profile!=null)for(Models.Draw draw:profile.historyForGenesisR920()){
            JSONObject row=new JSONObject();row.put("issue",draw.issue);row.put("open_time",draw.openTime);row.put("fetch_time",draw.fetchTime);row.put("numbers",new JSONArray(draw.numbers));history.put(row);
        }
        root.put("history",history);return new Recipe(root);
    }
    static Recipe decode(String encoded,String target,String lane,String id,double weight)throws Exception {
        RecipeBudgetScopeCP12.requireActive();if(encoded==null||encoded.length()>1048576)throw new IllegalArgumentException("RECIPE_ENCODED_BOUND");
        Recipe recipe=new Recipe(new JSONObject(encoded));
        if(!recipe.target.equals(target)||!recipe.lane.equals(lane)||!recipe.id.equals(id)||Double.compare(recipe.weight,weight)!=0)throw new IllegalArgumentException("FROZEN_CONSUMER_SELECTION_BINDING");
        return recipe;
    }
    static Mixed execute(Recipe recipe,V51Models.PositionAction[] actions){
        RecipeBudgetScopeCP12.requireActive();
        K3ActionSpaceEngine.validateOrThrow(actions);
        if(recipe.lane.equals(CertifiedPredictionR974.LANE))return CertifiedPredictionR974.rank(recipe,actions);
        if(recipe.lane.equals("INVENTED_ALGORITHM_CERTIFIED"))return algorithm(recipe,actions);
        if(recipe.lane.equals("GENESIS_NEW_CERTIFIED"))return genesis(recipe,actions);
        if(recipe.lane.equals("FORMULA_DSL_ACTIVE_SHADOW"))return formula(recipe,actions);
        throw new IllegalArgumentException("FROZEN_CONSUMER_LANE");
    }
    private static Mixed algorithm(Recipe recipe,V51Models.PositionAction[] productionActions){
        if(recipe.id.startsWith(FrozenCandidateAdapterR958.PREFIX))return FrozenCandidateAdapterR958.mix(recipe,productionActions);
        AlgorithmGenomeR908 genome=AlgorithmGenomeR908.parse(recipe.spec);
        AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.State.decode(recipe.state);
        if(!genome.algorithmGrade()||!genome.genomeId.equals(recipe.id)||!genome.structuralHash.equals(recipe.structure)||!state.encode().equals(recipe.state))throw new IllegalArgumentException("FROZEN_ALGORITHM_IDENTITY");
        HistoricalStrategyInventionR908.Profile profile=recipe.profile;double weight=recipe.weight;
        String before=state.encode();
            final double[] score = new double[productionActions.length];
            Integer[] order = new Integer[productionActions.length];
            for (int i = 0; i < productionActions.length; i++) {
                V51Models.PositionAction action = productionActions[i];
                if (action == null)
                    throw new IllegalArgumentException("ACTION_NULL");
                double[] feature = average(
                        inputs(profile, action.position, action.n1),
                        inputs(profile, action.position, action.n2),
                        inputs(profile, action.position, action.n3));
                double invented = clamp01(
                        AlgorithmGenomeVmR908.score(genome, state, action.position, feature));
                double fixedMainline = clamp01(action.rankScore);
                score[i] = (1.0d - weight) * fixedMainline + weight * invented;
                order[i] = Integer.valueOf(i);
            }
            // Predraw score() is contractually read-only.  A second pass is intentionally absent:
            // the outcome update program may run only after SettlementEngine labels this issue.
            Arrays.sort(
                    order,
                    new Comparator<Integer>() {
                        @Override
                        public int compare(Integer left, Integer right) {
                            int l = left.intValue(), r = right.intValue();
                            int by = Double.compare(score[r], score[l]);
                            if (by != 0) return by;
                            int old = Integer.compare(
                                    productionActions[l].rank, productionActions[r].rank);
                            return old != 0 ? old : Integer.compare(l, r);
                        }
                    });
            V51Models.PositionAction[] integrated =
                    new V51Models.PositionAction[productionActions.length];
            for (int rank = 0; rank < order.length; rank++) {
                int index = order[rank].intValue();
                V51Models.PositionAction a = productionActions[index];
                double margin = rank + 1 < order.length
                        ? Math.max(0.0d, score[index] - score[order[rank + 1].intValue()])
                        : 0.0d;
                integrated[index] = new V51Models.PositionAction(
                        a.position,
                        a.n1,
                        a.n2,
                        a.n3,
                        a.n4,
                        a.rawActionP,
                        score[index],
                        margin,
                        a.thirdMargin,
                        a.entropy,
                        a.stability,
                        a.metaFeatures,
                        a.fingerprint,
                        rank + 1,
                        V51ChecksumEngine.sha256(
                                a.checksum
                                        + '|' + AlgorithmProductionGovernorR915.VERSION
                                        + '|' + recipe.id
                                        + '|' + recipe.evidence
                                        + '|' + recipe.certificate
                                        + '|' + recipe.structure
                                        + '|' + score[index]
                                        + '|' + (rank + 1)));
            }
            K3ActionSpaceEngine.validateOrThrow(integrated);
            V51Models.PositionAction rankOne = integrated[order[0].intValue()];
        if(!before.equals(state.encode()))throw new IllegalStateException("FROZEN_ALGORITHM_STATE_MUTATED");
        return new Mixed(integrated,rankOne);
    }
    private static Mixed genesis(Recipe recipe,V51Models.PositionAction[] productionActions){
        GenesisAssetGenomeR915 genome=GenesisAssetGenomeR915.parse(recipe.spec);
        GenesisAssetVmR915.State state=GenesisAssetVmR915.State.decode(recipe.state);
        if(!genome.assetGrade()||!genome.assetId.equals(recipe.id)||!genome.structuralHash.equals(recipe.structure)||!state.encode().equals(recipe.state))throw new IllegalArgumentException("FROZEN_GENESIS_IDENTITY");
        HistoricalStrategyInventionR908.Profile profile=recipe.profile;double weight=recipe.weight;
        String stateBefore=state.encode();
            final double[] mixedScore = new double[productionActions.length];
            Integer[] order = new Integer[productionActions.length];
            int evaluated = 0;
            V51Models.PositionAction productionRankOne = null;
            for (int i=0;i<productionActions.length;i++) {
                V51Models.PositionAction action = productionActions[i];
                if (action == null) throw new IllegalArgumentException("ACTION_NULL");
                if (action.rank == 1) productionRankOne = action;
                double score = clamp01(action.rankScore);
                if (action.rank >= 1 && action.rank <= CertifiedResearchMixerR915.TOP_EVALUATION) {
                    GenesisAssetVmR915.PredrawContext context = GenesisAssetVmR915.buildContextSurface(profile,recipe.base,recipe.target,recipe.historyHash,recipe.surfaceHash,recipe.historyRows,recipe.elapsedMs,action);
                    if (context == null) throw new IllegalArgumentException("CONTEXT_BUILD_FAILED");
                    GenesisAssetVmR915.AssetValue asset = GenesisAssetVmR915.evaluateCompact(genome,state,context);
                    double signed = asset.primary - 0.5d;
                    score = clamp01(score + weight * signed * asset.stability * (1.0d-asset.risk));
                    evaluated++;
                }
                mixedScore[i]=score;order[i]=Integer.valueOf(i);
            }
            if (!stateBefore.equals(state.encode()))
                throw new IllegalStateException("R915_MIXER_MUTATED_ASSET_STATE");
            if (evaluated != CertifiedResearchMixerR915.TOP_EVALUATION || productionRankOne == null)
                throw new IllegalArgumentException("TOP64_OR_RANK1_MISSING");
            Arrays.sort(order,new Comparator<Integer>(){
                @Override public int compare(Integer a,Integer b){int i=a.intValue(),j=b.intValue();
                    int by=Double.compare(mixedScore[j],mixedScore[i]);if(by!=0)return by;
                    int old=Integer.compare(productionActions[i].rank,productionActions[j].rank);
                    return old!=0?old:Integer.compare(i,j);}});
            V51Models.PositionAction[] integrated=new V51Models.PositionAction[productionActions.length];
            for(int rank=0;rank<order.length;rank++){
                int index=order[rank].intValue();V51Models.PositionAction a=productionActions[index];
                double margin=rank+1<order.length?Math.max(0,mixedScore[index]-mixedScore[order[rank+1].intValue()]):0;
                integrated[index]=new V51Models.PositionAction(a.position,a.n1,a.n2,a.n3,a.n4,a.rawActionP,
                        mixedScore[index],margin,a.thirdMargin,a.entropy,a.stability,a.metaFeatures,a.fingerprint,
                        rank+1,V51ChecksumEngine.sha256(a.checksum+'|'+CertifiedResearchMixerR915.VERSION+'|'+recipe.id+'|'
                                +recipe.evidence+'|'+recipe.certificate+'|'+mixedScore[index]+'|'+(rank+1)));
            }
            K3ActionSpaceEngine.validateOrThrow(integrated);
            V51Models.PositionAction rankOne=integrated[order[0].intValue()];
        return new Mixed(integrated,rankOne);
    }
    private static Mixed formula(Recipe recipe,V51Models.PositionAction[] defaultRanked){
        ResearchMethodPlanR906 plan=ResearchMethodPlanR906.parseCanonical(recipe.spec);
        ResearchCompiledGraphRuntimeR906.Compiled compiled=ResearchCompiledGraphRuntimeR906.compile(plan);
        if(!compiled.executable||!compiled.artifactHash.equals(recipe.structure))throw new IllegalArgumentException("FROZEN_FORMULA_IDENTITY");
            final double[][] matrix=new double[defaultRanked.length][];
            Integer[] order=new Integer[defaultRanked.length];
            for(int i=0;i<defaultRanked.length;i++){
                V51Models.PositionAction a=defaultRanked[i];
                if(a==null)throw new IllegalArgumentException("ACTION_NULL");
                matrix[i]=ResearchCompiledGraphRuntimeR906.canonicalVector(a);
                order[i]=Integer.valueOf(i);
            }
            final double[] score=compiled.family==ResearchCompiledGraphRuntimeR906.Family.HISTORICAL_SYNTHESIS?HistoricalStrategyInventionR908.evaluateAll(recipe.profile,defaultRanked,plan):ResearchCompiledGraphRuntimeR906.evaluateAll(compiled,matrix);
            if(score.length!=defaultRanked.length)throw new IllegalArgumentException("CERTIFIED_GRAPH_OUTPUT_INVALID");
            // Comparable rank percentiles avoid mixing unrelated raw score scales.
            Integer[] researchOrder=order.clone();
            Arrays.sort(researchOrder,new Comparator<Integer>(){public int compare(Integer a,Integer b){int d=Double.compare(score[b],score[a]);return d!=0?d:Integer.compare(a,b);}});
            double[] percentile=new double[score.length];
            for(int r=0;r<researchOrder.length;r++)percentile[researchOrder[r]]=1.0-r/(double)(score.length-1);
            for(int i=0;i<score.length;i++)score[i]=(1-recipe.weight)*(1.0-(defaultRanked[i].rank-1)/(double)(score.length-1))+recipe.weight*percentile[i];
            Arrays.sort(order,new Comparator<Integer>(){public int compare(Integer l,Integer r){int a=l.intValue(),b=r.intValue();int by=Double.compare(score[b],score[a]);if(by!=0)return by;int old=Integer.compare(defaultRanked[a].rank,defaultRanked[b].rank);return old!=0?old:Integer.compare(a,b);}});
            V51Models.PositionAction[] reranked=new V51Models.PositionAction[defaultRanked.length];
            for(int rank=0;rank<order.length;rank++){
                int index=order[rank].intValue();V51Models.PositionAction a=defaultRanked[index];double margin=rank==0&&order.length>1?score[index]-score[order[1].intValue()]:0d;
                reranked[index]=new V51Models.PositionAction(a.position,a.n1,a.n2,a.n3,a.n4,a.rawActionP,score[index],margin,a.thirdMargin,a.entropy,a.stability,a.metaFeatures,a.fingerprint,rank+1,V51ChecksumEngine.sha256(a.checksum+"|R906_CERTIFIED_GOVERN|"+recipe.certificate+'|'+recipe.evidence+'|'+recipe.id+'|'+score[index]+'|'+(rank+1)));
            }
            V51Models.PositionAction selected=reranked[order[0].intValue()];
        return new Mixed(reranked,selected);
    }
    private static double[] inputs(
            HistoricalStrategyInventionR908.Profile profile, int position, int number) {
        double[] x = new double[AlgorithmGenomeR908.INPUTS];
        x[0] = profile.atom("position_transition_lift", position, number);
        x[1] = profile.atom("cross_position_migration", position, number);
        x[2] = profile.atom("short_long_residual", position, number);
        x[3] = profile.atom("motif_similarity", position, number);
        x[4] = profile.atom("gap_pressure", position, number);
        x[5] = profile.atom("frequency_slope", position, number);
        x[6] = profile.regime[Math.max(0, Math.min(9, position - 1))];
        x[7] = (position - 1) / 9.0d;
        x[8] = (number - 1) / 9.0d;
        return x;
    }

    private static double[] average(double[] a, double[] b, double[] c) {
        double[] out = new double[AlgorithmGenomeR908.INPUTS];
        for (int i = 0; i < out.length; i++) out[i] = (a[i] + b[i] + c[i]) / 3.0d;
        return out;
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0d, 1.0d);
    }

    private static double clamp(double value, double min, double max) {
        double x = Double.isNaN(value) || Double.isInfinite(value) ? min : value;
        return Math.max(min, Math.min(max, x));
    }

}
