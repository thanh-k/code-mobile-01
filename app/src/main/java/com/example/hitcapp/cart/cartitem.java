package com.example.hitcapp.cart;

import org.json.JSONObject;

public class cartitem {
    public String id;
    public String name;
    public String imageUrl;
    public double unitPrice;
    public int qty;

    public cartitem(String id, String name, String imageUrl, double unitPrice, int qty) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.qty = qty;
    }

    public double getLineTotal() { return unitPrice * qty; }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("imageUrl", imageUrl);
            o.put("unitPrice", unitPrice);
            o.put("qty", qty);
        } catch (Exception ignored) {}
        return o;
    }

    public static cartitem fromJson(JSONObject o) {
        if (o == null) return null;
        return new cartitem(
                o.optString("id",""),
                o.optString("name",""),
                o.optString("imageUrl",""),
                o.optDouble("unitPrice", 0),
                o.optInt("qty", 1)
        );
    }
}