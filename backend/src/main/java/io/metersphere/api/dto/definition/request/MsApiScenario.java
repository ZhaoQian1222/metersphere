package io.metersphere.api.dto.definition.request;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import io.metersphere.api.dto.definition.request.variable.ScenarioVariable;
import io.metersphere.api.dto.scenario.KeyValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@JSONType(typeName = "scenario")
public class MsApiScenario extends MsApiTestElement {

    private String type = "scenario";
    private String clazzName = MsApiScenario.class.getCanonicalName();

    @JSONField(ordinal = 21)
    private String referenced;

    @JSONField(ordinal = 22)
    private String environmentId;

    @JSONField(ordinal = 23)
    private List<ScenarioVariable> variables;

    @JSONField(ordinal = 24)
    private boolean enableCookieShare;

    @JSONField(ordinal = 26)
    private List<KeyValue> headers;

    @JSONField(ordinal = 27)
    private Map<String, String> environmentMap;

    @JSONField(ordinal = 28)
    private Boolean onSampleError;

    @JSONField(ordinal = 29)
    private boolean environmentEnable;

    @JSONField(ordinal = 30)
    private Boolean variableEnable;

}
