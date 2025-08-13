package com.example.hitcapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hitcapp.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SimpleProductAdapter extends RecyclerView.Adapter<SimpleProductAdapter.VH> {

    public static class ProductItem {
        public final String imageUrl;
        public final String name;
        public final double price;
        public String id;

        public ProductItem(String imageUrl, String name, double price) {
            this.imageUrl = imageUrl;
            this.name = name;
            this.price = price;
        }
    }

    private final List<ProductItem> items;
    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));

    public SimpleProductAdapter(List<ProductItem> items) { this.items = items; }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, price;
        Button btnAdd;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgProduct);
            name = v.findViewById(R.id.tvName);
            price = v.findViewById(R.id.tvPrice);
            btnAdd = v.findViewById(R.id.btnAddToCart);
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
        ProductItem it = items.get(position);
        h.name.setText(it.name);
        h.price.setText(vn.format(it.price));
        Glide.with(h.img).load(it.imageUrl).into(h.img);

        // Chưa gắn sự kiện cho btnAdd theo yêu cầu trước
        h.btnAdd.setOnClickListener(null);
    }

    @Override
    public int getItemCount() { return items.size(); }
}
