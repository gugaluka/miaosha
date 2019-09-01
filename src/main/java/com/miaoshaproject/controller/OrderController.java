package com.miaoshaproject.controller;

import com.miaoshaproject.controller.BaseController.BaseController;
import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/order")
@CrossOrigin(origins = "*", allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends BaseController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/createorder", method = RequestMethod.POST, consumes = "application/x-www-form-urlencoded")
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId, @RequestParam(name = "amount") Integer amount, @RequestParam(name = "promoId", required = false) Integer promoId) throws BusinessException {
        System.out.println("itemId:" + itemId + "; amount: " + amount);
//        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isBlank(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登录");
        }

        UserVO userVO = (UserVO)redisTemplate.opsForValue().get(token);
        if(userVO == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登录");
        }
//        UserVO userVO = (UserVO) httpServletRequest.getSession().getAttribute("LOGIN_USER");


        OrderModel orderModel = orderService.createOrder(userVO.getId(), itemId, promoId, amount);
        return CommonReturnType.create(null);
    }
}
