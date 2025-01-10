package com.ytb.codesandbox.model;

import lombok.Data;

@Data
public class ExecuteResultInfo {

    //执行结果代码
    private Integer code;

    //执行结果信息
    private String message;

    //执行错误信息
    private String errorInfo;

    //执行内存
    private Long memory;

    //执行时间
    private Long time;

}
