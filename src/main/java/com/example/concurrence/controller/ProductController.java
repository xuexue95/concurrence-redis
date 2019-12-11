package com.example.concurrence.controller;

import com.example.concurrence.entity.Product;
import com.example.concurrence.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    @Autowired
    ProductService productService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;


    /**
     * 初始化商品库存接口
     * 从 mysql 获取商品库存,存入 redis
     * @return String
     */
    @RequestMapping("/productInit")
    public String productInit(){
        List<Product> products =  productService.getProductFromMysql();

        for (Product product : products) {
            redisTemplate.opsForValue().set(product.getProductName(), String.valueOf(product.getStock()));
        }
        return "init success";
    }


    /**
     * 抢购接口
     * @param productName 商品名称
     * @param userId 用户id
     * @return String
     */
    @RequestMapping("/rush")
    public String rush(String productName,int userId){
        System.out.println("商品名称:"+ productName +"用户ID:" +userId);

        // 模拟高并发
//        for(int i=0;i<500;i++){
//            int finalI = i;
//            Thread thread=new Thread(() -> {
//                String res =  productService.createOrder(productName, finalI);
//                System.out.println(res);
//            });
//            thread.start();
//        }
        String res =  productService.createOrder(productName, userId);
        return res;
    }

    @RequestMapping("/forMysql")
        public void forMysql() throws InterruptedException {
            productService.setOrderToMysql();
        }
}
