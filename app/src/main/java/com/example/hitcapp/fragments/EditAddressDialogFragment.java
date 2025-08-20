package com.example.hitcapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.example.hitcapp.R;
import com.example.hitcapp.Address;   // import Address trong cùng project

public class EditAddressDialogFragment extends DialogFragment {

    public interface OnAddressSavedListener {
        void onAddressSaved(Address address);
    }

    private static final String ARG_ADDRESS = "arg_address";
    private OnAddressSavedListener listener;

    public static EditAddressDialogFragment newInstance(Address current) {
        EditAddressDialogFragment f = new EditAddressDialogFragment();
        Bundle b = new Bundle();
        b.putParcelable(ARG_ADDRESS, current);
        f.setArguments(b);
        return f;
    }

    public void setOnAddressSavedListener(OnAddressSavedListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_edit_address, container, false);

        TextInputEditText etName = v.findViewById(R.id.etName);
        TextInputEditText etPhone = v.findViewById(R.id.etPhone);
        TextInputEditText etStreet = v.findViewById(R.id.etStreet);
        TextInputEditText etWard = v.findViewById(R.id.etWard);
        TextInputEditText etDistrict = v.findViewById(R.id.etDistrict);
        TextInputEditText etCity = v.findViewById(R.id.etCity);

        Button btnCancel = v.findViewById(R.id.btnCancel);
        Button btnSave   = v.findViewById(R.id.btnSave);

        Address cur = getArguments() != null ? getArguments().getParcelable(ARG_ADDRESS) : null;
        if (cur != null) {
            etName.setText(cur.getName());
            etPhone.setText(cur.getPhone());
            etStreet.setText(cur.getStreet());
            etWard.setText(cur.getWard());
            etDistrict.setText(cur.getDistrict());
            etCity.setText(cur.getCity());
        }

        btnCancel.setOnClickListener(v1 -> dismiss());

        btnSave.setOnClickListener(v12 -> {
            if (TextUtils.isEmpty(etName.getText())) { etName.setError("Nhập họ tên"); return; }
            if (TextUtils.isEmpty(etPhone.getText())) { etPhone.setError("Nhập số điện thoại"); return; }
            if (TextUtils.isEmpty(etStreet.getText())) { etStreet.setError("Nhập địa chỉ"); return; }

            if (listener != null) {
                Address a = new Address(
                        etName.getText().toString().trim(),
                        etPhone.getText().toString().trim(),
                        etStreet.getText().toString().trim(),
                        etWard.getText().toString().trim(),
                        etDistrict.getText().toString().trim(),
                        etCity.getText().toString().trim()
                );
                listener.onAddressSaved(a);
            }
            dismiss();
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
