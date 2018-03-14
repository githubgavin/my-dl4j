import com.udgrp.stock.representation.InListData;
import org.junit.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestDB {

    static String selectsql = null;
    static ResultSet retsult = null;

    public static final String url = "jdbc:mysql://127.0.0.1/jtt?serverTimezone=UTC&characterEncoding=utf8&useUnicode=true&useSSL=false";
    public static final String name = "com.mysql.jdbc.Driver";
    public static final String user = "root";
    public static final String password = "root";

    public static Connection conn = null;
    public static PreparedStatement pst = null;

    public static void main(String[] args) {
        int paraCount = 9; //读取参数数量
        selectsql = "select * from t_2016_in_list_count_pre where month > 2 order by month,day,week,inHour";//SQL语句

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
                String[] paras = paras = new String[paraCount];
                for (int i = 0; i < paraCount; i++) {
                    paras[i] = retsult.getString(i + 1);
                }
                System.out.println(Arrays.toString(paras));
                list.add(paras);
            }//显示数据
            System.out.println(Arrays.toString(list.toArray()));
            retsult.close();
            conn.close();//关闭连接
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readLastDayData2() {
        int paraCount = 9; //读取参数数量
        selectsql = "select * from t_2016_in_list_count_pre where `month` = 12 and `day` in (30,31) order by month,day,week,inHour";//SQL语句
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
                    //stock.setLastMonCnt(retsult.getDouble(5));
                    //stock.setLastWeekCnt(retsult.getDouble(6));
                    //stock.setLastDayCnt(retsult.getDouble(7));
                    stock.setCurrFlowCnt(retsult.getDouble(8));
                }
                list.add(stock);
            }
            retsult.close();
            conn.close();//关闭连接
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //return list;
    }

    @Test
    public void insert() {
        String value = "200";
        selectsql = "insert into t_2016_in_list_count_pre_12 values('12','1','1','99','99','0','0','0','" + value + "')";//SQL语句
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
        //return list;
    }
}