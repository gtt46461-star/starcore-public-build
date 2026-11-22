package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.ReleaseManifestR926;
import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.ExpertRef;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
public class SignedRegisteredFamilies {
    private static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static CandidateAstR952 parentAt(int position) {
        List<String> scores = Collections.nCopies(10, "0x1.0p0");
        List<?> empty = Collections.emptyList();
        Map<String, Object> score = fields("id", "n0", "operator", "CONSTANT", "atomId", "",
                "outputType", "SCORE_VECTOR10", "stage", "FROZEN_PREDRAW", "unit", "dimensionless",
                "parameters", scores, "parents", empty);
        Map<String, Object> root = fields("version", 3, "compilerRevision", "AST3_CP4_1",
                "position", position, "game", ReleaseManifestR926.GAME_CODE, "family", "FREQUENCY",
                "modelKind", "FIXED_GRAPH", "normalizerPolicy", "TRAIN_ONLY_ZSCORE_V1",
                "nodes", Collections.singletonList(score), "featureOutputIds", empty,
                "outputId", "n0", "inputDimension", 10, "experts", empty);
        root.put("featureManifest", fields("revision", "CP4_FEATURE_1",
                "graphHash", CandidateAstR952.hashGraphKernelWithoutManifest(root),
                "orderedFeatureIds", empty, "featureTypes", empty, "requiredHistory", 0,
                "atomSpecHashes", empty, "bindingHashes", empty, "matrixSpecRefs", empty,
                "normalizerPolicy", "TRAIN_ONLY_ZSCORE_V1", "missingPolicyPerFeature", empty,
                "inputDimension", 10, "estimatedStateBytes", 4096));
        return CandidateAstR952.parse("AST3:" + StrictCanonicalJson.write(root));
    }

    private static void require(boolean valid,String reason){if(!valid)throw new AssertionError(reason);}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
    public static void main(String[] args)throws Exception{
        int position=Integer.parseInt(args[1]); String mode=args[2]; String token="\"position\":"+position+",";
        Map<Integer,List<AtomSpecCP4>> registry=new HashMap<>();int total=0;
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(ZipFile apk=new ZipFile(args[0]); BufferedReader br=new BufferedReader(new InputStreamReader(new DigestInputStream(apk.getInputStream(apk.getEntry("assets/cp4_atom_registry.jsonl")),digest),StandardCharsets.UTF_8))){
            for(String line;(line=br.readLine())!=null;){total++;if(!line.contains(token))continue;AtomSpecCP4 a=AtomSpecCP4.fromAssetLine(line);if(a.position==1||Arrays.asList("MARKOV_SCORE","FREQUENCY","OMISSION","REGIME_SCORE").contains(a.family))registry.computeIfAbsent(a.position,k->new ArrayList<>()).add(a);}
        }
        String identity=hex(digest.digest());require(total==AtomCatalogPinnedCP12.ROWS&&identity.equals(AtomCatalogPinnedCP12.SHA256),"asset identity");
        int families=0,variants=0,maxBytes=0;List<ExpertRef> refs=new ArrayList<>();
        for(int n=0;n<3;n++)refs.add(new ExpertRef(V51ChecksumEngine.sha256("freeze"+n),V51ChecksumEngine.sha256("model"+n),V51ChecksumEngine.sha256("manifest"+n),1));
        CandidateAstR952 parent=parentAt(position);
        if(!mode.equals("variants"))for(CandidateAstR952.Family f:CandidateAstR952.Family.values()){
            if(mode.equals("old-regime")&&f!=CandidateAstR952.Family.REGIME)continue;
            MutationGraphCP12.Proposal a=MutationGraphCP12.propose(parent,null,"signed-family-on-device","NEW_FAMILY",registry.get(1),Collections.emptySet(),refs,f,Long.MAX_VALUE);
            if(a.ast==null){System.err.println(f+":"+a.reason);throw new AssertionError(f+":"+a.reason);}
            CandidateAstR952 child=CandidateAstR952.parse(a.ast.canonical);
            CandidateCompilerR952.compile(child,child.graphSpec.manifest,MutationGraphCP12.usedSpecs(child,registry.get(1)));
            require(child.family==f&&!child.structuralHash.equals(parent.structuralHash),"child identity");
            System.out.println("FAMILY "+f.name()+" "+child.structuralHash+" "+child.graphSpec.manifest.hash);
            families++;
        }
        if(mode.equals("variants"))for(int w=0;w<4;w++){int p=position;
            CandidateAstR952 a=ModernFamilyFactoryCP12.newFamilyFromParentCP12(parentAt(p),CandidateAstR952.Family.REGIME,registry.get(p),Collections.emptyList(),w*10+p-1);
            CandidateAstR952 child=CandidateAstR952.parse(a.canonical);
            CandidateCompilerR952.compile(child,child.graphSpec.manifest,MutationGraphCP12.usedSpecs(child,registry.get(p)));
            int length=a.canonical.getBytes(StandardCharsets.UTF_8).length;maxBytes=Math.max(maxBytes,length);
            require(length<=32768&&child.position==p&&child.structuralHash.equals(a.structuralHash)&&child.graphSpec.manifest.matrixSpecRefs.size()==10&&child.graphSpec.manifest.physicalDimensionsR82.size()==10,"variant");
            variants++;
        }
        require(mode.equals("variants")?variants==4:families==15,"coverage");System.out.println("SIGNED_FAMILIES PASS families="+families+" regimeVariants="+variants+" maxCanonicalBytes="+maxBytes+" catalogRows="+total+" assetSha256="+identity);
    }
}