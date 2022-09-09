package io.metersphere.track.issue.domain.yunxiao;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class YunxiaoGetProjectResponse {

    private Boolean success;
    private JSONObject result;
    private String messages;
}
