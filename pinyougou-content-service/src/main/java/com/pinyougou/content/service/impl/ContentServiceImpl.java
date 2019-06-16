package com.pinyougou.content.service.impl;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbContentMapper;
import com.pinyougou.pojo.TbContent;
import com.pinyougou.pojo.TbContentExample;
import com.pinyougou.pojo.TbContentExample.Criteria;
import com.pinyougou.content.service.ContentService;

import entity.PageResult;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class ContentServiceImpl implements ContentService {

    @Autowired
    private TbContentMapper contentMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbContent> findAll() {
        return contentMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbContent> page = (Page<TbContent>) contentMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(TbContent content) {
        //清除缓存
        redisTemplate.boundHashOps("content").delete(content.getCategoryId());
        contentMapper.insert(content);

    }


    /**
     * 修改
     */
    @Override
    public void update(TbContent content) {
        //查询之前的分组Id
        Long categoryId = contentMapper.selectByPrimaryKey(content.getId()).getCategoryId();
        //先清除缓存之前的ID
        redisTemplate.boundHashOps("content").delete(categoryId);

        contentMapper.updateByPrimaryKey(content);
        //清除现在分组的缓存
        if (categoryId.longValue()!=content.getCategoryId().longValue()) {
            redisTemplate.boundHashOps("content").delete(content.getCategoryId());
        }
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbContent findOne(Long id) {
        return contentMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            //先查询出原Id
            Long categoryId = contentMapper.selectByPrimaryKey(id).getCategoryId();
            //清除缓存
            redisTemplate.boundHashOps("content").delete(categoryId);

            contentMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbContent content, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbContentExample example = new TbContentExample();
        Criteria criteria = example.createCriteria();

        if (content != null) {
            if (content.getTitle() != null && content.getTitle().length() > 0) {
                criteria.andTitleLike("%" + content.getTitle() + "%");
            }
            if (content.getUrl() != null && content.getUrl().length() > 0) {
                criteria.andUrlLike("%" + content.getUrl() + "%");
            }
            if (content.getPic() != null && content.getPic().length() > 0) {
                criteria.andPicLike("%" + content.getPic() + "%");
            }
            if (content.getStatus() != null && content.getStatus().length() > 0) {
                criteria.andStatusLike("%" + content.getStatus() + "%");
            }

        }

        Page<TbContent> page = (Page<TbContent>) contentMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    //注入Redis
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<TbContent> findByCategoryId(Long categoryId) {
        List<TbContent> list = (List<TbContent>) redisTemplate.boundHashOps("content").get(categoryId);
        if (list == null) {
            System.out.println("从数据库读取数据放入缓存");
            //2生成代码
            TbContentExample example = new TbContentExample();
            //3创建构建条件
            Criteria criteria = example.createCriteria();
            //4根据分类ID查询
            criteria.andCategoryIdEqualTo(categoryId);
            //5查询条件有效的
            criteria.andStatusEqualTo("1");
            //排序(排序的方法setOrderByClause)
            example.setOrderByClause("sort_order");
            //1根据条件查询
           list = contentMapper.selectByExample(example);
            //存入缓冲Redis
            redisTemplate.boundHashOps("content").put(categoryId, list);
        } else {
            System.out.println("从数据库读取数据");
        }
        return list;

    }
}
