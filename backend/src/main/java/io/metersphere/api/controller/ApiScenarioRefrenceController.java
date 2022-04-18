package io.metersphere.api.controller;

import io.metersphere.api.service.ScenarioRefrenceService;
import io.metersphere.log.annotation.MsAuditLog;
import io.metersphere.notice.annotation.SendNotice;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/scenarioRefrence")
public class ApiScenarioRefrenceController {

    @Resource
    private ScenarioRefrenceService scenarioRefrenceService;

    /**
     * 修正api_defenition_refrence_id表数据
     * @param request
     * @return
     */
    @PostMapping("/updateRefrenceByScenario")
    public void updateRefrenceByScenario() {
        scenarioRefrenceService.updateRefrenceByScenario();
    }

}

