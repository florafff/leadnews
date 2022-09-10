package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.NewsAutoScanConstants;
import com.heima.common.constants.WemediaContants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CostomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vo.WmNewsVo;
import com.heima.utils.threadlocal.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {


    @Value("${fdfs.url}")
    private String fileServerUrl;

    /**
     * 查询自媒体文章列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findAll(WmNewsPageReqDto dto) {
        //1.检查参数
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //分页条件检查
        dto.checkParam();

        //2.分页条件查询
        IPage pageParam = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper();

        //查询当前登录人的文章
        lambdaQueryWrapper.eq(WmNews::getUserId, WmThreadLocalUtils.getUser().getId());

        //状态精确查询
        if(dto.getStatus() != null){
            lambdaQueryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }

        //关键字模糊查询  -- 》 title
        if(StringUtils.isNotBlank(dto.getKeyword())){
            lambdaQueryWrapper.like(WmNews::getTitle,dto.getKeyword());
        }

        //频道精确查询
        if(dto.getChannelId() != null){
            lambdaQueryWrapper.eq(WmNews::getChannelId,dto.getChannelId());
        }

        //时间范围查询
        if(dto.getBeginPubDate()!=null && dto.getEndPubDate()!=null){
            lambdaQueryWrapper.between(WmNews::getPublishTime,dto.getBeginPubDate(),dto.getEndPubDate());
        }

        //时间倒序排列
        lambdaQueryWrapper.orderByDesc(WmNews::getCreatedTime);


        IPage page = page(pageParam, lambdaQueryWrapper);

        //3.结果封装返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        responseResult.setHost(fileServerUrl);
        return responseResult;
    }

    /**
     * @param dto
     * @param isSubmit 0 为草稿  1:待审核
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto, Short isSubmit) {
        //1.检查参数
        if(dto == null || StringUtils.isBlank(dto.getContent())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.保存或修改文章
        WmNews wmNews = new WmNews();
        //属性拷贝，前提条件是：属性名称和类型一致
        BeanUtils.copyProperties(dto,wmNews);
        //数据库中布局的字段类型为 tinyint  取值范围：0-255
        if(dto.getType().equals(WemediaContants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);
        }
        //图片需要转换
        if(dto.getImages()!=null && dto.getImages().size() > 0){
            //["dfdfdsfdfsd.jpg","dfdsfsdfd.jpg"]
           /* String imageStr = dto.getImages().toString().replace("[", "").replace("]", "")
                    .replace(fileServerUrl, "").replace(" ", "");*/
            String imageStr = StringUtils.join(dto.getImages().stream().map(x->x.replace(fileServerUrl,"")
                   .replace(" ",""))
                   .collect(Collectors.toList()), ",");
            wmNews.setImages(imageStr);
        }
        //保存或修改文章
        saveWmNews(wmNews,isSubmit);

        //3.保存文章内容中的图片与素材的关系
        List<String> materials = ectractUrlInfo(dto.getContent());
        //只有在提交的时候，才会保存关系
        if(isSubmit.equals(WmNews.Status.SUBMIT.getCode()) && materials.size() > 0){
            ResponseResult responseResult = saveRelativeInfoForContent(materials,wmNews.getId());
            if(responseResult != null){
                return responseResult;
            }
        }

        //4.保存文章封面中的图片与素材的关系，如果前台给传了一个自动匹配封面，需要处理
        if(isSubmit.equals(WmNews.Status.SUBMIT.getCode()) ){
            ResponseResult responseResult = saveRelativeInfoForCover(materials,wmNews,dto);
            if(responseResult != null){
                return responseResult;
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 保存文章封面中的图片与素材的关系，如果前台给传了一个自动匹配封面，需要处理
     * 截取规则为：
     *  内容图片的个数小于等于2  大于0 则为单图截图一张图，
     *  内容图片大于2，则为多图，截图三张图，
     *  内容中没有图片，则为无图
     * @param materials
     * @param wmNews
     * @param dto
     * @return
     */
    private ResponseResult saveRelativeInfoForCover(List<String> materials, WmNews wmNews, WmNewsDto dto) {
        List<String> images = dto.getImages();//前台传递过来的封面图片列表
        //如果是自动匹配，需要修改文章的type和设置文章封面的url
        if(dto.getType().equals(WemediaContants.WM_NEWS_TYPE_AUTO)){
            //内容图片的个数小于等于2  大于0 则为单图截图一张图
            if(materials.size()> 0 && materials.size() <=2){
                wmNews.setType(WemediaContants.WM_NEWS_SINGLE_IMAGE);
                images=materials.stream().limit(1).collect(Collectors.toList());
            }else if(materials.size()>2){
                //内容图片大于2，则为多图，截图三张图
                wmNews.setType(WemediaContants.WM_NEWS_MANY_IMAGE);
                images=materials.stream().limit(3).collect(Collectors.toList());
            }else {
                wmNews.setImages(null);
                wmNews.setType(WemediaContants.WM_NEWS_NONE_IMAGE);
            }

            //修改文章
            if(images != null && images.size() > 0){
                String imageStr = StringUtils.join(images.stream().map(x->x.replace(fileServerUrl,"")
                        .replace(" ",""))
                        .collect(Collectors.toList()), ",");
                wmNews.setImages(imageStr);
            }
            updateById(wmNews);
        }

        //保存封面图片与文章的关系
        if(images != null && images.size() > 0){
            ResponseResult responseResult = saveRelativeInfoForImage(images,wmNews.getId());
            if(responseResult != null){
                return responseResult;
            }
        }
        return null;
    }

    /**
     * 处理图片的引用类型
     * @param images
     * @param id
     * @return
     */
    private ResponseResult saveRelativeInfoForImage(List<String> images, Integer id) {
        images = images.stream().map(x->x.replace(fileServerUrl,"")).collect(Collectors.toList());
        return saveRelativeInfo(images,id,WemediaContants.WM_COVER_REFERENCE);
    }

    /**
     * 保存文章内容图片与素材的关系
     * @param materials
     * @param newsId
     * @return
     */
    private ResponseResult saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        return saveRelativeInfo(materials,newsId,WemediaContants.WM_CONTENT_REFERENCE);
    }

    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    /**
     * 保存关系到数据库
     * @param materials
     * @param newsId
     * @param type
     * @return
     */
    private ResponseResult saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
        //1.检查素材是否有效
        List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials)
                .eq(WmMaterial::getUserId, WmThreadLocalUtils.getUser().getId()));

        if(dbMaterials == null || dbMaterials.size() == 0){
            throw new CostomException(ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"素材失效"));
        }

        List<Integer> materialIds = new ArrayList<>();

        //{"dsfkdjfkldsj.jpb":23,"dfdfdsf.jpg":34}
        Map<String, Integer> urlIdMap = dbMaterials.stream().collect(Collectors.toMap(WmMaterial::getUrl, WmMaterial::getId));
        for (String material : materials) {
            Integer materialId = urlIdMap.get(material);
            //没找到
            if(materialId == null){
                throw new CostomException(ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"素材失效"));
            }
            //找到了
            //2.找出素材对应的id
            materialIds.add(materialId);
        }
        //3.批量保存
        wmNewsMaterialMapper.saveRelations(materialIds,newsId,type);
        return  null;
    }

    /**
     * 提取文章内容中的图片
     * @param content
     * @return
     */
    private List<String> ectractUrlInfo(String content) {

        List<String> materials = new ArrayList<>();

        List<Map> maps = JSONArray.parseArray(content, Map.class);
        for (Map map : maps) {
            if(map.get("type").equals("image")){
                String imageUrl = (String) map.get("value");
                imageUrl = imageUrl.replace(fileServerUrl,"");
                materials.add(imageUrl);
            }
        }
        return materials;
    }

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 保存或修改文章
     * @param wmNews
     * @param isSubmit
     */
    private void saveWmNews(WmNews wmNews, Short isSubmit) {
        wmNews.setStatus(isSubmit);//草稿或待审核
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setUserId(WmThreadLocalUtils.getUser().getId());
        wmNews.setEnable((short)1);//默认上架

        boolean flag = false;
        if(wmNews.getId() == null){
            //保存
            flag = save(wmNews);
        }else {
            //删除文章与素材的关系
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,wmNews.getId()));
            //修改
            flag = updateById(wmNews);
        }
        //发送消息，审核文章
        if(flag){
            kafkaTemplate.send(NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_TOPIC, JSON.toJSONString(wmNews.getId()));
        }

    }

    /**
     * 根据id查询文章
     * @param id
     * @return
     */
    @Override
    public ResponseResult findWmNewsById(Integer id) {
        //1.检查参数
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章
        WmNews wmNews = getById(id);
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"当前文章不存在");
        }

        //3.结果返回
        ResponseResult responseResult = ResponseResult.okResult(wmNews);
        responseResult.setHost(fileServerUrl);
        return responseResult;
    }

    /**
     * 删除文章
     * @param id
     * @return
     */
    @Override
    public ResponseResult delNews(Integer id) {
        //1.检查参数
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章
        WmNews wmNews = getById(id);
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"当前文章不存在");
        }

        //3.判断文章是否是上架且发布
        if(wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode()) && wmNews.getEnable().equals(WemediaContants.WM_NEWS_ENABLE_UP)){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"当前文章已发布，不能删除");
        }

        //4.删除文章与素材的关系
        wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,wmNews.getId()));

        //5.删除文章
        removeById(wmNews.getId());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 文章的上下架
     * @param dto
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {
        //1.检查参数
        if(dto == null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章
        WmNews wmNews = getById(dto.getId());

        //3.判断当前文章是否是发布状态
        if(!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"当前文章未发布");
        }

        //4.修改  上架和下架
        if(dto.getEnable() != null && dto.getEnable() > -1 && dto.getEnable() < 2){
            update(Wrappers.<WmNews>lambdaUpdate().eq(WmNews::getId,dto.getId()).eq(WmNews::getUserId,WmThreadLocalUtils.getUser().getId())
                    .set(WmNews::getEnable,dto.getEnable()));
            //发送消息给app端进行数据同步
            if(wmNews.getArticleId()!=null){
                Map<String,Object> map = new HashMap<>();
                map.put("enable",dto.getEnable());
                map.put("articleId",wmNews.getArticleId());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,JSON.toJSONString(map));
            }

        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 查询自媒体文章需要发布的文章id列表
     * @return
     */
    @Override
    public List<Integer> findRelease() {
        //条件  status = 4 || 8  && 发布时间小于等于当前时间
        List<WmNews> list = list(Wrappers.<WmNews>lambdaQuery().le(WmNews::getPublishTime, new Date()).in(WmNews::getStatus, 4, 8).select(WmNews::getId));
        List<Integer> idList = list.stream().map(WmNews::getId).collect(Collectors.toList());
        return idList;
    }

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 查询自媒体文章列表 返回的vo对象
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(NewsAuthDto dto) {
        //1.检查参数
        dto.checkParam();


        //2.设置分页
        int currentPage = dto.getPage();
        dto.setPage((dto.getPage()-1)*dto.getSize());
        //设置标题模糊查询
        if(StringUtils.isNotBlank(dto.getTitle())){
            dto.setTitle("%"+dto.getTitle()+"%");
        }
        //3.查询
        List<WmNewsVo> wmNewsVoList = wmNewsMapper.findListAndPage(dto);
        int count = wmNewsMapper.findListCount(dto);

        //4.结果封装
        ResponseResult responseResult = new PageResponseResult(currentPage,dto.getSize(),count);
        responseResult.setData(wmNewsVoList);
        responseResult.setHost(fileServerUrl);
        return responseResult;
    }

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 查询文章详情  返回的vo对象
     * @param id
     * @return
     */
    @Override
    public ResponseResult findWmNewsVo(Integer id) {
        //1.检查参数
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章信息
        WmNews wmNews = getById(id);
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //3.查询作者信息
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());

        //4.结果封装  vo
        WmNewsVo wmNewsVo = new WmNewsVo();
        BeanUtils.copyProperties(wmNews,wmNewsVo);
        wmNewsVo.setAuthorName(wmUser.getName());
        ResponseResult responseResult = ResponseResult.okResult(wmNewsVo);
        responseResult.setHost(fileServerUrl);
        return responseResult;
    }

    /**
     * 修改文章的状态
     * @param status 2 失败  4 人工审核成功
     * @param dto
     * @return
     */
    @Override
    public ResponseResult updateStatus(Short status, NewsAuthDto dto) {
        //1.检查参数
        if(dto== null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章信息
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3.修改文章
        wmNews.setStatus(status);
        if(StringUtils.isNotBlank(dto.getMsg())){
            wmNews.setReason(dto.getMsg());
        }
        updateById(wmNews);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
