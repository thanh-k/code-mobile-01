package com.example.hitcapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;
import com.example.hitcapp.adapters.SimpleProductAdapter;
import com.example.hitcapp.login;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String API_URL =
            "https://68940ddebe3700414e11df37.mockapi.io/log/product";

    private final List<SimpleProductAdapter.ProductItem> data = new ArrayList<>();
    private SimpleProductAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_home_fragment, container, false);

        // Grid 2 cột
        RecyclerView rv = root.findViewById(R.id.rvProducts);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new SimpleProductAdapter(data); // adapter nhận List<ProductItem>
        rv.setAdapter(adapter);

        // Avatar -> menu đăng xuất
        ImageView avatar = root.findViewById(R.id.imgAvatar);
        if (avatar != null) {
            avatar.setOnClickListener(this::showAvatarMenu);
        }

        fetchProducts();   // CALL API
        return root;
    }

    private void fetchProducts() {
        RequestQueue q = Volley.newRequestQueue(requireContext());

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                API_URL,
                null,
                this::bindHome,
                error -> Toast.makeText(requireContext(), "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show()
        );

        // Tuỳ chọn: timeout / retry
        req.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10s
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        q.add(req);
    }

    private void bindHome(JSONArray arr) {
        data.clear();

        // Lấy 8 sản phẩm đầu (tuỳ bạn)
        int n = Math.min(arr.length(), 8);
        for (int i = 0; i < n; i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            String name  = o.optString("Nameproduct", "");
            String img   = o.optString("image", "");
            String price = o.optString("price", "0");

            double p = parsePrice(price);
            data.add(new SimpleProductAdapter.ProductItem(img, name, p));
        }
        adapter.notifyDataSetChanged();
    }

    private double parsePrice(String s) {
        try {
            return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0d;
        }
    }

    private void showAvatarMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, "Đăng xuất");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                Intent i = new Intent(requireContext(), login.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                return true;
            }
            return false;
        });
        menu.show();
    }
    private void openDetailFromHome(SimpleProductAdapter.ProductItem it) {
        Fragment f = ProductDetailFragment.newInstance(
                it.id, it.name, /*desc*/ null, it.imageUrl, it.price
        );
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, f)
                .addToBackStack("product_detail")
                .commit();
    }
}
