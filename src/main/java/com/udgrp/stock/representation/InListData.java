package com.udgrp.stock.representation;

/**
 * Created by zhanghao on 26/7/17.
 * @author ZHANG HAO
 */
public class InListData {
    //今天车流量
    private double currFlowCnt;

    public InListData() {}

    public InListData(double currFlowCnt) {
        this.currFlowCnt = currFlowCnt;
    }

    public double getCurrFlowCnt() {
        return currFlowCnt;
    }

    public void setCurrFlowCnt(double currFlowCnt) {
        this.currFlowCnt = currFlowCnt;
    }
}
