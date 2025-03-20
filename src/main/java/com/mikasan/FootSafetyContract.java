package com.mikasan;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.mikasan.Pojo.GovernmentStandard;
import com.mikasan.Pojo.HistoryEntry;
import com.mikasan.Pojo.InspectionReport;
import com.mikasan.Pojo.Merchant;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author mikasan
 * title
 */
@Contract(name = "FootSafetyContract")
@Default//默认是智能合约
@Log
public class FootSafetyContract implements ContractInterface {

    private final Gson gson = new Gson();

    private static final List<GovernmentStandard> governmentStandards = GovernmentStandardLoader.loadStandards();


    //注册商家
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

    //提交检测报告，商家提交食品安全检测报告

    @Transaction
    public void submitInspectionReport(Context context, String reportId, String merchantId, String inspectionDate, String inspectionAgency, String safetyStandardsJson, String safetyLevel, boolean isPassed, String governmentId) {
        try{


            log.info("Submitting inspection report with reportId: " + reportId);
            log.info("Merchant ID: " + merchantId);
            log.info("Inspection date: " + inspectionDate);
            log.info("Inspection agency: " + inspectionAgency);
            log.info("Safety standards JSON: " + safetyStandardsJson);

            if (!merchantExists(context, merchantId)) {
                log.info("Merchant does not exist: " + merchantId);
                throw new ChaincodeException("Merchant does not exist: " + merchantId);
            }

            //验证 safetyStandardsJson 是否有效的JSON数组
            if (safetyStandardsJson == null || !safetyStandardsJson.startsWith("[") || !safetyStandardsJson.endsWith("]")){
                log.info("Invalid safetyStandardsJson format: " + safetyStandardsJson);
                throw new ChaincodeException("Invalid safetyStandardsJson format: " + safetyStandardsJson);
            }

            //解析 JSON 数组字符串
            List<String> safetyStandards = JSON.parseArray(safetyStandardsJson, String.class);

            //检验是否符合标准
            for (String standardId : safetyStandards) {
                boolean found = false;
                for (GovernmentStandard standard : governmentStandards) {
                    if (standard.getStandardId().equals(standardId) && standard.getGovernmentId().equals(governmentId)) {
                        found = true;
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
            stub.putStringState(reportId, JSON.toJSONString(report));
            log.info("Inspection report successfully: " + reportId);
        }catch (Exception e){
            log.severe("Error submitting inspection report: " + e.getMessage());
            throw new ChaincodeException("Error submitting inspection report: " + e.getMessage());
        }
    }




    //批准绿色认证，根据检测报告批准商家的绿色认证
    @Transaction
    public void approveGreenCertification(Context context, String merchantId, String reportId,boolean isApproved) {
        try {
            log.info("Approving green certification for merchant: " + merchantId + ",report: " + reportId);

            //获取检测报告
            InspectionReport report = getInspectionReport(context, reportId);
            //判断通过
            if (isApproved && !report.isPassed()) {
                log.info("Report not passed: " + report.getReportId());
                throw new ChaincodeException("Report not passed: " + report.getReportId());
            }
            //判断商家是否存在
            if (!merchantExists(context, merchantId)) {
                log.info("Merchant does not exist: " + merchantId);
                throw new ChaincodeException("Merchant does not exist: " + merchantId);
            }

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

            }

            //把信息更新到区块链中
            ChaincodeStub stub = context.getStub();
            stub.putStringState(merchantId, JSON.toJSONString(merchant));
            log.info("Updated merchant info : " + JSON.toJSONString(merchant));
            log.info("Green certification approved for merchant: " + merchantId);
        } catch (Exception e) {
            log.severe("Error submitting inspection report: " + e.getMessage());
            throw new ChaincodeException("Error approving gree certification: " + e.getMessage());
        }

    }
    //查询商家信息

    @Transaction
    public String queryMerchantInfo(Context context, String merchantId) {
        //查询商家信息
        ChaincodeStub stub = context.getStub();
        String stringState = stub.getStringState(merchantId);
        if(stringState == null || stringState.isEmpty()) {
            log.info("Merchant does not exist: " + merchantId);
            throw new ChaincodeException("Merchant does not exist: " + merchantId);
        }
        return stringState;
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


    //检测报告溯源
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

    private boolean merchantExists(Context context, String merchantId) {
        byte[] merchantBytes = context.getStub().getState(merchantId);

        return merchantBytes != null && merchantBytes.length > 0;
    }



    @Override
    public void beforeTransaction(Context ctx) {
        System.out.println("beforeTransaction-------->DataContract");
    }

    @Override
    public void afterTransaction(Context ctx, Object result) {
        System.out.println("afterTransaction-------->DataContract--------->result=" + result);
    }
}
