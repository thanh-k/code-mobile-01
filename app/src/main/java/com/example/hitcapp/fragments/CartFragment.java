package com.example.hitcapp.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hitcapp.R;
import com.example.hitcapp.adapters.CartAdapter;
import com.example.hitcapp.cart.CartItem;
import com.example.hitcapp.cart.CartRepository;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartFragment extends Fragment {

    private final List<CartItem> data = new ArrayList<>();
    private CartAdapter adapter;
    private TextView tvTotal;
    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_cart_fragment, container, false);

        RecyclerView rv = v.findViewById(R.id.rvCart);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CartAdapter(data, new CartAdapter.OnCartAction() {
            @Override public void onInc(CartItem it) {
                CartRepository.getInstance(requireContext()).updateQty(it.id, it.qty + 1);
                refresh();
            }
            @Override public void onDec(CartItem it) {
                CartRepository.getInstance(requireContext()).updateQty(it.id, it.qty - 1);
                refresh();
            }
            @Override public void onRemove(CartItem it) {
                CartRepository.getInstance(requireContext()).remove(it.id);
                refresh();
            }
        });
        rv.setAdapter(adapter);

        tvTotal = v.findViewById(R.id.tvTotal);
        v.findViewById(R.id.btnCheckout).setOnClickListener(_x -> {
            if (data.isEmpty()) {
                Toast.makeText(requireContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: chuyển sang trang thanh toán
                Toast.makeText(requireContext(), "Thanh toán (chưa triển khai)", Toast.LENGTH_SHORT).show();
            }
        });

        refresh();
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        data.clear();
        data.addAll(CartRepository.getInstance(requireContext()).getItems());
        adapter.notifyDataSetChanged();

        double total = CartRepository.getInstance(requireContext()).getSubtotal();
        tvTotal.setText("Tổng: " + vn.format(total));
    }
}
