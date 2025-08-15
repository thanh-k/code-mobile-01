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

public class CheckoutSummaryAdapter extends RecyclerView.Adapter<CheckoutSummaryAdapter.VH> {

    public static class Item {
        public final String id, name, imageUrl;
        public final double price;
        public final int qty;

        public Item(String id, String name, String imageUrl, double price, int qty) {
            this.id = id; this.name = name; this.imageUrl = imageUrl; this.price = price; this.qty = qty;
        }
    }

    private final List<Item> items;
    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));

    public CheckoutSummaryAdapter(List<Item> items) {
        this.items = items;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName, tvPrice, tvQty;
        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgProduct);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQty = itemView.findViewById(R.id.tvQty);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_checkout_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = items.get(position);
        h.tvName.setText(it.name);
        h.tvPrice.setText(vn.format(it.price));
        h.tvQty.setText("x" + it.qty);

        h.img.setImageResource(R.drawable.ic_image_placeholder);
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

    @Override public int getItemCount() { return items.size(); }
}
