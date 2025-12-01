package org.zerock.finance_dwpj1.service.stock.KIS;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.zerock.finance_dwpj1.config.StockKisConfig;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KisApiTokenService {

    private final StockKisConfig stockKISConfig;
    private final OkHttpClient okHttpClient;

    private String accessToken;
    private LocalDateTime expiredAt;

    public String getAccessToken() {
        if (accessToken == null||expiredAt==null||expiredAt.isBefore(LocalDateTime.now())) {
            refreshToken();
        }
            return accessToken;

    }

    private void refreshToken() {

        String url = stockKISConfig.getDomain() + "/oauth2/tokenP";

        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "client_credentials");
        json.addProperty("appkey", stockKISConfig.getAppKey());
        json.addProperty("appsecret", stockKISConfig.getAppSecret());

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("content-type", "application/json")
                .build();

        try(Response response = okHttpClient.newCall(request).execute()){
            if(!response.isSuccessful()){
                throw new IllegalStateException("토큰 발급 실패 원인 : "+response.code());
            }

            String bodyStr = response.body().string();
            JsonObject res = JsonParser.parseString(bodyStr).getAsJsonObject();

            this.accessToken = res.get("access_token").getAsString();
            int expiresIn = res.get("expires_in").getAsInt();

            this.expiredAt = LocalDateTime.now().plusSeconds(expiresIn);

            System.out.println("토큰 발급");
            System.out.println("엑세스 토큰 : "+accessToken);
            System.out.println("만료 기한 : "+expiredAt);
        }

        catch (IOException e){
            throw new RuntimeException("토큰 발급 중 오류", e);
        }



    
    }
}



