<template>
  <div v-loading="result.loading">
    <el-form :model="formInline" :rules="rules" ref="formInline" class="demo-form-inline"
             :disabled="show" v-loading="loading" size="small">
      <el-row>
        <el-col>
          <el-form-item :label="$t('sso.server.url')" prop="url">
            <el-input v-model="formInline.url" :placeholder="$t('sso.input_server_url_placeholder')" />
          </el-form-item>
          <el-form-item :label="$t('sso.client.id')" prop="clientId">
            <el-input v-model="formInline.clientId" :placeholder="$t('sso.input_client_id_placeholder')"/>
          </el-form-item>
          <el-form-item :label="$t('sso.secret.key')" prop="secretkey">
            <el-input v-model="formInline.secretKey" :placeholder="$t('sso.input_secret_key_placeholder')"/>
          </el-form-item>
          <el-form-item :label="$t('sso.secret.desKey')" prop="desKey">
            <el-input v-model="formInline.desKey" :placeholder="$t('sso.input_des_key_placeholder')"/>
          </el-form-item>
          <el-form-item :label="$t('sso.open')" prop="open">
            <el-checkbox v-model="formInline.open"/>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div>
      <el-button @click="edit" v-if="showEdit" size="small">{{ $t('commons.edit') }}</el-button>
      <el-button type="success" @click="save('formInline')" v-if="showSave" :disabled="disabledSave" size="small">
        {{ $t('commons.save') }}
      </el-button>
      <el-button @click="cancel" type="info" v-if="showCancel" size="small">{{ $t('commons.cancel') }}</el-button>
    </div>
  </div>
</template>

<script>

export default {
  name: "SSOSetting",
  data() {
    return {
      formInline: {open: false},
      input: '',
      visible: true,
      result: {},
      showEdit: true,
      showSave: false,
      showCancel: false,
      show: true,
      disabledConnection: false,
      disabledSave: false,
      loading: false,
      rules: {
        url: [
          {
            required: true,
            message: this.$t('sso.server.url_is_null'),
            trigger: ['change', 'blur']
          },
        ],
        clientId: [
          {
            required: true,
            message: this.$t('sso.client.id_is_null'),
            trigger: ['change', 'blur']
          },
        ],
        secretKey: [
          {
            required: true,
            message: this.$t('sso.secret.key_is_null'),
            trigger: ['change', 'blur']
          },
        ],
        desKey: [
          {
            required: true,
            message: this.$t('sso.secret.desKey_is_null'),
            trigger: ['change', 'blur']
          },
        ]
      }
    }
  },

  created() {
    this.query()
  },
  methods: {
    query() {
      this.result = this.$get("/system/sso/info", response => {
        this.formInline = response.data;
        this.formInline.open = this.formInline.open === 'true';
        this.$nextTick(() => {
          this.$refs.formInline.clearValidate();
        })
      })
    },
    edit() {
      this.showEdit = false;
      this.showSave = true;
      this.showCancel = true;
      this.show = false;
    },
    save(formInline) {
      this.showEdit = true;
      this.showCancel = false;
      this.showSave = false;
      this.show = true;
      let param = [
        {paramKey: "sso.url", paramValue: this.formInline.url, type: "text", sort: 1},
        {paramKey: "sso.clientId", paramValue: this.formInline.clientId, type: "text", sort: 2},
        {paramKey: "sso.secretKey", paramValue: this.formInline.secretKey, type: "text", sort: 3},
        {paramKey: "sso.desKey", paramValue: this.formInline.desKey, type: "text", sort: 4},
        {paramKey: "sso.open", paramValue: this.formInline.open, type: "text", sort: 4},
      ];

      this.$refs[formInline].validate(valid => {
        if (valid) {
          this.result = this.$post("/system/save/sso", param, response => {
            if (response.success) {
              this.$success(this.$t('commons.save_success'));
            } else {
              this.$message.error(this.$t('commons.save_failed'));
            }
          });
        } else {
          return false;
        }
      })
    },
    cancel() {
      this.showEdit = true;
      this.showCancel = false;
      this.showSave = false;
      this.show = true;
      this.query();
    }
  }
}
</script>

<style scoped>

  .el-form {
    min-height: 300px;
  }

</style>
