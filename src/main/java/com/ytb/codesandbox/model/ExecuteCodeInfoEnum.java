package com.ytb.codesandbox.model;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ExecuteCodeInfoEnum {
    ACCEPTED("成功", 8001,"Accepted"),
    COMPILE_ERROR("编译错误", 8003,"compile error"),
    RUNTIME_ERROR("运行错误", 8010,"runtime error"),
    TESTCASE_ERROR("测试用例错误", 8013,"test case error"),
    SYSTEM_ERROR("系统错误", 8014,"system error");

    private final String text;

    private final Integer value;

    private final String message;

    ExecuteCodeInfoEnum(String text, Integer value,String message) {
        this.text = text;
        this.value = value;
        this.message = message;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static ExecuteCodeInfoEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ExecuteCodeInfoEnum anEnum : ExecuteCodeInfoEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public String getMessage() {
        return message;
    }
}
