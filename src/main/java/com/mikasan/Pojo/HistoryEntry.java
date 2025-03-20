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
public class HistoryEntry {
    @Property
    private String txId; // 交易 ID

    @Property
    private String timestamp; // 交易时间戳

    @Property
    private String value; // 交易的值

    @Property
    private boolean isDelete; // 是否删除

    public HistoryEntry setIsDelete(boolean isDelete) {
        this.isDelete = isDelete;
        return this;
    }

}