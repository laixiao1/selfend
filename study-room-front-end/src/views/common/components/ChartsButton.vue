<template>
  <div>
    <el-button type="success" @click="openDailyDialog">
      当日座位使用次数统计
    </el-button>
    <el-button type="warning" @click="openOverallDialog">
      整体预约次数统计
    </el-button>
    <el-button type="info" @click="openTimeDialog">
      预约时间分布统计
    </el-button>

    <!-- 当日座位使用次数统计（饼图） -->
    <el-dialog
      title="当日座位使用次数统计"
      :visible.sync="showDailyDialog"
      width="60%"
      :close-on-click-modal="false"
      @opened="renderDailyPieChart"
    >
      <div v-if="loading.daily" style="text-align:center;padding:100px 0;">
        <el-spinner />
      </div>
      <div v-else ref="dailyPieChart" style="width: 100%; height: 400px;"></div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showDailyDialog = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 整体预约次数统计（柱状图） -->
    <el-dialog
      title="整体预约次数统计"
      :visible.sync="showOverallDialog"
      width="60%"
      :close-on-click-modal="false"
      @opened="onDialogOpened"
    >
      <el-form :inline="true" :model="overallForm" style="margin-bottom: 20px;">
        <el-form-item label="统计单位">
          <el-select
            v-model="overallForm.unit"
            placeholder="请选择"
            @change="getOverallData"
          >
            <el-option label="按年" value="year" />
            <el-option label="按月" value="month" />
          </el-select>
        </el-form-item>

        <el-form-item label="选择年份">
          <el-date-picker
            v-model="overallForm.year"
            type="year"
            placeholder="选择年份"
            value-format="yyyy"
            :clearable="false"
            style="width: 120px"
            @change="getOverallData"
          />
        </el-form-item>

        <!-- <el-form-item v-if="overallForm.unit === 'month'" label="选择月份">
          <el-date-picker
            v-model="overallForm.month"
            type="month"
            placeholder="选择月份"
            value-format="MM"
            :clearable="false"
            style="width: 120px"
            @change="getOverallData"
          />
        </el-form-item> -->
      </el-form>

      <div v-if="loading.overall" class="loading-container">
        <el-spinner />
      </div>

      <div v-else ref="overallBarChart" class="chart-container"></div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="showOverallDialog = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 预约时间分布统计（饼图） -->
    <el-dialog
      title="预约时间分布统计"
      :visible.sync="showTimeDialog"
      width="60%"
      :close-on-click-modal="false"
      @opened="renderTimePieChart"
    >
      <!-- 筛选表单 -->
      <el-form inline :model="timeForm" style="margin-bottom: 20px;">
        <el-form-item label="统计单位">
          <el-select v-model="timeForm.unit" @change="onTimeUnitChange">
            <el-option label="按年" value="year" />
            <el-option label="按月" value="month" />
          </el-select>
        </el-form-item>

        <el-form-item label="选择年份">
          <el-date-picker
            v-model="timeForm.year"
            type="year"
            placeholder="选择年份"
            value-format="yyyy"
            :clearable="false"
            style="width: 120px"
            @change="getTimeData"
          />
        </el-form-item>

        <el-form-item v-if="timeForm.unit === 'month'" label="选择月份">
          <el-date-picker
            v-model="timeForm.month"
            type="month"
            placeholder="选择月份"
            value-format="MM"
            :clearable="false"
            style="width: 120px"
            @change="getTimeData"
          />
        </el-form-item>
      </el-form>

      <!-- 加载中 -->
      <div v-if="loading.time" class="loading-container">
        <el-spinner />
      </div>

      <!-- 饼图容器 -->
      <div v-else ref="timePieChart" class="chart-container"></div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="showTimeDialog = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from "echarts";

export default {
  data() {
    const defaultYear = new Date().getFullYear().toString();
    return {
      showDailyDialog: false,
      showOverallDialog: false,
      showTimeDialog: false,
      dailyPieData: [],
      overallBarData: [],
      overallForm: {
        unit: "year",
        year: defaultYear,
        month: "",
      },
      timePieData: [],
      timeForm: {
        unit: "year",
        year: defaultYear,
        month: "",
      },
      loading: {
        daily: false,
        overall: false,
        time: false,
      },
      // 新增专用实例
      overallChart: null,
    };
  },
  props: {
    // 外部选择的时间
    valueTime: {
      type: String,
      default: "",
    },
  },
  methods: {
    // 打开弹窗并获取数据
    openDailyDialog() {
      this.showDailyDialog = true;
      this.getDailyPieData();
    },
    openOverallDialog() {
      this.showOverallDialog = true;
      this.getOverallData();
    },
    openTimeDialog() {
      this.showTimeDialog = true;
      this.getTimeData();
    },

    getTimeData() {
      this.loading.time = true;
      this.timePieData = [];

      const params = {
        unit: this.timeForm.unit,
        year: this.timeForm.year,
      };
      if (this.timeForm.unit === "month" && this.timeForm.month) {
        params.month = this.timeForm.month;
      }

      this.$http
        .get(this.$http.adornUrl("/basappointment/time"), { params })
        .then(({ data }) => {
          if (data && data.code === 0) {
            this.timePieData = (data.data || []).map((item) => ({
              name: item.timeSlot || item.hour || "-",
              value: Number(
                item.reservationCount ||
                  item.reservation_count ||
                  item.count ||
                  0
              ),
            }));
          }
        })
        .finally(() => {
          this.loading.time = false;
          this.$nextTick(this.renderTimePieChart);
        });
    },

    // 渲染饼图
    renderTimePieChart() {
      const dom = this.$refs.timePieChart;
      if (!dom || !this.timePieData.length) return;

      if (this.timeChart) {
        this.timeChart.clear();
      } else {
        this.timeChart = echarts.init(dom);
        window.addEventListener("resize", () => this.timeChart.resize());
      }

      const option = {
        title: {
          text: "预约时间分布",
          left: "center",
        },
        tooltip: {
          trigger: "item",
          formatter: "{b}: {c}次 ({d}%)",
        },
        legend: {
          bottom: 10,
          left: "center",
        },
        series: [
          {
            name: "预约时间段",
            type: "pie",
            radius: "60%",
            avoidLabelOverlap: false,
            label: {
              show: true,
              position: "outside",
              formatter: "{b}: {c}次 ({d}%)",
            },
            labelLine: { show: true },
            data: this.timePieData,
          },
        ],
      };
      this.timeChart.setOption(option);
    },

    // // 略去饼图 & 时间分布的代码，请保留你原来的实现…
    // getDailyPieData() {
    //   this.loading.daily = true;
    //   this.dailyPieData = [];
    //   this.$http
    //     .get(this.$http.adornUrl("/basappointment/seatCount"), {
    //       params: { date: this.valueTime },
    //     })
    //     .then(({ data }) => {
    //       console.log(data)
    //       if (data.code === 0) {
    //         this.dailyPieData = (data.data).map((item) => ({
    //           // name: item.seatNo || item.seat_id || "-",
    //           name: item.seat_name,
    //           value: item.usageCount || item.usage_count || 0,
    //         }));
    //       }else{
    //         console.log("111")
    //         this.dailyPieData =[];
    //          // 如果没有数据，显示提示
    //         this.$message.info(`所选日期 ${this.valueTime} 没有预约记录`);
    //       }
    //     })
    //     .finally(() => {
    //       this.loading.daily = false;
    //       this.$nextTick(this.renderDailyPieChart);
    //     });
    // },
    // 获取当日座位使用次数统计（饼图）
    getDailyPieData() {
      this.loading.daily = true;
      this.dailyPieData = [];
      this.$http({
        url: this.$http.adornUrl("/basappointment/seatCount"),
        method: "get",
        params: {
          date: this.valueTime,
        },
      })
        .then(({ data }) => {
          if (data && data.data.length>0) {
            this.dailyPieData = (data.data).map((item) => ({
              // name: item.seatNo || item.seat_id || "-",
              name: item.seat_name,
              value: item.usageCount || item.usage_count || 0,
            }));
          }else{  
            this.dailyPieData = [];
            // this.showDailyDialog = false;
             // 如果没有数据，显示提示
            this.$message.info(`所选日期没有预约记录`);
            
          }
        })
        .catch(() => {
          this.dailyPieData = [];
        })
        .finally(() => {
          this.loading.daily = false;
          this.$nextTick(this.renderDailyPieChart);
        });
    },
    renderDailyPieChart() {
      const dom = this.$refs.dailyPieChart;
      if (!dom ) return;

      if(!this.dailyPieData.length){
        if(this.dailyChart){
          this.dailyChart.dispose();
          this.dailyChart = null;
        }
      }

      if (this.dailyChart) {
        this.dailyChart.clear();
      } else {
        this.dailyChart = echarts.init(dom);
        window.addEventListener("resize", () => this.dailyChart.resize());
      }

      const option = {
        title: {
          text: `${this.valueTime}-当日座位使用次数统计`,
          left: "center",
        },
        tooltip: {
          trigger: "item",
          formatter: "{b}: {c}次 ({d}%)",
        },
        legend: {
          bottom: 10,
          left: "center",
          formatter: (name) => name,
        },
        series: [
          {
            name: "使用次数",
            type: "pie",
            radius: "60%",
            avoidLabelOverlap: false,
            label: {
              show: true,
              position: "outside",
              formatter: "座位:{b}: {c}次 ({d}%)",
            },
            labelLine: { show: true },
            data: this.dailyPieData,
          },
        ],
      };

      this.dailyChart.setOption(option);
    },
    // 获取整体预约次数统计（柱状图）
    getOverallData() {
      this.loading.overall = true;
      this.overallBarData = [];
      const params = {
        unit: this.overallForm.unit,
        year: this.overallForm.year,
      };
      if (this.overallForm.unit === "month" && this.overallForm.month) {
        params.month = this.overallForm.month;
      }
      this.$http
        .get(this.$http.adornUrl("/basappointment/overall"), { params })
        .then(({ data }) => {
          if (data && data.data.length>0) {
            this.overallBarData = (data.data || []).map((item) => {
              return {
                period: item.period,
                total: +item.count,
              };
            });
          }else{
            this.overallBarData = [];
            this.$message.info(`所选日期没有预约记录`);
          }
        })
        .finally(() => {
          this.loading.overall = false;
          this.$nextTick(this.renderOverallBarChart);
        });
    },

    // 修复后的 renderOverallBarChart
    renderOverallBarChart() {
      const dom = this.$refs.overallBarChart;
      if (!dom ) return;

      if(!this.overallBarData.length){
        if(this.overallBarChart){
          this.overallBarChart.dispose();
          this.overallBarChart = null;
        }
      }

      // 初始化或清空旧图
      if (this.overallChart) {
        this.overallChart.clear();
      } else {
        this.overallChart = echarts.init(dom);
        window.addEventListener("resize", () => this.overallChart.resize());
      }

      // 1. 抽取横/纵坐标
      const periods = this.overallBarData.map((item) => item.period);
      const counts = this.overallBarData.map((item) => Number(item.total));

      console.log(periods, counts, this.overallBarData, "ccc");

      // 2. X 轴名称
      const xAxisName = this.overallForm.unit === "year" ? "年份" : "月份";

      // 3. Option
      const option = {
        title: {
          text: "整体预约次数统计",
          left: "center",
        },
        tooltip: {
          trigger: "axis",
          axisPointer: { type: "shadow" },
          formatter: "{b}: {c} 次",
        },
        legend: {
          data: ["预约次数"],
          bottom: 0,
          left: "center",
        },
        grid: {
          left: "3%",
          right: "4%",
          bottom: this.overallForm.unit === "month" ? "25%" : "15%",
          containLabel: true,
        },
        xAxis: {
          type: "category",
          name: xAxisName,
          nameLocation: "middle",
          nameGap: 30,
          data: periods,
          axisTick: {
            alignWithLabel: true,
          },
          axisLabel: {
            rotate: 45,
            interval: 0,
          },
        },
        yAxis: {
          type: "value",
          name: "预约次数",
        },
        series: [
          {
            name: "预约次数",
            type: "bar",
            data: counts,
            barWidth: "20%",
            itemStyle: {
              color: "#409EFF",
            },
            label: {
              show: true,
              position: "top",
              formatter: "{c}",
            },
          },
        ],
      };

      // 4. 渲染
      this.overallChart.setOption(option);
    },
  },
};
</script>

<style scoped>
.loading-container {
  text-align: center;
  padding: 100px 0;
}
.chart-container {
  width: 100%;
  height: 400px;
}
.dialog-footer {
  text-align: right;
}
.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 16px;
}
</style>
