package cz.dmostek;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class HystrixTimeoutTest {

    @Autowired
    RemoteHttpClientCallService remoteCall;

    /**
     * Output
     Calling remote service on main
     Calling http client on hystrix-testGroup-1
     Fallback called on HystrixTimer-1
     Fallback reason null - probably timeout
     Remote call did not failed. Response Fallback
     Going to sleep 15 seconds on main
     Processing response on thread hystrix-testGroup-1
     Response from http call ....
     Awake again on main
     */
    @Test
    public void testTimeout() throws InterruptedException {
        Thread thread = Thread.currentThread();
        try {
            System.out.println("Calling remote service on " + thread.getName());
            String response = remoteCall.testCall();
            System.out.println("Remote call did not failed. Response: " + response);
        } catch (Exception e) {
            System.out.println("Remote call FAILED.");
            e.printStackTrace();
        }
        System.out.println("Going to sleep 15 seconds on " + thread.getName());
        Thread.sleep(15_000);
        System.out.println("Awake again on " + thread.getName());
    }

    @Configuration
    @EnableAspectJAutoProxy
    public static class SpringConfig {

        @Bean
        public HystrixCommandAspect hystrixCommandAspect() {
            return new HystrixCommandAspect();
        }

        @Bean
        public HttpClient sender() {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5_000)
                    .setSocketTimeout(15_000)
                    .build();
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", SSLConnectionSocketFactory.getSocketFactory())
                    .build();
            CloseableHttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build();
            return client;
        }

        @Bean
        public RemoteHttpClientCallService remoteCallService(HttpClient client) {
            return new RemoteHttpClientCallService(client);
        }

    }

}