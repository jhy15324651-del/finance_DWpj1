package org.zerock.finance_dwpj1.service.stock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;



@Service
public class StockNewsService {

    private final OkHttpClient client = new OkHttpClient();

    public String getNews(String ticker) {

        // 6자리면 = 한국 종목
        if (ticker.matches("^\\d{6}$") || ticker.endsWith(".KS") || ticker.endsWith(".KQ")) {
            return getKoreanNews(ticker);
        } else {
            return getForeignNews(ticker);
        }
    }


    // 🇰🇷 한국 뉴스 (Google News RSS)


    private String getKoreanNews(String ticker) {

        try {
            String keyword = ticker;

            // 예: 005930 → 삼성전자
            // 주식명 DB가 있으면 변환해서 쓰는 것이 베스트
            // 우선은 ticker 그대로 사용해도 됨

            String url = "https://news.google.com/rss/search?q=" + keyword;

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();

            Elements items = doc.select("item");

            JsonArray newsArr = new JsonArray();

            for (Element item : items) {
                JsonObject obj = new JsonObject();
                obj.addProperty("title", item.select("title").text());
                obj.addProperty("link", item.select("link").first().text());
                obj.addProperty("publisher", "Google News");
                obj.addProperty("providerPublishTime", 0);

                newsArr.add(obj);
            }

            JsonObject result = new JsonObject();
            result.add("news", newsArr);

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"news\":[]}";
        }
    }

    //  해외 뉴스 (Yahoo Search API)


    private String getForeignNews(String ticker) {

        String url = "https://query1.finance.yahoo.com/v1/finance/search?q=" + ticker;

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    return "{\"news\":[]}";
                }

                return extractYahooNews(response.body().string());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"news\":[]}";
        }
    }

    // Yahoo 뉴스 파싱

    private String extractYahooNews(String json) {

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        JsonArray resultArr = new JsonArray();

        if (root.has("news")) {
            JsonArray arr = root.getAsJsonArray("news");

            for (JsonElement el : arr) {
                JsonObject n = el.getAsJsonObject();

                JsonObject item = new JsonObject();
                item.addProperty("title", n.has("title") ? n.get("title").getAsString() : "");
                item.addProperty("link", n.has("link") ? n.get("link").getAsString() : "");
                item.addProperty("publisher", n.has("publisher") ? n.get("publisher").getAsString() : "");
                item.addProperty("providerPublishTime",
                        n.has("providerPublishTime") ? n.get("providerPublishTime").getAsLong() : 0);

                resultArr.add(item);
            }
        }

        JsonObject result = new JsonObject();
        result.add("news", resultArr);
        return result.toString();
    }
}