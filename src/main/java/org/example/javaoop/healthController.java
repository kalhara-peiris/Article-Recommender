package org.example.javaoop;

public class healthController extends categoryController {
    @Override
    protected String getCategoryId() {
        return "C02"; // Health category ID
    }

    @Override
    protected String getCategoryCssFile() {
        return "technology.css"; // Using same CSS file as shown in original code
    }
}