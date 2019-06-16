package com.pinyougou.solrutil;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SolrUtil {
    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private TbItemMapper itemMapper;
    /*
    * 导入商品数据
    *
    * */
    public void importItemData(){

        TbItemExample example = new TbItemExample();
        TbItemExample.Criteria criteria = example.createCriteria();
        //审核通过的才导入
        criteria.andStatusEqualTo("1");
        //根据条件查询
        List<TbItem> itemList = itemMapper.selectByExample(example);
        System.out.println("====商品====");
        for (TbItem item:itemList){
            System.out.println(item.getId()+""+item.getTitle()+""+item.getPrice());
            //将 spec 字段中的 json 字符串转换为 map
            Map specMap = JSON.parseObject(item.getSpec(), Map.class);

                //给带注解的字段赋值
            item.setSpecMap(specMap);
        }

        solrTemplate.saveBeans(itemList);
        solrTemplate.commit();

        System.out.println("====结束====");
    }
    public static void main(String[] args) {

        ApplicationContext context=new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
        SolrUtil solrUtil= (SolrUtil) context.getBean("solrUtil");
        solrUtil.importItemData();
    }
}



