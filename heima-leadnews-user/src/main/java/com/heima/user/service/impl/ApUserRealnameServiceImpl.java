package com.heima.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.UserContants;
import com.heima.common.exception.CostomException;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.feign.ArticleFeign;
import com.heima.user.feign.WemediaFeign;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import io.netty.util.internal.UnstableApi;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
    @Override
    public ResponseResult loadListByStatus(AuthDto dto) {
        //1.检查参数
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();

        //2.根据状态精确分页查询
        IPage pageParam = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<ApUserRealname> lambdaQueryWrapper =new LambdaQueryWrapper<>();
        if(dto.getStatus() != null){
            lambdaQueryWrapper.eq(ApUserRealname::getStatus,dto.getStatus());
        }
        IPage page = page(pageParam, lambdaQueryWrapper);

        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @GlobalTransactional
    @Override
    public ResponseResult updateStatusById(AuthDto dto, Short status) {
        //1.检查参数
        if(dto== null || dto.getId()==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.修改认证表的状态，如果是失败，需要说明原因
        ApUserRealname apUserRealname = new ApUserRealname();
        apUserRealname.setId(dto.getId());
        apUserRealname.setStatus(status);
        if(StringUtils.isNotBlank(dto.getMsg())){
            apUserRealname.setReason(dto.getMsg());
        }
        updateById(apUserRealname);

        //3.如果是审核成功，创建自媒体人和作者
        if(status.equals(UserContants.AUTH_PASS)){
            ResponseResult responseResult = createWmUserAndAuthor(dto);
            if(responseResult != null){
                return responseResult;
            }
        }
//        int a = 1/0;
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private ApUserMapper apUserMapper;

    @Autowired
    private WemediaFeign wemediaFeign;

    /**
     * 创建自媒体人和作者
     * @param dto
     * @return
     */
    private ResponseResult createWmUserAndAuthor(AuthDto dto) {
        Integer userRealnameId = dto.getId();
        ApUserRealname apUserRealname = getById(userRealnameId);
        ApUser apUser = apUserMapper.selectById(apUserRealname.getUserId());
        if(apUser == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmUser wmUser = wemediaFeign.findByName(apUser.getName());
        if(wmUser == null){
            //创建自媒体人
            wmUser = new WmUser();
            wmUser.setName(apUser.getName());
            wmUser.setPassword(apUser.getPassword());
            wmUser.setSalt(apUser.getSalt());
            wmUser.setApUserId(apUser.getId());
            wmUser.setCreatedTime(new Date());
            wmUser.setPhone(apUser.getPhone());
            wmUser.setStatus(9);
            ResponseResult responseResult = wemediaFeign.save(wmUser);
            if(!responseResult.getCode().equals(0)){
                throw new CostomException(ResponseResult.errorResult(AppHttpCodeEnum.AUTH_FAIL));
            }

        }
        //创建作者
        createApAuthor(wmUser);

        //设置apuser的flag
        apUser.setFlag((short)1);
        apUserMapper.updateById(apUser);
        return  null;

    }

    @Autowired
    private ArticleFeign articleFeign;

    /**
     * 创建作者
     * @param wmUser
     */
    private void createApAuthor(WmUser wmUser) {
        ApAuthor apAuthor = articleFeign.findByUserId(wmUser.getApUserId());
        if(apAuthor == null){
            apAuthor = new ApAuthor();
            apAuthor.setUserId(wmUser.getApUserId());
            apAuthor.setName(wmUser.getName());
            apAuthor.setCreatedTime(new Date());
            apAuthor.setType(2);
            articleFeign.save(apAuthor);
        }
    }
}
