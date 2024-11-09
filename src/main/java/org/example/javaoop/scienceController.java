package org.example.javaoop;

public class scienceController extends categoryController {
    @Override
    protected String getCategoryId() {
        return "C06"; // Health category ID
    }

    @Override
    protected String getCategoryCssFile() {
        return "technology.css"; // Using same CSS file as shown in original code
    }
}