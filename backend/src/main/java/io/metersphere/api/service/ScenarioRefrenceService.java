package io.metersphere.api.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.StringUtils;
import io.metersphere.api.dto.definition.request.MsApiScenario;
import io.metersphere.api.dto.definition.request.MsApiTestElement;
import io.metersphere.api.dto.definition.request.MsScenario;
import io.metersphere.api.dto.definition.request.sampler.MsTCPSampler;
import io.metersphere.base.domain.*;
import io.metersphere.base.mapper.ApiDefinitionMapper;
import io.metersphere.base.mapper.ApiScenarioMapper;
import io.metersphere.base.mapper.ApiScenarioReferenceIdMapper;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.plugin.core.MsTestElement;
import io.metersphere.utils.LoggerUtil;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ScenarioRefrenceService {
    @Resource
    private ApiScenarioMapper apiScenarioMapper;
    @Resource
    private ApiDefinitionMapper apiDefinitionMapper;
    @Resource
    private ApiScenarioReferenceIdMapper apiScenarioReferenceIdMapper;

    public void updateRefrenceByScenario() {
        // 查询全量的有效场景列表数据
        ApiScenarioExample scenarioExample = new ApiScenarioExample();
        scenarioExample.createCriteria().andStatusNotEqualTo("Trash").andStepTotalGreaterThan(0);
        List<ApiScenarioWithBLOBs> apiScenarioWithBLOBs = apiScenarioMapper.selectByExampleWithBLOBs(scenarioExample);

        // 遍历场景列表 解析场景步骤
        if (!CollectionUtils.isEmpty(apiScenarioWithBLOBs)) {
            for (int i = 0; i < apiScenarioWithBLOBs.size(); i++) {
                ApiScenarioWithBLOBs apiScenario = apiScenarioWithBLOBs.get(i);
                MsApiScenario scenario = JSONObject.parseObject(apiScenario.getScenarioDefinition(), MsApiScenario.class);
                if (scenario.getHashTree() != null && scenario.getHashTree().size() > 0) {
                    JSONArray jsonArray = scenario.getHashTree();
                    for (int j = 0; j < jsonArray.size(); j++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(j);
                        this.checkTestElement(jsonObject, apiScenario.getId());
                    }
                }
            }
        }
    }

    public void checkTestElement(JSONObject testElement, String apiScenarioId) {
        try {
            if (testElement != null) {
                // 递归判断 场景步骤 中包含的API
                if (Objects.equals(testElement.getString("refType"), "API") && Objects.equals(testElement.getString("referenced"), "Copy")) {
                    String path = testElement.getString("path");
                    String method = testElement.getString("method");
                    if (StringUtils.isNotBlank(path) && StringUtils.isNotBlank(method)) {
                        // 根据 API 的 path 查询接口数据的最新版本(api_definition)
                        ApiDefinitionExample definitionExample = new ApiDefinitionExample();
                        definitionExample.createCriteria().andMethodEqualTo(method).andPathEqualTo(path)
                                .andLatestEqualTo(true);
                        List<ApiDefinition> apiDefinitions = apiDefinitionMapper.selectByExample(definitionExample);
                        if (!CollectionUtils.isEmpty(apiDefinitions)) {
                            // 若能找到接口数据，且场景与接口的引用关联表中不存在此关联关系，则建立场景与接口的关联关系
                            ApiDefinition apiDefinition = apiDefinitions.get(0);
                            ApiScenarioReferenceIdExample referenceIdExample = new ApiScenarioReferenceIdExample();
                            referenceIdExample.createCriteria().andReferenceIdEqualTo(apiDefinition.getId())
                                    .andDataTypeEqualTo("API")
                                    .andApiScenarioIdEqualTo(apiScenarioId);
                            if (apiScenarioReferenceIdMapper.countByExample(referenceIdExample) < 1) {
                                ApiScenarioReferenceId saveItem = new ApiScenarioReferenceId();
                                saveItem.setId(UUID.randomUUID().toString());
                                saveItem.setApiScenarioId(apiScenarioId);
                                saveItem.setReferenceId(apiDefinition.getId());
                                saveItem.setReferenceType("Copy");
                                saveItem.setDataType("API");
                                saveItem.setCreateTime(System.currentTimeMillis());
                                saveItem.setCreateUserId("System");
                                apiScenarioReferenceIdMapper.insert(saveItem);
                            }
                        }
                    }
                }
                if (testElement.getJSONArray("hashTree") != null && testElement.getJSONArray("hashTree").size() > 0) {
                    JSONArray jsonArray = testElement.getJSONArray("hashTree");
                    for (int i = 0; i < jsonArray.size(); i++) {
                        this.checkTestElement(jsonArray.getJSONObject(i), apiScenarioId);
                    }
                }
            }

        } catch (Exception e) {
            LogUtil.error(e);
        }
    }

}
