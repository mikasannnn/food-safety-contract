package com.mikasan.Pojo;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * @Author mikasan
 * title    商家实体类
 */

@DataType
@Data
@Accessors(chain = true)
public class Merchant {

    @Property
    private String merchantId;  //商家id唯一标识符

    @Property
    private String name;    //商家名

    @Property
    private boolean isGreenCertified; //是否获得绿色认证

    @Property
    private String certificationDate;   //认证日期

    @Property
    private String safetyLevel;     //安全等级

    @Property
    private String governmentId;    //待定属性


}
