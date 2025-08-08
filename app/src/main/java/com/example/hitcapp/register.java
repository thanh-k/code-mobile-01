package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.*;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    EditText etFullName, etPhone, etEmail, etAddress, etPassword;
    RadioGroup radioGender;
    Button btnRegister;
    TextView tvLogin;

    // Địa chỉ API
    String url = "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ view
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);
        radioGender = findViewById(R.id.radioGender);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Chuyển sang màn Login
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(register.this, login.class);
            startActivity(intent);
        });

        // Sự kiện khi bấm nút "Đăng ký"
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        // Lấy dữ liệu nhập vào
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();


        int selectedId = radioGender.getCheckedRadioButtonId();
        RadioButton selectedRadio = findViewById(selectedId);
        String gender = selectedRadio != null ? selectedRadio.getText().toString() : "";


        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gửi request đến API bằng Volley
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(getApplicationContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), login.class));
                    finish();
                },
                error -> {
                    Toast.makeText(getApplicationContext(), "Lỗi: " + error.toString(), Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                // Tạo map chứa dữ liệu gửi đi
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("phone", phone);
                params.put("email", email);
                params.put("address", address);
                params.put("pass", pass);
                params.put("gender", gender);
                return params;
            }
        };

        // Thêm request vào hàng đợi
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
