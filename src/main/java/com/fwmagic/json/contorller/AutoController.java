package com.fwmagic.json.contorller;

import com.fwmagic.json.service.AutoGeneratorTableService;
import com.fwmagic.json.service.AutoInsertTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author:fw
 * @Description:
 * @Date:Create in 2017/12/01
 */
@RequestMapping("auto")
@RestController
public class AutoController {

    private static Logger logger = LoggerFactory.getLogger(AutoController.class);

    @Autowired
    private AutoGeneratorTableService autoGeneratorTableService;

    @Autowired
    private AutoInsertTableService autoInsertTableService;


    /**
     * 健康检查
     * @return
     */
    @RequestMapping("health")
    public String health(){
        return "success";
    }

    /**
     * 根据json自动建表
     *
     * @param tablePrefix
     * @param data
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "genTable", method = RequestMethod.POST)
    public String autoGenTable(@RequestParam(value = "tablePrefix") String tablePrefix, @RequestParam("data") String data) throws Exception {
        logger.info("fetch data => tablePreix:{},\n data:{}", tablePrefix, data);
        String s = "error";
        System.out.println("================================开始新建表===================================");
        try {
            s = this.autoGeneratorTableService.autoGenTable(tablePrefix, data);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("自动建表失败",e);
            throw new Exception("建表失败！", e);
        }
        return s;
    }

    /**
     * 解析json数据，自动插入表数据
     *
     * @param tablePrefix
     * @param data
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "insertTable", method = RequestMethod.POST)
    public String autoInsertTable(@RequestParam(value = "tablePrefix") String tablePrefix, @RequestParam("data") String data) throws Exception {
        logger.info("fetch data => tablePreix:{},\n data:{}", tablePrefix, data);
        System.out.println("================================开始插入表===================================");
        String message = "success";
        try {
            this.autoInsertTableService.autoInsertTable(tablePrefix, data, 0L);
            message = "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("插入失败!",e);
            throw new Exception("插入失败", e);
        }
        return message;
    }
}