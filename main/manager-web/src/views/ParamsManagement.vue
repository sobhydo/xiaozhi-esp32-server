<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('paramManagement.pageTitle') }}</h1>
          <p class="page-description">{{ $t('paramManagement.totalRecords', { total: filteredTotal }) }}</p>
        </div>
        <div class="page-actions">
          <el-button icon="el-icon-refresh" @click="fetchParams">
            {{ $t('paramManagement.refresh') }}
          </el-button>
          <el-button type="primary" icon="el-icon-plus" @click="showAddDialog">
            {{ $t('paramManagement.add') }}
          </el-button>
        </div>
      </header>

      <section class="params-card">
        <div class="card-toolbar">
          <!-- Top-level categories -->
          <div class="group-tabs" role="tablist" :aria-label="$t('paramManagement.pageTitle')"
            v-loading="loading && !allParams.length">
            <button
              v-for="tab in categoryTabs"
              :key="tab.key"
              type="button"
              role="tab"
              class="group-tab"
              :class="{ active: activeCategory === tab.key }"
              :aria-selected="activeCategory === tab.key ? 'true' : 'false'"
              @click="selectCategory(tab.key)"
            >
              <span class="group-tab-label">{{ tab.label }}</span>
              <span class="group-tab-count">{{ tab.count }}</span>
            </button>
          </div>
          <div class="search-group">
            <el-input
              :placeholder="$t('paramManagement.searchPlaceholder')"
              :aria-label="$t('paramManagement.searchPlaceholder')"
              v-model="searchCode"
              class="search-input"
              prefix-icon="el-icon-search"
              @keyup.enter.native="handleSearch"
              @clear="handleSearch"
              clearable
            />
            <el-button class="btn-search" @click="handleSearch">
              {{ $t('paramManagement.search') }}
            </el-button>
          </div>
        </div>

        <!-- Second-level subcategories -->
        <div class="sub-tabs" v-if="subCategoryTabs.length > 1">
          <button
            v-for="tab in subCategoryTabs"
            :key="tab.key"
            type="button"
            class="sub-tab"
            :class="{ active: activeSubCategory === tab.key }"
            :aria-pressed="activeSubCategory === tab.key ? 'true' : 'false'"
            @click="selectSubCategory(tab.key)"
          >
            {{ tab.label }}
            <em>{{ tab.count }}</em>
          </button>
        </div>

        <div class="params-body" v-loading="loading">
          <template v-if="displaySections.length">
            <el-collapse v-model="activeCollapse" class="params-collapse">
              <el-collapse-item
                v-for="section in displaySections"
                :key="section.key"
                :name="section.key"
              >
                <template slot="title">
                  <div class="section-title">
                    <span class="section-name">{{ section.label }}</span>
                    <span class="section-count">{{ section.items.length }}</span>
                    <span v-if="section.hint" class="section-hint">{{ section.hint }}</span>
                  </div>
                </template>
                <div class="table-scroll">
                  <el-table
                    :data="section.items"
                    class="params-table"
                    size="small"
                    :header-cell-class-name="'params-table-header'"
                  >
                    <el-table-column width="72" align="center" :label="$t('paramManagement.select')">
                      <template slot-scope="scope">
                        <el-checkbox v-model="scope.row.selected" :aria-label="scope.row.paramCode"
                          @change="syncSelectAllState" />
                      </template>
                    </el-table-column>
                    <el-table-column
                      prop="paramCode"
                      :label="$t('paramManagement.paramCode')"
                      min-width="220"
                      show-overflow-tooltip
                    >
                      <template slot-scope="scope">
                        <code class="param-code">{{ scope.row.paramCode }}</code>
                      </template>
                    </el-table-column>
                    <el-table-column
                      :label="$t('paramManagement.valueType')"
                      width="90"
                    >
                      <template slot-scope="scope">
                        <span class="type-tag">{{ scope.row.valueType || 'string' }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column
                      :label="$t('paramManagement.paramValue')"
                      min-width="180"
                      show-overflow-tooltip
                    >
                      <template slot-scope="scope">
                        <div class="value-cell" v-if="isSensitiveParam(scope.row.paramCode)">
                          <span class="value-text">
                            {{ scope.row.showValue ? formatValue(scope.row.paramValue) : maskSensitiveValue(scope.row.paramValue) }}
                          </span>
                          <el-button size="mini" type="text" class="action-btn" @click="toggleSensitiveValue(scope.row)">
                            {{ scope.row.showValue ? $t('paramManagement.hide') : $t('paramManagement.view') }}
                          </el-button>
                        </div>
                        <span v-else class="value-text">{{ formatValue(scope.row.paramValue) }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column
                      prop="remark"
                      :label="$t('paramManagement.remark')"
                      min-width="200"
                      show-overflow-tooltip
                    />
                    <el-table-column
                      :label="$t('paramManagement.operation')"
                      width="130"
                      align="right"
                    >
                      <template slot-scope="scope">
                        <div class="row-actions">
                          <el-button size="mini" type="text" class="action-btn" @click="editParam(scope.row)">
                            {{ $t('paramManagement.edit') }}
                          </el-button>
                          <el-button size="mini" type="text" class="action-btn action-btn--danger"
                            @click="deleteParam(scope.row)">
                            {{ $t('paramManagement.delete') }}
                          </el-button>
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>
          <div v-else class="empty-state">
            {{ $t('paramManagement.emptyResult') }}
          </div>
        </div>

        <div class="table-footer">
          <div class="batch-actions">
            <CustomButton
              :icon="isAllSelected ? 'el-icon-circle-close' : 'el-icon-circle-check'"
              size="small"
              @click="handleSelectAll"
            >
              {{ isAllSelected ? $t('paramManagement.deselectAll') : $t('paramManagement.selectAll') }}
            </CustomButton>
            <CustomButton size="small" type="delete" icon="el-icon-delete" @click="deleteSelectedParams">
              {{ $t('paramManagement.delete') }}
            </CustomButton>
          </div>
          <div class="footer-meta">
            <span>{{ $t('paramManagement.selectedCount', { count: selectedCount }) }}</span>
            <span class="sep" aria-hidden="true">·</span>
            <span>{{ $t('paramManagement.groupHint') }}</span>
          </div>
        </div>
      </section>
    </main>

    <param-dialog
      ref="paramDialog"
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      :form="paramForm"
      @submit="handleSubmit"
      @cancel="dialogVisible = false"
    />
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import ParamDialog from "@/components/ParamDialog.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import CustomButton from "@/components/CustomButton.vue";

const TOP_GROUP_ORDER = ['server', 'plugins', 'log', 'session_state', 'general'];
const FETCH_LIMIT = 500;

const SERVER_SUB_ORDER = [
  '_root', 'auth', 'connection', 'registry', 'resilience', 'metrics', 'tracing'
];

export default {
  components: { HeaderBar, ParamDialog, VersionFooter, CustomButton },
  data() {
    return {
      searchCode: "",
      appliedSearch: "",
      allParams: [],
      loading: false,
      dialogVisible: false,
      dialogTitle: "",
      isAllSelected: false,
      activeCategory: 'all',
      activeSubCategory: 'all',
      activeCollapse: [],
      sensitive_keys: [
        "api_key", "personal_access_token", "access_token", "token", "secret",
        "access_key_secret", "secret_key", "password", "mqtt_signature_key", "private_key"
      ],
      paramForm: {
        id: null,
        paramCode: "",
        paramValue: "",
        valueType: "string",
        remark: ""
      }
    };
  },
  computed: {
    filteredParams() {
      const keyword = (this.appliedSearch || "").trim().toLowerCase();
      let list = this.allParams;

      if (this.activeCategory !== 'all') {
        list = list.filter(item => this.getTopGroup(item.paramCode) === this.activeCategory);
      }

      if (this.activeSubCategory !== 'all') {
        list = list.filter(item => this.getSubGroup(item.paramCode) === this.activeSubCategory);
      }

      if (keyword) {
        list = list.filter(item => {
          const code = (item.paramCode || "").toLowerCase();
          const remark = (item.remark || "").toLowerCase();
          return code.includes(keyword) || remark.includes(keyword);
        });
      }

      return list;
    },
    filteredTotal() {
      return this.filteredParams.length;
    },
    selectedCount() {
      return this.filteredParams.filter(item => item.selected).length;
    },
    categoryTabs() {
      const keyword = (this.appliedSearch || "").trim().toLowerCase();
      const searchFiltered = keyword
        ? this.allParams.filter(item => {
            const code = (item.paramCode || "").toLowerCase();
            const remark = (item.remark || "").toLowerCase();
            return code.includes(keyword) || remark.includes(keyword);
          })
        : this.allParams;

      const searchCounts = {
        all: searchFiltered.length,
        server: 0,
        plugins: 0,
        log: 0,
        session_state: 0,
        general: 0
      };
      searchFiltered.forEach(item => {
        const key = this.getTopGroup(item.paramCode);
        searchCounts[key] = (searchCounts[key] || 0) + 1;
      });

      return [
        { key: 'all', label: this.$t('paramManagement.group.all'), count: searchCounts.all },
        { key: 'server', label: this.$t('paramManagement.group.server'), count: searchCounts.server },
        { key: 'plugins', label: this.$t('paramManagement.group.plugins'), count: searchCounts.plugins },
        { key: 'log', label: this.$t('paramManagement.group.log'), count: searchCounts.log },
        { key: 'session_state', label: this.$t('paramManagement.group.session'), count: searchCounts.session_state },
        { key: 'general', label: this.$t('paramManagement.group.general'), count: searchCounts.general }
      ].filter(tab => tab.key === 'all' || searchCounts[tab.key] > 0);
    },
    subCategoryTabs() {
      const source = this.activeCategory === 'all'
        ? []
        : this.allParams.filter(item => this.getTopGroup(item.paramCode) === this.activeCategory);

      if (!source.length) return [];

      const keyword = (this.appliedSearch || "").trim().toLowerCase();
      const filtered = keyword
        ? source.filter(item => {
            const code = (item.paramCode || "").toLowerCase();
            const remark = (item.remark || "").toLowerCase();
            return code.includes(keyword) || remark.includes(keyword);
          })
        : source;

      const countMap = {};
      filtered.forEach(item => {
        const sub = this.getSubGroup(item.paramCode);
        countMap[sub] = (countMap[sub] || 0) + 1;
      });

      const keys = Object.keys(countMap);
      if (keys.length <= 1) return [];

      const ordered = this.orderSubKeys(keys, this.activeCategory);
      return [
        { key: 'all', label: this.$t('paramManagement.group.all'), count: filtered.length },
        ...ordered.map(key => ({
          key,
          label: this.getSubGroupLabel(this.activeCategory, key),
          count: countMap[key]
        }))
      ];
    },
    displaySections() {
      const items = this.filteredParams;
      if (!items.length) return [];

      // When a top-level and a second-level category (not all) are selected, show a single section
      if (this.activeCategory !== 'all' && this.activeSubCategory !== 'all') {
        return [{
          key: `${this.activeCategory}.${this.activeSubCategory}`,
          label: this.getSubGroupLabel(this.activeCategory, this.activeSubCategory),
          hint: this.getTopGroupLabel(this.activeCategory),
          items
        }];
      }

      // A top-level category is selected: group by second level
      if (this.activeCategory !== 'all') {
        return this.buildSections(items, true);
      }

      // All: group by top level → second level into flat sections (top·second)
      return this.buildSections(items, false);
    }
  },
  watch: {
    displaySections: {
      immediate: true,
      handler(sections) {
        const keys = sections.map(s => s.key);
        if (!keys.length) {
          this.activeCollapse = [];
          this.isAllSelected = false;
          return;
        }
        // Expand all groups when there are few; otherwise expand the first few by default
        const preferOpen = keys.length <= 5 ? keys.slice() : keys.slice(0, 3);
        const stillValid = (this.activeCollapse || []).filter(k => keys.includes(k));
        this.activeCollapse = stillValid.length ? stillValid : preferOpen;
        this.syncSelectAllState();
      }
    },
    categoryTabs(tabs) {
      if (!tabs.some(t => t.key === this.activeCategory)) {
        this.activeCategory = 'all';
        this.activeSubCategory = 'all';
      }
    }
  },
  created() {
    this.fetchParams();
  },
  methods: {
    getTopGroup(paramCode = '') {
      if (paramCode.startsWith('server.')) return 'server';
      if (paramCode.startsWith('plugins.')) return 'plugins';
      if (paramCode.startsWith('log.')) return 'log';
      if (paramCode.startsWith('session_state.')) return 'session_state';
      return 'general';
    },
    getSubGroup(paramCode = '') {
      const top = this.getTopGroup(paramCode);
      const parts = paramCode.split('.');
      if (top === 'server') {
        return parts.length >= 3 ? parts[1] : '_root';
      }
      if (top === 'plugins') {
        return parts.length >= 3 ? parts[1] : '_root';
      }
      if (top === 'log' || top === 'session_state') {
        return '_all';
      }
      return '_all';
    },
    getTopGroupLabel(key) {
      const map = {
        server: 'paramManagement.group.server',
        plugins: 'paramManagement.group.plugins',
        log: 'paramManagement.group.log',
        session_state: 'paramManagement.group.session',
        general: 'paramManagement.group.general'
      };
      return this.$t(map[key] || 'paramManagement.group.general');
    },
    getSubGroupLabel(top, sub) {
      if (sub === '_all') return this.getTopGroupLabel(top);
      if (sub === '_root') return this.$t('paramManagement.subgroup.root');

      const known = {
        'server.auth': 'paramManagement.subgroup.auth',
        'server.connection': 'paramManagement.subgroup.connection',
        'server.registry': 'paramManagement.subgroup.registry',
        'server.resilience': 'paramManagement.subgroup.resilience',
        'server.metrics': 'paramManagement.subgroup.metrics',
        'server.tracing': 'paramManagement.subgroup.tracing'
      };
      const i18nKey = known[`${top}.${sub}`];
      if (i18nKey) return this.$t(i18nKey);

      if (top === 'plugins') {
        return this.$t('paramManagement.subgroup.plugin', { name: sub });
      }
      return sub;
    },
    orderSubKeys(keys, top) {
      if (top === 'server') {
        return [
          ...SERVER_SUB_ORDER.filter(k => keys.includes(k)),
          ...keys.filter(k => !SERVER_SUB_ORDER.includes(k)).sort()
        ];
      }
      return keys.slice().sort((a, b) => {
        if (a === '_root' || a === '_all') return -1;
        if (b === '_root' || b === '_all') return 1;
        return a.localeCompare(b);
      });
    },
    buildSections(items, withinTopCategory) {
      const bucket = {};
      items.forEach(item => {
        const top = this.getTopGroup(item.paramCode);
        const sub = this.getSubGroup(item.paramCode);
        const key = withinTopCategory ? `${top}.${sub}` : `${top}.${sub}`;
        if (!bucket[key]) {
          bucket[key] = {
            key,
            top,
            sub,
            items: []
          };
        }
        bucket[key].items.push(item);
      });

      const sections = Object.values(bucket).map(section => {
        const label = withinTopCategory
          ? this.getSubGroupLabel(section.top, section.sub)
          : (section.sub === '_all' || section.sub === '_root'
            ? this.getTopGroupLabel(section.top)
            : `${this.getTopGroupLabel(section.top)} · ${this.getSubGroupLabel(section.top, section.sub)}`);
        return {
          key: section.key,
          label,
          hint: withinTopCategory ? null : null,
          items: section.items.slice().sort((a, b) => a.paramCode.localeCompare(b.paramCode)),
          _top: section.top,
          _sub: section.sub
        };
      });

      sections.sort((a, b) => {
        const topDiff = TOP_GROUP_ORDER.indexOf(a._top) - TOP_GROUP_ORDER.indexOf(b._top);
        if (topDiff !== 0) return topDiff;
        const orderA = this.orderSubKeys([a._sub, b._sub], a._top);
        return orderA.indexOf(a._sub) - orderA.indexOf(b._sub);
      });

      return sections;
    },
    selectCategory(key) {
      if (this.activeCategory === key) return;
      this.activeCategory = key;
      this.activeSubCategory = 'all';
      this.isAllSelected = false;
      this.allParams.forEach(row => { row.selected = false; });
    },
    selectSubCategory(key) {
      if (this.activeSubCategory === key) return;
      this.activeSubCategory = key;
      this.isAllSelected = false;
      this.filteredParams.forEach(row => { row.selected = false; });
      this.syncSelectAllState();
    },
    fetchParamsPage(page) {
      return new Promise((resolve, reject) => {
        Api.admin.getParamsList(
          {
            page,
            limit: FETCH_LIMIT,
            paramCode: ""
          },
          ({ data }) => {
            if (data.code === 0) {
              resolve(data.data || {});
            } else {
              reject(new Error(data.msg || this.$t('paramManagement.getParamsListFailed')));
            }
          }
        );
      });
    },
    async fetchParams() {
      this.loading = true;
      try {
        const collected = [];
        let page = 1;
        let total = Infinity;
        // Loop through all pages based on total so the grouped view isn't missing data beyond FETCH_LIMIT
        while (collected.length < total) {
          const pageData = await this.fetchParamsPage(page);
          const list = pageData.list || [];
          total = typeof pageData.total === 'number' ? pageData.total : collected.length + list.length;
          collected.push(...list);
          if (!list.length) break;
          page += 1;
          // Safeguard: avoid an infinite loop on an abnormal total or API error
          if (page > 200) break;
        }

        this.allParams = collected.map(item => ({
          ...item,
          valueType: item.valueType || "string",
          selected: false,
          showValue: false
        }));
        this.isAllSelected = false;

        // If the current category has no data, fall back to all
        const exists = this.categoryTabs.some(t => t.key === this.activeCategory);
        if (!exists) {
          this.activeCategory = 'all';
          this.activeSubCategory = 'all';
        }
      } catch (err) {
        this.$message.error({
          message: (err && err.message) || this.$t('paramManagement.getParamsListFailed'),
          showClose: true
        });
      } finally {
        this.loading = false;
      }
    },
    handleSearch() {
      this.appliedSearch = this.searchCode;
      this.activeSubCategory = 'all';
      this.$nextTick(() => {
        if (!this.categoryTabs.some(t => t.key === this.activeCategory)) {
          this.activeCategory = 'all';
        }
        this.syncSelectAllState();
      });
    },
    syncSelectAllState() {
      const list = this.filteredParams;
      this.isAllSelected = list.length > 0 && list.every(row => row.selected);
    },
    handleSelectAll() {
      this.isAllSelected = !this.isAllSelected;
      this.filteredParams.forEach(row => {
        row.selected = this.isAllSelected;
      });
    },
    formatValue(value) {
      if (value == null) return '';
      const text = String(value);
      return text.length > 120 ? `${text.slice(0, 120)}…` : text;
    },
    showAddDialog() {
      this.dialogTitle = this.$t('paramManagement.addParam');
      this.paramForm = {
        id: null,
        paramCode: "",
        paramValue: "",
        valueType: "string",
        remark: ""
      };
      this.dialogVisible = true;
    },
    editParam(row) {
      this.dialogTitle = this.$t('paramManagement.editParam');
      this.paramForm = {
        id: row.id,
        paramCode: row.paramCode,
        paramValue: row.paramValue,
        valueType: row.valueType || "string",
        remark: row.remark
      };
      this.dialogVisible = true;
    },
    handleSubmit(form) {
      if (form.id) {
        Api.admin.updateParam(form, ({ data }) => {
          this.dialogVisible = false;
          this.fetchParams();
          this.$message.success({
            message: this.$t('paramManagement.updateSuccess'),
            showClose: true
          });
        }, ({ data }) => {
          this.$message.error({
            message: data.msg || this.$t('paramManagement.updateFailed'),
            showClose: true
          });
          if (this.$refs.paramDialog && typeof this.$refs.paramDialog.resetSaving === 'function') {
            this.$refs.paramDialog.resetSaving();
          }
        });
      } else {
        Api.admin.addParam(form, ({ data }) => {
          if (data.code === 0) {
            this.dialogVisible = false;
            this.fetchParams();
            this.$message.success({
              message: this.$t('paramManagement.addSuccess'),
              showClose: true
            });
          } else {
            this.$message.error({
              message: data.msg || this.$t('paramManagement.addFailed'),
              showClose: true
            });
            if (this.$refs.paramDialog && typeof this.$refs.paramDialog.resetSaving === 'function') {
              this.$refs.paramDialog.resetSaving();
            }
          }
        }, ({ data }) => {
          this.$message.error({
            message: data.msg || this.$t('paramManagement.updateFailed'),
            showClose: true
          });
          if (this.$refs.paramDialog && typeof this.$refs.paramDialog.resetSaving === 'function') {
            this.$refs.paramDialog.resetSaving();
          }
        });
      }
    },
    deleteSelectedParams() {
      const selectedParams = this.filteredParams.filter(row => row.selected);
      if (selectedParams.length === 0) {
        this.$message.warning({
          message: this.$t('paramManagement.selectParamsFirst'),
          showClose: true
        });
        return;
      }
      this.deleteParams(selectedParams);
    },
    deleteParam(row) {
      if (!row.id) {
        this.$message.warning({
          message: this.$t('paramManagement.selectParamsFirst'),
          showClose: true
        });
        return;
      }
      this.deleteParams([row]);
    },
    deleteParams(params) {
      const paramCount = params.length;
      const paramIds = params.map(param => param.id).filter(id => id);
      if (paramIds.length === 0) {
        this.$message.error({
          message: this.$t('paramManagement.invalidParamId'),
          showClose: true
        });
        return;
      }
      this.$confirm(this.$t('paramManagement.confirmBatchDelete', { paramCount }), this.$t('message.warning'), {
        confirmButtonText: this.$t('button.ok'),
        cancelButtonText: this.$t('button.cancel'),
        type: 'warning'
      }).then(() => {
        Api.admin.deleteParam(paramIds, ({ data }) => {
          if (data.code === 0) {
            this.fetchParams();
            this.$message.success({
              message: this.$t('paramManagement.batchDeleteSuccess', { paramCount }),
              showClose: true
            });
          } else {
            this.$message.error({
              message: data.msg || this.$t('paramManagement.deleteFailed'),
              showClose: true
            });
          }
        });
      }).catch(() => {
        this.$message({
          type: 'info',
          message: this.$t('paramManagement.operationCancelled'),
          duration: 1000
        });
      });
    },
    isSensitiveParam(paramCode) {
      return this.sensitive_keys.some(key => paramCode.toLowerCase().includes(key));
    },
    maskSensitiveValue(value) {
      if (!value || value.length <= 4) {
        return '****';
      }
      return value.substring(0, 2) + '****' + value.substring(value.length - 2);
    },
    toggleSensitiveValue(row) {
      row.showValue = !row.showValue;
    }
  }
};
</script>

<style lang="scss" scoped>
.welcome {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
  text-align: start;
}

.page {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  padding: 32px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-heading {
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.3;
  color: var(--x-text);
}

.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.params-card {
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.card-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--x-border);
}

.group-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-width: 0;
}

.group-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 6px 12px;
  border: 0;
  border-radius: var(--x-radius-control);
  background: transparent;
  color: var(--x-text-secondary);
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: var(--x-surface-raised);
    color: var(--x-text);
  }

  &.active {
    background: var(--x-selected);
    color: var(--x-text);

    .group-tab-count {
      background: var(--x-ink);
      color: var(--x-on-ink);
    }
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.group-tab-count {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  box-sizing: border-box;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-muted);
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
  text-align: center;
}

.search-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 260px;
  max-width: 100%;
}

::v-deep .search-input .el-input__inner {
  height: 34px;
  line-height: 34px;
}

::v-deep .search-input .el-input__icon {
  line-height: 34px;
  color: var(--x-text-muted);
}

.btn-search {
  height: 34px;
  padding: 0 14px;
}

.sub-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--x-divider);
}

.sub-tab {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: var(--x-surface);
  color: var(--x-text-secondary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;

  em {
    font-style: normal;
    margin-inline-start: 4px;
    color: var(--x-text-muted);
  }

  &:hover {
    border-color: var(--x-border-control);
    color: var(--x-text);
  }

  &.active {
    background: var(--x-ink);
    border-color: var(--x-ink);
    color: var(--x-on-ink);

    em {
      color: var(--x-on-ink);
      opacity: 0.75;
    }
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.params-body {
  min-height: 160px;
  padding: 16px;
}

.params-collapse {
  border: none;
  display: flex;
  flex-direction: column;
  gap: 12px;

  ::v-deep .el-collapse-item {
    border: 1px solid var(--x-border);
    border-radius: 12px;
    overflow: hidden;
  }

  ::v-deep .el-collapse-item__header {
    height: 44px;
    line-height: 44px;
    padding: 0 14px;
    background: var(--x-surface-raised);
    border-bottom: 1px solid transparent;
    color: var(--x-text);
  }

  ::v-deep .el-collapse-item__arrow {
    color: var(--x-text-muted);
  }

  ::v-deep .el-collapse-item__wrap {
    border-bottom: none;
  }

  ::v-deep .el-collapse-item__content {
    padding: 0;
  }

  ::v-deep .el-collapse-item.is-active .el-collapse-item__header {
    border-bottom-color: var(--x-divider);
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding-inline-end: 8px;
}

.section-name {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.section-count {
  flex-shrink: 0;
  height: 20px;
  padding: 0 8px;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-muted);
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
}

.section-hint {
  font-size: 12px;
  color: var(--x-text-muted);
}

.table-scroll {
  overflow-x: auto;
}

.params-table {
  width: 100%;
  min-width: 760px;

  &::before {
    display: none;
  }
}

::v-deep .params-table {
  th.el-table__cell {
    padding: 6px 0;
    white-space: nowrap;
    background: var(--x-surface);
    border-bottom: 1px solid var(--x-divider);
  }

  td.el-table__cell {
    padding: 8px 0;
    border-bottom: 1px solid var(--x-divider);
  }

  .cell {
    padding-inline-start: 12px;
    padding-inline-end: 12px;
  }

  th.el-table__cell > .cell {
    white-space: nowrap;
  }

  .el-table__row:last-child td.el-table__cell {
    border-bottom: 0;
  }
}

.param-code {
  font-family: var(--x-font-mono);
  font-size: 12px;
  color: var(--x-text);
  background: var(--x-canvas);
  padding: 2px 6px;
  border-radius: 6px;
}

.type-tag {
  display: inline-block;
  padding: 0 8px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  color: var(--x-text-secondary);
  font-family: var(--x-font-mono);
  font-size: 11px;
  line-height: 18px;
}

.value-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.value-text {
  color: var(--x-text-secondary);
  word-break: break-all;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  white-space: nowrap;
}

.action-btn {
  height: 28px;
  padding: 0 8px;
  margin: 0 !important;
  color: var(--x-text-secondary);
  font-size: 13px;

  &:hover,
  &:focus {
    background: var(--x-canvas);
    color: var(--x-text);
  }

  &--danger:hover,
  &--danger:focus {
    background: var(--x-danger-bg);
    color: var(--x-danger-text);
  }
}

.empty-state {
  padding: 64px 0;
  text-align: center;
  color: var(--x-text-muted);
  font-size: 14px;
}

.table-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--x-border);
}

.batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  ::v-deep .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.footer-meta {
  color: var(--x-text-muted);
  font-size: 12px;

  .sep {
    margin: 0 6px;
  }
}

.el-footer {
  height: auto !important;
  padding: 0;
}

@media (max-width: 768px) {
  .page {
    padding: 16px;
    gap: 16px;
  }

  .search-group {
    width: 100%;
  }

  .search-input {
    flex: 1;
    width: auto;
  }

  .params-body {
    padding: 12px;
  }
}
</style>
