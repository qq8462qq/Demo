package com.pinyougou.sellergoods.service.impl;
import java.util.*;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.*;
import com.pinyougou.pojogroup.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.pojo.TbGoodsExample.Criteria;
import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
//@Transactional
public class GoodsServiceImpl implements GoodsService {

	@Autowired
	private TbGoodsMapper goodsMapper;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbGoods> findAll() {
		return goodsMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbGoods> page=   (Page<TbGoods>) goodsMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}
	@Autowired
	private TbGoodsDescMapper goodsDescMapper;
	@Autowired
	private TbItemMapper itemMapper;
	@Autowired
    private TbBrandMapper brandMapper;
	@Autowired
    private TbItemCatMapper itemCatMapper;
	@Autowired
    private TbSellerMapper sellerMapper;

	/**
	 * 增加
	 */
	@Override
	public void add(Goods goods) {
        goods.getGoods().setAuditStatus("0");//状态未审核
        goodsMapper.insert(goods.getGoods());
/*
        int x=2/0;
*/
        goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());//将商品基本表的ID给扩展表
        goodsDescMapper.insert(goods.getGoodsDesc());//插入商品扩展表数据
        //插入SKU列表数据
        saveItemList(goods);




	}

	public void setItemValus(Goods goods,TbItem item){
        item.setGoodsId(goods.getGoods().getId());//商品 SPU 编号
        item.setSellerId(goods.getGoods().getSellerId());//商家编号
        item.setCategoryid(goods.getGoods().getCategory3Id());//商品分类编号（3 级）
        item.setCreateTime(new Date());//创建日期
        item.setUpdateTime(new Date());//修改日期
        //品牌名称
        TbBrand brand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
        item.setBrand(brand.getName());
        //分类名称
        TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(goods.getGoods().getCategory3Id());
        item.setBrand(brand.getName());
        //商家名称
        TbSeller seller = sellerMapper.selectByPrimaryKey(goods.getGoods().getSellerId());
        item.setSeller(seller.getNickName());
        //图片地址（取 spu 的第一个图片）
        List<Map> imageList = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
        if (imageList.size()>0){
            item.setImage((String) imageList.get(0).get("url"));
        }
    }
    //插入SKU列表数据
    public void saveItemList(Goods goods) {
        if ("1".equals(goods.getGoods().getIsEnableSpec())) {

            for (TbItem item : goods.getItemList()) {
                String title = goods.getGoods().getGoodsName();
                Map<String, Object> specMap = JSON.parseObject(item.getSpec());
                for (String key : specMap.keySet()) {
                    title += "" + specMap.get(key);
                }
                item.setTitle(title);
                setItemValus(goods, item);

                itemMapper.insert(item);
            }
        } else {
            TbItem item = new TbItem();
            item.setTitle(goods.getGoods().getGoodsName());//商品 KPU+规格描述串作为SKU 名称
            item.setPrice(goods.getGoods().getPrice());//价格
            item.setStatus("1");//状态
            item.setIsDefault("1");//是否默认
            item.setNum(99999);//库存数量
            item.setSpec("{}");
            setItemValus(goods, item);
            itemMapper.insert(item);

        }
    }
	
	/**
	 * 修改
	 */
	@Override
	public void update(Goods goods){
	    //更新基本表数据
        goodsMapper.updateByPrimaryKey(goods.getGoods());
        //更新扩展表数据
        goodsDescMapper.updateByPrimaryKey(goods.getGoodsDesc());
		//删除原有的SKU列表数据
        TbItemExample example=new TbItemExample();
        TbItemExample.Criteria criteria = example.createCriteria();
        criteria.andGoodsIdEqualTo(goods.getGoods().getId());

        itemMapper.deleteByExample(example);
        //插入修改后得到数据
            saveItemList(goods);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public Goods findOne(Long id){
	    Goods goods=new Goods();
        TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
        goods.setGoods(tbGoods);
        //商品扩展类
       TbGoodsDesc goodsDesc= goodsDescMapper.selectByPrimaryKey(id);
       goods.setGoodsDesc(goodsDesc);
       //goodsMapper.selectByPrimaryKey(id);
        //读取SKU列表
        TbItemExample example= new TbItemExample();
        TbItemExample.Criteria criteria = example.createCriteria();
        criteria.andGoodsIdEqualTo(id);
        List<TbItem> itemList = itemMapper.selectByExample(example);
        goods.setItemList(itemList);
        return goods;
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
            TbGoods goods = goodsMapper.selectByPrimaryKey(id);
            goods.setIsDelete("1");//表示逻辑删除
            //表示更新返回数据
            goodsMapper.updateByPrimaryKey(goods);
        }
	}
	
	
		@Override
	public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
            PageHelper.startPage(pageNum, pageSize);
		
		TbGoodsExample example=new TbGoodsExample();
		Criteria criteria = example.createCriteria();
            //指定为null时不显示(代表逻辑删除)
		criteria.andIsDeleteIsNull();
		
		if(goods!=null){			
						if(goods.getSellerId()!=null && goods.getSellerId().length()>0){
				//criteria.andSellerIdLike("%"+goods.getSellerId()+"%");模糊查询
                            criteria.andSellerIdEqualTo(goods.getSellerId());
			}
			if(goods.getGoodsName()!=null && goods.getGoodsName().length()>0){
				criteria.andGoodsNameLike("%"+goods.getGoodsName()+"%");
			}
			if(goods.getAuditStatus()!=null && goods.getAuditStatus().length()>0){
                if(goods.getAuditStatus().equals("6") ){
                    ArrayList<String> auditStatusList = new ArrayList<>();
                    auditStatusList.add("1");
                    auditStatusList.add("4");
                    auditStatusList.add("5");
                    criteria.andAuditStatusIn(auditStatusList);
                }else{
                    criteria.andAuditStatusLike("%"+goods.getAuditStatus()+"%");
                }
			}
			if(goods.getIsMarketable()!=null && goods.getIsMarketable().length()>0){
				criteria.andIsMarketableLike("%"+goods.getIsMarketable()+"%");
			}
			if(goods.getCaption()!=null && goods.getCaption().length()>0){
				criteria.andCaptionLike("%"+goods.getCaption()+"%");
			}
			if(goods.getSmallPic()!=null && goods.getSmallPic().length()>0){
				criteria.andSmallPicLike("%"+goods.getSmallPic()+"%");
			}
			if(goods.getIsEnableSpec()!=null && goods.getIsEnableSpec().length()>0){
				criteria.andIsEnableSpecLike("%"+goods.getIsEnableSpec()+"%");
			}
			if(goods.getIsDelete()!=null && goods.getIsDelete().length()>0){
				criteria.andIsDeleteLike("%"+goods.getIsDelete()+"%");
			}

	
		}
		
		Page<TbGoods> page= (Page<TbGoods>)goodsMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}

    @Override
    public void updateStatus(Long[] ids, String status) {
	    for(Long id:ids){
            TbGoods goods = goodsMapper.selectByPrimaryKey(id);
            goods.setAuditStatus(status);
            goodsMapper.updateByPrimaryKey(goods);

        }

    }

    @Override
    public void updateDown(Long[] ids, String statuss) {
        for (Long id :ids ){
            TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
            tbGoods.setAuditStatus(statuss);
            goodsMapper.updateByPrimaryKey(tbGoods);

        }
    }

    @Override
    public List<TbItem> findItemListByGoodsIdandStatus(Long[] goodsIds, String status) {
        TbItemExample example=new TbItemExample();
        com.pinyougou.pojo.TbItemExample.Criteria criteria = example.createCriteria();
        criteria.andGoodsIdIn(Arrays.asList(goodsIds));
        criteria.andStatusEqualTo(status);
        return itemMapper.selectByExample(example);
    }
}


