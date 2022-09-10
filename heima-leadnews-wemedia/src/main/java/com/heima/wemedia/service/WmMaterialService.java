package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import org.springframework.web.multipart.MultipartFile;

public interface WmMaterialService extends IService<WmMaterial> {

    /**
     * 文件上传
     * @param multipartFile
     * @return
     */
    public ResponseResult uploadPicture(MultipartFile multipartFile);

    /**
     * 加载素材列表
     * @param dto
     * @return
     */
    public ResponseResult findList(WmMaterialDto dto);

    /**
     * 删除素材
     * @param id
     * @return
     */
    public ResponseResult delPicture(Integer id);

    /**
     *
     * @param id
     * @param type  type 0 取消收藏  1 收藏
     * @return
     */
    public ResponseResult updateMaterialCollection(Integer id,Short type);
}
