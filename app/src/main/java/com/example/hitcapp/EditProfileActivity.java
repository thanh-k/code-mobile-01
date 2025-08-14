package com.example.hitcapp;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class EditProfileActivity extends AppCompatActivity {

    private static final String API_BASE =
            "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    private ImageView imgCover, imgProfile;
    private Button btnChangeCover, btnChangeAvatar, btnSave;
    private EditText edtUserId, edtName, edtPhone, edtEmail, edtAddress;

    // dữ liệu ảnh đã chọn (Base64), để PUT lên API
    private String coverB64 = null;
    private String avatarB64 = null;
    private String userIdPath = null;

    // pick ảnh từ gallery (không cần xin quyền thủ công)
    private ActivityResultLauncher<String> pickCoverLauncher;
    private ActivityResultLauncher<String> pickAvatarLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile); // đổi đúng layout của bạn

        bindViews();
        setupPickers();

        // không cho sửa id path
        edtUserId.setEnabled(false);

        btnChangeCover.setOnClickListener(v -> pickCoverLauncher.launch("image/*"));
        btnChangeAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());

        // Ưu tiên id truyền qua Intent, rồi tới session, rồi fallback user đầu
        String extraId = getIntent().getStringExtra("user_id");
        if (extraId != null && !extraId.isEmpty()) {
            fetchUserById(extraId);
        } else {
            String savedId = getSavedUserId(this);
            if (savedId != null && !savedId.isEmpty()) fetchUserById(savedId);
            else fetchFirstUser();
        }
    }

    private void bindViews() {
        imgCover        = findViewById(R.id.imgCover);
        imgProfile      = findViewById(R.id.imgProfile);
        btnChangeCover  = findViewById(R.id.btnChangeCover);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnSave         = findViewById(R.id.btnSave);

        edtUserId  = findViewById(R.id.edtUserId);
        edtName    = findViewById(R.id.edtName);
        edtPhone   = findViewById(R.id.edtPhone);
        edtEmail   = findViewById(R.id.edtEmail);
        edtAddress = findViewById(R.id.edtAddress);
    }

    private void setupPickers() {
        pickCoverLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        coverB64 = encodeImageUriToBase64(uri, 1080, 80); // scale + nén
                        imgCover.setImageURI(uri);
                    }
                });

        pickAvatarLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        avatarB64 = encodeImageUriToBase64(uri, 512, 85);
                        imgProfile.setImageURI(uri);
                    }
                });
    }

    /* =================== GET =================== */

    private void fetchUserById(String id) {
        userIdPath = id;
        String url = API_BASE + "/" + id;
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                this::bindUser,
                err -> fetchFirstUser()
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(req);
    }

    private void fetchFirstUser() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, API_BASE, null,
                arr -> {
                    JSONObject o = (arr != null && arr.length() > 0) ? arr.optJSONObject(0) : null;
                    if (o != null) bindUser(o);
                    else Toast.makeText(this, "Không có dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                },
                err -> Toast.makeText(this, "Lỗi tải hồ sơ", Toast.LENGTH_SHORT).show()
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(req);
    }

    /* =================== BIND =================== */

    private void bindUser(JSONObject o) {
        userIdPath = o.optString("id", userIdPath);
        String name    = o.optString("name", "");
        String phone   = o.optString("phone", "");
        String email   = o.optString("email", "");
        String address = o.optString("address", "");

        // Có thể API cũ đang trả link ở "img"/"cover" hoặc trả Base64 ở "img_b64"/"cover_b64"
        String avatarUrlOrB64 = o.optString("img_b64",
                o.optString("img", ""));
        String coverUrlOrB64 = o.optString("cover_b64",
                o.optString("cover", ""));

        edtUserId.setText(userIdPath != null ? userIdPath : "");
        edtName.setText(name);
        edtPhone.setText(phone);

        // tách phần trước @ nếu UI bạn để thêm TextView “@gmail.com” cạnh EditText
        String emailForInput = email;
        int at = email.indexOf('@');
        if (at > 0 && email.substring(at).equalsIgnoreCase("@gmail.com")) {
            emailForInput = email.substring(0, at);
        }
        edtEmail.setText(emailForInput);
        edtAddress.setText(address);

        // hiển thị ảnh hiện có (URL hoặc Base64)
        showUrlOrBase64(coverUrlOrB64, imgCover);
        showUrlOrBase64(avatarUrlOrB64, imgProfile);
    }

    private void showUrlOrBase64(String src, ImageView target) {
        if (src == null || src.isEmpty()) {
            target.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }
        if (isDataUrl(src) || looksLikeBase64(src)) {
            String pureB64 = stripDataUrlPrefix(src);
            byte[] bytes = Base64.decode(pureB64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) target.setImageBitmap(bmp);
            else target.setImageResource(android.R.drawable.ic_delete);
        } else {
            target.setImageResource(android.R.drawable.ic_menu_report_image);
            ImageRequest imgReq = new ImageRequest(
                    src, target::setImageBitmap,
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    error -> target.setImageResource(android.R.drawable.ic_delete)
            );
            Volley.newRequestQueue(this).add(imgReq);
        }
    }

    /* =================== PUT (SAVE) =================== */

    private void saveProfile() {
        if (userIdPath == null || userIdPath.isEmpty()) {
            Toast.makeText(this, "Thiếu ID người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        String name    = edtName.getText().toString().trim();
        String phone   = edtPhone.getText().toString().trim();
        String emailIn = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        if (name.isEmpty())  { edtName.setError("Nhập tên");  edtName.requestFocus();  btnSave.setEnabled(true); return; }
        if (phone.isEmpty()) { edtPhone.setError("Nhập SĐT"); edtPhone.requestFocus(); btnSave.setEnabled(true); return; }

        String email = emailIn.contains("@") ? emailIn : (emailIn.isEmpty() ? "" : emailIn + "@gmail.com");

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("phone", phone);
            body.put("email", email);
            body.put("address", address);

            // ghi Base64 thay vì link:
            if (avatarB64 != null) body.put("img_b64", "data:image/jpeg;base64," + avatarB64);
            if (coverB64  != null) body.put("cover_b64", "data:image/jpeg;base64," + coverB64);
        } catch (Exception ignored) {}

        String url = API_BASE + "/" + userIdPath;
        JsonObjectRequest putReq = new JsonObjectRequest(
                Request.Method.PUT, url, body,
                res -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                err -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Lỗi lưu hồ sơ", Toast.LENGTH_SHORT).show();
                }
        );
        putReq.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(putReq);
    }

    /* =================== Helpers =================== */

    private String getSavedUserId(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences("app_session", Context.MODE_PRIVATE)
                .getString("user_id", null);
    }

    // encode ảnh sau khi chọn thành base64 (scale để tránh file quá lớn)
    private String encodeImageUriToBase64(Uri uri, int maxWidth, int qualityJpeg) {
        try {
            ContentResolver cr = getContentResolver();
            InputStream in = cr.openInputStream(uri);
            if (in == null) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, opts);
            in.close();

            int sample = 1;
            while (opts.outWidth / sample > maxWidth) sample *= 2;

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;

            in = cr.openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            if (in != null) in.close();
            if (bmp == null) return null;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, qualityJpeg, bos);
            byte[] bytes = bos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Toast.makeText(this, "Không đọc được ảnh", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private static boolean isDataUrl(String s) {
        return s != null && s.startsWith("data:image/");
    }
    private static String stripDataUrlPrefix(String s) {
        int idx = s.indexOf(","); // data:image/...;base64,XXXX
        return idx >= 0 ? s.substring(idx + 1) : s;
    }
    private static boolean looksLikeBase64(String s) {
        return s != null && !s.startsWith("http") && s.length() > 200; // heuristic
    }
}
