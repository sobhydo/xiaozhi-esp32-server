<template>
  <div class="pagination-wrapper">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="pageSizeOptions"
      :total="total"
      layout="sizes, prev, pager, next, total"
      background
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script>
export default {
  name: 'Pagination',
  props: {
    total: {
      type: Number,
      default: 0
    },
    currentPage: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    pageSizeOptions: {
      type: Array,
      default: () => [10, 20, 50, 100]
    }
  },
  methods: {
    handleSizeChange(val) {
      this.$emit('size-change', val);
    },
    handlePageChange(page) {
      this.$emit('page-change', page);
    }
  }
};
</script>

<style lang="scss" scoped>
::v-deep .el-pagination {
  color: var(--x-text-muted);
  .btn-next,
  .btn-prev,
  .el-pager li {
    background: transparent !important;
    min-width: 32px !important;
    border-radius: var(--x-radius-control);
    color: var(--x-text-secondary);
    font-weight: 500;
  }
  .el-pager li:not(.disabled):not(.active):hover {
    background: var(--x-canvas) !important;
    color: var(--x-text);
  }
  .el-pager li:not(.disabled).active {
    background: var(--x-ink) !important;
    color: var(--x-on-ink);
    font-weight: 600;
  }
}
</style>
