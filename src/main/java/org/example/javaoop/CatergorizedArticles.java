package org.example.javaoop;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.sql.DriverManager.getConnection;

public class CatergorizedArticles extends Articles{

    //Variables for category
    private String categoryId;
    private String title;
    private String url;
    private String content;
    private Map<String,Double> categoryScores = new HashMap<>();


    private static final Map <String, Set<String>> KEYWORDS = new HashMap<>();
    private static final Map<String, Double> KEYWORD_WEIGHTS = new HashMap<>();

    static {
        // Technology keywords
        Set<String> techKeywords = new HashSet<>(Arrays.asList(
                "technology", "tech", "software", "hardware", "computer", "smartphone", "internet",
                "digital", "app", "device", "mobile", "innovation", "cybersecurity", "programming",
                "server", "gadget","smgs","ars", "apple", "ios", "ipod", "iphone", "itunes", "firmware",
                "download", "virtual machine", "vm", "drm", "sync", "gaming platform"
        ));
        KEYWORDS.put("C01", techKeywords);

        // Health keywords
        Set<String> healthKeywords = new HashSet<>(Arrays.asList(
                "health", "medical", "wellness", "disease", "treatment", "hospital", "doctor",
                "patient", "medicine", "healthcare", "virus", "vaccine", "vaccination", "infection",
                "clinic","clinical","exercise", "disease", "immunisation", "world health organization", "polio",
                "medical", "patients", "doses", "symptoms","diagnosis","outbreak", "epidemic", "pandemic",
                "public health", "health ministry","Therapist", "medical supplies", "humanitarian"
        ));
        KEYWORDS.put("C02", healthKeywords);

        // Sports keywords
        Set<String> sportsKeywords = new HashSet<>(Arrays.asList(
                "sport", "team", "player", "football", "basketball", "baseball", "league",
                "soccer", "match", "tournament", "championship", "athlete", "coach", "olympic",
                "win", "score", "racing", "competition", "stadium", "game score", "sports",
                "final", "semifinal", "playoffs", "sports federation", "athletics"
        ));
        KEYWORDS.put("C03", sportsKeywords);

        // AI keywords
        Set<String> aiKeywords = new HashSet<>(Arrays.asList(
                "ai", "artificial intelligence", "machine learning", "deep learning", "neural network",
                "algorithm", "data science", "chatbot", "ai model", "robotics", "nlp",
                "computer vision", "big data", "predictive", "autonomous", "training data",
                "ai system", "machine intelligence", "neural", "artificial neural", "ai technology",
                "language model", "chatgpt", "openai", "tensorflow", "pytorch"
        ));
        KEYWORDS.put("C04", aiKeywords);



        Set<String> scienceSpaceKeywords = new HashSet<>(Arrays.asList(
                "nasa", "space", "astronomy", "planet", "mars", "satellite", "rover", "mission",
                "spacecraft", "launch", "orbit", "galaxy", "solar system", "cosmic", "astronomical",
                "scientist", "research", "discovery", "experiment", "laboratory", "physics",
                "chemistry", "biology", "telescope", "asteroid", "moon", "earth", "space station",
                "spacex", "rocket", "shuttle", "astronaut", "probe","dinosaur", "exploration",
                "chemistry", "protein", "biochemistry", "nobel prize in chemistry",
                "scientific discovery", "research paper", "scientific journal"
        ));
        KEYWORDS.put("C06", scienceSpaceKeywords);



        initializeKeywordWeights();
    }

    private final ExecutorService executorService;
    private final int maxConcurrentConnections;
    private final AtomicInteger processedArticles = new AtomicInteger(0);

    public CatergorizedArticles(int maxConcurrentConnections) {
        this.maxConcurrentConnections = maxConcurrentConnections;
        this.executorService = Executors.newFixedThreadPool(maxConcurrentConnections);
    }

    private static void initializeKeywordWeights() {
        // Technology weights
        KEYWORD_WEIGHTS.put("technology", 2.0);
        KEYWORD_WEIGHTS.put("tech", 2.0);
        KEYWORD_WEIGHTS.put("game", 2.0);
        KEYWORD_WEIGHTS.put("hardware", 1.8);
        KEYWORD_WEIGHTS.put("computer", 1.8);
        KEYWORD_WEIGHTS.put("smartphone", 1.8);
        KEYWORD_WEIGHTS.put("digital", 1.5);
        KEYWORD_WEIGHTS.put("app", 1.5);


        // Health specific weights
        KEYWORD_WEIGHTS.put("vaccine", 2.0);
        KEYWORD_WEIGHTS.put("vaccination", 2.0);
        // Health specific weights - top 5 most important
        KEYWORD_WEIGHTS.put("health", 3.0);       // Core term, highest weight
        KEYWORD_WEIGHTS.put("medical",3.0);      // Core medical term
        KEYWORD_WEIGHTS.put("disease", 2.2);      // Key health condition indicator
        KEYWORD_WEIGHTS.put("patient", 2.0);      // Strong medical context
        KEYWORD_WEIGHTS.put("treatment", 2.0);    // Important medical procedure


        // Sports weights
        KEYWORD_WEIGHTS.put("championship", 2.0);
        KEYWORD_WEIGHTS.put("tournament", 1.8);
        KEYWORD_WEIGHTS.put("playoffs", 1.8);
        KEYWORD_WEIGHTS.put("football", 1.5);
        KEYWORD_WEIGHTS.put("basketball", 1.5);
        KEYWORD_WEIGHTS.put("soccer", 1.5);



        //AI
        KEYWORD_WEIGHTS.put("chatgpt", 3.0);
        KEYWORD_WEIGHTS.put("artificial intelligence", 4.0);
        KEYWORD_WEIGHTS.put("machine learning", 4.0);
        KEYWORD_WEIGHTS.put("openai", 5.0);
        KEYWORD_WEIGHTS.put("large language model", 2.5);
        KEYWORD_WEIGHTS.put("generative ai", 2.5);
        KEYWORD_WEIGHTS.put("llm", 2.0);
        KEYWORD_WEIGHTS.put("ai model", 2.0);
        KEYWORD_WEIGHTS.put("neural network", 2.0);
        KEYWORD_WEIGHTS.put("deep learning", 2.0);




        // Title keywords get extra weight
        KEYWORD_WEIGHTS.put("TITLE_MULTIPLIER", 2.0);
    }
    public void categorizeAndStoreArticles() {
        try (Connection conn = getConnection()) {
            // Get all articles from Article table
            String selectQuery = "SELECT ArticleID, title, url FROM Article";
            PreparedStatement pstmt = conn.prepareStatement(selectQuery);
            ResultSet rs = pstmt.executeQuery();

            List<Future<?>> futures = new ArrayList<>();
            BlockingQueue<Connection> connectionPool = new ArrayBlockingQueue<>(maxConcurrentConnections);
            for (int i = 0; i < maxConcurrentConnections; i++) {
                connectionPool.offer(getConnection());
            }


            while (rs.next()) {
                String articleId = rs.getString("ArticleID");
                String title = rs.getString("title");
                String url = rs.getString("url");

                Future<?> future = executorService.submit(() -> {
                    try {
                        Connection pooledConn = connectionPool.take();
                        try {
                            processArticle(pooledConn, articleId, title, url);
                        } finally {
                            connectionPool.put(pooledConn);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                futures.add(future);
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error processing article: " + e.getMessage());
                }
            }
            // Close all connections in the pool
            connectionPool.forEach(pooledConn -> {
                try {
                    pooledConn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            });
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }finally{
            executorService.shutdown();
        }
    }

    private void processArticle(Connection conn, String articleId, String title, String url) {
        try {
            String content = fetchArticleContent(url);
            if (!content.isEmpty()) {
                String categoryId = determineCategory(content.toLowerCase());
                storeCategorizationResult(conn, articleId, title, url, categoryId);

                // Update progress
                int processed = processedArticles.incrementAndGet();
                if (processed % 10 == 0) {  // Log every 10 articles
                    System.out.println("Processed " + processed + " articles");
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing article " + articleId + ": " + e.getMessage());
        }
    }

    private String fetchArticleContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .maxBodySize(0)
                    .followRedirects(true)
                    .get();

            Elements contentElements = doc.select("article, .article-content, .story-content, .main-content, .post-content");
            if (!contentElements.isEmpty()) {
                StringBuilder contentBuilder = new StringBuilder();
                for (Element element : contentElements) {
                    contentBuilder.append(element.text()).append(" ");
                }
                return contentBuilder.toString().trim();
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String determineCategory(String content) {
        categoryScores.clear();

        // Initialize scores for each category
        for (String categoryId : KEYWORDS.keySet()) {
            categoryScores.put(categoryId, 0.0);
        }

        // Calculate scores based on keywords
        for (Map.Entry<String, Set<String>> entry : KEYWORDS.entrySet()) {
            String categoryId = entry.getKey();
            Set<String> keywords = entry.getValue();

            double score = calculateCategoryScore(content, keywords);
            categoryScores.put(categoryId, score);
        }

        // Find category with highest score
        return categoryScores.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("C07"); // Default to General category if no matches
    }

    private double calculateCategoryScore(String text, Set<String> keywords) {
        double score = 0.0;

        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                // Apply keyword weight if exists, otherwise use 1.0
                double weight = KEYWORD_WEIGHTS.getOrDefault(keyword, 1.0);
                score += weight;

                // Apply title multiplier if keyword is in title
                if (text.equals(this.title)) {
                    score *= KEYWORD_WEIGHTS.get("TITLE_MULTIPLIER");
                }
            }
        }

        return score;
    }

    private void storeCategorizationResult(Connection conn, String articleId, String title,
                                           String url, String categoryId) {
        String insertQuery = "INSERT IGNORE INTO ArticleCategory " +
                "(ArticleID, title, url, categoryID) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            pstmt.setString(1, articleId);
            pstmt.setString(2, title);
            pstmt.setString(3, url);
            pstmt.setString(4, categoryId);

            pstmt.executeUpdate();
            System.out.println("Categorized article: " + title + " as " + categoryId);

        } catch (SQLException e) {
            System.err.println("Error storing categorization: " + e.getMessage());
        }
    }

    public void processCategorization() {
        try {
            System.out.println("Starting article categorization...");
            long startTime = System.currentTimeMillis();
            categorizeAndStoreArticles();
            long endTime = System.currentTimeMillis();
            System.out.println("Completed article categorization."+((endTime-startTime)/1000)+"seconds");
            System.out.println("Total articles processed: " + processedArticles.get());
        } catch (Exception e) {
            System.err.println("Error in categorization process: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        // Use number of available processors for concurrent connections
        int maxConcurrentConnections = Runtime.getRuntime().availableProcessors() * 2;
        CatergorizedArticles categorizer = new CatergorizedArticles(maxConcurrentConnections);
        categorizer.processCategorization();
    }
}



