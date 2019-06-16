package com.pinyougou.page.service.impl;


import com.pinyougou.mapper.TbGoodsDescMapper;
import com.pinyougou.mapper.TbGoodsMapper;
import com.pinyougou.mapper.TbItemCatMapper;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.page.service.ItemPageService;
import com.pinyougou.pojo.*;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class ItemPageServiceImpl implements ItemPageService {

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Value("${pagedir}")
    private String pagedir;
    @Autowired    //需要数据访问层(商品名称)把它注入
    private TbGoodsMapper goodsMapper;
    @Autowired//需要数据访问层(商品规格信息)注入
    private TbGoodsDescMapper goodsDescMapper;
    @Autowired//需要读取商品分类数据(把商品分类)注入
    private TbItemCatMapper itemCatMapper;
    @Autowired//需要读取SKU数据注入
    private TbItemMapper itemMapper;

    @Override
    public boolean genItemHtml(Long goodsId) {
        Configuration configuration = freeMarkerConfigurer.getConfiguration();
        try {
            Template template = configuration.getTemplate("item.ftl");
            //创建数据模型
            Map hashMap = new HashMap();
            //根据主键来查goodsId
            TbGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
            //1把商品主表数据装到HashMap
            hashMap.put("goods", goods);
            //根据主键来查goodsDescId(goodsId和goodsDescId是一致的)
            TbGoodsDesc goodsDesc = goodsDescMapper.selectByPrimaryKey(goodsId);
            //2把商品扩展表数据装到HashMap
            hashMap.put("goodsDesc", goodsDesc);
            //3读取商品分类
            String itemCat1 = itemCatMapper.selectByPrimaryKey(goods.getCategory1Id()).getName();
            String itemCat2 = itemCatMapper.selectByPrimaryKey(goods.getCategory2Id()).getName();
            String itemCat3 = itemCatMapper.selectByPrimaryKey(goods.getCategory3Id()).getName();
            hashMap.put("itemCat1",itemCat1);
            hashMap.put("itemCat2",itemCat2);
            hashMap.put("itemCat3",itemCat3);
            //4读取SKU列表
            TbItemExample example=new TbItemExample();
            TbItemExample.Criteria criteria=example.createCriteria();
            criteria.andStatusEqualTo("1");//状态为有效
            criteria.andGoodsIdEqualTo(goodsId);//指定 SPU ID
            example.setOrderByClause("is_default desc");//按照状态降序，保证第一个为默认是SKU

            List<TbItem> itemList = itemMapper.selectByExample(example);
            hashMap.put("itemList",itemList);


            //配置pagedir的路径加页面传ID加html得到具体目录
            Writer out = new FileWriter(pagedir + goodsId + ".html");
            template.process(hashMap, out);
            //关闭流对象
            out.close();
            //最后把返回值加上
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}
