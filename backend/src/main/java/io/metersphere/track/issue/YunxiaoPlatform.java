package io.metersphere.track.issue;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.metersphere.base.domain.IssuesDao;
import io.metersphere.base.domain.IssuesWithBLOBs;
import io.metersphere.base.domain.Project;
import io.metersphere.commons.constants.IssuesManagePlatform;
import io.metersphere.commons.constants.IssuesStatus;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.BeanUtils;
import io.metersphere.commons.utils.CommonBeanFactory;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.dto.UserDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.service.SystemParameterService;
import io.metersphere.track.dto.DemandDTO;
import io.metersphere.track.dto.PlatformStatusDTO;
import io.metersphere.track.issue.client.YunxiaoClient;
import io.metersphere.track.issue.domain.PlatformUser;
import io.metersphere.track.issue.domain.tapd.TapdBug;
import io.metersphere.track.issue.domain.tapd.TapdGetIssueResponse;
import io.metersphere.track.issue.domain.yunxiao.YunxiaoConfig;
import io.metersphere.track.request.testcase.IssuesRequest;
import io.metersphere.track.request.testcase.IssuesUpdateRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class YunxiaoPlatform extends AbstractIssuePlatform {

    protected YunxiaoClient yunxiaoClient;

    public YunxiaoPlatform(IssuesRequest issueRequest) {
        super(issueRequest);
        this.key = IssuesManagePlatform.Yunxiao.name();
        yunxiaoClient = new YunxiaoClient();
        setConfig();
    }

    @Override
    public List<IssuesDao> getIssue(IssuesRequest issuesRequest) {
        issuesRequest.setPlatform(key);
        List<IssuesDao> issues;
        if (StringUtils.isNotBlank(issuesRequest.getProjectId())) {
            issues = extIssuesMapper.getIssues(issuesRequest);
        } else {
            issues = extIssuesMapper.getIssuesByCaseId(issuesRequest);
        }
        for (IssuesDao issue : issues) {
            if(StringUtils.isNotBlank(issue.getPlatform())&&issue.getPlatform().equals("Yunxiao")){
                List<String> tapdUsers = getTapdUsers(issue.getProjectId(), issue.getPlatformId());
                issue.setTapdUsers(tapdUsers);
            }
        }
        return issues;
    }

    public List<String> getTapdUsers(String projectId,String num){
        List<String>ids = new ArrayList<>(1);
        ids.add(num);
        List<JSONObject> tapdIssues = yunxiaoClient.getIssueForPageByIds(getProjectId(projectId),1,50,ids).getData();
        List<String>tapdUsers = new ArrayList<>(tapdIssues.size());
        for (JSONObject tapdIssue : tapdIssues) {
            JSONObject bug = tapdIssue.getJSONObject("Bug");
            String currentOwner = bug.getString("current_owner");
            tapdUsers.add(currentOwner);
        }
        return tapdUsers;
    }

    @Override
    public List<DemandDTO> getDemandList(String projectId,String keyWord) {
        List<DemandDTO> demandList = new ArrayList<>();
        YunxiaoConfig config = getConfig();
        Project project = projectService.getProjectById(projectId);
        JSONArray demands = yunxiaoClient.getDemands(getProjectId(projectId),keyWord);
        for (int i = 0; i < demands.size(); i++) {
            JSONObject o = demands.getJSONObject(i);
            DemandDTO demandDTO = new DemandDTO();
            //获取id
            Integer id = (Integer) o.get("id");
            //获取需求名subject
            String subject = (String) o.get("subject");
            demandDTO.setId(id.toString());
            demandDTO.setName(subject);
            demandDTO.setPlatform(key);
            //天玑接口地址http://phecda.cicc.group/req?from=ak&akProjectId=690136#openTaskId=41415
            demandDTO.setHref(config.getUrl()+"?from=ak&akProjectId="+project.getYunxiaoKey()+"#openTaskId="+id);
            demandList.add(demandDTO);
        }
        return demandList;
    }

    @Override
    public IssuesWithBLOBs addIssue(IssuesUpdateRequest issuesRequest) {

        MultiValueMap<String, Object> param = buildUpdateParam(issuesRequest);
        TapdBug bug = yunxiaoClient.addIssue(param);
        Map<String, String> statusMap = yunxiaoClient.getStatusMap(getProjectId(this.projectId));
        issuesRequest.setPlatformStatus(statusMap.get(bug.getStatus()));

        issuesRequest.setPlatformId(bug.getId());
        issuesRequest.setPlatformStatus(bug.getStatus());
        issuesRequest.setId(UUID.randomUUID().toString());

        // 插入缺陷表
        IssuesWithBLOBs issues = insertIssues(issuesRequest);

        // 用例与第三方缺陷平台中的缺陷关联
        handleTestCaseIssues(issuesRequest);

        return issues;
    }

    @Override
    public void updateIssue(IssuesUpdateRequest request) {
        MultiValueMap<String, Object> param = buildUpdateParam(request);
        param.add("id", request.getPlatformId());
        handleIssueUpdate(request);
        yunxiaoClient.updateIssue(param);
    }

    private MultiValueMap<String, Object> buildUpdateParam(IssuesUpdateRequest issuesRequest) {
        issuesRequest.setPlatform(key);

        String tapdId = getProjectId(issuesRequest.getProjectId());

        if (StringUtils.isBlank(tapdId)) {
            MSException.throwException("未关联天玑 项目ID");
        }

        String usersStr = "";
        List<String> platformUsers = issuesRequest.getTapdUsers();
        if (CollectionUtils.isNotEmpty(platformUsers)) {
            usersStr = String.join(";", platformUsers);
        }

        String reporter = getReporter();
        if (StringUtils.isBlank(reporter)) {
            reporter = SessionUtils.getUser().getName();
        }

        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("title", issuesRequest.getTitle());
        paramMap.add("workspace_id", tapdId);
        paramMap.add("description", msDescription2Tapd(issuesRequest.getDescription()));
        paramMap.add("current_owner", usersStr);
        if (issuesRequest.getTransitions() != null) {
            paramMap.add("status", issuesRequest.getTransitions().getValue());
        }

        addCustomFields(issuesRequest, paramMap);

        paramMap.add("reporter", reporter);
        return paramMap;
    }

    private String msDescription2Tapd(String msDescription) {
        SystemParameterService parameterService = CommonBeanFactory.getBean(SystemParameterService.class);
        msDescription = msImg2HtmlImg(msDescription, parameterService.getValue("base.url"));
        return msDescription.replaceAll("\\n", "<br/>");
    }

    @Override
    public void deleteIssue(String id) {
        super.deleteIssue(id);
        // todo 暂无删除API
    }

    @Override
    public void testAuth() {
        yunxiaoClient.auth();
    }

    @Override
    public void userAuth(UserDTO.PlatformInfo userInfo) {
        testAuth();
    }

    @Override
    public List<PlatformUser> getPlatformUser() {
        Boolean exist = checkProjectExist(getProjectId(projectId));
        if (!exist) {
            MSException.throwException(Translator.get("tapd_project_not_exist"));
        }
        List<PlatformUser> users = new ArrayList<>();
        JSONArray res = yunxiaoClient.getPlatformUser(getProjectId(projectId));
        for (int i = 0; i < res.size(); i++) {
            JSONObject o = res.getJSONObject(i);
            PlatformUser user = o.getObject("UserWorkspace", PlatformUser.class);
            users.add(user);
        }
        return users;
    }

    @Override
    public void syncIssues(Project project, List<IssuesDao> tapdIssues) {
        Map<String, String> idMap = tapdIssues.stream()
                .collect(Collectors.toMap(IssuesDao::getPlatformId, IssuesDao::getId));

        List<String> ids = tapdIssues.stream()
                .map(IssuesDao::getPlatformId)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(ids)) return;

        Map<String, String> statusMap = yunxiaoClient.getStatusMap(project.getTapdId());

        int index = 0;
        int limit = 50;

        while (index < ids.size()) {
            List<String> subIds = ids.subList(index, (index + limit) > ids.size() ? ids.size() : (index + limit));
            TapdGetIssueResponse result = yunxiaoClient.getIssueForPageByIds(project.getTapdId(), 1, limit, subIds);
            List<JSONObject> datas = result.getData();
            datas.forEach(issue -> {
                JSONObject bug = issue.getJSONObject("Bug");
                String platformId = bug.getString("id");
                String id = idMap.get(platformId);
                IssuesWithBLOBs updateIssue = getUpdateIssue(issuesMapper.selectByPrimaryKey(id), bug, statusMap);
                updateIssue.setId(id);
                updateIssue.setCustomFields(syncIssueCustomField(updateIssue.getCustomFields(), bug));
                issuesMapper.updateByPrimaryKeySelective(updateIssue);
                ids.remove(platformId);
            });
            index += limit;
        }
        // 查不到的设置为删除
        ids.forEach((id) -> {
            if (StringUtils.isNotBlank(idMap.get(id))) {
                IssuesDao issuesDao = new IssuesDao();
                issuesDao.setId(idMap.get(id));
                issuesDao.setPlatformStatus(IssuesStatus.DELETE.toString());
                issuesMapper.updateByPrimaryKeySelective(issuesDao);
            }
        });
    }

    protected IssuesWithBLOBs getUpdateIssue(IssuesWithBLOBs issue, JSONObject bug, Map<String, String> statusMap) {
        if (issue == null) {
            issue = new IssuesWithBLOBs();
            issue.setCustomFields(defaultCustomFields);
        } else {
            mergeCustomField(issue, defaultCustomFields);
        }
        TapdBug bugObj = JSONObject.parseObject(bug.toJSONString(), TapdBug.class);
        BeanUtils.copyBean(issue, bugObj);
        issue.setPlatformStatus(bugObj.getStatus());
        issue.setDescription(htmlDesc2MsDesc(issue.getDescription()));
        issue.setCustomFields(syncIssueCustomField(issue.getCustomFields(), bug));
        issue.setPlatform(key);
        issue.setCreateTime(bug.getLong("created"));
        issue.setUpdateTime(bug.getLong("modified"));
        return issue;
    }

    @Override
    public String getProjectId(String projectId) {
        return getProjectId(projectId, Project::getYunxiaoKey);
    }

    public YunxiaoConfig getConfig() {
        return getConfig(key, YunxiaoConfig.class);
    }

    public YunxiaoConfig setConfig() {
        YunxiaoConfig config = getConfig();
        yunxiaoClient.setConfig(config);
        return config;
    }

    public String getReporter() {
        UserDTO.PlatformInfo userPlatInfo = getUserPlatInfo(this.workspaceId);
        if (userPlatInfo != null && StringUtils.isNotBlank(userPlatInfo.getTapdUserName())) {
            return userPlatInfo.getTapdUserName();
        }
        return null;
    }

    @Override
    public Boolean checkProjectExist(String relateId) {
        try {
            return yunxiaoClient.checkProjectExist(relateId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<PlatformStatusDTO> getTransitions(String issueKey) {
        List<PlatformStatusDTO> platformStatusDTOS = new ArrayList<>();
        Project project = projectService.getProjectById(this.projectId);

        // 获取缺陷状态数据
        Map<String, String> statusMap = yunxiaoClient.getStatusMap(project.getTapdId());
        for (String key : statusMap.keySet()) {
            PlatformStatusDTO platformStatusDTO = new PlatformStatusDTO();
            platformStatusDTO.setValue(key);
            platformStatusDTO.setLable(statusMap.get(key));
            platformStatusDTOS.add(platformStatusDTO);
        }

        return platformStatusDTOS;
    }
}
