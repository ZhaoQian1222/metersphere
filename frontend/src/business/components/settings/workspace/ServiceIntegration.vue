<template>
  <el-card>
    <el-tabs class="system-setting" v-model="activeName">
      <el-tab-pane :label="$t('organization.defect_manage')" name="defect">
        <bug-management/>
      </el-tab-pane>
      <el-tab-pane :label="$t('organization.git_repository')" name="repository" v-xpack>
        <workspace-repository/>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script>

  import BugManagement from "./BugManagement";
  import WorkspaceRepository from "@/business/components/xpack/repository/WorkspaceRepository";

  export default {
    name: "ServiceIntegration",
    components: {
      WorkspaceRepository,
      BugManagement
    },
    data() {
      return {
        activeName: 'defect'
      }
    }
  }
</script>

<style scoped>

</style>
