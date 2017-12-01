package com.fwmagic.json.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.*;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author:fw
 * @Description:
 * @Date:Create in 2017/12/01
 */
@Service
public class AutoInsertTableService {

    private Logger logger = LoggerFactory.getLogger(AutoInsertTableService.class);

    private static ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 自动插入数据
     *
     * @param tablePrefix
     * @param data
     * @param parentId
     * @throws Exception
     */
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    public void autoInsertTable(String tablePrefix, String data, Long parentId) throws Exception {
        JsonNode jsonNode = mapper.readTree(data);
        objectHandle(tablePrefix, jsonNode, parentId);
    }

    /**
     * 区分不同JsonNode，插入数据
     *
     * @param tableName
     * @param jsonNode
     * @param parentId
     * @throws Exception
     */
    public void objectHandle(String tableName, JsonNode jsonNode, long parentId) throws Exception {
        Map<String, Object> mapTable = new LinkedHashMap<>();
        Map<String, JsonNode> mapJN = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode node = entry.getValue();
            if (node != null) {
                if (node.isObject()) {
                    //先存起来
                    mapJN.put(key, node);
                } else if (node.isArray()) {
                    if (node.size() > 0) {
                        if (node.get(0).isObject()) {
                            mapJN.put(key, node);
                        } else {
                            String str = appendStr(node);
                            mapTable.put(key, str);
                        }
                    }
                } else {
                    mapTable.put(key, node.asText());
                }
            }
        }
        insertTable(tableName, mapTable, mapJN, parentId);
    }

    /**
     * 插入表数据
     *
     * @param tableName
     * @param mapTable
     * @param mapJN
     * @param parentId
     * @throws Exception
     */
    public void insertTable(String tableName, Map<String, Object> mapTable, Map<String, JsonNode> mapJN, long parentId) throws Exception {
        if (!CollectionUtils.isEmpty(mapTable)) {
            String sql = getsql(tableName, mapTable, parentId);
            System.out.println("sql = " + sql);
            //插入表数据,返回主键
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    return ps;
                }
            }, keyHolder);

            parentId = keyHolder.getKey().longValue();
        }

        //递归处理Object类型数据和Array类型数据，携带parentId
        if (!CollectionUtils.isEmpty(mapJN)) {
            otherHandle(tableName, mapJN, parentId);
        }

    }

    /**
     * 组装sql
     *
     * @param tableName
     * @param mapTable
     * @param parentId
     * @return
     */
    private String getsql(String tableName, Map<String, Object> mapTable, long parentId) {
        logger.info("插入表：" + tableName);
        StringBuffer sb = new StringBuffer();
        sb.append("insert into ").append(tableName).append(" (");
        if (parentId != 0) {
            sb.append("`parent_id`,");
        }
        //拼接key
        for (Map.Entry<String, Object> entry : mapTable.entrySet()) {
            String key = entry.getKey();
            sb.append("`").append(key).append("`").append(",");
        }
        sb.append("`create_time`)");
        //拼接values
        sb.append(" values (");
        if (parentId != 0) {
            sb.append(parentId + ",");
        }
        for (Map.Entry<String, Object> entry : mapTable.entrySet()) {
            Object value = entry.getValue();
            if (null == value || "null".equals(value)) {
                value = null;
            } else if (value instanceof String) {
                value = "\"" + value + "\"";
            }
            sb.append(value).append(",");
        }
        sb.append("\"" + new Timestamp(System.currentTimeMillis()) + "\"" + ");");
        return sb.toString();
    }

    /**
     * 递归处理Object类型数据和Array类型数据，携带parentId
     *
     * @param mapJN
     * @param parentId
     */
    public void otherHandle(String tablePrefix, Map<String, JsonNode> mapJN, long parentId) throws Exception {
        for (Map.Entry<String, JsonNode> entry : mapJN.entrySet()) {
            //表名
            String tableName = entry.getKey();
            //节点数据：Array或者是Object
            JsonNode node = entry.getValue();
            if (node.isArray()) {
                for (JsonNode n : node) {
                    objectHandle(tablePrefix + "_" + tableName, n, parentId);
                }
            } else if (node.isObject()) {
                objectHandle(tablePrefix + "_" + tableName, node, parentId);
            }
        }
    }

    /**
     * 组装集合数据
     *
     * @param node
     * @return
     */
    private String appendStr(JsonNode node) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < node.size(); i++) {
            if (i == node.size() - 1) {
                sb.append(node.get(i).asText());
            } else {
                sb.append(node.get(i).asText() + ",");
            }
        }
        return sb.toString();
    }

}
