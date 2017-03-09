package cz.dmostek;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

public class RemoteHttpClientCallService {

    public final HttpClient httpClient;

    public RemoteHttpClientCallService(final HttpClient client) {
        this.httpClient = client;
    }

    @HystrixCommand(groupKey = "testGroup", commandKey = "testCall",
            fallbackMethod = "testCallFallback",
            commandProperties = {
                    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "4000")
            })
    public String testCall() throws IOException {
        Thread thread = Thread.currentThread();
        System.out.println("Calling http client on " + thread.getName());
        HttpGet request = new HttpGet("http://www.fakeresponse.com/api/?sleep=10");
        HttpResponse response = httpClient.execute(request);
        String s = IOUtils.toString(response.getEntity().getContent());
        thread = Thread.currentThread();
        System.out.println("Processing response on thread " + thread.getName());
        System.out.println("Response from http call " + s);
        return s;
    }

    public String testCallFallback(Throwable t) {
        Thread thread = Thread.currentThread();
        System.out.println("Fallback called on " + thread.getName());
        if (t == null) {
            System.out.println("Fallback reason null - probably timeout"); // https://github.com/Netflix/Hystrix/issues/974 `Throwable in extended fallback is null for timeouts`
        } else {
            System.out.println("Fallback reason " + t.getMessage());
        }
        return "Fallback response";
    }
}
