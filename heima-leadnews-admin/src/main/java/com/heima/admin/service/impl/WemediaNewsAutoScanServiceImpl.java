package com.heima.admin.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.admin.feign.ArticleFeign;
import com.heima.admin.feign.WemediaFeign;
import com.heima.admin.mapper.AdChannelMapper;
import com.heima.admin.mapper.AdSensitiveMapper;
import com.heima.admin.service.WemediaNewsAutoScanService;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.fastdfs.FastDFSClient;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WemediaNewsAutoScanServiceImpl implements WemediaNewsAutoScanService {

    @Autowired
    private WemediaFeign wemediaFeign;

    /**
     * 自媒体文章自动审核
     *
     * @param id
     */
    @Override
    @GlobalTransactional
    public void autoScanByMediaNewsId(Integer id) {
        if (id == null) {
            throw new RuntimeException("当前文章id不存在");
        }
        //1.根据id查询自媒体文章信息
        WmNews wmNews = wemediaFeign.findNewsById(id);
        if (wmNews == null) {
            throw new RuntimeException("当前自媒体文章不存在");
        }

        //2.文章状态为4（人工审核通过）直接保存数据和创建索引
        if (wmNews.getStatus() == 4 && wmNews.getPublishTime().getTime() <= System.currentTimeMillis()) {
            //保存数据
            saveAppArticle(wmNews);
            return;
        }

        //3.文章状态为8 发布时间小于等于当前时间 直接保存数据
        if (wmNews.getStatus() == 8 && wmNews.getPublishTime().getTime() <= System.currentTimeMillis()) {
            //保存数据
            saveAppArticle(wmNews);
            return;
        }

        //4. 文章状态为1，待审核
        if (wmNews.getStatus() == 1) {

            //提取文章内容中的正文和图片
            //两部分  一个是文本  一个是图片列表
            Map<String, Object> resultMap = handleTextAndImages(wmNews);

            //4.1  文本审核-阿里云接口
            boolean isTextScan = handleTextScan((String) resultMap.get("content"), wmNews);
            if (!isTextScan) return;

            //4.2  图片审核-阿里云接口
            boolean isImageScan = handleImagesScan((List<String>) resultMap.get("images"), wmNews);
            if (!isImageScan) return;

            //4.3  文本审核- 自管理的敏感词
            boolean isSensitiveScan = handleSensitive((String) resultMap.get("content"), wmNews);
            if (!isSensitiveScan) return;

            //4.4 发布时间大于当前时间，修改文章状态为8
            if(wmNews.getPublishTime().getTime() > System.currentTimeMillis()){
                updateWmNews(wmNews,(short)8,"待发布");
                return;
            }

            //4.5 审核通过，修改自媒体文章状态为9  保存app端相关数据
            saveAppArticle(wmNews);
        }


    }

    @Autowired
    private AdSensitiveMapper adSensitiveMapper;

    /**
     * 自管理敏感词审核
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitive(String content, WmNews wmNews) {

        boolean flag = true;

        // select sensitives from ad_sensitive
        List<AdSensitive> sensitiveList = adSensitiveMapper.selectList(Wrappers.<AdSensitive>lambdaQuery().select(AdSensitive::getSensitives));
        List<String> allSensitive = sensitiveList.stream().map(x -> x.getSensitives()).collect(Collectors.toList());
        SensitiveWordUtil.initMap(allSensitive);//初始化敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if(map.size() > 0){
            updateWmNews(wmNews,(short)2,"文本内容包含了敏感词"+map);
            flag = false;
        }
        return flag;
    }

    @Autowired
    private FastDFSClient fastDFSClient;

    @Autowired
    private GreenImageScan greenImageScan;

    @Value("${fdfs.url}")
    private String fileServerUrl;

    /**
     * 阿里云图片审核
     *
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImagesScan(List<String> images, WmNews wmNews) {
        //没有图片，直接跳过
        if(images==null || images.size() == 0){
            return true;
        }

        boolean flag = true;

        try {

            List<byte []> imageList = new ArrayList<>();
            //下载图片
            //group1/M00/00/00/wKjIgl892uyAR12rAADi7UxPXeM267.jpg
            for (String image : images) {
                String imageName = image.replace(fileServerUrl, "");
                int index = imageName.indexOf("/");//第一个斜线的索引位置
                String groupName = imageName.substring(0, index);
                String imagePath = imageName.substring(index + 1);
                byte[] imageByte = fastDFSClient.download(groupName, imagePath);
                imageList.add(imageByte);
            }
            //审核图片
            Map map = greenImageScan.imageScan(imageList);
            //审核失败
            if (map.get("suggestion").equals("block")) {
                updateWmNews(wmNews, (short) 2, "图片内容违规");
                flag = false;
            }
            //人工审核
            if (map.get("suggestion").equals("review")) {
                updateWmNews(wmNews, (short) 3, "图片内容有不确定词汇");
                flag = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        }

        return flag;


    }

    @Autowired
    private GreenTextScan greenTextScan;

    /**
     * 阿里云文本审核
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {

        boolean flag = true;

        try {
            Map map = greenTextScan.greenTextScan(content);
            //审核失败
            if (map.get("suggestion").equals("block")) {
                updateWmNews(wmNews, (short) 2, "文本内容违规");
                flag = false;
            }
            //人工审核
            if (map.get("suggestion").equals("review")) {
                updateWmNews(wmNews, (short) 3, "文本内容有不确定词汇");
                flag = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        }
        return flag;
    }

    /**
     * 修改自媒体文章
     *
     * @param wmNews
     * @param status
     * @param msg
     */
    private void updateWmNews(WmNews wmNews, short status, String msg) {
        wmNews.setStatus(status);
        wmNews.setReason(msg);
        ResponseResult responseResult = wemediaFeign.updateWmNews(wmNews);
        if(!responseResult.getCode().equals(0)){
            throw new RuntimeException("修改自媒体文章失败");
        }
    }

    /**
     * 提取文章内容中的正文和图片（正文中的图片和封面图片）
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {
        String content = wmNews.getContent();//自媒体文章内容
        List<Map> maps = JSONArray.parseArray(content, Map.class);

        //正文
        StringBuilder sb = new StringBuilder();

        //图片列表
        List<String> images = new ArrayList<>();

        for (Map map : maps) {
            //拼接正文
            if (map.get("type").equals("text")) {
                sb.append(map.get("value"));
            }
            //拼接图片
            if (map.get("type").equals("image")) {
                images.add((String) map.get("value"));
            }

        }
        //封面图片
        if (wmNews.getImages() != null && wmNews.getImages().length() > 0) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("content", sb.toString());
        resultMap.put("images", images);
        return resultMap;
    }

    /**
     * 保存app端相关文章
     *
     * @param wmNews
     */
    private void saveAppArticle(WmNews wmNews) {
        //调用远程接口保存app相对的数据  ResponseResult
        ResponseResult responseResult = savaArticle(wmNews);
        if(!responseResult.getCode().equals(0)){
            throw new RuntimeException("保存app端文章失败");
        }

        //修改自媒体文章  设置articleId  修改状态为已发布  9
        Object articleId = responseResult.getData();
        wmNews.setArticleId((Long) articleId);
        updateWmNews(wmNews,(short) 9,"已发布");

        System.out.println("app文章保存。。。");
    }

    @Autowired
    ArticleFeign articleFeign;

    @Autowired
    private AdChannelMapper adChannelMapper;

    /**
     * 封面参数
     * @param wmNews
     * @return
     */
    private ResponseResult savaArticle(WmNews wmNews) {
        ArticleDto dto = new ArticleDto();
        //如果wmnews中包含了articleid
        if(wmNews.getArticleId() != null){
            dto.setId(wmNews.getArticleId());
        }

        dto.setTitle(wmNews.getTitle());
        dto.setContent(wmNews.getContent());
        dto.setImages(wmNews.getImages());
        dto.setPublishTime(wmNews.getPublishTime());
        dto.setLayout(wmNews.getType());
        dto.setCreatedTime(new Date());

        //频道设置
        AdChannel channel = adChannelMapper.selectById(wmNews.getChannelId());
        if(channel != null){
            dto.setChannelId(channel.getId());
            dto.setChannelName(channel.getName());
        }

        //作者相关设置
        WmUser wmUser = wemediaFeign.findUserById(wmNews.getUserId());
        if(wmUser != null){
            dto.setAuthorName(wmUser.getName());
        }


        return articleFeign.saveAppArticle(dto);
    }
}
