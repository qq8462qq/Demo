package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//设置超时时间(timeout)
@Service(timeout = 3000)
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;
    @Override
    public Map search(Map searchMap) {
        //关键字空格处理
        String keywords = (String) searchMap.get("keywords");
        searchMap.put("keywords", keywords.replace(" ", ""));
        Map map = new HashMap();
     //1查询列表
        map.putAll(searchList(searchMap));
        //2.分组查询商品分类列表
        List<String> categoryList = searchCategoryList(searchMap);
        map.put("categoryList",categoryList);

        //3查询品牌和规格列表
        String categoryName=(String)searchMap.get("category");
        if(!"".equals(categoryName)){//如果有分类名称
            map.putAll(searchBrandAndSpecList(categoryName));
        }else{//如果没有分类名称，按照第一个查询
            if(categoryList.size()>0){
                map.putAll(searchBrandAndSpecList(categoryList.get(0)));
            }
        }
        return map;
    }
    /**
     * 导入数据
     * @param list
     */

    @Override
    public void importList(List list) {
        solrTemplate.saveBeans(list);
        solrTemplate.commit();
    }
    /**
     * 删除数据
     * @param
     */

    @Override
    public void deleteByGoodsIds(List goodsIdList) {
        System.out.println("删除商品ID"+goodsIdList);
        Query query=new SimpleQuery();
        Criteria criteria=new Criteria("item_goodsid").in(goodsIdList);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }

    /**
     * 根据关键字搜索列表
     * @param
     * @return
     */
    private Map searchList(Map searchMap){
        Map map=new HashMap();
        //2生成query方法
        HighlightQuery query=new SimpleHighlightQuery();
        //4在哪一列上显示高亮
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");
        highlightOptions.setSimplePrefix("<em style='color:red'>");//前缀
        highlightOptions.setSimplePostfix("</em>");//后缀
        //为查询对象设置高亮选项
        query.setHighlightOptions(highlightOptions);
        //3关键查询
        Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);

        //1.2按商品分类过滤
        if (!"".equals(searchMap.get("category"))) {//如果用户选择了
            FilterQuery filterQuery = new SimpleFacetQuery();
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }

        //1.3按商品分类过滤
        if (!"".equals(searchMap.get("brand"))) {//如果用户选择了
            FilterQuery filterQuery = new SimpleFacetQuery();
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }
        //1.4 过滤规格
        if(searchMap.get("spec")!=null){
            Map<String,String> specMap= (Map) searchMap.get("spec");
            for(String key:specMap.keySet() ){
                Criteria filterCriteria=new Criteria("item_spec_"+key).is( specMap.get(key) );
                FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
            //1.5按价格筛选.....
            if(!"".equals(searchMap.get("price"))){
                String[] price = ((String) searchMap.get("price")).split("-");
                if(!price[0].equals("0")){//如果区间起点不等于0
                    Criteria filterCriteria=new Criteria("item_price").greaterThanEqual(price[0]);
                    FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
                    query.addFilterQuery(filterQuery);
                }
                if(!price[1].equals("*")){//如果区间终点不等于*
                    Criteria filterCriteria=new  Criteria("item_price").lessThanEqual(price[1]);
                    FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
                    query.addFilterQuery(filterQuery);
                }
            }
            //1.6 分页查询
            Integer pageNo= (Integer) searchMap.get("pageNo");//提取页码
            if(pageNo==null){
                pageNo=1;//默认第一页
            }
            Integer pageSize=(Integer) searchMap.get("pageSize");//每页记录数
            if(pageSize==null){
                pageSize=20;//默认20
            }
            query.setOffset((pageNo-1)*pageSize);//从第几条记录查询
            query.setRows(pageSize);//每页显示多少条
        }
            //1.7排序
        //1.7排序
        String sortValue= (String) searchMap.get("sort");//ASC  DESC
        String sortField= (String) searchMap.get("sortField");//排序字段
        if(sortValue!=null && !sortValue.equals("")){  //StringUtils.isEmpty()
            if(sortValue.equals("ASC")){
                Sort sort=new Sort(Sort.Direction.ASC, "item_"+sortField);//按商品升序排序
                query.addSort(sort);//把sort添加到query
            }
            if(sortValue.equals("DESC")){
                Sort sort=new Sort(Sort.Direction.DESC, "item_"+sortField);
                query.addSort(sort);
            }
        }


        //*********************获取高亮结果集*************************
        //1查询高亮显示方法(queryForHighlightPage)
        //返回高亮页对象
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
        //高亮入口集合
        List<HighlightEntry<TbItem>> entryList = page.getHighlighted();
        //循环高亮入口集合
        for (HighlightEntry<TbItem> h: page.getHighlighted()){
            //获取原实体类
            TbItem item = h.getEntity();
            if(h.getHighlights().size()>0 && h.getHighlights().get(0).getSnipplets().size()>0){
                item.setTitle(h.getHighlights().get(0).getSnipplets().get(0));//设置高亮的结果
            }
        }
        map.put("rows",page.getContent());
        map.put("totalPages", page.getTotalPages());//返回总页数
        map.put("total", page.getTotalElements());//返回总记录数
        return map;
    }/*
    分组查询(查询商品分类列表)

    */
    public List<String> searchCategoryList(Map searchMap){
        List<String>list=new ArrayList<String>();
        Query query=new SimpleQuery("*:*");

        //3根据关键字查询
        Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //设置分组选项
        GroupOptions groupOptions =new GroupOptions().addGroupByField("item_category");//相当于group by
        query.setGroupOptions(groupOptions);
        //获得分组页
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        //获得分组集合结果对象
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        //获得分组入口页
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        //获得分组入口集合
        List<GroupEntry<TbItem>> entryList = groupEntries.getContent();
        for (GroupEntry<TbItem> entry:entryList){
            list.add(entry.getGroupValue());//将分组的结果添加到返回值中
        }
        return list;
    }
    @Autowired
    private RedisTemplate redisTemplate;
    //查询品牌和规格列表
    private Map searchBrandAndSpecList(String category){
        Map map = new HashMap();
        //1根据商品分类名称得到模板ID
        Long  templateId= (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if (templateId!=null) {
            //2根据模板ID获取品牌列表
            List brandList = (List) redisTemplate.boundHashOps("brandList").get(templateId);
            map.put("brandList", brandList);
            //3根据模板ID获取品牌列表
            List specList = (List) redisTemplate.boundHashOps("specList").get(templateId);
            map.put("specList", specList);

        }
        return map;
    }

}

