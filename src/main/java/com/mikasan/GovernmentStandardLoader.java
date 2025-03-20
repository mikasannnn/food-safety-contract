package com.mikasan;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.mikasan.Pojo.GovernmentStandard;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author mikasan
 * title    读取静态数据加载json文件到区块链上
 */
public class GovernmentStandardLoader {
    public static List<GovernmentStandard> loadStandards(){
        try (InputStream inputStream = GovernmentStandardLoader.class.getClassLoader().getResourceAsStream("standards.json")) {
            if (inputStream == null) {
                throw new RuntimeException("Government standards file not found");
            }
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.parseObject(json, new TypeReference<List<GovernmentStandard>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load government standards", e);
        }
    }
}
