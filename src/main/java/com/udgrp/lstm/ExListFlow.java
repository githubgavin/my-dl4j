package com.udgrp.lstm;

/**
 * @author kejw
 * @version V1.0
 * @Project my-nd4j-lstm
 * @Description: TODO
 * @date 2018/1/12
 */
public class ExListFlow {

    //上月同一时间流量
    private Double lastMonCnt;
    //上周同一时间流量
    private Double lastWeekCnt;
    //昨天同一时间流量
    private Double lastDayCnt;
    //今天车流量
    private Double currFlowCnt;

    public Double getLastMonCnt() {
        return lastMonCnt;
    }

    public void setLastMonCnt(Double lastMonCnt) {
        this.lastMonCnt = lastMonCnt;
    }

    public Double getLastWeekCnt() {
        return lastWeekCnt;
    }

    public void setLastWeekCnt(Double lastWeekCnt) {
        this.lastWeekCnt = lastWeekCnt;
    }

    public Double getLastDayCnt() {
        return lastDayCnt;
    }

    public void setLastDayCnt(Double lastDayCnt) {
        this.lastDayCnt = lastDayCnt;
    }

    public Double getCurrFlowCnt() {
        return currFlowCnt;
    }

    public void setCurrFlowCnt(Double currFlowCnt) {
        this.currFlowCnt = currFlowCnt;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("上月同一时间流量=" + this.lastMonCnt + ", ");
        builder.append("上周同一时间流量=" + this.lastWeekCnt + ", ");
        builder.append("昨天同一时间流量=" + this.lastDayCnt + ", ");
        builder.append("今天车流量=" + this.currFlowCnt + ", ");
        return builder.toString();
    }
}
