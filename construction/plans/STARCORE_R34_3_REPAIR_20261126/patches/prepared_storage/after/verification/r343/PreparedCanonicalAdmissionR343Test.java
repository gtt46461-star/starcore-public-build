package com.ethk3.novaonepro;

public final class PreparedCanonicalAdmissionR343Test {
    public static void main(String[] args) {
        long previous=0;
        for (long size : new long[]{1,4095,4096,4097,65535,65536,65537,1048575,1048576,16777216}) {
            long bound=PreparedCanonicalAdmissionR343.physicalGrowthBound(size);
            if(bound<size || bound<previous || bound>2*16777216L+65536L)
                throw new AssertionError("envelope: "+size);
            // A data image, an atomic companion, and a directory allocation fit.
            long rounded=((size+65535L)/65536L)*65536L;
            if (bound < rounded*2+65536L) throw new AssertionError("atomic copies");
            previous=bound;
        }
        for (long invalid : new long[]{Long.MIN_VALUE,-1,0,16777217L,Long.MAX_VALUE}) {
            boolean refused=false;
            try{PreparedCanonicalAdmissionR343.physicalGrowthBound(invalid);}
            catch(IllegalArgumentException expected){refused=true;}
            if(!refused)throw new AssertionError("invalid size accepted");
        }
        System.out.println("PASS: R343 prepared physical envelope boundary/overflow cases; not Android allocation certification");
    }
}
