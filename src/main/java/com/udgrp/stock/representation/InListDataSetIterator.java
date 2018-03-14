package com.udgrp.stock.representation;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.ImmutableMap;
import com.udgrp.stock.utils.DBUtil;
import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by zhanghao on 26/7/17.
 * Modified by zhanghao on 28/9/17.
 *
 * @author ZHANG HAO
 */
public class InListDataSetIterator implements DataSetIterator {

    private final int VECTOR_SIZE = 1; // number of features for a stock data
    private int miniBatchSize; // mini-batch size
    private int exampleLength = 24; // default 22, say, 22 working days per month
    private int predictLength = 24; // default 1, say, one day ahead prediction

    /**
     * minimal values of each feature in stock dataset
     */
    private double[] minArray = new double[VECTOR_SIZE];
    /**
     * maximal values of each feature in stock dataset
     */
    private double[] maxArray = new double[VECTOR_SIZE];

    /**
     * mini-batch offset
     */
    private LinkedList<Integer> exampleStartOffsets = new LinkedList<>();

    /**
     * stock dataset for training
     */
    private List<InListData> train;
    /**
     * adjusted stock dataset for testing
     */
    private List<Pair<INDArray, INDArray>> test;

    public InListDataSetIterator(String filename, int miniBatchSize, int exampleLength, double splitRatio) {
        List<InListData> inListDataList = readStockDataFromDB(filename);
        this.miniBatchSize = miniBatchSize;
        this.exampleLength = exampleLength;
        int split = (int) Math.round(inListDataList.size() * splitRatio);
        train = inListDataList.subList(0, split);
        test = generateTestDataSet(inListDataList.subList(split, inListDataList.size()));
        //test = preLastDayDataSet2();
        initializeOffsets();
    }

    /**
     * initialize the mini-batch offsets
     */
    private void initializeOffsets() {
        exampleStartOffsets.clear();
        int window = exampleLength + predictLength;
        for (int i = 0; i < train.size() - window; i++) {
            exampleStartOffsets.add(i);
        }
    }

    public List<Pair<INDArray, INDArray>> getTestDataSet() {
        return test;
    }

    public double[] getMaxArray() {
        return maxArray;
    }

    public double[] getMinArray() {
        return minArray;
    }

    public double getMaxNum() {
        return maxArray[0];
    }

    public double getMinNum() {
        return minArray[0];
    }

    @Override
    public DataSet next(int num) {
        if (exampleStartOffsets.size() == 0) {
            throw new NoSuchElementException();
        }
        int actualMiniBatchSize = Math.min(num, exampleStartOffsets.size());
        INDArray input = Nd4j.create(new int[]{actualMiniBatchSize, VECTOR_SIZE, exampleLength}, 'f');
        INDArray label = Nd4j.create(new int[]{actualMiniBatchSize, predictLength, exampleLength}, 'f');

        for (int index = 0; index < actualMiniBatchSize; index++) {
            int startIdx = exampleStartOffsets.removeFirst();
            int endIdx = startIdx + exampleLength;
            InListData curData = train.get(startIdx);
            InListData nextData;
            for (int i = startIdx; i < endIdx; i++) {
                int c = i - startIdx;
                input.putScalar(new int[]{index, 0, c}, (curData.getCurrFlowCnt() - minArray[0]) / (maxArray[0] - minArray[0]));
                nextData = train.get(i + 1);
                label.putScalar(new int[]{index, 0, c}, feedLabel(nextData));
                curData = nextData;
            }
            if (exampleStartOffsets.size() == 0) break;
        }
        return new DataSet(input, label);
    }

    private double feedLabel(InListData data) {
        double value = (data.getCurrFlowCnt() - minArray[0]) / (maxArray[0] - minArray[0]);
        return value;
    }

    @Override
    public int totalExamples() {
        return train.size() - exampleLength - predictLength;
    }

    @Override
    public int inputColumns() {
        return VECTOR_SIZE;
    }

    @Override
    public int totalOutcomes() {
        return predictLength;
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
    public void reset() {
        initializeOffsets();
    }

    @Override
    public int batch() {
        return miniBatchSize;
    }

    @Override
    public int cursor() {
        return totalExamples() - exampleStartOffsets.size();
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public List<String> getLabels() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public boolean hasNext() {
        return exampleStartOffsets.size() > 0;
    }

    @Override
    public DataSet next() {
        return next(miniBatchSize);
    }

    private List<Pair<INDArray, INDArray>> generateTestDataSet(List<InListData> inListDataList) {
        //inListDataList = DBUtil.readLastDayData2();
        System.out.println(inListDataList.size());
        int window = exampleLength + predictLength;
        List<Pair<INDArray, INDArray>> test = new ArrayList<>();
        for (int i = 0; i < inListDataList.size() - window; i++) {
            INDArray input = Nd4j.create(new int[]{exampleLength, VECTOR_SIZE}, 'f');
            for (int j = i; j < i + exampleLength; j++) {
                InListData stock = inListDataList.get(j);
                input.putScalar(new int[]{j - i, 0}, (stock.getCurrFlowCnt() - minArray[0]) / (maxArray[0] - minArray[0]));
            }
            InListData stock = inListDataList.get(i + exampleLength);
            INDArray label;
            label = Nd4j.create(new int[]{1}, 'f');
            label.putScalar(new int[]{0}, stock.getCurrFlowCnt());
            test.add(new Pair<>(input, label));
        }
        return test;
    }

    public List<Pair<INDArray, INDArray>> preLastDayDataSet() {
        List<double[]> lastDayDataList = DBUtil.readLastDayData();
        List<Pair<INDArray, INDArray>> test = new ArrayList<>();
        System.out.println(lastDayDataList.size());
        for (int i = 0; i < 1; i++) {
            INDArray input = Nd4j.create(new int[]{exampleLength, VECTOR_SIZE}, 'f');
            for (int j = i; j < lastDayDataList.size(); j++) {
                double[] stock = lastDayDataList.get(j);
                input.putScalar(new int[]{j - i, 0}, (stock[8] - minArray[0]) / (maxArray[0] - minArray[0]));
            }
            double[] stock = lastDayDataList.get(i);
            INDArray label = Nd4j.create(new int[]{1}, 'f');
            label.putScalar(new int[]{0}, stock[8]);
            test.add(new Pair<>(input, label));
        }
        return test;
    }

    public List<Pair<INDArray, INDArray>> preLastDayDataSet2() {
        List<double[]> lastDayDataList = DBUtil.readLastDayData();
        List<Pair<INDArray, INDArray>> test = new ArrayList<>();
        System.out.println(lastDayDataList.size());
        INDArray input = Nd4j.create(new int[]{exampleLength, VECTOR_SIZE}, 'f');
        for (int i = 0; i < lastDayDataList.size(); i++) {
            double[] stock = lastDayDataList.get(i);
            input.putScalar(new int[]{i, 0}, (stock[8] - minArray[0]) / (maxArray[0] - minArray[0]));
        }
        INDArray label = Nd4j.create(new int[]{1}, 'f');
        test.add(new Pair<>(input, label));
        return test;
    }

    private List<InListData> readStockDataFromFile(String filename) {
        List<InListData> inListDataList = new ArrayList<>();
        try {
            for (int i = 0; i < maxArray.length; i++) { // initialize max and min arrays
                maxArray[i] = Double.MIN_VALUE;
                minArray[i] = Double.MAX_VALUE;
            }
            List<String[]> list = new CSVReader(new FileReader(filename)).readAll(); // load all elements in a list
            for (String[] arr : list) {
                //if (!arr[1].equals(symbol)) continue;
                double[] nums = new double[VECTOR_SIZE];
                for (int i = 0; i < arr.length - 8; i++) {
                    nums[i] = Double.valueOf(arr[i + 8]);
                    if (nums[i] > maxArray[i]) maxArray[i] = nums[i];
                    if (nums[i] < minArray[i]) minArray[i] = nums[i];
                }
                inListDataList.add(new InListData(nums[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inListDataList;
    }

    private List<InListData> readStockDataFromDB(String filename) {
        List<InListData> inListDataList = new ArrayList<>();
        for (int i = 0; i < maxArray.length; i++) { // initialize max and min arrays
            maxArray[i] = Double.MIN_VALUE;
            minArray[i] = Double.MAX_VALUE;
        }
        List<String[]> list = DBUtil.readTrainData(); // load all elements in a list

        for (String[] arr : list) {
            double[] nums = new double[VECTOR_SIZE];
            for (int i = 0; i < arr.length - 8; i++) {
                nums[i] = Double.valueOf(arr[i + 8]);
                if (nums[i] > maxArray[i]) maxArray[i] = nums[i];
                if (nums[i] < minArray[i]) minArray[i] = nums[i];
            }
            inListDataList.add(new InListData(nums[0]));
        }
        return inListDataList;
    }
}
