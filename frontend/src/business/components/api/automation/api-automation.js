import {getUUID, hasLicense} from "@/common/js/utils";
import {getUploadConfig, request} from "@/common/js/ajax";
import {basePost} from "@/network/base-network";
import {ELEMENT_TYPE} from "@/business/components/api/automation/scenario/Setting";

function buildBodyFile(item, bodyUploadFiles, obj, bodyParam) {
  if (bodyParam) {
    bodyParam.forEach(param => {
      if (param.files) {
        param.files.forEach(fileItem => {
          if (fileItem.file) {
            fileItem.name = fileItem.file.name;
            obj.bodyFileRequestIds.push(item.id);
            bodyUploadFiles.push(fileItem.file);
          }
        });
      }
    });
  }
}

function setFiles(item, bodyUploadFiles, obj) {
  if (item.body) {
    buildBodyFile(item, bodyUploadFiles, obj, item.body.kvs);
    buildBodyFile(item, bodyUploadFiles, obj, item.body.binary);
  }
}

function recursiveFile(arr, bodyUploadFiles, obj) {
  arr.forEach(item => {
    setFiles(item, bodyUploadFiles, obj);
    if (item.hashTree !== undefined && item.hashTree.length > 0) {
      recursiveFile(item.hashTree, bodyUploadFiles, obj);
    }
  });
}

export function getBodyUploadFiles(obj, scenarioDefinition) {
  let bodyUploadFiles = [];
  obj.bodyFileRequestIds = [];
  scenarioDefinition.forEach(item => {
    setFiles(item, bodyUploadFiles, obj);
    if (item.hashTree !== undefined && item.hashTree.length > 0) {
      recursiveFile(item.hashTree, bodyUploadFiles, obj);
    }
  })
  return bodyUploadFiles;
}

function getScenarioFiles(obj) {
  let scenarioFiles = [];
  obj.scenarioFileIds = [];
  // 场景变量csv 文件
  if (obj.variables) {
    obj.variables.forEach(param => {
      if (param.type === 'CSV' && param.files) {
        param.files.forEach(item => {
          if (item.file) {
            if (!item.id) {
              let fileId = getUUID().substring(0, 12);
              item.name = item.file.name;
              item.id = fileId;
            }
            obj.scenarioFileIds.push(item.id);
            scenarioFiles.push(item.file);
          }
        })
      }
    });
  }
  return scenarioFiles;
}

function getRepositoryFiles(obj) {
  // todo：组织repositoryFiles数据，将 this.editData.files 即 obj.variables.files的数据封装进去，后端只需要存数据即可，无需再次拉取git仓库
  let repositoryFiles = [];
  // 场景变量csv 文件
  if (obj.variables) {
    obj.variables.forEach(param => {
      if (param.type === 'CSV' && param.fileResource === 'repository') {
        param.files.forEach(item => {
          if (!item.id) {
            let fileId = getUUID().substring(0, 12);
            let repositoryFile = {
              repositoryId: param.repositoryId,
              repositoryBranch: param.repositoryBranch,
              repositoryFilePath: param.repositoryFilePath,
              fileId: fileId,
              commitId: ""
            };
            repositoryFiles.push(repositoryFile);
          } else {
            let repositoryFile = {
              repositoryId: param.repositoryId,
              repositoryBranch: param.repositoryBranch,
              repositoryFilePath: param.repositoryFilePath,
              fileId: item.id,
              commitId: item.commitId
            };
            repositoryFiles.push(repositoryFile);
          }
        });
      }
    });
  }
  return repositoryFiles;
}

export function saveScenario(url, scenario, scenarioDefinition, _this, success) {
  let bodyFiles = getBodyUploadFiles(scenario, scenarioDefinition);
  if (_this && _this.$store && _this.$store.state && _this.$store.state.pluginFiles && _this.$store.state.pluginFiles.length > 0) {
    _this.$store.state.pluginFiles.forEach(fileItem => {
      if (fileItem.file) {
        scenario.bodyFileRequestIds.push(fileItem.file.uid);
        bodyFiles.push(fileItem.file);
      }
    });
  }
  let scenarioFiles = getScenarioFiles(scenario);
  let formData = new FormData();
  if (bodyFiles) {
    bodyFiles.forEach(f => {
      formData.append("bodyFiles", f);
    })
  }
  if (scenarioFiles) {
    scenarioFiles.forEach(f => {
      formData.append("scenarioFiles", f);
    })
  }
  if (hasLicense()) {
    let repositoryFiles = getRepositoryFiles(scenario);
    formData.append('repositoryFiles', new Blob([JSON.stringify(repositoryFiles)], {type: "application/json"}));
  }
  formData.append('request', new Blob([JSON.stringify(scenario)], {type: "application/json"}));
  let axiosRequestConfig = getUploadConfig(url, formData);
  request(axiosRequestConfig, (response) => {
    if (success) {
      success(response);
    }
  }, error => {
    _this.$emit('errorRefresh', error);
  });
}

export function editApiScenarioCaseOrder(request, callback) {
  return basePost('/api/automation/edit/order', request, callback);
}

export function savePreciseEnvProjectIds(projectIds, envMap) {
  if (envMap != null && projectIds != null) {
    let keys = envMap.keys();
    for (let key of keys) {
      if (!projectIds.has(key)) {
        envMap.delete(key);
      }
    }
    for (let id of projectIds) {
      if (!envMap.get(id)) {
        envMap.set(id, "");
      }
    }
  }
}


export function scenarioSort(_this) {
  for (let i in _this.scenarioDefinition) {
    // 排序
    _this.scenarioDefinition[i].index = Number(i) + 1;
    // 设置循环控制
    if (_this.scenarioDefinition[i].type === ELEMENT_TYPE.LoopController && _this.scenarioDefinition[i].hashTree
      && _this.scenarioDefinition[i].hashTree.length > 1) {
      _this.scenarioDefinition[i].countController.proceed = true;
    }
    // 设置项目ID
    if (!_this.scenarioDefinition[i].projectId) {
      _this.scenarioDefinition[i].projectId = _this.projectId;
    }

    if (_this.scenarioDefinition[i].hashTree != undefined && _this.scenarioDefinition[i].hashTree.length > 0) {
      if (_this.hideTreeNode) {
        _this.hideTreeNode(_this.scenarioDefinition[i], _this.scenarioDefinition[i].hashTree);
      }
      recursiveSorting(_this, _this.scenarioDefinition[i].hashTree, _this.scenarioDefinition[i].projectId);
    }
    // 添加debug结果
    if (_this.debugResult && _this.debugResult.get(_this.scenarioDefinition[i].id + _this.scenarioDefinition[i].name)) {
      _this.scenarioDefinition[i].requestResult = _this.debugResult.get(_this.scenarioDefinition[i].id + _this.scenarioDefinition[i].name);
    }
  }
}

export function recursiveSorting(_this, arr, scenarioProjectId) {
  for (let i in arr) {
    arr[i].index = Number(i) + 1;
    if (arr[i].type === ELEMENT_TYPE.LoopController && arr[i].loopType === "LOOP_COUNT" && arr[i].hashTree && arr[i].hashTree.length > 1) {
      arr[i].countController.proceed = true;
    }
    if (!arr[i].projectId) {
      arr[i].projectId = scenarioProjectId ? scenarioProjectId : _this.projectId;
    }
    if (arr[i].hashTree != undefined && arr[i].hashTree.length > 0) {
      if (_this.hideTreeNode) {
        _this.hideTreeNode(arr[i], arr[i].hashTree);
      }
      recursiveSorting(arr[i].hashTree, arr[i].projectId);
    }
    // 添加debug结果
    if (_this.debugResult && _this.debugResult.get(arr[i].id + arr[i].name)) {
      arr[i].requestResult = _this.debugResult.get(arr[i].id + arr[i].name);
    }
  }
}


export function copyScenarioRow(row, node) {
  if (!row || !node) {
    return;
  }
  const parent = node.parent
  const hashTree = parent.data.hashTree || parent.data;
  // 深度复制
  let obj = JSON.parse(JSON.stringify(row));
  if (obj.hashTree && obj.hashTree.length > 0) {
    resetResourceId(obj.hashTree);
  }
  obj.resourceId = getUUID();
  if (obj.name) {
    obj.name = obj.name + '_copy';
  }
  const index = hashTree.findIndex(d => d.resourceId === row.resourceId);
  if (index != -1) {
    hashTree.splice(index + 1, 0, obj);
  } else {
    hashTree.push(obj);
  }
}

export function resetResourceId(hashTree) {
  hashTree.forEach(item => {
    item.resourceId = getUUID();
    if (item.hashTree && item.hashTree.length > 0) {
      resetResourceId(item.hashTree);
    }
  })
}
