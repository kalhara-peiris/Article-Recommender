package org.example.javaoop;

public class technologyController extends categoryController{
    @Override
    protected String getCategoryId() {
        return "C01"; // Technology category ID
    }

    @Override
    protected String getCategoryCssFile() {
        return "technology.css";
    }
}
