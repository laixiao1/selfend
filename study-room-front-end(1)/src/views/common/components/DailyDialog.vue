<template>
  <el-dialog
    title="当日座位预约情况"
    :visible.sync="visible"
    width="80%"
    :close-on-click-modal="false"
    @opened="renderChart"
  >
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
      const counts = {}(this.data || []).forEach((item) => {
        counts[item.status] = (counts[item.status] || 0) + 1;
      });
      const pieData = Object.keys(counts).map((key) => ({
        name: key,
        value: counts[key],
      }));
      const chart = echarts.init(this.$refs.chart);
      chart.setOption({
        title: { text: "当日座位使用状态分布", left: "center" },
        tooltip: { trigger: "item" },
        legend: { bottom: 10, left: "center" },
        series: [
          {
            name: "使用状态",
            type: "pie",
            radius: "60%",
            data: pieData,
          },
        ],
      });
    },
  },
};
</script>
