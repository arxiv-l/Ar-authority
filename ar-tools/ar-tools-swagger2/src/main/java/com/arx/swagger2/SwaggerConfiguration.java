package com.arx.swagger2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author arx
 * @version 1.0
 * @description swagger2启动类
 *              启动条件：1.配置文件中arx.swagger.enable=true
 *                      2.配置文件中不存在：arx.swagger.enabled 值
 * @className com.arx.swagger2.Swagger2Configuration
 * @create 2021-12-18 15:34
 *
 */
@Configuration
@ConditionalOnProperty(name = "arx.swagger.enable",havingValue = "true",matchIfMissing = true)
@EnableSwagger2
@ComponentScan(
        basePackages = {
                "com.github.xiaoymin.knife4j.spring.plugin",
                "com.github.xiaoymin.knife4j.spring.web"
        }
)
@Import(BeanValidatorPluginsConfiguration.class)
public class SwaggerConfiguration implements WebMvcConfigurer {

    /**
     * 注入资源文件，相当于xml配置的
     *       <!--swagger资源配置-->
     *      <mvc:resources location="classpath:/META-INF/resources/" mapping="swagger-ui.html"/>
     *      <mvc:resources location="classpath:/META-INF/resources/webjars/" mapping="/webjars/**"/>
     * 防止@EnableMvc把默认的静态资源路径覆盖，手动设置
     * @param registry registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //解决静态资源无法访问的问题
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        //解决swagger无法访问的问题
        registry.addResourceHandler("swaggers-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        //解决swagger的js文件无法访问的问题
        registry.addResourceHandler("/webjars*").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
