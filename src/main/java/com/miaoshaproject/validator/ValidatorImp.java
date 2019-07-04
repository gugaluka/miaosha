package com.miaoshaproject.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

@Component
public class ValidatorImp implements InitializingBean {

    private Validator validator;

    //实现校验方法并返回结果
    public ValidationResult validate(Object bean) {
        ValidationResult result = new ValidationResult();
        Set<ConstraintViolation<Object>> validationSet = validator.validate(bean);
        if (validationSet.size() > 0) {
            result.setHasErrors(true);
            validationSet.forEach(validation -> {
                String errMsg = validation.getMessage();
                String propertyName = validation.getPropertyPath().toString();
                result.getErrorMsgMap().put(propertyName, errMsg);
            });
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //将hibernate validator通过工厂的初始化方法使其实例化
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
}
