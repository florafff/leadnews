package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.search.pojos.ApAssociateWords;
import com.heima.search.mapper.ApAssociateWordsMapper;
import com.heima.search.model.Trie;
import com.heima.search.service.ApAssociateWordsService;
import org.apache.commons.collections.ArrayStack;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ApAssociateWordsServiceImpl extends ServiceImpl<ApAssociateWordsMapper, ApAssociateWords> implements ApAssociateWordsService {

    /**
     * 查询关键字联想词
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) {
        //1.检查参数
        if(StringUtils.isBlank(dto.getSearchWords())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if(dto.getPageSize() > 50){
            dto.setPageSize(50);
        }

        //2.分页模糊查询
        IPage pageParam = new Page(0,dto.getPageSize());
        IPage page = page(pageParam, Wrappers.<ApAssociateWords>lambdaQuery().like(ApAssociateWords::getAssociateWords, dto.getSearchWords()));
        return ResponseResult.okResult(page.getRecords());//list-->[{AssociateWords:sss,},{AssociateWords:sss}]
    }

    @Autowired
    private StringRedisTemplate redisTemplate ;

    /**
     * 查询关键字联想词  v2版本
     * @param dto
     * @return
     */
    @Override
    public ResponseResult searchV2(UserSearchDto dto) {
        //1.从缓存中获取数据
        String associateListStr = redisTemplate.opsForValue().get("associate_list");

        //2.如果存在 直接返回数据

        List<ApAssociateWords> list = null;

        if(StringUtils.isNotBlank(associateListStr)){
            list = JSON.parseArray(associateListStr, ApAssociateWords.class);
        }else {
            //3.如果不存在，从数据库中查询数据，并且放入缓存中
            list = list();
            redisTemplate.opsForValue().set("associate_list", JSON.toJSONString(list));
        }

        //4.初始化Trie 数据结构
        Trie  t = new Trie();
        for (ApAssociateWords associateWords : list) {
            t.insert(associateWords.getAssociateWords());
        }

        List<Map> resultList  = new ArrayList<>();

        //5.匹配关键字，返回
        List<String> associateWordList = t.startWith(dto.getSearchWords());//[sss,dddd,ddd,ddd,]
        for (String s : associateWordList) {
            Map map = new HashMap();
            map.put("associateWords",s);
            resultList.add(map);

        }
        return ResponseResult.okResult(resultList);
    }
}
