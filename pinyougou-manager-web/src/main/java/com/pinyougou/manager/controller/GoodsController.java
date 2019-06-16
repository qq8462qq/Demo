package com.pinyougou.manager.controller;
import java.util.Arrays;
import java.util.List;

import com.alibaba.fastjson.JSON;

import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojogroup.Goods;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;
import entity.Result;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * controller
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

	@Reference
	private GoodsService goodsService;
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findAll")
	public List<TbGoods> findAll(){			
		return goodsService.findAll();
	}
	
	
	/**
	 * 返回全部列表
	 * @return
	 */
	@RequestMapping("/findPage")
	public PageResult  findPage(int page,int rows){			
		return goodsService.findPage(page, rows);
	}
	

	
	/**
	 * 修改
	 * @param goods
	 * @return
	 */
	@RequestMapping("/update")
	public Result update(@RequestBody Goods goods){
		try {
			goodsService.update(goods);
			return new Result(true, "修改成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "修改失败");
		}
	}	
	
	/**
	 * 获取实体
	 * @param id
	 * @return
	 */
	@RequestMapping("/findOne")
	public Goods findOne(Long id){
		return goodsService.findOne(id);		
	}
	
	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@Autowired//用户在索引库中删除记录
	private Destination queueSolrDeleteDestination;
	@RequestMapping("/delete")
	public Result delete(final Long [] ids){
		try {
			goodsService.delete(ids);
			jmsTemplate.send(queueSolrDeleteDestination, new MessageCreator() {
				@Override
				public Message createMessage(Session session) throws JMSException {
					return session.createObjectMessage(ids);
				}
			});
			return new Result(true, "删除成功"); 
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "删除失败");
		}
	}
	
		/**
	 * 查询+分页
	 * @param
	 * @param page
	 * @param rows
	 * @return
	 */
	@RequestMapping("/search")
	public PageResult search(@RequestBody TbGoods goods, int page, int rows  ){
		return goodsService.findPage(goods, page, rows);		
	}
	//@Reference
	//private ItemSearchService itemSearchService;

	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private Destination queueSolrDestination;
	@Autowired
	private Destination topicPageDestination;


	@RequestMapping("/updateStatus")
	public Result updateStatus(Long[] ids, String status){
		System.out.println(topicPageDestination+"99999999999999999999999999");
		try {
			goodsService.updateStatus(ids,status);
		//按照SPU ID 查询 SKU 列表(状态为 1)
			if(status.equals("1")) {//审核通过
				//#######导入索引库
				List<TbItem> itemList = goodsService.findItemListByGoodsIdandStatus(ids, status);//status状态
				//调用搜索接口实现数据批量导入
				if (itemList.size() > 0) {
					//传输数据转成String类型数据
					final String jsonString = JSON.toJSONString(itemList);
					//用jmsTemplate模板send
					jmsTemplate.send(queueSolrDestination, new MessageCreator() {
						//new一个匿名内部类
						@Override
						public Message createMessage(Session session) throws JMSException {
							return session.createTextMessage(jsonString);
						}
					});
					//itemSearchService.importList(itemList);
				} else {
					System.out.println("没有明细数据");
				}
				//########生成商品详细页
				for(final Long goodsId:ids){
					//itemPageService.genItemHtml(goodsId);
					//静态页生成topicPageDestination
					System.out.println();
					jmsTemplate.send(topicPageDestination, new MessageCreator() {
						@Override
						public Message createMessage(Session session) throws JMSException {
							return session.createTextMessage(goodsId+"");
						}
					});
				}


			}
			return new Result(true,"修改状态成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(true,"修改状态失败");
		}
	};
	@Reference//把谁注入就可以用谁来进行操作(属于远程调用)
	//private ItemPageService itemPageService;
	@RequestMapping("/genHtml")
	public void genHtml(Long goodsId){
		//itemPageService.genItemHtml(goodsId);

	}
}
