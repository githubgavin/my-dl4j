package com.udgrp.stock.utils;

import com.udgrp.stock.representation.InListData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kejw
 * @version V1.0
 * @Project my-nd4j-lstm
 * @Description: TODO
 * @date 16:11
 */
public class DBUtil {
    private static String selectsql = null;
    private static ResultSet retsult = null;
    private static int count1 = 1;
    private static int count2 = 24;
    private static final String url = "jdbc:mysql://127.0.0.1/jtt?serverTimezone=UTC&characterEncoding=utf8&useUnicode=true&useSSL=false";
    private static final String name = "com.mysql.jdbc.Driver";
    private static final String user = "root";
    private static final String password = "root";

    private static Connection conn = null;
    private static PreparedStatement pst = null;

    public static List<String[]> readTrainData() {
        int paraCount = 9; //读取参数数量
        selectsql = "select * from t_2016_in_list_count_pre where `month` between 3 and 11 order by month,day,week,inHour";//SQL语句
        try {
            Class.forName(name);//指定连接类型
            conn = DriverManager.getConnection(url, user, password);//获取连接
            pst = conn.prepareStatement(selectsql);//准备执行语句
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String[]> list = new ArrayList<>();
        try {
            retsult = pst.executeQuery();//执行语句，得到结果集
            while (retsult.next()) {
                String[] paras = new String[paraCount];
                for (int i = 0; i < paraCount; i++) {
                    paras[i] = retsult.getString(i + 1);
                }
                list.add(paras);
            }
            retsult.close();
            conn.close();//关闭连接
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<double[]> readLastDayData() {

        int paraCount = 9; //读取参数数量
        selectsql = "select * from t_2016_in_list_count_pre where `month` = 12 and day in (31) order by month,day,week,inHour";//SQL语句
        count1++;
        count2++;
        //selectsql = "select * from t_2016_in_list_count_pre order by month desc,day desc,week,inHour asc limit 24";//SQL语句
        try {
            Class.forName(name);//指定连接类型
            conn = DriverManager.getConnection(url, user, password);//获取连接
            pst = conn.prepareStatement(selectsql);//准备执行语句
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<double[]> list = new ArrayList<>();
        try {
            retsult = pst.executeQuery();//执行语句，得到结果集
            while (retsult.next()) {
                double[] paras = new double[paraCount];
                for (int i = 0; i < paraCount; i++) {
                    paras[i] = retsult.getDouble(i + 1);
                }
                list.add(paras);
            }
            retsult.close();
            conn.close();//关闭连接
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<InListData> readLastDayData2() {
        int paraCount = 9; //读取参数数量
        selectsql = "select * from t_2016_in_list_count_pre where `month` = 12 and `day` in (29,30,31) order by month,day,week,inHour";//SQL语句
        try {
            Class.forName(name);//指定连接类型
            conn = DriverManager.getConnection(url, user, password);//获取连接
            pst = conn.prepareStatement(selectsql);//准备执行语句
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<InListData> list = new ArrayList<>();
        try {
            retsult = pst.executeQuery();//执行语句，得到结果集
            while (retsult.next()) {
                InListData stock = new InListData();
                for (int i = 0; i < paraCount; i++) {
                    //stock.setLastMonCnt(retsult.getDouble(6));
                    //stock.setLastWeekCnt(retsult.getDouble(7));
                    //stock.setLastDayCnt(retsult.getDouble(8));
                    stock.setCurrFlowCnt(retsult.getDouble(9));
                }
                list.add(stock);
            }
            retsult.close();
            conn.close();//关闭连接
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void insert(double value) {
        selectsql = "insert into t_2016_in_list_count_pre_12 values('" + count2 + "','0','0','0','" + count2 + 1 + "','0','0','0','" + value + "')";//SQL语句
        System.out.println(selectsql);
        try {
            Class.forName(name);//指定连接类型
            conn = DriverManager.getConnection(url, user, password);//获取连接
            pst = conn.prepareStatement(selectsql);//准备执行语句
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            pst.executeUpdate();//执行语句
            conn.close();//关闭连接
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
