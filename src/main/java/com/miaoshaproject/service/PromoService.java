package com.miaoshaproject.service;

import com.miaoshaproject.service.model.PromoModel;

public interface PromoService {
    PromoModel getPromoByItemId(int itemId);

    void publishPromo(Integer promoId);
}
