package com.lifehub.starter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.HttpClientSettings;

class LifehubSecurityPropertiesTest {

    @Test
    void withoutACaCertificateCallsToLogtoUseJavasNormalTrust() {
        assertThat(properties(null).logtoHttpClientSettings().sslBundle()).isNull();
        assertThat(properties(" ").logtoHttpClientSettings().sslBundle()).isNull();
    }

    @Test
    void aCaCertificateBecomesTheOnlyTrustedCertificate() throws Exception {
        String path = Path.of("src/test/resources/security/test-ca.pem").toAbsolutePath().toString();

        HttpClientSettings settings = properties(path).logtoHttpClientSettings();

        KeyStore trustStore = settings.sslBundle().getStores().getTrustStore();
        assertThat(Collections.list(trustStore.aliases())).hasSize(1);
        X509Certificate ca = (X509Certificate) trustStore.getCertificate(trustStore.aliases().nextElement());
        assertThat(ca.getSubjectX500Principal().getName()).isEqualTo("CN=LifeHub test CA");
        assertThat(settings.readTimeout()).hasSeconds(5);
    }

    @Test
    void aMissingCaCertificateFileFailsInsteadOfFallingBackToNoTrust() {
        assertThatThrownBy(() -> properties("/does/not/exist.pem").logtoHttpClientSettings()
                .sslBundle().getStores().getTrustStore())
                .isInstanceOf(RuntimeException.class);
    }

    /** Properties with dummy Logto settings and this CA certificate location. */
    private static LifehubSecurityProperties properties(String caCertificate) {
        return new LifehubSecurityProperties("https://auth.lifehub.test/oidc", null, "https://api.lifehub.test",
                "https://internal.lifehub.test", caCertificate);
    }
}
