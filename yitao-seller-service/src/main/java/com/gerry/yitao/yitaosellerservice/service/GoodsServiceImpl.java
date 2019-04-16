package com.gerry.yitao.yitaosellerservice.service;

import com.gerry.yitao.common.exception.ServiceException;
import com.gerry.yitao.domain.*;
import com.gerry.yitao.dto.CartDto;
import com.gerry.yitao.entity.PageResult;
import com.gerry.yitao.mapper.*;
import com.gerry.yitao.service.GoodsService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bystander
 * @date 2018/9/18
 */
@Slf4j
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;


    @Override
    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, String key, Boolean saleable) {
        //分页
        PageHelper.startPage(page, rows);

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();

        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        if (saleable != null) {
            criteria.orEqualTo("saleable", saleable);
        }
        //默认以上一次更新时间排序
        example.setOrderByClause("last_update_time desc");

        //只查询未删除的商品
        criteria.andEqualTo("valid", 1);

        //查询
        List<Spu> spuList = spuMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(spuList)) {
            throw new ServiceException("查询的商品部存在");
        }
        //对查询结果中的分类名和品牌名进行处理
        handleCategoryAndBrand(spuList);

        PageInfo<Spu> pageInfo = new PageInfo<>(spuList);

        return new PageResult<>(pageInfo.getTotal(), spuList);
    }

    @Override
    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null) {
            throw new ServiceException("查询的商品部存在");
        }
        return spuDetail;
    }

    @Override
    public List<Sku> querySkuBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new ServiceException("查询的商品部存在");
        }

        //查询库存
        for (Sku sku1 : skuList) {
            sku1.setStock(stockMapper.selectByPrimaryKey(sku1.getId()).getStock());
        }
        return skuList;
    }

    @Transactional
    @Override
    public void deleteGoodsBySpuId(Long spuId) {
        if (spuId == null) {
            throw new ServiceException("查询的商品部存在");
        }
        //删除spu,把spu中的valid字段设置成false
        Spu spu = new Spu();
        spu.setId(spuId);
        spu.setValid(false);
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count == 0) {
            throw new ServiceException("查询的商品部存在");
        }
    }

    @Transactional
    @Override
    public void addGoods(Spu spu) {
        //添加商品要添加四个表 spu, spuDetail, sku, stock四张表
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        //插入数据
        int count = spuMapper.insert(spu);
        if (count != 1) {
            throw new ServiceException("查询的商品部存在");
        }
        //插入spuDetail数据
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.insert(spuDetail);
        if (count != 1) {
            throw new ServiceException("查询的商品部存在");
        }

        //插入sku和库存
        saveSkuAndStock(spu);

    }

    @Transactional
    @Override
    public void updateGoods(Spu spu) {
        if (spu.getId() == 0) {
            throw new ServiceException("查询的商品部存在");
        }
        //首先查询sku
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skuList = skuMapper.select(sku);
        if (!CollectionUtils.isEmpty(skuList)) {
            //删除所有sku
            skuMapper.delete(sku);
            //删除库存
            List<Long> ids = skuList.stream()
                    .map(Sku::getId)
                    .collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        //更新数据库  spu  spuDetail
        spu.setLastUpdateTime(new Date());
        //更新spu spuDetail
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count == 0) {
            throw new ServiceException("查询的商品部存在");
        }


        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.updateByPrimaryKeySelective(spuDetail);
        if (count == 0) {
            throw new ServiceException("查询的商品部存在");
        }

        //更新sku和stock
        saveSkuAndStock(spu);
    }

    @Override
    public void handleSaleable(Spu spu) {
        spu.setSaleable(!spu.getSaleable());
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count != 1) {
            throw new ServiceException("查询的商品部存在");
        }
    }

    @Override
    public Spu querySpuBySpuId(Long spuId) {
        //根据spuId查询spu
        Spu spu = spuMapper.selectByPrimaryKey(spuId);

        //查询spuDetail
        SpuDetail detail = querySpuDetailBySpuId(spuId);

        //查询skus
        List<Sku> skus = querySkuBySpuId(spuId);

        spu.setSpuDetail(detail);
        spu.setSkus(skus);

        return spu;

    }

    @Override
    public List<Sku> querySkusByIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skus)) {
            throw new ServiceException("查询的商品部存在");
        }
        //填充库存
        fillStock(ids, skus);
        return skus;
    }

    @Transactional
    @Override
    public void decreaseStock(List<CartDto> cartDtos) {
        for (CartDto cartDto : cartDtos) {
            int count = stockMapper.decreaseStock(cartDto.getSkuId(), cartDto.getNum());
            if (count != 1) {
                throw new ServiceException("扣减库存失败");
            }
        }
    }

    private void fillStock(List<Long> ids, List<Sku> skus) {
        //批量查询库存
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stocks)) {
            throw new ServiceException("保存库存失败");
        }
        //首先将库存转换为map，key为sku的ID
        Map<Long, Integer> map = stocks.stream().collect(Collectors.toMap(s -> s.getSkuId(), s -> s.getStock()));

        //遍历skus，并填充库存
        for (Sku sku : skus) {
            sku.setStock(map.get(sku.getId()));
        }
    }


    /**
     * 保存sku和库存
     *
     * @param spu
     */
    private void saveSkuAndStock(Spu spu) {
        List<Sku> skuList = spu.getSkus();
        List<Stock> stocks = new ArrayList<>();

        for (Sku sku : skuList) {
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            int count = skuMapper.insert(sku);
            if (count != 1) {
                throw new ServiceException("保存库存失败");
            }

            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stocks.add(stock);
        }
        //批量插入库存数据
        int count = stockMapper.insertList(stocks);
        if (count == 0) {
            throw new ServiceException("保存库存失败");
        }
    }


    /**
     * 处理商品分类名和品牌名
     *
     * @param spuList
     */
    private void handleCategoryAndBrand(List<Spu> spuList) {
        for (Spu spu : spuList) {
            //根据spu中的分类ids查询分类名
            List<String> nameList = categoryMapper.selectByIdList(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream()
                    .map(Category::getName)
                    .collect(Collectors.toList());
            //对分类名进行处理
            spu.setCname(StringUtils.join(nameList, "/"));

            //查询品牌
            spu.setBname(brandMapper.selectByPrimaryKey(spu.getBrandId()).getName());
        }
    }
}
