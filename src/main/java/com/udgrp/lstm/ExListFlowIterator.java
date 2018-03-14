package com.udgrp.lstm;


import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ExListFlowIterator implements DataSetIterator {

    private static final int VECTOR_SIZE = 4;
    //每批次的训练数据组数
    private int batchNum;

    //每组训练数据长度(DailyData的个数)
    private int exampleLength;

    //数据集
    private List<ExListFlow> dataList;

    //存放剩余数据组的index信息
    private List<Integer> dataRecord;

    private double[] maxNum;
    private double[] minNum;

    /**
     * 构造方法
     */
    public ExListFlowIterator() {
        dataRecord = new ArrayList<>();
    }

    /**
     * 加载数据并初始化
     */
    public boolean loadData(String fileName, int batchNum, int exampleLength) {
        this.batchNum = batchNum;
        this.exampleLength = exampleLength;
        maxNum = new double[4];
        //加载文件中的股票数据
        try {
            readDataFromFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //重置训练批次列表
        resetDataRecord();
        return true;
    }

    /**
     * 重置训练批次列表
     */
    private void resetDataRecord() {
        dataRecord.clear();
        int total = dataList.size() / exampleLength + 1;
        for (int i = 0; i < total; i++) {
            dataRecord.add(i * exampleLength);
        }
    }

    /**
     * 从文件中读取股票数据
     */
    public List<ExListFlow> readDataFromFile(String fileName) throws IOException {
        dataList = new ArrayList<>();
        FileInputStream fis = new FileInputStream(fileName);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        String line = in.readLine();
        for (int i = 0; i < maxNum.length; i++) {
            maxNum[i] = 0;
        }
        System.out.println("读取数据..");
        while (line != null) {
            String[] strArr = line.split(",");
            if (strArr.length == 9) {
                ExListFlow data = new ExListFlow();
                //获得最大值信息，用于归一化
                double[] nums = new double[4];
                for (int j = 0; j < 4; j++) {
                    nums[j] = Double.valueOf(strArr[j + 5]);
                    if (nums[j] > maxNum[j]) {
                        maxNum[j] = nums[j];
                    }
                }
                //构造data对象
                data.setLastMonCnt(Double.valueOf(nums[0]));
                data.setLastWeekCnt(Double.valueOf(nums[1]));
                data.setLastDayCnt(Double.valueOf(nums[2]));
                data.setCurrFlowCnt(Double.valueOf(nums[3]));
                dataList.add(data);
            }
            line = in.readLine();
        }
        in.close();
        fis.close();
        //System.out.println("反转list...");
        //Collections.reverse(dataList);
        return dataList;
    }

    public double[] getMaxArr() {
        return this.maxNum;
    }

    @Override
    public void reset() {
        resetDataRecord();
    }

    @Override
    public boolean hasNext() {
        return dataRecord.size() > 0;
    }

    @Override
    public DataSet next() {
        return next(batchNum);
    }

    /**
     * 获得接下来一次的训练数据集
     */
    @Override
    public DataSet next(int num) {
        if (dataRecord.size() <= 0) {
            throw new NoSuchElementException();
        }
        try {
            return nextDataSet(num);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private DataSet nextDataSet(int num) throws IOException {
        int actualBatchSize = Math.min(num, dataRecord.size());
        int actualLength = Math.min(exampleLength, dataList.size() - dataRecord.get(0) - 1);
        INDArray input = Nd4j.create(new int[]{actualBatchSize, VECTOR_SIZE, actualLength}, 'f');
        INDArray label = Nd4j.create(new int[]{actualBatchSize, 1, actualLength}, 'f');
        ExListFlow nextData = null, curData = null;
        //获取每批次的训练数据和标签数据
        for (int i = 0; i < actualBatchSize; i++) {
            int index = dataRecord.remove(0);
            int endIndex = Math.min(index + exampleLength, dataList.size() - 1);
            curData = dataList.get(index);
            for (int j = index; j < endIndex; j++) {
                //获取数据信息
                nextData = dataList.get(j + 1);
                //构造训练向量
                int c = endIndex - j - 1;
                input.putScalar(new int[]{i, 0, c}, curData.getLastMonCnt() / maxNum[0]);
                input.putScalar(new int[]{i, 1, c}, curData.getLastWeekCnt() / maxNum[1]);
                input.putScalar(new int[]{i, 2, c}, curData.getLastDayCnt() / maxNum[2]);
                input.putScalar(new int[]{i, 3, c}, curData.getLastDayCnt() / maxNum[3]);
                //构造label向量
                label.putScalar(new int[]{i, 0, c}, nextData.getCurrFlowCnt() / maxNum[3]);

                curData = nextData;
            }
            if (dataRecord.size() <= 0) {
                break;
            }
        }

        return new DataSet(input, label);
    }


    @Override
    public int batch() {
        return batchNum;
    }

    @Override
    public int cursor() {
        return totalExamples() - dataRecord.size();
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor preProcessor) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        return null;
    }

    @Override
    public int totalExamples() {
        return (dataList.size()) / exampleLength;
    }

    @Override
    public int inputColumns() {
        return dataList.size();
    }

    @Override
    public int totalOutcomes() {
        return 1;
    }

    @Override
    public boolean resetSupported() {
        return false;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public List<String> getLabels() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
