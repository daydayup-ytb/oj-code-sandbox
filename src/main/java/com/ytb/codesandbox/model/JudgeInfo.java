package com.ytb.codesandbox.model;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {

    /**
     * 时间限制(ms)
     */
    private String message;

    /**
     * 内存限制(KB)
     */
    private Long memory;

    /**
     * 堆栈限制(KB)
     */
    private Long time;
}
