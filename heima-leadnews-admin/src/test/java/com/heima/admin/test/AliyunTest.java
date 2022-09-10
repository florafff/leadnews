package com.heima.admin.test;

import com.heima.admin.AdminApplication;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.fastdfs.FastDFSClient;
import com.netflix.ribbon.proxy.annotation.TemplateName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = AdminApplication.class)
@RunWith(SpringRunner.class)
public class AliyunTest {

    @Autowired
    private GreenTextScan greenTextScan;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Autowired
    protected GreenImageScan greenImageScan;

    @Test
    public void testScanTest() throws Exception {
        Map map = greenTextScan.greenTextScan("你也是一个好人，我知道你不买卖冰毒");
        System.out.println(map);
    }

    @Test
    public void testScanImage() throws Exception {

        List<byte[]> list = new ArrayList<>();

        //下载图片
        byte[] bytes = fastDFSClient.download("group1", "M00/00/00/wKjIgl-2fWyAMiNqAAXmT2JV9VE944.jpg");
        list.add(bytes);
        //审核图片
        Map map = greenImageScan.imageScan(list);
        System.out.println(map);
    }
}
