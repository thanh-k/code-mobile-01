package com.example.hitcapp;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class paymentsuccesshelper {

    private static final String ORDER_URL = "https://68940ddebe3700414e11df37.mockapi.io/log/order";

    private paymentsuccesshelper() {}

    /** Gọi hàm này ngay khi thanh toán thành công */
    public static void postOrderAfterPaymentSuccess(
            Context ctx,
            String idCate,        // ví dụ: "12"
            String idPro,         // ví dụ: "345"
            String productName,   // ví dụ: "iPhone 14"
            String categoryName   // ví dụ: "Điện thoại"
    ) {
        final String creationDate = isoNowUtc(); // "2025-07-06T04:04:13.851Z"

        StringRequest req = new StringRequest(
                Request.Method.POST,
                ORDER_URL,
                response -> {
                    // TODO: xử lý khi log đơn thành công (nếu cần)
                    // Log.d("ORDER", "posted: " + response);
                },
                error -> {
                    // TODO: xử lý khi lỗi (ghi log/hiện toast nếu muốn)
                    // Log.e("ORDER", "error: " + error);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("idcate",       idCate);
                params.put("idpro",        idPro);
                params.put("product",      productName);
                params.put("categories",   categoryName);
                params.put("creationdate", creationDate);
                return params;
            }
        };

        // timeout 10s, retry 2 lần
        req.setRetryPolicy(new DefaultRetryPolicy(
                10_000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(ctx.getApplicationContext()).add(req);
    }

    private static String isoNowUtc() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}