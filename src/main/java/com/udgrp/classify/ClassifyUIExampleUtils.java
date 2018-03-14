package com.udgrp.classify;

import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Alex on 11/11/2016.
 */
public class ClassifyUIExampleUtils {
    public static String userDirectory = "";
    public static String DATA_PATH = "";
    public static String WORD_VECTORS_PATH = "";
    private static TokenizerFactory tokenizerFactory;

    private static int batchSize = 58;     //Number of examples in each minibatch
    private static int nEpochs = 100;        //Number of epochs (full passes of training data) to train on
    private static int truncateReviewsToLength = 300;  //Truncate reviews with length (# words) greater than this

    public static MultiLayerNetwork getNetwork(int inputNeurons, int outputs) {

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(1)
                .updater(Updater.RMSPROP)
                .regularization(true)
                .l2(1e-5)
                .weightInit(WeightInit.XAVIER)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                .gradientNormalizationThreshold(1.0)
                .learningRate(0.005)
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(inputNeurons).nOut(200).activation(Activation.SOFTSIGN).build())
                .layer(1, new RnnOutputLayer.Builder().activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).nIn(200).nOut(outputs).build())
                .pretrain(false)
                .backprop(true)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        return net;
    }

    public static HashMap<String, NewsIterator> getData(WordVectors wordVectors) throws FileNotFoundException {
        userDirectory = new ClassPathResource("NewsData").getFile().getAbsolutePath() + File.separator;
        DATA_PATH = userDirectory + "LabelledNews";
        WORD_VECTORS_PATH = userDirectory + "NewsWordVector.txt";
        TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
        tokenizerFactory.setTokenPreProcessor(new CommonPreprocessor());
        NewsIterator iTrain = new NewsIterator.Builder()
                .dataDirectory(DATA_PATH)
                .wordVectors(wordVectors)
                .batchSize(batchSize)
                .truncateLength(truncateReviewsToLength)
                .tokenizerFactory(tokenizerFactory)
                .train(true)
                .build();

        NewsIterator iTest = new NewsIterator.Builder()
                .dataDirectory(DATA_PATH)
                .wordVectors(wordVectors)
                .batchSize(batchSize)
                .tokenizerFactory(tokenizerFactory)
                .truncateLength(truncateReviewsToLength)
                .train(false)
                .build();
        HashMap map = new HashMap<String, NewsIterator>();
        map.put("train", iTrain);
        map.put("test", iTest);
        return map;
    }

    public static void train(MultiLayerNetwork net, NewsIterator iTrain, NewsIterator iTest) throws IOException {
        System.out.println("Starting training");
        for (int i = 0; i < nEpochs; i++) {
            net.fit(iTrain);
            iTrain.reset();
            System.out.println("Epoch " + i + " complete. Starting evaluation:");

            //Run evaluation. This is on 25k reviews, so can take some time
            Evaluation evaluation = net.evaluate(iTest);

            System.out.println(evaluation.stats());
            if (evaluation.f1() >= 0.95) {
                System.out.println(evaluation.f1() + "----- Example complete -----");
                ModelSerializer.writeModel(net, "C:\\unionwork\\code\\my-nd4j-lstm\\src\\main\\resources\\NewsData\\NewsModel.model", true);
            }
        }
        //ModelSerializer.writeModel(net, userDirectory + "NewsModel.net", true);
        ModelSerializer.writeModel(net, "C:\\unionwork\\code\\my-nd4j-lstm\\src\\main\\resources\\NewsData\\NewsModel_2.model", true);
        System.out.println("----- Example complete -----");
    }

}
