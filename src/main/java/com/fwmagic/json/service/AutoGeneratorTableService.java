package com.fwmagic.json.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author:fw
 * @Description:
 * @Date:Create in 2017/12/01
 */
@Service
public class AutoGeneratorTableService {

    private Logger logger = LoggerFactory.getLogger(AutoGeneratorTableService.class);

    /**
     * 记录插入表数据数量
     */
    private static AtomicLong num = null;

    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * 生成表集合
     */
    private static List<String> tables = null;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 根据json自动建表
     *
     * @param data
     * @throws Exception
     */
    public String autoGenTable(String tableName, String data) throws Exception {
        //用于记录生成表数量及控制parentId的生成
        num = new AtomicLong(0);
        tables = new ArrayList<>();

        JsonNode jsonNode = mapper.readTree(data);
        String tablesStr = objectHandle(tableName, jsonNode);
        return tablesStr;
    }

    /**
     * 处理不同JsonNode:Array,Object和普通节点生成表
     *
     * @param tableName
     * @param jsonNode
     * @throws Exception
     */
    public String objectHandle(String tableName, JsonNode jsonNode) throws Exception {
        Map<String, Object> mapTable = new LinkedHashMap<>();
        Map<String, JsonNode> mapJN = new LinkedHashMap<>();
        if (jsonNode.isArray()) {
            for (JsonNode jn : jsonNode) {
                partitionJsonNode(jn, mapTable, mapJN);
            }
        } else {
            partitionJsonNode(jsonNode, mapTable, mapJN);
        }
        return genTable(tableName, mapTable, mapJN);
    }

    /**
     * 区分不同类型的JsonNode
     *
     * @param jsonNode
     * @param mapTable
     * @param mapJN
     */
    private void partitionJsonNode(JsonNode jsonNode, Map<String, Object> mapTable, Map<String, JsonNode> mapJN) {
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode node = entry.getValue();
            if (node.isObject() || node.isArray()) {
                //先存起来
                mapJN.put(key, node);
            } else {
                mapTable.put(key, node.asText());
            }
        }
    }

    /**
     * 自动生成表
     *
     * @param originTableName
     * @param mapTable
     * @param mapJN
     * @throws Exception
     */

    public String genTable(String originTableName, Map<String, Object> mapTable, Map<String, JsonNode> mapJN) throws Exception {
        try {
            if (!CollectionUtils.isEmpty(mapTable)) {
                //表名默认是动态拼接的
                String currentTableName = originTableName;
                Object tableName = mapTable.get("tableName");
                //自定义表名
                if (tableName != null && tableName.toString().trim().length() > 0) {
                    currentTableName = String.valueOf(tableName);
                }
                mapTable.remove("tableName");
                StringBuffer sb = new StringBuffer();
                sb.append("CREATE TABLE IF NOT EXISTS ").append("`").append(currentTableName).append("`").append(" (")
                        .append(" `id` bigint(20) NOT NULL AUTO_INCREMENT,");
                //不是最外层，有parent_id字段
                if (num.get() != 0) {
                    sb.append("`parent_id` int NOT NULL,");
                }
                //表注释
                Object tableComment = mapTable.get("tableComment")==null?"":mapTable.get("tableComment");
                mapTable.remove("tableComment");
                if (!CollectionUtils.isEmpty(mapTable)) {
                    for (Map.Entry<String, Object> entry : mapTable.entrySet()) {
                        String field = entry.getKey();
                        String type = asText(entry.getValue()).replaceAll("\"", "");
                        sb.append("`" + field + "` " + type + ",");
                    }
                }
                sb.append("`create_time` datetime,PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8 ")
                        .append("COMMENT=").append("\"" + tableComment + "\"").append(";");

                //新建表
                jdbcTemplate.execute(sb.toString());
                long n = num.addAndGet(1);
                logger.info("\n" + "新建第【" + n + "】张表:" + currentTableName + "\n" + sb.toString());
                tables.add("\n新建第【" + n + "】张表:" + currentTableName);
                System.out.println("================================================");
            }

            //递归处理Object类型数据和Array类型数据
            if (!CollectionUtils.isEmpty(mapJN)) {
                for (Map.Entry<String, JsonNode> entry : mapJN.entrySet()) {
                    String tn = entry.getKey();
                    JsonNode node = entry.getValue();
                    objectHandle(originTableName + "_" + tn, node);
                }
            }
        } catch (Exception e) {
            logger.error("建表发生异常！", e);
            if (!CollectionUtils.isEmpty(tables)) {
                //建表操作没有事物,需手动删除已创建的表,模拟事物回滚
                for (String tableStr : tables) {
                    String[] split = tableStr.split(":");
                    String tbName = split[1];
                    String sql = "DROP  TABLE IF exists " + tbName;
                    logger.info("DROP  TABLE IF exists " + tbName);
                    jdbcTemplate.execute(sql);
                }
            }
            throw new Exception("建表发生异常:" + e.getLocalizedMessage(), e);
        }
        return tables.toString();
    }

    private String asText(Object obj) {
        return (obj == null) ? "" : obj.toString();
    }
}
