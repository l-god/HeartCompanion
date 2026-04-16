package com.yupi.springai.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class LoveAppDocumentLoaderTest {
    @Resource
    public LoveAppDocumentLoader loveAppDocumentLoader;
    @Test
    void loadMarkdowns() {
      loveAppDocumentLoader.loadMarkdowns();
    }
}