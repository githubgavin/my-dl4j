package com.udgrp.lstm;

import com.udgrp.regress.StockDataIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class ExListFlowPredict {
    private static final int IN_NUM = 4;
    private static final int OUT_NUM = 1;
    private static final int Epochs = 1000;

    private static final int lstmLayer1Size = 50;
    private static final int lstmLayer2Size = 100;

    public static MultiLayerNetwork getNetModel(int nIn, int nOut) {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(1)
                .learningRate(0.00012)
                .seed(12345)
                .regularization(true)
                .l2(1e-5)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.RMSPROP)
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(nIn).nOut(lstmLayer1Size).activation(Activation.SIGMOID).build())
                .layer(1, new GravesLSTM.Builder().nIn(lstmLayer1Size).nOut(lstmLayer2Size).activation(Activation.TANH).build())
                .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.L2).activation(Activation.IDENTITY).nIn(lstmLayer2Size).nOut(nOut).build())
                .pretrain(false)
                .backprop(true)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        return net;
    }

    public static void train(MultiLayerNetwork net, ExListFlowIterator iterator) {
        //迭代训练
        for (int i = 0; i < Epochs; i++) {
            DataSet dataSet = null;
            while (iterator.hasNext()) {
                dataSet = iterator.next();
                net.fit(dataSet);
            }
            iterator.reset();
            System.out.println();
            System.out.println("=================>完成第" + i + "次完整训练");
            INDArray initArray = getInitArray(iterator);

            System.out.println("预测结果：");
            for (int j = 0; j < 24; j++) {
                INDArray output = net.rnnTimeStep(initArray);
                System.out.println(output.getDouble(0) * iterator.getMaxArr()[0] + " ");
            }
            System.out.println();
            net.rnnClearPreviousState();
        }
    }

    private static INDArray getInitArray(ExListFlowIterator iter) {
        double[] maxNums = iter.getMaxArr();
        INDArray initArray = Nd4j.zeros(1,4, 1);

        initArray.putScalar(new int[]{0, 0, 0}, 139 / maxNums[0]);
        initArray.putScalar(new int[]{0, 1, 0}, 163 / maxNums[1]);
        initArray.putScalar(new int[]{0, 2, 0}, 162 / maxNums[2]);
        initArray.putScalar(new int[]{0, 3, 0}, 142 / maxNums[3]);
        return initArray;
    }
    public static void main(String[] args) {
        String inputFile = ExListFlowPredict.class.getClassLoader().getResource("exlistpredict/train/2016_in_list_data.csv").getPath();
        int batchSize = 1;
        int exampleLength = 48;
        //初始化深度神经网络
        ExListFlowIterator iterator = new ExListFlowIterator();
        iterator.loadData(inputFile, batchSize, exampleLength);

        MultiLayerNetwork net = getNetModel(IN_NUM, OUT_NUM);
        train(net, iterator);
    }

}
