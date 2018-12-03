package com.yhbuao.superrecycleview.entity;

import java.io.Serializable;
import java.util.ArrayList;



public class ShopGoods implements Serializable {

    private ArrayList<ShopGoodsBean> mList = new ArrayList<>();

    public ShopGoods(ArrayList<ShopGoodsBean> list) {
        mList = list;
    }

    public ArrayList<ShopGoodsBean> getList() {
        return mList;
    }

    public void setList(ArrayList<ShopGoodsBean> list) {
        mList = list;
    }
}
