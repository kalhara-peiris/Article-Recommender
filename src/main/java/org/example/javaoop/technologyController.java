package org.example.javaoop;

public class technologyController extends categoryController {
    @Override
    public String getCategoryId() {
        return "C01"; // Technology category ID
    }

    @Override
    public String getCategoryCssFile() {
        return "technology.css";
    }
}
