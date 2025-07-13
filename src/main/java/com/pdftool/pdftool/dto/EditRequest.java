package com.pdftool.pdftool.dto;


import lombok.NoArgsConstructor;

public class EditRequest {

    private int pageNumber;  // 0-based page index
    private float x;         // X-coordinate in points
    private float y;         // Y-coordinate in points
    private float width;     // Width of the rectangle to cover (for editing existing text)
    private float height;    // Height of the rectangle to cover
    private String newText;  // New text to add

    public EditRequest() {}

    public EditRequest(int pageNumber, float x, float y, float width, float height, String newText) {
        this.pageNumber = pageNumber;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.newText = newText;
    }

    // Getters and setters
    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public String getNewText() {
        return newText;
    }

    public void setNewText(String newText) {
        this.newText = newText;
    }
}
