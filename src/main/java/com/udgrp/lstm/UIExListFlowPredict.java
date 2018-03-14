package com.udgrp.lstm;

import com.udgrp.regress.LSTMPredict;
import com.udgrp.regress.StockDataIterator;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;

public class UIExListFlowPredict {
    private static final int IN_NUM = 4;
    private static final int OUT_NUM = 1;

    public static void main(String[] args) {
        String inputFile = LSTMPredict.class.getClassLoader().getResource("exlistpredict/train/2016_in_list_data.csv").getPath();
        int batchSize = 1;
        int exampleLength = 24;
        //初始化深度神经网络
        ExListFlowIterator iterator = new ExListFlowIterator();
        iterator.loadData(inputFile, batchSize, exampleLength);

        MultiLayerNetwork net = ExListFlowPredict.getNetModel(IN_NUM, OUT_NUM);

        //Initialize the user interface backend
        UIServer uiServer = UIServer.getInstance();

        //Configure where the network information (gradients, activations, score vs. time etc) is to be stored
        //Then add the StatsListener to collect this information from the network, as it trains
        //Alternative: new FileStatsStorage(File) - see UIStorageExample
        StatsStorage statsStorage = new InMemoryStatsStorage();
        int listenerFrequency = 1;
        net.setListeners(new StatsListener(statsStorage, listenerFrequency));

        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        uiServer.attach(statsStorage);
        ExListFlowPredict.train(net, iterator);

        //Finally: open your browser and go to http://localhost:9000/train
    }
}
