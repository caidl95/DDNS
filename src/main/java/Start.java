import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;

import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取当前主机公网IP
 */
public class Start {

    public static String qingdao = "cn-qingdao"; //地域ID 默认
    public static String accessKeyId = "";//阿里云生成的AccessKeyId
    public static String accessKeySecret = "";//阿里云生成的AccessKeySecret

    public static void main(String[] args) {
        timerRun();
    }

    public static void timerRun() {
        // 一个小时的毫秒数
        long daySpan = 60 * 60 * 1000;
        // 规定的每天时间8:30:00运行
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 8:30:00");
        // 首次运行时间
        try {
            Date startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sdf.format(new Date()));
            // 如果今天的已经过了 首次运行时间就改为明天
            if (System.currentTimeMillis() > startTime.getTime()){
                startTime = new Date(startTime.getTime() + daySpan);
            }
            Timer t = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    start();
                    System.out.print("定时器执行 "+new Date());
                }
            };
            // 以每24小时执行一次
            t.schedule(task, startTime, daySpan);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void start(){
        // 设置鉴权参数，初始化客户端
        DefaultProfile profile = DefaultProfile.getProfile(qingdao,// 地域ID
                accessKeyId ,// 您的AccessKey ID
                accessKeySecret);// 您的AccessKey Secret
        IAcsClient client = new DefaultAcsClient(profile);
        // 查询指定二级域名的最新解析记录
        DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
        // 主域名
        describeDomainRecordsRequest.setDomainName("caidl.top");
        // 主机记录
        describeDomainRecordsRequest.setRRKeyWord("www");
        // 解析记录类型
        describeDomainRecordsRequest.setType("A");
        DescribeDomainRecordsResponse describeDomainRecordsResponse = describeDomainRecords(describeDomainRecordsRequest, client);
        log_print("describeDomainRecords",describeDomainRecordsResponse);
        List<DescribeDomainRecordsResponse.Record> domainRecords = describeDomainRecordsResponse.getDomainRecords();
        // 最新的一条解析记录
        DescribeDomainRecordsResponse.Record record = domainRecords.get(0);
        // 记录ID
        String recordId = record.getRecordId();
        // 记录值
        String recordsValue = record.getValue();
        // 当前主机公网IP
        String currentHostIP = getCurrentHostIP();
        System.out.println("-------------------------------当前主机公网IP为："+currentHostIP+"-------------------------------");
        if(!currentHostIP.equals(recordsValue)){
            // 修改解析记录
            UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
            // 主机记录
            updateDomainRecordRequest.setRR("www");
            // 记录ID
            updateDomainRecordRequest.setRecordId(recordId);
            // 将主机记录值改为当前主机IP
            updateDomainRecordRequest.setValue(currentHostIP);
            //解析记录类型
            updateDomainRecordRequest.setType("A");
            UpdateDomainRecordResponse updateDomainRecordResponse = updateDomainRecord(updateDomainRecordRequest, client);
            log_print("updateDomainRecord",updateDomainRecordResponse);
        }
    }

    public static void log_print(String functionName, Object result) {
        Gson gson = new Gson();
        System.out.println("-------------------------------" + functionName + "-------------------------------");
        System.out.println(gson.toJson(result));
    }

    /**
     * 获取主域名的所有解析记录列表
     */
    public static DescribeDomainRecordsResponse describeDomainRecords(DescribeDomainRecordsRequest request, IAcsClient client){
        try {
            // 调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
            // 发生调用错误，抛出运行时异常
            throw new RuntimeException();
        }
    }


    /**
     * 修改解析记录
     */
    public static UpdateDomainRecordResponse updateDomainRecord(UpdateDomainRecordRequest request, IAcsClient client){
        try {
            // 调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
            // 发生调用错误，抛出运行时异常
            throw new RuntimeException();
        }
    }

    /**
     * 获取当前主机公网IP
     */
    public  static String getCurrentHostIP(){
        // 这里使用jsonip.com第三方接口获取本地IP
        String jsonip = "https://jsonip.com/";
        // 接口返回结果
        String result = "";
        BufferedReader in = null;
        try {
            // 使用HttpURLConnection网络请求第三方接口
            URL url = new URL(jsonip);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null)
                    in.close();
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

}
