package com.example.hitcapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.EditProfileActivity;
import com.example.hitcapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private static final String API_BASE =
            "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    // Views
    private ImageView imgAvatar, imgCover;
    private TextView tvUserName, tvUserId, tvEmail, tvPhone, tvAddress;
    private Button btnEditProfile;

    // Lưu id user hiện tại
    private String currentUserId;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_profile_fragment, container, false);

        // Bind views
        ImageButton btnSettings = v.findViewById(R.id.btnSettings);
        imgCover   = v.findViewById(R.id.imgCover);
        imgAvatar  = v.findViewById(R.id.imgAvatar);
        tvUserName = v.findViewById(R.id.tvUserName);
        tvUserId   = v.findViewById(R.id.tvUserId);
        tvEmail    = v.findViewById(R.id.tvEmail);
        tvPhone    = v.findViewById(R.id.tvPhone);
        tvAddress  = v.findViewById(R.id.tvAddress);
        btnEditProfile = v.findViewById(R.id.btnEditProfile);

        if (btnSettings != null) {
            btnSettings.setOnClickListener(_x ->
                    Toast.makeText(requireContext(), "Mở cài đặt (chưa làm)", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(_x -> {
                Intent i = new Intent(requireContext(), EditProfileActivity.class);
                if (currentUserId != null && !currentUserId.isEmpty()) {
                    i.putExtra("user_id", currentUserId);
                } else {
                    String savedId = getSavedUserId(requireContext());
                    if (savedId != null && !savedId.isEmpty()) i.putExtra("user_id", savedId);
                }
                startActivity(i);
            });
        }

        fetchUserInitial();
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        // Quay lại từ màn Edit → refresh
        fetchUserInitial();
    }

    private void fetchUserInitial() {
        String savedId = getSavedUserId(requireContext());
        if (savedId != null && !savedId.isEmpty()) {
            fetchUserById(savedId);
        } else if (currentUserId != null && !currentUserId.isEmpty()) {
            fetchUserById(currentUserId);
        } else {
            fetchFirstUser();
        }
    }

    /* ---------------- API ---------------- */

    private void fetchUserById(String id) {
        String url = API_BASE + "/" + id;
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                this::bindUser,
                err -> fetchFirstUser()
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void fetchFirstUser() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, API_BASE, null,
                arr -> {
                    JSONObject o = (arr != null && arr.length() > 0) ? arr.optJSONObject(0) : null;
                    if (o != null) bindUser(o);
                    else Toast.makeText(requireContext(), "Không có dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                },
                err -> Toast.makeText(requireContext(), "Lỗi tải hồ sơ", Toast.LENGTH_SHORT).show()
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    /* --------------- BIND UI --------------- */

    private void bindUser(JSONObject o) {
        currentUserId = o.optString("id", currentUserId);
        String name    = o.optString("name", "");
        String email   = o.optString("email", "");
        String phone   = o.optString("phone", "");
        String address = o.optString("address", "");

        // Ưu tiên base64, fallback URL
        String avatarSrc = o.optString("img_b64",  o.optString("img", ""));
        String coverSrc  = o.optString("cover_b64",o.optString("cover", ""));

        tvUserName.setText(name);
        tvUserId.setText(currentUserId != null && !currentUserId.isEmpty() ? "@" + currentUserId : "");
        tvEmail.setText("📧 Email: " + email);
        tvPhone.setText("📞 SĐT: " + phone);
        tvAddress.setText("📍 Địa chỉ: " + address);

        loadImageSmart(coverSrc,  imgCover);
        loadImageSmart(avatarSrc, imgAvatar);
    }

    /* --------------- Image helpers (URL or Base64) --------------- */

    private void loadImageSmart(String src, ImageView target) {
        if (target == null) return;
        if (src == null || src.isEmpty()) {
            target.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }

        // Base64 / data URL?
        if (isDataUrl(src) || looksLikeBase64(src)) {
            String pure = stripDataUrlPrefix(src);
            try {
                byte[] bytes = Base64.decode(pure, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    target.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
            target.setImageResource(android.R.drawable.ic_delete);
            return;
        }

        // URL → dùng Volley
        target.setImageResource(android.R.drawable.ic_menu_report_image);
        ImageRequest imgReq = new ImageRequest(
                src,
                target::setImageBitmap,
                0, 0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> target.setImageResource(android.R.drawable.ic_delete)
        );
        Volley.newRequestQueue(requireContext()).add(imgReq);
    }

    private static boolean isDataUrl(String s) {
        return s != null && s.startsWith("data:image/");
    }

    private static String stripDataUrlPrefix(String s) {
        int idx = s.indexOf(','); // "data:image/...;base64,XXXX"
        return idx >= 0 ? s.substring(idx + 1) : s;
    }

    private static boolean looksLikeBase64(String s) {
        // Heuristic: chuỗi dài, không bắt đầu bằng http/https
        return s != null && !s.startsWith("http") && s.length() > 200;
    }

    /* --------------- Helpers --------------- */

    private String getSavedUserId(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences("app_session", Context.MODE_PRIVATE)
                .getString("user_id", null);
    }
}
