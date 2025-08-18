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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.hitcapp.R;
import com.example.hitcapp.adapters.CheckoutSummaryAdapter;
import com.example.hitcapp.cart.cartitem;
import com.example.hitcapp.cart.CartRepository;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckoutFragment extends Fragment {

    private static final String USERS_URL = "https://68940ddebe3700414e11df37.mockapi.io/log/user";
    private static final String ORDER_URL = "https://68940ddebe3700414e11df37.mockapi.io/log/order";
    private static final String PRODUCT_DETAIL_URL = "https://api.escuelajs.co/api/v1/products/"; // + {id}

    private RecyclerView rv;
    private TextView tvSubtotal, tvShipping, tvDiscount, tvTotal;
    private TextView tvShipNamePhone, tvShipAddress;
    private RadioGroup rgPayment;
    private Button btnPlaceOrder, btnChangeAddress;

    private final NumberFormat vn = NumberFormat.getCurrencyInstance(new Locale("vi","VN"));
    private final List<CheckoutSummaryAdapter.Item> data = new ArrayList<>();
    private CheckoutSummaryAdapter adapter;

    private double subtotal = 0d, shipping = 0d, discount = 0d;

    /* ===== User profile model để gắn vào đơn ===== */
    private static class UserProfile {
        String id;
        String name;
        String phone;
        String address;
        String email; // có thể rỗng
    }

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
                        // 1) Resolve đầy đủ thông tin user
                        resolveUserProfile(profile -> {
                            if (profile == null) {
                                Toast.makeText(requireContext(), "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // 2) Gửi đơn cho từng sản phẩm trong giỏ
                            sendOrdersForCart(profile);
                        });
                    })
                    .setNegativeButton("Huỷ", null)
                    .show();
        });

        return v;
    }

    /* ---------- Shipping info hiển thị ---------- */

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
                    String email   = o.optString("email", "");

                    tvShipNamePhone.setText(formatNamePhone(name, phone));
                    tvShipAddress.setText(address);

                    // cache lại vào session cho lần sau
                    requireContext().getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString("user_name", name)
                            .putString("user_phone", phone)
                            .putString("user_address", address)
                            .putString("user_email", email)
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

    /* ---------- Resolve user profile trước khi gửi đơn ---------- */

    private interface ProfileCallback { void onReady(UserProfile profile); }

    private void resolveUserProfile(ProfileCallback cb) {
        SharedPreferences sp = requireContext()
                .getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE);

        UserProfile p = new UserProfile();
        p.id      = sp.getString("user_id", null);
        p.name    = sp.getString("user_name", null);
        p.phone   = sp.getString("user_phone", null);
        p.address = sp.getString("user_address", null);
        p.email   = sp.getString("user_email", null);

        // Nếu đã có thông tin tối thiểu, dùng luôn
        if ((p.name != null && !p.name.isEmpty())
                || (p.phone != null && !p.phone.isEmpty())
                || (p.address != null && !p.address.isEmpty())) {
            cb.onReady(p);
            return;
        }

        // Nếu có user_id nhưng thiếu thông tin => fetch từ API rồi cache lại
        if (p.id != null && !p.id.isEmpty()) {
            String url = USERS_URL + "/" + p.id;
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.GET, url, null,
                    o -> {
                        p.name    = o.optString("name", "");
                        p.phone   = o.optString("phone", "");
                        p.address = o.optString("address", "");
                        p.email   = o.optString("email", "");

                        // cache
                        sp.edit()
                                .putString("user_name", p.name)
                                .putString("user_phone", p.phone)
                                .putString("user_address", p.address)
                                .putString("user_email", p.email)
                                .apply();

                        cb.onReady(p);
                    },
                    err -> cb.onReady(p) // trả về những gì có (fallback)
            );
            req.setRetryPolicy(new DefaultRetryPolicy(8000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            Volley.newRequestQueue(requireContext()).add(req);
        } else {
            cb.onReady(p);
        }
    }

    /* ---------- Gửi đơn hàng ---------- */

    private void sendOrdersForCart(UserProfile profile) {
        CartRepository repo = CartRepository.getInstance(requireContext());
        List<cartitem> items = repo.getItems();
        if (items == null || items.isEmpty()) {
            Toast.makeText(requireContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlaceOrder.setEnabled(false);

        final int total = items.size();
        final AtomicInteger finished = new AtomicInteger(0);

        for (cartitem it : items) {
            // B1: Lấy chi tiết sản phẩm để có category id + name
            String detailUrl = PRODUCT_DETAIL_URL + it.id;
            JsonObjectRequest getDetail = new JsonObjectRequest(
                    Request.Method.GET, detailUrl, null,
                    o -> {
                        JSONObject cat = o.optJSONObject("category");
                        String idCate = cat != null ? String.valueOf(cat.optInt("id", -1)) : "";
                        String catName = cat != null ? cat.optString("name", "") : "";
                        // B2: Đẩy đơn hàng (1 record) lên MockAPI với kèm thông tin user
                        postOrderLine(it, idCate, catName, profile, () -> afterOnePosted(finished, total));
                    },
                    err -> {
                        // Nếu lỗi lấy category, vẫn post với idcate/categories rỗng
                        postOrderLine(it, "", "", profile, () -> afterOnePosted(finished, total));
                    }
            );
            getDetail.setRetryPolicy(new DefaultRetryPolicy(10_000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            Volley.newRequestQueue(requireContext()).add(getDetail);
        }
    }

    private void afterOnePosted(AtomicInteger finished, int total) {
        if (finished.incrementAndGet() == total) {
            // Tất cả dòng đã post xong
            CartRepository.getInstance(requireContext()).clear();
            btnPlaceOrder.setEnabled(true);
            Toast.makeText(requireContext(), "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();

            // Điều hướng về Cart (tuỳ luồng app)
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new CartFragment())
                    .addToBackStack("cart")
                    .commit();
        }
    }

    private void postOrderLine(cartitem it, String idCate, String categoryName, UserProfile profile, Runnable onDone) {
        final String creationDate = isoNowUtc();

        StringRequest req = new StringRequest(
                Request.Method.POST,
                ORDER_URL,
                resp -> onDone.run(),
                err  -> {
                    // Có thể log/hiện toast nếu muốn theo dõi lỗi từng dòng
                    onDone.run();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                // Thông tin người dùng
                if (profile != null) {
                    if (profile.id != null)      p.put("userid", profile.id);
                    if (profile.name != null)    p.put("username", profile.name);
                    if (profile.phone != null)   p.put("phone", profile.phone);
                    if (profile.address != null) p.put("address", profile.address);
                    if (profile.email != null && !profile.email.isEmpty()) p.put("email", profile.email);
                }

                // Thông tin sản phẩm/đơn
                p.put("idcate",       idCate);                     // id danh mục
                p.put("idpro",        it.id);                      // id sản phẩm
                p.put("product",      it.name);                    // tên sản phẩm
                p.put("categories",   categoryName);               // tên danh mục
                p.put("creationdate", creationDate);               // thời gian tạo đơn

                // Tuỳ chọn (tham khảo mẫu trên MockAPI):
                p.put("quantity",     String.valueOf(it.qty));
                p.put("price",        String.format(Locale.US, "%.2f", it.unitPrice * it.qty));
                return p;
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(10_000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private static String isoNowUtc() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}
