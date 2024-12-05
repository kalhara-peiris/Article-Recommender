package org.example.javaoop;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class
Article {
    private static final String API_KEY = "4e6f822740374499be7c2a5f6d721592";

    //Make them protected so child class can access
    protected static final String dbUrl = "jdbc:mysql://localhost:3306/javacw";
    protected static final String dbUser = "root";
    protected static final String dbPassword = "";
    private final String articleId;
    private final String title;
    private final String url;
    private String category;
    public ArrayList<User> users = new ArrayList<>();


    public Article(String articleId, String title, String url) {
        this.articleId = articleId;
        this.title = title;
        this.url = url;
    }
    public Article(String articleId, String title, String url,String category) {
        this.articleId = articleId;
        this.title = title;
        this.url = url;
        this.category=category;
    }
    public Article(){
        articleId="";
        title="";
        url="";
    }
    public Article(String articleId){
        this.articleId=articleId;
        this.title="";
        this.url="";
        addUser(new User(User.getCurrentUsername()));
    }

    public void addUser(User user) {
        if (!users.contains(user)) {
            users.add(user);
        }
    }
    public String getArticleId() {
        return articleId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    // Map API query terms to category names for better results
    private Map<String, String> categoryQueries = new HashMap<>() {{
        put("technology", "Technology");
        put("health", "Health");
        put("sports", "Sport");
        put("artificial-intelligence", "AI");
        put("science", "Science");
    }};

    public void collectArticles(ProgressCallback progressCallback) {
        Map<String, Integer> successCounts = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        int totalTargetArticles=10;
        int currentTotalArticles=0;

        // Initialize success counts
        for (String category : categoryQueries.values()) {
            successCounts.put(category, 0);
        }

        for (Map.Entry<String, String> entry : categoryQueries.entrySet()) {
            String queryTerm = entry.getKey();
            String category = entry.getValue();
            int pageNumber = 1;
            int maxAttempts = 3; // Try up to 3 pages per category

            System.out.println("\nCollecting articles for category: " + category);

            while (successCounts.get(category) < 2 && pageNumber <= maxAttempts) {
                try {
                    // For AI category, use a different approach
                    String apiUrl;
                    if (category.equals("AI")) {
                        apiUrl = String.format("https://newsapi.org/v2/everything?q=artificial intelligence OR machine learning&language=en&pageSize=100&page=%d&apiKey=%s",
                                pageNumber, API_KEY);
                    } else {
                        apiUrl = String.format("https://newsapi.org/v2/top-headlines?category=%s&language=en&pageSize=100&page=%d&apiKey=%s",
                                queryTerm, pageNumber, API_KEY);
                    }

                    Request request = new Request.Builder()
                            .url(apiUrl)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);

                    if (jsonResponse.getString("status").equals("error")) {
                        System.out.println("API Error for " + category + ": " + jsonResponse.getString("message"));
                        break;
                    }

                    JSONArray articles = jsonResponse.getJSONArray("articles");
                    if (articles.length() == 0) break;

                    for (int i = 0; i < articles.length() && successCounts.get(category) < 2; i++) {
                        JSONObject article = articles.getJSONObject(i);
                        String title = article.optString("title", "").trim();
                        String articleUrl = article.optString("url", "").trim();

                        // Skip problematic URLs and empty titles
                        if (articleUrl.isEmpty() || title.isEmpty() ||
                                articleUrl.contains("news.google.com") ||
                                articleUrl.contains("youtube.com") ||
                                articleUrl.contains("bloomberg.com") ||
                                articleUrl.contains("ft.com")) {
                            continue;
                        }

                        // Check if article is fetchable with Jsoup
                        if (canFetchContent(articleUrl)) {
                            if (storeArticle(title, articleUrl)) {
                                currentTotalArticles++;
                                successCounts.put(category, successCounts.get(category) + 1);
                                double progress = (double) currentTotalArticles / totalTargetArticles;
                                progressCallback.onProgress(progress);
                                System.out.println(category + " - Added article " + successCounts.get(category) + ": " + title);
                            }
                        }
                    }

                    pageNumber++;
                    // Add small delay to prevent API rate limiting
                    Thread.sleep(100);

                } catch (Exception e) {
                    System.err.println("Error processing " + category + ": " + e.getMessage());
                }
            }
        }

        // Print final results
        System.out.println("\nArticle Collection Summary:");
        System.out.println("------------------------");
        for (Map.Entry<String, Integer> entry : successCounts.entrySet()) {
            System.out.printf("%s: %d articles added successfully%n",
                    entry.getKey(), entry.getValue());
        }
    }
    public interface ProgressCallback {
        void onProgress(double progress);
    }

    private boolean canFetchContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            Elements contentElements = doc.select("article, .article-content, .story-content, .main-content, .post-content");
            if (!contentElements.isEmpty()) {
                Elements paragraphs = contentElements.select("p");
                int meaningfulParagraphs = 0;
                for (Element paragraph : paragraphs) {
                    if (paragraph.text().trim().length() > 50) {
                        meaningfulParagraphs++;
                    }
                    if (meaningfulParagraphs >= 3) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean storeArticle(String title, String url) {
        String insertQuery = "INSERT IGNORE INTO article (ArticleID, title, url) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            String articleId = UUID.randomUUID().toString();
            pstmt.setString(1, articleId);
            pstmt.setString(2, title);
            pstmt.setString(3, url);

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.err.println("Database error storing article: " + e.getMessage());
            return false;
        }
    }
}