package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.ItemModel;

import java.util.List;

public interface ItemService {

    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    List<ItemModel> listItem();

    ItemModel getItemById(Integer id);

    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;

    boolean increaseStock(Integer itemId, Integer amount) throws BusinessException;

    void increaseSales(Integer itemId, Integer amount) throws BusinessException;

    boolean asyncReduceStock(Integer itemId, Integer amount) throws BusinessException;

    //item及promo mode缓存模型
    ItemModel getItemByIdInCache(Integer id);

    String initStockLog(Integer itemId, Integer amount);

}
