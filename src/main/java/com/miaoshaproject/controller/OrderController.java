package com.miaoshaproject.controller;

import com.miaoshaproject.controller.BaseController.BaseController;
import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.mq.MqProducer;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

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

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(20);
    }

    @RequestMapping(value = "/createorder", method = RequestMethod.POST, consumes = "application/x-www-form-urlencoded")
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId, @RequestParam(name = "amount") Integer amount, @RequestParam(name = "promoId", required = false) Integer promoId, @RequestParam(name = "promoToken", required = false) String promoToken) throws BusinessException {

//        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登录");
        }

        UserVO userVO = (UserVO) redisTemplate.opsForValue().get(token);
        if (userVO == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登录");
        }
//        UserVO userVO = (UserVO) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        //校验秒杀令牌是否正确
        if (promoId != null) {
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_" + promoId + "_" + userVO.getId() + "_" + itemId);
            if (StringUtils.isBlank(inRedisPromoToken) || !StringUtils.equals(promoToken, inRedisPromoToken)) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌验证失败");
            }

        }

        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        //同步调用线程池的submit方法
        Future<Object> future= executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入库存流水
                String stockLogId = itemService.initStockLog(itemId, amount);

//              OrderModel orderModel = orderService.createOrder(userVO.getId(), itemId, promoId, amount);
                if (!mqProducer.trancastionAsyncReduceStock(userVO.getId(), itemId, promoId, amount, stockLogId)) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
                }
                return null;
            }
        });

        try {
            future.get();
        } catch (Exception e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        return CommonReturnType.create(null);
    }

    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken", method = RequestMethod.POST, consumes = "application/x-www-form-urlencoded")
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(name = "itemId") Integer itemId, @RequestParam(name = "promoId", required = true) Integer promoId) throws BusinessException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登录");
        }

        UserVO userVO = (UserVO) redisTemplate.opsForValue().get(token);
        if (userVO == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登录");
        }


        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userVO.getId());
        if (StringUtils.isBlank(promoToken)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }

        return CommonReturnType.create(promoToken);
    }
}
