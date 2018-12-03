package com.yhbuao.superrecycleview.ui.dialog;

import com.yhbuao.superrecycleview.R;

/**
 */
public class DiscountCouponDialog extends BaseDialog {

    @Override
    protected void init() {
        setCancelable(false);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_discount_coupon;
    }


}
