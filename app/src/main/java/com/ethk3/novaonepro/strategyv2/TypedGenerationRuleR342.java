package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A finite typed grammar derived from two executed development programs. */
public final class TypedGenerationRuleR342 {
    private static final String VERSION = "R342_TYPED_RULE_1";
    private static final int MAX_BYTES = 16_384;
    private final PolicySpecR27 first, second;
    private final List<String> slots;
    public final String dataset, abi, firstProof, secondProof;
    public final long revision, from, through;
    public final String ruleId, shapeHash;

    private TypedGenerationRuleR342(PolicySpecR27 first, String firstProof, PolicySpecR27 second,
            String secondProof, String dataset, long revision, long from, long through, String abi) {
        this.first = PolicySpecR27.parse(first.fullJson());
        this.second = PolicySpecR27.parse(second.fullJson());
        this.firstProof = ResearchContractR30R1.sha(firstProof);
        this.secondProof = ResearchContractR30R1.sha(secondProof);
        this.dataset = ResearchContractR30R1.sha(dataset);
        this.revision = revision;
        this.from = from;
        this.through = through;
        this.abi = abi;
        require(revision >= 0 && from >= 0 && through >= from && abi != null && !abi.isEmpty());
        require(first.position == second.position && first.rule == second.rule
                && first.rule != PolicySpecR27.Rule.RANDOM && first.fitTemperature == second.fitTemperature);
        require(BirthGateR27.candidateContract(first).ok && BirthGateR27.candidateContract(second).ok);
        require(!FormulaIdentityR341.structure(first).equals(FormulaIdentityR341.structure(second)));
        List<String> typedSlots = new ArrayList<>();
        describeSlots(first.ast, second.ast, "", typedSlots);
        require(typedSlots.size() >= 2 && typedSlots.size() <= 4);
        slots = Collections.unmodifiableList(typedSlots);
        shapeHash = StatsR27.sha256(JsonR27.write(Arrays.asList(VERSION,
                FormulaIdentityR341.structure(first), FormulaIdentityR341.structure(second), slots)));
        require(transferVariants(0).size() >= 2);
        String canonical = encodeCanonical();
        require(PolicyAstR27.utf8Bytes(canonical,MAX_BYTES) <= MAX_BYTES);
        ruleId = StatsR27.sha256(canonical);
    }

    public static TypedGenerationRuleR342 induct(PolicySpecR27 a, String aProof, PolicySpecR27 b,
            String bProof, String dataset, long revision, long from, long through, String abi) {
        require(a != null && b != null && aProof != null && bProof != null
                && !a.policyHash().equals(b.policyHash()) && !aProof.equals(bProof));
        return a.policyHash().compareTo(b.policyHash()) < 0
                ? new TypedGenerationRuleR342(a,aProof,b,bProof,dataset,revision,from,through,abi)
                : new TypedGenerationRuleR342(b,bProof,a,aProof,dataset,revision,from,through,abi);
    }

    private static void describeSlots(PolicyAstR27.Node a, PolicyAstR27.Node b, String path, List<String> slots) {
        if (!a.op.equals(b.op)) {
            PolicyAstR27.Type type = PolicyAstR27.type(a);
            require(!path.isEmpty() && type == PolicyAstR27.type(b) && !type.equals(PolicyAstR27.Type.CONST));
            slots.add(path + ':' + type.name());
            return;
        }
        require(a.kids.length == b.kids.length);
        for (int i = 0; i < a.kids.length; i++) describeSlots(a.kids[i],b.kids[i],path + '/' + i,slots);
    }

    private static PolicyAstR27.Node replaceSlots(PolicyAstR27.Node a, PolicyAstR27.Node b, int mask, int[] next) {
        if (!a.op.equals(b.op)) return (mask & (1 << next[0]++)) == 0 ? a : b;
        if (a.kids.length == 0) return a;
        PolicyAstR27.Node[] children = new PolicyAstR27.Node[a.kids.length];
        for (int i = 0; i < children.length; i++) children[i] = replaceSlots(a.kids[i],b.kids[i],mask,next);
        return new PolicyAstR27.Node(a.op,a.ints,a.param,children);
    }

    private List<PolicySpecR27> transferVariants(int generation) {
        List<PolicySpecR27> variants = new ArrayList<>();
        Set<String> shapes = new HashSet<>(Arrays.asList(
                FormulaIdentityR341.structure(first), FormulaIdentityR341.structure(second)));
        for (int mask = 1; mask < (1 << slots.size()) - 1 && variants.size() < 2; mask++) {
            PolicyAstR27.Node ast = replaceSlots(first.ast,second.ast,mask,new int[]{0});
            PolicySpecR27 child = first.withAst(ast,new PolicySpecR27.Meta("I09","TYPED_GENERATION_RULE",
                    parents(),generation,mask,"Development transfer of a versioned typed rule"));
            if (BirthGateR27.candidateContract(child).ok && shapes.add(FormulaIdentityR341.structure(child))) {
                variants.add(child);
            }
        }
        return Collections.unmodifiableList(variants);
    }

    List<String> parents() { return Arrays.asList(first.policyHash(),second.policyHash()); }

    public List<PolicySpecR27> instantiate(String targetDataset,long begin,long end,int generation) {
        require(targetDataset != null && targetDataset.matches("[a-f0-9]{64}") && !targetDataset.equals(dataset)
                && begin > through && begin <= end);
        return transferVariants(generation);
    }

    public String encodeCanonical() {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("schema",VERSION);
        data.put("abi",abi);
        data.put("dataset",dataset);
        data.put("revision",revision);
        data.put("from",from);
        data.put("through",through);
        data.put("first",first.fullJson());
        data.put("firstProof",firstProof);
        data.put("second",second.fullJson());
        data.put("secondProof",secondProof);
        data.put("slots",slots);
        data.put("shapeHash",shapeHash);
        return JsonR27.write(data);
    }

    public static TypedGenerationRuleR342 decode(String original) {
        require(original != null && PolicyAstR27.utf8Bytes(original,MAX_BYTES) <= MAX_BYTES);
        try {
            Map<?,?> row = (Map<?,?>)JsonR27.parse(original);
            require(VERSION.equals(row.get("schema")));
            TypedGenerationRuleR342 rule = induct(PolicySpecR27.parse((String)row.get("first")),
                    (String)row.get("firstProof"),PolicySpecR27.parse((String)row.get("second")),
                    (String)row.get("secondProof"),(String)row.get("dataset"),
                    ((Number)row.get("revision")).longValue(),((Number)row.get("from")).longValue(),
                    ((Number)row.get("through")).longValue(),(String)row.get("abi"));
            require(rule.encodeCanonical().equals(original));
            return rule;
        } catch (ClassCastException | NullPointerException invalid) {
            throw new IllegalArgumentException("R342_TYPED_RULE_INVALID",invalid);
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException("R342_TYPED_RULE_CONTRACT");
    }
}
