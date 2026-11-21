package com.ethk3.novaonepro;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import javax.net.ssl.*;

public class PoolProbe {
    public static void main(String[] args) throws Exception {
        KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
        trust.load(null,null);
        try(FileInputStream cert=new FileInputStream(args[1])){
            trust.setCertificateEntry("ephemeral-loopback",CertificateFactory.getInstance("X.509").generateCertificate(cert));
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trust);
        SSLContext tls=SSLContext.getInstance("TLS");
        tls.init(null,factory.getTrustManagers(),null);
        HttpsURLConnection.setDefaultSSLSocketFactory(tls.getSocketFactory());
        ApiClient client=new ApiClient();
        for(int i=0;i<4;i++){
            ApiClient.RawHttpResult r=i==1?client.requestPostJson(args[0],"{\"probe\":true}"):client.requestGet(args[0]);
            System.out.println("request="+i+" status="+r.httpCode+" ok="+r.ok+" activeAfter="+client.activeRequestCountR31());
            if(!r.ok||r.httpCode!=200||client.activeRequestCountR31()!=0||!r.body.contains("acknowledged"))throw new AssertionError("TLS request failure: "+r.errorMessage);
        }
        client.setRequestCancellationR31(()->true);
        if(client.requestGet(args[0]).ok||client.activeRequestCountR31()!=0)throw new AssertionError("Cancellation did not stop request");
    }
}
