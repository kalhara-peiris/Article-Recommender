package org.example.javaoop;

public class AiController extends categoryController {
    @Override
    public String getCategoryId() {
        return "C04"; // Health category ID
    }

    @Override
    public String getCategoryCssFile() {
        return "technology.css"; // Using same CSS file as shown in original code
    }
}