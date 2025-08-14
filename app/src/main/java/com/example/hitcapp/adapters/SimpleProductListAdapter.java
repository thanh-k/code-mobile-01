package com.example.hitcapp.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SimpleProductListAdapter extends RecyclerView.Adapter<SimpleProductListAdapter.VH> {

    public static class ProductItem {
        public final String id, imageUrl, name, subtitle;
        public final double price;
        public ProductItem(String id, String imageUrl, String name, String subtitle, double price) {
            this.id=id; this.imageUrl=imageUrl; this.name=name; this.subtitle=subtitle; this.price=price;
        }
    }

    public interface OnProductActionListener {
        void onClickDetail(ProductItem item);
        void onClickAdd(ProductItem item);
    }

    private final List<ProductItem> items;
    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));
    private final OnProductActionListener listener;

    public SimpleProductListAdapter(List<ProductItem> items, OnProductActionListener l) {
        this.items = items; this.listener = l;
    }
    public SimpleProductListAdapter(List<ProductItem> items) { this(items, null); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, subtitle, price;
        View btnDetail, btnAdd;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgProduct);
            name = v.findViewById(R.id.tvName);
            subtitle = v.findViewById(R.id.tvSubtitle);
            price = v.findViewById(R.id.tvPrice);

            btnDetail = v.findViewById(R.id.ibDetails);
            btnAdd    = v.findViewById(R.id.ibAddToCart);
            if (btnAdd == null) btnAdd = v.findViewById(R.id.btnAddToCart);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_list, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ProductItem it = items.get(position);
        h.name.setText(it.name);
        h.subtitle.setText(it.subtitle);
        h.price.setText(vn.format(it.price));

        h.img.setImageResource(android.R.drawable.ic_menu_report_image);
        ImageRequest req = new ImageRequest(
                it.imageUrl,
                (Bitmap bmp) -> h.img.setImageBitmap(bmp),
                0, 0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> h.img.setImageResource(android.R.drawable.ic_delete)
        );
        Volley.newRequestQueue(h.img.getContext()).add(req);

        if (listener != null) {
            if (h.btnDetail != null) h.btnDetail.setOnClickListener(v -> listener.onClickDetail(it));
            if (h.btnAdd    != null) h.btnAdd.setOnClickListener(v -> listener.onClickAdd(it));
        } else {
            if (h.btnDetail != null) h.btnDetail.setOnClickListener(null);
            if (h.btnAdd    != null) h.btnAdd.setOnClickListener(null);
        }
    }

    @Override public int getItemCount() { return items.size(); }
}
