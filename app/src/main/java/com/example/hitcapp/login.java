package com.example.hitcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class login extends AppCompatActivity {

    EditText editPhone, editPass;
    String url = "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editPhone    = findViewById(R.id.etPhone);
        editPass     = findViewById(R.id.etPassword);
        Button btnLogin    = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvForgot  = findViewById(R.id.tvForgot);

        btnLogin.setOnClickListener(v -> {
            String inputPhone = editPhone.getText().toString().trim();
            String inputPass  = editPass.getText().toString().trim();

            if (inputPhone.isEmpty() || inputPass.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            checkLogin(inputPhone, inputPass);
        });

        btnRegister.setOnClickListener(v -> {
            Intent it = new Intent(getApplicationContext(), register.class);
            startActivity(it);
        });

        tvForgot.setOnClickListener(v -> {
            Intent it = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
            startActivity(it);
        });
    }

    private void checkLogin(String phone, String pass) {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    boolean isLoginSuccess = false;
                    JSONObject matchedUser = null;

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject user = response.getJSONObject(i);
                            String apiPhone = user.optString("phone", "");
                            String apiPass  = user.optString("pass", "");
                            if (phone.equals(apiPhone) && pass.equals(apiPass)) {
                                matchedUser = user;
                                isLoginSuccess = true;
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (isLoginSuccess && matchedUser != null) {
                        // LƯU ĐẦY ĐỦ THÔNG TIN USER VÀO SESSION
                        SharedPreferences sp = getApplicationContext()
                                .getSharedPreferences("app_session", MODE_PRIVATE);

                        sp.edit()
                                .putString("user_id",      matchedUser.optString("id",""))
                                .putString("user_name",    matchedUser.optString("name",""))
                                .putString("user_phone",   matchedUser.optString("phone",""))
                                .putString("user_email",   matchedUser.optString("email",""))
                                .putString("user_address", matchedUser.optString("address",""))
                                .putString("user_avatar",  matchedUser.optString("img","")) // có thể là URL/base64
                                .apply();

                        Toast.makeText(getApplicationContext(), "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("phone", matchedUser.optString("phone",""));
                        intent.putExtra("name",  matchedUser.optString("name",""));
                        startActivity(intent);
                        finish(); // tránh back về Login
                    } else {
                        Toast.makeText(getApplicationContext(), "Sai số điện thoại hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(getApplicationContext(), "Lỗi kết nối đến server", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }
        );

        queue.add(request);
    }
}
