package com.udgrp.emotion;

import com.udgrp.classify.ClassifyUIExampleUtils;
import com.udgrp.classify.NewsIterator;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class EmotionUIExample {
    public static WordVectors wordVectors;

    public static void main(String[] args) throws IOException {

        //Get our network and training data
        wordVectors = WordVectorSerializer.readWord2VecModel(new File("C:\\unionwork\\tf\\wiki.zh.text.vector"));
        int inputNeurons = wordVectors.getWordVector(wordVectors.vocab().wordAtIndex(0)).length; // 100 in our case

        HashMap<String, EmotionIterator> dataMap = EmotionUIExampleUtils.getData(wordVectors);
        EmotionIterator iTrain = dataMap.get("train");
        EmotionIterator iTest = dataMap.get("test");
        MultiLayerNetwork net = EmotionUIExampleUtils.getNetwork(inputNeurons, iTrain.getLabels().size());

        //Initialize the user interface backend
        UIServer uiServer = UIServer.getInstance();

        //Configure where the network information (gradients, activations, score vs. time etc) is to be stored
        //Then add the StatsListener to collect this information from the network, as it trains
        StatsStorage statsStorage = new InMemoryStatsStorage();             //Alternative: new FileStatsStorage(File) - see UIStorageExample
        int listenerFrequency = 1;
        net.setListeners(new StatsListener(statsStorage, listenerFrequency));

        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        uiServer.attach(statsStorage);

        //Start training:
        EmotionUIExampleUtils.train(net,iTrain,iTest);

        //Finally: open your browser and go to http://localhost:9000/train
    }
}
