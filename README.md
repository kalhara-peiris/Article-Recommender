# News Article Recommender with JavaFX and SQL Database

This project is a news article recommender system built using JavaFX for the graphical user interface (GUI) and SQL for managing user preferences and article data. The system recommends news articles based on user preferences, utilizing a preference score mechanism. It also categorizes articles through keyword extraction and records user interaction details in the database. Articles are fetched via a news API.

## Features

- **News Article Recommendation**: Recommends news articles based on user preferences, with a preference score used to personalize recommendations.
- **Keyword Extraction**: Categorizes articles using keyword extraction methods.
- **Database Integration**: Stores user details, preferences, article interactions (like, read, skip, etc.), and article data in an SQL database.
- **News API Integration**: Fetches articles from a news API for real-time updates and recommendations.
- **User Interaction Tracking**: Records user interactions with articles and updates the user preferences based on those interactions.

## Technologies Used

- **JavaFX**: For building the graphical user interface.
- **SQL Database**: For storing user preferences, article data, and user interactions.
- **Java**: The primary programming language for backend logic.
- **News API**: For fetching real-time news articles.
- **Keyword Extraction**: Used for article categorization based on extracted keywords.

## Setup

### Prerequisites

- Java 8 or later.
- JavaFX SDK.
- MySQL or any other SQL-compatible database.
- Access to a News API (e.g., NewsAPI or similar).

### Installation Steps

1. Clone this repository:

   ```bash
   git clone https://github.com/your-username/news-article-recommender.git
