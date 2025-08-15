package com.example.hitcapp.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;
import com.example.hitcapp.adapters.CheckoutSummaryAdapter;
import com.example.hitcapp.cart.cartitem;
import com.example.hitcapp.cart.CartRepository;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutFragment extends Fragment {

    private static final String USERS_URL = "https://68940ddebe3700414e11df37.mockapi.io/log/user";

    private RecyclerView rv;
    private TextView tvSubtotal, tvShipping, tvDiscount, tvTotal;
    private TextView tvShipNamePhone, tvShipAddress;
    private RadioGroup rgPayment;
    private Button btnPlaceOrder, btnChangeAddress;

    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));
    private final List<CheckoutSummaryAdapter.Item> data = new ArrayList<>();
    private CheckoutSummaryAdapter adapter;

    private double subtotal = 0d, shipping = 0d, discount = 0d;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_checkout_fragment, container, false);

        rv = v.findViewById(R.id.rvCheckoutItems);
        tvSubtotal = v.findViewById(R.id.tvSubtotal);
        tvShipping = v.findViewById(R.id.tvShipping);
        tvDiscount = v.findViewById(R.id.tvDiscount);
        tvTotal    = v.findViewById(R.id.tvTotal);
        tvShipNamePhone = v.findViewById(R.id.tvShipNamePhone);
        tvShipAddress   = v.findViewById(R.id.tvShipAddress);
        rgPayment = v.findViewById(R.id.rgPayment);
        btnPlaceOrder = v.findViewById(R.id.btnPlaceOrder);
        btnChangeAddress = v.findViewById(R.id.btnChangeAddress);

        // Địa chỉ: lấy từ session (fallback gọi API nếu thiếu)
        bindShippingFromSessionOrApi();

        // List sản phẩm
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CheckoutSummaryAdapter(data);
        rv.setAdapter(adapter);
        loadFromCart();

        btnChangeAddress.setOnClickListener(_v ->
                Toast.makeText(requireContext(), "Mở màn chỉnh sửa địa chỉ (chưa làm)", Toast.LENGTH_SHORT).show()
        );

        btnPlaceOrder.setOnClickListener(_v -> {
            if (data.isEmpty()) {
                Toast.makeText(requireContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                return;
            }
            String payment = (rgPayment.getCheckedRadioButtonId() == R.id.rbCOD) ? "COD" : "Khác";

            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận đặt hàng")
                    .setMessage("Phương thức: " + payment + "\nTổng: " + tvTotal.getText())
                    .setPositiveButton("Đồng ý", (d, w) -> {
                        CartRepository.getInstance(requireContext()).clear();
                        Toast.makeText(requireContext(), "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();

                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_layout, new CartFragment())
                                .addToBackStack("cart")
                                .commit();
                    })
                    .setNegativeButton("Huỷ", null)
                    .show();
        });

        return v;
    }

    /* ---------- Shipping info ---------- */

    private void bindShippingFromSessionOrApi() {
        SharedPreferences sp = requireContext()
                .getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE);

        String name    = sp.getString("user_name", null);
        String phone   = sp.getString("user_phone", null);
        String address = sp.getString("user_address", null);
        String userId  = sp.getString("user_id", null);

        boolean hasLocal = (name != null || phone != null || address != null);
        if (hasLocal) {
            tvShipNamePhone.setText(formatNamePhone(name, phone));
            tvShipAddress.setText(address != null ? address : "");
        } else if (userId != null && !userId.isEmpty()) {
            fetchUserByIdAndBind(userId);
        } else {
            // fallback mặc định
            tvShipNamePhone.setText("Khách hàng · 090xxxxxxx");
            tvShipAddress.setText("");
        }
    }

    private void fetchUserByIdAndBind(String id) {
        String url = USERS_URL + "/" + id;
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                o -> {
                    String name    = o.optString("name", "");
                    String phone   = o.optString("phone", "");
                    String address = o.optString("address", "");

                    tvShipNamePhone.setText(formatNamePhone(name, phone));
                    tvShipAddress.setText(address);

                    // cache lại vào session cho lần sau
                    requireContext().getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString("user_name", name)
                            .putString("user_phone", phone)
                            .putString("user_address", address)
                            .apply();
                },
                err -> {
                    tvShipNamePhone.setText("Khách hàng · 090xxxxxxx");
                    tvShipAddress.setText("");
                }
        );
        req.setRetryPolicy(new DefaultRetryPolicy(8000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private String formatNamePhone(String name, String phone) {
        String n = (name == null || name.isEmpty()) ? "Khách hàng" : name;
        String p = (phone == null || phone.isEmpty()) ? "—" : phone;
        return n + " · " + p;
    }

    /* ---------- Cart ---------- */

    private void loadFromCart() {
        data.clear();
        subtotal = 0d;

        CartRepository repo = CartRepository.getInstance(requireContext());
        List<cartitem> items = repo.getItems();

        for (cartitem it : items) {
            data.add(new CheckoutSummaryAdapter.Item(
                    it.id, it.name, it.imageUrl, it.unitPrice, it.qty
            ));
            subtotal += it.unitPrice * it.qty;
        }
        adapter.notifyDataSetChanged();

        shipping = 0d;
        discount = 0d;

        tvSubtotal.setText(vn.format(subtotal));
        tvShipping.setText(vn.format(shipping));
        tvDiscount.setText(vn.format(discount));
        tvTotal.setText(vn.format(subtotal + shipping - discount));
    }
}
