package com.example.hitcapp.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

public class ProductDetailFragment extends Fragment {

    private static final String ARG_ID    = "arg_id";
    private static final String ARG_NAME  = "arg_name";
    private static final String ARG_DESC  = "arg_desc";
    private static final String ARG_IMAGE = "arg_image";
    private static final String ARG_PRICE = "arg_price";

    private static final String API_BASE =
            "https://68940ddebe3700414e11df37.mockapi.io/log/product";

    public static ProductDetailFragment newInstance(String id,
                                                    @Nullable String name,
                                                    @Nullable String desc,
                                                    @Nullable String image,
                                                    double price) {
        ProductDetailFragment f = new ProductDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ID, id);
        if (name  != null) b.putString(ARG_NAME,  name);
        if (desc  != null) b.putString(ARG_DESC,  desc);
        if (image != null) b.putString(ARG_IMAGE, image);
        b.putDouble(ARG_PRICE, price);
        f.setArguments(b);
        return f;
    }

    private ImageView img;
    private TextView tvName, tvPrice, tvDesc;
    private Button btnAdd;
    private ProgressBar progress;
    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.activity_product_detail_fragment, container, false);

        img = v.findViewById(R.id.imgProduct);
        tvName = v.findViewById(R.id.tvName);
        tvPrice = v.findViewById(R.id.tvPrice);
        tvDesc = v.findViewById(R.id.tvDescription);
        btnAdd = v.findViewById(R.id.btnAddToCart);
        progress = v.findViewById(R.id.progress);

        // Hiển thị dữ liệu tạm từ args (nếu có) để UI không trống
        Bundle args = getArguments();
        String id    = args != null ? args.getString(ARG_ID) : null;
        String name  = args != null ? args.getString(ARG_NAME) : null;
        String desc  = args != null ? args.getString(ARG_DESC) : null;
        String image = args != null ? args.getString(ARG_IMAGE) : null;
        double price = args != null ? args.getDouble(ARG_PRICE, 0d) : 0d;

        if (name  != null) tvName.setText(name);
        if (desc  != null) tvDesc.setText(desc);
        if (price > 0)     tvPrice.setText(vn.format(price));
        if (image != null) loadImage(image);

        if (id != null && !id.isEmpty()) fetchDetailById(id);

        btnAdd.setOnClickListener(_v ->
                Toast.makeText(requireContext(), "Thêm vào giỏ (chưa triển khai)", Toast.LENGTH_SHORT).show()
        );

        return v;
    }

    private void fetchDetailById(String id) {
        setLoading(true);
        String url = API_BASE + "/" + id;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                this::bindDetail,
                err -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Lỗi tải chi tiết", Toast.LENGTH_SHORT).show();
                }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void bindDetail(JSONObject o) {
        String name  = o.optString("Nameproduct", "");
        String desc  = o.optString("describe", "");
        String image = o.optString("image", "");
        String priceStr = o.optString("price", "0");

        tvName.setText(name);
        tvDesc.setText(desc);
        tvPrice.setText(vn.format(parsePrice(priceStr)));
        loadImage(image);

        setLoading(false);
    }

    private void loadImage(String url) {
        if (url == null || url.isEmpty()) return;

        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        // Dùng icon hệ thống để khỏi cần drawable tự tạo
        img.setImageResource(android.R.drawable.ic_menu_report_image);

        ImageRequest imgReq = new ImageRequest(
                url,
                (Bitmap bmp) -> img.setImageBitmap(bmp),
                0, 0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> img.setImageResource(android.R.drawable.ic_delete)
        );
        Volley.newRequestQueue(requireContext()).add(imgReq);
    }

    private double parsePrice(String s) {
        try { return Double.parseDouble(s.replaceAll("[^0-9.]", "")); }
        catch (Exception e) { return 0d; }
    }

    private void setLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
