//package com.yupi.yuojcodesandbox;
//
//
//import cn.hutool.core.util.ArrayUtil;
//
//import com.github.dockerjava.api.DockerClient;
//import com.github.dockerjava.api.async.ResultCallback;
//import com.github.dockerjava.api.command.*;
//import com.github.dockerjava.api.model.*;
//import com.github.dockerjava.core.DockerClientBuilder;
//import com.github.dockerjava.core.command.ExecStartResultCallback;
//import model.com.ytb.codesandbox.ExecuteMessage;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StopWatch;
//
//
//import java.io.Closeable;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//@Component
//public class JavaDockerCodeSandBox extends JavaCodeSandboxTemplate{
//
//    private static final long TIME_OUT = 5000L;
//
//    public static final boolean FIRST_INIT = true;
//
//
//    @Override
//    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
//        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
//        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
//        //拉取镜像
//        String image = "openjdk:8-alpine";
//        if (FIRST_INIT){
//            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
//                @Override
//                public void onNext(PullResponseItem item) {
//                    System.out.println("下载镜像"+item.getStatus());
//                    super.onNext(item);
//                }
//            };
//            try {
//                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
//            } catch (InterruptedException e) {
//                System.out.println("拉取镜像异常");
//                throw new RuntimeException(e);
//            }
//        }
//        System.out.println("下载完成");
//        //创建容器
//        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
//        HostConfig hostConfig = new HostConfig();
//        hostConfig.withMemory(100*1000*1000L);
//        hostConfig.withCpuCount(1L);
//        hostConfig.setBinds(new Bind(userCodeParentPath,new Volume("/app")));
//        CreateContainerResponse createContainerResponse = containerCmd
//                .withHostConfig(hostConfig)
//                .withNetworkDisabled(true)
//                .withAttachStdin(true)
//                .withAttachStderr(true)
//                .withAttachStdout(true)
//                .withTty(true)
//                .exec();
//        System.out.println(createContainerResponse);
//        String containerId = createContainerResponse.getId();
//        //启动容器
//        dockerClient.startContainerCmd(containerId).exec();
//
//        List<ExecuteMessage> executeMessageList = new ArrayList<>();
//        for (String inputArgs : inputList){
//            StopWatch stopWatch = new StopWatch();
//            String[] inputArgsArray = inputArgs.split(" ");
//            String[] cmdArray  = ArrayUtil.append(new String[]{"java","-cp","/app","Main"},inputArgsArray);
//            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
//                    .withCmd(cmdArray)
//                    .withAttachStdin(true)
//                    .withAttachStderr(true)
//                    .withAttachStdout(true)
//                    .exec();
//            System.out.println("创建执行命令："+execCreateCmdResponse);
//
//            ExecuteMessage executeMessage = new ExecuteMessage();
//            final String[] message = {null};
//            final String[] errorMessage = {null};
//            long time = 0L;
//            final boolean[] timeout = {true};
//            String execId = execCreateCmdResponse.getId();
//            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback(){
//
//                @Override
//                public void onComplete() {
//                    timeout[0] = false;
//                    super.onComplete();
//                }
//
//                @Override
//                public void onNext(Frame frame) {
//                    StreamType streamType = frame.getStreamType();
//                    if (StreamType.STDERR.equals(streamType)){
//                        errorMessage[0] = new String(frame.getPayload());
//                        System.out.println("输出错误结果"+ errorMessage[0]);
//                    }else{
//                        message[0] = new String(frame.getPayload());
//                        System.out.println("输出结果"+ message[0]);
//                    }
//                    super.onNext(frame);
//                }
//
//            };
//
//            final long[] maxMemory = {0L};
//            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
//            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
//                @Override
//                public void onStart(Closeable closeable) {
//
//                }
//
//                @Override
//                public void onNext(Statistics statistics) {
//                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
//                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
//                }
//
//                @Override
//                public void onError(Throwable throwable) {
//
//                }
//
//                @Override
//                public void onComplete() {
//
//                }
//
//                @Override
//                public void close() throws IOException {
//
//                }
//            });
//            statsCmd.exec(statisticsResultCallback);
//            try {
//                stopWatch.start();
//                dockerClient.execStartCmd(execId).exec(execStartResultCallback).awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
//                stopWatch.stop();
//                time = stopWatch.getLastTaskTimeMillis();
//            } catch (InterruptedException e) {
//                System.out.println("程序执行异常");
//                throw new RuntimeException(e);
//            }
//            executeMessage.setMessage(message[0]);
//            executeMessage.setErrorMessage(errorMessage[0]);
//            executeMessage.setTime(time);
//            executeMessage.setMemory(maxMemory[0]);
//            executeMessageList.add(executeMessage);
//        }
//        return executeMessageList;
//    }
//
//}
