package com.example.hitcapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;
import com.example.hitcapp.adapters.SimpleProductListAdapter;
import com.example.hitcapp.cart.CartRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductsFragment extends Fragment {

    private static final String PRODUCTS_URL   = "https://api.escuelajs.co/api/v1/products";
    private static final String CATEGORIES_URL = "https://api.escuelajs.co/api/v1/categories";

    // Nếu muốn truyền categoryId để lọc, dùng newInstance(categoryId)
    private static final String ARG_CATEGORY_ID = "arg_category_id";
    @Nullable private Integer categoryIdArg = null;

    private final List<SimpleProductListAdapter.ProductItem> data = new ArrayList<>();
    private SimpleProductListAdapter adapter;

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
            if (images != null && images.length() > 0) {
                img = images.optString(0, "");
            }
            if (img == null || img.isEmpty()) {
                img = o.optString("thumbnail", "");
            }
            if (img == null || img.isEmpty()) {
                JSONObject category = o.optJSONObject("category");
                if (category != null) {
                    img = category.optString("image", "");
                }
            }

            // Truyền đủ 5 tham số: id, imageUrl, name, subtitle, price
            data.add(new SimpleProductListAdapter.ProductItem(id, img, name, desc, price));
        }
        adapter.notifyDataSetChanged();
    }
}
