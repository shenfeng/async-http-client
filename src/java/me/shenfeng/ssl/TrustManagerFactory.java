package me.shenfeng.ssl;

/**
 * copy from netty
 */
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

public class TrustManagerFactory extends TrustManagerFactorySpi {

    private static final TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain,
                String authType) throws CertificateException {
            // Always trust
        }

        public void checkServerTrusted(X509Certificate[] chain,
                String authType) throws CertificateException {
            // Always trust
        }
    };

    public static TrustManager[] getTrustManagers() {
        return new TrustManager[] { DUMMY_TRUST_MANAGER };
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return getTrustManagers();
    }

    @Override
    protected void engineInit(KeyStore keystore) throws KeyStoreException {
        // Unused
    }

    @Override
    protected void engineInit(
            ManagerFactoryParameters managerFactoryParameters)
            throws InvalidAlgorithmParameterException {
        // Unused
    }
}
