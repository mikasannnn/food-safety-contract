package com.mikasan.Pojo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author mikasan
 * title
 * 食品检测报告
 * 记录商家的食品安全检测结果，并表示检测标准和相关政府
 */

@Data
@DataType
@Accessors(chain = true)
public class InspectionReport {

    @Property
    private String reportId;    //报告id唯一标识符

    @Property
    private String merchantId;  //相关商家id,(查询过滤

    @Property
    private String inspectionDate;  //检测日期 报告有效性    待定

    @Property
    private String inspectionAgency;    //检测机构      返回信息

    @Property
    private List<String> safetyStandards;   //检测标准

    @Property
    private String safetyLevel;     //安全等级

    @Property
    private boolean isPassed;       //是否通过检测

    @Property
    private String governmentId;    //待定属性

/*    @Property
    private List<String> foodReports;*/

    //转化为 JSON 字符串
    public String getSafetyStandard(){
        return new Gson().toJson(safetyStandards);
    }

    // 从 JSON 字符串解析 safetyStandards
    public void setSafetyStandardsJson(String safetyStandardsJson) {
        Type listType = new TypeToken<List<String>>() {}.getType();
        this.safetyStandards = new Gson().fromJson(safetyStandardsJson, listType);
    }

/*    // 添加食品检测报告ID
    public void addFoodReport(String foodReportId) {
        if (this.foodReports == null) {
            this.foodReports = new ArrayList<>();
        }
        this.foodReports.add(foodReportId);
    }*/
}
