package org.example.javaoop;

public class sportController extends categoryController {
    @Override
    protected String getCategoryId() {
        return "C03"; // Sports category ID
    }

    @Override
    protected String getCategoryCssFile() {
        return "technology.css"; // Using same CSS file as shown in original code
    }
}