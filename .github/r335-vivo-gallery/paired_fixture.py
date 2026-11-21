"""Keep the measured R06 chart projection linked to its genuine paired-study original in the insufficient case."""
from pathlib import Path
folder=Path('app/src/androidTest/java/com/ethk3/novaonepro')
fixture=folder/'strategyv2/RepairAnalysisAndroidFixture.java'
s=fixture.read_text()
assert s.count('public static Map<String,String> create(DbStore store)')==1
s=s.replace('public static Map<String,String> create(DbStore store){','public static Map<String,String> create(DbStore store){return create(store,false); }\n public static Map<String,String> create(DbStore store,boolean insufficient){')
tag='write(store,ResearchStoreR30R1.paired(runner,3));'
replacement="""if(insufficient){
    ResearchContractR30R1 shortStudy=new ResearchContractR30R1(data.datasetHash,parent.policyHash(),FrozenConfirmationR30.currentSemantics(),BuildConfig.SOURCE_COMMIT_R26,StatsR27.sha256("ANDROID_INSUFFICIENT_R06"),ResearchContractR30R1.Role.SELECT,contract.fromOrdinal,contract.throughOrdinal,2,revision);
    ResearchGainR30R1.Estimate empty=new ResearchGainR30R1(shortStudy.fromOrdinal,shortStudy.throughOrdinal,64,2,128).estimate();
    ResearchAdviceR30R1 noAdvice=ResearchAdviceR30R1.component(shortStudy,"root/0",empty,shortStudy.throughOrdinal+1,System.currentTimeMillis());
    if(empty.supported()||empty.evaluatedIssues!=0||noAdvice.action!=ResearchAdviceR30R1.Action.NONE)throw new AssertionError("EXPECTED_EMPTY_PAIRED_STUDY");
    write(store,ResearchStoreR30R1.paired(shortStudy,empty,noAdvice,runner.without.fullJson(),runner.without.policyHash(),1000));
    return Collections.singletonMap("R06","INSUFFICIENT");
   }
   """+tag
assert s.count(tag)==1
fixture.write_text(s.replace(tag,replacement))
chart=folder/'R30RepairChartsAndroidTest.java';s=chart.read_text()
tag='   try{CancellationSignal cancelled='
assert s.count(tag)==1
s=s.replace(tag,'   if(scenario.equals("INSUFFICIENT")){ResearchVisualDataR82.Panel rejected=ResearchWorkbenchRepositoryR30R1.load(store.readable(),0,new CancellationSignal()).get(5);assertEquals("unbound R06 projection must be rejected","READ_ERROR",rejected.status);RepairAnalysisAndroidFixture.create(store,true); }\n'+tag)
chart.write_text(s)
