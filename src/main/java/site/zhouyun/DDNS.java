package site.zhouyun;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态域名解析
 */
public class DDNS {

    public static Integer period = 10;
    private static String REGION_ID ;
    private static String ACCESS_KEY_ID ;
    private static String SECRET;
    private static String DOMAIN ;
    private static String ADDRESS_API = "https://jsonip.com/";
    private static IAcsClient CLIENT;
    private static DDNS ddns;

    /**
     * 静态代码块， 类加载时加载， 仅加载一次
     * 初始化配置
     */

    static
    {
        // 使用InPutStream流读取properties文件
        Properties properties = new Properties();
        // 使用ClassLoader加载properties配置文件生成对应的输入流

        try {
            InputStream in = DDNS.class.getClassLoader().getResourceAsStream("config.properties");
            // 使用properties对象加载输入流
            properties.load(in);
            // 获取key对应的value值
            period = Integer.valueOf(properties.getProperty("Period"));
            REGION_ID = properties.getProperty("Region_Id");
            ACCESS_KEY_ID = properties.getProperty("Access_Key_Id");
            SECRET = properties.getProperty("Secret");
            DOMAIN = properties.getProperty("Domain");
            ADDRESS_API = properties.getProperty("Address_Api");

            // 设置鉴权参数，初始化客户端
            DefaultProfile profile = DefaultProfile.getProfile(
                    REGION_ID,// 地域ID
                    ACCESS_KEY_ID,// 您的AccessKey ID
                    SECRET);// 您的AccessKey Secret
            CLIENT = new DefaultAcsClient(profile);
            ddns = new DDNS();

        } catch (FileNotFoundException e) {
            System.err.println("配置文件未找到");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取主域名的所有解析记录列表
     */
    private DescribeDomainRecordsResponse describeDomainRecords(DescribeDomainRecordsRequest request, IAcsClient client){
        try {
            // 调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            //e.printStackTrace();
            System.err.println(e.getMessage());
            // 发生调用错误，抛出运行时异常
            throw new RuntimeException();
        }
    }

    /**
     * 获取当前主机公网IP
     */
    private String getCurrentHostIP(){
        // 这里使用jsonip.com第三方接口获取本地IP
        String jsonip = ADDRESS_API;
        // 接口返回结果
        String result = "";
        BufferedReader in = null;
        try {
            // 使用HttpURLConnection网络请求第三方接口
            URL url = new URL(jsonip);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            in = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            // e.printStackTrace();
            System.err.println(e.getMessage());
            return "";
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }

        }
        // 正则表达式，提取xxx.xxx.xxx.xxx，将IP地址从接口返回结果中提取出来
        String rexp = "(\\d{1,3}\\.){3}\\d{1,3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(result);
        String res="";
        while (mat.find()) {
            res=mat.group();
            break;
        }
        return res;
    }

    /**
     * 修改解析记录
     */
    private UpdateDomainRecordResponse updateDomainRecord(UpdateDomainRecordRequest request, IAcsClient client){
        try {
            // 调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            //e.printStackTrace();
            System.err.println(e.getMessage());
            // 发生调用错误，抛出运行时异常
            throw new RuntimeException();
        }
    }

    private static void log_print(String functionName, Object result) {
        Gson gson = new Gson();
        System.out.println("-------------------------------" + functionName + "-------------------------------");
        System.out.println(gson.toJson(result));
    }

    /**
     * 更新记录值
     */
    private static void update (String currentHostIP, String rr, String recordId) throws Exception
    {
        UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
        // 主机记录
        updateDomainRecordRequest.setRR(rr);
        // 记录ID
        updateDomainRecordRequest.setRecordId(recordId);
        // 将主机记录值改为当前主机IP
        updateDomainRecordRequest.setValue(currentHostIP);
        // 解析记录类型
        updateDomainRecordRequest.setType("A");
        UpdateDomainRecordResponse updateDomainRecordResponse = ddns.updateDomainRecord(updateDomainRecordRequest, CLIENT);
        log_print("updateDomainRecord",updateDomainRecordResponse);
    }

    /**
     * 查询记录值
     */
    private static String select (String rr) throws Exception
    {
        // 查询指定二级域名的最新解析记录
        DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
        // 主域名
        describeDomainRecordsRequest.setDomainName(DOMAIN);
        // 主机记录
        describeDomainRecordsRequest.setRRKeyWord(rr);
        // 解析记录类型
        describeDomainRecordsRequest.setType("A");
        DescribeDomainRecordsResponse describeDomainRecordsResponse = ddns.describeDomainRecords(describeDomainRecordsRequest, CLIENT);
        log_print("describeDomainRecords",describeDomainRecordsResponse);

        List<DescribeDomainRecordsResponse.Record> domainRecords = describeDomainRecordsResponse.getDomainRecords();
        // 最新的一条解析记录
        if(domainRecords.size() != 0 ) {
            DescribeDomainRecordsResponse.Record record = domainRecords.get(0);
            // 记录ID
            String recordId = record.getRecordId();
            // 记录值
            String recordsValue = record.getValue();
            return recordsValue + ":" + recordId;
        }
        return "";
    }

    public static void  task()
    {
        // 当前主机公网IP
        String currentHostIP = "";
        while ("".equals(currentHostIP))
        {
            // 当前主机公网IP
            currentHostIP = ddns.getCurrentHostIP();
            System.out.println("-------------------------------当前主机公网IP为："+currentHostIP+"-------------------------------");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try
        {
            // @记录
            String[] recordsValues = select("@").split(":");
            String recordsValue = recordsValues[0];
            String recordId = recordsValues[1];
            if(!currentHostIP.equals(recordsValue))
            {
                update(currentHostIP, "@", recordId);
            }

            // www记录
            recordsValues = select("www").split(":");
            recordsValue = recordsValues[0];
            recordId = recordsValues[1];
            if(!currentHostIP.equals(recordsValue))
            {
                update(currentHostIP, "www", recordId);

            }
        }catch (Exception e)
        {
            System.err.println(e.getMessage());
            return;
        }

    }
}