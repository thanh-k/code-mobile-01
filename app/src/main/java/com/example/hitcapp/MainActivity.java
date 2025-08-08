package com.example.hitcapp;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.fragments.HomeFragment;
import com.example.hitcapp.fragments.ProductsFragment;
import com.example.hitcapp.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    RequestQueue mRequestQueue;
    StringRequest mStringRequest;
    String url = "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Hiển thị HomeFragment mặc định
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new HomeFragment()).commit();

        // Xử lý chọn icon bằng if-else
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_products) {
                selectedFragment = new ProductsFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_layout, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        getData(); // Gọi hàm lấy dữ liệu và hiển thị lời chào
    }

    private void getData() {
        // Lấy dữ liệu từ Intent
        String name = getIntent().getStringExtra("name");
        String phone = getIntent().getStringExtra("phone");

        // Hiển thị lời chào
        if (name != null && !name.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Xin chào " + name + " (" + phone + ")", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Xin chào!", Toast.LENGTH_LONG).show();
        }

        // Gọi API
        mRequestQueue = Volley.newRequestQueue(this);
        mStringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Response: " + response);
                        // Có thể xử lý thêm dữ liệu tại đây nếu muốn
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, "Error: " + error.toString());
                    }
                });

        mRequestQueue.add(mStringRequest);
    }
}
