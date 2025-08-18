package com.example.hitcapp.adapters;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class SimpleProductAdapter extends RecyclerView.Adapter<SimpleProductAdapter.VH> implements Filterable {

    /* ========= MODEL ========= */
    public static class ProductItem {
        public final String id, imageUrl, name;
        public final double price;
        public ProductItem(String id, String imageUrl, String name, double price) {
            this.id = id; this.imageUrl = imageUrl; this.name = name; this.price = price;
        }
    }

    public interface OnProductActionListener {
        void onClickDetail(ProductItem item);
        void onClickAdd(ProductItem item);
    }

    /* ========= STATE ========= */
    private List<ProductItem> items;          // danh sách đang hiển thị
    private final List<ProductItem> allItems; // danh sách gốc để filter
    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));
    private final OnProductActionListener listener;

    public SimpleProductAdapter(List<ProductItem> items, OnProductActionListener l) {
        // sao chép để có thể mutate
        this.items = new ArrayList<>(items);
        this.allItems = new ArrayList<>(items);
        this.listener = l;
    }
    public SimpleProductAdapter(List<ProductItem> items) { this(items, null); }

    /** Cập nhật danh sách sau khi tải API */
    public void submitList(List<ProductItem> data) {
        this.allItems.clear();
        this.allItems.addAll(data);
        this.items = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, price;
        View btnDetail, btnAdd;
        VH(@NonNull View v) {
            super(v);
            img  = v.findViewById(R.id.imgProduct);
            name = v.findViewById(R.id.tvName);
            price= v.findViewById(R.id.tvPrice);

            btnDetail = v.findViewById(R.id.ibDetails);
            btnAdd    = v.findViewById(R.id.ibAddToCart);
            if (btnAdd == null) btnAdd = v.findViewById(R.id.btnAddToCart); // fallback cho layout list
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_grid, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final ProductItem it = items.get(position);

        h.name.setText(it.name);
        h.price.setText(vn.format(it.price));

        // Load ảnh bằng Volley
        if (TextUtils.isEmpty(it.imageUrl)) {
            h.img.setImageResource(R.drawable.ic_image_placeholder);
        } else {
            h.img.setImageResource(android.R.drawable.ic_menu_report_image);
            ImageRequest req = new ImageRequest(
                    it.imageUrl,
                    (Bitmap bmp) -> h.img.setImageBitmap(bmp),
                    0, 0,
                    ImageView.ScaleType.CENTER_CROP,
                    Bitmap.Config.RGB_565,
                    error -> h.img.setImageResource(R.drawable.ic_broken_image)
            );
            Volley.newRequestQueue(h.img.getContext()).add(req);
        }

        if (listener != null) {
            if (h.btnDetail != null) h.btnDetail.setOnClickListener(v -> listener.onClickDetail(it));
            if (h.btnAdd    != null) h.btnAdd.setOnClickListener(v -> listener.onClickAdd(it));
            // click item mở chi tiết (tuỳ thích)
            h.itemView.setOnClickListener(v -> listener.onClickDetail(it));
        } else {
            if (h.btnDetail != null) h.btnDetail.setOnClickListener(null);
            if (h.btnAdd    != null) h.btnAdd.setOnClickListener(null);
            h.itemView.setOnClickListener(null);
        }
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    /* ========= FILTER ========= */
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                String q = constraint == null ? "" : normalize(constraint.toString());
                List<ProductItem> filtered = new ArrayList<>();
                if (q.isEmpty()) {
                    filtered.addAll(allItems);
                } else {
                    for (ProductItem it : allItems) {
                        String hay = normalize(it.name);
                        if (hay.contains(q)) filtered.add(it);
                    }
                }
                FilterResults res = new FilterResults();
                res.values = filtered;
                return res;
            }

            @SuppressWarnings("unchecked")
            @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                items = (List<ProductItem>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    // Chuẩn hoá để tìm kiếm "không dấu", không phân biệt hoa/thường
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replace('đ','d').replace('Đ','D')
                .toLowerCase(Locale.ROOT);
        return DIACRITICS.matcher(n).replaceAll("");
    }
}
