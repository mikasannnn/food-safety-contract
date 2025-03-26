package com.mikasan.Pojo;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * @Author mikasan
 * title
 */
@Data
@DataType
@Accessors(chain = true)
public class FoodInspectionReport {

    @Property
    String foodId;

    @Property
    String merchantId;

    @Property
    String inspectionDate;

    @Property
    String inspectionAgency;

    @Property
    String SafetyLevel;

    @Property
    boolean isPassed;

    @Property
    String complaintId; //投诉用户id（非必要）

    @Property
    String governmentId;
}
