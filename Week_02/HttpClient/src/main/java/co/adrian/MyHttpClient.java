package co.adrian;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class MyHttpClient {
    OkHttpClient client;
    MyHttpClient() {
        client = new OkHttpClient();
    }
    public String get(String url)  {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException error) {
            return "request error";
        }
    }

}
