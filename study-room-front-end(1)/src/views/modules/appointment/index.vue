<template>
  <div class="mod-role">
    <el-form
      :inline="true"
      :model="dataForm"
      @keyup.enter.native="getDataList()"
    >
      <el-form-item>
        <el-input
          v-model="dataForm.seatName"
          placeholder="预约者名称"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-input
          v-model="dataForm.seatPhone"
          placeholder="预约者电话"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-select
          v-model="dataForm.seatState"
          placeholder="预约状态"
          clearable
        >
          <el-option
            v-for="item in options"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="getDataList()">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table
      :data="dataList"
      border
      v-loading="dataListLoading"
      style="width: 100%;"
    >
      <el-table-column
        prop="seatName"
        header-align="center"
        align="center"
        label="用户名称"
      >
      </el-table-column>
      <el-table-column
        prop="seatPhone"
        header-align="center"
        align="center"
        label="联系方式"
        :show-overflow-tooltip="true"
      >
      </el-table-column>
      <el-table-column
        prop="floor"
        header-align="center"
        align="center"
        label="楼层"
        :show-overflow-tooltip="true"
      >
      </el-table-column>
      <el-table-column
        prop="roomName"
        header-align="center"
        align="center"
        label="自习室"
        :show-overflow-tooltip="true"
      >
      </el-table-column>
      <el-table-column
        prop="seatNumber"
        header-align="center"
        align="center"
        label="座位号"
        :show-overflow-tooltip="true"
      >
      </el-table-column>
      <el-table-column
        prop="seatDay"
        header-align="center"
        align="center"
        width="180"
        label="预约时间段"
      >
      </el-table-column>
      <el-table-column
        prop="seatState"
        header-align="center"
        align="center"
        label="预约状态"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <div class="table-contents">
            <el-tag :type="getTagType(scope.row.seatState)">
              {{ getStatusText(scope.row.seatState) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="updateTime"
        header-align="center"
        align="center"
        width="180"
        label="签到时间"
      >
      </el-table-column>
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        width="150"
        label="操作"
      >
        <template slot-scope="scope">
          <!-- 当 scope.row.seatState !== 0 时显示签到按钮，否则不显示 -->
          <el-button
            v-if="scope.row.seatState == 1"
            type="text"
            size="small"
            @click="examineHandle(scope.row)"
          >
            签到
          </el-button>
          <!-- 删除按钮一直显示 -->
          <el-button
            type="text"
            size="small"
            @click="deleteHandle(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      :total="totalPage"
      layout="total, sizes, prev, pager, next, jumper"
    >
    </el-pagination>
  </div>
</template>

<script>
export default {
  name: "SelfStudyIndex",

  data() {
    return {
      dataForm: {
        seatPhone: "",
        seatName: "",
        seatState: ""
      },
      dataList: [],
      pageIndex: 1,
      pageSize: 10,
      totalPage: 0,
      dataListLoading: false,
      options: [
        {
          value: "0",
          label: "已签到"
        },
        {
          value: "1",
          label: "未签到"
        }
      ]
    };
  },

  mounted() {},

  activated() {
    this.getDataList();
  },

  methods: {
    getTagType(state) {
      const map = {
        0: 'success',  // 已签到
        1: 'warning',  // 未签到
        2: 'danger',   // 已迟到
        3: 'info'      // 已完成
      };
      return map[state] || 'danger';
    },
    getStatusText(state) {
      const map = {
        0: '已签到',
        1: '未签到',
        2: '已迟到',
        3: '已完成'
      };
      return map[state] || '未知状态';
    },
    sizeChangeHandle(val) {
      this.pageSize = val;
      this.pageIndex = 1;
      this.getDataList();
    },
    // 当前页
    currentChangeHandle(val) {
      this.pageIndex = val;
      this.getDataList();
    },
    getDataList() {
      this.dataListLoading = true;
      this.$http({
        url: this.$http.adornUrl("/basappointment/list"),
        method: "get",
        params: this.$http.adornParams({
          page: this.pageIndex,
          limit: this.pageSize,
          seatPhone: this.dataForm.seatPhone,
          seatName: this.dataForm.seatName,
          seatState: this.dataForm.seatState,
        })
      }).then(({ data }) => {
        if (data && data.code === 0) {
          this.dataList = data.data.content;
          this.totalPage = Number(data.data.totalCount);
        } else {
          this.dataList = [];
          this.totalPage = 0;
        }
        this.dataListLoading = false;
      });
    },
    // 删除
    deleteHandle(id)
    {
      this.$confirm(`确定删除此预约?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl("/basappointment/delete"),
            method: "delete",
            data: [id]
          }).then(({ data }) => {
            if (data && data.code === 0) {
              this.$message({
                message: "操作成功",
                type: "success",
                duration: 500,
                onClose: () => {
                  this.getDataList();
                }
              });
            } else {
              this.$message.error(data.msg);
            }
          });
        })
        .catch(() => {});
    },
    // 签到
    examineHandle(info) {
      // 1. 解析预约时间段
      const seatDayParts = info.seatDay.split(' ');
      if (seatDayParts.length < 2) {
        this.$message.error('预约时间格式错误');
        return;
      }

      const datePart = seatDayParts[0];
      const timeRange = seatDayParts[1];

      // 2. 提取开始时间
      const startTimeStr = timeRange.split('-')[0];
      const startDateTime = `${datePart} ${startTimeStr}`;

      // 3. 创建预约开始时间的Date对象
      const startDate = new Date(startDateTime.replace(/-/g, '/'));

      // 4. 获取当前时间
      const now = new Date();

      // 5. 获取预约创建时间（create_time）
      const createTime = new Date(info.createTime.replace(/-/g, '/'));

      // 6. 确定签到截止时间（根据两种不同情况）
      let deadline;

      if (createTime <= startDate) {
        // 情况1：预约创建时间早于预约开始时间
        // 签到截止时间 = 开始时间 + 15分钟
        deadline = new Date(startDate.getTime() + 15 * 60000);
      } else {
        // 情况2：预约创建时间晚于预约开始时间
        // 签到截止时间 = 创建时间 + 15分钟
        deadline = new Date(createTime.getTime() + 15 * 60000);
      }

      // 7. 验证当前时间是否在允许签到的时间范围内
      if (now > deadline) {
        this.$message.warning('已迟到无法签到');
        return;
      }
      let func = "";
      let tips = "";
      if (info.seatState) {
        console.log("签到");
        func = "/basappointment/stateOn";
        tips = "确定已到场?";
      }

      this.$confirm(tips, "提示", {
        confirmButtonText: "签到",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl(func + "?id=" + info.id + '&' + 'seatId=' + info.seatId),
            method: "post"
          }).then(({ data }) => {
            if (data && data.code === 0) {
              this.$message({
                message: "操作成功",
                type: "success",
                duration: 500,
                onClose: () => {
                  this.getDataList();
                }
              });
            } else {
              this.$message.error(data.msg);
            }
          });
        })
        .catch(() => {});
    }
  }
};
</script>

<style lang="scss" scoped></style>
