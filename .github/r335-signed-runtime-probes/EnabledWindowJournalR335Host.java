package com.ethk3.novaonepro;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
public final class EnabledWindowJournalR335Host {
    static int checks;
    static void yes(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
    static void fails(Runnable code){checks++;try{code.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("missing rejection "+checks);}
    static final String P="11111111111111111111111111111111",S="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    public static void main(String[] args)throws Exception {
        yes(EnabledWindowJournalR335.parse("").events.isEmpty());
        String first=EnabledWindowJournalR335.append("",true,10000,100,P,S);
        yes(EnabledWindowJournalR335.parse(first).events.get(0).enabled);
        yes(EnabledWindowJournalR335.parse(first).events.get(0).sequence==1);
        String next=EnabledWindowJournalR335.append(first,false,11000,1100,P,S);
        yes(!EnabledWindowJournalR335.parse(next).events.get(1).enabled);
        yes(EnabledWindowJournalR335.parse(next).events.get(1).previousHash.equals(EnabledWindowJournalR335.parse(first).events.get(0).hash));
        fails(()->EnabledWindowJournalR335.parse(next.substring(0,next.length()-1)));
        fails(()->EnabledWindowJournalR335.parse(next.replace("|0|aaaa","|1|aaaa")));
        fails(()->EnabledWindowJournalR335.append(first,true,9999,1001,P,S));
        fails(()->EnabledWindowJournalR335.append(first,true,11000,99,P,S));
        fails(()->EnabledWindowJournalR335.append(first,true,15000,1100,P,S));
        fails(()->EnabledWindowJournalR335.append(first,true,11000,1100,"BAD",S));
        fails(()->EnabledWindowJournalR335.append(first,true,11000,1100,P,"UNKNOWN"));
        fails(()->EnabledWindowJournalR335.parse(next.replace("|0|0000000000000000","|1|0000000000000000")));
        String restarted=EnabledWindowJournalR335.append(first,true,11000,1,"22222222222222222222222222222222",S);
        yes(EnabledWindowJournalR335.parse(restarted).events.size()==2);
        String refused=EnabledWindowJournalR335.appendState(first,"R",11000,1100,P,S);
        yes(!EnabledWindowJournalR335.parse(refused).events.get(1).enabled);
        yes("R".equals(EnabledWindowJournalR335.parse(refused).events.get(1).assignment));
        fails(()->EnabledWindowJournalR335.appendState(first,"UNKNOWN",11000,1100,P,S));
        String all="";
        for(int i=0;i<520;i++)all=EnabledWindowJournalR335.append(all,(i&1)==0,10000+i,100+i,P,S);
        EnabledWindowJournalR335.Journal rotated=EnabledWindowJournalR335.parse(all);
        yes(rotated.events.size()==512);yes(rotated.dropped==8);
        yes(rotated.events.get(0).sequence==9);yes(rotated.events.get(511).sequence==520);
        yes(rotated.events.get(0).previousHash.equals(rotated.prefixHash));
        yes(rotated.encode().equals(all));
        if(args.length>0)Files.write(Paths.get(args[0]),all.getBytes(StandardCharsets.UTF_8));
        System.out.println("ENABLED_ASSIGNMENT_JOURNAL_PASS checks="+checks);
    }
}
