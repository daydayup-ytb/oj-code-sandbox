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
     * 代码沙箱执行结果信息
     */
    private ExecuteResultInfo executeResultInfo;

    private List<List<OutputItem>> outputTestResultList;

    private List<OutputItem> outputTestResult;
}
