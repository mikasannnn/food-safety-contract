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
        //获取当前类的类加载路径
        ClassLoader classLoader = GovernmentStandardLoader.class.getClassLoader();
        //通过类加载器从类路径中加载standards.json文件的输入流
        try (InputStream inputStream = classLoader.getResourceAsStream("standards.json")) {
            //如果文件不存在 -> 抛异常
            if (inputStream == null) {
                throw new RuntimeException("Government standards file not found");
            }
            //inputStream.readAllBytes()：读取输入流中的所有字节
            //new String(..., StandardCharsets.UTF_8)：将字节数组转换为字符串，使用UTF-8编码
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            //JSON.parseObject() -> Fastjson 提供的方法，将json字符串解析为 List<GovernmentStandard>
            //new TypeReference<List<GovernmentStandard>>用于保留泛型类型信息，切薄解析结果的类型是确保解析结果为 List<GovernmentStandard>
            return JSON.parseObject(json, new TypeReference<List<GovernmentStandard>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load government standards", e);
        }
    }
}
