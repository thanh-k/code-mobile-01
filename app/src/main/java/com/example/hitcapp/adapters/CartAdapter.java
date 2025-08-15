package com.example.hitcapp.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;
import com.example.hitcapp.cart.cartitem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface OnCartAction {
        void onInc(cartitem it);
        void onDec(cartitem it);
        void onRemove(cartitem it);
    }

    private final List<cartitem> items;
    private final OnCartAction listener;
    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));

    public CartAdapter(List<cartitem> items, OnCartAction l) {
        this.items = items;
        this.listener = l;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, unitPrice, lineTotal, qty;
        ImageButton btnPlus, btnMinus, btnRemove;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgCart);
            name = v.findViewById(R.id.tvCartName);
            unitPrice = v.findViewById(R.id.tvUnitPrice);
            lineTotal = v.findViewById(R.id.tvLineTotal);
            qty = v.findViewById(R.id.tvQty);
            btnPlus = v.findViewById(R.id.btnPlus);
            btnMinus = v.findViewById(R.id.btnMinus);
            btnRemove = v.findViewById(R.id.btnRemove);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        cartitem it = items.get(position);

        h.name.setText(it.name);
        h.unitPrice.setText(vn.format(it.unitPrice));
        h.lineTotal.setText(vn.format(it.getLineTotal()));
        h.qty.setText(String.valueOf(it.qty));

        h.img.setImageResource(android.R.drawable.ic_menu_report_image);
        if (it.imageUrl != null && it.imageUrl.startsWith("http")) {
            ImageRequest req = new ImageRequest(
                    it.imageUrl,
                    (Bitmap bmp) -> h.img.setImageBitmap(bmp),
                    0, 0,
                    ImageView.ScaleType.CENTER_CROP,
                    Bitmap.Config.RGB_565,
                    error -> h.img.setImageResource(android.R.drawable.ic_delete)
            );
            Volley.newRequestQueue(h.img.getContext()).add(req);
        }

        h.btnPlus.setOnClickListener(v -> { if (listener != null) listener.onInc(it); });
        h.btnMinus.setOnClickListener(v -> { if (listener != null) listener.onDec(it); });
        h.btnRemove.setOnClickListener(v -> { if (listener != null) listener.onRemove(it); });
    }

    @Override public int getItemCount() { return items.size(); }
}
