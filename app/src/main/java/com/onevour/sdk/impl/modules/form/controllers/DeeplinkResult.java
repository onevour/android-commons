package com.onevour.sdk.impl.modules.form.controllers;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DeeplinkResult {

    @Expose
    @SerializedName("photo")
    String face;

    @Expose
    @SerializedName("ttd")
    String signature;

    @Expose
    @SerializedName("nik")
    String nik;

    @Expose
    @SerializedName("nama")
    String name;

    @Expose
    @SerializedName("pob")
    String pob;

    @Expose
    @SerializedName("dob")
    String dob;

    @Expose
    @SerializedName("gol_darah")
    String blood;

    @Expose
    @SerializedName("jenis_kelamin")
    String sex;

    @Expose
    @SerializedName("alamat")
    String address;

    @Expose
    @SerializedName("rt")
    String rt;

    @Expose
    @SerializedName("rw")
    String rw;

    @Expose
    @SerializedName("kelurahan_desa")
    String kel;

    @Expose
    @SerializedName("kecamatan")
    String kec;

    @Expose
    @SerializedName("agama")
    String religion;

    @Expose
    @SerializedName("status_perkawinan")
    String marriage;

    @Expose
    @SerializedName("pekerjaan")
    String occupation;

    @Expose
    @SerializedName("kewarganegaraan")
    String nationality;

    @Expose
    @SerializedName("kota")
    String kota;

    @Expose
    @SerializedName("provinsi")
    String provinsi;

    @Expose
    @SerializedName("expired_date")
    String expiredDate;

    @Expose
    @SerializedName("printed_at")
    String printedAt;

    @Expose
    @SerializedName("fp")
    String fp;

    @Expose
    @SerializedName("sn")
    String sn;

    @Expose
    @SerializedName("pcid")
    String pcid;

    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPob() {
        return pob;
    }

    public void setPob(String pob) {
        this.pob = pob;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getBlood() {
        return blood;
    }

    public void setBlood(String blood) {
        this.blood = blood;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRt() {
        return rt;
    }

    public void setRt(String rt) {
        this.rt = rt;
    }

    public String getRw() {
        return rw;
    }

    public void setRw(String rw) {
        this.rw = rw;
    }

    public String getKel() {
        return kel;
    }

    public void setKel(String kel) {
        this.kel = kel;
    }

    public String getKec() {
        return kec;
    }

    public void setKec(String kec) {
        this.kec = kec;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getMarriage() {
        return marriage;
    }

    public void setMarriage(String marriage) {
        this.marriage = marriage;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getKota() {
        return kota;
    }

    public void setKota(String kota) {
        this.kota = kota;
    }

    public String getProvinsi() {
        return provinsi;
    }

    public void setProvinsi(String provinsi) {
        this.provinsi = provinsi;
    }

    public String getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(String expiredDate) {
        this.expiredDate = expiredDate;
    }

    public String getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(String printedAt) {
        this.printedAt = printedAt;
    }

    public String getFp() {
        return fp;
    }

    public void setFp(String fp) {
        this.fp = fp;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getPcid() {
        return pcid;
    }

    public void setPcid(String pcid) {
        this.pcid = pcid;
    }
}
