package com.miaoshaproject.service;

//封装本地缓存操作类
public interface CacheService {

    void setCommonCache(String key, Object value);

    Object getCommonCache(String key);
}
