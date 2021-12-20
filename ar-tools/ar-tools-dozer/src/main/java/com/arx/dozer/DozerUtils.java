package com.arx.dozer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.dozermapper.core.Mapper;

/**
 * @author arx
 * @version 1.0
 * @description Dozer bean转换工具类
 * @className com.arx.dozer.DozerUtils
 * @create 2021-12-20 8:24
 */
public class DozerUtils {

    /**
     * 声明mapper对象
     */
    private Mapper mapper;

    public DozerUtils(Mapper mapper) {
        this.mapper = mapper;
    }

    public Mapper getMapper() {
        return this.mapper;
    }

    /**
     * Constructs new instance of destinationClass and performs mapping between from source
     * 构造 destinationClass 的新实例并执行源之间的映射
     * 基于Dozer转换对象的类型.
     *
     * @param source 源数据
     * @param destinationClass 转换目标类
     * @param <T> T
     * @return 返回完成的对象
     */
    public <T> T map(Object source, Class<T> destinationClass) {
        if (source == null) {
            return null;
        }
        return mapper.map(source, destinationClass);
    }

    /**
     * Constructs new instance of destinationClass and performs mapping between from source
     * 构造 destinationClass 的新实例并执行源之间的映射
     * 基于Dozer转换对象的类型.
     *
     * @param source 源数据
     * @param destinationClass 转换目标类
     * @param <T> T
     * @return 返回完成的对象
     */
    public <T> T map2(Object source, Class<T> destinationClass) {
        if (source == null) {
            try {
                return destinationClass.newInstance();
            } catch (Exception ignored) {
            }
        }
        return mapper.map(source, destinationClass);
    }

    /**
     * Performs mapping between source and destination objects
     *
     * @param source
     * @param destination
     */
    public void map(Object source, Object destination) {
        if (source == null) {
            return;
        }
        mapper.map(source, destination);
    }

    /**
     * Constructs new instance of destinationClass and performs mapping between from source
     *
     * @param source
     * @param destinationClass
     * @param mapId
     * @param <T>
     * @return
     */
    public <T> T map(Object source, Class<T> destinationClass, String mapId) {
        if (source == null) {
            return null;
        }
        return mapper.map(source, destinationClass, mapId);
    }

    /**
     * Performs mapping between source and destination objects
     *
     * @param source
     * @param destination
     * @param mapId
     */
    public void map(Object source, Object destination, String mapId) {
        if (source == null) {
            return;
        }
        mapper.map(source, destination, mapId);
    }

    /**
     * 将集合转成集合
     * List<A> -->  List<B>
     *
     * @param sourceList       源集合
     * @param destinationClass 待转类型
     * @param <T>
     * @return
     */
    public <T, E> List<T> mapList(Collection<E> sourceList, Class<T> destinationClass) {
        return mapPage(sourceList, destinationClass);
    }


    public <T, E> List<T> mapPage(Collection<E> sourceList, Class<T> destinationClass) {
        if (sourceList == null || sourceList.isEmpty() || destinationClass == null) {
            return Collections.emptyList();
        }
        List<T> destinationList = sourceList.stream()
                .filter(item -> item != null)
                .map((sourceObject) -> mapper.map(sourceObject, destinationClass))
                .collect(Collectors.toList());

        return destinationList;
    }

    public <T, E> Set<T> mapSet(Collection<E> sourceList, Class<T> destinationClass) {
        if (sourceList == null || sourceList.isEmpty() || destinationClass == null) {
            return Collections.emptySet();
        }
        return sourceList.stream().map((sourceObject) -> mapper.map(sourceObject, destinationClass)).collect(Collectors.toSet());
    }
}
