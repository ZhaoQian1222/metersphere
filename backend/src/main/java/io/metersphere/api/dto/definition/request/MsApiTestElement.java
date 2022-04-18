package io.metersphere.api.dto.definition.request;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.metersphere.plugin.core.MsParameter;
import io.metersphere.plugin.core.MsTestElement;
import io.metersphere.plugin.core.utils.LogUtil;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "clazzName")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class MsApiTestElement{
    private String type;
    private String clazzName = "io.metersphere.plugin.core.MsApiTestElement";
    @JSONField(ordinal = 1)
    private String id;
    @JSONField(ordinal = 2)
    private String name;
    @JSONField(ordinal = 3)
    private String label;
    @JSONField(ordinal = 4)
    private String resourceId;
    @JSONField(ordinal = 5)
    private String referenced;
    @JSONField(ordinal = 6)
    private boolean active;
    @JSONField(ordinal = 7)
    private String index;
    @JSONField(ordinal = 8)
    private boolean enable = true;
    @JSONField(ordinal = 9)
    private String refType;
    @JSONField(ordinal = 10)
    private JSONArray hashTree;
    @JSONField(ordinal = 12)
    private String projectId;
    @JSONField(ordinal = 13)
    private boolean isMockEnvironment;
    @JSONField(ordinal = 14)
    private String environmentId;
    @JSONField(ordinal = 15)
    private String pluginId;
    @JSONField(ordinal = 16)
    private String stepName;
    @JSONField(ordinal = 17)
    private String path;
    @JSONField(ordinal = 18)
    private String method;

    private MsApiTestElement parent;
}





