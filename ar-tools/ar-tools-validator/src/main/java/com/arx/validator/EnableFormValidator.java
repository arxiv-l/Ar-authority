package com.arx.validator;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author arx
 * @version 1.0
 * @description 在启动类上添加该注解来启动表单验证功能
 * @className com.arx.validator.EnableFormValidator
 * @create 2021-12-20 9:02
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ValidatorConfiguration.class)
public @interface EnableFormValidator {
}
