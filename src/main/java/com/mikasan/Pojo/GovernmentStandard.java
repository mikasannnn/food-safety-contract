package com.mikasan.Pojo;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * @Author mikasan
 * title    政府实体类
 * 定义食品安全标准
 */
@Data
@DataType
@Accessors(chain = true)
public class GovernmentStandard {
    @Property
    private String standardId;  //食品标准id

    @Property
    private String standardName;    //标准名称

    @Property
    private String description;     //标准描述

    @Property
    private String governmentId;    //政府id      待定

    @Property
    private int validityPeriod;     //有效期（月

}
