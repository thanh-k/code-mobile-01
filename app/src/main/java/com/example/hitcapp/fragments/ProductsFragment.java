package com.example.hitcapp.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;
import com.example.hitcapp.adapters.SimpleProductListAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductsFragment extends Fragment {

    private static final String API_URL =
            "https://68940ddebe3700414e11df37.mockapi.io/log/product";

    private final List<SimpleProductListAdapter.ProductItem> data = new ArrayList<>();
    private SimpleProductListAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_products_fragment, container, false);

        RecyclerView rv = root.findViewById(R.id.rvProductsList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SimpleProductListAdapter(data);   // bạn đang bỏ qua sự kiện Add-to-cart
        rv.setAdapter(adapter);

        fetchProducts();   // <-- CALL API
        return root;
    }

    private void fetchProducts() {
        RequestQueue q = Volley.newRequestQueue(requireContext());
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, API_URL, null,
                this::bindList, error ->
                Toast.makeText(requireContext(), "Lỗi tải danh sách", Toast.LENGTH_SHORT).show()
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        q.add(req);
    }

    private void bindList(JSONArray arr) {
        data.clear();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            String name = o.optString("Nameproduct", "");
            String desc = o.optString("describe", "");
            String img  = o.optString("image", "");
            String priceStr = o.optString("price", "0");

            double price = parsePrice(priceStr);
            data.add(new SimpleProductListAdapter.ProductItem(img, name, desc, price));
        }
        adapter.notifyDataSetChanged();
    }

    private double parsePrice(String s) {
        try { return Double.parseDouble(s.replaceAll("[^0-9.]", "")); }
        catch (Exception e) { return 0d; }
    }
    private void openDetail(SimpleProductListAdapter.ProductItem it) {
        Fragment f = ProductDetailFragment.newInstance(
                it.id, it.name, it.subtitle, it.imageUrl, it.price
        );
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, f)  // container trong activity_main.xml
                .addToBackStack("product_detail")
                .commit();
    }
}
