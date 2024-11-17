package com.ytb.codesandbox.model;

import lombok.Data;

import java.util.List;

@Data
public class ExecuteMessage {

    /**
     * 错误码
     */
    private Integer exitValue;

    /**
     * 正常信息
     */
    private List<OutputItem> message;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间
     */
    private Long time;

    private Long memory;
}
