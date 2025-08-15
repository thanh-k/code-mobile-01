package com.example.hitcapp.cart;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class CartRepository {
    private static final String PREF = "cart_prefs";
    private static final String KEY  = "cart_json";

    private static CartRepository instance;
    private final Context appCtx;
    private final List<cartitem> cache = new ArrayList<>();

    private CartRepository(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
        load();
    }

    public static synchronized CartRepository getInstance(Context ctx) {
        if (instance == null) instance = new CartRepository(ctx);
        return instance;
    }

    public synchronized List<cartitem> getItems() {
        return new ArrayList<>(cache);
    }

    public synchronized void addOrInc(String id, String name, String imageUrl, double price) {
        for (cartitem c : cache) {
            if (c.id.equals(id)) { c.qty += 1; save(); return; }
        }
        cache.add(new cartitem(id, name, imageUrl, price, 1));
        save();
    }

    public synchronized void updateQty(String id, int newQty) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(id)) {
                if (newQty <= 0) cache.remove(i);
                else cache.get(i).qty = newQty;
                save(); return;
            }
        }
    }

    public synchronized void remove(String id) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(id)) { cache.remove(i); break; }
        }
        save();
    }

    public synchronized void clear() { cache.clear(); save(); }

    public synchronized double getSubtotal() {
        double s = 0;
        for (cartitem c : cache) s += c.getLineTotal();
        return s;
    }

    /* ---------- persistence ---------- */
    private void load() {
        SharedPreferences sp = appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY, "[]");
        cache.clear();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                cartitem it = cartitem.fromJson(arr.optJSONObject(i));
                if (it != null) cache.add(it);
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        JSONArray arr = new JSONArray();
        for (cartitem c : cache) arr.put(c.toJson());
        appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY, arr.toString()).apply();
    }
}
