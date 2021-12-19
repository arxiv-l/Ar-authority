package com.arx.swagger2;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.*;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author arx
 * @version 1.0
 * @description swagger自动配置类
 * @className com.arx.swagger2.SwaggerAutoConfiguration
 * @create 2021-12-18 16:03
 */
@Import(SwaggerConfiguration.class)
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerAutoConfiguration implements BeanFactoryAware {

    /**
     * 授权密码
     */
    private static final String AUTH_KEY = "token";

    /**
     * 读取配置类数据
     */
    @Autowired
    SwaggerProperties swaggerProperties;

    /**
     * Bean工厂
     */
    private BeanFactory beanFactory;

    /**
     * 构建并且配置docket对象
     *
     * @return 返回一个构建完成的swagger API应用
     */
    @Bean
    @ConditionalOnProperty(name = "arx.swagger.enable", havingValue = "true", matchIfMissing = true)
    public List<Docket> createRestApi() {
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;

        List<Docket> docketList = new LinkedList<>();

        //没有分组
        if (swaggerProperties.getDocket().isEmpty()) {
            Docket docket = createDocket(swaggerProperties);
            configurableBeanFactory.registerSingleton(swaggerProperties.getTitle(), docket);
            docketList.add(docket);
            return docketList;
        }

        //分组创建
        for (String groupName : swaggerProperties.getDocket().keySet()) {
            SwaggerProperties.DocketInfo docketInfo = swaggerProperties.getDocket().get(groupName);
            //构建文档API基础信息
            final ApiInfo apiInfo = new ApiInfoBuilder()
                    //判断分组文档中的标题是否为存在，存在使用分组中的标题，不存在使用默认标题。避免分组标题为空的情况
                    .title(docketInfo.getTitle().isEmpty() ? swaggerProperties.getTitle() : docketInfo.getTitle())
                    .description(docketInfo.getDescription().isEmpty() ? swaggerProperties.getDescription() : docketInfo.getDescription())
                    .version(docketInfo.getVersion().isEmpty() ? swaggerProperties.getVersion() : docketInfo.getVersion())
                    .license(docketInfo.getLicense().isEmpty() ? swaggerProperties.getLicense() : docketInfo.getLicense())
                    .licenseUrl(docketInfo.getLicenseUrl().isEmpty() ? swaggerProperties.getLicenseUrl() : docketInfo.getLicenseUrl())
                    .termsOfServiceUrl(docketInfo.getTermsOfServiceUrl().isEmpty() ? swaggerProperties.getTermsOfServiceUrl() : docketInfo.getTermsOfServiceUrl())
                    .contact(
                            new Contact(
                                    docketInfo.getContact().getName().isEmpty() ? swaggerProperties.getContact().getName() : docketInfo.getContact().getName(),
                                    docketInfo.getContact().getUrl().isEmpty() ? swaggerProperties.getContact().getUrl() : docketInfo.getContact().getUrl(),
                                    docketInfo.getContact().getEmail().isEmpty() ? swaggerProperties.getContact().getEmail() : docketInfo.getContact().getEmail()
                            )
                    )
                    .build();
            // 当没有配置任何path的时候，解析/**
            if (docketInfo.getBasePath().isEmpty()) {
                docketInfo.getBasePath().add("/**");
            }

            List<Predicate<String>> basePath = new ArrayList<>(docketInfo.getBasePath().size());
            for (String path : docketInfo.getBasePath()) {
                basePath.add(PathSelectors.ant(path));
            }

            // exclude-path处理
            List<Predicate<String>> excludePath = new ArrayList<>(docketInfo.getExcludePath().size());
            for (String path : docketInfo.getExcludePath()) {
                excludePath.add(PathSelectors.ant(path));
            }
            //读取分组终中的信息，有修改的地方把修改的地方对全局配置进行覆盖
            List<Parameter> parameters = assemblyGlobalOperationParameters(swaggerProperties.getGlobalOperationParameters(), docketInfo.getGlobalOperationParameters());
            //构建docket对象
            Docket docket = new Docket(DocumentationType.SWAGGER_2)
                    .host(swaggerProperties.getHost())
                    .apiInfo(apiInfo)
                    .globalOperationParameters(parameters)
                    .groupName(docketInfo.getGroup())
                    .select()
                    .apis(RequestHandlerSelectors.basePackage(docketInfo.getBasePackage()))
                    .paths(
                            Predicates.and(
                                    Predicates.not(Predicates.or(excludePath)),
                                    Predicates.or(basePath)
                            )
                    )
                    .build()
                    //安全配置 安全规则
                    .securitySchemes(securitySchemes())
                    //安全配置上下文
                    .securityContexts(securityContexts())
                    .globalResponseMessage(RequestMethod.GET, getResponseMessages())
                    .globalResponseMessage(RequestMethod.POST, getResponseMessages())
                    .globalResponseMessage(RequestMethod.PUT, getResponseMessages())
                    .globalResponseMessage(RequestMethod.DELETE, getResponseMessages());
            configurableBeanFactory.registerSingleton(groupName, docket);
            docketList.add(docket);
        }
        return docketList;
    }

    /**
     * 创建 docket 对象
     *
     * @param swaggerProperties 配置文件信息javaBean
     * @return 返回文档信息
     */
    private Docket createDocket(SwaggerProperties swaggerProperties) {
        //配置swagger API 基础信息
        final ApiInfo apiInfo = new ApiInfoBuilder()
                .title(swaggerProperties.getTitle())
                .description(swaggerProperties.getDescription())
                .version(swaggerProperties.getVersion())
                .license(swaggerProperties.getLicense())
                .licenseUrl(swaggerProperties.getLicenseUrl())
                .termsOfServiceUrl(swaggerProperties.getTermsOfServiceUrl())
                .contact(new Contact(swaggerProperties.getContact().getName(), swaggerProperties.getContact().getUrl(), swaggerProperties.getContact().getEmail()))
                .build();
        //判断是否配置基础路径 basePath
        if (swaggerProperties.getBasePath().isEmpty()) {
            //没有配置直接解析位“/**”
            swaggerProperties.getBasePath().add("/**");
        }
        //构建basePath集合
        List<Predicate<String>> basePath = new ArrayList<>();
        for (String path : swaggerProperties.getBasePath()) {
            basePath.add(PathSelectors.ant(path));
        }
        //处理exclude-path处理
        List<Predicate<String>> excludePath = new ArrayList<>();
        for (String path : swaggerProperties.getExcludePath()) {
            excludePath.add(PathSelectors.ant(path));
        }
        return new Docket(DocumentationType.SWAGGER_2)
                .host(swaggerProperties.getHost())
                //项目接口基础信息
                .apiInfo(apiInfo)
                //分组名称
                .groupName(swaggerProperties.getGroup())
                .globalOperationParameters(
                        buildGlobalOperationParametersFromSwaggerProperties(swaggerProperties.getGlobalOperationParameters())
                )

                .select()
                .apis(RequestHandlerSelectors.basePackage(swaggerProperties.getBasePackage()))
                .paths(
                        Predicates.and(
                                Predicates.not(Predicates.or(excludePath)),
                                Predicates.or(basePath)
                        )
                )
                .build()
                //安全配置 安全规则
                .securitySchemes(securitySchemes())
                //安全配置的上下文
                .securityContexts(securityContexts())
                .globalResponseMessage(RequestMethod.GET, getResponseMessages())
                .globalResponseMessage(RequestMethod.POST, getResponseMessages())
                .globalResponseMessage(RequestMethod.PUT, getResponseMessages())
                .globalResponseMessage(RequestMethod.DELETE, getResponseMessages());
    }

    /**
     * 构建响应信息
     *
     * @return 返回响应信息
     */
    private List<ResponseMessage> getResponseMessages() {
        return Arrays.asList(
                new ResponseMessageBuilder().code(0).message("成功").build(),
                new ResponseMessageBuilder().code(-1).message("系统繁忙").build(),
                new ResponseMessageBuilder().code(-2).message("服务超时").build(),
                new ResponseMessageBuilder().code(40001).message("绘话超时，请重新登陆").build(),
                new ResponseMessageBuilder().code(40002).message("缺少token参数").build()
        );
    }

    /**
     * 安全配置的上下文
     *
     * @return 返回配置完成的参数
     */
    private List<SecurityContext> securityContexts() {
        List<SecurityContext> contexts = new ArrayList<>(1);
        SecurityContext context = SecurityContext.builder()
                .securityReferences(defaultAuth())
                .build();
        contexts.add(context);
        return contexts;
    }

    /**
     * 默认验证参数
     *
     * @return 返回配置完成的参数
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> references = new ArrayList<>(1);
        references.add(new SecurityReference(AUTH_KEY, authorizationScopes));
        return references;
    }

    /**
     * 配置完全配置参数 配置token
     *
     * @return 返回配置完成的参数
     */
    private List<ApiKey> securitySchemes() {
        List<ApiKey> apiKeys = new ArrayList<>(1);
        final ApiKey apiKey = new ApiKey(AUTH_KEY, AUTH_KEY, "header");
        apiKeys.add(apiKey);
        return apiKeys;
    }

    /**
     * 全局参数配置
     *
     * @param globalOperationParameters globalOperationParameters参数
     * @return 返回配置完成的参数
     */
    private List<Parameter> buildGlobalOperationParametersFromSwaggerProperties(List<SwaggerProperties.GlobalOperationParameter> globalOperationParameters) {
        List<Parameter> parameters = Lists.newArrayList();
        if (Objects.isNull(globalOperationParameters)) {
            return parameters;
        }
        for (SwaggerProperties.GlobalOperationParameter globalOperationParameter : globalOperationParameters) {
            parameters.add(new ParameterBuilder()
                    .name(globalOperationParameter.getName())
                    .description(globalOperationParameter.getDescription())
                    .modelRef(new ModelRef(globalOperationParameter.getModelRef()))
                    .parameterType(globalOperationParameter.getParameterType())
                    .required(globalOperationParameter.getRequired())
                    .defaultValue(globalOperationParameter.getDefaultValue())
                    .allowEmptyValue(globalOperationParameter.getAllowEmptyValue())
                    .order(globalOperationParameter.getOrder())
                    .build()
            );
        }
        return parameters;
    }

    /**
     * 局部参数按照name覆盖全局参数
     *
     * @param globalOperationParameters 全局参数
     * @param docketOperationParameters 分组参数
     * @return 返回覆盖完成的参数
     */
    private List<Parameter> assemblyGlobalOperationParameters(List<SwaggerProperties.GlobalOperationParameter> globalOperationParameters, List<SwaggerProperties.GlobalOperationParameter> docketOperationParameters) {
        if (Objects.isNull(docketOperationParameters) || docketOperationParameters.isEmpty()) {
            return buildGlobalOperationParametersFromSwaggerProperties(globalOperationParameters);
        }

        docketOperationParameters.stream().map(SwaggerProperties.GlobalOperationParameter::getName).collect(Collectors.toSet());
        List<SwaggerProperties.GlobalOperationParameter> resultOperationParameters = Lists.newArrayList();
        if (Objects.nonNull(globalOperationParameters)) {
            for (SwaggerProperties.GlobalOperationParameter parameter : globalOperationParameters) {
                resultOperationParameters.add(parameter);
            }
        }
        resultOperationParameters.addAll(docketOperationParameters);
        return buildGlobalOperationParametersFromSwaggerProperties(resultOperationParameters);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
