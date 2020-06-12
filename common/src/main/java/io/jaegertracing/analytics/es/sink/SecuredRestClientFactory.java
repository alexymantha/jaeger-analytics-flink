package io.jaegertracing.analytics.es.sink;

import lombok.AllArgsConstructor;
import org.apache.flink.streaming.connectors.elasticsearch6.RestClientFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;

import java.io.Serializable;

@AllArgsConstructor
public class SecuredRestClientFactory implements RestClientFactory, Serializable {
    private final String username;
    private final String password;

    @Override
    public void configureRestClientBuilder(RestClientBuilder restClientBuilder) {
        restClientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                return httpClientBuilder;
            }
        });
    }
}
