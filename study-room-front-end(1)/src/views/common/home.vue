<template>
  <div class="mod-home">
    <header id="title">自习室座位管理系统(当日预约情况)</header>
    <el-form
      :inline="true"
      :model="dataForm"
      @keyup.enter.native="getDataList()"
    >
      <el-form-item>
        <el-date-picker
          v-model="dataForm.valueTime"
          value-format="yyyy-MM-dd"
          type="date"
          placeholder="选择日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <div
          style="display: flex; justify-content: flex-start;align-items: center;"
        >
          <el-button type="primary" @click="getList()">查询</el-button>
          <el-button type="success" @click="getDailyData()"
            >当日座位使用情况</el-button
          >
          <!-- <el-button type="warning" @click="getOverallDataHandle()"
            >整体预约情况</el-button
          >
          <el-button type="info" @click="getTimeDataHandle()"
            >预约时间情况</el-button
          > -->
          <ChartsButton
            :valueTime="dataForm.valueTime"
            style="margin-left: 5px;"
          />
        </div>
      </el-form-item>
    </el-form>

    <el-table
      :data="dataList"
      border
      v-loading="dataListLoading"
      style="width: 100%;"
    >
      <el-table-column type="index" width="50"></el-table-column>
      <el-table-column
        prop="floor"
        header-align="center"
        align="center"
        label="所属楼层"
      ></el-table-column>
      <el-table-column
        prop="roomName"
        header-align="center"
        align="center"
        label="自习室"
      ></el-table-column>
      <!-- <el-table-column
        prop="seat_name"
        header-align="center"
        align="center"
        label="预约人"
      ></el-table-column> -->
      <el-table-column
        prop="reserveCount"
        header-align="center"
        align="center"
        label="预约次数"
      ></el-table-column>
    </el-table>

    <!-- 当日座位使用情况弹窗 -->
    <el-dialog
      title="当日座位预约情况"
      :visible.sync="showDailyDialog"
      width="80%"
      :close-on-click-modal="false"
    >
      <el-table
        :data="dailyDataList"
        border
        v-loading="dailyListLoading"
        style="width: 100%;"
        empty-text="暂无座位数据"
      >
        <el-table-column type="index" width="50" label="#"></el-table-column>
        <el-table-column
          prop="seatNo"
          header-align="center"
          align="center"
          label="座位号"
          width="120"
        ></el-table-column>
        <el-table-column
          prop="status"
          header-align="center"
          align="center"
          label="使用状态"
          width="120"
        >
          <template slot-scope="scope">
            <el-tag
              :type="scope.row.seatStatus === '占用' ? 'danger' : 'success'"
              size="medium"
            >
              {{ scope.row.seatStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="bookTime"
          header-align="center"
          align="center"
          label="预约时间"
          width="180"
        >
          <template slot-scope="scope">
            {{ scope.row.bookTime || "-" }}
          </template>
        </el-table-column>
        <el-table-column
          prop="userName"
          header-align="center"
          align="center"
          label="使用人"
        >
          <template slot-scope="scope">
            {{ scope.row.userName || "-" }}
          </template>
        </el-table-column>
        <el-table-column
          prop="phone"
          header-align="center"
          align="center"
          label="联系方式"
          width="150"
        >
          <template slot-scope="scope">
            {{ scope.row.phone || "-" }}
          </template>
        </el-table-column>
      </el-table>

      <div slot="footer" class="dialog-footer">
        <el-button @click="showDailyDialog = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 整体预约情况弹窗 -->
    <el-dialog
      title="整体预约情况"
      :visible.sync="showOverallDialog"
      width="80%"
      :close-on-click-modal="false"
    >
      <el-form :inline="true" :model="overallForm" style="margin-bottom: 20px;">
        <el-form-item label="统计单位">
          <el-select v-model="overallForm.unit" placeholder="请选择">
            <el-option label="按周" value="week"></el-option>
            <el-option label="按月" value="month"></el-option>
            <el-option label="按年" value="year"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getOverallData()">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="overallDataList"
        border
        v-loading="overallListLoading"
        style="width: 100%;"
        height="400"
      >
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column
          prop="period"
          header-align="center"
          align="center"
          label="统计周期"
        ></el-table-column>
        <el-table-column
          prop="total"
          header-align="center"
          align="center"
          label="总预约次数"
        ></el-table-column>
        <el-table-column
          prop="average"
          header-align="center"
          align="center"
          label="日均预约次数"
        ></el-table-column>
        <el-table-column
          prop="maxDay"
          header-align="center"
          align="center"
          label="最高预约日"
        ></el-table-column>
        <el-table-column
          prop="maxCount"
          header-align="center"
          align="center"
          label="最高预约量"
        ></el-table-column>
      </el-table>

      <div slot="footer" class="dialog-footer">
        <el-button @click="showOverallDialog = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 预约时间情况弹窗 -->
    <el-dialog
      title="预约时间情况"
      :visible.sync="showTimeDialog"
      width="80%"
      :close-on-click-modal="false"
    >
      <el-form :inline="true" :model="timeForm" style="margin-bottom: 20px;">
        <el-form-item label="统计单位">
          <el-select v-model="timeForm.unit" placeholder="请选择">
            <el-option label="按周" value="week"></el-option>
            <el-option label="按月" value="month"></el-option>
            <el-option label="按年" value="year"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getTimeData()">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="timeDataList"
        border
        v-loading="timeListLoading"
        style="width: 100%;"
        height="400"
      >
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column
          prop="period"
          header-align="center"
          align="center"
          label="时间段"
        ></el-table-column>
        <el-table-column
          prop="count"
          header-align="center"
          align="center"
          label="预约次数"
        ></el-table-column>
        <el-table-column
          prop="ratio"
          header-align="center"
          align="center"
          label="占比"
        >
          <template slot-scope="scope"> {{ scope.row.ratio }}</template>
        </el-table-column>
        <el-table-column
          prop="trend"
          header-align="center"
          align="center"
          label="趋势"
        >
          <template slot-scope="scope">
            <i
              :class="getTrendIcon(scope.row.trend)"
              :style="{ color: getTrendColor(scope.row.trend) }"
            ></i>
          </template>
        </el-table-column>
      </el-table>

      <div slot="footer" class="dialog-footer">
        <el-button @click="showTimeDialog = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ChartsButton from "./components/ChartsButton.vue";
export default {
  data() {
    return {
      dataList: [],
      dataListLoading: false,
      dataForm: {
        valueTime: this.getNowDate()
      },
      dailyDataList: [],
      dailyListLoading: false,
      showDailyDialog: false,
      overallDataList: [],
      overallListLoading: false,
      showOverallDialog: false,
      overallForm: {
        unit: "week"
      },
      timeDataList: [],
      timeListLoading: false,
      showTimeDialog: false,
      timeForm: {
        unit: "week"
      }
    };
  },
  created() {
    this.getList();
  },
  components: {
    ChartsButton
  },
  methods: {
    // 获取当前日期（YYYY-MM-DD格式）
    getNowDate() {
      const now = new Date();
      const year = now.getFullYear();
      const month = (now.getMonth() + 1).toString().padStart(2, "0");
      const day = now
        .getDate()
        .toString()
        .padStart(2, "0");
      return `${year}-${month}-${day}`;
    },

    // 获取座位预约列表
    getList() {
      this.dataListLoading = true;
      this.$http({
        url: this.$http.adornUrl("/basappointment/count"),
        method: "get",
        params: this.$http.adornParams({
          date: this.dataForm.valueTime
        })
      })
        .then(({ data }) => {
          this.dataListLoading = false;
          if (data && data.code === 0) {
            this.dataList = data.data || [];
          } else {
            this.$message.error(data.msg || "获取数据失败");
          }
        })
        .catch(() => {
          this.dataListLoading = false;
          this.$message.error("请求失败，请稍后重试");
        });
    },

    // 修改后的获取当日数据方法
    getDailyData() {
      this.dailyListLoading = true;
      this.showDailyDialog = true;

      const requestDate = this.dataForm.valueTime || this.getNowDate();

      this.$http({
        url: this.$http.adornUrl("/basappointment/daily"),
        method: "get",
        params: this.$http.adornParams({
          date: requestDate
        })
      })
        .then(response => {
          // 处理响应数据
          let resData = response.data;
          if (resData && resData.code === 0) {
            // 处理数据格式，确保所有字段都有值
            this.dailyDataList = (resData.data || []).map(item => {
              return {
                seatNo: item.seatId || "-",
                seatStatus: item.seatStatus=='0'?"空闲":"占用",
                bookTime: item.seatTime || null,
                userName: item.seatName || "-",
                phone: item.seatPhone || "-"
              };
            });
            // console.log(this.dailyDataList)
            // 如果没有数据，显示提示
            if (this.dailyDataList.length === 0) {
              this.$message.info(`所选日期 ${requestDate} 没有预约记录`);
            }
          } else {
            this.$message.error(resData.msg || "获取数据失败");
          }
        })
        .catch(error => {
          console.error("获取当日数据失败:", error);
          this.$message.error("获取数据失败，请检查网络或稍后重试");
        })
        .finally(() => {
          this.dailyListLoading = false;
        });
    },

    // 获取整体预约情况
    getOverallDataHandle() {
      this.showOverallDialog = true;
      this.getOverallData();
    },

    getOverallData() {
      this.overallListLoading = true;
      this.$http({
        url: this.$http.adornUrl("/basappointment/overview"),
        method: "get",
        params: this.$http.adornParams({
          unit: this.overallForm.unit
        })
      })
        .then(({ data }) => {
          this.overallListLoading = false;
          if (data && data.code === 0) {
            this.overallDataList = data.data || [];
          } else {
            this.$message.error(data.msg || "获取数据失败");
          }
        })
        .catch(() => {
          this.overallListLoading = false;
          this.$message.error("获取数据失败，请稍后重试");
        });
    },

    // 获取预约时间情况
    getTimeDataHandle() {
      this.showTimeDialog = true;
      this.getTimeData();
    },

    getTimeData() {
      this.timeListLoading = true;
      this.$http({
        url: this.$http.adornUrl("/basappointment/timeStats"),
        method: "get",
        params: this.$http.adornParams({
          unit: this.timeForm.unit
        })
      })
        .then(({ data }) => {
          this.timeListLoading = false;
          if (data && data.code === 0) {
            this.timeDataList = data.data || [];
          } else {
            this.$message.error(data.msg || "获取数据失败");
          }
        })
        .catch(() => {
          this.timeListLoading = false;
          this.$message.error("获取数据失败，请稍后重试");
        });
    },

    // 状态标签类型
    getStatusType(status) {
      const map = {
        已预约: "primary",
        使用中: "success",
        已取消: "info",
        超时未到: "danger",
        已完成: "warning"
      };
      return map[status] || "info";
    },

    // 趋势图标
    getTrendIcon(trend) {
      return trend === "up"
        ? "el-icon-top"
        : trend === "down"
        ? "el-icon-bottom"
        : "el-icon-minus";
    },

    // 趋势颜色
    getTrendColor(trend) {
      return trend === "up"
        ? "#F56C6C"
        : trend === "down"
        ? "#67C23A"
        : "#909399";
    }
  }
};
</script>

<style scoped>
.mod-home {
  padding: 20px;
}

#title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  text-align: center;
}

.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  font-size: 16px;
}

.empty-tip i {
  font-size: 24px;
  margin-right: 8px;
}

.dialog-footer {
  text-align: center;
}

:deep(.el-dialog__body) {
  padding: 20px;
}
</style>
