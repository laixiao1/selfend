<template>
  <el-dialog
    title="预约时间情况"
    :visible.sync="visible"
    width="80%"
    :close-on-click-modal="false"
    @opened="renderChart"
  >
    <el-form :inline="true" :model="form" style="margin-bottom: 20px;">
      <el-form-item label="统计单位">
        <el-select
          v-model="form.unit"
          placeholder="请选择"
          @change="onUnitChange"
        >
          <el-option label="按周" value="week"></el-option>
          <el-option label="按月" value="month"></el-option>
          <el-option label="按年" value="year"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="$emit('reload')">查询</el-button>
      </el-form-item>
    </el-form>
    <div
      ref="chart"
      style="width: 100%; height: 400px; margin-bottom: 20px;"
    ></div>
    <slot></slot>
    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">关 闭</el-button>
    </div>
  </el-dialog>
</template>
<script>
import * as echarts from "echarts";
export default {
  props: {
    visible: Boolean,
    data: Array,
    form: Object,
  },
  watch: {
    visible(val) {
      if (val) this.$nextTick(this.renderChart);
    },
    data() {
      if (this.visible) this.$nextTick(this.renderChart);
    },
  },
  methods: {
    renderChart() {
      if (!this.$refs.chart) return;
      const pieData = (this.data || []).map((item) => ({
        name: item.timeSlot,
        value: item.bookCount,
      }));
      const chart = echarts.init(this.$refs.chart);
      chart.setOption({
        title: { text: "预约时间分布", left: "center" },
        tooltip: { trigger: "item" },
        legend: { bottom: 10, left: "center" },
        series: [
          {
            name: "预约时间段",
            type: "pie",
            radius: "60%",
            data: pieData,
          },
        ],
      });
    },
    onUnitChange() {
      this.$emit("unit-change", this.form.unit);
    },
  },
};
</script>
