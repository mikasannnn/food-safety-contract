package com.mikasan;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.mikasan.Pojo.*;
import lombok.extern.java.Log;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.valueOf;

/**
 * @Author mikasan
 * title
 */
@Contract(name = "FootSafetyContract")  //指定合约名
@Default//默认是智能合约
@Log
public class FootSafetyContract implements ContractInterface {

    //gson 用于JSON序列化和反序列化
    private final Gson gson = new Gson();

    //加载并存储政府标准数据，这些数据在合约初始化时从standards.json文件中加载
    private static final List<GovernmentStandard> governmentStandards = GovernmentStandardLoader.loadStandards();


    //注册商家
/*
    @Transaction
    public void registerMerChant(Context context, String merchantId, String name , String governmentId) {

        //是否存在
        if(merchantExists(context,merchantId)){
            log.info("Merchant already exists: " + merchantId);
            throw  new ChaincodeException("Merchant already exists: " + merchantId);
        }
        if(name == null || name.isEmpty()){
            log.info("Merchant name is empty: " + name);
            throw new ChaincodeException("Merchant name is empty: " + name);
        }
        if (governmentId == null || governmentId.isEmpty()) {
            log.info("Government id is empty: " + governmentId);
            throw new ChaincodeException("Government id is empty: " + governmentId);
        }
        if (merchantId == null || merchantId.isEmpty()) {
            log.info("Merchant id is empty: " + merchantId);
            throw new ChaincodeException("Merchant id is empty: " + merchantId);
        }
        //不存在 -> 创建商家
        Merchant merchant = new Merchant();
        merchant.setName(name)
                .setMerchantId(merchantId)
                .setGreenCertified(false)
                .setCertificationDate("")
                .setSafetyLevel("")
                .setGovernmentId(governmentId);

        ChaincodeStub stub = context.getStub();

        //把信息存储到区块链中
//        stub.putState(merchantId, JSON.toJSONString(merchant).getBytes(StandardCharsets.UTF_8));
        stub.putStringState(merchantId, JSON.toJSONString(merchant));
        log.info("Registered Merchant successfully: " + merchantId);
    }
*/



    //提交检测报告，商家提交商家安全检测报告
    @Transaction
    public void submitInspectionReport(Context context,
                                       String reportId,
                                       String merchantId,
                                       String inspectionDate,
                                       String inspectionAgency,
                                       String safetyStandardsJson, 
                                       String safetyLevel,
                                       boolean isPassed,
                                       String governmentId) {
        try{


            log.info("----------------------------------------------------Submitting inspection report with reportId: " + reportId);
            log.info("Merchant ID: " + merchantId);
            log.info("Inspection date: " + inspectionDate);
            log.info("Inspection agency: " + inspectionAgency);
            log.info("safetyLevel: " + safetyLevel);
            log.info("isPassed: " + isPassed);
            log.info("governmentId: " + governmentId);
            log.info("Safety standards JSON: " + safetyStandardsJson);

/*            if (!merchantExists(context, merchantId)) {
                log.info("Merchant does not exist: " + merchantId);
                throw new ChaincodeException("Merchant does not exist: " + merchantId);
            }*/

            //验证 safetyStandardsJson 是否有效的JSON数组
            if (safetyStandardsJson == null || !safetyStandardsJson.startsWith("[") || !safetyStandardsJson.endsWith("]")){
                log.info("Invalid safetyStandardsJson format: " + safetyStandardsJson);
                throw new ChaincodeException("Invalid safetyStandardsJson format: " + safetyStandardsJson);
            }

            // 解析 safetyStandardsJson
            List<String> safetyStandards;
            try {
                // 将 safetyStandardsJson 解析为 List<String>
                // String.class -》 指定解析后的数据类型为 String
                safetyStandards = JSON.parseArray(safetyStandardsJson, String.class);
            } catch (Exception e) {
                log.severe("Invalid safetyStandardsJson format: " + safetyStandardsJson);
                throw new ChaincodeException("Invalid safetyStandardsJson format: " + safetyStandardsJson);
            }

            //遍历 safetyStandards 检验是否符合标准
            for (String standardId : safetyStandards) {
                boolean found = false;
                for (GovernmentStandard standard : governmentStandards) {
                    //检查每个标准是否在 governmentStandards 中存在
                    if (standard.getStandardId().equals(standardId) && standard.getGovernmentId().equals(governmentId)) {
                        found = true;

                        //检查证书是否过期
                        if (isCertificateExpired(inspectionDate, standard.getValidityPeriod())) {
                            throw new ChaincodeException(" Your " + standard.getStandardName() + " has expired and the merchant's detection did not pass.");
                        }

                        break;
                    }
                }
                if (!found) {
                    throw new ChaincodeException("Standard not found: " + standardId);
                }
            }


            // 创建检测报告对象
            InspectionReport report = new InspectionReport()
                    .setReportId(reportId)
                    .setMerchantId(merchantId)
                    .setInspectionDate(inspectionDate)
                    .setInspectionAgency(inspectionAgency)
                    .setSafetyLevel(safetyLevel)
                    .setPassed(isPassed)
                    .setGovernmentId(governmentId)
                    .setSafetyStandards(safetyStandards);
            // 存储检测报告
            ChaincodeStub stub = context.getStub();
            // 参数：reportId是键，JSON.toJSONString(report)是值
            stub.putStringState(reportId, JSON.toJSONString(report)); //将 report 对象转换为 JSON 字符串
            log.info("Inspection report successfully: " + reportId);
        }catch (Exception e){
            log.severe("Error submitting inspection report: " + e.getMessage());
            throw new ChaincodeException("Error submitting inspection report: " + e.getMessage());
        }
    }

    //证书是否过期
    private boolean isCertificateExpired(String inspectionDate, int validityPeriod) {
        //根据检测日期和有效期（月数）计算证书的截止日期
        try {
            // 解析检测日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            //将字符串格式的检测日期解析为 Date 对象。
            Date inspectionDateObj = sdf.parse(inspectionDate);

            // 计算有效期截止日期
            // 获取当前日期的 Calendar 实例
            Calendar calendar = Calendar.getInstance();
            // 把inspectionDateObj 设置为 Calendar 的时间
            calendar.setTime(inspectionDateObj);
            // 增加有效期
            calendar.add(Calendar.MONTH, validityPeriod);
            // 获取计算后的截止日期 expirationDate
            Date expirationDate = calendar.getTime();

            // 获取当前日期
            Date currentDate = new Date();

            // 判断是否过期
            // 如果当前日期在截止日期之后，返回 true（表示过期）
            return currentDate.after(expirationDate);
        } catch (Exception e) {
            log.severe("Error parsing inspection date: " + e.getMessage());
            throw new ChaincodeException("Error parsing inspection date: " + e.getMessage());
        }
    }



    //提交食品检测报告，增加报告的权威性
    @Transaction
    public String submitFoodInspectionReport(Context context,
                                           String foodId,
                                           String merchantId,
                                           String inspectionDate,
                                           String inspectionAgency,
                                           String SafetyLevel,
                                           boolean isPassed,
                                           String complaintId,
                                           String governmentId
                                           ){
        //检查商家、foodId是否存在
        /*!merchantExists(context, merchantId) && (*/
        if (foodId == null || foodId.isEmpty()) {
            log.info("Merchant does not exist or Food ID is empty:"+ foodId);
            throw new ChaincodeException(
                    "Food ID is empty" + foodId);

        }

        log.info("Submitting food report with reportId: " + foodId);
        log.info("Merchant ID: " + merchantId);
        log.info("Inspection date: " + inspectionDate);
        log.info("Inspection agency: " + inspectionAgency);
        log.info("Safety level: " + SafetyLevel);
        log.info("Passed: " + isPassed);
        log.info("Complaint ID: " + complaintId);
        log.info("Government ID: " + governmentId);

        try{
            //创建食品检测报告
            FoodInspectionReport report = new FoodInspectionReport();
            report.setFoodId(foodId)
                    .setMerchantId(merchantId)
                    .setInspectionDate(inspectionDate)
                    .setInspectionAgency(inspectionAgency)
                    .setSafetyLevel(SafetyLevel)
                    .setPassed(isPassed)
                    .setComplaintId(complaintId)
                    .setGovernmentId(governmentId);

            //存食品报告对象
            ChaincodeStub stub = context.getStub();
            stub.putStringState(foodId, JSON.toJSONString(report));
            log.info("Food inspection foodReport submitted: " + foodId);
            return foodId;

        }catch (Exception e){
            log.severe("Error submitting food inspection report: " + e.getMessage());
            throw new ChaincodeException("Error submitting food inspection report: " + e.getMessage());
        }

    }


    //批准绿色认证，根据检测报告批准商家的绿色认证
    @Transaction
    public void approveGreenCertification(Context context, String merchantId, String reportId,String foodId,boolean isApproved) {
        try {
            log.info("Approving green certification for merchant: " + merchantId + ",report: " + reportId);

/*            //判断商家是否存在
            if (!merchantExists(context, merchantId)) {
                log.info("Merchant does not exist: " + merchantId);
                throw new ChaincodeException("Merchant does not exist: " + merchantId);
            }*/


            //获取检测报告
            InspectionReport report = getInspectionReport(context, reportId);
            //判断商家报告是否通过
            if (isApproved && !report.isPassed()) {
                log.info("Report not passed: " + report.getReportId());
                throw new ChaincodeException("Report not passed: " + report.getReportId());
            }
            //如果 foodId 不为空，检查食品检测报告是否通过
            if(foodId !=null && !foodId.isEmpty()){
                //存在|不为空  ->  获取食报对象
                FoodInspectionReport foodReport = getFoodInspectionReport(context,foodId);
                //食品报告是否通过
            if (isApproved && !foodReport.isPassed()) {
                log.info("Merchant report is passed, but food report not passed: " + foodId);
                throw new ChaincodeException("Merchant report is passed, but food report not passed: " + foodId);
            }


            }

/*
            //获取商家信息
            Merchant merchant = getMerchant(context, merchantId);
            //更新商家信息
            if (isApproved) {
                merchant.setGreenCertified(true);
                merchant.setCertificationDate(report.getInspectionDate())
                        .setSafetyLevel(report.getSafetyLevel());
            }else {
                merchant.setGreenCertified(false);
                merchant.setCertificationDate("")   //清空认证记录：事件等级
                        .setSafetyLevel("");
                log.info("Green certification is not approved for merchant: " + merchantId);
                throw new ChaincodeException("Green certification is not approved for merchant: " + merchantId);
            }

            //把信息更新到区块链中
            ChaincodeStub stub = context.getStub();
            stub.putState(merchantId, JSON.toJSONString(merchant).getBytes(StandardCharsets.UTF_8));
            log.info("Updated merchant info : " + JSON.toJSONString(merchant));
*/

            log.info("Green certification approved for merchant: " + merchantId);
        } catch (Exception e) {
            log.severe("Error submitting inspection report: " + e.getMessage());
            throw new ChaincodeException("Error approving green certification: " + e.getMessage());
        }

    }

    private FoodInspectionReport getFoodInspectionReport(Context context, String foodId) {
        ChaincodeStub stub = context.getStub();
        String stringState = stub.getStringState(foodId);
        if(stringState == null || stringState.isEmpty()) {
            log.info("Merchant does not exist: " + foodId);
            throw new ChaincodeException("Merchant does not exist: " + foodId);
        }
        return JSON.parseObject(stringState, FoodInspectionReport.class);
    }


    //查询商家信息
/*    @Transaction
    public String queryMerchantInfo(Context context, String merchantId) {
        //查询商家信息
        ChaincodeStub stub = context.getStub();
        String stringState = stub.getStringState(merchantId);
        if(stringState == null || stringState.isEmpty()) {
            log.info("Merchant does not exist: " + merchantId);
            throw new ChaincodeException("Merchant does not exist: " + merchantId);
        }
        return stringState;
    }*/

    //查询食品报告
    @Transaction
    public String queryFoodReports(Context context, String merchantId) {

        //构建查询
        //使用 CouchDB 的富查询语法，查询 merchantId 匹配的记录
        String queryString = String.format("{\"selector\" : {\"merchantId\" : \"%s\"}}", merchantId);
        //执行查询
        ChaincodeStub stub = context.getStub();
        QueryResultsIterator<KeyValue> results = stub.getQueryResult(queryString);
        //封装查询结果
        List<FoodInspectionReport> reportList = new ArrayList<>();
        //遍历查询结果，将每个结果的json字符串解析为FoodInspectionReport对象 并存储在reportList中
        for(KeyValue result : results){
            String jsonValue = result.getStringValue();
            if(jsonValue != null && !jsonValue.isEmpty()){
                reportList.add(JSON.parseObject(jsonValue,FoodInspectionReport.class));
            }
        }
        //返回 JSON 格式的查询结果
        return JSON.toJSONString(reportList);

    }



    //查询检测报告 -> 查询某个商家的所有检测报告 ---
    @Transaction
    public String queryInspectionReports(Context context, String merchantId) {
        //构建富查询----考虑分页查询
        String queryString = String.format("{\"selector\" : {\"merchantId\" : \"%s\"}}", merchantId);

        ChaincodeStub stub = context.getStub();
        QueryResultsIterator<KeyValue> results = stub.getQueryResult(queryString);
        //封装查询结果
        List<InspectionReport> reportList = new ArrayList<>();
        for(KeyValue result : results){
            String jsonValue = result.getStringValue();
            if(jsonValue != null && !jsonValue.isEmpty()){
                reportList.add(JSON.parseObject(jsonValue,InspectionReport.class));
            }
        }
        //返回 JSON 格式的查询结果
        return JSON.toJSONString(reportList);
    }


    // 查询政府标准，某个政府
    @Transaction
    public String queryGovernmentStandards(Context context, String governmentId) {
/*        ChaincodeStub stub = context.getStub();

        //构建富查询
        String queryString = String.format("{\"selector\" : {\"governmentId\" : \"%s\"}}", governmentId);

        QueryResultsIterator<KeyValue> results = stub.getQueryResult(queryString);

        List<GovernmentStandard> standardList = new ArrayList<>();
        for(KeyValue result : results){
            String jsonValue = result.getStringValue();
            if(jsonValue != null && !jsonValue.isEmpty()){
                standardList.add(JSON.parseObject(jsonValue,GovernmentStandard.class));
            }
        }
        //返回 JSON 格式的查询结果
        return JSON.toJSONString(standardList);*/


        List<GovernmentStandard> result = new ArrayList<>();
        for (GovernmentStandard standard :governmentStandards){
            if(standard.getGovernmentId().equals(governmentId)){
                result.add(standard);
            }
        }
        return JSON.toJSONString(result);

    }



    @Transaction
    public String queryHistoryForKey(Context context, String key) {
        try{
            ChaincodeStub stub = context.getStub();
            log.info("Querying history for key: " + key);

            //查询历史记录
            QueryResultsIterator<KeyModification> history = stub.getHistoryForKey(key);

            //封装结果
            List<HistoryEntry> historyEntries = new ArrayList<>();
            //遍历历史记录，将每条记录封装为 HistoryEntry 对象。
            for (KeyModification keyModification : history) {
                HistoryEntry entry = new HistoryEntry();
                entry.setTxId(keyModification.getTxId())
                        .setTimestamp(keyModification.getTimestamp().toString())
                        .setValue(keyModification.getStringValue())
                        .setIsDelete(keyModification.isDeleted());
                historyEntries.add(entry);


            }
            //返回json
            log.info("History query results: " + JSON.toJSONString(historyEntries));
            return JSON.toJSONString(historyEntries);
        }catch (Exception e){
            log.severe("Error querying history for key: " + key + ", error: " + e.getMessage());
            throw new ChaincodeException("Error querying history for key: " + key + ", error: " + e.getMessage());
        }
    }


/*    //绿色认证状态溯源
    @Transaction
    public String queryMerChantCertificationHistory(Context context, String merchantId) {
        return queryHistoryForKey(context, merchantId);
    }*/

    //食品检查报告溯源
    @Transaction
    public String queryFoodReportHistory(Context context, String foodId) {

        try{
            log.info("Querying history for food id: " + foodId);
            return queryHistoryForKey(context, foodId);
        }catch (Exception e){
            log.severe("Error querying history for food id: " + foodId + ", error: " + e.getMessage());
            throw new ChaincodeException("Error querying history for key: " + foodId + ", error: " + e.getMessage());
        }

    }


    //商家检测报告溯源
    @Transaction
    public String queryInspectionReportHistory(Context context, String reportId) {
        try {
            log.info("Querying inspection report history for report: " + reportId);
            return queryHistoryForKey(context, reportId);
        } catch (Exception e) {
            log.severe("Error querying inspection report history: " + e.getMessage());
            throw new ChaincodeException("Error querying inspection report history: " + e.getMessage());
        }
    }


    //获取商家信息
    private Merchant getMerchant(Context context, String merchantId) {
        ChaincodeStub stub = context.getStub();
        
        String stringState = stub.getStringState(merchantId);
        if(stringState == null || stringState.isEmpty()) {
            log.info("Merchant does not exist: " + merchantId);
            throw new ChaincodeException("Merchant does not exist: " + merchantId);

        }
        return JSON.parseObject(stringState, Merchant.class);
    }


    //获取检查报告
    private InspectionReport getInspectionReport(Context context, String reportId) {
        ChaincodeStub stub = context.getStub();
        String stringState = stub.getStringState(reportId);
        if(stringState == null || stringState.isEmpty()) {
            log.info("Report not passed: " + reportId);
            throw new ChaincodeException("Report not passed: " + reportId);
        }
        return JSON.parseObject(stringState, InspectionReport.class);
    }


    //商家是否存在
//    private boolean merchantExists(Context context, String merchantId) {
//        byte[] merchantBytes = context.getStub().getState(merchantId);
//
//        return merchantBytes != null && merchantBytes.length > 0;
//    }



    @Override
    public void beforeTransaction(Context ctx) {
        System.out.println("beforeTransaction-------->DataContract");
    }


    @Override
    public void afterTransaction(Context ctx, Object result) {
        System.out.println("afterTransaction-------->DataContract--------->result=" + result);
    }
}
