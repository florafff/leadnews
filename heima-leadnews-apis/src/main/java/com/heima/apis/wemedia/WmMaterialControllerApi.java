package com.heima.apis.wemedia;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import io.swagger.models.auth.In;
import org.springframework.web.multipart.MultipartFile;

public interface WmMaterialControllerApi {

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
     * 收藏
     * @param id
     * @return
     */
    public ResponseResult collectionMaterial(Integer id);

    /**
     * 收藏
     * @param id
     * @return
     */
    public ResponseResult cancelCollectionMaterial(Integer id);


}
