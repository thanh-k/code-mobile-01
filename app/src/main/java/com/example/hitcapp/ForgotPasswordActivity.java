package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String USERS_API = "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    private EditText edtPhone, edtNewPass, edtConfirm;
    private View btnReset, pb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtPhone   = findViewById(R.id.edtPhone);
        edtNewPass = findViewById(R.id.edtNewPass);
        edtConfirm = findViewById(R.id.edtConfirm);
        btnReset   = findViewById(R.id.btnReset);
        pb         = findViewById(R.id.pb);

        btnReset.setOnClickListener(v -> doReset());
    }

    private void doReset() {
        String phoneInput = edtPhone.getText().toString().trim();
        String pass1 = edtNewPass.getText().toString();
        String pass2 = edtConfirm.getText().toString();

        if (TextUtils.isEmpty(phoneInput)) { toast("Vui lòng nhập số điện thoại"); return; }
        if (pass1.length() < 6)            { toast("Mật khẩu tối thiểu 6 ký tự"); return; }
        if (!pass1.equals(pass2))          { toast("Mật khẩu xác nhận không khớp"); return; }

        setLoading(true);

        // Lấy toàn bộ user rồi lọc theo phone (đã chuẩn hoá)
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, USERS_API, null,
                arr -> handleSearchByPhone(phoneInput, pass1, arr),
                err -> { setLoading(false); toast("Lỗi kết nối, thử lại sau"); }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(getApplicationContext()).add(req);
    }

    private void handleSearchByPhone(String rawPhone, String newPass, JSONArray arr) {
        String target = normPhone(rawPhone);
        JSONObject found = null;

        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String p = o.optString("phone", "");
                if (equalsPhone(target, normPhone(p))) { found = o; break; }
            }
        }

        if (found == null) {
            setLoading(false);
            toast("Không tìm thấy tài khoản với SĐT đã nhập");
            return;
        }

        String userId = found.optString("id", null);
        if (TextUtils.isEmpty(userId)) {
            setLoading(false);
            toast("Không xác định được tài khoản");
            return;
        }

        // Cập nhật mật khẩu: ưu tiên PATCH 'pass', fallback PUT nếu PATCH lỗi
        patchPassword(userId, newPass, found);
    }

    /* ------------------- UPDATE ------------------- */

    private void patchPassword(String userId, String newPass, JSONObject originalUser) {
        try {
            JSONObject body = new JSONObject();
            body.put("pass", newPass); // <<< login đang dùng field 'pass'

            String url = USERS_API + "/" + userId;
            JsonObjectRequest patch = new JsonObjectRequest(
                    Request.Method.PATCH, url, body,
                    res -> onUpdateSuccess(),
                    err -> putPassword(userId, newPass, originalUser) // fallback
            );
            patch.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            Volley.newRequestQueue(getApplicationContext()).add(patch);
        } catch (Exception e) {
            setLoading(false);
            toast("Đã xảy ra lỗi");
        }
    }

    // Fallback nếu PATCH không được hỗ trợ trên môi trường của bạn
    private void putPassword(String userId, String newPass, JSONObject originalUser) {
        try {
            JSONObject body = new JSONObject(originalUser.toString());
            body.put("pass", newPass);
            // MockAPI không cần 'id' trong body cho PUT; bỏ đi để an toàn
            body.remove("id");

            String url = USERS_API + "/" + userId;
            JsonObjectRequest put = new JsonObjectRequest(
                    Request.Method.PUT, url, body,
                    res -> onUpdateSuccess(),
                    err -> {
                        setLoading(false);
                        toast("Không thể cập nhật mật khẩu");
                    }
            );
            put.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            Volley.newRequestQueue(getApplicationContext()).add(put);
        } catch (Exception e) {
            setLoading(false);
            toast("Đã xảy ra lỗi");
        }
    }

    private void onUpdateSuccess() {
        setLoading(false);
        toast("Đặt lại mật khẩu thành công");

        // Về trang đăng nhập và xoá back stack
        Intent i = new Intent(ForgotPasswordActivity.this, login.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    /* ------------------- Helpers ------------------- */

    private void setLoading(boolean b) {
        pb.setVisibility(b ? View.VISIBLE : View.GONE);
        btnReset.setEnabled(!b);
    }

    private void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    // Chuẩn hoá số VN: bỏ ký tự không phải số; chuyển 84xxxxxxxxx -> 0xxxxxxxxx
    private String normPhone(String p) {
        if (p == null) return "";
        String d = p.replaceAll("[^0-9]", "");
        if (d.startsWith("84")) d = "0" + d.substring(2);
        return d;
    }

    private boolean equalsPhone(String a, String b) {
        return !TextUtils.isEmpty(a) && a.equals(b);
    }
}
