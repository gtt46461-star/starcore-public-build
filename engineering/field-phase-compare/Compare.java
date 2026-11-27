import com.ethk3.novaonepro.V51ChecksumEngine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
public final class Compare {
    static String expected(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((s==null?"":s).getBytes(StandardCharsets.UTF_8)));
    }
    public static void main(String[] ignored) throws Exception {
        List<String> samples=new ArrayList<>();
        samples.add(null);samples.add("");samples.add("验收😀");samples.add("\ud800");samples.add("\udc00");
        for (int i=0;i<12000;i++) samples.add("202611270793|A1200|"+i+"|"+Integer.toHexString(i)+"|"+ "0123456789".repeat(i%15));
        var pool=Executors.newFixedThreadPool(4);
        try {
            for(int thread=0;thread<4;thread++) pool.submit(()->{
                for(String s:samples) if(!V51ChecksumEngine.sha256(s).equals(expected(s))) throw new AssertionError("hash changed");
                return null;
            }).get();
        } finally { pool.shutdown(); }
        System.out.println("Exact UTF-8/SHA-256 equivalence passed for "+samples.size()*4+" inputs");
    }
}
