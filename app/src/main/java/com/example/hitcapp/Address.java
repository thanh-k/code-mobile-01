package com.example.hitcapp;

import android.os.Parcel;
import android.os.Parcelable;

public class Address implements Parcelable {
    private String name;
    private String phone;
    private String street;
    private String ward;
    private String district;
    private String city;

    public Address() {}

    public Address(String name, String phone, String street,
                   String ward, String district, String city) {
        this.name = name;
        this.phone = phone;
        this.street = street;
        this.ward = ward;
        this.district = district;
        this.city = city;
    }

    protected Address(Parcel in) {
        name = in.readString();
        phone = in.readString();
        street = in.readString();
        ward = in.readString();
        district = in.readString();
        city = in.readString();
    }

    public static final Creator<Address> CREATOR = new Creator<Address>() {
        @Override
        public Address createFromParcel(Parcel in) {
            return new Address(in);
        }

        @Override
        public Address[] newArray(int size) {
            return new Address[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(phone);
        dest.writeString(street);
        dest.writeString(ward);
        dest.writeString(district);
        dest.writeString(city);
    }

    // Getter & Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
