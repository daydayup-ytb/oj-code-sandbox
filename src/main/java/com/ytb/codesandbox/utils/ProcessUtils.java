package com.ytb.codesandbox.utils;

import com.ytb.codesandbox.model.InputItem;
import com.ytb.codesandbox.model.OutputItem;
import com.ytb.codesandbox.model.ExecuteMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * 进程工具类
 */
@Slf4j
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     * @param runProcess
     * @param opName 操作名称
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName, List<InputItem> inputItemList){
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            if ("run".equals(opName)){
                for (InputItem inputItem: inputItemList){
                    String paramName = inputItem.getParamName();
                    String paramValue = inputItem.getParamValue();
                    String str = paramName+":"+paramValue+"\n";
                    outputStreamWriter.write(str);
                    //相当于按了回车
                    outputStreamWriter.flush();
                }
            }
            //等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            //正常退出
            if (exitValue == 0){
                log.info(opName+" success");
                //分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream(), "gbk"));
                List<OutputItem> outputItemList = new ArrayList<>();
                List<String> outputStrList = new ArrayList<>();
                //逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null){
                    outputStrList.add(compileOutputLine);
                    OutputItem outputItem = new OutputItem();
                    outputItem.setParamName("result");
                    outputItem.setParamValue(compileOutputLine);
                    outputItemList.add(outputItem);
                }
                executeMessage.setMessage(outputItemList);
            }else{
                //异常退出
                log.info(opName+" failure");
                //分批获取进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream(),"gbk"));
                List<String> errorCompileOutputStrList = new ArrayList<>();
                //逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null){
                    errorCompileOutputStrList.add(errorCompileOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(errorCompileOutputStrList,'\n'));
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        }catch (Exception e){
            e.printStackTrace();
        }
        return executeMessage;
    }
}
