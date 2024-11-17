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
public abstract class JavaCodeSandboxTemplate implements CodeSandBox{


    private final static String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private final static String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<List<InputItem>> inputTestCaseList = executeCodeRequest.getInputTestCaseList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //1.把用户代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        //2.编译代码，得到class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);

        //3.执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputTestCaseList);

        //4.收集整理输出信息
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);
        //5.文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b){
            log.error("deleteFile error,userCodeFilePath = {}",userCodeFile.getAbsolutePath());
        }
        //6.错误处理
        return outputResponse;
    }

    /**
     * 1.把代码保存为文件
     * @param code
     * @return
     */
    public File saveCodeToFile(String code){
        //1.把用户代码保存为文件
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator+GLOBAL_CODE_DIR_NAME;
        //判断全局代码存放目录，没有则新建
        if (!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath+File.separator+GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2.编译文件
     * @param file
     * @return
     */
    public ExecuteMessage compileFile(File file){
        //2.编译代码，得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s",file.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译",null);
            if (executeMessage.getExitValue() != 0){
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * 3.执行文件，获得执行结果列表
     * @param userCodeFile
     * @param
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile,List<List<InputItem>> inputTestCaseList){
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (List inputItemList : inputTestCaseList){
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputItemList);
            try{
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //超时控制 限制用户执行超时程序
                new Thread(() ->{
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行",inputItemList);
                log.info("executeMessage=:"+executeMessage.toString());
                executeMessageList.add(executeMessage);
            }catch (Exception e){
                throw new RuntimeException("执行错误",e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4.获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<List<OutputItem>> outputTestResultList = new ArrayList<>();
        //取用最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList){
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                //用户提交的程序执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputTestResultList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null){
                maxTime = Math.max(maxTime,time);
            }
        }
        //正常运行完成
        if (outputTestResultList.size() == executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputTestResultList(outputTestResultList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        //要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5.删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile){
        if (userCodeFile.getParentFile() != null){
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del ? "成功":"失败"));
            return del;
        }
        return true;
    }

    /**
     * 获取错误响应
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputTestResultList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        //表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
