package com.example.hitcapp.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;
import com.example.hitcapp.adapters.SimpleProductListAdapter;
import com.example.hitcapp.cart.CartRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;            // <-- QUAN TRỌNG: java.text
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;        // <-- dùng cho bỏ dấu

public class ProductsFragment extends Fragment {

    /* ========= API ========= */
    private static final String PRODUCTS_URL   = "https://api.escuelajs.co/api/v1/products";
    private static final String CATEGORIES_URL = "https://api.escuelajs.co/api/v1/categories";
    private static final String USERS_URL      = "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    // Nếu muốn truyền categoryId để lọc, dùng newInstance(categoryId)
    private static final String ARG_CATEGORY_ID = "arg_category_id";
    @Nullable private Integer categoryIdArg = null;

    /* ========= UI ========= */
    private ImageView ivAvatar;
    private EditText etSearch;

    /* ========= DATA ========= */
    // allData: dữ liệu gốc dùng để filter; data: dữ liệu đang hiển thị (gán cho adapter)
    private final List<SimpleProductListAdapter.ProductItem> allData = new ArrayList<>();
    private final List<SimpleProductListAdapter.ProductItem> data    = new ArrayList<>();
    private SimpleProductListAdapter adapter;
    private String currentQuery = "";

    public static ProductsFragment newInstance(@Nullable Integer categoryId) {
        ProductsFragment f = new ProductsFragment();
        if (categoryId != null) {
            Bundle b = new Bundle();
            b.putInt(ARG_CATEGORY_ID, categoryId);
            f.setArguments(b);
        }
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_products_fragment, container, false);

        if (getArguments() != null && getArguments().containsKey(ARG_CATEGORY_ID)) {
            categoryIdArg = getArguments().getInt(ARG_CATEGORY_ID);
        }

        // Avatar
        ivAvatar = root.findViewById(R.id.imgAvatar);
        if (ivAvatar != null) {
            loadAvatarBySavedId();
        }

        // Search
        etSearch = root.findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = (s == null) ? "" : s.toString();
                    applyFilter(currentQuery);
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }

        // RecyclerView
        RecyclerView rv = root.findViewById(R.id.rvProductsList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Adapter: mở chi tiết + thêm vào giỏ
        adapter = new SimpleProductListAdapter(
                data,
                new SimpleProductListAdapter.OnProductActionListener() {
                    @Override
                    public void onClickDetail(SimpleProductListAdapter.ProductItem it) {
                        Fragment f = ProductDetailFragment.newInstance(
                                it.id, it.name, it.subtitle, it.imageUrl, it.price
                        );
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_layout, f)
                                .addToBackStack("product_detail")
                                .commit();
                    }

                    @Override
                    public void onClickAdd(SimpleProductListAdapter.ProductItem it) {
                        CartRepository.getInstance(requireContext())
                                .addOrInc(it.id, it.name, it.imageUrl, it.price);
                        Toast.makeText(requireContext(),
                                "Đã thêm: " + it.name,
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
        rv.setAdapter(adapter);

        fetchProducts();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ivAvatar != null) loadAvatarBySavedId();
    }

    /* =================== PRODUCTS =================== */

    /** Nếu categoryId == null → lấy tất cả; ngược lại /categories/{id}/products */
    private void fetchProducts() {
        final String url = (categoryIdArg == null)
                ? PRODUCTS_URL
                : (CATEGORIES_URL + "/" + categoryIdArg + "/products");

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                this::bindList,
                error -> Toast.makeText(requireContext(), "Lỗi tải danh sách", Toast.LENGTH_SHORT).show()
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void bindList(JSONArray arr) {
        allData.clear();
        data.clear();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            String id    = String.valueOf(o.optInt("id", 0));
            String name  = o.optString("title", "");
            String desc  = o.optString("description", "");
            double price = o.optDouble("price", 0);

            // Ảnh: ưu tiên images[0], fallback thumbnail, rồi category.image
            String img = null;
            JSONArray images = o.optJSONArray("images");
            if (images != null && images.length() > 0) img = images.optString(0, "");
            if (img == null || img.isEmpty()) img = o.optString("thumbnail", "");
            if (img == null || img.isEmpty()) {
                JSONObject category = o.optJSONObject("category");
                if (category != null) img = category.optString("image", "");
            }

            allData.add(new SimpleProductListAdapter.ProductItem(id, img, name, desc, price));
        }

        // Áp dụng filter hiện tại (nếu có)
        applyFilter(currentQuery);
    }

    /* =================== SEARCH (fragment-level) =================== */

    private void applyFilter(String q) {
        String key = normalize(q);
        data.clear();

        if (key.isEmpty()) {
            data.addAll(allData);
        } else {
            for (SimpleProductListAdapter.ProductItem it : allData) {
                String hay = normalize(it.name + " " + (it.subtitle == null ? "" : it.subtitle));
                if (hay.contains(key)) data.add(it);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Chuẩn hoá để tìm kiếm không dấu, không phân biệt hoa/thường
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replace('đ','d').replace('Đ','D')
                .toLowerCase(Locale.ROOT);
        return DIACRITICS.matcher(n).replaceAll("");
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
            } catch (Exception ignored) { }
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
}
