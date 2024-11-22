package org.example.javaoop;

public class scienceController extends categoryController {
    @Override
    public String getCategoryId() {
        return "C06"; // Health category ID
    }

    @Override
    public String getCategoryCssFile() {
        return "technology.css"; // Using same CSS file as shown in original code
    }
}