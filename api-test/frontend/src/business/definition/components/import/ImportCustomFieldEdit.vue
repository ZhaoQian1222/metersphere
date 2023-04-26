<template>
  <ms-edit-dialog
    width="30%"
    :visible.sync="visible"
    @confirm="save"
    :title="title"
    append-to-body
    ref="msEditDialog">
      <el-form  :model="customFieldForm" :rules="customFieldRules" ref="customFieldForm" class="api-form">
        <custom-filed-form-item :form="customFieldForm" :form-label-width="labelWidth"
                                :issue-template="apiTemplate" :col-span="24" :col-num="1"/>
      </el-form>
  </ms-edit-dialog>
</template>

<script>
import MsEditDialog from '@/business/commons/MsEditDialog';
import { getApiTemplate } from '@/api/api-template';
import {buildCustomFields, parseCustomField} from "metersphere-frontend/src/utils/custom_field";
import CustomFiledFormItem from "metersphere-frontend/src/components/form/CustomFiledFormItem";
export default {
  name: "ImportCustomFieldEdit",
  components: {MsEditDialog, CustomFiledFormItem},
  props: {
    scene: String,
    labelWidth: {
      Object: String,
      default() {
        return '100px';
      }
    },
    projectId: String,
    apiCustomFieldForm: [],
  },
  data() {
    return {
      customFieldForm:{},
      customFieldRules: {},
      apiTemplate:{},
      form: {},
      visible: false,
      title: this.$t('custom_field.name')
    };
  },
  computed: {

  },
  created() {

  },
  methods: {
    open() {
      this.visible = true;
      getApiTemplate(this.projectId)
        .then((template) => {
          this.apiTemplate = template;
          //设置自定义熟悉默认值
          this.customFieldForm = parseCustomField({fields: this.apiCustomFieldForm}, this.apiTemplate, this.customFieldRules,null);
        });
    },
    save() {
      this.$refs['customFieldForm'].validate((valid) => {
        if (valid){
          let param = {customFields: ''};
          buildCustomFields(this.form, param, this.apiTemplate);
          param.requestFields.forEach(field => {
            field.fieldId = field.id;
          });
          this.$emit('saveCustomFields', JSON.stringify(param.requestFields));
          this.visible = false;
        }
      });
    }
  }
};
</script>

<style scoped>

</style>
