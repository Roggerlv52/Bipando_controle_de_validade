package com.rogger.bipando.model;

public class UserAuth {
    public String product;
    private String note;
    private String barcod;
    private String date;
    private String uriProfile;
    private String uriProduct;

    public UserAuth() {

    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getBarcod() {
        return barcod;
    }

    public void setBarcod(String barcod) {
        this.barcod = barcod;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUriProfile() {
        return uriProfile;
    }

    public void setUriProfile(String uriProfile) {
        this.uriProfile = uriProfile;
    }

    public String getUriProduct() {
        return uriProduct;
    }

    public void setUriProduct(String uriProduct) {
        this.uriProduct = uriProduct;
    }
}
