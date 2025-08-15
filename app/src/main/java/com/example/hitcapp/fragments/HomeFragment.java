package com.example.hitcapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;
import com.example.hitcapp.adapters.SimpleProductAdapter;
import com.example.hitcapp.cart.CartRepository;
import com.example.hitcapp.login;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    /* ========= API ========= */
    private static final String PRODUCTS_URL   = "https://api.escuelajs.co/api/v1/products";
    private static final String CATEGORIES_URL = "https://api.escuelajs.co/api/v1/categories";
    private static final String USERS_URL      = "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    /* ========= UI / DATA ========= */
    private final List<SimpleProductAdapter.ProductItem> data = new ArrayList<>();
    private SimpleProductAdapter adapter;

    private ImageView ivAvatar;
    private LinearLayout catContainer; // id: linearCategories

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_home_fragment, container, false);

        // RecyclerView: grid 2 cột
        RecyclerView rv = root.findViewById(R.id.rvProducts);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // Adapter + listener (chi tiết / thêm giỏ)
        adapter = new SimpleProductAdapter(
                data,
                new SimpleProductAdapter.OnProductActionListener() {
                    @Override
                    public void onClickDetail(SimpleProductAdapter.ProductItem it) {
                        Fragment f = ProductDetailFragment.newInstance(
                                it.id, it.name, /*desc*/ null, it.imageUrl, it.price
                        );
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_layout, f)  // container trong activity_main.xml
                                .addToBackStack("product_detail")
                                .commit();
                    }

                    @Override
                    public void onClickAdd(SimpleProductAdapter.ProductItem it) {
                        CartRepository.getInstance(requireContext())
                                .addOrInc(it.id, it.name, it.imageUrl, it.price);
                        Toast.makeText(requireContext(), "Đã thêm: " + it.name, Toast.LENGTH_SHORT).show();
                    }
                }
        );
        rv.setAdapter(adapter);

        // Avatar + menu đăng xuất
        ivAvatar = root.findViewById(R.id.imgAvatar);
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(this::showAvatarMenu);
            loadAvatarBySavedId();
        }

        // Categories
        catContainer = root.findViewById(R.id.linearCategories);
        fetchCategories();
        fetchProducts(null); // tất cả sản phẩm

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ivAvatar != null) loadAvatarBySavedId();
    }

    /* =================== PRODUCTS =================== */

    /** Nếu categoryId == null → lấy tất cả; ngược lại /categories/{id}/products */
    private void fetchProducts(@Nullable Integer categoryId) {
        String url = (categoryId == null)
                ? PRODUCTS_URL
                : (CATEGORIES_URL + "/" + categoryId + "/products");

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                this::bindProducts,
                err -> Toast.makeText(requireContext(), "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show()
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void bindProducts(JSONArray arr) {
        data.clear();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            String idStr = String.valueOf(o.optInt("id", 0));
            String title = o.optString("title", "");
            double price = o.optDouble("price", 0);
            String img   = null;

            JSONArray images = o.optJSONArray("images");
            if (images != null && images.length() > 0) img = images.optString(0, null);
            if (img == null) img = o.optString("thumbnail", "");

            data.add(new SimpleProductAdapter.ProductItem(idStr, img, title, price));
        }
        adapter.notifyDataSetChanged();
    }

    /* =================== CATEGORIES =================== */

    private void fetchCategories() {
        if (catContainer == null) return;

        catContainer.removeAllViews();
        // nút Tất cả
        addCategoryView(-1, "Tất cả", "https://i.imgur.com/QkIa5tT.jpeg");

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, CATEGORIES_URL, null,
                arr -> {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        int id = o.optInt("id", -1);
                        String name = o.optString("name", "");
                        String img  = o.optString("image", "");
                        addCategoryView(id, name, img);
                    }
                },
                err -> Toast.makeText(requireContext(), "Lỗi tải danh mục", Toast.LENGTH_SHORT).show()
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    /** Tạo view danh mục (ảnh tròn + tên) + click lọc */
    private void addCategoryView(final int id, String name, String imgUrl) {
        if (catContainer == null) return;
        Context ctx = requireContext();

        LinearLayout item = new LinearLayout(ctx);
        item.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(dp(8), dp(4), dp(8), dp(4));
        item.setLayoutParams(lp);
        item.setGravity(android.view.Gravity.CENTER);

        ImageView icon = new ImageView(ctx);
        LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(dp(60), dp(60));
        icon.setLayoutParams(lpImg);
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setBackgroundResource(R.drawable.btn_su); // bg bo góc bạn đang có
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        loadImageUrl(icon, imgUrl);

        TextView tv = new TextView(ctx);
        tv.setText(name);
        tv.setTextSize(14f);
        tv.setPadding(0, dp(4), 0, 0);

        item.setOnClickListener(v -> {
            if (id == -1) fetchProducts(null);
            else fetchProducts(id);
        });

        item.addView(icon);
        item.addView(tv);
        catContainer.addView(item);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void loadImageUrl(ImageView iv, String url) {
        if (url == null || url.isEmpty()) return;
        ImageRequest imgReq = new ImageRequest(
                url,
                iv::setImageBitmap,
                0, 0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> { /* giữ placeholder */ }
        );
        Volley.newRequestQueue(requireContext()).add(imgReq);
    }

    /* =================== AVATAR (user_id đã lưu) =================== */

    private void loadAvatarBySavedId() {
        String id = getSavedUserId(requireContext());
        if (id == null || id.isEmpty()) {
            ivAvatar.setImageResource(R.drawable.avatar);
            return;
        }
        String url = USERS_URL + "/" + id;
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                this::bindAvatar,
                err -> ivAvatar.setImageResource(R.drawable.avatar)
        );
        req.setRetryPolicy(new DefaultRetryPolicy(8000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void bindAvatar(JSONObject o) {
        if (ivAvatar == null) return;
        String src = o.optString("img_b64", o.optString("img", ""));
        if (src == null || src.isEmpty()) {
            ivAvatar.setImageResource(R.drawable.avatar);
            return;
        }

        if (isDataUrl(src) || looksLikeBase64(src)) {
            String pure = stripDataUrlPrefix(src);
            try {
                byte[] bytes = Base64.decode(pure, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    ivAvatar.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
            ivAvatar.setImageResource(R.drawable.avatar);
        } else {
            ivAvatar.setImageResource(R.drawable.avatar); // placeholder
            ImageRequest imgReq = new ImageRequest(
                    src,
                    ivAvatar::setImageBitmap,
                    0, 0,
                    ImageView.ScaleType.CENTER_CROP,
                    Bitmap.Config.RGB_565,
                    error -> ivAvatar.setImageResource(R.drawable.avatar)
            );
            Volley.newRequestQueue(requireContext()).add(imgReq);
        }
    }

    private static boolean isDataUrl(String s) { return s != null && s.startsWith("data:image/"); }
    private static String stripDataUrlPrefix(String s) {
        int idx = s.indexOf(',');
        return idx >= 0 ? s.substring(idx + 1) : s;
    }
    private static boolean looksLikeBase64(String s) {
        return s != null && !s.startsWith("http") && s.length() > 200;
    }

    private String getSavedUserId(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences("app_session", Context.MODE_PRIVATE)
                .getString("user_id", null);
    }

    /* =================== MENU AVATAR =================== */

    private void showAvatarMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, "Đăng xuất");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                // clear session
                requireContext().getSharedPreferences("app_session", Context.MODE_PRIVATE)
                        .edit().clear().apply();
                // về login
                Intent i = new Intent(requireContext(), login.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                return true;
            }
            return false;
        });
        menu.show();
    }
}
