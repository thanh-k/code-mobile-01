package com.example.hitcapp.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductDetailFragment extends Fragment {

    private static final String ARG_ID    = "arg_id";
    private static final String ARG_NAME  = "arg_name";
    private static final String ARG_DESC  = "arg_desc";
    private static final String ARG_IMAGE = "arg_image";
    private static final String ARG_PRICE = "arg_price";

    private static final String API_PRODUCTS   = "https://api.escuelajs.co/api/v1/products";
    private static final String API_CATEGORIES = "https://api.escuelajs.co/api/v1/categories";

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
    private TextView tvName, tvPrice, tvDesc, tvRelatedTitle;
    private View btnAdd;
    private ProgressBar progress;

    // Related
    private RecyclerView rvRelated;
    private final List<RelatedItem> related = new ArrayList<>();
    private RelatedAdapter relatedAdapter;

    private String currentId;
    private Integer currentCategoryId;

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
        tvRelatedTitle = v.findViewById(R.id.tvRelatedTitle);
        btnAdd = v.findViewById(R.id.btnAddToCart);
        progress = v.findViewById(R.id.progress);

        // Related: horizontal list
        rvRelated = v.findViewById(R.id.rvRelated);
        rvRelated.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        relatedAdapter = new RelatedAdapter(related, item -> {
            // mở chi tiết của item liên quan
            Fragment f2 = ProductDetailFragment.newInstance(
                    item.id, item.name, null, item.imageUrl, item.price
            );
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, f2)
                    .addToBackStack("product_detail")
                    .commit();
        });
        rvRelated.setAdapter(relatedAdapter);

        // Hiển thị tạm từ args (để UI không trống khi đang load)
        Bundle args = getArguments();
        currentId = args != null ? args.getString(ARG_ID) : null;
        String name  = args != null ? args.getString(ARG_NAME) : null;
        String desc  = args != null ? args.getString(ARG_DESC) : null;
        String image = args != null ? args.getString(ARG_IMAGE) : null;
        double price = args != null ? args.getDouble(ARG_PRICE, 0d) : 0d;

        if (name  != null) tvName.setText(name);
        if (desc  != null) tvDesc.setText(desc);
        if (price > 0)     tvPrice.setText(vn.format(price));
        if (image != null) loadImage(image);

        // Gọi API chi tiết để lấy đầy đủ + categoryId
        if (currentId != null && !currentId.isEmpty()) fetchDetailById(currentId);

        btnAdd.setOnClickListener(_v ->
                Toast.makeText(requireContext(), "Thêm vào giỏ (demo)", Toast.LENGTH_SHORT).show()
        );

        return v;
    }

    /* ---------------- Detail ---------------- */

    private void fetchDetailById(String id) {
        setLoading(true);
        String url = API_PRODUCTS + "/" + id;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                this::bindDetail,
                err -> { setLoading(false); Toast.makeText(requireContext(), "Lỗi tải chi tiết", Toast.LENGTH_SHORT).show(); }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void bindDetail(JSONObject o) {
        String name  = o.optString("title", "");
        String desc  = o.optString("description", "");
        double price = o.optDouble("price", 0);

        String image = null;
        JSONArray images = o.optJSONArray("images");
        if (images != null && images.length() > 0) image = images.optString(0, "");

        // category id
        JSONObject cat = o.optJSONObject("category");
        currentCategoryId = (cat != null) ? Integer.valueOf(cat.optInt("id", 0)) : null;

        tvName.setText(name);
        tvDesc.setText(desc);
        tvPrice.setText(vn.format(price));
        if (image != null) loadImage(image);

        setLoading(false);

        // gọi gợi ý theo danh mục (nếu có category)
        if (currentCategoryId != null && currentCategoryId > 0) {
            fetchRelated(currentCategoryId);
        } else {
            showRelated(false);
        }
    }

    /* ---------------- Related by category ---------------- */

    private void fetchRelated(int categoryId) {
        String url = API_CATEGORIES + "/" + categoryId + "/products?limit=20&offset=0";
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                this::bindRelated,
                err -> showRelated(false)
        );
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void bindRelated(JSONArray arr) {
        related.clear();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            String id = String.valueOf(o.optInt("id", 0));
            if (id.equals(currentId)) continue; // bỏ sản phẩm hiện tại

            String title = o.optString("title", "");
            double price = o.optDouble("price", 0);
            String img = null;
            JSONArray images = o.optJSONArray("images");
            if (images != null && images.length() > 0) img = images.optString(0, "");

            related.add(new RelatedItem(id, img, title, price));
        }
        relatedAdapter.notifyDataSetChanged();
        showRelated(!related.isEmpty());
    }

    private void showRelated(boolean show) {
        if (tvRelatedTitle != null) tvRelatedTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvRelated != null) rvRelated.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /* ---------------- Utils ---------------- */

    private void loadImage(String url) {
        if (url == null || url.isEmpty()) return;
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageResource(R.drawable.ic_image_placeholder);
        ImageRequest imgReq = new ImageRequest(
                url,
                (Bitmap bmp) -> img.setImageBitmap(bmp),
                0, 0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> img.setImageResource(R.drawable.ic_broken_image)
        );
        Volley.newRequestQueue(requireContext()).add(imgReq);
    }

    private void setLoading(boolean b) {
        if (progress != null) progress.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    /* ================= Adapter gợi ý (nội bộ) ================= */

    private static class RelatedItem {
        final String id, imageUrl, name;
        final double price;
        RelatedItem(String id, String imageUrl, String name, double price) {
            this.id = id; this.imageUrl = imageUrl; this.name = name; this.price = price;
        }
    }

    private static class RelatedAdapter extends RecyclerView.Adapter<RelatedAdapter.RelatedVH> {

        interface OnClick { void onItem(RelatedItem it); }

        private final List<RelatedItem> items;
        private final OnClick onClick;

        RelatedAdapter(List<RelatedItem> items, OnClick onClick) {
            this.items = items; this.onClick = onClick;
        }

        static class RelatedVH extends RecyclerView.ViewHolder {
            ImageView img;
            TextView name, price;
            RelatedVH(@NonNull View itemView) {
                super(itemView);
                img = itemView.findViewById(1);
                name = itemView.findViewById(2);
                price = itemView.findViewById(3);
            }
        }

        @NonNull @Override
        public RelatedVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Tạo view nhỏ gọn bằng code (không cần file XML)
            android.content.Context ctx = parent.getContext();
            int pad = dp(ctx, 8);

            LinearLayout root = new LinearLayout(ctx);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(pad, pad, pad, pad);
            RecyclerView.LayoutParams rlp = new RecyclerView.LayoutParams(dp(ctx, 140), ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(dp(ctx, 4), dp(ctx, 4), dp(ctx, 4), dp(ctx, 4));
            root.setLayoutParams(rlp);

            ImageView iv = new ImageView(ctx);
            iv.setId(1);
            iv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 110)));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            root.addView(iv);

            TextView tvName = new TextView(ctx);
            tvName.setId(2);
            tvName.setTextSize(13f);
            tvName.setMaxLines(2);
            tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvName.setPadding(0, dp(ctx, 6), 0, 0);
            root.addView(tvName);

            TextView tvPrice = new TextView(ctx);
            tvPrice.setId(3);
            tvPrice.setTextSize(13f);
            tvPrice.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvPrice.setPadding(0, dp(ctx, 4), 0, 0);
            root.addView(tvPrice);

            return new RelatedVH(root);
        }

        @Override
        public void onBindViewHolder(@NonNull RelatedVH h, int position) {
            RelatedItem it = items.get(position);
            h.name.setText(it.name);
            h.price.setText(NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(it.price));

            h.img.setImageResource(R.drawable.ic_image_placeholder);
            ImageRequest req = new ImageRequest(
                    it.imageUrl,
                    h.img::setImageBitmap,
                    0, 0,
                    ImageView.ScaleType.CENTER_CROP,
                    Bitmap.Config.RGB_565,
                    error -> h.img.setImageResource(R.drawable.ic_broken_image)
            );
            Volley.newRequestQueue(h.img.getContext()).add(req);

            h.itemView.setOnClickListener(v -> { if (onClick != null) onClick.onItem(it); });
        }

        @Override public int getItemCount() { return items.size(); }

        private static int dp(android.content.Context ctx, int v) {
            return Math.round(v * ctx.getResources().getDisplayMetrics().density);
        }
    }
}
