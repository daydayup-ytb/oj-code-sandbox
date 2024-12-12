package com.ytb.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCodeResponse {

    /**
     * 代码
     */
    private Integer code;

    /**
     * 信息
     */
    private String message;

    /**
     * 错误信息
     */
    private String errorInfo;

    /**
     * 执行状态
     */
    private Integer status;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;

    private List<List<OutputItem>> outputTestResultList;

    private List<OutputItem> outputTestResult;
}
