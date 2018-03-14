package com.udgrp.emotion;

import com.udgrp.classify.NewsIterator;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Alex on 11/11/2016.
 */
public class EmotionUIExampleUtils {
    public static String userDirectory = "";
    public static String DATA_PATH = "";
    public static String WORD_VECTORS_PATH = "";
    private static TokenizerFactory tokenizerFactory;

    private static int batchSize = 55;     //Number of examples in each minibatch
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
                .learningRate(0.0015)
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

    public static HashMap<String, EmotionIterator> getData(WordVectors wordVectors) throws FileNotFoundException {
        userDirectory = new ClassPathResource("Emotion").getFile().getAbsolutePath() + File.separator;
        DATA_PATH = userDirectory;
        //WORD_VECTORS_PATH = userDirectory + "NewsWordVector.txt";
        TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
        tokenizerFactory.setTokenPreProcessor(new CommonPreprocessor());
        EmotionIterator iTrain = new EmotionIterator.Builder()
                .dataDirectory(DATA_PATH)
                .wordVectors(wordVectors)
                .batchSize(batchSize)
                .truncateLength(truncateReviewsToLength)
                .tokenizerFactory(tokenizerFactory)
                .train(true)
                .build();

        EmotionIterator iTest = new EmotionIterator.Builder()
                .dataDirectory(DATA_PATH)
                .wordVectors(wordVectors)
                .batchSize(batchSize)
                .tokenizerFactory(tokenizerFactory)
                .truncateLength(truncateReviewsToLength)
                .train(false)
                .build();
        HashMap map = new HashMap<String, EmotionIterator>();
        map.put("train", iTrain);
        map.put("test", iTest);
        return map;
    }

    public static void train(MultiLayerNetwork net, EmotionIterator iTrain, EmotionIterator iTest) throws IOException {
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
                ModelSerializer.writeModel(net, "C:\\unionwork\\code\\my-nd4j-lstm\\src\\main\\resources\\Emotion\\EmotionModel.model", true);
            }
        }
        //ModelSerializer.writeModel(net, userDirectory + "NewsModel.net", true);
        ModelSerializer.writeModel(net, "C:\\unionwork\\code\\my-nd4j-lstm\\src\\main\\resources\\Emotion\\EmotionModel_2.model", true);
        System.out.println("----- Example complete -----");
    }

}
