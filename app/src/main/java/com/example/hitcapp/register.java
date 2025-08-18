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
        etPhone    = findViewById(R.id.etPhone);
        etEmail    = findViewById(R.id.etEmail);
        etAddress  = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);
        radioGender= findViewById(R.id.radioGender);
        btnRegister= findViewById(R.id.btnRegister);
        tvLogin    = findViewById(R.id.tvLogin);

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
        String name  = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();   // KHÔNG bắt buộc
        String addr  = etAddress.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        int selectedId = radioGender.getCheckedRadioButtonId();
        RadioButton selectedRadio = selectedId != -1 ? findViewById(selectedId) : null;
        String gender = selectedRadio != null ? selectedRadio.getText().toString() : "";

        // ========== VALIDATION ==========
        // Họ tên
        if (name.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            etFullName.requestFocus();
            return;
        }

        // SĐT: bắt buộc, 10 số, bắt đầu bằng 0
        if (phone.isEmpty()) {
            etPhone.setError("Vui lòng nhập SĐT");
            etPhone.requestFocus();
            return;
        }
        if (!phone.matches("^0\\d{9}$")) {
            etPhone.setError("SĐT phải 10 số và bắt đầu bằng 0");
            etPhone.requestFocus();
            return;
        }

        // Email: KHÔNG bắt buộc;
        if (!email.isEmpty()) {
            int at = email.indexOf('@');
            if (at <= 0 || at >= email.length() - 1) {
                etEmail.setError("Email không hợp lệ (phải có '@')");
                etEmail.requestFocus();
                return;
            }
        }

        // Địa chỉ
        if (addr.isEmpty()) {
            etAddress.setError("Vui lòng nhập địa chỉ");
            etAddress.requestFocus();
            return;
        }

        // Mật khẩu: tối thiểu 6 ký tự
        if (pass.isEmpty()) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }
        if (pass.length() < 6) {
            etPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        // Giới tính
        if (gender.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show();
            radioGender.requestFocus();
            return;
        }

        // ========== GỬI API ==========
        btnRegister.setEnabled(false);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), login.class));
                    finish();
                },
                error -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Lỗi: " + error.toString(), Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                // Tạo map chứa dữ liệu gửi đi
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("phone", phone);
                if (!email.isEmpty()) { // email không bắt buộc
                    params.put("email", email);
                }
                params.put("address", addr);
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
