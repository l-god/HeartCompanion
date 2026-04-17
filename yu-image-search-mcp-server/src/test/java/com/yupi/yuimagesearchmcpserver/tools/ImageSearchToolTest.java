package com.yupi.yuimagesearchmcpserver.tools;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void searchImage() {
        String result = imageSearchTool.searchImage("computer");
        Assertions.assertNotNull(result);
    }
    @Test
    void searchMediumImages() {
        List<String> images = imageSearchTool.searchMediumImages("computer");

        System.out.println("找到 " + images.size() + " 张图片");
        System.out.println("图片URL列表:");
        for (int i = 0; i < images.size(); i++) {
            System.out.println((i + 1) + ". " + images.get(i));
        }

        Assertions.assertNotNull(images);
        Assertions.assertFalse(images.isEmpty(), "应该至少有一张图片");
    }
}
