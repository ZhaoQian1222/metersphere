/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.metersphere.api.jmeter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.api.dto.RequestResultExpandDTO;
import io.metersphere.api.dto.RunningParamKeys;
import io.metersphere.api.exec.queue.PoolExecBlockingQueueUtil;
import io.metersphere.api.exec.utils.ResultParseUtil;
import io.metersphere.base.domain.ApiScenarioReport;
import io.metersphere.base.domain.WorkspaceRepositoryFileVersion;
import io.metersphere.commons.utils.*;
import io.metersphere.dto.RequestResult;
import io.metersphere.jmeter.JMeterBase;
import io.metersphere.utils.JMeterVars;
import io.metersphere.utils.LoggerUtil;
import io.metersphere.websocket.c.to.c.WebSocketUtils;
import io.metersphere.websocket.c.to.c.util.MsgDto;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.reporters.AbstractListenerElement;
import org.apache.jmeter.samplers.*;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.threads.JMeterVariables;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时结果监听
 */
public class MsDebugListener extends AbstractListenerElement implements SampleListener, Clearable, Serializable,
        TestStateListener, Remoteable, NoThreadClone {

    private static final String ERROR_LOGGING = "MsResultCollector.error_logging"; // $NON-NLS-1$

    private static final String SUCCESS_ONLY_LOGGING = "MsResultCollector.success_only_logging"; // $NON-NLS-1$

    private static final String TEST_IS_LOCAL = "*local*"; // $NON-NLS-1$

    public static final String TEST_END = "MS_TEST_END";

    @Override
    public Object clone() {
        MsDebugListener clone = (MsDebugListener) super.clone();
        return clone;
    }

    public boolean isErrorLogging() {
        return getPropertyAsBoolean(ERROR_LOGGING);
    }

    public final void setSuccessOnlyLogging(boolean value) {
        if (value) {
            setProperty(new BooleanProperty(SUCCESS_ONLY_LOGGING, true));
        } else {
            removeProperty(SUCCESS_ONLY_LOGGING);
        }
    }

    /**
     * Get the state of successful only logging
     *
     * @return Flag whether only successful samples should be logged
     */
    public boolean isSuccessOnlyLogging() {
        return getPropertyAsBoolean(SUCCESS_ONLY_LOGGING, false);
    }

    public boolean isSampleWanted(boolean success, SampleResult result) {
        boolean errorOnly = isErrorLogging();
        boolean successOnly = isSuccessOnlyLogging();
        return isSampleWanted(success, errorOnly, successOnly) && !StringUtils.containsIgnoreCase(result.getSampleLabel(), "MS_CLEAR_LOOPS_VAR_");
    }

    public static boolean isSampleWanted(boolean success, boolean errorOnly,
                                         boolean successOnly) {
        return (!errorOnly && !successOnly) ||
                (success && successOnly) ||
                (!success && errorOnly);
    }

    @Override
    public void testEnded(String host) {
        LoggerUtil.info("Debug TestEnded " + this.getName());
        MsgDto dto = new MsgDto();
        dto.setExecEnd(false);
        dto.setContent(TEST_END);
        dto.setReportId("send." + this.getName());
        dto.setToReport(this.getName());
        LoggerUtil.debug("send. " + this.getName());
        WebSocketUtils.sendMessageSingle(dto);
        WebSocketUtils.onClose(this.getName());
        PoolExecBlockingQueueUtil.offer(this.getName());
    }

    @Override
    public void testStarted(String host) {
        LogUtil.debug("TestStarted " + this.getName());
    }

    @Override
    public void testEnded() {
        testEnded(TEST_IS_LOCAL);
    }

    @Override
    public void testStarted() {
        testStarted(TEST_IS_LOCAL);
    }

    @Override
    public void sampleStarted(SampleEvent e) {
        try {
            MsgDto dto = new MsgDto();
            dto.setContent(e.getThreadGroup());
            dto.setReportId("send." + this.getName());
            dto.setToReport(this.getName());
            LoggerUtil.debug("send. " + this.getName());
            WebSocketUtils.sendMessageSingle(dto);
        } catch (Exception ex) {
            LoggerUtil.error("消息推送失败：", ex);
        }
    }

    @Override
    public void sampleStopped(SampleEvent e) {
    }

    @Override
    public void sampleOccurred(SampleEvent event) {
        SampleResult result = event.getResult();
        this.setVars(result);
        if (isSampleWanted(result.isSuccessful(), result) && !StringUtils.equals(result.getSampleLabel(), RunningParamKeys.RUNNING_DEBUG_SAMPLER_NAME)) {
            RequestResult requestResult = JMeterBase.getRequestResult(result);
            if (requestResult != null && ResultParseUtil.isNotAutoGenerateSampler(requestResult)) {
                MsgDto dto = new MsgDto();
                dto.setExecEnd(false);
                dto.setReportId("send." + this.getName());
                dto.setToReport(this.getName());

                StringBuffer stringBuffer = new StringBuffer();
                //使用反射查询场景里csv信息
                try {
                    String reportId = this.getName();
                    if(Class.forName("io.metersphere.api.service.ApiScenarioReportService")!=null){
                        Class clazz = Class.forName("io.metersphere.api.service.ApiScenarioReportService");
                        Method method = clazz.getMethod("getByIds", List.class);
                        ArrayList<String> strings = new ArrayList<>();
                        strings.add(reportId);
                        Object apiScenarioReportObject = method.invoke(CommonBeanFactory.getBean("apiScenarioReportService"), strings);
                        if(apiScenarioReportObject!=null){
                            List<ApiScenarioReport> apiScenarioReportList = (List<ApiScenarioReport>) apiScenarioReportObject;
                            String scenarioId = apiScenarioReportList.get(0).getScenarioId();
                            String scenarioName = apiScenarioReportList.get(0).getScenarioName();
                            if (Class.forName("io.metersphere.xpack.repository.service.RepositoryApiAutomationService") != null) {
                                Class clazz1 = Class.forName("io.metersphere.xpack.repository.service.RepositoryFileVersionService");
                                Method method1 = clazz1.getMethod("queryRepositoryFileList", String.class);
                                Object repositoryApiAutomationService = method1.invoke(CommonBeanFactory.getBean("repositoryFileVersionService"), scenarioId);
                                if (repositoryApiAutomationService != null) {
                                    List<WorkspaceRepositoryFileVersion> workspaceRepositoryFileVersionList = (List<WorkspaceRepositoryFileVersion>) repositoryApiAutomationService;
                                    if (CollectionUtils.isNotEmpty(workspaceRepositoryFileVersionList)) {
                                        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
                                        for (WorkspaceRepositoryFileVersion workspaceRepositoryFileVersion : workspaceRepositoryFileVersionList) {
                                            objectObjectHashMap.put(workspaceRepositoryFileVersion.getPath(), workspaceRepositoryFileVersion.getCommitId());
                                        }
                                        stringBuffer.append("场景名称：" + scenarioName + "(" + scenarioId + ")当前的git-csv文件：" + JSONObject.toJSONString(objectObjectHashMap) + "\n");
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    LoggerUtil.error("webSocket追加git-csv日志失败！");
                }
                String console = FixedCapacityUtils.getJmeterLogger(this.getName(), false) + stringBuffer;
                if (StringUtils.isNotEmpty(requestResult.getName()) && requestResult.getName().startsWith("Transaction=")) {
                    requestResult.getSubRequestResults().forEach(transactionResult -> {
                        transactionResult.getResponseResult().setConsole(console);
                        //解析误报内容
                        RequestResultExpandDTO expandDTO = ResponseUtil.parseByRequestResult(transactionResult);
                        JSONObject requestResultObject = JSONObject.parseObject(JSON.toJSONString(expandDTO));
                        dto.setContent("result_" + JSON.toJSONString(requestResultObject));
                        WebSocketUtils.sendMessageSingle(dto);
                    });
                } else {
                    requestResult.getResponseResult().setConsole(console);
                    //解析误报内容
                    RequestResultExpandDTO expandDTO = ResponseUtil.parseByRequestResult(requestResult);
                    JSONObject requestResultObject = JSONObject.parseObject(JSON.toJSONString(expandDTO));
                    dto.setContent("result_" + JSON.toJSONString(requestResultObject));
                    WebSocketUtils.sendMessageSingle(dto);
                }
                LoggerUtil.debug("send. " + this.getName());
            }
        }
    }

    private void setVars(SampleResult result) {
        if (StringUtils.isNotEmpty(result.getSampleLabel()) && result.getSampleLabel().startsWith("Transaction=")) {
            for (int i = 0; i < result.getSubResults().length; i++) {
                SampleResult subResult = result.getSubResults()[i];
                this.setVars(subResult);
            }
        }
        JMeterVariables variables = JMeterVars.get(result.getResourceId());
        if (variables != null && CollectionUtils.isNotEmpty(variables.entrySet())) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                builder.append(entry.getKey()).append("：").append(entry.getValue()).append("\n");
            }
            if (StringUtils.isNotEmpty(builder)) {
                result.setExtVars(builder.toString());
            }
        }
    }

    @Override
    public void clearData() {
    }
}
