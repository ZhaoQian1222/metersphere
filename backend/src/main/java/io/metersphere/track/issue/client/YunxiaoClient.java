package io.metersphere.track.issue.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.track.issue.domain.yunxiao.YunxiaoConfig;
import io.metersphere.track.issue.domain.tapd.AddTapdIssueResponse;
import io.metersphere.track.issue.domain.tapd.TapdBug;
import io.metersphere.track.issue.domain.tapd.TapdGetIssueResponse;
import io.metersphere.track.issue.domain.yunxiao.YunxiaoGetProjectResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class YunxiaoClient extends BaseClient {

    private String ENDPOINT;

    private String INTERFACEURL;

    protected String USER_NAME;

    protected String PASSWD;

    public TapdGetIssueResponse getIssueForPage(String projectId, int pageNum, int limit) {
        return getIssueForPageByIds(projectId, pageNum, limit, null);
    }

    public Map<String, String> getStatusMap(String projectId) {
        String url = getBaseUrl() + "/workflows/status_map?workspace_id={1}&system=bug";
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, null, String.class, projectId);
        } catch (Exception e) {
            MSException.throwException("请检查配置信息是否填写正确！");
            LogUtil.error(e);
        }
        String resultForObject = (String) getResultForObject(String.class, response);
        JSONObject jsonObject = JSONObject.parseObject(resultForObject);
        String data = jsonObject.getString("data");
        return JSONObject.parseObject(data, Map.class);
    }

    public JSONArray getPlatformUser(String projectId) {
        String url = getBaseUrl() + "/workspaces/users?workspace_id=" + projectId;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class, projectId);
        return JSONArray.parseObject(response.getBody()).getJSONArray("data");
    }

    public void auth() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.exchange("https://localhost:8443/mock/100002/anonymous/hello?username=admin&password=888", HttpMethod.GET, null, String.class);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            MSException.throwException("验证失败: " + e.getMessage());
        }
    }

    public TapdGetIssueResponse getIssueForPageByIds(String projectId, int pageNum, int limit, List<String> ids) {
        String url = getBaseUrl() + "/bugs?workspace_id={1}&page={2}&limit={3}";
        StringBuilder idStr = new StringBuilder();
        if (!CollectionUtils.isEmpty(ids)) {
            ids.forEach(item -> {
                idStr.append(item + ",");
            });
            url += "&id={4}";
        }
        LogUtil.info("ids: " + idStr);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class,
                projectId, pageNum, limit, idStr);
        return (TapdGetIssueResponse) getResultForObject(TapdGetIssueResponse.class, response);
    }

    public JSONArray getDemands(String projectId, String keyWord) {
        JSONArray objects = new JSONArray();
        try {
            if (keyWord==null || keyWord.equals("") ) {
                //本地测试链接
//                String url = getBaseUrl() + "/hello?akProjectId={1}&stamp=Req&perPage=50&page=1";
            String url = getBaseUrl() + "/SearchIssue?akProjectId={1}&stamp=Req&perPage=50&page=1";
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class, projectId);
                objects.addAll(JSONArray.parseObject(response.getBody()).getJSONArray("result"));
            } else {
                String  regex= "^\\d{3,6}$";
                String url = "";
                boolean matches = keyWord.matches(regex);
                if(matches){
                    //本地测试链接
//                    url = getBaseUrl() + "/hello?akProjectId={1}&stamp=Req&perPage=200&page=1&idList={2}";
                    url = getBaseUrl() + "/SearchIssue?akProjectId={1}&stamp=Req&perPage=200&page=1&idList={2}";
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class, projectId , keyWord);
                    objects.addAll(JSONArray.parseObject(response.getBody()).getJSONArray("result"));
                    //总页数
                    Integer totalPages = (Integer) JSONObject.parseObject(response.getBody()).get("totalPages");
                    if (totalPages > 1) {
                        for (int i = 2; i <= totalPages; i++) {
                            //本地测试链接
//                          url = getBaseUrl() + "/hello?akProjectId={1}&idList={2}&stamp=Req&perPage=200&page=" + i;
                            url = getBaseUrl() + "/SearchIssue?akProjectId={1}&idList={2}&stamp=Req&perPage=200&page="+i;
                            response = restTemplate.exchange(url, HttpMethod.GET, null, String.class, projectId,keyWord);
                            objects.addAll(JSONArray.parseObject(response.getBody()).getJSONArray("result"));
                        }
                    }
                }else{
                    //本地测试链接
//                    url = getBaseUrl() + "/hello?akProjectId={1}&stamp=Req&perPage=200&page=1&subject={2}";
                    url = getBaseUrl() + "/SearchIssue?akProjectId={1}&stamp=Req&perPage=200&page=1&subject={2}";
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class, projectId , keyWord);
                    objects.addAll(JSONArray.parseObject(response.getBody()).getJSONArray("result"));
                    //总页数
                    Integer totalPages = (Integer) JSONObject.parseObject(response.getBody()).get("totalPages");
                    if (totalPages > 1) {
                        for (int i = 2; i <= totalPages; i++) {
                            //本地测试链接
//                            url = getBaseUrl() + "/hello?akProjectId={1}&subject={2}&stamp=Req&perPage=200&page=" + i;
                            url = getBaseUrl() + "/SearchIssue?akProjectId={1}&subject={2}&stamp=Req&perPage=200&page="+i;
                            response = restTemplate.exchange(url, HttpMethod.GET, null, String.class, projectId,keyWord);
                            objects.addAll(JSONArray.parseObject(response.getBody()).getJSONArray("result"));
                        }
                    }
                }

            }
        } catch (Exception e) {
            LogUtil.error("云效接口返回体解析报错" + e.getMessage());
        }
        return objects;
    }

    public TapdBug addIssue(MultiValueMap<String, Object> paramMap) {
        String url = getBaseUrl() + "/bugs";
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(paramMap, null);
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            MSException.throwException(e.getMessage());
        }
        return ((AddTapdIssueResponse) getResultForObject(AddTapdIssueResponse.class, response)).getData().getBug();
    }

    public TapdBug updateIssue(MultiValueMap<String, Object> paramMap) {
        // 带id为更新
        return addIssue(paramMap);
    }

//    protected HttpEntity<MultiValueMap> getAuthHttpEntity() {
//        return new HttpEntity<>(getAuthHeader());
//    }

//    protected HttpHeaders getAuthHeader() {
//        return getBasicHttpHeaders(USER_NAME, PASSWD);
//    }

    protected String getBaseUrl() {
        return INTERFACEURL;
    }

    public void setConfig(YunxiaoConfig config) {
        if (config == null) {
            MSException.throwException("config is null");
        }
        USER_NAME = config.getAccount();
        PASSWD = config.getPassword();
        ENDPOINT = config.getUrl();
        INTERFACEURL = config.getInterfaceUrl();
    }

    public boolean checkProjectExist(String relateId) {
        //本地测试链接
//        String url = getBaseUrl() + "/hello?projectId={1}";
        String url = getBaseUrl() + "/GetProjectInfo?projectId={1}";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class, relateId);
        YunxiaoGetProjectResponse res = (YunxiaoGetProjectResponse) getResultForObject(YunxiaoGetProjectResponse.class, response);
        return res == null || res.getSuccess() == true;
    }
}
