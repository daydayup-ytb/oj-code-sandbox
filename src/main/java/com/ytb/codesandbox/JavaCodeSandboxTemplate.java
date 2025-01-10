package com.ytb.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ytb.codesandbox.model.*;
import com.ytb.codesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandBox {


    private final static String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private final static String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        //获取测试用例列表
        List<List<InputItem>> inputTestCaseList = executeCodeRequest.getInputTestCaseList();
        //获取单个测试用例
        List<InputItem> inputTestCase = executeCodeRequest.getInputTestCase();
        //获取执行代码
        String code = executeCodeRequest.getCode();
        //获取代码语言
        String language = executeCodeRequest.getLanguage();
        //执行代码返回信息
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        ExecuteResultInfo executeResultInfo = new ExecuteResultInfo();
        //1.把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        //2.编译代码，得到class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        log.info("编译代码返回信息："+ compileFileExecuteMessage);
        //编译错误
        if (compileFileExecuteMessage.getExitValue() != 0) {
            executeResultInfo.setCode(ExecuteCodeInfoEnum.COMPILE_ERROR.getValue());
            executeResultInfo.setMessage(ExecuteCodeInfoEnum.COMPILE_ERROR.getMessage());
            //格式化错误信息，删除文件路径
            String errorMessage = compileFileExecuteMessage.getErrorMessage();
            // 定义固定字符串
            String fixedString = "Main.java:";
            // 找到固定字符串的位置
            int fixedStringIndex = errorMessage.indexOf(fixedString);
            // 如果找到了固定字符串，则提取其后的部分
            String errorInfo = errorMessage.substring(fixedStringIndex + fixedString.length()).trim();
            executeResultInfo.setErrorInfo(errorInfo);
            executeCodeResponse.setExecuteResultInfo(executeResultInfo);
            return executeCodeResponse;
        }

        //输入测试用例错误
        if ((inputTestCaseList == null || inputTestCaseList.isEmpty()) && inputTestCase == null){
            executeResultInfo.setCode(ExecuteCodeInfoEnum.TESTCASE_ERROR.getValue());
            executeResultInfo.setMessage(ExecuteCodeInfoEnum.TESTCASE_ERROR.getMessage());
            executeCodeResponse.setExecuteResultInfo(executeResultInfo);
            return executeCodeResponse;
        }

        if (inputTestCaseList == null || inputTestCaseList.isEmpty()) {
            //3.执行代码，得到输出结果
            ExecuteMessage executeMessage = runFileByTestCase(userCodeFile, inputTestCase);
            //4.收集整理输出信息
            String errorMessage = executeMessage.getErrorMessage();
            //运行错误
            if (StrUtil.isNotBlank(errorMessage)) {
                executeResultInfo.setCode(ExecuteCodeInfoEnum.RUNTIME_ERROR.getValue());
                executeResultInfo.setMessage(ExecuteCodeInfoEnum.RUNTIME_ERROR.getMessage());
                executeResultInfo.setErrorInfo(errorMessage);
                executeCodeResponse.setExecuteResultInfo(executeResultInfo);
                return executeCodeResponse;
            }
            //正常运行完成
            executeResultInfo.setCode(ExecuteCodeInfoEnum.ACCEPTED.getValue());
            executeResultInfo.setMessage(ExecuteCodeInfoEnum.ACCEPTED.getMessage());
            executeResultInfo.setTime(executeMessage.getTime());
            //要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//            executeResultInfo.setMemory();
            executeCodeResponse.setExecuteResultInfo(executeResultInfo);
            executeCodeResponse.setOutputTestResult(executeMessage.getMessage());
        } else {
            //3.执行代码，得到输出结果
            List<ExecuteMessage> executeMessageList = runFileByTestCaseList(userCodeFile, inputTestCaseList);
            //4.收集整理输出信息
            List<List<OutputItem>> outputTestResultList = new ArrayList<>();
            //获取所有测试用例执行时间最大值，便于判断是否超时
            long maxTime = 0;
            for (ExecuteMessage executeMessage : executeMessageList) {
                String errorMessage = executeMessage.getErrorMessage();
                //运行错误
                if (StrUtil.isNotBlank(errorMessage)) {
                    executeResultInfo.setCode(ExecuteCodeInfoEnum.RUNTIME_ERROR.getValue());
                    executeResultInfo.setMessage(ExecuteCodeInfoEnum.RUNTIME_ERROR.getMessage());
                    executeResultInfo.setErrorInfo(errorMessage);
                    executeCodeResponse.setExecuteResultInfo(executeResultInfo);
                    return executeCodeResponse;
                }
                //运行正常，获取执行代码输出信息
                outputTestResultList.add(executeMessage.getMessage());
                Long time = executeMessage.getTime();
                if (time != null) {
                    maxTime = Math.max(maxTime, time);
                }
            }

            if (outputTestResultList.size() != executeMessageList.size()) {
                executeResultInfo.setCode(ExecuteCodeInfoEnum.RUNTIME_ERROR.getValue());
                executeResultInfo.setMessage(ExecuteCodeInfoEnum.RUNTIME_ERROR.getMessage());
                executeCodeResponse.setExecuteResultInfo(executeResultInfo);
                return executeCodeResponse;
            }
            //正常运行完成
            executeResultInfo.setCode(ExecuteCodeInfoEnum.ACCEPTED.getValue());
            executeResultInfo.setMessage(ExecuteCodeInfoEnum.ACCEPTED.getMessage());
            executeResultInfo.setTime(maxTime);
            //要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//            executeResultInfo.setMemory();
            executeCodeResponse.setExecuteResultInfo(executeResultInfo);
            executeCodeResponse.setOutputTestResultList(outputTestResultList);
            return executeCodeResponse;
        }

        //5.文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("deleteFile error,userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        //6.错误处理
        return executeCodeResponse;
    }

    /**
     * 1.把代码保存为文件
     *
     * @param code
     * @return
     */
    public File saveCodeToFile(String code) {
        //1.把用户代码保存为文件
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //判断全局代码存放目录，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2.编译文件
     *
     * @param file
     * @return
     */
    public ExecuteMessage compileFile(File file) {
        //2.编译代码，得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", file.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "compile", null);
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * 3.执行文件，获得执行结果列表
     *
     * @param userCodeFile
     * @param
     * @return
     */
    public List<ExecuteMessage> runFileByTestCaseList(File userCodeFile, List<List<InputItem>> inputTestCaseList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (List inputItemList : inputTestCaseList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputItemList);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //超时控制 限制用户执行超时程序
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "run", inputItemList);
                log.info("executeMessage=:" + executeMessage.toString());
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 3.执行文件，获得执行结果列表
     *
     * @param userCodeFile
     * @param
     * @return
     */
    public ExecuteMessage runFileByTestCase(File userCodeFile, List<InputItem> inputTestCase) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputTestCase);
        try {
            Process runProcess = Runtime.getRuntime().exec(runCmd);
            //超时控制 限制用户执行超时程序
            new Thread(() -> {
                try {
                    Thread.sleep(TIME_OUT);
                    System.out.println("超时了，中断");
                    runProcess.destroy();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "run", inputTestCase);
            log.info("运行代码返回信息:" + executeMessage);
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException("执行错误", e);
        }
    }

//    /**
//     * 4.获取输出结果
//     *
//     * @param executeMessageList
//     * @return
//     */
//    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
//        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
//        List<List<OutputItem>> outputTestResultList = new ArrayList<>();
//        //获取所有测试用例执行时间最大值，便于判断是否超时
//        long maxTime = 0;
//        for (ExecuteMessage executeMessage : executeMessageList) {
//            String errorMessage = executeMessage.getErrorMessage();
//            //运行错误
//            if (StrUtil.isNotBlank(errorMessage)) {
//                executeCodeResponse.setErrorInfo(errorMessage);
//                executeCodeResponse.setCode(ExecuteCodeInfoEnum.RUNTIME_ERROR.getValue());
//                executeCodeResponse.setStatus(3);
//                return executeCodeResponse;
//            }
//            //运行正常，获取执行代码输出信息
//            outputTestResultList.add(executeMessage.getMessage());
//            Long time = executeMessage.getTime();
//            if (time != null) {
//                maxTime = Math.max(maxTime, time);
//            }
//        }
//
//        if (outputTestResultList.size() != executeMessageList.size()) {
//            executeCodeResponse.setCode(ExecuteCodeInfoEnum.RUNTIME_ERROR.getValue());
//            executeCodeResponse.setStatus(3);
//            return executeCodeResponse;
//        }
//        //正常运行完成
//        executeCodeResponse.setCode(ExecuteCodeInfoEnum.ACCEPTED.getValue());
//        executeCodeResponse.setStatus(1);
//        executeCodeResponse.setOutputTestResultList(outputTestResultList);
//        JudgeInfo judgeInfo = new JudgeInfo();
//        judgeInfo.setTime(maxTime);
//        //要借助第三方库来获取内存占用，非常麻烦，此处不做实现
////        judgeInfo.setMemory();
//        executeCodeResponse.setJudgeInfo(judgeInfo);
//        return executeCodeResponse;
//    }

//    /**
//     * 4.获取输出结果
//     *
//     * @param executeMessage
//     * @return
//     */
//    public ExecuteCodeResponse getOutputResponse(ExecuteMessage executeMessage) {
//        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
//        String errorMessage = executeMessage.getErrorMessage();
//        //运行错误
//        if (StrUtil.isNotBlank(errorMessage)) {
//            executeCodeResponse.setErrorInfo(errorMessage);
//            executeCodeResponse.setStatus(3);
//            executeCodeResponse.setCode(ExecuteCodeInfoEnum.RUNTIME_ERROR.getValue());
//            return executeCodeResponse;
//        }
//        //正常运行完成
//        executeCodeResponse.setCode(ExecuteCodeInfoEnum.ACCEPTED.getValue());
//        executeCodeResponse.setStatus(1);
//        executeCodeResponse.setOutputTestResult(executeMessage.getMessage());
//        JudgeInfo judgeInfo = new JudgeInfo();
//        judgeInfo.setTime(executeMessage.getTime());
//        //要借助第三方库来获取内存占用，非常麻烦，此处不做实现
////        judgeInfo.setMemory();
//        executeCodeResponse.setJudgeInfo(judgeInfo);
//
//        return executeCodeResponse;
//    }

    /**
     * 5.删除文件
     *
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

//    /**
//     * 获取错误响应
//     *
//     * @param e
//     * @return
//     */
//    private ExecuteCodeResponse getErrorResponse(Throwable e) {
//        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
//        executeCodeResponse.setOutputTestResultList(new ArrayList<>());
//        executeCodeResponse.setMessage(e.getMessage());
//        //表示代码沙箱错误
//        executeCodeResponse.setStatus(2);
//        executeCodeResponse.setJudgeInfo(new JudgeInfo());
//        return executeCodeResponse;
//    }
}
